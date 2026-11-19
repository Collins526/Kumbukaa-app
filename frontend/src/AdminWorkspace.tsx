import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { FormEvent, ReactNode } from "react";
import {
  Activity,
  ArrowDownLeft,
  ArrowRight,
  ArrowUpRight,
  Bell,
  Check,
  CircleHelp,
  Clock3,
  Download,
  Eye,
  EyeOff,
  FileText,
  HandCoins,
  KeyRound,
  LayoutDashboard,
  LoaderCircle,
  LogOut,
  Mail,
  MessageSquareText,
  RefreshCw,
  Search,
  Send,
  ShieldAlert,
  ShieldCheck,
  Smartphone,
  Trash2,
  UserCheck,
  UserPlus,
  Users,
  Wallet,
  X,
} from "lucide-react";
import { ApiError, api } from "./api";
import type {
  AdminAccountRole,
  AdminAccountStatus,
  AdminAnnouncement,
  AdminAuditEntry,
  AdminGlobalLoan,
  AdminNote,
  AppNotification,
  AdminOverview,
  AdminProfile,
  AdminUserDetails,
  AdminUserSummary,
  AnnouncementAudience,
  AnnouncementChannel,
  VerificationStatus,
} from "./types";

type AdminPage =
  | "overview"
  | "users"
  | "loans"
  | "repayments"
  | "reports"
  | "announcements"
  | "audit"
  | "security"
  | "user";
type AdminModal = "create" | "reset" | "delete" | "password" | null;
type UserFilter = "all" | "user" | "admin" | "pending";
type ChartRange = "7d" | "30d" | "90d";

const pageTitles: Record<AdminPage, string> = {
  overview: "Overview",
  users: "User directory",
  loans: "Loans",
  repayments: "Repayments",
  reports: "Reports",
  announcements: "Announcements",
  audit: "Audit log",
  security: "Account security",
  user: "User profile",
};

const audienceLabels: Record<AnnouncementAudience, string> = {
  ALL_USERS: "All users",
  BORROWERS_WITH_OVERDUE: "Borrowers with overdue loans",
  LENDERS_ONLY: "Lenders only",
  ADMINISTRATORS: "Administrators",
};

const channelLabels = {
  IN_APP: "In-app",
  EMAIL: "Email",
  SMS: "SMS (not connected)",
} satisfies Record<AnnouncementChannel, string>;

const adminMoney = (amount: number) =>
  new Intl.NumberFormat("en-KE", {
    style: "currency",
    currency: "KES",
    maximumFractionDigits: 0,
  }).format(Number.isFinite(amount) ? amount : 0);

const adminDate = (value?: string) => {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "—"
    : new Intl.DateTimeFormat("en-KE", {
        day: "numeric",
        month: "short",
        year: "numeric",
      }).format(date);
};

const adminDateTime = (value?: string) => {
  if (!value) return "Never";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "—"
    : new Intl.DateTimeFormat("en-KE", {
        day: "numeric",
        month: "short",
        hour: "2-digit",
        minute: "2-digit",
      }).format(date);
};

function errorText(caught: unknown, fallback: string) {
  if (caught instanceof ApiError && caught.status === 403) {
    return "Your administrator role cannot perform this action.";
  }
  return caught instanceof Error ? caught.message : fallback;
}

function downloadCsv(filename: string, rows: string[][]) {
  const content = rows
    .map((row) =>
      row
        .map((value) => `"${String(value ?? "").replaceAll('"', '""')}"`)
        .join(","),
    )
    .join("\r\n");
  const url = URL.createObjectURL(new Blob([content], { type: "text/csv;charset=utf-8" }));
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

function accountLabel(user: Pick<AdminUserSummary, "fullName" | "email">) {
  return user.fullName?.trim() || user.email;
}

function asAdminUserSummary(user: AdminUserSummary | AdminUserDetails): AdminUserSummary {
  return {
    id: user.id,
    fullName: user.fullName,
    email: user.email,
    phoneNumber: user.phoneNumber,
    loansLent: Array.isArray(user.loansLent) ? user.loansLent.length : user.loansLent,
    loansBorrowed: Array.isArray(user.loansBorrowed)
      ? user.loansBorrowed.length
      : user.loansBorrowed,
    admin: user.admin,
    role: user.role,
    status: user.status,
    verificationStatus: user.verificationStatus,
    createdAt: user.createdAt,
    lastSeenAt: user.lastSeenAt,
    lastDevice: user.lastDevice,
  };
}

function AdminWorkspace({
  profile,
  onProfileChange,
  onSessionExpired,
  onSignOut,
}: {
  profile: AdminProfile;
  onProfileChange: (profile: AdminProfile) => void;
  onSessionExpired: () => void;
  onSignOut: () => void;
}) {
  const [page, setPage] = useState<AdminPage>("overview");
  const [users, setUsers] = useState<AdminUserSummary[]>([]);
  const [overview, setOverview] = useState<AdminOverview | null>(null);
  const [loans, setLoans] = useState<AdminGlobalLoan[]>([]);
  const [auditLog, setAuditLog] = useState<AdminAuditEntry[]>([]);
  const [announcements, setAnnouncements] = useState<AdminAnnouncement[]>([]);
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [selectedAccount, setSelectedAccount] = useState<AdminUserDetails | null>(null);
  const [accountNotes, setAccountNotes] = useState<AdminNote[]>([]);
  const [selectedUser, setSelectedUser] = useState<AdminUserSummary | null>(null);
  const [query, setQuery] = useState("");
  const [userFilter, setUserFilter] = useState<UserFilter>("all");
  const [chartRange, setChartRange] = useState<ChartRange>("30d");
  const [loading, setLoading] = useState(true);
  const [accountLoading, setAccountLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [accountError, setAccountError] = useState("");
  const [notice, setNotice] = useState("");
  const [modal, setModal] = useState<AdminModal>(
    profile.mustChangePassword ? "password" : null,
  );
  const [notificationOpen, setNotificationOpen] = useState(false);
  const [apiOnline, setApiOnline] = useState<boolean | null>(null);
  const [noteDraft, setNoteDraft] = useState("");
  const [roleDraft, setRoleDraft] = useState<AdminAccountRole>("USER");
  const [verificationDraft, setVerificationDraft] =
    useState<VerificationStatus>("PENDING");
  const [deleteConfirmation, setDeleteConfirmation] = useState("");
  const [announcementTargetId, setAnnouncementTargetId] = useState<number | undefined>();
  const [announcementTitle, setAnnouncementTitle] = useState("");
  const [announcementMessage, setAnnouncementMessage] = useState("");
  const [announcementAudience, setAnnouncementAudience] =
    useState<AnnouncementAudience>("ALL_USERS");
  const [announcementChannel, setAnnouncementChannel] =
    useState<AnnouncementChannel>("IN_APP");
  const searchRef = useRef<HTMLInputElement>(null);
  const accountRequestRef = useRef(0);

  const canManage = profile.accessRole === "ADMIN";
  const displayName = profile.fullName?.trim() || "Administrator";
  const unreadNotifications = notifications.filter((notification) => !notification.read).length;

  const loadWorkspace = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [
        nextUsers,
        nextOverview,
        nextLoans,
        nextAuditLog,
        nextAnnouncements,
        nextNotifications,
      ] = await Promise.all([
        api.adminUsers(),
        api.adminOverview(),
        api.adminLoans(),
        api.adminAuditLog(),
        api.adminAnnouncements(),
        api.notifications(),
      ]);
      setUsers(nextUsers);
      setOverview(nextOverview);
      setLoans(nextLoans);
      setAuditLog(nextAuditLog);
      setAnnouncements(nextAnnouncements);
      setNotifications(nextNotifications);
      setApiOnline(true);
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) {
        onSessionExpired();
      } else {
        setError(errorText(caught, "Could not load the administration workspace."));
      }
    } finally {
      setLoading(false);
    }
    void api.health().then(
      () => setApiOnline(true),
      () => setApiOnline(false),
    );
  }, [onSessionExpired]);

  const loadAccount = useCallback(
    async (id: number) => {
      const requestId = ++accountRequestRef.current;
      setAccountLoading(true);
      setAccountError("");
      setSelectedAccount(null);
      setAccountNotes([]);
      try {
        const [account, notes] = await Promise.all([
          api.adminUser(id),
          api.adminUserNotes(id),
        ]);
        if (requestId !== accountRequestRef.current) return;
        setSelectedAccount(account);
        setAccountNotes(notes);
        setRoleDraft(account.role);
        setVerificationDraft(account.verificationStatus);
      } catch (caught) {
        if (caught instanceof ApiError && caught.status === 401) {
          onSessionExpired();
        } else if (requestId === accountRequestRef.current) {
          setAccountError(errorText(caught, "Could not load this account."));
        }
      } finally {
        if (requestId === accountRequestRef.current) setAccountLoading(false);
      }
    },
    [onSessionExpired],
  );

  useEffect(() => {
    void loadWorkspace();
  }, [loadWorkspace]);

  useEffect(() => {
    if (!notice) return;
    const timeout = window.setTimeout(() => setNotice(""), 4000);
    return () => window.clearTimeout(timeout);
  }, [notice]);

  useEffect(() => {
    function handleShortcut(event: KeyboardEvent) {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        searchRef.current?.focus();
      }
      if (event.key === "Escape") setNotificationOpen(false);
    }
    window.addEventListener("keydown", handleShortcut);
    return () => window.removeEventListener("keydown", handleShortcut);
  }, []);

  const filteredUsers = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return users.filter((user) => {
      const matchesSearch =
        !needle ||
        [user.fullName, user.email, user.phoneNumber, user.role, user.status]
          .filter(Boolean)
          .some((value) => value.toLowerCase().includes(needle));
      const matchesFilter =
        userFilter === "all" ||
        (userFilter === "user" && user.role === "USER") ||
        (userFilter === "admin" && user.role !== "USER") ||
        (userFilter === "pending" && user.verificationStatus === "PENDING");
      return matchesSearch && matchesFilter;
    });
  }, [query, userFilter, users]);

  const filteredLoans = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) return loans;
    return loans.filter((loan) =>
      [
        loan.accountName,
        loan.accountEmail,
        loan.counterpartyName,
        loan.counterpartyPhone,
        loan.status,
        loan.kind,
      ]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(needle)),
    );
  }, [loans, query]);

  const overdueLoans = useMemo(
    () => loans.filter((loan) => loan.status === "OVERDUE"),
    [loans],
  );
  const overdueWithEmail = overdueLoans.filter((loan) => loan.counterpartyEmail);
  const pendingVerificationCount = users.filter(
    (user) => user.verificationStatus === "PENDING",
  ).length;

  const volumeBars = useMemo(
    () => buildVolumeBars(loans, chartRange),
    [chartRange, loans],
  );
  const maxVolume = Math.max(1, ...volumeBars.flatMap((bar) => [bar.lent, bar.repaid]));

  function showError(caught: unknown, fallback: string) {
    if (caught instanceof ApiError && caught.status === 401) {
      onSessionExpired();
      return;
    }
    setError(errorText(caught, fallback));
  }

  function openAccount(user: AdminUserSummary) {
    setSelectedUser(user);
    setPage("user");
    void loadAccount(user.id);
  }

  function openModal(
    nextModal: Exclude<AdminModal, null>,
    user?: AdminUserSummary | AdminUserDetails,
  ) {
    setSelectedUser(user ? asAdminUserSummary(user) : selectedUser);
    setAccountError("");
    setDeleteConfirmation("");
    setModal(nextModal);
  }

  function closeModal() {
    if (modal === "password" && profile.mustChangePassword) return;
    setModal(null);
    setDeleteConfirmation("");
    setAccountError("");
  }

  function beginDirectMessage(user: AdminUserSummary) {
    setAnnouncementTargetId(user.id);
    setAnnouncementTitle("");
    setAnnouncementMessage("");
    setAnnouncementChannel("IN_APP");
    setPage("announcements");
  }

  async function saveUserRole() {
    if (!selectedAccount || !canManage) return;
    setBusy(true);
    setAccountError("");
    try {
      await api.updateAdminUserRole(selectedAccount.id, roleDraft);
      setNotice("Account access updated.");
      await Promise.all([loadAccount(selectedAccount.id), loadWorkspace()]);
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) onSessionExpired();
      else setAccountError(errorText(caught, "Could not update account access."));
    } finally {
      setBusy(false);
    }
  }

  async function toggleUserStatus() {
    if (!selectedAccount || !canManage) return;
    const nextStatus: AdminAccountStatus =
      selectedAccount.status === "SUSPENDED" ? "ACTIVE" : "SUSPENDED";
    const action = nextStatus === "SUSPENDED" ? "suspend" : "reactivate";
    if (!window.confirm(`${action[0].toUpperCase()}${action.slice(1)} ${accountLabel(selectedAccount)}?`)) {
      return;
    }
    setBusy(true);
    setAccountError("");
    try {
      await api.updateAdminUserStatus(selectedAccount.id, nextStatus);
      setNotice(nextStatus === "SUSPENDED" ? "Account suspended." : "Account reactivated.");
      await Promise.all([loadAccount(selectedAccount.id), loadWorkspace()]);
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) onSessionExpired();
      else setAccountError(errorText(caught, "Could not update account status."));
    } finally {
      setBusy(false);
    }
  }

  async function saveVerification() {
    if (!selectedAccount || !canManage) return;
    setBusy(true);
    setAccountError("");
    try {
      await api.updateAdminUserVerification(selectedAccount.id, verificationDraft);
      setNotice("Verification status updated.");
      await Promise.all([loadAccount(selectedAccount.id), loadWorkspace()]);
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) onSessionExpired();
      else setAccountError(errorText(caught, "Could not update verification."));
    } finally {
      setBusy(false);
    }
  }

  async function addInternalNote(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedAccount || !canManage || !noteDraft.trim()) return;
    setBusy(true);
    setAccountError("");
    try {
      const note = await api.createAdminUserNote(selectedAccount.id, noteDraft.trim());
      setAccountNotes((current) => [note, ...current]);
      setNoteDraft("");
      setNotice("Internal note saved.");
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) onSessionExpired();
      else setAccountError(errorText(caught, "Could not save this note."));
    } finally {
      setBusy(false);
    }
  }

  async function createAnnouncement(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canManage) return;
    setBusy(true);
    setError("");
    try {
      const result = await api.createAdminAnnouncement({
        title: announcementTitle.trim(),
        message: announcementMessage.trim(),
        audience: announcementAudience,
        channel: announcementChannel,
        ...(announcementTargetId ? { targetUserId: announcementTargetId } : {}),
      });
      setAnnouncements((current) => [result, ...current]);
      setAnnouncementTitle("");
      setAnnouncementMessage("");
      setAnnouncementTargetId(undefined);
      setNotice("Announcement sent.");
      setPage("announcements");
    } catch (caught) {
      showError(caught, "Could not send this announcement.");
    } finally {
      setBusy(false);
    }
  }

  async function createAdmin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    const password = String(values.get("password") ?? "");
    if (password !== String(values.get("confirmPassword") ?? "")) {
      setAccountError("The passwords do not match.");
      return;
    }
    setBusy(true);
    setAccountError("");
    try {
      const result = await api.createAdmin({
        fullName: String(values.get("fullName") ?? "").trim(),
        email: String(values.get("email") ?? "").trim(),
        phoneNumber: String(values.get("phoneNumber") ?? "").trim(),
        password,
        confirmPassword: String(values.get("confirmPassword") ?? ""),
      });
      setModal(null);
      setNotice(result.message || "Administrator created.");
      await loadWorkspace();
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) onSessionExpired();
      else setAccountError(errorText(caught, "Could not create the administrator."));
    } finally {
      setBusy(false);
    }
  }

  async function resetUserPassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedUser) return;
    const values = new FormData(event.currentTarget);
    const password = String(values.get("password") ?? "");
    if (password !== String(values.get("confirmPassword") ?? "")) {
      setAccountError("The passwords do not match.");
      return;
    }
    setBusy(true);
    setAccountError("");
    try {
      const result = await api.resetAdminUserPassword(selectedUser.id, password);
      setModal(null);
      setNotice(result.message || "Password reset.");
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) onSessionExpired();
      else setAccountError(errorText(caught, "Could not reset this password."));
    } finally {
      setBusy(false);
    }
  }

  async function deleteUser(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedUser || selectedUser.id === profile.id) return;
    if (deleteConfirmation.trim().toLowerCase() !== selectedUser.email.toLowerCase()) {
      setAccountError("Enter the account email exactly to confirm deletion.");
      return;
    }
    setBusy(true);
    setAccountError("");
    try {
      const result = await api.deleteAdminUser(selectedUser.id);
      setModal(null);
      setSelectedUser(null);
      setSelectedAccount(null);
      setPage("users");
      setNotice(result.message || "Account deleted.");
      await loadWorkspace();
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) onSessionExpired();
      else setAccountError(errorText(caught, "Could not delete this account."));
    } finally {
      setBusy(false);
    }
  }

  async function changeAdminPassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    const newPassword = String(values.get("newPassword") ?? "");
    if (newPassword !== String(values.get("confirmPassword") ?? "")) {
      setAccountError("The new passwords do not match.");
      return;
    }
    setBusy(true);
    setAccountError("");
    try {
      await api.changeAdminPassword({
        ...(profile.mustChangePassword
          ? {}
          : { currentPassword: String(values.get("currentPassword") ?? "") }),
        newPassword,
      });
      onProfileChange({ ...profile, mustChangePassword: false });
      setModal(null);
      setNotice("Password updated.");
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) onSessionExpired();
      else setAccountError(errorText(caught, "Could not update your password."));
    } finally {
      setBusy(false);
    }
  }

  async function sendReminder(loan?: AdminGlobalLoan) {
    if (!canManage) return;
    setBusy(true);
    setError("");
    try {
      const result = loan
        ? await api.remindAdminLoan(loan.kind, loan.id)
        : await api.remindOverdueLoans();
      setNotice(`${result.sent} reminder email${result.sent === 1 ? "" : "s"} sent.`);
      await loadWorkspace();
    } catch (caught) {
      showError(caught, "Could not send the reminder.");
    } finally {
      setBusy(false);
    }
  }

  async function markNotificationRead(notification: AppNotification) {
    if (notification.read) return;
    try {
      await api.markNotificationRead(notification.id);
      setNotifications((current) =>
        current.map((item) => (item.id === notification.id ? { ...item, read: true } : item)),
      );
    } catch (caught) {
      showError(caught, "Could not mark the notification as read.");
    }
  }

  function exportUsers() {
    downloadCsv("kumbukaa-users.csv", [
      ["Name", "Email", "Phone", "Role", "Status", "Verification", "Loans lent", "Loans borrowed", "Last active"],
      ...users.map((user) => [
        user.fullName,
        user.email,
        user.phoneNumber,
        user.role,
        user.status,
        user.verificationStatus,
        String(user.loansLent),
        String(user.loansBorrowed),
        user.lastSeenAt ?? "",
      ]),
    ]);
  }

  function exportLoans() {
    downloadCsv("kumbukaa-loans.csv", [
      ["Account", "Email", "Type", "Counterparty", "Counterparty email", "Amount", "Paid", "Outstanding", "Status", "Started", "Due"],
      ...loans.map((loan) => [
        loan.accountName,
        loan.accountEmail,
        loan.kind,
        loan.counterpartyName,
        loan.counterpartyEmail ?? "",
        String(loan.amount),
        String(loan.amountPaid),
        String(loan.balance),
        loan.status,
        loan.startedAt ?? "",
        loan.dueDate ?? "",
      ]),
    ]);
  }

  function exportAudit() {
    downloadCsv("kumbukaa-admin-audit.csv", [
      ["When", "Actor", "Action", "Target type", "Target", "Details"],
      ...auditLog.map((entry) => [
        entry.createdAt,
        entry.actorName,
        entry.action,
        entry.targetType ?? "",
        entry.targetName ?? "",
        entry.details,
      ]),
    ]);
  }

  function openGlobalSearch() {
    const needle = query.trim().toLowerCase();
    if (needle && users.some((user) => `${user.fullName} ${user.email} ${user.phoneNumber}`.toLowerCase().includes(needle))) {
      setPage("users");
    } else {
      setPage("loans");
    }
  }

  const accountAudit = selectedAccount
    ? auditLog.filter(
        (entry) => entry.targetType === "USER" && entry.targetId === selectedAccount.id,
      )
    : [];
  const accountLoans = selectedAccount
    ? loans.filter((loan) => loan.accountId === selectedAccount.id)
    : [];
  const totalLent = accountLoans
    .filter((loan) => loan.kind === "LENT")
    .reduce((sum, loan) => sum + loan.amount, 0);
  const totalBorrowed = accountLoans
    .filter((loan) => loan.kind === "BORROWED")
    .reduce((sum, loan) => sum + loan.amount, 0);
  const balanceToAccount = accountLoans
    .filter((loan) => loan.kind === "LENT")
    .reduce((sum, loan) => sum + loan.balance, 0);
  const overdueCount = accountLoans.filter((loan) => loan.status === "OVERDUE").length;

  return (
    <div className="admin-shell admin-upgraded">
      <aside className="admin-sidebar">
        <a className="admin-brand" href="#overview" onClick={(event) => { event.preventDefault(); setPage("overview"); }}>
          <span className="admin-brand-mark"><ShieldCheck size={19} /></span>
          <span>KUMBUKAA<small>ADMIN</small></span>
        </a>
        <div className="admin-nav-label">ADMINISTRATION</div>
        <nav className="admin-nav" aria-label="Admin navigation">
          <AdminNavButton active={page === "overview"} icon={<LayoutDashboard size={17} />} onClick={() => setPage("overview")}>Overview</AdminNavButton>
          <AdminNavButton active={page === "users" || page === "user"} icon={<Users size={17} />} onClick={() => { setPage("users"); setUserFilter("all"); }}>User directory</AdminNavButton>
          <AdminNavButton active={page === "loans"} icon={<HandCoins size={17} />} onClick={() => setPage("loans")}>Loans <span className="admin-nav-count">{loans.length || ""}</span></AdminNavButton>
          <AdminNavButton active={page === "repayments"} icon={<ArrowDownLeft size={17} />} onClick={() => setPage("repayments")}>Repayments</AdminNavButton>
          <AdminNavButton active={page === "reports"} icon={<FileText size={17} />} onClick={() => setPage("reports")}>Reports</AdminNavButton>
          <AdminNavButton active={page === "announcements"} icon={<MessageSquareText size={17} />} onClick={() => { setAnnouncementTargetId(undefined); setPage("announcements"); }}>Announcements</AdminNavButton>
        </nav>
        <div className="admin-nav-label admin-nav-label-secondary">SECURITY</div>
        <nav className="admin-nav" aria-label="Security navigation">
          <AdminNavButton active={page === "audit"} icon={<Activity size={17} />} onClick={() => setPage("audit")}>Audit log</AdminNavButton>
          <AdminNavButton active={page === "security"} icon={<KeyRound size={17} />} onClick={() => setPage("security")}>Account security</AdminNavButton>
        </nav>
        <div className="admin-sidebar-bottom">
          <div className="admin-secure-label"><span /> RBAC protected</div>
          <div className="admin-identity">
            <span className="admin-avatar">{displayName.slice(0, 1).toUpperCase()}</span>
            <span><strong>{displayName}</strong><small>{profile.email}</small></span>
            <button onClick={onSignOut} aria-label="Sign out" title="Sign out"><LogOut size={16} /></button>
          </div>
        </div>
      </aside>

      <main className="admin-main">
        <header className="admin-topbar">
          <div className="admin-breadcrumb">Platform <span>/</span> <strong>{page === "user" && selectedAccount ? accountLabel(selectedAccount) : pageTitles[page]}</strong></div>
          <div className="admin-topbar-actions admin-topbar-search-area">
            <label className="admin-global-search">
              <Search size={16} />
              <input
                ref={searchRef}
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                onKeyDown={(event) => { if (event.key === "Enter") openGlobalSearch(); }}
                placeholder="Search users, loans or phone numbers"
                aria-label="Search users and loans"
              />
              <kbd>Ctrl K</kbd>
            </label>
            <span className={`admin-role-chip ${profile.accessRole === "SUPPORT" ? "support" : ""}`}>
              <ShieldCheck size={14} /> {profile.accessRole === "SUPPORT" ? "Support agent" : "Administrator"}
            </span>
            <div className="admin-notification-wrap">
              <button
                className="admin-icon-button admin-notification-trigger"
                onClick={() => setNotificationOpen((open) => !open)}
                aria-label={`Notifications${unreadNotifications ? `, ${unreadNotifications} unread` : ""}`}
                title="Notifications"
              >
                <Bell size={16} />
                {unreadNotifications > 0 && <span>{unreadNotifications}</span>}
              </button>
              {notificationOpen && (
                <div className="admin-notification-menu">
                  <div className="admin-notification-heading">Notifications <button onClick={() => setNotificationOpen(false)} aria-label="Close notifications"><X size={14} /></button></div>
                  {notifications.length === 0 ? <p className="admin-empty-copy">No notifications.</p> : notifications.slice(0, 8).map((notification) => (
                    <button
                      className={`admin-notification-item ${notification.read ? "" : "unread"}`}
                      key={notification.id}
                      onClick={() => void markNotificationRead(notification)}
                    >
                      <strong>{notification.title}</strong>
                      <span>{notification.message}</span>
                      <small>{adminDateTime(notification.createdAt)}</small>
                    </button>
                  ))}
                </div>
              )}
            </div>
            <button className="admin-icon-button" onClick={() => setPage("security")} aria-label="Account security" title="Account security">
              <CircleHelp size={17} />
            </button>
          </div>
        </header>

        <div className="admin-content">
          {notice && <div className="admin-toast" role="status"><Check size={16} />{notice}<button onClick={() => setNotice("")} aria-label="Dismiss"><X size={14} /></button></div>}
          {error && <div className="admin-error-banner" role="alert">{error}<button onClick={() => setError("")} aria-label="Dismiss error"><X size={14} /></button></div>}

          {page === "overview" && (
            <>
              <PageHeader
                eyebrow="PLATFORM OPERATIONS"
                title="Control center"
                description="Manage accounts, track loans and review activity across Kumbukaa."
                actions={
                  <>
                    <button className="admin-secondary-button" onClick={() => void loadWorkspace()} disabled={loading}>
                      {loading ? <LoaderCircle className="spin" size={16} /> : <RefreshCw size={15} />} Refresh
                    </button>
                    <button className="admin-secondary-button" onClick={exportLoans}><Download size={15} /> Export report</button>
                    {canManage && <button className="admin-primary-button" onClick={() => openModal("create")}><UserPlus size={16} /> Add administrator</button>}
                  </>
                }
              />

              <div className="admin-live-banner"><ShieldCheck size={16} /><span>Live data from the Kumbukaa account and loan records.</span></div>

              <section className="admin-overview-metrics" aria-label="Platform metrics">
                <AdminMetric icon={<Users size={18} />} label="Registered accounts" value={loading ? "—" : (overview?.totalAccounts ?? users.length).toLocaleString()} sub="Accounts in the system" tone="blue" />
                <AdminMetric icon={<UserCheck size={18} />} label="Active accounts" value={loading ? "—" : (overview?.activeAccounts ?? 0).toLocaleString()} sub="Signed in during the last 7 days" tone="green" />
                <AdminMetric icon={<HandCoins size={18} />} label="Loans tracked" value={loading ? "—" : (overview?.totalLoans ?? loans.length).toLocaleString()} sub="Lent and borrowed" tone="violet" />
                <AdminMetric icon={<Wallet size={18} />} label="Outstanding balance" value={loading ? "—" : adminMoney(overview?.outstandingBalance ?? 0)} sub="Across lent and borrowed loans" tone="amber" />
                <AdminMetric icon={<Clock3 size={18} />} label="Overdue loans" value={loading ? "—" : (overview?.overdueLoans ?? overdueLoans.length).toLocaleString()} sub={`${adminMoney(overview?.overdueAmount ?? 0)} past due`} tone="red" />
              </section>

              <div className="admin-overview-grid">
                <section className="admin-panel admin-chart-panel">
                  <div className="admin-panel-heading">
                    <div><span className="admin-eyebrow">ACTIVITY</span><h2>Loan volume, lent vs repaid</h2><p>Values are calculated from recorded loans and payments.</p></div>
                    <div className="admin-range-switch" aria-label="Chart range">
                      {(["7d", "30d", "90d"] as const).map((range) => (
                        <button key={range} className={chartRange === range ? "active" : ""} onClick={() => setChartRange(range)}>{range === "7d" ? "7 days" : range === "30d" ? "30 days" : "90 days"}</button>
                      ))}
                    </div>
                  </div>
                  <div className="admin-volume-chart">
                    {volumeBars.every((bar) => bar.lent === 0 && bar.repaid === 0) ? (
                      <p className="admin-empty-copy">No loan activity in this period.</p>
                    ) : volumeBars.map((bar) => (
                      <div className="admin-chart-group" key={bar.label}>
                        <div className="admin-chart-bars">
                          <span className="lent" style={{ height: `${Math.max(3, (bar.lent / maxVolume) * 100)}%` }} title={`Lent ${adminMoney(bar.lent)}`} />
                          <span className="repaid" style={{ height: `${Math.max(3, (bar.repaid / maxVolume) * 100)}%` }} title={`Repaid ${adminMoney(bar.repaid)}`} />
                        </div>
                        <small>{bar.label}</small>
                      </div>
                    ))}
                    {!volumeBars.length && <p className="admin-empty-copy">No loan activity in this period.</p>}
                  </div>
                  <div className="admin-chart-legend"><span><i className="lent" /> Lent</span><span><i className="repaid" /> Repaid</span></div>
                </section>

                <section className="admin-panel admin-portfolio-panel">
                  <div className="admin-panel-heading">
                    <div><span className="admin-eyebrow">PORTFOLIO</span><h2>Loans by status</h2><p>{overview?.totalLoans ?? loans.length} recorded loans</p></div>
                    <button className="admin-icon-button" onClick={() => setPage("loans")} aria-label="View all loans"><ArrowRight size={16} /></button>
                  </div>
                  <div className="admin-status-list">
                    <StatusMetric label="Active, on time" count={overview?.statusCounts.active ?? loans.filter((loan) => loan.status === "ACTIVE" || loan.status === "PARTIALLY_PAID").length} tone="green" />
                    <StatusMetric label="Overdue" count={overview?.statusCounts.overdue ?? overdueLoans.length} tone="red" />
                    <StatusMetric label="Fully repaid" count={overview?.statusCounts.repaid ?? loans.filter((loan) => loan.status === "PAID").length} tone="blue" />
                  </div>
                </section>
              </div>

              <div className="admin-overview-grid admin-overview-grid-secondary">
                <section className="admin-panel admin-overdue-panel">
                  <div className="admin-panel-heading">
                    <div><span className="admin-eyebrow">NEEDS ATTENTION</span><h2>Overdue loans</h2><p>{overdueLoans.length} overdue across tracked accounts.</p></div>
                    <button
                      className="admin-secondary-button"
                      disabled={!canManage || busy || overdueWithEmail.length !== overdueLoans.length || overdueLoans.length === 0}
                      title={overdueWithEmail.length === overdueLoans.length && overdueLoans.length ? "Send reminders to overdue counterpart emails" : "Every overdue loan needs a counterparty email"}
                      onClick={() => void sendReminder()}
                    ><Send size={14} /> Remind all borrowers</button>
                  </div>
                  <LoansTable
                    loans={overdueLoans.slice(0, 5)}
                    canManage={canManage}
                    busy={busy}
                    onViewAccount={(id) => {
                      const user = users.find((item) => item.id === id);
                      if (user) openAccount(user);
                    }}
                    onRemind={(loan) => void sendReminder(loan)}
                    compact
                  />
                </section>

                <section className="admin-panel admin-quick-announcement">
                  <div className="admin-panel-heading">
                    <div><span className="admin-eyebrow">COMMUNICATION</span><h2>Send an announcement</h2><p>Reach users through in-app notification or email.</p></div>
                    <MessageSquareText size={18} className="admin-panel-icon" />
                  </div>
                  <AnnouncementForm
                    canManage={canManage}
                    busy={busy}
                    title={announcementTitle}
                    message={announcementMessage}
                    audience={announcementAudience}
                    channel={announcementChannel}
                    target={announcementTargetId}
                    onTitleChange={setAnnouncementTitle}
                    onMessageChange={setAnnouncementMessage}
                    onAudienceChange={setAnnouncementAudience}
                    onChannelChange={setAnnouncementChannel}
                    onSubmit={(event) => void createAnnouncement(event)}
                    onClearTarget={() => setAnnouncementTargetId(undefined)}
                  />
                </section>
              </div>

              <section className="admin-panel admin-directory admin-directory-upgraded">
                <div className="admin-directory-heading">
                  <div><span className="admin-eyebrow">ACCOUNT MANAGEMENT</span><h2>User directory</h2><p>Inspect accounts, loan activity, verification and access level.</p></div>
                  <div className="admin-heading-actions">
                    <label className="admin-search"><Search size={16} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search accounts" aria-label="Search accounts" /></label>
                    <button className="admin-secondary-button" onClick={exportUsers}><Download size={14} /> Export CSV</button>
                  </div>
                </div>
                <UserTable
                  users={filteredUsers.slice(0, 8)}
                  loading={loading}
                  canManage={canManage}
                  currentUserId={profile.id}
                  onView={openAccount}
                  onReset={(user) => openModal("reset", user)}
                  onDelete={(user) => openModal("delete", user)}
                />
                <div className="admin-directory-footer">
                  <span>Showing {Math.min(filteredUsers.length, 8)} of {filteredUsers.length} matching accounts.</span>
                  <button className="admin-text-button" onClick={() => setPage("users")}>Open directory <ArrowRight size={13} /></button>
                  <span><ShieldCheck size={14} /> Role-checked by the backend</span>
                </div>
              </section>

              <div className="admin-overview-grid admin-overview-grid-secondary">
                <section className="admin-panel admin-activity-panel">
                  <div className="admin-panel-heading"><div><span className="admin-eyebrow">AUDIT TRAIL</span><h2>Recent activity</h2><p>Recorded administrator and account events.</p></div><button className="admin-text-button" onClick={() => setPage("audit")}>View full audit log <ArrowRight size={13} /></button></div>
                  <AuditList entries={auditLog.slice(0, 5)} />
                </section>
                <div className="admin-side-panels">
                  <section className="admin-panel admin-todo-panel">
                    <div className="admin-panel-heading"><div><span className="admin-eyebrow">TO DO</span><h2>Pending actions</h2></div></div>
                    {pendingVerificationCount > 0 && <button className="admin-todo-row" onClick={() => { setPage("users"); setUserFilter("pending"); }}><span><strong>Review {pendingVerificationCount} verification {pendingVerificationCount === 1 ? "request" : "requests"}</strong><small>Accounts awaiting KYC status review</small></span><span className="admin-todo-cta">Review</span></button>}
                    {overdueLoans.length > 0 && <button className="admin-todo-row" disabled={!canManage || overdueWithEmail.length !== overdueLoans.length} title={overdueWithEmail.length === overdueLoans.length ? "" : "Every overdue loan needs a counterparty email"} onClick={() => void sendReminder()}><span><strong>Send {overdueLoans.length} overdue reminder{overdueLoans.length === 1 ? "" : "s"}</strong><small>{overdueWithEmail.length} of {overdueLoans.length} counterparty emails available</small></span><span className="admin-todo-cta">{overdueWithEmail.length === overdueLoans.length ? "Send" : "Unavailable"}</span></button>}
                    {!pendingVerificationCount && !overdueLoans.length && <p className="admin-empty-copy">No pending account actions.</p>}
                  </section>
                  <section className="admin-panel admin-health-panel">
                    <div className="admin-panel-heading"><div><span className="admin-eyebrow">PLATFORM</span><h2>System health</h2></div><Activity size={17} className="admin-panel-icon" /></div>
                    <HealthRow name="API and database" state={apiOnline === null ? "Checking" : apiOnline ? "Operational" : "Unavailable"} healthy={apiOnline === true} />
                    <HealthRow name="SMS reminders" state="Not connected" healthy={false} />
                    <HealthRow name="M-Pesa payments" state="Not connected" healthy={false} />
                  </section>
                </div>
              </div>
              <footer className="admin-page-footer">Kumbukaa Administration <span>·</span> Secure workspace</footer>
            </>
          )}

          {page === "users" && (
            <>
              <PageHeader eyebrow="ACCOUNT MANAGEMENT" title="User directory" description="Inspect account status, verification, loan activity and access." actions={<><button className="admin-secondary-button" onClick={() => void loadWorkspace()} disabled={loading}><RefreshCw size={14} /> Refresh</button><button className="admin-secondary-button" onClick={exportUsers}><Download size={14} /> Export CSV</button>{canManage && <button className="admin-primary-button" onClick={() => openModal("create")}><UserPlus size={15} /> Add administrator</button>}</>} />
              <div className="admin-filter-bar">
                <div className="admin-filter-chips">
                  {([
                    ["all", "All", users.length],
                    ["user", "Users", users.filter((user) => user.role === "USER").length],
                    ["admin", "Admins & support", users.filter((user) => user.role !== "USER").length],
                    ["pending", "Pending verification", pendingVerificationCount],
                  ] as const).map(([filter, label, count]) => (
                    <button key={filter} className={userFilter === filter ? "active" : ""} onClick={() => setUserFilter(filter)}>{label}<span>{count}</span></button>
                  ))}
                </div>
                <label className="admin-search"><Search size={16} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search accounts" aria-label="Search accounts" /></label>
              </div>
              <section className="admin-panel admin-directory admin-directory-upgraded">
                <UserTable users={filteredUsers} loading={loading} canManage={canManage} currentUserId={profile.id} onView={openAccount} onReset={(user) => openModal("reset", user)} onDelete={(user) => openModal("delete", user)} />
                <div className="admin-directory-footer"><span>Showing {filteredUsers.length} of {users.length} accounts.</span><span><ShieldCheck size={14} /> Role-checked by the backend</span></div>
              </section>
            </>
          )}

          {page === "loans" && (
            <>
              <PageHeader eyebrow="PORTFOLIO" title="Loans" description="Review every lent and borrowed loan across accounts." actions={<><button className="admin-secondary-button" onClick={() => void loadWorkspace()} disabled={loading}><RefreshCw size={14} /> Refresh</button><button className="admin-primary-button" onClick={exportLoans}><Download size={15} /> Export loans</button></>} />
              <div className="admin-filter-bar"><span className="admin-result-count">{filteredLoans.length} loans tracked</span><label className="admin-search"><Search size={16} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search loans or accounts" aria-label="Search loans" /></label></div>
              <section className="admin-panel admin-directory admin-directory-upgraded">
                <LoansTable loans={filteredLoans} canManage={canManage} busy={busy} onViewAccount={(id) => { const user = users.find((item) => item.id === id); if (user) openAccount(user); }} onRemind={(loan) => void sendReminder(loan)} />
              </section>
            </>
          )}

          {page === "repayments" && (
            <>
              <PageHeader eyebrow="PAYMENT ACTIVITY" title="Repayments" description="Payment installments recorded against tracked loans." actions={<button className="admin-secondary-button" onClick={() => void loadWorkspace()} disabled={loading}><RefreshCw size={14} /> Refresh</button>} />
              <RepaymentTable loans={loans} loading={loading} />
            </>
          )}

          {page === "reports" && (
            <>
              <PageHeader eyebrow="REPORTING" title="Reports" description="Export current account, loan, and audit data as CSV files." />
              <div className="admin-report-grid">
                <ReportCard icon={<Users size={20} />} title="Account directory" description={`${users.length} accounts, roles, statuses and verification data.`} onExport={exportUsers} />
                <ReportCard icon={<HandCoins size={20} />} title="Loan portfolio" description={`${loans.length} loans with outstanding balances and repayment status.`} onExport={exportLoans} />
                <ReportCard icon={<Activity size={20} />} title="Admin audit log" description={`${auditLog.length} recorded administrative and account events.`} onExport={exportAudit} />
              </div>
            </>
          )}

          {page === "announcements" && (
            <>
              <PageHeader eyebrow="COMMUNICATION" title="Announcements" description="Send in-app notices or email to selected account audiences." />
              <div className="admin-announcement-grid">
                <section className="admin-panel admin-compose-panel">
                  <div className="admin-panel-heading"><div><span className="admin-eyebrow">NEW MESSAGE</span><h2>{announcementTargetId ? `Message ${users.find((user) => user.id === announcementTargetId)?.fullName || "account"}` : "Send an announcement"}</h2><p>SMS is unavailable until an SMS provider is configured.</p></div><MessageSquareText size={18} className="admin-panel-icon" /></div>
                  <AnnouncementForm
                    canManage={canManage}
                    busy={busy}
                    title={announcementTitle}
                    message={announcementMessage}
                    audience={announcementAudience}
                    channel={announcementChannel}
                    target={announcementTargetId}
                    onTitleChange={setAnnouncementTitle}
                    onMessageChange={setAnnouncementMessage}
                    onAudienceChange={setAnnouncementAudience}
                    onChannelChange={setAnnouncementChannel}
                    onSubmit={(event) => void createAnnouncement(event)}
                    onClearTarget={() => setAnnouncementTargetId(undefined)}
                  />
                </section>
                <section className="admin-panel admin-announcement-history">
                  <div className="admin-panel-heading"><div><span className="admin-eyebrow">SENT MESSAGES</span><h2>Announcement history</h2></div></div>
                  <AnnouncementList items={announcements} />
                </section>
              </div>
            </>
          )}

          {page === "audit" && (
            <>
              <PageHeader eyebrow="SECURITY" title="Audit log" description="A durable record of administrative and account events." actions={<button className="admin-secondary-button" onClick={exportAudit}><Download size={14} /> Export CSV</button>} />
              <section className="admin-panel admin-audit-page"><AuditList entries={auditLog} detailed /></section>
            </>
          )}

          {page === "security" && (
            <>
              <PageHeader eyebrow="SECURITY" title="Account security" description="Update your administrator password and review access protections." />
              <div className="admin-security-grid">
                <section className="admin-panel admin-security-card">
                  <div className="admin-panel-heading"><div><span className="admin-eyebrow">YOUR ACCOUNT</span><h2>Change password</h2><p>Use a unique password with at least 8 characters.</p></div><KeyRound size={18} className="admin-panel-icon" /></div>
                  {canManage
                    ? <PasswordForm busy={busy} forced={false} onSubmit={(event) => void changeAdminPassword(event)} error={accountError} />
                    : <p className="admin-form-hint">This support account is read-only. Contact an administrator to change account credentials.</p>}
                </section>
                <section className="admin-panel admin-security-card">
                  <div className="admin-panel-heading"><div><span className="admin-eyebrow">ACCESS CONTROL</span><h2>Workspace protections</h2><p>Administrative access is enforced by the backend.</p></div><ShieldCheck size={18} className="admin-panel-icon" /></div>
                  <HealthRow name="Role-based access control" state={profile.accessRole === "ADMIN" ? "Administrator" : "Read only"} healthy />
                  <HealthRow name="Backend authorization" state="Enabled" healthy />
                  <HealthRow name="SMS delivery" state="Not connected" healthy={false} />
                  {profile.accessRole === "SUPPORT" && <p className="admin-form-hint">Support agents can review records but cannot make changes.</p>}
                </section>
              </div>
            </>
          )}

          {page === "user" && (
            <>
              <div className="admin-detail-toolbar">
                <button className="admin-text-button" onClick={() => setPage("users")}><ArrowRight className="admin-back-arrow" size={14} /> User directory</button>
                <span>/</span><strong>{selectedAccount ? accountLabel(selectedAccount) : selectedUser ? accountLabel(selectedUser) : "Account"}</strong>
              </div>
              {accountError && <div className="admin-error-banner" role="alert">{accountError}</div>}
              {accountLoading || !selectedAccount ? (
                <div className="admin-panel admin-detail-loading"><LoaderCircle className="spin" size={19} />{accountError ? "Account details could not be loaded." : "Loading account profile…"}</div>
              ) : (
                <>
                  <section className="admin-panel admin-profile-hero">
                    <div className="admin-profile-identity">
                      <span className="admin-profile-avatar">{(selectedAccount.fullName || selectedAccount.email).slice(0, 1).toUpperCase()}</span>
                      <div><div className="admin-profile-name-row"><h1>{accountLabel(selectedAccount)}</h1><span className={`admin-account-status status-${selectedAccount.status.toLowerCase()}`}>{selectedAccount.status === "ACTIVE" ? "Active" : "Suspended"}</span><span className={`admin-access-badge ${selectedAccount.role === "USER" ? "" : "is-admin"}`}>{roleLabel(selectedAccount.role)}</span><span className={`admin-verify-badge verify-${selectedAccount.verificationStatus.toLowerCase()}`}>{verificationLabel(selectedAccount.verificationStatus)}</span></div><p>{selectedAccount.email} <span>·</span> {selectedAccount.phoneNumber || "No phone number"}</p></div>
                    </div>
                    <div className="admin-profile-actions">
                      {canManage && <button className="admin-secondary-button" onClick={() => beginDirectMessage(asAdminUserSummary(selectedAccount))}><Mail size={14} /> Send message</button>}
                      {canManage && <button className="admin-secondary-button" onClick={() => openModal("reset", selectedAccount)}><KeyRound size={14} /> Reset password</button>}
                      {canManage && selectedAccount.id !== profile.id && <button className={selectedAccount.status === "SUSPENDED" ? "admin-primary-button" : "admin-danger-button"} disabled={busy} onClick={() => void toggleUserStatus()}><ShieldAlert size={14} /> {selectedAccount.status === "SUSPENDED" ? "Reactivate" : "Suspend account"}</button>}
                    </div>
                  </section>

                  <section className="admin-profile-metrics">
                    <AdminMetric icon={<ArrowUpRight size={17} />} label="Total lent" value={adminMoney(totalLent)} sub={`${selectedAccount.loansLent.length} loans`} tone="green" />
                    <AdminMetric icon={<ArrowDownLeft size={17} />} label="Total borrowed" value={adminMoney(totalBorrowed)} sub={`${selectedAccount.loansBorrowed.length} loans`} tone="violet" />
                    <AdminMetric icon={<Wallet size={17} />} label="Owed to this user" value={adminMoney(balanceToAccount)} sub={`${overdueCount} overdue`} tone="blue" />
                    <AdminMetric icon={<Clock3 size={17} />} label="Last active" value={selectedAccount.lastSeenAt ? adminDateTime(selectedAccount.lastSeenAt) : "Never"} sub={selectedAccount.lastDevice || "Device not recorded"} tone="amber" />
                  </section>

                  <div className="admin-profile-grid">
                    <section className="admin-panel admin-account-details-panel">
                      <div className="admin-panel-heading"><div><span className="admin-eyebrow">ACCOUNT DETAILS</span><h2>Profile and access</h2></div><ShieldCheck size={17} className="admin-panel-icon" /></div>
                      <div className="admin-details-grid">
                        <DetailValue label="Email" value={selectedAccount.email} />
                        <DetailValue label="Phone" value={selectedAccount.phoneNumber || "—"} />
                        <DetailValue label="Joined" value={adminDate(selectedAccount.createdAt)} />
                        <DetailValue label="Last sign-in" value={adminDateTime(selectedAccount.lastSeenAt)} />
                        <DetailValue label="Last device" value={selectedAccount.lastDevice || "Not recorded"} />
                        <DetailValue label="Verification" value={verificationLabel(selectedAccount.verificationStatus)} />
                        <DetailValue label="Access level" value={roleLabel(selectedAccount.role)} />
                        <DetailValue label="Account status" value={selectedAccount.status === "ACTIVE" ? "Active" : "Suspended"} />
                      </div>
                      <div className="admin-access-editor">
                        <div><strong>Access level</strong><small>Role changes are checked by the backend and recorded in the audit log.</small></div>
                        <div className="admin-inline-editor">
                          <select value={roleDraft} disabled={!canManage || busy || selectedAccount.id === profile.id} onChange={(event) => setRoleDraft(event.target.value as AdminAccountRole)} aria-label="Account access level">
                            <option value="USER">User</option><option value="SUPPORT_AGENT">Support agent (view only)</option><option value="ADMIN">Administrator</option>
                          </select>
                          {canManage && <button className="admin-primary-button" onClick={() => void saveUserRole()} disabled={busy || roleDraft === selectedAccount.role || selectedAccount.id === profile.id}>{busy ? <LoaderCircle className="spin" size={14} /> : null} Save role</button>}
                        </div>
                      </div>
                      <div className="admin-access-editor">
                        <div><strong>Verification</strong><small>Set the review status for this account.</small></div>
                        <div className="admin-inline-editor">
                          <select value={verificationDraft} disabled={!canManage || busy} onChange={(event) => setVerificationDraft(event.target.value as VerificationStatus)} aria-label="Verification status">
                            <option value="PENDING">Pending review</option><option value="VERIFIED">Verified</option><option value="REJECTED">Rejected</option>
                          </select>
                          {canManage && <button className="admin-primary-button" onClick={() => void saveVerification()} disabled={busy || verificationDraft === selectedAccount.verificationStatus}>Save status</button>}
                        </div>
                      </div>
                    </section>

                    <section className="admin-panel admin-notes-panel">
                      <div className="admin-panel-heading"><div><span className="admin-eyebrow">INTERNAL NOTES</span><h2>Administrator notes</h2><p>Visible only to authorized staff.</p></div><MessageSquareText size={17} className="admin-panel-icon" /></div>
                      {canManage && <form className="admin-note-form" onSubmit={(event) => void addInternalNote(event)}><textarea value={noteDraft} onChange={(event) => setNoteDraft(event.target.value)} placeholder="Add a note (only administrators see this)" maxLength={1500} required /><button className="admin-primary-button" type="submit" disabled={busy || !noteDraft.trim()}>{busy ? <LoaderCircle className="spin" size={14} /> : null} Save note</button></form>}
                      <NotesList notes={accountNotes} />
                    </section>
                  </div>

                  <section className="admin-panel admin-profile-loans">
                    <div className="admin-panel-heading"><div><span className="admin-eyebrow">PORTFOLIO</span><h2>Loans</h2><p>Loan history associated with this account.</p></div><button className="admin-text-button" onClick={() => setPage("loans")}>All loans <ArrowRight size={13} /></button></div>
                    <LoansTable loans={accountLoans} canManage={canManage} busy={busy} onRemind={(loan) => void sendReminder(loan)} />
                  </section>

                  <section className="admin-panel admin-timeline-panel">
                    <div className="admin-panel-heading"><div><span className="admin-eyebrow">ACTIVITY TIMELINE</span><h2>Recent account activity</h2></div><button className="admin-text-button" onClick={() => setPage("audit")}>Full audit log <ArrowRight size={13} /></button></div>
                    <AuditList entries={accountAudit.slice(0, 8)} />
                  </section>

                  {canManage && selectedAccount.id !== profile.id && (
                    <section className="admin-danger-zone">
                      <div><span className="admin-eyebrow">DANGER ZONE</span><h2>Delete account</h2><p>Account deletion may also remove associated loan history. This cannot be undone.</p></div>
                      <button className="admin-danger-button" onClick={() => openModal("delete", selectedAccount)}><Trash2 size={14} /> Delete account</button>
                    </section>
                  )}
                </>
              )}
            </>
          )}
        </div>
      </main>

      {modal && (
        <div className="admin-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) closeModal(); }}>
          <section className="admin-modal" role="dialog" aria-modal="true" aria-labelledby="admin-modal-title">
            <header className="admin-modal-header">
              <div><span className="admin-eyebrow">KUMBUKAA ADMIN</span><h2 id="admin-modal-title">{modal === "create" ? "Add administrator" : modal === "reset" ? "Reset password" : modal === "delete" ? "Delete account" : "Change your password"}</h2></div>
              {!(modal === "password" && profile.mustChangePassword) && <button className="admin-modal-close" onClick={closeModal} aria-label="Close dialog"><X size={18} /></button>}
            </header>
            {modal === "create" && (
              <form className="admin-modal-form" onSubmit={(event) => void createAdmin(event)}>
                <p className="admin-modal-description">New administrators must change their password when they first sign in.</p>
                <label>Full name<input name="fullName" autoComplete="name" required /></label>
                <label>Email address<input name="email" type="email" autoComplete="email" required /></label>
                <label>Phone number<input name="phoneNumber" type="tel" autoComplete="tel" required /></label>
                <AdminPasswordInput label="Password" name="password" autoComplete="new-password" minLength={8} />
                <AdminPasswordInput label="Confirm password" name="confirmPassword" autoComplete="new-password" minLength={8} />
                {accountError && <div className="admin-form-error" role="alert">{accountError}</div>}
                <AdminModalActions busy={busy} submitLabel="Create administrator" onCancel={closeModal} />
              </form>
            )}
            {modal === "reset" && selectedUser && (
              <form className="admin-modal-form" onSubmit={(event) => void resetUserPassword(event)}>
                <p className="admin-modal-description">Set a new password for <strong>{accountLabel(selectedUser)}</strong>.</p>
                <AdminPasswordInput label="New password" name="password" autoComplete="new-password" minLength={8} />
                <AdminPasswordInput label="Confirm password" name="confirmPassword" autoComplete="new-password" minLength={8} />
                {accountError && <div className="admin-form-error" role="alert">{accountError}</div>}
                <AdminModalActions busy={busy} submitLabel="Reset password" onCancel={closeModal} />
              </form>
            )}
            {modal === "delete" && selectedUser && (
              <form className="admin-modal-form" onSubmit={(event) => void deleteUser(event)}>
                <p className="admin-modal-description">This removes <strong>{accountLabel(selectedUser)}</strong> and may remove its loan history. Enter the account email to confirm.</p>
                <label>Account email<input value={deleteConfirmation} onChange={(event) => setDeleteConfirmation(event.target.value)} autoComplete="off" required /></label>
                {accountError && <div className="admin-form-error" role="alert">{accountError}</div>}
                <div className="admin-modal-actions"><button className="admin-secondary-button" type="button" onClick={closeModal}>Cancel</button><button className="admin-danger-button" type="submit" disabled={busy || selectedUser.id === profile.id}>{busy ? <LoaderCircle className="spin" size={14} /> : <Trash2 size={14} />} Delete account</button></div>
              </form>
            )}
            {modal === "password" && (
              <>
                {profile.mustChangePassword && <p className="admin-forced-change">Change the initial password to continue using admin tools.</p>}
                <PasswordForm busy={busy} forced={profile.mustChangePassword} onSubmit={(event) => void changeAdminPassword(event)} error={accountError} />
              </>
            )}
          </section>
        </div>
      )}
    </div>
  );
}

function buildVolumeBars(loans: AdminGlobalLoan[], range: ChartRange) {
  const now = new Date();
  const size = range === "7d" ? 7 : range === "30d" ? 5 : 4;
  const bars = Array.from({ length: size }, (_, index) => {
    const date = new Date(now);
    if (range === "7d") {
      date.setDate(now.getDate() - (size - index - 1));
      return { label: new Intl.DateTimeFormat("en", { weekday: "short" }).format(date), lent: 0, repaid: 0, key: date.toDateString() };
    }
    if (range === "30d") {
      return { label: `Week ${index + 1}`, lent: 0, repaid: 0, key: String(index) };
    }
    date.setMonth(now.getMonth() - (size - index - 1), 1);
    return { label: new Intl.DateTimeFormat("en", { month: "short" }).format(date), lent: 0, repaid: 0, key: `${date.getFullYear()}-${date.getMonth()}` };
  });
  const days = range === "7d" ? 7 : range === "30d" ? 30 : 90;
  const start = new Date(now);
  start.setDate(now.getDate() - days + 1);
  for (const loan of loans) {
    const loanDate = loan.startedAt ? new Date(loan.startedAt) : null;
    if (loanDate && !Number.isNaN(loanDate.getTime()) && loanDate >= start && loanDate <= now && loan.kind === "LENT") {
      const index = range === "7d"
        ? size - 1 - Math.floor((now.getTime() - loanDate.getTime()) / 86400000)
        : range === "30d"
          ? Math.min(size - 1, size - 1 - Math.floor((now.getTime() - loanDate.getTime()) / (7 * 86400000)))
          : Math.min(size - 1, Math.max(0, size - 1 - ((now.getFullYear() - loanDate.getFullYear()) * 12 + now.getMonth() - loanDate.getMonth())));
      if (bars[index]) bars[index].lent += loan.amount;
    }
    for (const payment of loan.kind === "LENT" ? loan.installments ?? [] : []) {
      if (!payment.paymentDate) continue;
      const paymentDate = new Date(payment.paymentDate);
      if (Number.isNaN(paymentDate.getTime()) || paymentDate < start || paymentDate > now) continue;
      const index = range === "7d"
        ? size - 1 - Math.floor((now.getTime() - paymentDate.getTime()) / 86400000)
        : range === "30d"
          ? Math.min(size - 1, size - 1 - Math.floor((now.getTime() - paymentDate.getTime()) / (7 * 86400000)))
          : Math.min(size - 1, Math.max(0, size - 1 - ((now.getFullYear() - paymentDate.getFullYear()) * 12 + now.getMonth() - paymentDate.getMonth())));
      if (bars[index]) bars[index].repaid += payment.amount;
    }
  }
  return bars;
}

function AdminNavButton({
  active,
  icon,
  onClick,
  children,
}: {
  active: boolean;
  icon: ReactNode;
  onClick: () => void;
  children: ReactNode;
}) {
  return <button className={`admin-nav-item ${active ? "admin-nav-active" : ""}`} onClick={onClick}>{icon}{children}</button>;
}

function PageHeader({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow: string;
  title: string;
  description: string;
  actions?: ReactNode;
}) {
  return (
    <section className="admin-page-heading">
      <div><span className="admin-eyebrow">{eyebrow}</span><h1>{title}</h1><p>{description}</p></div>
      {actions && <div className="admin-heading-actions">{actions}</div>}
    </section>
  );
}

function AdminMetric({
  icon,
  label,
  value,
  sub,
  tone,
}: {
  icon: ReactNode;
  label: string;
  value: string;
  sub: string;
  tone: string;
}) {
  return (
    <article className="admin-metric-card admin-metric-card-upgraded">
      <span className={`admin-metric-icon ${tone}`}>{icon}</span>
      <span className="admin-metric-label">{label}</span>
      <strong>{value}</strong>
      <small>{sub}</small>
    </article>
  );
}

function StatusMetric({ label, count, tone }: { label: string; count: number; tone: string }) {
  return <div className="admin-status-metric"><span className={`admin-status-dot ${tone}`} /><span>{label}</span><strong>{count}</strong></div>;
}

function HealthRow({ name, state, healthy }: { name: string; state: string; healthy: boolean }) {
  return <div className="admin-health-row"><span className={`admin-health-dot ${healthy ? "healthy" : ""}`} /><span>{name}</span><strong className={healthy ? "healthy" : ""}>{state}</strong></div>;
}

function UserTable({
  users,
  loading,
  canManage,
  currentUserId,
  onView,
  onReset,
  onDelete,
}: {
  users: AdminUserSummary[];
  loading: boolean;
  canManage: boolean;
  currentUserId: number;
  onView: (user: AdminUserSummary) => void;
  onReset: (user: AdminUserSummary) => void;
  onDelete: (user: AdminUserSummary) => void;
}) {
  return (
    <div className="admin-table-scroll">
      <table className="admin-user-table admin-user-table-wide">
        <thead><tr><th>ACCOUNT</th><th>PHONE</th><th>ACCESS</th><th>STATUS</th><th>LENT / BORROWED</th><th>LAST ACTIVE</th><th>VERIFICATION</th><th>ACTIONS</th></tr></thead>
        <tbody>
          {loading && users.length === 0 ? <tr><td className="admin-table-empty" colSpan={8}><LoaderCircle className="spin" size={17} /> Loading accounts…</td></tr> :
            users.length === 0 ? <tr><td className="admin-table-empty" colSpan={8}>No matching accounts found.</td></tr> :
              users.map((user) => (
                <tr key={user.id}>
                  <td><button className="admin-account-cell" onClick={() => onView(user)}><span className="admin-user-avatar">{(user.fullName || user.email).slice(0, 1).toUpperCase()}</span><span><strong>{accountLabel(user)}</strong><small>{user.email}</small></span></button></td>
                  <td>{user.phoneNumber || "—"}</td>
                  <td><span className={`admin-access-badge ${user.role === "USER" ? "" : "is-admin"}`}>{roleLabel(user.role)}</span></td>
                  <td><span className={`admin-account-status status-${user.status.toLowerCase()}`}>{user.status === "ACTIVE" ? "Active" : "Suspended"}</span></td>
                  <td>{user.loansLent} / {user.loansBorrowed}</td>
                  <td>{user.lastSeenAt ? adminDateTime(user.lastSeenAt) : "Never"}</td>
                  <td><span className={`admin-verify-badge verify-${user.verificationStatus.toLowerCase()}`}>{verificationLabel(user.verificationStatus)}</span></td>
                  <td><div className="admin-row-actions">
                    <button onClick={() => onView(user)} aria-label={`View ${accountLabel(user)}`} title="View account"><Eye size={15} /></button>
                    {canManage && <button onClick={() => onReset(user)} aria-label={`Reset password for ${accountLabel(user)}`} title="Reset password"><KeyRound size={15} /></button>}
                    {canManage && user.id !== currentUserId && <button className="admin-delete-button" onClick={() => onDelete(user)} aria-label={`Delete ${accountLabel(user)}`} title="Delete account"><Trash2 size={15} /></button>}
                    {user.id === currentUserId && <span className="admin-self-label">You</span>}
                  </div></td>
                </tr>
              ))}
        </tbody>
      </table>
    </div>
  );
}

function LoansTable({
  loans,
  canManage,
  busy,
  onViewAccount,
  onRemind,
  compact = false,
}: {
  loans: AdminGlobalLoan[];
  canManage: boolean;
  busy: boolean;
  onViewAccount?: (id: number) => void;
  onRemind: (loan: AdminGlobalLoan) => void;
  compact?: boolean;
}) {
  return (
    <div className="admin-table-scroll">
      <table className={`admin-loans-table ${compact ? "compact" : ""}`}>
        <thead><tr><th>LOAN</th><th>ACCOUNT</th><th>COUNTERPARTY</th><th>AMOUNT</th><th>OUTSTANDING</th><th>STATUS</th><th>DUE</th><th>ACTION</th></tr></thead>
        <tbody>
          {loans.length === 0 ? <tr><td className="admin-table-empty" colSpan={8}>No loans recorded.</td></tr> : loans.map((loan) => (
            <tr key={`${loan.kind}-${loan.id}`}>
              <td><strong>L-{String(loan.id).padStart(4, "0")}</strong><small>{loan.kind === "LENT" ? "Lent" : "Borrowed"}</small></td>
              <td>{onViewAccount ? <button className="admin-table-link" onClick={() => onViewAccount(loan.accountId)}>{loan.accountName || loan.accountEmail}</button> : <strong>{loan.accountName || loan.accountEmail}</strong>}<small>{loan.accountEmail}</small></td>
              <td><strong>{loan.counterpartyName || "—"}</strong><small>{loan.counterpartyPhone || loan.counterpartyEmail || "No contact"}</small></td>
              <td>{adminMoney(loan.amount)}<small>Paid {adminMoney(loan.amountPaid)}</small></td>
              <td>{adminMoney(loan.balance)}</td>
              <td><span className={`admin-loan-status status-${loan.status.toLowerCase()}`}>{loan.status.replaceAll("_", " ")}</span></td>
              <td>{adminDate(loan.dueDate)}</td>
              <td>{loan.status === "OVERDUE" && canManage ? <button className="admin-text-button" disabled={busy || !loan.counterpartyEmail} title={loan.counterpartyEmail ? "Send reminder email" : "Counterparty email is not recorded"} onClick={() => onRemind(loan)}><Send size={13} /> Remind</button> : <span className="admin-muted">—</span>}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function RepaymentTable({ loans, loading }: { loans: AdminGlobalLoan[]; loading: boolean }) {
  const payments = loans.flatMap((loan) =>
    (loan.installments ?? []).map((payment, index) => ({
      id: `${loan.kind}-${loan.id}-${index}`,
      accountName: loan.accountName,
      counterpartyName: loan.counterpartyName,
      loanId: loan.id,
      amount: payment.amount,
      paymentDate: payment.paymentDate,
      kind: loan.kind,
    })),
  ).sort((a, b) => new Date(b.paymentDate ?? 0).getTime() - new Date(a.paymentDate ?? 0).getTime());
  return (
    <section className="admin-panel admin-directory admin-directory-upgraded">
      <div className="admin-directory-heading"><div><span className="admin-eyebrow">RECORDED PAYMENTS</span><h2>Repayment history</h2><p>Payments are grouped by their associated loan and account.</p></div><span className="admin-result-count">{loading ? "Loading…" : `${payments.length} repayments`}</span></div>
      <div className="admin-table-scroll"><table className="admin-loans-table">
        <thead><tr><th>DATE</th><th>ACCOUNT</th><th>COUNTERPARTY</th><th>LOAN</th><th>TYPE</th><th>PAYMENT</th></tr></thead>
        <tbody>{loading && !payments.length ? <tr><td className="admin-table-empty" colSpan={6}><LoaderCircle className="spin" size={17} /> Loading repayments…</td></tr> : payments.length ? payments.map((payment) => <tr key={payment.id}><td>{adminDateTime(payment.paymentDate)}</td><td>{payment.accountName}</td><td>{payment.counterpartyName}</td><td>L-{String(payment.loanId).padStart(4, "0")}</td><td>{payment.kind === "LENT" ? "Lent" : "Borrowed"}</td><td><strong>{adminMoney(payment.amount)}</strong></td></tr>) : <tr><td className="admin-table-empty" colSpan={6}>No repayments recorded.</td></tr>}</tbody>
      </table></div>
    </section>
  );
}

function AuditList({ entries, detailed = false }: { entries: AdminAuditEntry[]; detailed?: boolean }) {
  if (!entries.length) return <p className="admin-empty-copy">No activity recorded yet.</p>;
  return (
    <div className={`admin-audit-list ${detailed ? "detailed" : ""}`}>
      {entries.map((entry) => (
        <article className="admin-audit-row" key={entry.id}>
          <span className="admin-audit-dot"><Activity size={13} /></span>
          <div><strong>{entry.action.replaceAll("_", " ")}</strong><p>{entry.details || entry.targetName || "Administrative event"}</p><small>{entry.actorName}{entry.targetName ? ` · ${entry.targetName}` : ""}</small></div>
          <time>{adminDateTime(entry.createdAt)}</time>
        </article>
      ))}
    </div>
  );
}

function AnnouncementForm({
  canManage,
  busy,
  title,
  message,
  audience,
  channel,
  target,
  onTitleChange,
  onMessageChange,
  onAudienceChange,
  onChannelChange,
  onSubmit,
  onClearTarget,
}: {
  canManage: boolean;
  busy: boolean;
  title: string;
  message: string;
  audience: AnnouncementAudience;
  channel: AnnouncementChannel;
  target?: number;
  onTitleChange: (value: string) => void;
  onMessageChange: (value: string) => void;
  onAudienceChange: (value: AnnouncementAudience) => void;
  onChannelChange: (value: AnnouncementChannel) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onClearTarget: () => void;
}) {
  return (
    <form className="admin-announcement-form" onSubmit={onSubmit}>
      {target !== undefined && <div className="admin-target-notice">Sending to one selected account.<button type="button" onClick={onClearTarget}>Choose an audience</button></div>}
      <label>Audience<select value={audience} disabled={!canManage || target !== undefined} onChange={(event) => onAudienceChange(event.target.value as AnnouncementAudience)}>{(Object.keys(audienceLabels) as AnnouncementAudience[]).map((key) => <option key={key} value={key}>{audienceLabels[key]}</option>)}</select></label>
      <label>Delivery method<select value={channel} disabled={!canManage} onChange={(event) => onChannelChange(event.target.value as AnnouncementChannel)}><option value="IN_APP">{channelLabels.IN_APP}</option><option value="EMAIL">{channelLabels.EMAIL}</option><option value="SMS" disabled>{channelLabels.SMS}</option></select></label>
      <label>Title<input value={title} onChange={(event) => onTitleChange(event.target.value)} maxLength={120} required disabled={!canManage} placeholder="e.g. Scheduled maintenance" /></label>
      <label>Message<textarea value={message} onChange={(event) => onMessageChange(event.target.value)} maxLength={2000} required disabled={!canManage} placeholder="Write a clear message for your audience." /></label>
      <div className="admin-form-hint"><Smartphone size={13} /> SMS delivery is not configured. Choose in-app or email.</div>
      {canManage ? <button className="admin-primary-button" type="submit" disabled={busy || !title.trim() || !message.trim()}>{busy ? <LoaderCircle className="spin" size={14} /> : <Send size={14} />} Send announcement</button> : <p className="admin-form-hint">Support agents can view announcements but cannot send them.</p>}
    </form>
  );
}

function AnnouncementList({ items }: { items: AdminAnnouncement[] }) {
  if (!items.length) return <p className="admin-empty-copy">No announcements have been sent.</p>;
  return (
    <div className="admin-announcement-list">
      {items.map((item) => (
        <article className="admin-announcement-row" key={item.id}>
          <span className="admin-announcement-icon">{item.channel === "EMAIL" ? <Mail size={16} /> : <MessageSquareText size={16} />}</span>
          <div><strong>{item.title}</strong><p>{item.message}</p><small>{item.targetName ? `Direct to ${item.targetName}` : item.audience === "TARGET_USER" ? "Direct message" : audienceLabels[item.audience]} · {channelLabels[item.channel]}</small></div>
          <time>{adminDateTime(item.createdAt)}<small>{item.recipientCount} recipients</small></time>
        </article>
      ))}
    </div>
  );
}

function NotesList({ notes }: { notes: AdminNote[] }) {
  if (!notes.length) return <p className="admin-empty-copy">No internal notes yet.</p>;
  return <div className="admin-notes-list">{notes.map((note) => <article key={note.id}><p>{note.note}</p><small>{note.authorName} · {adminDateTime(note.createdAt)}</small></article>)}</div>;
}

function DetailValue({ label, value }: { label: string; value: string }) {
  return <div className="admin-detail-value"><small>{label}</small><strong>{value}</strong></div>;
}

function ReportCard({
  icon,
  title,
  description,
  onExport,
}: {
  icon: ReactNode;
  title: string;
  description: string;
  onExport: () => void;
}) {
  return <article className="admin-panel admin-report-card"><span className="admin-report-icon">{icon}</span><h2>{title}</h2><p>{description}</p><button className="admin-secondary-button" onClick={onExport}><Download size={14} /> Download CSV</button></article>;
}

function PasswordForm({
  busy,
  forced,
  onSubmit,
  error,
}: {
  busy: boolean;
  forced: boolean;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  error: string;
}) {
  return (
    <form className="admin-modal-form admin-password-form" onSubmit={onSubmit}>
      {!forced && <AdminPasswordInput label="Current password" name="currentPassword" autoComplete="current-password" />}
      <AdminPasswordInput label="New password" name="newPassword" autoComplete="new-password" minLength={8} />
      <AdminPasswordInput label="Confirm new password" name="confirmPassword" autoComplete="new-password" minLength={8} />
      {error && <div className="admin-form-error" role="alert">{error}</div>}
      <div className="admin-modal-actions">{!forced && <span className="admin-form-hint">At least 8 characters</span>}<button className="admin-primary-button" type="submit" disabled={busy}>{busy && <LoaderCircle size={15} className="spin" />} Update password</button></div>
    </form>
  );
}

function AdminPasswordInput({
  label,
  name,
  autoComplete,
  minLength,
}: {
  label: string;
  name: string;
  autoComplete: string;
  minLength?: number;
}) {
  const [visible, setVisible] = useState(false);
  return (
    <div className="admin-password-field">
      <label htmlFor={`admin-${name}`}>{label}</label>
      <span className="admin-password-control">
        <input
          id={`admin-${name}`}
          name={name}
          type={visible ? "text" : "password"}
          autoComplete={autoComplete}
          minLength={minLength}
          required
        />
        <button
          className="admin-password-toggle"
          type="button"
          onClick={() => setVisible((current) => !current)}
          aria-label={`${visible ? "Hide" : "Show"} ${label.toLowerCase()}`}
          title={visible ? "Hide password" : "Show password"}
        >
          {visible ? <EyeOff size={16} /> : <Eye size={16} />}
        </button>
      </span>
    </div>
  );
}

function AdminModalActions({
  busy,
  submitLabel,
  onCancel,
}: {
  busy: boolean;
  submitLabel: string;
  onCancel: () => void;
}) {
  return <div className="admin-modal-actions"><button className="admin-secondary-button" type="button" onClick={onCancel}>Cancel</button><button className="admin-primary-button" type="submit" disabled={busy}>{busy && <LoaderCircle size={15} className="spin" />}{submitLabel}</button></div>;
}

function roleLabel(role: AdminAccountRole) {
  if (role === "ADMIN") return "Administrator";
  if (role === "SUPPORT_AGENT") return "Support agent";
  return "User";
}

function verificationLabel(status: VerificationStatus) {
  if (status === "VERIFIED") return "Verified";
  if (status === "REJECTED") return "Rejected";
  return "Pending";
}

export default AdminWorkspace;
