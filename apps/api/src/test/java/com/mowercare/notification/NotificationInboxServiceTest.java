package com.mowercare.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.mowercare.notification.NotificationRecipient;
import com.mowercare.notification.NotificationRecipientRepository;

@ExtendWith(MockitoExtension.class)
class NotificationInboxServiceTest {

	@Mock
	private NotificationRecipientRepository notificationRecipientRepository;

	@InjectMocks
	private NotificationInboxService notificationInboxService;

	private final UUID orgId = UUID.randomUUID();
	private final UUID userId = UUID.randomUUID();

	@Test
	@DisplayName("given oversized page when list then caps at 100")
	void givenOversizedPage_whenListForUser_thenCapsAt100() {
		Pageable huge = PageRequest.of(0, 500);
		when(notificationRecipientRepository.findByOrganization_IdAndRecipient_Id(eq(orgId), eq(userId), any()))
				.thenAnswer(inv -> Page.<NotificationRecipient>empty(inv.getArgument(2)));
		notificationInboxService.listForUser(orgId, userId, huge);
		ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
		verify(notificationRecipientRepository).findByOrganization_IdAndRecipient_Id(eq(orgId), eq(userId), cap.capture());
		assertThat(cap.getValue().getPageSize()).isEqualTo(100);
		assertThat(cap.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
	}

	@Test
	@DisplayName("given zero page size when list then uses minimum 1")
	void givenZeroPageSize_whenListForUser_thenUsesMinimum1() {
		Pageable bad = Mockito.mock(Pageable.class);
		when(bad.getPageNumber()).thenReturn(0);
		when(bad.getPageSize()).thenReturn(0);
		when(notificationRecipientRepository.findByOrganization_IdAndRecipient_Id(eq(orgId), eq(userId), any()))
				.thenReturn(new PageImpl<>(List.of()));
		notificationInboxService.listForUser(orgId, userId, bad);
		ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
		verify(notificationRecipientRepository).findByOrganization_IdAndRecipient_Id(eq(orgId), eq(userId), cap.capture());
		assertThat(cap.getValue().getPageSize()).isEqualTo(1);
	}

	@Test
	@DisplayName("given negative page index when list then clamps to page zero")
	void givenNegativePageIndex_whenListForUser_thenClampsToPageZero() {
		Pageable negativePage = Mockito.mock(Pageable.class);
		when(negativePage.getPageNumber()).thenReturn(-3);
		when(negativePage.getPageSize()).thenReturn(10);
		when(notificationRecipientRepository.findByOrganization_IdAndRecipient_Id(eq(orgId), eq(userId), any()))
				.thenAnswer(inv -> Page.<NotificationRecipient>empty(inv.getArgument(2)));
		notificationInboxService.listForUser(orgId, userId, negativePage);
		ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
		verify(notificationRecipientRepository).findByOrganization_IdAndRecipient_Id(eq(orgId), eq(userId), cap.capture());
		assertThat(cap.getValue().getPageNumber()).isZero();
	}
}
