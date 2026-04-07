package com.mowercare.issue;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.mowercare.issue.Issue;
import com.mowercare.issue.IssueListScope;
import com.mowercare.issue.IssuePriority;
import com.mowercare.issue.IssueStatus;

public final class IssueSpecifications {

	private IssueSpecifications() {}

	public static Specification<Issue> build(
			UUID organizationId,
			UUID actorUserId,
			IssueListScope scope,
			Set<IssueStatus> openStatuses,
			List<IssueStatus> statusFilter,
			List<IssuePriority> priorityFilter) {

		Specification<Issue> spec =
				(root, query, cb) -> cb.equal(root.get("organization").get("id"), organizationId);
		spec = spec.and(scopePredicate(scope, actorUserId, openStatuses));
		if (!statusFilter.isEmpty()) {
			spec = spec.and((root, query, cb) -> root.get("status").in(statusFilter));
		}
		if (!priorityFilter.isEmpty()) {
			spec = spec.and((root, query, cb) -> root.get("priority").in(priorityFilter));
		}
		return spec;
	}

	private static Specification<Issue> scopePredicate(
			IssueListScope scope, UUID actorUserId, Set<IssueStatus> openStatuses) {
		return (root, query, cb) -> {
			return switch (scope) {
				case OPEN -> root.get("status").in(openStatuses);
				case ALL -> cb.conjunction();
				case MINE -> cb.and(
						cb.isNotNull(root.get("assignee")), cb.equal(root.get("assignee").get("id"), actorUserId));
			};
		};
	}
}
