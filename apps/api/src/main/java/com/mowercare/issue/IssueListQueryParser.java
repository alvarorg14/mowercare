package com.mowercare.issue;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses optional issue list query parameters. {@code status} and {@code priority} use repeated
 * params with enum names matching JSON ({@code OPEN}, {@code LOW}, …). Blank tokens are skipped.
 */
public final class IssueListQueryParser {

	private IssueListQueryParser() {}

	public record IssueListFilters(
			List<IssueStatus> statuses,
			List<IssuePriority> priorities,
			IssueListSortField sortField,
			boolean sortDesc) {}

	public static IssueListFilters parse(
			List<String> statusParams, List<String> priorityParams, String sortRaw, String directionRaw) {
		return new IssueListFilters(
				parseStatuses(statusParams), parsePriorities(priorityParams), parseSort(sortRaw), parseDirection(directionRaw));
	}

	static List<IssueStatus> parseStatuses(List<String> raw) {
		if (raw == null || raw.isEmpty()) {
			return List.of();
		}
		List<IssueStatus> out = new ArrayList<>();
		for (String s : raw) {
			if (s == null || s.isBlank()) {
				continue;
			}
			try {
				out.add(IssueStatus.valueOf(s.trim()));
			} catch (IllegalArgumentException e) {
				throw new InvalidIssueListQueryException("Invalid status: " + s.trim());
			}
		}
		return List.copyOf(out);
	}

	static List<IssuePriority> parsePriorities(List<String> raw) {
		if (raw == null || raw.isEmpty()) {
			return List.of();
		}
		List<IssuePriority> out = new ArrayList<>();
		for (String s : raw) {
			if (s == null || s.isBlank()) {
				continue;
			}
			try {
				out.add(IssuePriority.valueOf(s.trim()));
			} catch (IllegalArgumentException e) {
				throw new InvalidIssueListQueryException("Invalid priority: " + s.trim());
			}
		}
		return List.copyOf(out);
	}

	static IssueListSortField parseSort(String raw) {
		if (raw == null || raw.isBlank()) {
			return IssueListSortField.UPDATED_AT;
		}
		String t = raw.trim();
		for (IssueListSortField f : IssueListSortField.values()) {
			if (f.queryValue().equals(t)) {
				return f;
			}
		}
		throw new InvalidIssueListQueryException("Invalid sort: " + t + ". Use updatedAt, createdAt, or priority.");
	}

	/** Default {@code desc} when omitted (recency and priority severity). */
	static boolean parseDirection(String raw) {
		if (raw == null || raw.isBlank()) {
			return true;
		}
		return switch (raw.trim().toLowerCase()) {
			case "asc" -> false;
			case "desc" -> true;
			default -> throw new InvalidIssueListQueryException("Invalid direction: " + raw.trim() + ". Use asc or desc.");
		};
	}
}
