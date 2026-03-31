package com.mowercare.model.request;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.mowercare.exception.EmptyIssuePatchException;
import com.mowercare.exception.InvalidIssuePatchException;
import com.mowercare.model.Issue;
import com.mowercare.model.IssuePriority;
import com.mowercare.model.IssueStatus;

/**
 * Parsed PATCH body: only known fields; used for partial updates.
 */
public record IssuePatch(
		boolean titlePresent,
		String title,
		boolean descriptionPresent,
		String description,
		boolean statusPresent,
		IssueStatus status,
		boolean priorityPresent,
		IssuePriority priority,
		boolean assigneeUserIdPresent,
		UUID assigneeUserId,
		boolean customerLabelPresent,
		String customerLabel,
		boolean siteLabelPresent,
		String siteLabel) {

	private static final Set<String> ALLOWED =
			Set.of("title", "description", "status", "priority", "assigneeUserId", "customerLabel", "siteLabel");

	public static IssuePatch from(JsonNode body) {
		if (body == null || body.isNull() || !body.isObject()) {
			throw new InvalidIssuePatchException("JSON object required");
		}
		if (body.size() == 0) {
			throw new EmptyIssuePatchException();
		}
		var it = body.fieldNames();
		while (it.hasNext()) {
			String k = it.next();
			if (!ALLOWED.contains(k)) {
				throw new InvalidIssuePatchException("Unknown field: " + k);
			}
		}

		boolean titlePresent = body.has("title");
		String title = null;
		if (titlePresent) {
			JsonNode n = body.get("title");
			if (n.isNull()) {
				throw new InvalidIssuePatchException("title cannot be null");
			}
			title = n.asText();
			if (title.isBlank()) {
				throw new InvalidIssuePatchException("title cannot be blank");
			}
		}

		boolean descriptionPresent = body.has("description");
		String description = null;
		if (descriptionPresent) {
			JsonNode n = body.get("description");
			description = n.isNull() ? null : n.asText();
		}

		boolean statusPresent = body.has("status");
		IssueStatus status = null;
		if (statusPresent) {
			JsonNode n = body.get("status");
			if (n.isNull()) {
				throw new InvalidIssuePatchException("status cannot be null");
			}
			try {
				status = IssueStatus.valueOf(n.asText());
			} catch (IllegalArgumentException ex) {
				throw new InvalidIssuePatchException("Invalid status value");
			}
		}

		boolean priorityPresent = body.has("priority");
		IssuePriority priority = null;
		if (priorityPresent) {
			JsonNode n = body.get("priority");
			if (n.isNull()) {
				throw new InvalidIssuePatchException("priority cannot be null");
			}
			try {
				priority = IssuePriority.valueOf(n.asText());
			} catch (IllegalArgumentException ex) {
				throw new InvalidIssuePatchException("Invalid priority value");
			}
		}

		boolean assigneeUserIdPresent = body.has("assigneeUserId");
		UUID assigneeUserId = null;
		if (assigneeUserIdPresent) {
			JsonNode n = body.get("assigneeUserId");
			if (n.isNull()) {
				assigneeUserId = null;
			} else {
				try {
					assigneeUserId = UUID.fromString(n.asText());
				} catch (IllegalArgumentException ex) {
					throw new InvalidIssuePatchException("assigneeUserId must be a UUID");
				}
			}
		}

		boolean customerLabelPresent = body.has("customerLabel");
		String customerLabel = null;
		if (customerLabelPresent) {
			JsonNode n = body.get("customerLabel");
			customerLabel = n.isNull() ? null : n.asText();
		}

		boolean siteLabelPresent = body.has("siteLabel");
		String siteLabel = null;
		if (siteLabelPresent) {
			JsonNode n = body.get("siteLabel");
			siteLabel = n.isNull() ? null : n.asText();
		}

		return new IssuePatch(
				titlePresent,
				title,
				descriptionPresent,
				description,
				statusPresent,
				status,
				priorityPresent,
				priority,
				assigneeUserIdPresent,
				assigneeUserId,
				customerLabelPresent,
				customerLabel,
				siteLabelPresent,
				siteLabel);
	}

	/** True if any present field differs from the current issue (for CLOSED immutability check). */
	public boolean wouldChange(Issue issue) {
		if (titlePresent && !Objects.equals(title, issue.getTitle())) {
			return true;
		}
		if (descriptionPresent) {
			String normalized = normalizeNullableText(description);
			if (!Objects.equals(normalized, issue.getDescription())) {
				return true;
			}
		}
		if (statusPresent && status != issue.getStatus()) {
			return true;
		}
		if (priorityPresent && priority != issue.getPriority()) {
			return true;
		}
		if (assigneeUserIdPresent) {
			UUID current = issue.getAssignee() != null ? issue.getAssignee().getId() : null;
			if (!Objects.equals(assigneeUserId, current)) {
				return true;
			}
		}
		if (customerLabelPresent) {
			String normalized = normalizeNullableText(customerLabel);
			if (!Objects.equals(normalized, issue.getCustomerLabel())) {
				return true;
			}
		}
		if (siteLabelPresent) {
			String normalized = normalizeNullableText(siteLabel);
			if (!Objects.equals(normalized, issue.getSiteLabel())) {
				return true;
			}
		}
		return false;
	}

	/** Same trimming / blank-to-null semantics as {@link com.mowercare.service.IssueService#patchIssue}. */
	private static String normalizeNullableText(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		return raw.trim();
	}
}
