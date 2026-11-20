import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  ArrowDownLeft,
  ArrowRight,
  ArrowUpRight,
  Activity,
  Bell,
  CalendarDays,
  Check,
  CircleHelp,
  Clock3,
  Eye,
  EyeOff,
  HandCoins,
  LayoutDashboard,
  LoaderCircle,
  LogOut,
  Menu,
  Plus,
  Search,
  ShieldCheck,
  TrendingUp,
  Users,
  UserRound,
  Wallet,
  X,
} from "lucide-react";
import { ApiError, api, clearSession, getSavedUser, saveSession, saveUserProfile } from "./api";
import AdminWorkspace from "./AdminWorkspace";
import type {
  AdminProfile,
  AppNotification,
  AuthResponse,
  DashboardSummary,
  Loan,
  LoanKind,
  LoanStatus,
  UserProfile,
} from "./types";

type User = Pick<AuthResponse, "userId" | "email" | "name">;
type Page = "overview" | "announcements" | "profile" | LoanKind;
type AccessState = "checking" | "admin" | "user" | "error";

const emptySummary: DashboardSummary = {
  totalLent: 0,
  totalBorrowed: 0,
  amountOwedToMe: 0,
  amountIOwe: 0,
  activeLoansLent: 0,
  activeLoansBorrowed: 0,
  overdueLoans: 0,
};

const money = (amount: number) =>
  new Intl.NumberFormat("en-KE", {
    style: "currency",
    currency: "KES",
    maximumFractionDigits: 0,
  }).format(amount || 0);

const shortDate = (date?: string) =>
  date
    ? new Intl.DateTimeFormat("en-KE", {
        day: "numeric",
        month: "short",
        year: "numeric",
      }).format(new Date(date))
    : "No due date";

function App() {
  const [user, setUser] = useState<User | null>(() => getSavedUser());
  const [accessState, setAccessState] = useState<AccessState>(() =>
    getSavedUser() ? "checking" : "user",
  );
  const [adminProfile, setAdminProfile] = useState<AdminProfile | null>(null);
  const [roleCheckAttempt, setRoleCheckAttempt] = useState(0);
  const [page, setPage] = useState<Page>("overview");
  const [summary, setSummary] = useState(emptySummary);
  const [lent, setLent] = useState<Loan[]>([]);
  const [borrowed, setBorrowed] = useState<Loan[]>([]);
  const [loading, setLoading] = useState(false);
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [notificationsLoading, setNotificationsLoading] = useState(false);
  const [notificationsError, setNotificationsError] = useState("");
  const [markingNotificationId, setMarkingNotificationId] = useState<number | null>(null);
  const notificationsRequestRef = useRef(0);
  const notificationsUserRef = useRef<number | null>(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [query, setQuery] = useState("");
  const [globalSearch, setGlobalSearch] = useState("");
  const [modal, setModal] = useState<"loan" | "payment" | null>(null);
  const [selectedLoan, setSelectedLoan] = useState<Loan | null>(null);
  const [selectedKind, setSelectedKind] = useState<LoanKind>("lent");
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const [apiOnline, setApiOnline] = useState<boolean | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [nextSummary, nextLent, nextBorrowed] = await Promise.all([
        api.summary(),
        api.loans("lent"),
        api.loans("borrowed"),
      ]);
      setSummary(nextSummary);
      setLent(nextLent);
      setBorrowed(nextBorrowed);
      setApiOnline(true);
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) {
        clearSession();
        setUser(null);
        setError("Your session has expired. Please sign in again.");
      } else {
        setError(
          caught instanceof Error ? caught.message : "Could not load your loans.",
        );
        setApiOnline(false);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!user) {
      setAccessState("user");
      setAdminProfile(null);
      return;
    }

    let active = true;
    setAccessState("checking");
    api.adminProfile().then((profile) => {
      if (!active) return;
      setAdminProfile(profile);
      setAccessState("admin");
      setError("");
    }).catch((caught: unknown) => {
      if (!active) return;
      if (caught instanceof ApiError && caught.status === 403) {
        setAdminProfile(null);
        setAccessState("user");
        setError("");
      } else if (caught instanceof ApiError && caught.status === 401) {
        clearSession();
        setUser(null);
        setAdminProfile(null);
        setAccessState("user");
        setError("Your session has expired. Please sign in again.");
      } else {
        setError(caught instanceof Error ? caught.message : "Could not verify account access.");
        setAccessState("error");
      }
    });

    return () => {
      active = false;
    };
  }, [user, roleCheckAttempt]);

  useEffect(() => {
    if (user && accessState === "user") void loadData();
  }, [user, accessState, loadData]);

  useEffect(() => {
    if (!notice) return;
    const timeout = window.setTimeout(() => setNotice(""), 3500);
    return () => window.clearTimeout(timeout);
  }, [notice]);

  const allLoans = useMemo(() => [...lent, ...borrowed].sort((a, b) => {
    const aDate = a.dateLent ?? a.dateBorrowed ?? "";
    const bDate = b.dateLent ?? b.dateBorrowed ?? "";
    return bDate.localeCompare(aDate);
  }), [lent, borrowed]);
  const currentLoans = page === "borrowed" ? borrowed : lent;
  const unreadNotifications = notifications.filter((notification) => !notification.read).length;
  const globalMatches = useMemo(() => {
    const needle = globalSearch.trim().toLocaleLowerCase();
    if (!needle) return [];
    return allLoans.filter((loan) =>
      [loan.personName, loan.phoneNumber, loan.status, loan.notes]
        .filter(Boolean)
        .some((value) => value!.toLocaleLowerCase().includes(needle)),
    ).slice(0, 5);
  }, [allLoans, globalSearch]);
  const filteredLoans = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) return currentLoans;
    return currentLoans.filter((loan) =>
      [loan.personName, loan.phoneNumber, loan.status, loan.notes]
        .filter(Boolean)
        .some((value) => value!.toLowerCase().includes(needle)),
    );
  }, [currentLoans, query]);

  async function handleLogin(email: string, password: string) {
    const response = await api.login(email, password);
    acceptSession(response);
  }

  async function handleRegister(input: {
    name: string;
    email: string;
    phoneNumber: string;
    password: string;
    confirmPassword: string;
  }) {
    const response = await api.register(input);
    acceptSession(response);
  }

  function acceptSession(response: AuthResponse) {
    saveSession(response);
    setUser({
      userId: response.userId,
      email: response.email,
      name: response.name,
    });
    setAdminProfile(null);
    setNotifications([]);
    setNotificationsError("");
    setMarkingNotificationId(null);
    setAccessState("checking");
    setError("");
  }

  function handleProfileUpdated(profile: UserProfile) {
    const updatedUser = {
      userId: profile.userId,
      email: profile.email,
      name: profile.name,
    };
    setUser(updatedUser);
    saveUserProfile(updatedUser);
    setNotice("Your profile has been updated.");
  }

  const logout = useCallback((message = "") => {
    clearSession();
    setUser(null);
    setAdminProfile(null);
    setSummary(emptySummary);
    setLent([]);
    setBorrowed([]);
    setNotifications([]);
    setNotificationsError("");
    setMarkingNotificationId(null);
    setPage("overview");
    setAccessState("user");
    setError(message);
  }, []);
  const loadNotifications = useCallback(async () => {
    const requestId = ++notificationsRequestRef.current;
    const requestUserId = user?.userId;
    if (requestUserId === undefined) return;
    setNotificationsLoading(true);
    setNotificationsError("");
    try {
      const nextNotifications = await api.notifications();
      if (
        requestId === notificationsRequestRef.current &&
        notificationsUserRef.current === requestUserId
      ) {
        setNotifications(nextNotifications);
      }
    } catch (caught) {
      if (
        requestId !== notificationsRequestRef.current ||
        notificationsUserRef.current !== requestUserId
      ) {
        return;
      }
      if (caught instanceof ApiError && caught.status === 401) {
        logout("Your session has expired. Please sign in again.");
      } else {
        setNotificationsError(
          caught instanceof Error ? caught.message : "Could not load announcements.",
        );
      }
    } finally {
      if (
        requestId === notificationsRequestRef.current &&
        notificationsUserRef.current === requestUserId
      ) {
        setNotificationsLoading(false);
      }
    }
  }, [logout, user?.userId]);
  useEffect(() => {
    if (!user || accessState !== "user") {
      notificationsUserRef.current = null;
      notificationsRequestRef.current += 1;
      setNotificationsLoading(false);
      return;
    }
    notificationsUserRef.current = user.userId;
    void loadNotifications();
    const interval = window.setInterval(() => void loadNotifications(), 30_000);
    return () => {
      window.clearInterval(interval);
      if (notificationsUserRef.current === user.userId) {
        notificationsUserRef.current = null;
      }
      notificationsRequestRef.current += 1;
    };
  }, [user, accessState, loadNotifications]);
  const handleAdminSessionExpired = useCallback(
    () => logout("Your admin session has expired. Please sign in again."),
    [logout],
  );
  const handleSignOut = useCallback(() => logout(), [logout]);

  function openLoanForm(kind: LoanKind) {
    setSelectedKind(kind);
    setModal("loan");
  }

  async function createLoan(values: {
    personName: string;
    phoneNumber: string;
    counterpartyEmail: string;
    amount: number;
    date: string;
    dueDate: string;
    notes: string;
  }) {
    const request = {
      personName: values.personName,
      phoneNumber: values.phoneNumber,
      ...(values.counterpartyEmail ? { counterpartyEmail: values.counterpartyEmail } : {}),
      ...(selectedKind === "lent"
        ? { amountLent: values.amount, dateLent: values.date }
        : { amountBorrowed: values.amount, dateBorrowed: values.date }),
      ...(values.dueDate ? { dueDate: values.dueDate } : {}),
      ...(values.notes ? { notes: values.notes } : {}),
    };
    await api.createLoan(selectedKind, request);
    setModal(null);
    setNotice(
      `Loan ${selectedKind === "lent" ? "lent" : "borrowed"} added successfully.`,
    );
    await loadData();
  }

  async function recordPayment(amount: number) {
    if (!selectedLoan) return;
    await api.recordPayment(selectedKind, selectedLoan.id, amount);
    setModal(null);
    setSelectedLoan(null);
    setNotice("Payment recorded successfully.");
    await loadData();
  }

  async function deleteLoan(loan: Loan, kind: LoanKind) {
    const confirmed = window.confirm(
      `Delete the loan with ${loan.personName}? This cannot be undone.`,
    );
    if (!confirmed) return;
    try {
      await api.deleteLoan(kind, loan.id);
      setNotice("Loan deleted.");
      await loadData();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Could not delete loan.");
    }
  }

  async function markNotificationRead(notification: AppNotification) {
    if (notification.read || markingNotificationId === notification.id) return;
    const requestUserId = user?.userId;
    if (requestUserId === undefined) return;
    setMarkingNotificationId(notification.id);
    setNotificationsError("");
    try {
      await api.markNotificationRead(notification.id);
      if (notificationsUserRef.current === requestUserId) {
        setNotifications((current) =>
          current.map((item) => item.id === notification.id ? { ...item, read: true } : item),
        );
      }
    } catch (caught) {
      if (notificationsUserRef.current !== requestUserId) return;
      if (caught instanceof ApiError && caught.status === 401) {
        logout("Your session has expired. Please sign in again.");
      } else {
        setNotificationsError(
          caught instanceof Error ? caught.message : "Could not update this announcement.",
        );
      }
    } finally {
      if (notificationsUserRef.current === requestUserId) {
        setMarkingNotificationId(null);
      }
    }
  }

  function openPayment(loan: Loan, kind: LoanKind) {
    setSelectedLoan(loan);
    setSelectedKind(kind);
    setModal("payment");
  }

  function openLoanSearchResult(loan: Loan) {
    const kind: LoanKind = loan.dateBorrowed ? "borrowed" : "lent";
    setQuery(loan.personName);
    setPage(kind);
    setGlobalSearch("");
  }

  if (!user) {
    return (
      <AuthScreen
        error={error}
        onClearError={() => setError("")}
        onLogin={handleLogin}
        onRegister={handleRegister}
      />
    );
  }

  if (accessState === "checking") {
    return (
      <div className="access-check-screen" role="status">
        <LoaderCircle className="spin" size={22} />
        <span>Checking your account permissions…</span>
      </div>
    );
  }

  if (accessState === "error") {
    return (
      <div className="access-check-screen">
        <div className="access-check-card">
          <ShieldCheck size={24} />
          <h1>Could not verify access</h1>
          <p>{error}</p>
          <div>
            <button className="button button-outline" onClick={() => {
              setError("");
              setRoleCheckAttempt((attempt) => attempt + 1);
            }}>Try again</button>
            <button className="button button-primary" onClick={() => logout()}>Sign out</button>
          </div>
        </div>
      </div>
    );
  }

  if (accessState === "admin" && adminProfile) {
    return (
      <AdminWorkspace
        profile={adminProfile}
        onProfileChange={setAdminProfile}
        onSessionExpired={handleAdminSessionExpired}
        onSignOut={handleSignOut}
      />
    );
  }

  const displayName = user.name?.trim() || user.email.split("@")[0];

  return (
    <div className="app-shell">
      {mobileNavOpen && (
        <button
          className="mobile-backdrop"
          aria-label="Close navigation"
          onClick={() => setMobileNavOpen(false)}
        />
      )}
      <aside className={`sidebar ${mobileNavOpen ? "sidebar-open" : ""}`}>
        <a className="brand" href="#" onClick={(event) => event.preventDefault()}>
          <span className="brand-mark"><HandCoins size={21} /></span>
          <span>Kumbukaa<span className="brand-dot">.</span></span>
        </a>
        <div className="workspace-label">WORKSPACE</div>
        <nav className="side-nav" aria-label="Main navigation">
          <button
            className={`nav-item ${page === "overview" ? "nav-active" : ""}`}
            onClick={() => {
              setPage("overview");
              setMobileNavOpen(false);
            }}
          >
            <LayoutDashboard size={18} /> Overview
          </button>
          <button
            className={`nav-item ${page === "lent" ? "nav-active" : ""}`}
            onClick={() => {
              setPage("lent");
              setMobileNavOpen(false);
            }}
          >
            <ArrowUpRight size={18} /> Money lent
            <span className="nav-count">{lent.length}</span>
          </button>
          <button
            className={`nav-item ${page === "borrowed" ? "nav-active" : ""}`}
            onClick={() => {
              setPage("borrowed");
              setMobileNavOpen(false);
            }}
          >
            <ArrowDownLeft size={18} /> Money borrowed
            <span className="nav-count">{borrowed.length}</span>
          </button>
          <button
            className={`nav-item ${page === "announcements" ? "nav-active" : ""}`}
            onClick={() => {
              setPage("announcements");
              setMobileNavOpen(false);
            }}
          >
            <Bell size={18} /> Announcements
            {unreadNotifications > 0 && <span className="nav-count">{unreadNotifications}</span>}
          </button>
          <button
            className={`nav-item ${page === "profile" ? "nav-active" : ""}`}
            onClick={() => {
              setPage("profile");
              setMobileNavOpen(false);
            }}
          >
            <UserRound size={18} /> My profile
          </button>
        </nav>
        <div className="sidebar-bottom">
          <div className={`connection ${apiOnline === false ? "connection-offline" : ""}`}>
            <span className="connection-dot" />
            <span>{apiOnline === false ? "API unavailable" : "Secure workspace"}</span>
            <ShieldCheck size={16} />
          </div>
          <div className="profile-card">
            <div className="avatar">{displayName.slice(0, 1).toUpperCase()}</div>
            <div className="profile-info">
              <strong>{displayName}</strong>
              <span>{user.email}</span>
            </div>
            <button className="icon-button profile-logout" onClick={() => logout()} aria-label="Sign out" title="Sign out">
              <LogOut size={17} />
            </button>
          </div>
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <button
            className="icon-button menu-button"
            onClick={() => setMobileNavOpen(true)}
            aria-label="Open navigation"
          >
            <Menu size={20} />
          </button>
          <div className="breadcrumb">
            <span>Workspace</span><span className="crumb-slash">/</span>
            <strong>{pageTitle(page)}</strong>
          </div>
          <div className="global-search-wrap">
            <label className="global-search">
              <Search size={15} />
              <input
                value={globalSearch}
                onChange={(event) => setGlobalSearch(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Escape") setGlobalSearch("");
                  if (event.key === "Enter" && globalMatches[0]) {
                    openLoanSearchResult(globalMatches[0]);
                  }
                }}
                placeholder="Search loans and people"
                aria-label="Search loans and people"
                aria-expanded={Boolean(globalSearch.trim())}
                aria-controls="global-search-results"
              />
              {globalSearch && (
                <button
                  type="button"
                  className="search-clear"
                  onClick={() => setGlobalSearch("")}
                  aria-label="Clear global search"
                >
                  <X size={14} />
                </button>
              )}
            </label>
            {globalSearch.trim() && (
              <div className="global-search-results" id="global-search-results" role="listbox">
                {globalMatches.length ? globalMatches.map((loan) => {
                  const kind: LoanKind = loan.dateBorrowed ? "borrowed" : "lent";
                  return (
                    <button
                      type="button"
                      role="option"
                      aria-selected="false"
                      key={`${kind}-${loan.id}`}
                      onClick={() => openLoanSearchResult(loan)}
                    >
                      <span className="global-search-avatar">{loan.personName.slice(0, 1).toUpperCase()}</span>
                      <span className="global-search-copy">
                        <strong>{loan.personName}</strong>
                        <small>{kind === "lent" ? "Money lent" : "Money borrowed"} · {loan.status.replace("_", " ")}</small>
                      </span>
                      <span className="global-search-balance">{money(loan.balance)}</span>
                    </button>
                  );
                }) : (
                  <div className="global-search-empty">No matching loans found.</div>
                )}
              </div>
            )}
          </div>
          <div className="topbar-actions">
            <span className="today-label">{new Intl.DateTimeFormat("en-KE", {
              weekday: "short", day: "numeric", month: "short",
            }).format(new Date())}</span>
            <button
              className="icon-button notification-button"
              aria-label={`Announcements${unreadNotifications ? `, ${unreadNotifications} unread` : ""}`}
              title="Announcements"
              onClick={() => setPage("announcements")}
            >
              <Bell size={18} />
              {unreadNotifications > 0 && <span className="notification-badge">{unreadNotifications}</span>}
            </button>
            <button className="icon-button notification-button" aria-label="Help" title="Kumbukaa loan tracker">
              <CircleHelp size={18} />
            </button>
            <div className="avatar avatar-small">{displayName.slice(0, 1).toUpperCase()}</div>
          </div>
        </header>

        <div className="page-content">
          {error && <div className="alert alert-error" role="alert">{error}</div>}
          {notice && <div className="toast" role="status"><Check size={17} />{notice}</div>}
          {page === "overview" ? (
            <Overview
              summary={summary}
              loans={allLoans}
              notifications={notifications}
              notificationsLoading={notificationsLoading}
              notificationsError={notificationsError}
              userName={displayName}
              loading={loading}
              onAdd={openLoanForm}
              onNavigate={setPage}
              onPayment={openPayment}
              onMarkNotificationRead={(notification) => void markNotificationRead(notification)}
              onRefreshNotifications={() => void loadNotifications()}
              onViewPerson={(name, kind) => {
                setQuery(name);
                setPage(kind);
              }}
            />
          ) : page === "announcements" ? (
            <AnnouncementPage
              notifications={notifications}
              loading={notificationsLoading}
              error={notificationsError}
              markingNotificationId={markingNotificationId}
              onRefresh={() => void loadNotifications()}
              onMarkRead={(notification) => void markNotificationRead(notification)}
            />
          ) : page === "profile" ? (
            <ProfilePage
              user={user}
              onSaved={handleProfileUpdated}
            />
          ) : (
            <LoanList
              kind={page}
              loans={filteredLoans}
              loading={loading}
              query={query}
              onQueryChange={setQuery}
              onAdd={() => openLoanForm(page)}
              onPayment={(loan) => openPayment(loan, page)}
              onDelete={(loan) => void deleteLoan(loan, page)}
              onRefresh={() => void loadData()}
            />
          )}
        </div>
      </main>

      {modal === "loan" && (
        <LoanModal
          kind={selectedKind}
          onKindChange={setSelectedKind}
          onClose={() => setModal(null)}
          onSubmit={createLoan}
        />
      )}
      {modal === "payment" && selectedLoan && (
        <PaymentModal
          loan={selectedLoan}
          onClose={() => {
            setModal(null);
            setSelectedLoan(null);
          }}
          onSubmit={recordPayment}
        />
      )}
    </div>
  );
}

function pageTitle(page: Page) {
  if (page === "lent") return "Money lent";
  if (page === "borrowed") return "Money borrowed";
  if (page === "announcements") return "Announcements";
  if (page === "profile") return "My profile";
  return "Overview";
}

function AnnouncementPage({
  notifications,
  loading,
  error,
  markingNotificationId,
  onRefresh,
  onMarkRead,
}: {
  notifications: AppNotification[];
  loading: boolean;
  error: string;
  markingNotificationId: number | null;
  onRefresh: () => void;
  onMarkRead: (notification: AppNotification) => void;
}) {
  return (
    <>
      <div className="welcome-row">
        <div>
          <div className="eyebrow"><span className="eyebrow-line" /> UPDATES FROM KUMBUKAA</div>
          <h1>Announcements</h1>
          <p className="page-subtitle">Important messages sent to your account.</p>
        </div>
        <button className="button button-outline" onClick={onRefresh} disabled={loading}>
          {loading ? <LoaderCircle className="spin" size={16} /> : <Bell size={16} />}
          Refresh
        </button>
      </div>

      {error && <div className="alert alert-error" role="alert">{error}</div>}
      {loading && notifications.length === 0 && (
        <div className="loading-state" role="status">
          <LoaderCircle className="spin" size={18} /> Loading announcements…
        </div>
      )}
      {!loading && !error && notifications.length === 0 && (
        <div className="empty-state announcement-empty">
          <div className="empty-icon"><Bell size={20} /></div>
          <strong>No announcements yet</strong>
          <p>Messages from Kumbukaa will appear here.</p>
        </div>
      )}
      <div className="announcement-list">
        {notifications.map((notification) => (
          <article
            className={`announcement-card ${notification.read ? "" : "announcement-unread"}`}
            key={notification.id}
          >
            <div className="announcement-card-icon"><Bell size={18} /></div>
            <div className="announcement-card-content">
              <div className="announcement-card-heading">
                <h2>{notification.title}</h2>
                {!notification.read && <span className="announcement-new">New</span>}
              </div>
              <time dateTime={notification.createdAt}>
                {new Intl.DateTimeFormat("en-KE", {
                  day: "numeric",
                  month: "short",
                  year: "numeric",
                  hour: "numeric",
                  minute: "2-digit",
                }).format(new Date(notification.createdAt))}
              </time>
              <p>{notification.message}</p>
            </div>
            {notification.read ? (
              <span className="announcement-read"><Check size={14} /> Read</span>
            ) : (
              <button
                className="button button-outline announcement-read-button"
                onClick={() => onMarkRead(notification)}
                disabled={markingNotificationId === notification.id}
              >
                {markingNotificationId === notification.id
                  ? <LoaderCircle className="spin" size={14} />
                  : <Check size={14} />}
                Mark as read
              </button>
            )}
          </article>
        ))}
      </div>
    </>
  );
}

function Overview({
  summary,
  loans,
  notifications,
  notificationsLoading,
  notificationsError,
  userName,
  loading,
  onAdd,
  onNavigate,
  onPayment,
  onMarkNotificationRead,
  onRefreshNotifications,
  onViewPerson,
}: {
  summary: DashboardSummary;
  loans: Loan[];
  notifications: AppNotification[];
  notificationsLoading: boolean;
  notificationsError: string;
  userName: string;
  loading: boolean;
  onAdd: (kind: LoanKind) => void;
  onNavigate: (page: Page) => void;
  onPayment: (loan: Loan, kind: LoanKind) => void;
  onMarkNotificationRead: (notification: AppNotification) => void;
  onRefreshNotifications: () => void;
  onViewPerson: (name: string, kind: LoanKind) => void;
}) {
  const recent = loans.slice(0, 5);
  const allDuePayments = getUpcomingPayments(loans);
  const duePayments = allDuePayments.slice(0, 5);
  const recentActivity = getRecentActivity(loans).slice(0, 5);
  const people = getPeopleYouDealWith(loans).slice(0, 5);
  const announcements = notifications.slice(0, 2);
  const outstanding = allDuePayments.filter((item) => item.daysUntilDue <= 7);
  const lentPaid = Math.max(0, summary.totalLent - summary.amountOwedToMe);
  const borrowedPaid = Math.max(0, summary.totalBorrowed - summary.amountIOwe);

  return (
    <>
      <div className="welcome-row">
        <div>
          <div className="eyebrow"><span className="eyebrow-line" /> YOUR MONEY, IN PERSPECTIVE</div>
          <h1>Good to see you, {userName}<span className="heading-period">.</span></h1>
          <p className="page-subtitle">A clear view of what’s owed to you and what you owe.</p>
        </div>
        <button className="button button-primary" onClick={() => onAdd("lent")}>
          <Plus size={17} /> Add a loan
        </button>
      </div>

      {outstanding.length > 0 && (
        <section className="dashboard-reminder" aria-label="Upcoming payment reminder">
          <span className="dashboard-reminder-icon"><CalendarDays size={18} /></span>
          <div className="dashboard-reminder-copy">
            <strong>
              {outstanding.length} {outstanding.length === 1 ? "payment" : "payments"}{" "}
              {outstanding.some((item) => item.daysUntilDue < 0) ? "need attention" : "due soon"}
            </strong>
            <span>
              {outstanding[0].loan.personName} · {outstanding[0].daysUntilDue < 0
                ? `${Math.abs(outstanding[0].daysUntilDue)} days overdue`
                : outstanding[0].daysUntilDue === 0
                  ? "Due today"
                  : `Due in ${outstanding[0].daysUntilDue} days`}
            </span>
          </div>
          <button
            className="button button-outline"
            onClick={() => onNavigate(outstanding[0].kind)}
          >
            Review payments <ArrowRight size={14} />
          </button>
        </section>
      )}

      <section className="stats-grid" aria-label="Loan summary">
        <StatCard
          icon={<Wallet size={19} />}
          label="Total lent"
          value={money(summary.totalLent)}
          sub={`${money(summary.amountOwedToMe)} still owed to you`}
          tone="mint"
        />
        <StatCard
          icon={<ArrowDownLeft size={19} />}
          label="Total borrowed"
          value={money(summary.totalBorrowed)}
          sub={`${money(summary.amountIOwe)} left to repay`}
          tone="lavender"
        />
        <StatCard
          icon={<Clock3 size={19} />}
          label="Active loans"
          value={String(summary.activeLoansLent + summary.activeLoansBorrowed)}
          sub={`${summary.activeLoansLent} lent · ${summary.activeLoansBorrowed} borrowed`}
          tone="blue"
        />
        <StatCard
          icon={<Bell size={19} />}
          label="Overdue"
          value={String(summary.overdueLoans)}
          sub={summary.overdueLoans ? "Loans past their due date" : "You’re all caught up"}
          tone={summary.overdueLoans ? "peach" : "mint"}
        />
      </section>

      <section className="overview-lower">
        <div className="panel recent-panel">
          <div className="panel-heading">
            <div>
              <h2>Your loans</h2>
              <p>Recent lending and borrowing activity</p>
            </div>
            <button className="text-button" onClick={() => onNavigate("lent")}>
              View lent <ArrowRight size={15} />
            </button>
          </div>
          {loading && loans.length === 0 ? (
            <LoadingRows />
          ) : recent.length ? (
            <div className="recent-list">
              {recent.map((loan) => {
                const kind: LoanKind = loan.dateBorrowed ? "borrowed" : "lent";
                return (
                  <div className="recent-row" key={`${kind}-${loan.id}`}>
                    <div className={`loan-avatar ${kind === "borrowed" ? "loan-avatar-borrowed" : ""}`}>
                      {kind === "borrowed" ? <ArrowDownLeft size={17} /> : <ArrowUpRight size={17} />}
                    </div>
                    <div className="recent-person">
                      <strong>{loan.personName}</strong>
                      <span>{kind === "lent" ? "You lent" : "You borrowed"} · {shortDate(loan.dateLent ?? loan.dateBorrowed)}</span>
                    </div>
                    <div className="recent-amount">
                      <strong>{money(loan.loanAmount)}</strong>
                      <span>{kind === "lent" ? "lent" : "borrowed"}</span>
                    </div>
                    <StatusBadge status={loan.status} />
                    {loan.balance > 0 && (
                      <button className="row-action" onClick={() => onPayment(loan, kind)}>
                        Record payment
                      </button>
                    )}
                  </div>
                );
              })}
            </div>
          ) : (
            <EmptyState
              title="Your story starts here"
              description="Add a loan to see your activity and keep track of repayments."
              action="Add your first loan"
              onAction={() => onAdd("lent")}
            />
          )}
        </div>

        <div className="panel snapshot-panel">
          <div className="panel-heading">
            <div>
              <h2>At a glance</h2>
              <p>Your lending balance</p>
            </div>
            <span className="snapshot-icon"><HandCoins size={18} /></span>
          </div>
          <div className="balance-block">
            <span>Net balance</span>
            <strong className={summary.amountOwedToMe - summary.amountIOwe < 0 ? "negative-value" : ""}>
              {money(summary.amountOwedToMe - summary.amountIOwe)}
            </strong>
            <small>What’s owed to you, less what you owe</small>
          </div>
          <div className="snapshot-divider" />
          <button className="snapshot-link" onClick={() => onNavigate("lent")}>
            <span><span className="mini-dot mini-mint" /> Outstanding to you</span>
            <strong>{money(summary.amountOwedToMe)} <ArrowRight size={15} /></strong>
          </button>
          <button className="snapshot-link" onClick={() => onNavigate("borrowed")}>
            <span><span className="mini-dot mini-lavender" /> Your repayments</span>
            <strong>{money(summary.amountIOwe)} <ArrowRight size={15} /></strong>
          </button>
          <div className="snapshot-footer">
            <div className="tiny-avatars"><span>↗</span><span>↙</span><span>✓</span></div>
            <span>{totalActive(summary)} active loans being tracked</span>
          </div>
        </div>
      </section>

      <section className="dashboard-two-column">
        <div className="panel dashboard-panel">
          <div className="panel-heading">
            <div>
              <h2>Upcoming payments</h2>
              <p>Due dates and outstanding balances</p>
            </div>
            <button className="text-button" onClick={() => onNavigate("borrowed")}>
              View loans <ArrowRight size={15} />
            </button>
          </div>
          {loading && loans.length === 0 ? (
            <LoadingRows />
          ) : duePayments.length ? (
            <div className="dashboard-list">
              {duePayments.map(({ loan, kind, daysUntilDue }) => (
                <div className="dashboard-list-row" key={`due-${kind}-${loan.id}`}>
                  <span className={`dashboard-row-icon ${daysUntilDue < 0 ? "dashboard-row-overdue" : ""}`}>
                    <CalendarDays size={16} />
                  </span>
                  <div className="dashboard-row-main">
                    <strong>{loan.personName}</strong>
                    <span>{kind === "borrowed" ? "You owe" : "Expected from"} · {shortDate(loan.dueDate)}</span>
                  </div>
                  <div className="dashboard-row-value">
                    <strong>{money(loan.balance)}</strong>
                    <span className={daysUntilDue < 0 ? "dashboard-overdue-label" : ""}>
                      {daysUntilDue < 0
                        ? `${Math.abs(daysUntilDue)}d overdue`
                        : daysUntilDue === 0 ? "Due today" : `In ${daysUntilDue}d`}
                    </span>
                  </div>
                  <button
                    className="row-action"
                    onClick={() => onPayment(loan, kind)}
                    aria-label={`${kind === "borrowed" ? "Record repayment" : "Record payment"} for ${loan.personName}`}
                  >
                    {kind === "borrowed" ? "Repay" : "Record"}
                  </button>
                </div>
              ))}
            </div>
          ) : (
            <div className="dashboard-empty">
              <CalendarDays size={18} />
              <span>No outstanding payments are due in the next 30 days.</span>
            </div>
          )}
        </div>

        <div className="panel dashboard-panel">
          <div className="panel-heading">
            <div>
              <h2>Recent activity</h2>
              <p>Your latest loans and recorded repayments</p>
            </div>
            <Activity size={17} className="dashboard-heading-icon" />
          </div>
          {loading && loans.length === 0 ? (
            <LoadingRows />
          ) : recentActivity.length ? (
            <div className="dashboard-list">
              {recentActivity.map((item) => (
                <button
                  className="activity-row"
                  key={item.id}
                  onClick={() => onNavigate(item.kind)}
                >
                  <span className={`activity-dot ${item.type === "payment" ? "activity-dot-payment" : ""}`} />
                  <span className="activity-copy">
                    <strong>{item.type === "payment" ? "Payment recorded" : "Loan added"}</strong>
                    <small>{item.personName} · {item.kind === "lent" ? "Money lent" : "Money borrowed"}</small>
                  </span>
                  <span className="activity-amount">{money(item.amount)}</span>
                  <time>{shortDate(item.date)}</time>
                </button>
              ))}
            </div>
          ) : (
            <div className="dashboard-empty">
              <Activity size={18} />
              <span>Your loan and repayment activity will appear here.</span>
            </div>
          )}
        </div>
      </section>

      <section className="panel analytics-panel">
        <div className="panel-heading">
          <div>
            <h2>Loan analytics</h2>
            <p>Repayment progress across your current portfolio</p>
          </div>
          <TrendingUp size={18} className="dashboard-heading-icon" />
        </div>
        <PortfolioProgress
          label="Money lent"
          principal={summary.totalLent}
          outstanding={summary.amountOwedToMe}
          paid={lentPaid}
          tone="mint"
        />
        <PortfolioProgress
          label="Money borrowed"
          principal={summary.totalBorrowed}
          outstanding={summary.amountIOwe}
          paid={borrowedPaid}
          tone="lavender"
        />
        <div className="analytics-totals">
          <span><strong>{loans.length}</strong> loans tracked</span>
          <span><strong>{money(summary.amountOwedToMe + summary.amountIOwe)}</strong> total outstanding</span>
          <span><strong>{money(lentPaid + borrowedPaid)}</strong> repaid</span>
        </div>
      </section>

      <section className="dashboard-two-column">
        <div className="panel dashboard-panel">
          <div className="panel-heading">
            <div>
              <h2>People you deal with</h2>
              <p>Contacts connected to your loans</p>
            </div>
            <Users size={18} className="dashboard-heading-icon" />
          </div>
          {people.length ? (
            <div className="dashboard-list">
              {people.map((person) => (
                <div className="person-row" key={person.key}>
                  <span className="person-avatar">{person.name.slice(0, 1).toUpperCase()}</span>
                  <div className="dashboard-row-main">
                    <strong>{person.name}</strong>
                    <span>{person.loanCount} {person.loanCount === 1 ? "loan" : "loans"} · {money(person.balance)} outstanding</span>
                  </div>
                  <button
                    className="text-button"
                    onClick={() => onViewPerson(person.name, person.kind)}
                  >
                    View <ArrowRight size={14} />
                  </button>
                </div>
              ))}
            </div>
          ) : (
            <div className="dashboard-empty">
              <Users size={18} />
              <span>People from your loans will appear here.</span>
            </div>
          )}
        </div>

        <div className="panel dashboard-panel">
          <div className="panel-heading">
            <div>
              <h2>Announcements</h2>
              <p>Updates sent to your account</p>
            </div>
            <button className="text-button" onClick={() => onNavigate("announcements")}>
              View all <ArrowRight size={15} />
            </button>
          </div>
          {notificationsLoading && !notifications.length ? (
            <LoadingRows />
          ) : notificationsError ? (
            <div className="dashboard-empty dashboard-empty-error">
              <span>{notificationsError}</span>
              <button className="text-button" onClick={onRefreshNotifications}>
                Try again <ArrowRight size={14} />
              </button>
            </div>
          ) : announcements.length ? (
            <div className="dashboard-list">
              {announcements.map((announcement) => (
                <div className="announcement-preview" key={announcement.id}>
                  <button
                    className="announcement-preview-copy"
                    onClick={() => onNavigate("announcements")}
                  >
                    <span className="announcement-preview-heading">
                      <strong>{announcement.title}</strong>
                      {!announcement.read && <span className="announcement-new">New</span>}
                    </span>
                    <span>{announcement.message}</span>
                    <time>{shortDate(announcement.createdAt)}</time>
                  </button>
                  {!announcement.read && (
                    <button
                      className="announcement-quick-read"
                      onClick={() => onMarkNotificationRead(announcement)}
                      aria-label={`Mark ${announcement.title} as read`}
                      title="Mark as read"
                    >
                      <Check size={14} />
                    </button>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <div className="dashboard-empty">
              <Bell size={18} />
              <span>No announcements yet. New updates will appear here.</span>
              <button className="text-button" onClick={() => onNavigate("announcements")}>
                Open announcements <ArrowRight size={14} />
              </button>
            </div>
          )}
        </div>
      </section>

      <div className="footnote"><ShieldCheck size={14} /> Your financial details are private to your account.</div>
    </>
  );
}

function getUpcomingPayments(loans: Loan[]) {
  const today = localDateKey(new Date());
  return loans
    .filter((loan) => loan.balance > 0 && loan.dueDate)
    .map((loan) => {
      const kind: LoanKind = loan.dateBorrowed ? "borrowed" : "lent";
      const dueDate = loan.dueDate ?? "";
      const daysUntilDue = Math.round(
        (Date.parse(`${dueDate.slice(0, 10)}T00:00:00Z`) -
          Date.parse(`${today}T00:00:00Z`)) / 86_400_000,
      );
      return { loan, kind, daysUntilDue };
    })
    .filter((item) => Number.isFinite(item.daysUntilDue) && item.daysUntilDue <= 30)
    .sort((a, b) => a.daysUntilDue - b.daysUntilDue);
}

function localDateKey(date: Date) {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return localDate.toISOString().slice(0, 10);
}

type LoanActivityItem = {
  id: string;
  type: "loan" | "payment";
  kind: LoanKind;
  personName: string;
  amount: number;
  date: string;
};

function getRecentActivity(loans: Loan[]) {
  return loans.flatMap((loan) => {
    const kind: LoanKind = loan.dateBorrowed ? "borrowed" : "lent";
    const startDate = loan.dateLent ?? loan.dateBorrowed;
    const entries: LoanActivityItem[] = startDate
      ? [{
          id: `${kind}-${loan.id}-created`,
          type: "loan",
          kind,
          personName: loan.personName,
          amount: loan.loanAmount,
          date: startDate,
        }]
      : [];
    (loan.installments ?? []).forEach((payment, index) => {
      if (payment.paymentDate) {
        entries.push({
          id: `${kind}-${loan.id}-payment-${index}`,
          type: "payment",
          kind,
          personName: loan.personName,
          amount: payment.amount,
          date: payment.paymentDate,
        });
      }
    });
    return entries;
  }).sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
}

function getPeopleYouDealWith(loans: Loan[]) {
  const people = new Map<string, {
    key: string;
    name: string;
    loanCount: number;
    balance: number;
    kind: LoanKind;
    latestDate: string;
  }>();
  loans.forEach((loan) => {
    const name = loan.personName?.trim() || "Unknown contact";
    const key = loan.phoneNumber?.trim().toLocaleLowerCase()
      || loan.counterpartyEmail?.trim().toLocaleLowerCase()
      || name.toLocaleLowerCase();
    const kind: LoanKind = loan.dateBorrowed ? "borrowed" : "lent";
    const date = loan.dateLent ?? loan.dateBorrowed ?? "";
    const person = people.get(key) ?? {
      key,
      name,
      loanCount: 0,
      balance: 0,
      kind,
      latestDate: date,
    };
    person.loanCount += 1;
    person.balance += loan.balance || 0;
    if (date > person.latestDate) {
      person.latestDate = date;
      person.kind = kind;
    }
    people.set(key, person);
  });
  return [...people.values()].sort((a, b) => b.balance - a.balance || b.latestDate.localeCompare(a.latestDate));
}

function PortfolioProgress({
  label,
  principal,
  outstanding,
  paid,
  tone,
}: {
  label: string;
  principal: number;
  outstanding: number;
  paid: number;
  tone: "mint" | "lavender";
}) {
  const paidPercent = principal > 0 ? Math.min(100, Math.max(0, (paid / principal) * 100)) : 0;
  return (
    <div className="portfolio-progress">
      <div className="portfolio-progress-heading">
        <strong>{label}</strong>
        <span>{money(principal)} total</span>
      </div>
      <div
        className={`portfolio-progress-track ${tone}`}
        role="progressbar"
        aria-label={`${label} repayment progress`}
        aria-valuenow={Math.round(paidPercent)}
        aria-valuemin={0}
        aria-valuemax={100}
      >
        <span style={{ width: `${paidPercent}%` }} />
      </div>
      <div className="portfolio-progress-footer">
        <span>{Math.round(paidPercent)}% repaid · {money(paid)} paid</span>
        <strong>{money(outstanding)} outstanding</strong>
      </div>
    </div>
  );
}

function totalActive(summary: DashboardSummary) {
  return summary.activeLoansLent + summary.activeLoansBorrowed;
}

function StatCard({
  icon,
  label,
  value,
  sub,
  tone,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  sub: string;
  tone: string;
}) {
  return (
    <div className="stat-card">
      <div className="stat-top">
        <span className={`stat-icon ${tone}`}>{icon}</span>
        <span className="stat-label">{label}</span>
      </div>
      <strong className="stat-value">{value}</strong>
      <span className="stat-sub">{sub}</span>
    </div>
  );
}

function LoanList({
  kind,
  loans,
  loading,
  query,
  onQueryChange,
  onAdd,
  onPayment,
  onDelete,
  onRefresh,
}: {
  kind: LoanKind;
  loans: Loan[];
  loading: boolean;
  query: string;
  onQueryChange: (value: string) => void;
  onAdd: () => void;
  onPayment: (loan: Loan) => void;
  onDelete: (loan: Loan) => void;
  onRefresh: () => void;
}) {
  const isLent = kind === "lent";
  return (
    <>
      <div className="welcome-row loans-welcome">
        <div>
          <div className="eyebrow"><span className="eyebrow-line" /> LOAN REGISTER</div>
          <h1>Money {isLent ? "lent" : "borrowed"}<span className="heading-period">.</span></h1>
          <p className="page-subtitle">
            {isLent
              ? "Keep every promise and repayment in one place."
              : "Stay on top of what you owe and when it’s due."}
          </p>
        </div>
        <button className="button button-primary" onClick={onAdd}>
          <Plus size={17} /> Add {isLent ? "lent" : "borrowed"} loan
        </button>
      </div>

      <div className="list-toolbar">
        <div className="list-tabs">
          <span className="list-tab list-tab-active">All loans <span>{loans.length}</span></span>
        </div>
        <div className="toolbar-actions">
          <label className="search-field">
            <Search size={16} />
            <input
              value={query}
              onChange={(event) => onQueryChange(event.target.value)}
              placeholder="Search loans"
              aria-label="Search loans"
            />
            {query && <button className="search-clear" onClick={() => onQueryChange("")} aria-label="Clear search"><X size={14} /></button>}
          </label>
          <button className="button button-outline" onClick={onRefresh} disabled={loading}>
            {loading ? <LoaderCircle size={16} className="spin" /> : <span className="refresh-glyph">↻</span>}
            Refresh
          </button>
        </div>
      </div>

      <div className="panel table-panel">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>{isLent ? "BORROWER" : "LENDER"}</th>
                <th>AMOUNT</th>
                <th>REMAINING</th>
                <th>DATE</th>
                <th>DUE DATE</th>
                <th>STATUS</th>
                <th><span className="visually-hidden">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              {loading && loans.length === 0 ? (
                <tr><td colSpan={7}><LoadingRows /></td></tr>
              ) : loans.length ? (
                loans.map((loan) => (
                  <tr key={loan.id}>
                    <td>
                      <div className="table-person">
                        <span className={`table-avatar ${isLent ? "" : "table-avatar-alt"}`}>
                          {loan.personName.slice(0, 1).toUpperCase()}
                        </span>
                        <span><strong>{loan.personName}</strong><small>{loan.phoneNumber}</small></span>
                      </div>
                    </td>
                    <td><strong className="table-money">{money(loan.loanAmount)}</strong></td>
                    <td>
                      <strong className="table-money">{money(loan.balance)}</strong>
                      <small className="table-muted">{money(loan.amountPaid)} paid</small>
                    </td>
                    <td>{shortDate(loan.dateLent ?? loan.dateBorrowed)}</td>
                    <td>{shortDate(loan.dueDate)}</td>
                    <td><StatusBadge status={loan.status} /></td>
                    <td>
                      <div className="table-actions">
                        {loan.balance > 0 && (
                          <button className="row-action" onClick={() => onPayment(loan)}>
                            {isLent ? "Payment" : "Repay"}
                          </button>
                        )}
                        <button className="delete-action" onClick={() => onDelete(loan)} aria-label={`Delete loan with ${loan.personName}`} title="Delete loan">
                          <X size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr><td colSpan={7}>
                  <EmptyState
                    title={query ? "No matching loans" : "No loans here yet"}
                    description={query ? "Try a different name or phone number." : `Add a ${isLent ? "loan you’ve made" : "loan you’ve taken"} to start tracking it.`}
                    action={query ? undefined : "Add a loan"}
                    onAction={query ? undefined : onAdd}
                  />
                </td></tr>
              )}
            </tbody>
          </table>
        </div>
        <div className="table-footer">
          <span>Showing <strong>{loans.length}</strong> {loans.length === 1 ? "loan" : "loans"}</span>
          <span className="table-footer-secure"><ShieldCheck size={14} /> Private to you</span>
        </div>
      </div>
      <div className="footnote"><CircleHelp size={14} /> Loan balances and statuses update automatically when you record a payment.</div>
    </>
  );
}

function StatusBadge({ status }: { status: LoanStatus }) {
  return <span className={`status-badge status-${status.toLowerCase()}`}><span />{status.replace("_", " ")}</span>;
}

function EmptyState({
  title,
  description,
  action,
  onAction,
}: {
  title: string;
  description: string;
  action?: string;
  onAction?: () => void;
}) {
  return (
    <div className="empty-state">
      <span className="empty-icon"><HandCoins size={22} /></span>
      <strong>{title}</strong>
      <p>{description}</p>
      {action && onAction && <button className="button button-outline" onClick={onAction}><Plus size={16} />{action}</button>}
    </div>
  );
}

function LoadingRows() {
  return <div className="loading-state"><LoaderCircle size={19} className="spin" /> Loading your loans…</div>;
}

function ProfilePage({
  user,
  onSaved,
}: {
  user: User;
  onSaved: (profile: UserProfile) => void;
}) {
  const [profile, setProfile] = useState<UserProfile>({
    userId: user.userId,
    name: user.name,
    email: user.email,
    phoneNumber: "",
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    let active = true;
    api.profile().then((nextProfile) => {
      if (active) setProfile(nextProfile);
    }).catch((caught: unknown) => {
      if (active) setError(caught instanceof Error ? caught.message : "Could not load your profile.");
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => {
      active = false;
    };
  }, []);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const updatedProfile = await api.updateProfile({
        name: profile.name.trim(),
        email: profile.email.trim(),
        phoneNumber: profile.phoneNumber.trim(),
      });
      setProfile(updatedProfile);
      onSaved(updatedProfile);
      setSuccess("Your profile changes have been saved.");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Could not update your profile.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="profile-page">
      <div className="profile-page-heading">
        <div>
          <span className="eyebrow">ACCOUNT</span>
          <h1>Your profile</h1>
          <p>Keep your contact details up to date.</p>
        </div>
      </div>
      <div className="panel profile-settings">
        <div className="profile-settings-heading">
          <span className="profile-settings-avatar">{profile.name.trim().slice(0, 1).toUpperCase() || "U"}</span>
          <div>
            <h2>Personal details</h2>
            <p>These details are used to identify your account.</p>
          </div>
        </div>
        {error && <div className="alert alert-error" role="alert">{error}</div>}
        {success && <div className="profile-success" role="status">{success}</div>}
        {loading ? (
          <div className="loading-state"><LoaderCircle size={19} className="spin" /> Loading your profile…</div>
        ) : (
          <form className="profile-form" onSubmit={(event) => void submit(event)}>
            <FormField label="Full name">
              <input
                autoComplete="name"
                required
                value={profile.name}
                onChange={(event) => setProfile({ ...profile, name: event.target.value })}
              />
            </FormField>
            <FormField label="Email address">
              <input
                type="email"
                autoComplete="email"
                required
                value={profile.email}
                onChange={(event) => setProfile({ ...profile, email: event.target.value })}
              />
            </FormField>
            <FormField label="Phone number">
              <input
                type="tel"
                autoComplete="tel"
                required
                value={profile.phoneNumber}
                onChange={(event) => setProfile({ ...profile, phoneNumber: event.target.value })}
              />
            </FormField>
            <div className="profile-form-actions">
              <button className="button button-primary" disabled={saving}>
                {saving ? <LoaderCircle className="spin" size={16} /> : null}
                Save changes
              </button>
            </div>
          </form>
        )}
      </div>
    </section>
  );
}

function AuthScreen({
  error,
  onClearError,
  onLogin,
  onRegister,
}: {
  error: string;
  onClearError: () => void;
  onLogin: (email: string, password: string) => Promise<void>;
  onRegister: (input: {
    name: string;
    email: string;
    phoneNumber: string;
    password: string;
    confirmPassword: string;
  }) => Promise<void>;
}) {
  const [mode, setMode] = useState<"login" | "register" | "forgot" | "verify" | "reset">("login");
  const [busy, setBusy] = useState(false);
  const [localError, setLocalError] = useState("");
  const [feedback, setFeedback] = useState("");
  const [resetEmail, setResetEmail] = useState("");
  const [resetToken, setResetToken] = useState("");
  const registering = mode === "register";
  const resetting = mode === "forgot" || mode === "verify" || mode === "reset";

  function changeMode(nextMode: typeof mode) {
    setMode(nextMode);
    setLocalError("");
    setFeedback("");
    onClearError();
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLocalError("");
    onClearError();
    setBusy(true);
    const form = new FormData(event.currentTarget);
    try {
      if (mode === "register") {
        const password = String(form.get("password") ?? "");
        const confirmPassword = String(form.get("confirmPassword") ?? "");
        if (password !== confirmPassword) {
          setLocalError("Passwords don’t match.");
          return;
        }
        await onRegister({
          name: String(form.get("name") ?? "").trim(),
          email: String(form.get("email") ?? "").trim(),
          phoneNumber: String(form.get("phoneNumber") ?? "").trim(),
          password,
          confirmPassword,
        });
      } else if (mode === "login") {
        await onLogin(
          String(form.get("email") ?? "").trim(),
          String(form.get("password") ?? ""),
        );
      } else if (mode === "forgot") {
        const email = String(form.get("email") ?? "").trim();
        const response = await api.forgotPassword(email);
        setResetEmail(email);
        if (!response.message.toLowerCase().includes("sent")) {
          setFeedback(response.message);
        } else {
          setFeedback("Check your email for a six-digit verification code.");
          setMode("verify");
        }
      } else if (mode === "verify") {
        const response = await api.verifyPasswordResetOtp(
          resetEmail,
          String(form.get("otp") ?? "").trim(),
        );
        setResetToken(response.resetToken);
        setFeedback("");
        setMode("reset");
      } else {
        const newPassword = String(form.get("newPassword") ?? "");
        const confirmPassword = String(form.get("confirmPassword") ?? "");
        if (newPassword !== confirmPassword) {
          setLocalError("Passwords don’t match.");
          return;
        }
        const response = await api.resetPassword({
          resetToken,
          newPassword,
          confirmPassword,
        });
        setMode("login");
        setFeedback(response.message);
      }
    } catch (caught) {
      setLocalError(caught instanceof Error ? caught.message : "Could not complete your request.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-decoration auth-decoration-one" />
      <div className="auth-decoration auth-decoration-two" />
      <div className="auth-layout">
        <section className="auth-story">
          <a className="brand auth-brand" href="#" onClick={(event) => event.preventDefault()}>
            <span className="brand-mark"><HandCoins size={21} /></span>
            <span>Kumbukaa<span className="brand-dot">.</span></span>
          </a>
          <div className="auth-story-copy">
            <div className="eyebrow"><span className="eyebrow-line" /> PERSONAL FINANCE, MADE HUMAN</div>
            <h1>Good lending<br />starts with<br /><em>good keeping.</em></h1>
            <p>Remember every loan, every promise, every repayment. All in one thoughtful place.</p>
            <div className="auth-proof">
              <div className="proof-avatars"><span>A</span><span>M</span><span>J</span><span>+</span></div>
              <span>Your money deserves<br /><strong>a good memory.</strong></span>
            </div>
          </div>
          <div className="auth-copyright">© {new Date().getFullYear()} Kumbukaa <span>·</span> Keep what matters in mind.</div>
        </section>

        <section className="auth-form-side">
          <div className="auth-form-wrap">
            <div className="auth-welcome">
              <span className="auth-kicker">
                {registering ? "A FRESH START" : resetting ? "ACCOUNT RECOVERY" : "WELCOME BACK"}
              </span>
              <h2>
                {registering ? "Create your account"
                  : mode === "forgot" ? "Reset your password"
                    : mode === "verify" ? "Check your email"
                      : mode === "reset" ? "Choose a new password"
                        : "Sign in to your space"}
              </h2>
              <p>
                {registering ? "Start keeping track of the things that matter."
                  : mode === "forgot" ? "We’ll email you a code to verify your account."
                    : mode === "verify" ? `Enter the code sent to ${resetEmail}.`
                      : mode === "reset" ? "Choose a strong password for your account."
                        : "Your loans are right where you left them."}
              </p>
            </div>
            {(localError || error) && <div className="alert alert-error auth-alert" role="alert">{localError || error}</div>}
            {feedback && <div className="profile-success auth-feedback" role="status">{feedback}</div>}
            <form className="auth-form" onSubmit={(event) => void submit(event)}>
              {mode === "register" && (
                <>
                  <FormField label="Your name"><input name="name" autoComplete="name" required placeholder="e.g. Wanjiku Kamau" /></FormField>
                  <FormField label="Phone number"><input name="phoneNumber" type="tel" autoComplete="tel" required placeholder="+254 7XX XXX XXX" /></FormField>
                </>
              )}
              {(mode === "login" || mode === "register" || mode === "forgot") && (
                <FormField label="Email address">
                  <input
                    name="email"
                    type="email"
                    autoComplete="email"
                    required
                    defaultValue={mode === "forgot" ? resetEmail : undefined}
                    placeholder="you@example.com"
                  />
                </FormField>
              )}
              {mode === "login" && (
                <PasswordField
                  label="Password"
                  name="password"
                  autoComplete="current-password"
                  placeholder="Enter your password"
                />
              )}
              {mode === "verify" && (
                <FormField label="Six-digit verification code">
                  <input
                    name="otp"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    pattern="[0-9]{6}"
                    minLength={6}
                    maxLength={6}
                    required
                    placeholder="Enter the code from your email"
                  />
                </FormField>
              )}
              {mode === "reset" && (
                <>
                  <PasswordField
                    label="New password"
                    name="newPassword"
                    autoComplete="new-password"
                    minLength={8}
                    placeholder="At least 8 characters"
                  />
                  <PasswordField
                    label="Confirm new password"
                    name="confirmPassword"
                    autoComplete="new-password"
                    minLength={8}
                    placeholder="Enter your new password again"
                  />
                </>
              )}
              {mode === "register" && (
                <>
                  <PasswordField
                    label="Password"
                    name="password"
                    autoComplete="new-password"
                    minLength={8}
                    placeholder="At least 8 characters"
                  />
                <PasswordField
                  label="Confirm password"
                  name="confirmPassword"
                  autoComplete="new-password"
                  minLength={8}
                  placeholder="Enter your password again"
                />
                </>
              )}
              {mode === "login" && (
                <button className="auth-forgot-link" type="button" onClick={() => changeMode("forgot")}>
                  Forgot password?
                </button>
              )}
              <button className="button button-primary auth-submit" disabled={busy}>
                {busy ? <LoaderCircle className="spin" size={17} /> : null}
                {registering ? "Create account"
                  : mode === "login" ? "Sign in"
                    : mode === "forgot" ? "Send verification code"
                      : mode === "verify" ? "Verify code"
                        : "Update password"} {!busy && <ArrowRight size={17} />}
              </button>
            </form>
            {mode === "verify" && (
              <p className="auth-switch">
                <button onClick={() => changeMode("forgot")}>Use a different email</button>
              </p>
            )}
            {mode === "reset" && (
              <p className="auth-switch">
                <button onClick={() => changeMode("login")}>Back to sign in</button>
              </p>
            )}
            {!resetting && (
              <p className="auth-switch">
                {registering ? "Already have an account?" : "New to Kumbukaa?"}{" "}
                <button onClick={() => changeMode(registering ? "login" : "register")}>
                  {registering ? "Sign in" : "Create an account"}
                </button>
              </p>
            )}
            {mode === "forgot" && (
              <p className="auth-switch"><button onClick={() => changeMode("login")}>Back to sign in</button></p>
            )}
            <div className="auth-secure"><ShieldCheck size={15} /> Your account is private and secure</div>
          </div>
        </section>
      </div>
    </div>
  );
}

function FormField({ label, children }: { label: string; children: React.ReactNode }) {
  return <label className="form-field"><span>{label}</span>{children}</label>;
}

function PasswordField({
  label,
  name,
  autoComplete,
  minLength,
  placeholder,
}: {
  label: string;
  name: string;
  autoComplete: string;
  minLength?: number;
  placeholder: string;
}) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="form-field">
      <label htmlFor={name}>{label}</label>
      <div className="auth-password-control">
        <input
          id={name}
          name={name}
          type={visible ? "text" : "password"}
          autoComplete={autoComplete}
          minLength={minLength}
          required
          placeholder={placeholder}
        />
        <button
          className="auth-password-toggle"
          type="button"
          aria-label={visible ? `Hide ${label.toLowerCase()}` : `Show ${label.toLowerCase()}`}
          aria-pressed={visible}
          onClick={() => setVisible((current) => !current)}
        >
          {visible ? <EyeOff size={17} /> : <Eye size={17} />}
        </button>
      </div>
    </div>
  );
}

function LoanModal({
  kind,
  onKindChange,
  onClose,
  onSubmit,
}: {
  kind: LoanKind;
  onKindChange: (kind: LoanKind) => void;
  onClose: () => void;
  onSubmit: (values: {
    personName: string;
    phoneNumber: string;
    counterpartyEmail: string;
    amount: number;
    date: string;
    dueDate: string;
    notes: string;
  }) => Promise<void>;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const lent = kind === "lent";

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    const form = new FormData(event.currentTarget);
    try {
      await onSubmit({
        personName: String(form.get("personName") ?? "").trim(),
        phoneNumber: String(form.get("phoneNumber") ?? "").trim(),
        counterpartyEmail: String(form.get("counterpartyEmail") ?? "").trim(),
        amount: Number(form.get("amount")),
        date: String(form.get("date") ?? ""),
        dueDate: String(form.get("dueDate") ?? ""),
        notes: String(form.get("notes") ?? "").trim(),
      });
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Could not save this loan.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal onClose={onClose}>
      <div className="modal-heading">
        <span className="modal-icon"><HandCoins size={19} /></span>
        <div><h2>Add a loan</h2><p>Keep the details handy. You can update them later.</p></div>
        <button className="icon-button modal-close" onClick={onClose} aria-label="Close"><X size={19} /></button>
      </div>
      <div className="kind-toggle">
        <button type="button" className={lent ? "kind-selected" : ""} onClick={() => onKindChange("lent")}><ArrowUpRight size={15} /> I lent money</button>
        <button type="button" className={!lent ? "kind-selected" : ""} onClick={() => onKindChange("borrowed")}><ArrowDownLeft size={15} /> I borrowed</button>
      </div>
      {error && <div className="alert alert-error">{error}</div>}
      <form className="modal-form" onSubmit={(event) => void submit(event)}>
        <FormField label={lent ? "Borrower’s name" : "Lender’s name"}>
          <input name="personName" required autoFocus placeholder="Full name" />
        </FormField>
        <FormField label="Phone number">
          <input name="phoneNumber" type="tel" required placeholder="+254 7XX XXX XXX" />
        </FormField>
        <FormField label="Counterparty email (optional)">
          <input name="counterpartyEmail" type="email" autoComplete="email" placeholder="name@example.com" />
        </FormField>
        <FormField label="Amount (KES)">
          <input name="amount" type="number" min="0.01" step="0.01" required placeholder="0.00" />
        </FormField>
        <div className="form-grid-two">
          <FormField label={lent ? "Date lent" : "Date borrowed"}>
            <input name="date" type="date" required defaultValue={new Date().toISOString().slice(0, 10)} />
          </FormField>
          <FormField label="Due date (optional)">
            <input name="dueDate" type="date" />
          </FormField>
        </div>
        <FormField label="Notes (optional)">
          <textarea name="notes" rows={3} placeholder="Add a note to remember…" />
        </FormField>
        <div className="modal-actions">
          <button type="button" className="button button-outline" onClick={onClose}>Cancel</button>
          <button className="button button-primary" disabled={busy}>
            {busy ? <LoaderCircle size={16} className="spin" /> : <Plus size={16} />}
            Save loan
          </button>
        </div>
      </form>
    </Modal>
  );
}

function PaymentModal({
  loan,
  onClose,
  onSubmit,
}: {
  loan: Loan;
  onClose: () => void;
  onSubmit: (amount: number) => Promise<void>;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    const amount = Number(new FormData(event.currentTarget).get("amount"));
    if (amount > loan.balance) {
      setError(`Payment cannot be more than the remaining balance of ${money(loan.balance)}.`);
      setBusy(false);
      return;
    }
    try {
      await onSubmit(amount);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Could not record this payment.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal onClose={onClose}>
      <div className="modal-heading">
        <span className="modal-icon payment-icon"><Check size={19} /></span>
        <div><h2>Record a payment</h2><p>For the loan with {loan.personName}.</p></div>
        <button className="icon-button modal-close" onClick={onClose} aria-label="Close"><X size={19} /></button>
      </div>
      <div className="payment-summary">
        <span>Remaining balance</span><strong>{money(loan.balance)}</strong>
        <small>{money(loan.amountPaid)} paid of {money(loan.loanAmount)}</small>
      </div>
      {error && <div className="alert alert-error">{error}</div>}
      <form className="modal-form" onSubmit={(event) => void submit(event)}>
        <FormField label="Payment amount (KES)">
          <input name="amount" type="number" min="0.01" max={loan.balance} step="0.01" required autoFocus placeholder="0.00" />
        </FormField>
        <div className="modal-actions">
          <button type="button" className="button button-outline" onClick={onClose}>Cancel</button>
          <button className="button button-primary" disabled={busy}>
            {busy ? <LoaderCircle size={16} className="spin" /> : <Check size={16} />}
            Record payment
          </button>
        </div>
      </form>
    </Modal>
  );
}

function Modal({ children, onClose }: { children: React.ReactNode; onClose: () => void }) {
  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [onClose]);

  return (
    <div className="modal-backdrop" onMouseDown={(event) => {
      if (event.target === event.currentTarget) onClose();
    }}>
      <section className="modal-card" role="dialog" aria-modal="true">
        {children}
      </section>
    </div>
  );
}

export default App;
