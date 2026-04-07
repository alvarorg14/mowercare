package com.mowercare.notification.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
		description =
				"Paginated notifications for the current user (newest first by recipient createdAt). "
						+ "Query page and size (default size 50, max 100).")
public record NotificationListResponse(
		@Schema(description = "Notification rows for this page") List<NotificationItemResponse> items,
		@Schema(description = "Total elements across all pages") long totalElements,
		@Schema(description = "Total pages") int totalPages,
		@Schema(description = "Current page index (0-based)") int number,
		@Schema(description = "Page size") int size) {}
