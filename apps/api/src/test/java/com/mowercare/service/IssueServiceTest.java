package com.mowercare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.mowercare.exception.ResourceNotFoundException;
import com.mowercare.model.Issue;
import com.mowercare.model.IssueChangeEvent;
import com.mowercare.model.IssueChangeType;
import com.mowercare.model.IssuePriority;
import com.mowercare.model.IssueStatus;
import com.mowercare.model.Organization;
import com.mowercare.model.User;
import com.mowercare.model.UserRole;
import com.mowercare.repository.IssueChangeEventRepository;
import com.mowercare.repository.IssueRepository;
import com.mowercare.repository.OrganizationRepository;
import com.mowercare.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

	private static final UUID ORG_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID ACTOR_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
	private static final UUID ASSIGNEE_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
	private static final UUID ISSUE_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");

	@Mock
	private IssueRepository issueRepository;

	@Mock
	private IssueChangeEventRepository issueChangeEventRepository;

	@Mock
	private OrganizationRepository organizationRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private NotificationEventRecorder notificationEventRecorder;

	@InjectMocks
	private IssueService issueService;

	private Organization organization;
	private User actor;
	private User assignee;

	@BeforeEach
	void setUp() {
		organization = new Organization("Test Org");
		ReflectionTestUtils.setField(organization, "id", ORG_ID);
		actor = new User(organization, "actor@test", "hash", UserRole.ADMIN);
		ReflectionTestUtils.setField(actor, "id", ACTOR_ID);
		assignee = new User(organization, "assignee@test", "hash", UserRole.TECHNICIAN);
		ReflectionTestUtils.setField(assignee, "id", ASSIGNEE_ID);
		lenient().when(issueChangeEventRepository.save(any(IssueChangeEvent.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	@Test
	@DisplayName("Given null title, when createIssue, then throws NullPointerException")
	void given_nullTitle_whenCreateIssue_thenThrowsNullPointerException() {
		assertThatThrownBy(() -> issueService.createIssue(
						ORG_ID,
						ACTOR_ID,
						null,
						null,
						IssueStatus.OPEN,
						IssuePriority.LOW,
						null,
						null,
						null))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("title");

		verify(organizationRepository, never()).findById(any());
	}

	@Test
	@DisplayName("Given no organization, when createIssue, then throws ResourceNotFoundException")
	void given_noOrganization_whenCreateIssue_thenThrowsResourceNotFoundException() {
		when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> issueService.createIssue(
						ORG_ID,
						ACTOR_ID,
						"T",
						null,
						IssueStatus.OPEN,
						IssuePriority.LOW,
						null,
						null,
						null))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Organization");

		verify(userRepository, never()).findByOrganization_IdAndId(any(), any());
	}

	@Test
	@DisplayName("Given actor missing in org, when createIssue, then throws ResourceNotFoundException")
	void given_actorMissing_whenCreateIssue_thenThrowsResourceNotFoundException() {
		when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
		when(userRepository.findByOrganization_IdAndId(ORG_ID, ACTOR_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> issueService.createIssue(
						ORG_ID,
						ACTOR_ID,
						"T",
						null,
						IssueStatus.OPEN,
						IssuePriority.LOW,
						null,
						null,
						null))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Actor");

		verify(issueRepository, never()).save(any());
	}

	@Test
	@DisplayName("Given assignee id unknown in org, when createIssue, then throws ResourceNotFoundException")
	void given_assigneeUnknown_whenCreateIssue_thenThrowsResourceNotFoundException() {
		when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
		when(userRepository.findByOrganization_IdAndId(ORG_ID, ACTOR_ID)).thenReturn(Optional.of(actor));
		when(userRepository.findByOrganization_IdAndId(ORG_ID, ASSIGNEE_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> issueService.createIssue(
						ORG_ID,
						ACTOR_ID,
						"T",
						null,
						IssueStatus.OPEN,
						IssuePriority.LOW,
						ASSIGNEE_ID,
						null,
						null))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Assignee");
	}

	@Test
	@DisplayName("Given valid org and actor, when createIssue, then saves issue and emits CREATED event")
	void given_validOrgAndActor_whenCreateIssue_thenSavesIssueAndEmitsCreatedEvent() {
		when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
		when(userRepository.findByOrganization_IdAndId(ORG_ID, ACTOR_ID)).thenReturn(Optional.of(actor));
		when(issueRepository.save(any(Issue.class)))
				.thenAnswer(inv -> {
					Issue i = inv.getArgument(0);
					ReflectionTestUtils.setField(i, "id", ISSUE_ID);
					return i;
				});

		Issue result = issueService.createIssue(
				ORG_ID,
				ACTOR_ID,
				"My title",
				"desc",
				IssueStatus.OPEN,
				IssuePriority.MEDIUM,
				null,
				"cust",
				"site");

		assertThat(result.getId()).isEqualTo(ISSUE_ID);
		verify(issueRepository).save(any(Issue.class));

		ArgumentCaptor<IssueChangeEvent> eventCap = ArgumentCaptor.forClass(IssueChangeEvent.class);
		verify(issueChangeEventRepository).save(eventCap.capture());
		IssueChangeEvent ev = eventCap.getValue();
		assertThat(ev.getChangeType()).isEqualTo(IssueChangeType.CREATED);
		assertThat(ev.getNewValue()).isEqualTo("My title");
		assertThat(ev.getOldValue()).isNull();
	}

	@Test
	@DisplayName("Given issue not in org, when updateStatus, then throws ResourceNotFoundException")
	void given_issueMissingInOrg_whenUpdateStatus_thenThrowsResourceNotFoundException() {
		when(issueRepository.findByIdAndOrganization_Id(ISSUE_ID, ORG_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> issueService.updateStatus(ISSUE_ID, ORG_ID, ACTOR_ID, IssueStatus.CLOSED))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Issue");

		verify(userRepository, never()).findByOrganization_IdAndId(any(), any());
	}

	@Test
	@DisplayName("Given actor missing in org, when updateStatus, then throws ResourceNotFoundException")
	void given_actorMissing_whenUpdateStatus_thenThrowsResourceNotFoundException() {
		Issue issue = new Issue(organization, "t", null, IssueStatus.OPEN, IssuePriority.LOW, null, null, null);
		ReflectionTestUtils.setField(issue, "id", ISSUE_ID);
		when(issueRepository.findByIdAndOrganization_Id(ISSUE_ID, ORG_ID)).thenReturn(Optional.of(issue));
		when(userRepository.findByOrganization_IdAndId(ORG_ID, ACTOR_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> issueService.updateStatus(ISSUE_ID, ORG_ID, ACTOR_ID, IssueStatus.IN_PROGRESS))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Actor");
	}

	@Test
	@DisplayName("Given same status as current, when updateStatus, then no persist and no history")
	void given_sameStatusRequested_whenUpdateStatus_thenNoPersistOrHistory() {
		Issue issue = new Issue(organization, "t", null, IssueStatus.OPEN, IssuePriority.LOW, null, null, null);
		ReflectionTestUtils.setField(issue, "id", ISSUE_ID);
		when(issueRepository.findByIdAndOrganization_Id(ISSUE_ID, ORG_ID)).thenReturn(Optional.of(issue));
		when(userRepository.findByOrganization_IdAndId(ORG_ID, ACTOR_ID)).thenReturn(Optional.of(actor));

		Issue out = issueService.updateStatus(ISSUE_ID, ORG_ID, ACTOR_ID, IssueStatus.OPEN);

		assertThat(out).isSameAs(issue);
		verify(issueRepository, never()).save(any());
		verify(issueChangeEventRepository, never()).save(any());
	}

	@Test
	@DisplayName("Given status changes, when updateStatus, then persists and emits STATUS_CHANGED")
	void given_statusChanges_whenUpdateStatus_thenEmitsStatusChangedEvent() {
		Issue issue = new Issue(organization, "t", null, IssueStatus.OPEN, IssuePriority.LOW, null, null, null);
		ReflectionTestUtils.setField(issue, "id", ISSUE_ID);
		when(issueRepository.findByIdAndOrganization_Id(ISSUE_ID, ORG_ID)).thenReturn(Optional.of(issue));
		when(userRepository.findByOrganization_IdAndId(ORG_ID, ACTOR_ID)).thenReturn(Optional.of(actor));
		when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));

		issueService.updateStatus(ISSUE_ID, ORG_ID, ACTOR_ID, IssueStatus.IN_PROGRESS);

		verify(issueRepository).save(issue);
		ArgumentCaptor<IssueChangeEvent> cap = ArgumentCaptor.forClass(IssueChangeEvent.class);
		verify(issueChangeEventRepository).save(cap.capture());
		assertThat(cap.getValue().getChangeType()).isEqualTo(IssueChangeType.STATUS_CHANGED);
		assertThat(cap.getValue().getOldValue()).isEqualTo(IssueStatus.OPEN.name());
		assertThat(cap.getValue().getNewValue()).isEqualTo(IssueStatus.IN_PROGRESS.name());
	}

	@Test
	@DisplayName("Given new assignee unknown in org, when updateAssignee, then throws ResourceNotFoundException")
	void given_newAssigneeUnknown_whenUpdateAssignee_thenThrowsResourceNotFoundException() {
		Issue issue = new Issue(organization, "t", null, IssueStatus.OPEN, IssuePriority.LOW, null, null, null);
		ReflectionTestUtils.setField(issue, "id", ISSUE_ID);
		when(issueRepository.findByIdAndOrganization_Id(ISSUE_ID, ORG_ID)).thenReturn(Optional.of(issue));
		when(userRepository.findByOrganization_IdAndId(ORG_ID, ACTOR_ID)).thenReturn(Optional.of(actor));
		when(userRepository.findByOrganization_IdAndId(ORG_ID, ASSIGNEE_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> issueService.updateAssignee(ISSUE_ID, ORG_ID, ACTOR_ID, ASSIGNEE_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Assignee");
	}

	@Test
	@DisplayName("Given assignee changes, when updateAssignee, then emits ASSIGNEE_CHANGED with uuids")
	void given_assigneeChanges_whenUpdateAssignee_thenEmitsAssigneeChangedEvent() {
		Issue issue = new Issue(organization, "t", null, IssueStatus.OPEN, IssuePriority.LOW, assignee, null, null);
		ReflectionTestUtils.setField(issue, "id", ISSUE_ID);
		User newAssignee = new User(organization, "new@test", "h", UserRole.TECHNICIAN);
		UUID newId = UUID.fromString("50000000-0000-0000-0000-000000000005");
		ReflectionTestUtils.setField(newAssignee, "id", newId);

		when(issueRepository.findByIdAndOrganization_Id(ISSUE_ID, ORG_ID)).thenReturn(Optional.of(issue));
		when(userRepository.findByOrganization_IdAndId(ORG_ID, ACTOR_ID)).thenReturn(Optional.of(actor));
		when(userRepository.findByOrganization_IdAndId(ORG_ID, newId)).thenReturn(Optional.of(newAssignee));
		when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));

		issueService.updateAssignee(ISSUE_ID, ORG_ID, ACTOR_ID, newId);

		ArgumentCaptor<IssueChangeEvent> cap = ArgumentCaptor.forClass(IssueChangeEvent.class);
		verify(issueChangeEventRepository).save(cap.capture());
		assertThat(cap.getValue().getChangeType()).isEqualTo(IssueChangeType.ASSIGNEE_CHANGED);
		assertThat(cap.getValue().getOldValue()).isEqualTo(ASSIGNEE_ID.toString());
		assertThat(cap.getValue().getNewValue()).isEqualTo(newId.toString());
	}

	@Test
	@DisplayName("Given priority changes, when updatePriority, then emits PRIORITY_CHANGED")
	void given_priorityChanges_whenUpdatePriority_thenEmitsPriorityChangedEvent() {
		Issue issue = new Issue(organization, "t", null, IssueStatus.OPEN, IssuePriority.LOW, null, null, null);
		ReflectionTestUtils.setField(issue, "id", ISSUE_ID);
		when(issueRepository.findByIdAndOrganization_Id(ISSUE_ID, ORG_ID)).thenReturn(Optional.of(issue));
		when(userRepository.findByOrganization_IdAndId(ORG_ID, ACTOR_ID)).thenReturn(Optional.of(actor));
		when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));

		issueService.updatePriority(ISSUE_ID, ORG_ID, ACTOR_ID, IssuePriority.HIGH);

		ArgumentCaptor<IssueChangeEvent> cap = ArgumentCaptor.forClass(IssueChangeEvent.class);
		verify(issueChangeEventRepository).save(cap.capture());
		assertThat(cap.getValue().getChangeType()).isEqualTo(IssueChangeType.PRIORITY_CHANGED);
		assertThat(cap.getValue().getOldValue()).isEqualTo(IssuePriority.LOW.name());
		assertThat(cap.getValue().getNewValue()).isEqualTo(IssuePriority.HIGH.name());
	}

	@Test
	@DisplayName("Given title changes, when updateTitle, then single TITLE_CHANGED event")
	void given_titleChanges_whenUpdateTitle_thenSingleTitleChangedEvent() {
		Issue issue = new Issue(organization, "old", null, IssueStatus.OPEN, IssuePriority.LOW, null, null, null);
		ReflectionTestUtils.setField(issue, "id", ISSUE_ID);
		when(issueRepository.findByIdAndOrganization_Id(ISSUE_ID, ORG_ID)).thenReturn(Optional.of(issue));
		when(userRepository.findByOrganization_IdAndId(ORG_ID, ACTOR_ID)).thenReturn(Optional.of(actor));
		when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));

		issueService.updateTitle(ISSUE_ID, ORG_ID, ACTOR_ID, "new");

		verify(issueChangeEventRepository, times(1)).save(any());
		ArgumentCaptor<IssueChangeEvent> cap = ArgumentCaptor.forClass(IssueChangeEvent.class);
		verify(issueChangeEventRepository).save(cap.capture());
		assertThat(cap.getValue().getChangeType()).isEqualTo(IssueChangeType.TITLE_CHANGED);
	}

	@Test
	@DisplayName("Given customer label changes, when updateCustomerLabel, then CUSTOMER_LABEL_CHANGED event")
	void given_customerLabelChanges_whenUpdateCustomerLabel_thenCustomerLabelChangedEvent() {
		Issue issue = new Issue(organization, "t", null, IssueStatus.OPEN, IssuePriority.LOW, null, "old", null);
		ReflectionTestUtils.setField(issue, "id", ISSUE_ID);
		when(issueRepository.findByIdAndOrganization_Id(ISSUE_ID, ORG_ID)).thenReturn(Optional.of(issue));
		when(userRepository.findByOrganization_IdAndId(ORG_ID, ACTOR_ID)).thenReturn(Optional.of(actor));
		when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));

		issueService.updateCustomerLabel(ISSUE_ID, ORG_ID, ACTOR_ID, "new");

		ArgumentCaptor<IssueChangeEvent> cap = ArgumentCaptor.forClass(IssueChangeEvent.class);
		verify(issueChangeEventRepository).save(cap.capture());
		assertThat(cap.getValue().getChangeType()).isEqualTo(IssueChangeType.CUSTOMER_LABEL_CHANGED);
		assertThat(cap.getValue().getOldValue()).isEqualTo("old");
		assertThat(cap.getValue().getNewValue()).isEqualTo("new");
	}

	@Test
	@DisplayName("Given site label changes, when updateSiteLabel, then SITE_LABEL_CHANGED event")
	void given_siteLabelChanges_whenUpdateSiteLabel_thenSiteLabelChangedEvent() {
		Issue issue = new Issue(organization, "t", null, IssueStatus.OPEN, IssuePriority.LOW, null, null, "old");
		ReflectionTestUtils.setField(issue, "id", ISSUE_ID);
		when(issueRepository.findByIdAndOrganization_Id(ISSUE_ID, ORG_ID)).thenReturn(Optional.of(issue));
		when(userRepository.findByOrganization_IdAndId(ORG_ID, ACTOR_ID)).thenReturn(Optional.of(actor));
		when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));

		issueService.updateSiteLabel(ISSUE_ID, ORG_ID, ACTOR_ID, "new");

		ArgumentCaptor<IssueChangeEvent> cap = ArgumentCaptor.forClass(IssueChangeEvent.class);
		verify(issueChangeEventRepository).save(cap.capture());
		assertThat(cap.getValue().getChangeType()).isEqualTo(IssueChangeType.SITE_LABEL_CHANGED);
		assertThat(cap.getValue().getOldValue()).isEqualTo("old");
		assertThat(cap.getValue().getNewValue()).isEqualTo("new");
	}
}
