import { useCallback, useEffect, useMemo, useState } from "react";
import type { FormEvent, ReactNode } from "react";
import {
  ArrowUpRight,
  Check,
  CircleHelp,
  Eye,
  HandCoins,
  KeyRound,
  LayoutDashboard,
  LoaderCircle,
  LogOut,
  RefreshCw,
  Search,
  ShieldCheck,
  Trash2,
  UserPlus,
  Users,
  X,
} from "lucide-react";
import { ApiError, api } from "./api";
import type { AdminProfile, AdminUserDetails, AdminUserSummary, Loan } from "./types";

type AdminModal = "details" | "reset" | "create" | "settings" | null;

const adminAccessDeniedMessage =
  "The backend denied this admin request (403). Your session is still active; verify that this account has ROLE_ADMIN and restart the backend after role changes.";

const adminMoney = (amount: number) =>
  new Intl.NumberFormat("en-KE", {
    style: "currency",
    currency: "KES",
    maximumFractionDigits: 0,
  }).format(amount || 0);

const adminDate = (value?: string) =>
  value
    ? new Intl.DateTimeFormat("en-KE", {
        day: "numeric",
        month: "short",
        year: "numeric",
      }).format(new Date(value))
    : "—";

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
  const [users, setUsers] = useState<AdminUserSummary[]>([]);
  const [selectedUser, setSelectedUser] = useState<AdminUserSummary | null>(null);
  const [userDetails, setUserDetails] = useState<AdminUserDetails | null>(null);
  const [modal, setModal] = useState<AdminModal>(
    profile.mustChangePassword ? "settings" : null,
  );
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [formError, setFormError] = useState("");
  const [notice, setNotice] = useState("");

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setUsers(await api.adminUsers());
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) {
        onSessionExpired();
      } else {
        setError(
          caught instanceof ApiError && caught.status === 403
            ? adminAccessDeniedMessage
            : caught instanceof Error
              ? caught.message
              : "Could not load accounts.",
        );
      }
    } finally {
      setLoading(false);
    }
  }, [onSessionExpired]);

  useEffect(() => {
    void loadUsers();
  }, [loadUsers]);

  useEffect(() => {
    if (!notice) return;
    const timeout = window.setTimeout(() => setNotice(""), 3500);
    return () => window.clearTimeout(timeout);
  }, [notice]);

  useEffect(() => {
    if (!modal) return;
    function handleKeyDown(event: KeyboardEvent) {
      if (
        event.key === "Escape" &&
        !(modal === "settings" && profile.mustChangePassword)
      ) {
        setModal(null);
      }
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [modal, profile.mustChangePassword]);

  const filteredUsers = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) return users;
    return users.filter((user) =>
      [user.fullName, user.email, user.phoneNumber]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(needle)),
    );
  }, [query, users]);

  const totalLoans = users.reduce(
    (total, user) => total + user.loansLent + user.loansBorrowed,
    0,
  );
  const activeUsers = users.filter((user) => user.loansLent + user.loansBorrowed > 0).length;
  const adminCount = users.filter((user) => user.admin).length;
  const displayName = profile.fullName?.trim() || "Administrator";

  function openModal(nextModal: Exclude<AdminModal, null>, user?: AdminUserSummary) {
    setSelectedUser(user ?? null);
    setUserDetails(null);
    setFormError("");
    setModal(nextModal);
  }

  function closeModal() {
    if (modal === "settings" && profile.mustChangePassword) return;
    setModal(null);
    setSelectedUser(null);
    setUserDetails(null);
    setFormError("");
  }

  function showError(caught: unknown) {
    setFormError(
      caught instanceof ApiError && caught.status === 403
        ? adminAccessDeniedMessage
        : caught instanceof Error
          ? caught.message
          : "The request could not be completed.",
    );
  }

  async function viewUser(user: AdminUserSummary) {
    setSelectedUser(user);
    setUserDetails(null);
    setFormError("");
    setModal("details");
    try {
      setUserDetails(await api.adminUser(user.id));
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) {
        onSessionExpired();
      } else {
        showError(caught);
      }
    }
  }

  async function resetUserPassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedUser) return;
    const values = new FormData(event.currentTarget);
    const password = String(values.get("password") ?? "");
    if (password !== String(values.get("confirmPassword") ?? "")) {
      setFormError("The passwords do not match.");
      return;
    }
    setBusy(true);
    setFormError("");
    try {
      const result = await api.resetAdminUserPassword(selectedUser.id, password);
      setNotice(result.message || "User password reset.");
      closeModal();
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) {
        onSessionExpired();
      } else {
        showError(caught);
      }
    } finally {
      setBusy(false);
    }
  }

  async function createAdmin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    const password = String(values.get("password") ?? "");
    const confirmPassword = String(values.get("confirmPassword") ?? "");
    if (password !== confirmPassword) {
      setFormError("The passwords do not match.");
      return;
    }
    setBusy(true);
    setFormError("");
    try {
      const result = await api.createAdmin({
        fullName: String(values.get("fullName") ?? "").trim(),
        email: String(values.get("email") ?? "").trim(),
        phoneNumber: String(values.get("phoneNumber") ?? "").trim(),
        password,
        confirmPassword,
      });
      setModal(null);
      setNotice(result.message || "Administrator created.");
      await loadUsers();
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) {
        onSessionExpired();
      } else {
        showError(caught);
      }
    } finally {
      setBusy(false);
    }
  }

  async function changeAdminPassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    const newPassword = String(values.get("newPassword") ?? "");
    if (newPassword !== String(values.get("confirmPassword") ?? "")) {
      setFormError("The new passwords do not match.");
      return;
    }
    setBusy(true);
    setFormError("");
    try {
      await api.changeAdminPassword({
        ...(profile.mustChangePassword
          ? {}
          : { currentPassword: String(values.get("currentPassword") ?? "") }),
        newPassword,
      });
      onProfileChange({ ...profile, mustChangePassword: false });
      setModal(null);
      setNotice("Admin password updated.");
    } catch (caught) {
      showError(caught);
    } finally {
      setBusy(false);
    }
  }

  async function deleteUser(user: AdminUserSummary) {
    if (user.id === profile.id) return;
    if (!window.confirm(`Delete ${user.fullName || user.email}? This cannot be undone.`)) return;
    setError("");
    try {
      const result = await api.deleteAdminUser(user.id);
      setUsers((current) => current.filter((item) => item.id !== user.id));
      setNotice(result.message || "Account deleted.");
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) {
        onSessionExpired();
      } else {
        setError(
          caught instanceof ApiError && caught.status === 403
            ? adminAccessDeniedMessage
            : caught instanceof Error
              ? caught.message
              : "Could not delete the account.",
        );
      }
    }
  }

  const modalTitle = {
    details: "Account details",
    reset: "Reset user password",
    create: "Create administrator",
    settings: "Admin account settings",
  } as const;

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <a className="admin-brand" href="#" onClick={(event) => event.preventDefault()}>
          <span className="admin-brand-mark"><ShieldCheck size={19} /></span>
          <span>KUMBUKAA<small>ADMIN</small></span>
        </a>
        <div className="admin-nav-label">ADMINISTRATION</div>
        <nav className="admin-nav" aria-label="Admin navigation">
          <a className="admin-nav-item admin-nav-active" href="#admin-overview">
            <LayoutDashboard size={17} /> Overview
          </a>
          <a className="admin-nav-item" href="#admin-users">
            <Users size={17} /> User directory
          </a>
          <button className="admin-nav-item" onClick={() => openModal("settings")}>
            <KeyRound size={17} /> Account security
          </button>
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
          <div className="admin-breadcrumb">Platform <span>/</span> <strong>Overview</strong></div>
          <div className="admin-topbar-actions">
            <span className="admin-role-chip"><ShieldCheck size={14} /> Administrator</span>
            <button className="admin-icon-button" onClick={() => openModal("settings")} aria-label="Account settings">
              <CircleHelp size={17} />
            </button>
          </div>
        </header>

        <div className="admin-content">
          {notice && <div className="admin-toast" role="status"><Check size={16} />{notice}<button onClick={() => setNotice("")} aria-label="Dismiss"><X size={14} /></button></div>}
          <section id="admin-overview" className="admin-page-heading">
            <div>
              <span className="admin-eyebrow">PLATFORM OPERATIONS</span>
              <h1>Control center</h1>
              <p>Manage accounts and review activity across Kumbukaa.</p>
            </div>
            <div className="admin-heading-actions">
              <button className="admin-secondary-button" onClick={() => void loadUsers()} disabled={loading}>
                {loading ? <LoaderCircle className="spin" size={16} /> : <RefreshCw size={15} />} Refresh
              </button>
              <button className="admin-primary-button" onClick={() => openModal("create")}>
                <UserPlus size={16} /> Add administrator
              </button>
            </div>
          </section>

          {error && <div className="admin-error-banner" role="alert">{error}</div>}

          <section className="admin-metrics" aria-label="Platform account metrics">
            <AdminMetric icon={<Users size={18} />} label="Registered accounts" value={loading ? "—" : users.length.toLocaleString()} tone="blue" />
            <AdminMetric icon={<ShieldCheck size={18} />} label="Administrators" value={loading ? "—" : adminCount.toLocaleString()} tone="violet" />
            <AdminMetric icon={<HandCoins size={18} />} label="Loans tracked" value={loading ? "—" : totalLoans.toLocaleString()} tone="green" />
            <AdminMetric icon={<ArrowUpRight size={18} />} label="Active accounts" value={loading ? "—" : activeUsers.toLocaleString()} tone="amber" />
          </section>

          <section id="admin-users" className="admin-directory">
            <div className="admin-directory-heading">
              <div>
                <span className="admin-eyebrow">ACCOUNT MANAGEMENT</span>
                <h2>User directory</h2>
                <p>Inspect accounts, loan activity, and access level.</p>
              </div>
              <label className="admin-search">
                <Search size={16} />
                <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search accounts" aria-label="Search accounts" />
              </label>
            </div>
            <div className="admin-table-scroll">
              <table className="admin-user-table">
                <thead>
                  <tr><th>ACCOUNT</th><th>EMAIL</th><th>ACCESS</th><th>PHONE</th><th>LOANS LENT</th><th>LOANS BORROWED</th><th>ACTIONS</th></tr>
                </thead>
                <tbody>
                  {loading && users.length === 0 ? (
                    <tr><td className="admin-table-empty" colSpan={7}><LoaderCircle className="spin" size={17} /> Loading accounts…</td></tr>
                  ) : filteredUsers.length ? filteredUsers.map((user) => (
                    <tr key={user.id}>
                      <td><span className="admin-user-name"><span className="admin-user-avatar">{(user.fullName || user.email).slice(0, 1).toUpperCase()}</span><strong>{user.fullName || "Unnamed account"}</strong></span></td>
                      <td>{user.email}</td>
                      <td><span className={`admin-access-badge ${user.admin ? "is-admin" : ""}`}>{user.admin ? "Admin" : "User"}</span></td>
                      <td>{user.phoneNumber || "—"}</td>
                      <td>{user.loansLent}</td>
                      <td>{user.loansBorrowed}</td>
                      <td><div className="admin-row-actions">
                        <button onClick={() => void viewUser(user)} aria-label={`View ${user.fullName || user.email}`} title="View account"><Eye size={15} /></button>
                        <button onClick={() => openModal("reset", user)} aria-label={`Reset password for ${user.fullName || user.email}`} title="Reset password"><KeyRound size={15} /></button>
                        {user.id === profile.id
                          ? <span className="admin-self-label">You</span>
                          : <button className="admin-delete-button" onClick={() => void deleteUser(user)} aria-label={`Delete ${user.fullName || user.email}`} title="Delete account"><Trash2 size={15} /></button>}
                      </div></td>
                    </tr>
                  )) : (
                    <tr><td className="admin-table-empty" colSpan={7}>{query ? "No accounts match your search." : "No accounts found."}</td></tr>
                  )}
                </tbody>
              </table>
            </div>
            <footer className="admin-directory-footer">
              <span>{filteredUsers.length} of {users.length} accounts</span>
              <span><ShieldCheck size={14} /> Role-checked by the backend</span>
            </footer>
          </section>
          <footer className="admin-page-footer">Kumbukaa Administration <span>·</span> Secure workspace</footer>
        </div>
      </main>

      {modal && (
        <div className="admin-modal-backdrop" onMouseDown={(event) => {
          if (event.target === event.currentTarget) closeModal();
        }}>
          <section className="admin-modal" role="dialog" aria-modal="true" aria-labelledby="admin-modal-title">
            <header className="admin-modal-header">
              <div><span className="admin-eyebrow">KUMBUKAA ADMIN</span><h2 id="admin-modal-title">{modalTitle[modal]}</h2></div>
              {!(modal === "settings" && profile.mustChangePassword) && (
                <button className="admin-modal-close" onClick={closeModal} aria-label="Close dialog"><X size={18} /></button>
              )}
            </header>

            {modal === "details" && (
              <div className="admin-modal-content">
                {formError && <div className="admin-form-error" role="alert">{formError}</div>}
                {!userDetails ? <div className="admin-details-loading"><LoaderCircle className="spin" size={18} /> Loading account details…</div> : (
                  <>
                    <div className="admin-account-summary">
                      <div><small>Name</small><strong>{userDetails.fullName || "—"}</strong></div>
                      <div><small>Email</small><strong>{userDetails.email}</strong></div>
                      <div><small>Phone</small><strong>{userDetails.phoneNumber || "—"}</strong></div>
                      <div><small>Access</small><strong>{userDetails.admin ? "Administrator" : "User"}</strong></div>
                    </div>
                    <AdminLoanTable title="Money lent" loans={userDetails.loansLent} />
                    <AdminLoanTable title="Money borrowed" loans={userDetails.loansBorrowed} />
                  </>
                )}
              </div>
            )}

            {modal === "reset" && selectedUser && (
              <form className="admin-modal-form" onSubmit={(event) => void resetUserPassword(event)}>
                <p className="admin-modal-description">Set a new password for <strong>{selectedUser.fullName || selectedUser.email}</strong>.</p>
                <label>New password<input name="password" type="password" autoComplete="new-password" minLength={8} required /></label>
                <label>Confirm password<input name="confirmPassword" type="password" autoComplete="new-password" minLength={8} required /></label>
                {formError && <div className="admin-form-error" role="alert">{formError}</div>}
                <AdminModalActions busy={busy} submitLabel="Reset password" onCancel={closeModal} />
              </form>
            )}

            {modal === "create" && (
              <form className="admin-modal-form" onSubmit={(event) => void createAdmin(event)}>
                <p className="admin-modal-description">New administrators must change their password when they first sign in.</p>
                <label>Full name<input name="fullName" autoComplete="name" required /></label>
                <label>Email address<input name="email" type="email" autoComplete="email" required /></label>
                <label>Phone number<input name="phoneNumber" type="tel" autoComplete="tel" required /></label>
                <label>Password<input name="password" type="password" autoComplete="new-password" minLength={8} required /></label>
                <label>Confirm password<input name="confirmPassword" type="password" autoComplete="new-password" minLength={8} required /></label>
                {formError && <div className="admin-form-error" role="alert">{formError}</div>}
                <AdminModalActions busy={busy} submitLabel="Create administrator" onCancel={closeModal} />
              </form>
            )}

            {modal === "settings" && (
              <div className="admin-modal-content">
                <div className="admin-profile-card">
                  <span className="admin-avatar">{displayName.slice(0, 1).toUpperCase()}</span>
                  <span><strong>{displayName}</strong><small>{profile.email}</small></span>
                </div>
                {profile.mustChangePassword && <p className="admin-forced-change">Change the initial password to continue using admin tools.</p>}
                <form className="admin-modal-form" onSubmit={(event) => void changeAdminPassword(event)}>
                  {!profile.mustChangePassword && <label>Current password<input name="currentPassword" type="password" autoComplete="current-password" required /></label>}
                  <label>New password<input name="newPassword" type="password" autoComplete="new-password" minLength={8} required /></label>
                  <label>Confirm new password<input name="confirmPassword" type="password" autoComplete="new-password" minLength={8} required /></label>
                  {formError && <div className="admin-form-error" role="alert">{formError}</div>}
                  <AdminModalActions busy={busy} submitLabel="Update password" onCancel={closeModal} forceChange={profile.mustChangePassword} />
                </form>
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
}

function AdminMetric({
  icon,
  label,
  value,
  tone,
}: {
  icon: ReactNode;
  label: string;
  value: string;
  tone: string;
}) {
  return (
    <article className="admin-metric-card">
      <span className={`admin-metric-icon ${tone}`}>{icon}</span>
      <span className="admin-metric-label">{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function AdminLoanTable({ title, loans }: { title: string; loans: Loan[] }) {
  return (
    <section className="admin-loan-section">
      <h3>{title}<span>{loans.length}</span></h3>
      {loans.length === 0 ? <p className="admin-details-empty">No loans recorded.</p> : (
        <div className="admin-details-table-scroll">
          <table className="admin-details-table">
            <thead><tr><th>COUNTERPARTY</th><th>AMOUNT</th><th>PAID</th><th>BALANCE</th><th>STARTED</th><th>DUE</th><th>STATUS</th></tr></thead>
            <tbody>{loans.map((loan) => (
              <tr key={loan.id}>
                <td>
                  <strong>{loan.personName || "—"}</strong>
                  {(loan.notes || loan.installments?.length > 0) && (
                    <details className="admin-loan-extra">
                      <summary>Notes and payments</summary>
                      {loan.notes && <p>{loan.notes}</p>}
                      {loan.installments?.map((payment, index) => (
                        <small key={`${loan.id}-payment-${index}`}>{adminDate(payment.paymentDate)} · {adminMoney(payment.amount)}</small>
                      ))}
                    </details>
                  )}
                </td>
                <td>{adminMoney(loan.loanAmount)}</td>
                <td>{adminMoney(loan.amountPaid)}</td>
                <td>{adminMoney(loan.balance)}</td>
                <td>{adminDate(loan.dateLent || loan.dateBorrowed)}</td>
                <td>{adminDate(loan.dueDate)}</td>
                <td><span className={`admin-loan-status status-${loan.status.toLowerCase()}`}>{loan.status.replaceAll("_", " ")}</span></td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function AdminModalActions({
  busy,
  submitLabel,
  onCancel,
  forceChange = false,
}: {
  busy: boolean;
  submitLabel: string;
  onCancel: () => void;
  forceChange?: boolean;
}) {
  return (
    <div className="admin-modal-actions">
      {!forceChange && <button className="admin-secondary-button" type="button" onClick={onCancel}>Cancel</button>}
      <button className="admin-primary-button" type="submit" disabled={busy}>
        {busy && <LoaderCircle size={15} className="spin" />}{submitLabel}
      </button>
    </div>
  );
}

export default AdminWorkspace;
