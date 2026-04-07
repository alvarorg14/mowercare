package com.mowercare.notification;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Pure FR21 routing: eligible assignee (when ACTIVE and not actor) plus all ACTIVE admins, then remove
 * actor from the combined set (dedupes admin+assignee). Used for {@code issue.created}, {@code issue.assigned},
 * and {@code issue.status_changed} per Story 4.2 matrix.
 */
public final class NotificationRecipientRules {

	private NotificationRecipientRules() {}

	/**
	 * @param eligibleAssigneeIdOrNull assignee user id only if account is ACTIVE; null for unassigned or inactive assignee
	 * @param activeAdminUserIds ids of ACTIVE ADMIN users in the organization (order preserved in output where relevant)
	 */
	public static LinkedHashSet<UUID> resolve(
			UUID actorUserId, UUID eligibleAssigneeIdOrNull, List<UUID> activeAdminUserIds) {
		LinkedHashSet<UUID> out = new LinkedHashSet<>(activeAdminUserIds);
		if (eligibleAssigneeIdOrNull != null && !eligibleAssigneeIdOrNull.equals(actorUserId)) {
			out.add(eligibleAssigneeIdOrNull);
		}
		out.remove(actorUserId);
		return out;
	}
}
