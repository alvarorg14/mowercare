package com.mowercare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.mowercare.exception.ResourceNotFoundException;
import com.mowercare.model.Issue;
import com.mowercare.model.IssueChangeEvent;
import com.mowercare.model.IssueChangeType;
import com.mowercare.model.IssuePriority;
import com.mowercare.model.IssueStatus;
import com.mowercare.model.NotificationEvent;
import com.mowercare.model.NotificationEventType;
import com.mowercare.model.Organization;
import com.mowercare.model.User;
import com.mowercare.model.UserRole;
import com.mowercare.repository.IssueChangeEventRepository;
import com.mowercare.repository.IssueRepository;
import com.mowercare.repository.NotificationEventRepository;
import com.mowercare.repository.OrganizationRepository;
import com.mowercare.repository.RefreshTokenRepository;
import com.mowercare.repository.UserRepository;
import com.mowercare.testsupport.AbstractPostgresIntegrationTest;

@Transactional
class IssueServiceIT extends AbstractPostgresIntegrationTest {

	@Autowired
	private IssueService issueService;

	@Autowired
	private IssueRepository issueRepository;

	@Autowired
	private IssueChangeEventRepository issueChangeEventRepository;

	@Autowired
	private NotificationEventRepository notificationEventRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void clean() {
		notificationEventRepository.deleteAll();
		issueChangeEventRepository.deleteAll();
		issueRepository.deleteAll();
		refreshTokenRepository.deleteAll();
		userRepository.deleteAll();
		organizationRepository.deleteAll();
	}

	@Test
	@DisplayName("Given issue created, when status update and cross-org lookup, then history and tenant isolation")
	void given_issueCreated_whenStatusUpdateAndCrossOrgLookup_thenHistoryAndTenantIsolation() {
		Organization orgA = organizationRepository.save(new Organization("Org A"));
		User actor = userRepository.save(
				new User(orgA, "actor@example.com", passwordEncoder.encode("password123"), UserRole.ADMIN));
		User other = userRepository.save(
				new User(orgA, "other@example.com", passwordEncoder.encode("password123"), UserRole.TECHNICIAN));

		Issue issue = issueService.createIssue(
				orgA.getId(),
				actor.getId(),
				"Blade stuck",
				"Customer reports noise",
				IssueStatus.OPEN,
				IssuePriority.MEDIUM,
				other.getId(),
				"Acme Yard",
				"Site 7");

		List<IssueChangeEvent> afterCreate =
				issueChangeEventRepository.findByIssue_IdOrderByOccurredAtAsc(issue.getId());
		assertThat(afterCreate).hasSize(1);
		assertThat(afterCreate.getFirst().getChangeType()).isEqualTo(IssueChangeType.CREATED);
		assertThat(afterCreate.getFirst().getActor().getId()).isEqualTo(actor.getId());
		assertThat(afterCreate.getFirst().getNewValue()).isEqualTo("Blade stuck");

		List<NotificationEvent> notifAfterCreate =
				notificationEventRepository.findByIssue_IdOrderByOccurredAtAsc(issue.getId());
		assertThat(notifAfterCreate).hasSize(1);
		assertThat(notifAfterCreate.getFirst().getEventType()).isEqualTo(NotificationEventType.ISSUE_CREATED.taxonomyValue());
		assertThat(notifAfterCreate.getFirst().getSourceIssueChangeEvent().getId())
				.isEqualTo(afterCreate.getFirst().getId());

		issueService.updateStatus(issue.getId(), orgA.getId(), actor.getId(), IssueStatus.IN_PROGRESS);

		List<IssueChangeEvent> afterStatus =
				issueChangeEventRepository.findByIssue_IdOrderByOccurredAtAsc(issue.getId());
		assertThat(afterStatus).hasSize(2);
		assertThat(afterStatus.get(1).getChangeType()).isEqualTo(IssueChangeType.STATUS_CHANGED);
		assertThat(afterStatus.get(1).getOldValue()).isEqualTo(IssueStatus.OPEN.name());
		assertThat(afterStatus.get(1).getNewValue()).isEqualTo(IssueStatus.IN_PROGRESS.name());
		assertThat(afterStatus.get(1).getActor().getId()).isEqualTo(actor.getId());

		List<NotificationEvent> notifAfterStatus =
				notificationEventRepository.findByIssue_IdOrderByOccurredAtAsc(issue.getId());
		assertThat(notifAfterStatus).hasSize(2);
		assertThat(notifAfterStatus.get(1).getEventType())
				.isEqualTo(NotificationEventType.ISSUE_STATUS_CHANGED.taxonomyValue());
		assertThat(notifAfterStatus.get(1).getOccurredAt()).isEqualTo(afterStatus.get(1).getOccurredAt());

		Organization orgB = organizationRepository.save(new Organization("Org B"));
		assertThat(issueRepository.findByIdAndOrganization_Id(issue.getId(), orgB.getId())).isEmpty();
		assertThat(issueRepository.findByIdAndOrganization_Id(issue.getId(), orgA.getId())).isPresent();
	}

	@Test
	@DisplayName("Given same status, when updateStatus, then no duplicate history row")
	void given_sameStatus_whenUpdateStatus_thenNoDuplicateHistory() {
		Organization org = organizationRepository.save(new Organization("Org C"));
		User actor = userRepository.save(
				new User(org, "solo@example.com", passwordEncoder.encode("password123"), UserRole.ADMIN));

		Issue issue = issueService.createIssue(
				org.getId(),
				actor.getId(),
				"T",
				null,
				IssueStatus.OPEN,
				IssuePriority.LOW,
				null,
				null,
				null);

		issueService.updateStatus(issue.getId(), org.getId(), actor.getId(), IssueStatus.OPEN);

		assertThat(issueChangeEventRepository.findByIssue_IdOrderByOccurredAtAsc(issue.getId()))
				.hasSize(1);
		assertThat(notificationEventRepository.findByIssue_IdOrderByOccurredAtAsc(issue.getId()))
				.hasSize(1)
				.first()
				.satisfies(n -> assertThat(n.getEventType()).isEqualTo(NotificationEventType.ISSUE_CREATED.taxonomyValue()));
	}

	@Test
	@DisplayName("Given reassign, when updateAssignee, then ASSIGNEE_CHANGED with old and new user ids")
	void given_reassign_whenUpdateAssignee_thenAssigneeChangedWithUserIds() {
		Organization org = organizationRepository.save(new Organization("Org D"));
		User actor = userRepository.save(
				new User(org, "admin@d.test", passwordEncoder.encode("password123"), UserRole.ADMIN));
		User first = userRepository.save(
				new User(org, "first@d.test", passwordEncoder.encode("password123"), UserRole.TECHNICIAN));
		User second = userRepository.save(
				new User(org, "second@d.test", passwordEncoder.encode("password123"), UserRole.TECHNICIAN));

		Issue issue = issueService.createIssue(
				org.getId(),
				actor.getId(),
				"Handoff",
				null,
				IssueStatus.OPEN,
				IssuePriority.MEDIUM,
				first.getId(),
				null,
				null);

		issueService.updateAssignee(issue.getId(), org.getId(), actor.getId(), second.getId());

		List<IssueChangeEvent> events = issueChangeEventRepository.findByIssue_IdOrderByOccurredAtAsc(issue.getId());
		assertThat(events).hasSize(2);
		IssueChangeEvent assigneeEv = events.get(1);
		assertThat(assigneeEv.getChangeType()).isEqualTo(IssueChangeType.ASSIGNEE_CHANGED);
		assertThat(assigneeEv.getOldValue()).isEqualTo(first.getId().toString());
		assertThat(assigneeEv.getNewValue()).isEqualTo(second.getId().toString());
	}

	@Test
	@DisplayName("Given priority bump, when updatePriority, then PRIORITY_CHANGED")
	void given_priorityBump_whenUpdatePriority_thenPriorityChanged() {
		Organization org = organizationRepository.save(new Organization("Org E"));
		User actor = userRepository.save(
				new User(org, "a@e.test", passwordEncoder.encode("password123"), UserRole.ADMIN));

		Issue issue = issueService.createIssue(
				org.getId(),
				actor.getId(),
				"P",
				null,
				IssueStatus.OPEN,
				IssuePriority.LOW,
				null,
				null,
				null);

		issueService.updatePriority(issue.getId(), org.getId(), actor.getId(), IssuePriority.HIGH);

		List<IssueChangeEvent> events = issueChangeEventRepository.findByIssue_IdOrderByOccurredAtAsc(issue.getId());
		assertThat(events).hasSize(2);
		assertThat(events.get(1).getChangeType()).isEqualTo(IssueChangeType.PRIORITY_CHANGED);
		assertThat(events.get(1).getOldValue()).isEqualTo(IssuePriority.LOW.name());
		assertThat(events.get(1).getNewValue()).isEqualTo(IssuePriority.HIGH.name());

		assertThat(notificationEventRepository.findByIssue_IdOrderByOccurredAtAsc(issue.getId()))
				.hasSize(1)
				.first()
				.satisfies(n -> assertThat(n.getEventType()).isEqualTo(NotificationEventType.ISSUE_CREATED.taxonomyValue()));
	}

	@Test
	@DisplayName("Given title and description changes, when updates, then TITLE_CHANGED and DESCRIPTION_CHANGED")
	void given_titleAndDescriptionChanges_whenUpdates_thenTitleAndDescriptionEvents() {
		Organization org = organizationRepository.save(new Organization("Org F"));
		User actor = userRepository.save(
				new User(org, "a@f.test", passwordEncoder.encode("password123"), UserRole.ADMIN));

		Issue issue = issueService.createIssue(
				org.getId(),
				actor.getId(),
				"Old title",
				null,
				IssueStatus.OPEN,
				IssuePriority.LOW,
				null,
				null,
				null);

		issueService.updateTitle(issue.getId(), org.getId(), actor.getId(), "New title");
		issueService.updateDescription(issue.getId(), org.getId(), actor.getId(), "Details");

		List<IssueChangeEvent> events = issueChangeEventRepository.findByIssue_IdOrderByOccurredAtAsc(issue.getId());
		assertThat(events).hasSize(3);
		assertThat(events.get(1).getChangeType()).isEqualTo(IssueChangeType.TITLE_CHANGED);
		assertThat(events.get(1).getOldValue()).isEqualTo("Old title");
		assertThat(events.get(1).getNewValue()).isEqualTo("New title");
		assertThat(events.get(2).getChangeType()).isEqualTo(IssueChangeType.DESCRIPTION_CHANGED);
		assertThat(events.get(2).getOldValue()).isNull();
		assertThat(events.get(2).getNewValue()).isEqualTo("Details");

		assertThat(notificationEventRepository.findByIssue_IdOrderByOccurredAtAsc(issue.getId()))
				.hasSize(1)
				.first()
				.satisfies(n -> assertThat(n.getEventType()).isEqualTo(NotificationEventType.ISSUE_CREATED.taxonomyValue()));
	}

	@Test
	@DisplayName("Given assignee change, when updateAssignee, then issue.assigned notification")
	void given_assigneeChange_whenUpdateAssignee_thenIssueAssignedNotification() {
		Organization org = organizationRepository.save(new Organization("Org Notif Assign"));
		User actor = userRepository.save(
				new User(org, "admin@na.test", passwordEncoder.encode("password123"), UserRole.ADMIN));
		User first = userRepository.save(
				new User(org, "first@na.test", passwordEncoder.encode("password123"), UserRole.TECHNICIAN));
		User second = userRepository.save(
				new User(org, "second@na.test", passwordEncoder.encode("password123"), UserRole.TECHNICIAN));

		Issue issue = issueService.createIssue(
				org.getId(),
				actor.getId(),
				"N",
				null,
				IssueStatus.OPEN,
				IssuePriority.MEDIUM,
				first.getId(),
				null,
				null);

		issueService.updateAssignee(issue.getId(), org.getId(), actor.getId(), second.getId());

		List<NotificationEvent> notifs = notificationEventRepository.findByIssue_IdOrderByOccurredAtAsc(issue.getId());
		assertThat(notifs).hasSize(2);
		assertThat(notifs.get(0).getEventType()).isEqualTo(NotificationEventType.ISSUE_CREATED.taxonomyValue());
		assertThat(notifs.get(1).getEventType()).isEqualTo(NotificationEventType.ISSUE_ASSIGNED.taxonomyValue());
		assertThat(notifs.get(1).getActor().getId()).isEqualTo(actor.getId());
	}

	@Test
	@DisplayName("Given wrong organization id, when updateStatus, then throws ResourceNotFoundException")
	void given_wrongOrganizationId_whenUpdateStatus_thenThrowsResourceNotFoundException() {
		Organization orgA = organizationRepository.save(new Organization("Org A2"));
		Organization orgB = organizationRepository.save(new Organization("Org B2"));
		User actor = userRepository.save(
				new User(orgA, "a@ab.test", passwordEncoder.encode("password123"), UserRole.ADMIN));

		Issue issue = issueService.createIssue(
				orgA.getId(),
				actor.getId(),
				"X",
				null,
				IssueStatus.OPEN,
				IssuePriority.LOW,
				null,
				null,
				null);

		assertThatThrownBy(() -> issueService.updateStatus(issue.getId(), orgB.getId(), actor.getId(), IssueStatus.CLOSED))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Issue");
	}
}
