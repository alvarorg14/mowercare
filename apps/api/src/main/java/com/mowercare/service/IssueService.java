package com.mowercare.service;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mowercare.exception.IssueClosedException;
import com.mowercare.exception.ResourceNotFoundException;
import com.mowercare.issue.IssueStatusTransitionValidator;
import com.mowercare.model.request.IssuePatch;
import com.mowercare.model.Issue;
import com.mowercare.model.IssueChangeEvent;
import com.mowercare.model.IssueChangeType;
import com.mowercare.model.IssueListScope;
import com.mowercare.model.IssuePriority;
import com.mowercare.model.IssueStatus;
import com.mowercare.model.Organization;
import com.mowercare.model.User;
import com.mowercare.model.response.IssueChangeEventItemResponse;
import com.mowercare.model.response.IssueChangeEventsResponse;
import com.mowercare.model.response.IssueDetailResponse;
import com.mowercare.model.response.IssueListItemResponse;
import com.mowercare.model.response.IssueListResponse;
import com.mowercare.repository.IssueChangeEventRepository;
import com.mowercare.repository.IssueRepository;
import com.mowercare.repository.OrganizationRepository;
import com.mowercare.repository.UserRepository;

@Service
public class IssueService {

	private static final int LIST_MAX = 200;
	private static final int CHANGE_EVENTS_MAX_PAGE = 100;

	private static final Pageable LIST_PAGE =
			PageRequest.of(0, LIST_MAX, Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id")));

	private static final Set<IssueStatus> OPEN_STATUSES =
			Set.of(IssueStatus.OPEN, IssueStatus.IN_PROGRESS, IssueStatus.WAITING);

	private final IssueRepository issueRepository;
	private final IssueChangeEventRepository issueChangeEventRepository;
	private final OrganizationRepository organizationRepository;
	private final UserRepository userRepository;

	public IssueService(
			IssueRepository issueRepository,
			IssueChangeEventRepository issueChangeEventRepository,
			OrganizationRepository organizationRepository,
			UserRepository userRepository) {
		this.issueRepository = issueRepository;
		this.issueChangeEventRepository = issueChangeEventRepository;
		this.organizationRepository = organizationRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public IssueDetailResponse getIssue(UUID organizationId, UUID issueId) {
		Issue issue = loadIssue(issueId, organizationId);
		return toDetailResponse(issue);
	}

	/**
	 * Paginated append-only history for an issue. Sort is always {@code occurredAt} ascending (timeline order).
	 */
	@Transactional(readOnly = true)
	public IssueChangeEventsResponse listChangeEvents(UUID organizationId, UUID issueId, Pageable pageable) {
		loadIssue(issueId, organizationId);
		int size = Math.min(Math.max(1, pageable.getPageSize()), CHANGE_EVENTS_MAX_PAGE);
		int page = Math.max(0, pageable.getPageNumber());
		Pageable sorted = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "occurredAt"));
		Page<IssueChangeEvent> result =
				issueChangeEventRepository.findByIssue_IdAndOrganization_Id(issueId, organizationId, sorted);
		return new IssueChangeEventsResponse(
				result.getContent().stream()
						.map(ev -> toChangeEventItem(organizationId, ev))
						.toList(),
				result.getTotalElements(),
				result.getTotalPages(),
				result.getNumber(),
				result.getSize());
	}

	@Transactional(readOnly = true)
	public IssueListResponse listIssues(UUID organizationId, UUID actorUserId, IssueListScope scope) {
		Page<Issue> page =
				switch (scope) {
					case OPEN -> issueRepository.findByOrganization_IdAndStatusIn(organizationId, OPEN_STATUSES, LIST_PAGE);
					case ALL -> issueRepository.findByOrganization_Id(organizationId, LIST_PAGE);
					case MINE -> issueRepository.findByOrganization_IdAndAssignee_Id(organizationId, actorUserId, LIST_PAGE);
				};
		return new IssueListResponse(page.getContent().stream().map(this::toListItem).toList());
	}

	@Transactional
	public Issue createIssue(
			UUID organizationId,
			UUID actorUserId,
			String title,
			String description,
			IssueStatus status,
			IssuePriority priority,
			UUID assigneeUserId,
			String customerLabel,
			String siteLabel) {
		Objects.requireNonNull(title, "title");
		Objects.requireNonNull(status, "status");
		Objects.requireNonNull(priority, "priority");
		Organization organization =
				organizationRepository.findById(organizationId).orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
		User actor = userRepository
				.findByOrganization_IdAndId(organizationId, actorUserId)
				.orElseThrow(() -> new ResourceNotFoundException("Actor not found"));
		User assignee = resolveAssigneeOrNull(organizationId, assigneeUserId);

		Issue issue = new Issue(organization, title, description, status, priority, assignee, customerLabel, siteLabel);
		Issue saved = issueRepository.save(issue);
		appendEvent(saved, actor, IssueChangeType.CREATED, null, saved.getTitle());
		return saved;
	}

	@Transactional
	public IssueDetailResponse patchIssue(UUID organizationId, UUID issueId, UUID actorUserId, IssuePatch patch) {
		Issue issue = loadIssue(issueId, organizationId);
		if (issue.getStatus() == IssueStatus.CLOSED) {
			if (patch.statusPresent() && patch.status() != issue.getStatus()) {
				IssueStatusTransitionValidator.validate(issue.getStatus(), patch.status());
			}
			if (patch.wouldChange(issue)) {
				throw new IssueClosedException();
			}
			return toDetailResponse(issue);
		}
		if (patch.titlePresent()) {
			updateTitle(issueId, organizationId, actorUserId, patch.title());
		}
		if (patch.descriptionPresent()) {
			String d = patch.description();
			String normalized = d == null || d.isBlank() ? null : d.trim();
			updateDescription(issueId, organizationId, actorUserId, normalized);
		}
		if (patch.priorityPresent()) {
			updatePriority(issueId, organizationId, actorUserId, patch.priority());
		}
		if (patch.assigneeUserIdPresent()) {
			updateAssignee(issueId, organizationId, actorUserId, patch.assigneeUserId());
		}
		if (patch.customerLabelPresent()) {
			String c = patch.customerLabel();
			String normalized = c == null || c.isBlank() ? null : c.trim();
			updateCustomerLabel(issueId, organizationId, actorUserId, normalized);
		}
		if (patch.siteLabelPresent()) {
			String s = patch.siteLabel();
			String normalized = s == null || s.isBlank() ? null : s.trim();
			updateSiteLabel(issueId, organizationId, actorUserId, normalized);
		}
		if (patch.statusPresent()) {
			updateStatus(issueId, organizationId, actorUserId, patch.status());
		}
		return getIssue(organizationId, issueId);
	}

	@Transactional
	public Issue updateStatus(UUID issueId, UUID organizationId, UUID actorUserId, IssueStatus newStatus) {
		Issue issue = loadIssue(issueId, organizationId);
		IssueStatusTransitionValidator.validate(issue.getStatus(), newStatus);
		User actor = loadActor(organizationId, actorUserId);
		IssueStatus old = issue.getStatus();
		if (old == newStatus) {
			return issue;
		}
		issue.setStatus(newStatus);
		Issue saved = issueRepository.save(issue);
		appendEvent(saved, actor, IssueChangeType.STATUS_CHANGED, old.name(), newStatus.name());
		return saved;
	}

	@Transactional
	public Issue updateAssignee(UUID issueId, UUID organizationId, UUID actorUserId, UUID newAssigneeUserId) {
		Issue issue = loadIssue(issueId, organizationId);
		User actor = loadActor(organizationId, actorUserId);
		UUID oldId = issue.getAssignee() != null ? issue.getAssignee().getId() : null;
		User newAssignee = resolveAssigneeOrNull(organizationId, newAssigneeUserId);
		if (Objects.equals(oldId, newAssigneeUserId)) {
			return issue;
		}
		issue.setAssignee(newAssignee);
		Issue saved = issueRepository.save(issue);
		appendEvent(
				saved,
				actor,
				IssueChangeType.ASSIGNEE_CHANGED,
				oldId == null ? null : oldId.toString(),
				newAssigneeUserId == null ? null : newAssigneeUserId.toString());
		return saved;
	}

	@Transactional
	public Issue updatePriority(UUID issueId, UUID organizationId, UUID actorUserId, IssuePriority newPriority) {
		Issue issue = loadIssue(issueId, organizationId);
		User actor = loadActor(organizationId, actorUserId);
		IssuePriority old = issue.getPriority();
		if (old == newPriority) {
			return issue;
		}
		issue.setPriority(newPriority);
		Issue saved = issueRepository.save(issue);
		appendEvent(saved, actor, IssueChangeType.PRIORITY_CHANGED, old.name(), newPriority.name());
		return saved;
	}

	@Transactional
	public Issue updateTitle(UUID issueId, UUID organizationId, UUID actorUserId, String newTitle) {
		Issue issue = loadIssue(issueId, organizationId);
		User actor = loadActor(organizationId, actorUserId);
		String old = issue.getTitle();
		if (Objects.equals(old, newTitle)) {
			return issue;
		}
		issue.setTitle(newTitle);
		Issue saved = issueRepository.save(issue);
		appendEvent(saved, actor, IssueChangeType.TITLE_CHANGED, old, newTitle);
		return saved;
	}

	@Transactional
	public Issue updateDescription(UUID issueId, UUID organizationId, UUID actorUserId, String newDescription) {
		Issue issue = loadIssue(issueId, organizationId);
		User actor = loadActor(organizationId, actorUserId);
		String old = issue.getDescription();
		if (Objects.equals(old, newDescription)) {
			return issue;
		}
		issue.setDescription(newDescription);
		Issue saved = issueRepository.save(issue);
		appendEvent(saved, actor, IssueChangeType.DESCRIPTION_CHANGED, old, newDescription);
		return saved;
	}

	@Transactional
	public Issue updateCustomerLabel(UUID issueId, UUID organizationId, UUID actorUserId, String newLabel) {
		Issue issue = loadIssue(issueId, organizationId);
		User actor = loadActor(organizationId, actorUserId);
		String old = issue.getCustomerLabel();
		if (Objects.equals(old, newLabel)) {
			return issue;
		}
		issue.setCustomerLabel(newLabel);
		Issue saved = issueRepository.save(issue);
		appendEvent(saved, actor, IssueChangeType.CUSTOMER_LABEL_CHANGED, old, newLabel);
		return saved;
	}

	@Transactional
	public Issue updateSiteLabel(UUID issueId, UUID organizationId, UUID actorUserId, String newLabel) {
		Issue issue = loadIssue(issueId, organizationId);
		User actor = loadActor(organizationId, actorUserId);
		String old = issue.getSiteLabel();
		if (Objects.equals(old, newLabel)) {
			return issue;
		}
		issue.setSiteLabel(newLabel);
		Issue saved = issueRepository.save(issue);
		appendEvent(saved, actor, IssueChangeType.SITE_LABEL_CHANGED, old, newLabel);
		return saved;
	}

	private Issue loadIssue(UUID issueId, UUID organizationId) {
		return issueRepository
				.findByIdAndOrganization_Id(issueId, organizationId)
				.orElseThrow(() -> new ResourceNotFoundException("Issue not found"));
	}

	private User loadActor(UUID organizationId, UUID actorUserId) {
		return userRepository
				.findByOrganization_IdAndId(organizationId, actorUserId)
				.orElseThrow(() -> new ResourceNotFoundException("Actor not found"));
	}

	private User resolveAssigneeOrNull(UUID organizationId, UUID assigneeUserId) {
		if (assigneeUserId == null) {
			return null;
		}
		return userRepository
				.findByOrganization_IdAndId(organizationId, assigneeUserId)
				.orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
	}

	private void appendEvent(Issue issue, User actor, IssueChangeType type, String oldValue, String newValue) {
		IssueChangeEvent event = new IssueChangeEvent(
				issue, issue.getOrganization(), actor, Instant.now(), type, oldValue, newValue);
		issueChangeEventRepository.save(event);
	}

	private IssueDetailResponse toDetailResponse(Issue issue) {
		IssueListItemResponse row = toListItem(issue);
		return new IssueDetailResponse(
				row.id(),
				row.title(),
				row.status(),
				row.priority(),
				issue.getDescription(),
				row.customerLabel(),
				row.siteLabel(),
				row.assigneeUserId(),
				row.assigneeLabel(),
				row.createdAt(),
				row.updatedAt());
	}

	private IssueListItemResponse toListItem(Issue issue) {
		User assignee = issue.getAssignee();
		UUID assigneeId = assignee != null ? assignee.getId() : null;
		String assigneeLabel = assignee != null ? assignee.getEmail() : null;
		return new IssueListItemResponse(
				issue.getId(),
				issue.getTitle(),
				issue.getStatus(),
				issue.getPriority(),
				issue.getCustomerLabel(),
				issue.getSiteLabel(),
				assigneeId,
				assigneeLabel,
				issue.getCreatedAt(),
				issue.getUpdatedAt());
	}

	private IssueChangeEventItemResponse toChangeEventItem(UUID organizationId, IssueChangeEvent ev) {
		User actor = ev.getActor();
		IssueChangeType type = ev.getChangeType();
		String oldAssigneeLabel = null;
		String newAssigneeLabel = null;
		if (type == IssueChangeType.ASSIGNEE_CHANGED) {
			oldAssigneeLabel = resolveAssigneeSnapshotLabel(organizationId, ev.getOldValue());
			newAssigneeLabel = resolveAssigneeSnapshotLabel(organizationId, ev.getNewValue());
		}
		return new IssueChangeEventItemResponse(
				ev.getId(),
				ev.getOccurredAt(),
				type,
				actor.getId(),
				actor.getEmail(),
				ev.getOldValue(),
				ev.getNewValue(),
				oldAssigneeLabel,
				newAssigneeLabel);
	}

	/**
	 * History stores assignee ids as strings; user may no longer exist in org — show email when present, else
	 * {@code Former user}. Values that are not valid UUIDs use {@code Unknown}.
	 */
	private String resolveAssigneeSnapshotLabel(UUID organizationId, String uuidOrNull) {
		if (uuidOrNull == null || uuidOrNull.isBlank()) {
			return null;
		}
		try {
			UUID userId = UUID.fromString(uuidOrNull.trim());
			return userRepository
					.findByOrganization_IdAndId(organizationId, userId)
					.map(User::getEmail)
					.orElse("Former user");
		} catch (IllegalArgumentException e) {
			return "Unknown";
		}
	}
}
