package com.kumbukaa.service;

import com.kumbukaa.dto.LoanResponse;
import com.kumbukaa.dto.UserDetailDto;
import com.kumbukaa.dto.UserSummaryDto;
import com.kumbukaa.dto.*;
import com.kumbukaa.entity.*;
import com.kumbukaa.enums.PersonalLoanStatus;
import com.kumbukaa.repository.*;
import com.kumbukaa.entity.LoanBorrowed;
import com.kumbukaa.entity.LoanLent;
import com.kumbukaa.entity.User;
import com.kumbukaa.mapper.LoanResponseMapper;
import com.kumbukaa.util.UserRoleUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@SuppressWarnings("null")
public class AdminService {

    private final UserRepository userRepository;
    private final LoanLentRepository loanLentRepository;
    private final LoanBorrowedRepository loanBorrowedRepository;
    private final AdminAuditEventRepository auditRepository;
    private final AdminUserNoteRepository noteRepository;
    private final AdminAnnouncementRepository announcementRepository;
    private final AppNotificationRepository notificationRepository;
    private final EmailService emailService;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    public AdminService(UserRepository userRepository,
                        LoanLentRepository loanLentRepository,
                        LoanBorrowedRepository loanBorrowedRepository,
                        AdminAuditEventRepository auditRepository,
                        AdminUserNoteRepository noteRepository,
                        AdminAnnouncementRepository announcementRepository,
                        AppNotificationRepository notificationRepository,
                        EmailService emailService,
                        PasswordResetOtpRepository passwordResetOtpRepository,
                        PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.loanLentRepository = loanLentRepository;
        this.loanBorrowedRepository = loanBorrowedRepository;
        this.auditRepository = auditRepository;
        this.noteRepository = noteRepository;
        this.announcementRepository = announcementRepository;
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
        this.passwordResetOtpRepository = passwordResetOtpRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    public AdminService(UserRepository userRepository, LoanLentRepository loanLentRepository,
                        LoanBorrowedRepository loanBorrowedRepository) {
        this(userRepository, loanLentRepository, loanBorrowedRepository, null, null, null, null, null, null, null);
    }

    public List<UserSummaryDto> listAllUsersWithLoans() {
        List<User> users = userRepository.findAll();
        return users.stream().map(this::toUserWithLoansDto).collect(Collectors.toList());
    }

    public UserDetailDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toUserDetailDto(user);
    }

    private UserSummaryDto toUserWithLoansDto(User user) {
        long lentCount = loanLentRepository.findAllByUserId(user.getId()).size();
        long borrowedCount = loanBorrowedRepository.findAllByUserId(user.getId()).size();

        return UserSummaryDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .loansLent(lentCount)
                .loansBorrowed(borrowedCount)
                .admin(hasAdminRole(user))
                .role(accountRole(user))
                .status(normalizedStatus(user.getAccountStatus()))
                .verificationStatus(normalizedVerification(user.getVerificationStatus()))
                .createdAt(user.getCreatedAt())
                .lastSeenAt(user.getLastSeenAt())
                .lastDevice(user.getLastDevice())
                .build();
    }

    private UserDetailDto toUserDetailDto(User user) {
        List<LoanLent> loansLent = loanLentRepository.findAllByUserId(user.getId());
        List<LoanBorrowed> loansBorrowed = loanBorrowedRepository.findAllByUserId(user.getId());

        java.util.List<LoanResponse> lentResponses = loansLent.stream()
                .map(LoanResponseMapper::toResponse)
                .collect(Collectors.toList());
        java.util.List<LoanResponse> borrowedResponses = loansBorrowed.stream()
                .map(LoanResponseMapper::toResponse)
                .collect(Collectors.toList());

        return UserDetailDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .admin(hasAdminRole(user))
                .role(accountRole(user))
                .status(normalizedStatus(user.getAccountStatus()))
                .verificationStatus(normalizedVerification(user.getVerificationStatus()))
                .createdAt(user.getCreatedAt())
                .lastSeenAt(user.getLastSeenAt())
                .lastDevice(user.getLastDevice())
                .loansLent(lentResponses)
                .loansBorrowed(borrowedResponses)
                .build();
    }

    private boolean hasAdminRole(User user) {
        return UserRoleUtils.hasRole(user.getRoles(), "ADMIN");
    }

    public static String accountRole(User user) {
        if (hasRole(user, "ROLE_ADMIN")) return "ADMIN";
        if (hasRole(user, "ROLE_SUPPORT_AGENT")) return "SUPPORT_AGENT";
        return "USER";
    }

    private static boolean hasRole(User user, String role) {
        return UserRoleUtils.hasRole(user.getRoles(), role);
    }

    private String normalizedStatus(String status) { return "SUSPENDED".equals(status) ? status : "ACTIVE"; }
    private String normalizedVerification(String status) {
        return "VERIFIED".equals(status) || "REJECTED".equals(status) ? status : "PENDING";
    }

    private void audit(User actor, String action, Long targetId, String targetName, String details) {
        if (auditRepository == null) return;
        auditRepository.save(AdminAuditEvent.builder().actorId(actor == null ? null : actor.getId())
                .actorName(actor == null ? "System" : actor.getFullName()).action(action).targetType(auditTargetType(action))
                .targetId(targetId).targetName(targetName).details(details).build());
    }

    private String auditTargetType(String action) {
        if (action != null && action.contains("ANNOUNCEMENT")) return "ANNOUNCEMENT";
        if (action != null && (action.contains("REMINDER") || action.contains("LOAN"))) return "LOAN";
        if ("ADMIN_CREATED".equals(action)) return "ACCOUNT";
        return "USER";
    }

    public User createAdminUser(String fullName, String email, String phoneNumber, String password) {
        return createAdminUser(fullName, email, phoneNumber, password, authenticatedActor());
    }

    public User createAdminUser(String fullName, String email, String phoneNumber, String password, User actor) {
        if (userRepository.findByEmail(email.toLowerCase().trim()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }
        User user = User.builder()
                .fullName(fullName.trim())
                .email(email.toLowerCase().trim())
                .phoneNumber(phoneNumber.trim())
                .passwordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(password))
                .roles("ROLE_ADMIN")
                .mustChangePassword(true)
                .build();
        User created = userRepository.save(user);
        if (created == null) created = user;
        audit(actor, "ADMIN_CREATED", created.getId(), created.getFullName(), "Administrator account created");
        return created;
    }

    public void resetUserPassword(Long userId, String newPassword) {
        resetUserPassword(userId, newPassword, authenticatedActor());
    }

    public void resetUserPassword(Long userId, String newPassword, User actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(newPassword));
        userRepository.save(user);
        audit(actor, "USER_PASSWORD_RESET", user.getId(), user.getFullName(), "Password reset by administrator");
    }

    public void changeAdminPassword(Long adminId, String newPassword) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        admin.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(newPassword));
        admin.setMustChangePassword(false);
        userRepository.save(admin);
        audit(admin, "PASSWORD_CHANGED", admin.getId(), admin.getFullName(), "Own password changed");
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        ensureAdminRemainsIfRemoving(user);
        audit(null, "USER_DELETED", user.getId(), user.getFullName(), "Account deleted");
        removePasswordResetRecords(userId);
        userRepository.delete(user);
    }

    public void deleteUser(Long userId, User actor) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        ensureAdminRemainsIfRemoving(user);
        audit(actor, "USER_DELETED", user.getId(), user.getFullName(), "Account deleted");
        removePasswordResetRecords(userId);
        userRepository.delete(user);
    }

    private void removePasswordResetRecords(Long userId) {
        if (passwordResetOtpRepository != null) passwordResetOtpRepository.deleteAllByUser_Id(userId);
        if (passwordResetTokenRepository != null) passwordResetTokenRepository.deleteAllByUser_Id(userId);
    }

    public void changeRole(Long id, String role, User actor) {
        User target = requireUser(id);
        if (!List.of("USER", "ADMIN", "SUPPORT_AGENT").contains(role))
            throw new IllegalArgumentException("Role must be USER, ADMIN, or SUPPORT_AGENT");
        if (actor.getId().equals(id) && hasAdminRole(target) && !"ADMIN".equals(role))
            throw new IllegalStateException("You cannot remove your own administrator access");
        if (hasAdminRole(target) && !"ADMIN".equals(role)) ensureAdminRemainsIfRemoving(target);
        target.setRoles(switch (role) {
            case "ADMIN" -> "ROLE_ADMIN";
            case "SUPPORT_AGENT" -> "ROLE_SUPPORT_AGENT";
            default -> "ROLE_USER";
        });
        userRepository.save(target);
        audit(actor, "ROLE_CHANGED", id, target.getFullName(), role);
    }

    public void setAccountStatus(Long id, String status, User actor) {
        User target = requireUser(id);
        if (actor.getId().equals(id) && hasAdminRole(target) && "SUSPENDED".equals(status))
            throw new IllegalStateException("You cannot suspend your own administrator account");
        if (!List.of("ACTIVE", "SUSPENDED").contains(status))
            throw new IllegalArgumentException("Status must be ACTIVE or SUSPENDED");
        target.setAccountStatus(status);
        userRepository.save(target);
        audit(actor, "ACCOUNT_" + status, id, target.getFullName(), "Account status changed to " + status);
    }

    public void setVerificationStatus(Long id, String status, User actor) {
        User target = requireUser(id);
        if (!List.of("PENDING", "VERIFIED", "REJECTED").contains(status))
            throw new IllegalArgumentException("Verification status must be PENDING, VERIFIED, or REJECTED");
        target.setVerificationStatus(status);
        userRepository.save(target);
        audit(actor, "KYC_" + status, id, target.getFullName(), "Verification status changed to " + status);
    }

    private User requireUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private void ensureAdminRemainsIfRemoving(User target) {
        if (!hasAdminRole(target)) return;
        long administrators = userRepository.findAll().stream().filter(this::hasAdminRole).count();
        if (administrators <= 1) {
            throw new IllegalStateException("The last administrator account cannot be removed or demoted");
        }
    }

    private User authenticatedActor() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof User user ? user : null;
    }

    public AdminOverviewDto overview() {
        List<User> users = userRepository.findAll();
        List<LoanLent> lent = loanLentRepository.findAll();
        List<LoanBorrowed> borrowed = loanBorrowedRepository.findAll();
        LocalDate today = LocalDate.now();
        List<AdminOverviewDto.MonthlyVolume> months = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            double volume = lent.stream().filter(l -> l.getDateLent() != null && YearMonth.from(l.getDateLent()).equals(month))
                    .mapToDouble(l -> val(l.getAmountLent())).sum();
            double repaid = lent.stream().flatMap(l -> l.getPayments().stream())
                    .filter(p -> p.getPaymentDate() != null && YearMonth.from(p.getPaymentDate().toLocalDate()).equals(month))
                    .mapToDouble(p -> val(p.getAmount())).sum();
            months.add(AdminOverviewDto.MonthlyVolume.builder().label(month.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                    .lent(volume).repaid(repaid).build());
        }
        List<LoanRef> refs = loanRefs(lent, borrowed, users);
        long overdue = refs.stream().filter(r -> isOverdue(r.dueDate, r.balance, today)).count();
        double overdueAmount = refs.stream().filter(r -> isOverdue(r.dueDate, r.balance, today)).mapToDouble(r -> r.balance).sum();
        long active = refs.stream().filter(r -> r.balance > 0 && !isOverdue(r.dueDate, r.balance, today)).count();
        long repaid = refs.stream().filter(r -> r.balance <= 0 || "PAID".equals(r.status)).count();
        return AdminOverviewDto.builder().totalAccounts(users.size())
                .activeAccounts(users.stream().filter(u -> u.getLastSeenAt() != null && u.getLastSeenAt().isAfter(LocalDateTime.now().minusDays(7))).count())
                .totalLoans(refs.size()).totalLent(lent.stream().mapToDouble(l -> val(l.getAmountLent())).sum())
                .outstandingBalance(refs.stream().mapToDouble(r -> r.balance).sum()).overdueLoans(overdue)
                .overdueAmount(overdueAmount).monthlyVolume(months)
                .statusCounts(new AdminOverviewDto.StatusCounts(active, overdue, repaid)).build();
    }

    private record LoanRef(LocalDate dueDate, double balance, String status) {}
    private List<LoanRef> loanRefs(List<LoanLent> lent, List<LoanBorrowed> borrowed, List<User> ignored) {
        List<LoanRef> result = new ArrayList<>();
        lent.forEach(l -> result.add(new LoanRef(l.getDueDate(), val(l.getBalance()), l.getStatus() == null ? "" : l.getStatus().name())));
        borrowed.forEach(l -> result.add(new LoanRef(l.getDueDate(), val(l.getBalance()), l.getStatus() == null ? "" : l.getStatus().name())));
        return result;
    }
    private static double val(Double value) { return value == null ? 0 : value; }
    private boolean isOverdue(LocalDate due, double balance, LocalDate today) { return balance > 0 && due != null && due.isBefore(today); }

    public List<AdminLoanDto> globalLoans() {
        List<AdminLoanDto> result = new ArrayList<>();
        for (LoanLent l : loanLentRepository.findAll()) result.add(toAdminLoan(l));
        for (LoanBorrowed l : loanBorrowedRepository.findAll()) result.add(toAdminLoan(l));
        result.sort(Comparator.comparing(AdminLoanDto::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }
    private AdminLoanDto toAdminLoan(LoanLent l) {
        User owner = userRepository.findById(l.getUserId()).orElse(null);
        return AdminLoanDto.builder().id(l.getId()).kind("LENT").accountId(l.getUserId()).accountName(owner == null ? null : owner.getFullName())
                .accountEmail(owner == null ? null : owner.getEmail()).counterpartyName(l.getPersonName()).counterpartyPhone(l.getPhoneNumber())
                .counterpartyEmail(l.getCounterpartyEmail()).amount(l.getAmountLent()).amountPaid(val(l.getAmountPaid())).balance(val(l.getBalance()))
                .startedAt(l.getDateLent()).dueDate(l.getDueDate()).status(effectiveStatus(l.getStatus(), l.getDueDate(), l.getBalance()))
                .installments(LoanResponseMapper.toResponse(l).getInstallments()).build();
    }
    private AdminLoanDto toAdminLoan(LoanBorrowed l) {
        User owner = userRepository.findById(l.getUserId()).orElse(null);
        return AdminLoanDto.builder().id(l.getId()).kind("BORROWED").accountId(l.getUserId()).accountName(owner == null ? null : owner.getFullName())
                .accountEmail(owner == null ? null : owner.getEmail()).counterpartyName(l.getPersonName()).counterpartyPhone(l.getPhoneNumber())
                .counterpartyEmail(l.getCounterpartyEmail()).amount(l.getAmountBorrowed()).amountPaid(val(l.getAmountPaid())).balance(val(l.getBalance()))
                .startedAt(l.getDateBorrowed()).dueDate(l.getDueDate()).status(effectiveStatus(l.getStatus(), l.getDueDate(), l.getBalance()))
                .installments(LoanResponseMapper.toResponse(l).getInstallments()).build();
    }
    private String effectiveStatus(PersonalLoanStatus status, LocalDate dueDate, Double balance) {
        if (val(balance) <= 0) return "PAID";
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) return "OVERDUE";
        return status == null ? "ACTIVE" : status.name();
    }

    public List<AdminAuditDto> auditLog() {
        return auditRepository.findAll().stream().sorted(Comparator.comparing(AdminAuditEvent::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())))                .map(e -> AdminAuditDto.builder().id(e.getId()).action(e.getAction()).targetType(e.getTargetType())
                .actorName(e.getActorName()).targetId(e.getTargetId()).targetName(e.getTargetName()).details(e.getDetails())
                .createdAt(e.getCreatedAt()).build()).toList();
    }
    public List<AdminUserNoteDto> notes(Long userId) {
        requireUser(userId);
        return noteRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> AdminUserNoteDto.builder().id(n.getId()).note(n.getNote()).authorName(n.getAuthorName()).createdAt(n.getCreatedAt()).build()).toList();
    }
    public AdminUserNoteDto addNote(Long userId, String note, User actor) {
        User target = requireUser(userId);
        if (note == null || note.isBlank() || note.length() > 10000) throw new IllegalArgumentException("Note must be between 1 and 10000 characters");
        AdminUserNote saved = noteRepository.save(AdminUserNote.builder().userId(userId).note(note.trim()).authorId(actor.getId()).authorName(actor.getFullName()).build());
        audit(actor, "USER_NOTE_ADDED", userId, target.getFullName(), "Internal note added");
        return AdminUserNoteDto.builder().id(saved.getId()).note(saved.getNote()).authorName(saved.getAuthorName()).createdAt(saved.getCreatedAt()).build();
    }

    public List<AdminAnnouncementDto> announcements() {
        return announcementRepository.findAll().stream().sorted(Comparator.comparing(AdminAnnouncement::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder()))).map(a -> AdminAnnouncementDto.builder().id(a.getId()).title(a.getTitle())
                .message(a.getMessage()).audience(a.getAudience()).channel(a.getChannel()).createdAt(a.getCreatedAt())
                .authorName(a.getAuthorName()).recipientCount(a.getRecipientCount())
                .targetUserId(a.getTargetUserId()).targetName(a.getTargetName()).build()).toList();
    }
    public AdminAnnouncementDto createAnnouncement(AnnouncementRequest request, User actor) {
        if (request == null || blank(request.getTitle()) || request.getTitle().length() > 200 || blank(request.getMessage()) || request.getMessage().length() > 10000)
            throw new IllegalArgumentException("Title and message are required (title max 200, message max 10000)");
        String channel = request.getChannel() == null ? "" : request.getChannel().trim().toUpperCase();
        if ("SMS".equals(channel)) throw new IllegalArgumentException("SMS announcements are unavailable");
        if (!List.of("IN_APP", "EMAIL").contains(channel)) throw new IllegalArgumentException("Channel must be IN_APP or EMAIL");
        if (request.getTargetUserId() == null && (request.getAudience() == null
                || !List.of("ALL_USERS", "BORROWERS_WITH_OVERDUE", "LENDERS_ONLY", "ADMINISTRATORS")
                .contains(request.getAudience().trim().toUpperCase())))
            throw new IllegalArgumentException("A valid audience is required when targetUserId is not provided");
        List<User> recipients;
        if (request.getTargetUserId() != null) recipients = List.of(requireUser(request.getTargetUserId()));
        else recipients = selectRecipients(request.getAudience().trim().toUpperCase());
        String audience = request.getTargetUserId() == null ? request.getAudience().toUpperCase() : "TARGET_USER";
        AdminAnnouncement announcement = announcementRepository.save(AdminAnnouncement.builder().title(request.getTitle().trim())
                .message(request.getMessage().trim()).audience(audience).channel(channel).targetUserId(request.getTargetUserId())
                .targetName(request.getTargetUserId() == null ? null
                        : blank(recipients.get(0).getFullName()) ? recipients.get(0).getEmail() : recipients.get(0).getFullName())
                .authorId(actor.getId()).authorName(actor.getFullName()).recipientCount(recipients.size()).build());
        for (User recipient : recipients) {
            if ("IN_APP".equals(channel)) notificationRepository.save(AppNotification.builder().userId(recipient.getId())
                    .announcementId(announcement.getId()).title(announcement.getTitle()).message(announcement.getMessage()).read(false).build());
            else emailService.sendEmail(recipient.getEmail(), announcement.getTitle(), announcement.getMessage());
        }
        audit(actor, "ANNOUNCEMENT_CREATED", announcement.getId(), announcement.getTitle(),
                "Audience=" + audience + ", channel=" + channel + ", recipients=" + recipients.size());
        return AdminAnnouncementDto.builder().id(announcement.getId()).title(announcement.getTitle()).message(announcement.getMessage())
                .audience(announcement.getAudience()).channel(channel).createdAt(announcement.getCreatedAt()).authorName(actor.getFullName())
                .recipientCount(recipients.size()).targetUserId(announcement.getTargetUserId()).targetName(announcement.getTargetName()).build();
    }
    private boolean blank(String s) { return s == null || s.isBlank(); }
    private List<User> selectRecipients(String audience) {
        List<User> users = userRepository.findAll();
        return switch (audience) {
            case "ADMINISTRATORS" -> users.stream().filter(this::hasAdminRole).toList();
            case "LENDERS_ONLY" -> users.stream().filter(u -> !loanLentRepository.findAllByUserId(u.getId()).isEmpty()).toList();
            case "BORROWERS_WITH_OVERDUE" -> users.stream().filter(u -> loanBorrowedRepository.findAllByUserId(u.getId()).stream()
                    .anyMatch(l -> isOverdue(l.getDueDate(), val(l.getBalance()), LocalDate.now()))).toList();
            default -> users;
        };
    }
    public List<NotificationDto> notifications(Long userId) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> NotificationDto.builder().id(n.getId()).title(n.getTitle()).message(n.getMessage())
                        .createdAt(n.getCreatedAt()).read(n.getRead()).build()).toList();
    }
    public void markNotificationRead(Long id, Long userId) {
        AppNotification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public int sendOverdueReminder(Long loanId, User actor) {
        return sendOverdueReminder(loanId, null, actor);
    }

    public int sendOverdueReminder(Long loanId, String kind, User actor) {
        String normalizedKind = kind == null || kind.isBlank() ? null : kind.trim().toUpperCase();
        if (normalizedKind != null && !List.of("LENT", "BORROWED").contains(normalizedKind))
            throw new IllegalArgumentException("kind must be LENT or BORROWED");
        LoanLent lent = normalizedKind == null || "LENT".equals(normalizedKind)
                ? loanLentRepository.findById(loanId).orElse(null) : null;
        LoanBorrowed borrowed = normalizedKind == null || "BORROWED".equals(normalizedKind)
                ? loanBorrowedRepository.findById(loanId).orElse(null) : null;
        if (lent != null && borrowed != null && normalizedKind == null)
            throw new IllegalArgumentException("Loan ID is ambiguous; specify kind=LENT or kind=BORROWED");
        if (lent != null) {
            if (!isOverdue(lent.getDueDate(), val(lent.getBalance()), LocalDate.now()))
                throw new IllegalArgumentException("Loan is not overdue");
            sendReminder(lent.getCounterpartyEmail(), lent.getPersonName(), lent.getBalance(), lent.getDueDate());
            audit(actor, "OVERDUE_REMINDER_SENT", loanId, lent.getPersonName(), "Email reminder sent");
            return 1;
        }
        if (borrowed == null) throw new IllegalArgumentException("Loan not found");
        if (!isOverdue(borrowed.getDueDate(), val(borrowed.getBalance()), LocalDate.now()))
            throw new IllegalArgumentException("Loan is not overdue");
        sendReminder(borrowed.getCounterpartyEmail(), borrowed.getPersonName(), borrowed.getBalance(), borrowed.getDueDate());
        audit(actor, "OVERDUE_REMINDER_SENT", loanId, borrowed.getPersonName(), "Email reminder sent");
        return 1;
    }
    public int sendOverdueReminders(User actor) {
        List<LoanLent> overdueLent = loanLentRepository.findAll().stream()
                .filter(l -> isOverdue(l.getDueDate(), val(l.getBalance()), LocalDate.now())).toList();
        List<LoanBorrowed> overdueBorrowed = loanBorrowedRepository.findAll().stream()
                .filter(l -> isOverdue(l.getDueDate(), val(l.getBalance()), LocalDate.now())).toList();
        if (overdueLent.stream().anyMatch(l -> blank(l.getCounterpartyEmail()))
                || overdueBorrowed.stream().anyMatch(l -> blank(l.getCounterpartyEmail())))
            throw new IllegalStateException("One or more overdue loans have no stored counterparty email");
        int count = 0;
        for (LoanLent loan : overdueLent) { sendReminder(loan.getCounterpartyEmail(), loan.getPersonName(), loan.getBalance(), loan.getDueDate()); count++; }
        for (LoanBorrowed loan : overdueBorrowed) { sendReminder(loan.getCounterpartyEmail(), loan.getPersonName(), loan.getBalance(), loan.getDueDate()); count++; }
        audit(actor, "OVERDUE_REMINDERS_SENT", null, null, "Email reminders sent: " + count);
        return count;
    }
    private void sendReminder(String email, String name, Double balance, LocalDate dueDate) {
        if (email == null || email.isBlank()) throw new IllegalStateException("Loan has no stored counterparty email");
        emailService.sendEmail(email, "Overdue loan reminder",
                "Hello " + (name == null ? "" : name) + ", this is a reminder that your loan balance of "
                        + val(balance) + " was due on " + dueDate + ".");
    }
}
