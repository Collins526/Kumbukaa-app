import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ArrowDownLeft,
  ArrowRight,
  ArrowUpRight,
  Bell,
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
  Wallet,
  X,
} from "lucide-react";
import { ApiError, api, clearSession, getSavedUser, saveSession } from "./api";
import AdminWorkspace from "./AdminWorkspace";
import type {
  AdminProfile,
  AuthResponse,
  DashboardSummary,
  Loan,
  LoanKind,
  LoanStatus,
} from "./types";

type User = Pick<AuthResponse, "userId" | "email" | "name">;
type Page = "overview" | LoanKind;
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
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [query, setQuery] = useState("");
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

  const currentLoans = page === "borrowed" ? borrowed : lent;
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
    setAccessState("checking");
    setError("");
  }

  const logout = useCallback((message = "") => {
    clearSession();
    setUser(null);
    setAdminProfile(null);
    setSummary(emptySummary);
    setLent([]);
    setBorrowed([]);
    setPage("overview");
    setAccessState("user");
    setError(message);
  }, []);
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

  function openPayment(loan: Loan, kind: LoanKind) {
    setSelectedLoan(loan);
    setSelectedKind(kind);
    setModal("payment");
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

  const allLoans = [...lent, ...borrowed].sort((a, b) => {
    const aDate = a.dateLent ?? a.dateBorrowed ?? "";
    const bDate = b.dateLent ?? b.dateBorrowed ?? "";
    return bDate.localeCompare(aDate);
  });
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
          <div className="topbar-actions">
            <span className="today-label">{new Intl.DateTimeFormat("en-KE", {
              weekday: "short", day: "numeric", month: "short",
            }).format(new Date())}</span>
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
              loading={loading}
              onAdd={openLoanForm}
              onNavigate={setPage}
              onPayment={openPayment}
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
  return "Overview";
}

function Overview({
  summary,
  loans,
  loading,
  onAdd,
  onNavigate,
  onPayment,
}: {
  summary: DashboardSummary;
  loans: Loan[];
  loading: boolean;
  onAdd: (kind: LoanKind) => void;
  onNavigate: (page: Page) => void;
  onPayment: (loan: Loan, kind: LoanKind) => void;
}) {
  const recent = loans.slice(0, 5);
  return (
    <>
      <div className="welcome-row">
        <div>
          <div className="eyebrow"><span className="eyebrow-line" /> YOUR MONEY, IN PERSPECTIVE</div>
          <h1>Good to see you<span className="heading-period">.</span></h1>
          <p className="page-subtitle">A clear view of what’s owed to you and what you owe.</p>
        </div>
        <button className="button button-primary" onClick={() => onAdd("lent")}>
          <Plus size={17} /> Add a loan
        </button>
      </div>

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
              <h2>Recent loans</h2>
              <p>Your latest lending activity</p>
            </div>
            <button className="text-button" onClick={() => onNavigate("lent")}>
              View all <ArrowRight size={15} />
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
      <div className="footnote"><ShieldCheck size={14} /> Your financial details are private to your account.</div>
    </>
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
  const [registering, setRegistering] = useState(false);
  const [busy, setBusy] = useState(false);
  const [localError, setLocalError] = useState("");

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLocalError("");
    onClearError();
    setBusy(true);
    const form = new FormData(event.currentTarget);
    try {
      if (registering) {
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
      } else {
        await onLogin(
          String(form.get("email") ?? "").trim(),
          String(form.get("password") ?? ""),
        );
      }
    } catch (caught) {
      setLocalError(caught instanceof Error ? caught.message : "Could not sign in.");
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
              <span className="auth-kicker">{registering ? "A FRESH START" : "WELCOME BACK"}</span>
              <h2>{registering ? "Create your account" : "Sign in to your space"}</h2>
              <p>{registering ? "Start keeping track of the things that matter." : "Your loans are right where you left them."}</p>
            </div>
            {(localError || error) && <div className="alert alert-error auth-alert" role="alert">{localError || error}</div>}
            <form className="auth-form" onSubmit={(event) => void submit(event)}>
              {registering && (
                <>
                  <FormField label="Your name"><input name="name" autoComplete="name" required placeholder="e.g. Wanjiku Kamau" /></FormField>
                  <FormField label="Phone number"><input name="phoneNumber" type="tel" autoComplete="tel" required placeholder="+254 7XX XXX XXX" /></FormField>
                </>
              )}
              <FormField label="Email address">
                <input name="email" type="email" autoComplete="email" required placeholder="you@example.com" />
              </FormField>
              <PasswordField
                label="Password"
                name="password"
                autoComplete={registering ? "new-password" : "current-password"}
                minLength={registering ? 8 : undefined}
                placeholder={registering ? "At least 8 characters" : "Enter your password"}
              />
              {registering && (
                <PasswordField
                  label="Confirm password"
                  name="confirmPassword"
                  autoComplete="new-password"
                  minLength={8}
                  placeholder="Enter your password again"
                />
              )}
              <button className="button button-primary auth-submit" disabled={busy}>
                {busy ? <LoaderCircle className="spin" size={17} /> : null}
                {registering ? "Create account" : "Sign in"} {!busy && <ArrowRight size={17} />}
              </button>
            </form>
            <p className="auth-switch">
              {registering ? "Already have an account?" : "New to Kumbukaa?"}{" "}
              <button onClick={() => {
                setRegistering(!registering);
                setLocalError("");
                onClearError();
              }}>{registering ? "Sign in" : "Create an account"}</button>
            </p>
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
