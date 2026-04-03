package com.mowercare.issue;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;

/**
 * Builds {@link Sort} for issue lists. Priority uses explicit severity order (not lexicographic string sort).
 */
public final class IssueSorts {

	private IssueSorts() {}

	public static Sort build(IssueListSortField field, boolean desc) {
		Sort tieBreak = Sort.by(Sort.Direction.DESC, "id");
		return switch (field) {
			case UPDATED_AT -> Sort.by(direction(desc), "updatedAt").and(tieBreak);
			case CREATED_AT -> Sort.by(direction(desc), "createdAt").and(tieBreak);
			case PRIORITY -> prioritySort(desc).and(tieBreak);
		};
	}

	private static Sort.Direction direction(boolean desc) {
		return desc ? Sort.Direction.DESC : Sort.Direction.ASC;
	}

	/**
	 * Maps priority to 0..3 for LOW..URGENT; {@code desc} puts URGENT first.
	 */
	private static Sort prioritySort(boolean desc) {
		Sort.Direction d = direction(desc);
		return JpaSort.unsafe(
				d,
				"CASE priority WHEN 'LOW' THEN 0 WHEN 'MEDIUM' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'URGENT' THEN 3 ELSE -1 END");
	}
}
