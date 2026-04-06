package com.mowercare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.mowercare.exception.ResourceNotFoundException;
import com.mowercare.model.AccountStatus;
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
import com.mowercare.model.NotificationRecipient;
import com.mowercare.repository.NotificationEventRepository;
import com.mowercare.repository.NotificationRecipientRepository;
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
	private NotificationRecipientRepository notificationRecipientRepository;

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
		notificationRecipientRepository.deleteAll();
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

		List<NotificationRecipient> assignedRec =
				notificationRecipientRepository.findByNotificationEvent_Id(notifs.get(1).getId());
		assertThat(assignedRec).hasSize(1);
		assertThat(assignedRec.getFirst().getRecipient().getId()).isEqualTo(second.getId());
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

	@Test
	@DisplayName("Story 4.2: issue.created — actor admin excluded; assignee technician receives row")
	void given_createIssue_whenAssigneeTechnician_thenRecipientIsAssigneeOnly() {
		Organization org = organizationRepository.save(new Organization("Org Recipients 1"));
		User adminActor = userRepository.save(
				new User(org, "admin@rec1.test", passwordEncoder.encode("password123"), UserRole.ADMIN));
		User tech = userRepository.save(
				new User(org, "tech@rec1.test", passwordEncoder.encode("password123"), UserRole.TECHNICIAN));

		Issue issue =
				issueService.createIssue(
						org.getId(),
						adminActor.getId(),
						"T",
						null,
						IssueStatus.OPEN,
						IssuePriority.LOW,
						tech.getId(),
						null,
						null);

		NotificationEvent created =
				notificationEventRepository.findByIssue_IdOrderByOccurredAtAsc(issue.getId()).getFirst();
		List<NotificationRecipient> rec = notificationRecipientRepository.findByNotificationEvent_Id(created.getId());
		assertThat(rec).hasSize(1);
		assertThat(rec.getFirst().getRecipient().getId()).isEqualTo(tech.getId());
	}

	@Test
	@DisplayName("Story 4.2: issue.created — assignee admin deduped to one row when also only other eligible admin")
	void given_createIssue_whenAssigneeIsSecondAdmin_thenSingleRecipientRow() {
		Organization org = organizationRepository.save(new Organization("Org Recipients 2"));
		User adminActor = userRepository.save(
				new User(org, "a1@rec2.test", passwordEncoder.encode("password123"), UserRole.ADMIN));
		User adminAssignee = userRepository.save(
				new User(org, "a2@rec2.test", passwordEncoder.encode("password123"), UserRole.ADMIN));

		Issue issue =
				issueService.createIssue(
						org.getId(),
						adminActor.getId(),
						"T",
						null,
						IssueStatus.OPEN,
						IssuePriority.LOW,
						adminAssignee.getId(),
						null,
						null);

		UUID neId = notificationEventRepository.findByIssue_IdOrderByOccurredAtAsc(issue.getId()).getFirst().getId();
		List<NotificationRecipient> rec = notificationRecipientRepository.findByNotificationEvent_Id(neId);
		assertThat(rec).hasSize(1);
		assertThat(rec.getFirst().getRecipient().getId()).isEqualTo(adminAssignee.getId());
	}

	@Test
	@DisplayName("Story 4.2: issue.status_changed — non-actor admin and assignee notified")
	void given_statusChange_whenTwoAdminsAndAssigneeTech_thenRecipientsExcludeActor() {
		Organization org = organizationRepository.save(new Organization("Org Recipients 3"));
		User adminActor = userRepository.save(
				new User(org, "a1@rec3.test", passwordEncoder.encode("password123"), UserRole.ADMIN));
		User adminOther = userRepository.save(
				new User(org, "a2@rec3.test", passwordEncoder.encode("password123"), UserRole.ADMIN));
		User tech = userRepository.save(
				new User(org, "t@rec3.test", passwordEncoder.encode("password123"), UserRole.TECHNICIAN));

		Issue issue =
				issueService.createIssue(
						org.getId(),
						adminActor.getId(),
						"T",
						null,
						IssueStatus.OPEN,
						IssuePriority.LOW,
						tech.getId(),
						null,
						null);

		issueService.updateStatus(issue.getId(), org.getId(), adminActor.getId(), IssueStatus.IN_PROGRESS);

		List<NotificationEvent> evs = notificationEventRepository.findByIssue_IdOrderByOccurredAtAsc(issue.getId());
		assertThat(evs).hasSize(2);
		UUID statusEventId = evs.get(1).getId();
		List<NotificationRecipient> rec = notificationRecipientRepository.findByNotificationEvent_Id(statusEventId);
		assertThat(rec).hasSize(2);
		assertThat(rec.stream().map(NotificationRecipient::getRecipient).map(User::getId).toList())
				.containsExactlyInAnyOrder(tech.getId(), adminOther.getId());
	}

	@Test
	@DisplayName("Story 4.2: issue.assigned — unassign notifies non-actor admins only")
	void given_unassign_whenTwoAdmins_thenAssignedEventRecipientIsOtherAdminOnly() {
		Organization org = organizationRepository.save(new Organization("Org Recipients 4"));
		User actor = userRepository.save(
				new User(org, "a1@rec4.test", passwordEncoder.encode("password123"), UserRole.ADMIN));
		User otherAdmin = userRepository.save(
				new User(org, "a2@rec4.test", passwordEncoder.encode("password123"), UserRole.ADMIN));
		User tech = userRepository.save(
				new User(org, "t@rec4.test", passwordEncoder.encode("password123"), UserRole.TECHNICIAN));

		Issue issue =
				issueService.createIssue(
						org.getId(),
						actor.getId(),
						"T",
						null,
						IssueStatus.OPEN,
						IssuePriority.LOW,
						tech.getId(),
						null,
						null);

		issueService.updateAssignee(issue.getId(), org.getId(), actor.getId(), null);

		List<NotificationEvent> evs = notificationEventRepository.findByIssue_IdOrderByOccurredAtAsc(issue.getId());
		assertThat(evs).hasSize(2);
		List<NotificationRecipient> rec =
				notificationRecipientRepository.findByNotificationEvent_Id(evs.get(1).getId());
		assertThat(rec).hasSize(1);
		assertThat(rec.getFirst().getRecipient().getId()).isEqualTo(otherAdmin.getId());
	}

	@Test
	@DisplayName("Story 4.2: issue.created — PENDING_INVITE admin does not receive recipient row")
	void given_createIssue_whenPendingInviteAdminInOrg_thenOnlyActiveAssigneeReceives() {
		Organization org = organizationRepository.save(new Organization("Org Recipients 5"));
		User adminActor = userRepository.save(
				new User(org, "admin@rec5.test", passwordEncoder.encode("password123"), UserRole.ADMIN));
		User pendingAdmin =
				userRepository.save(
						new User(
								org,
								"pending@rec5.test",
								passwordEncoder.encode("password123"),
								UserRole.ADMIN,
								AccountStatus.PENDING_INVITE,
								"invite-token-hash-rec5",
								Instant.now().plusSeconds(86400)));
		User tech = userRepository.save(
				new User(org, "tech@rec5.test", passwordEncoder.encode("password123"), UserRole.TECHNICIAN));

		Issue issue =
				issueService.createIssue(
						org.getId(),
						adminActor.getId(),
						"T",
						null,
						IssueStatus.OPEN,
						IssuePriority.LOW,
						tech.getId(),
						null,
						null);

		NotificationEvent created =
				notificationEventRepository.findByIssue_IdOrderByOccurredAtAsc(issue.getId()).getFirst();
		List<NotificationRecipient> rec = notificationRecipientRepository.findByNotificationEvent_Id(created.getId());
		assertThat(rec).hasSize(1);
		assertThat(rec.getFirst().getRecipient().getId()).isEqualTo(tech.getId());
		assertThat(rec.stream().map(NotificationRecipient::getRecipient).map(User::getId))
				.doesNotContain(pendingAdmin.getId());
	}

	@Test
	@DisplayName("Story 4.2: issue.created — deactivated assignee does not receive recipient row")
	void given_createIssue_whenAssigneeDeactivated_thenNoRecipientRows() {
		Organization org = organizationRepository.save(new Organization("Org Recipients 6"));
		User adminActor = userRepository.save(
				new User(org, "admin@rec6.test", passwordEncoder.encode("password123"), UserRole.ADMIN));
		User tech = userRepository.save(
				new User(org, "tech@rec6.test", passwordEncoder.encode("password123"), UserRole.TECHNICIAN));
		tech.deactivate(adminActor.getId(), Instant.now());
		userRepository.save(tech);

		Issue issue =
				issueService.createIssue(
						org.getId(),
						adminActor.getId(),
						"T",
						null,
						IssueStatus.OPEN,
						IssuePriority.LOW,
						tech.getId(),
						null,
						null);

		NotificationEvent created =
				notificationEventRepository.findByIssue_IdOrderByOccurredAtAsc(issue.getId()).getFirst();
		List<NotificationRecipient> rec = notificationRecipientRepository.findByNotificationEvent_Id(created.getId());
		assertThat(rec).isEmpty();
	}
}
