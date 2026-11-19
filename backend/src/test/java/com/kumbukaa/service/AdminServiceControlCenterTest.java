package com.kumbukaa.service;

import com.kumbukaa.dto.AnnouncementRequest;
import com.kumbukaa.entity.AdminAnnouncement;
import com.kumbukaa.entity.User;
import com.kumbukaa.repository.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminServiceControlCenterTest {
    private AdminService service(UserRepository users, AdminAuditEventRepository audit,
                                 AdminAnnouncementRepository announcements, AppNotificationRepository notifications) {
        return new AdminService(users, mock(LoanLentRepository.class), mock(LoanBorrowedRepository.class),
                audit, mock(AdminUserNoteRepository.class), announcements, notifications, mock(EmailService.class),
                mock(PasswordResetOtpRepository.class), mock(PasswordResetTokenRepository.class));
    }

    @Test
    void preventsAdminFromSuspendingOrDemotingSelf() {
        User actor = User.builder().id(1L).fullName("Admin").roles("ROLE_ADMIN").build();
        UserRepository users = mock(UserRepository.class);
        when(users.findById(1L)).thenReturn(Optional.of(actor));
        AdminService service = service(users, mock(AdminAuditEventRepository.class),
                mock(AdminAnnouncementRepository.class), mock(AppNotificationRepository.class));

        assertThrows(IllegalStateException.class, () -> service.setAccountStatus(1L, "SUSPENDED", actor));
        assertThrows(IllegalStateException.class, () -> service.changeRole(1L, "USER", actor));
        verify(users, never()).save(any());
    }

    @Test
    void preventsDemotingOrDeletingLastAdministratorWithoutAudit() {
        User actor = User.builder().id(1L).fullName("Admin").roles("ROLE_ADMIN").build();
        UserRepository users = mock(UserRepository.class);
        User finalAdmin = User.builder().id(2L).fullName("Second Admin").roles("ROLE_ADMIN").build();
        when(users.findById(2L)).thenReturn(Optional.of(finalAdmin));
        when(users.findAll()).thenReturn(java.util.List.of(actor));
        AdminAuditEventRepository audit = mock(AdminAuditEventRepository.class);
        AdminService service = service(users, audit, mock(AdminAnnouncementRepository.class),
                mock(AppNotificationRepository.class));

        assertThrows(IllegalStateException.class, () -> service.changeRole(2L, "USER", actor));
        assertThrows(IllegalStateException.class, () -> service.deleteUser(2L, actor));
        verify(users, never()).save(any());
        verify(users, never()).delete(any(User.class));
        verifyNoInteractions(audit);
    }

    @Test
    void savesRequestedTargetAnnouncementAsInAppNotification() {
        User actor = User.builder().id(1L).fullName("Admin").roles("ROLE_ADMIN").build();
        User recipient = User.builder().id(2L).email("target@example.com").fullName("Target").build();
        UserRepository users = mock(UserRepository.class);
        when(users.findById(2L)).thenReturn(Optional.of(recipient));
        AdminAnnouncementRepository announcements = mock(AdminAnnouncementRepository.class);
        when(announcements.save(any(AdminAnnouncement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AppNotificationRepository notifications = mock(AppNotificationRepository.class);
        AdminAuditEventRepository audit = mock(AdminAuditEventRepository.class);
        AdminService service = service(users, audit, announcements, notifications);

        var result = service.createAnnouncement(new AnnouncementRequest("Notice", "Hello", null, "IN_APP", 2L), actor);

        assertEquals("TARGET_USER", result.getAudience());
        assertEquals(1, result.getRecipientCount());
        verify(notifications).save(argThat(n -> n.getUserId().equals(2L) && n.getTitle().equals("Notice")));
        verify(audit).save(argThat(event -> "ANNOUNCEMENT".equals(event.getTargetType())));
    }

    @Test
    void persistsRoleAndSuspensionWithUserTimelineAuditType() {
        User actor = User.builder().id(1L).fullName("Admin").roles("ROLE_ADMIN").build();
        User target = User.builder().id(2L).fullName("Target").roles("ROLE_USER").build();
        UserRepository users = mock(UserRepository.class);
        when(users.findById(2L)).thenReturn(Optional.of(target));
        AdminAuditEventRepository audit = mock(AdminAuditEventRepository.class);
        AdminService service = service(users, audit, mock(AdminAnnouncementRepository.class),
                mock(AppNotificationRepository.class));

        service.changeRole(2L, "SUPPORT_AGENT", actor);
        service.setAccountStatus(2L, "SUSPENDED", actor);

        assertEquals("ROLE_SUPPORT_AGENT", target.getRoles());
        assertEquals("SUSPENDED", target.getAccountStatus());
        verify(audit, times(2)).save(argThat(event -> "USER".equals(event.getTargetType())));
    }

    @Test
    void targetTakesPrecedenceOverAudienceAndSmsIsRejected() {
        UserRepository users = mock(UserRepository.class);
        when(users.findById(2L)).thenReturn(Optional.of(User.builder().id(2L).email("target@example.com").build()));
        AdminAnnouncementRepository announcements = mock(AdminAnnouncementRepository.class);
        when(announcements.save(any(AdminAnnouncement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AppNotificationRepository notifications = mock(AppNotificationRepository.class);
        AdminService service = service(users, mock(AdminAuditEventRepository.class),
                announcements, notifications);
        User actor = User.builder().id(1L).fullName("Admin").build();

        var direct = service.createAnnouncement(
                new AnnouncementRequest("Title", "Message", "ALL_USERS", "IN_APP", 2L), actor);
        assertEquals("TARGET_USER", direct.getAudience());
        assertEquals(1, direct.getRecipientCount());
        assertThrows(IllegalArgumentException.class, () -> service.createAnnouncement(
                new AnnouncementRequest("Title", "Message", "ALL_USERS", "SMS", null), actor));
        verify(notifications).save(argThat(n -> n.getUserId().equals(2L)));
    }
}
