import type {
  AuthResponse,
  AdminProfile,
  AdminUserDetails,
  AdminUserSummary,
  AdminAccountRole,
  AdminAccountStatus,
  AdminAnnouncement,
  AdminAuditEntry,
  AdminGlobalLoan,
  AdminNote,
  AdminOverview,
  AppNotification,
  AnnouncementAudience,
  AnnouncementChannel,
  DashboardSummary,
  Loan,
  LoanInput,
  LoanKind,
  UserProfile,
} from "./types";

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");
const tokenKey = "kumbukaa_token";
const userKey = "kumbukaa_user";

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  authenticated = true,
): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const token = localStorage.getItem(tokenKey);
  if (authenticated && token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  let response: Response;
  try {
    response = await fetch(`${apiBaseUrl}${path}`, { ...options, headers });
  } catch {
    throw new ApiError(
      "Could not connect to Kumbukaa. Check that the backend is running.",
      0,
    );
  }

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    const body = await response.text();
    if (body) {
      try {
        const parsed: unknown = JSON.parse(body);
        if (
          parsed &&
          typeof parsed === "object" &&
          "message" in parsed &&
          typeof parsed.message === "string"
        ) {
          message = parsed.message;
        } else if (
          parsed &&
          typeof parsed === "object" &&
          "error" in parsed &&
          typeof parsed.error === "string"
        ) {
          message = parsed.error;
        } else {
          message = body;
        }
      } catch {
        message = body;
      }
    }
    throw new ApiError(message, response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  const contentType = response.headers.get("Content-Type") ?? "";
  if (contentType.includes("application/json")) {
    return (await response.json()) as T;
  }
  return (await response.text()) as T;
}

export const api = {
  async login(email: string, password: string) {
    return request<AuthResponse>(
      "/api/auth/login",
      {
        method: "POST",
        body: JSON.stringify({ email, password }),
      },
      false,
    );
  },

  async register(input: {
    name: string;
    email: string;
    phoneNumber: string;
    password: string;
    confirmPassword: string;
  }) {
    return request<AuthResponse>(
      "/api/auth/register",
      { method: "POST", body: JSON.stringify(input) },
      false,
    );
  },

  forgotPassword(email: string) {
    return request<{ message: string }>(
      "/api/auth/forgot-password",
      { method: "POST", body: JSON.stringify({ email }) },
      false,
    );
  },

  verifyPasswordResetOtp(email: string, otp: string) {
    return request<{ resetToken: string; message: string }>(
      "/api/auth/verify-otp",
      { method: "POST", body: JSON.stringify({ email, otp }) },
      false,
    );
  },

  resetPassword(input: {
    resetToken: string;
    newPassword: string;
    confirmPassword: string;
  }) {
    return request<{ message: string }>(
      "/api/auth/reset-password",
      { method: "POST", body: JSON.stringify(input) },
      false,
    );
  },

  profile() {
    return request<UserProfile>("/api/profile");
  },

  updateProfile(input: Pick<UserProfile, "name" | "email" | "phoneNumber">) {
    return request<UserProfile>("/api/profile", {
      method: "PUT",
      body: JSON.stringify(input),
    });
  },

  summary() {
    return request<DashboardSummary>("/api/dashboard/summary");
  },

  loans(kind: LoanKind) {
    return request<Loan[]>(`/api/loans-${kind}`);
  },

  createLoan(kind: LoanKind, input: LoanInput) {
    return request<Loan>(`/api/loans-${kind}`, {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  recordPayment(kind: LoanKind, id: number, amount: number) {
    return request<Loan>(`/api/loans-${kind}/${id}/payment`, {
      method: "POST",
      body: JSON.stringify({ amount }),
    });
  },

  deleteLoan(kind: LoanKind, id: number) {
    return request<void>(`/api/loans-${kind}/${id}`, { method: "DELETE" });
  },

  health() {
    return request<{ status: string }>("/health", {}, false);
  },

  adminProfile() {
    return request<AdminProfile>("/api/admin/profile");
  },

  adminUsers() {
    return request<AdminUserSummary[]>("/api/admin/users");
  },

  adminOverview() {
    return request<AdminOverview>("/api/admin/overview");
  },

  adminLoans() {
    return request<AdminGlobalLoan[]>("/api/admin/loans");
  },

  adminAuditLog() {
    return request<AdminAuditEntry[]>("/api/admin/audit-log");
  },

  adminUser(id: number) {
    return request<AdminUserDetails>(`/api/admin/users/${id}`);
  },

  updateAdminUserRole(id: number, role: AdminAccountRole) {
    return request<AdminUserDetails>(`/api/admin/users/${id}/role`, {
      method: "PATCH",
      body: JSON.stringify({ role }),
    });
  },

  updateAdminUserStatus(id: number, status: AdminAccountStatus) {
    return request<AdminUserDetails>(`/api/admin/users/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    });
  },

  updateAdminUserVerification(id: number, status: AdminUserSummary["verificationStatus"]) {
    return request<AdminUserDetails>(`/api/admin/users/${id}/verification`, {
      method: "PATCH",
      body: JSON.stringify({ verificationStatus: status }),
    });
  },

  adminUserNotes(id: number) {
    return request<AdminNote[]>(`/api/admin/users/${id}/notes`);
  },

  createAdminUserNote(id: number, note: string) {
    return request<AdminNote>(`/api/admin/users/${id}/notes`, {
      method: "POST",
      body: JSON.stringify({ note }),
    });
  },

  resetAdminUserPassword(id: number, password: string) {
    return request<{ message: string; password: string }>(`/api/admin/users/${id}/reset-password`, {
      method: "POST",
      body: JSON.stringify({ password }),
    });
  },

  deleteAdminUser(id: number) {
    return request<{ message: string }>(`/api/admin/users/${id}`, { method: "DELETE" });
  },

  createAdmin(input: {
    fullName: string;
    email: string;
    phoneNumber: string;
    password: string;
    confirmPassword: string;
  }) {
    return request<AuthResponse>("/api/admins", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  adminAnnouncements() {
    return request<AdminAnnouncement[]>("/api/admin/announcements");
  },

  createAdminAnnouncement(input: {
    title: string;
    message: string;
    audience: AnnouncementAudience;
    channel: AnnouncementChannel;
    targetUserId?: number;
  }) {
    return request<AdminAnnouncement>("/api/admin/announcements", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  notifications() {
    return request<AppNotification[]>("/api/notifications");
  },

  markNotificationRead(id: number) {
    return request<void>(`/api/notifications/${id}/read`, { method: "POST" });
  },

  remindAdminLoan(kind: AdminGlobalLoan["kind"], id: number) {
    return request<{ sent: number }>(`/api/admin/loans/${kind}/${id}/remind`, {
      method: "POST",
    });
  },

  remindOverdueLoans() {
    return request<{ sent: number }>("/api/admin/reminders/overdue", {
      method: "POST",
    });
  },

  changeAdminPassword(input: { currentPassword?: string; newPassword: string }) {
    return request<string>("/api/admin/change-password", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },
};

export function saveSession(user: AuthResponse) {
  localStorage.setItem(tokenKey, user.token);
  localStorage.setItem(
    userKey,
    JSON.stringify({ userId: user.userId, email: user.email, name: user.name }),
  );
}

export function saveUserProfile(user: Pick<AuthResponse, "userId" | "email" | "name">) {
  localStorage.setItem(userKey, JSON.stringify(user));
}

export function clearSession() {
  localStorage.removeItem(tokenKey);
  localStorage.removeItem(userKey);
}

export function getSavedUser(): Pick<AuthResponse, "userId" | "email" | "name"> | null {
  const token = localStorage.getItem(tokenKey);
  const rawUser = localStorage.getItem(userKey);
  if (!token || !rawUser) return null;

  try {
    const user: unknown = JSON.parse(rawUser);
    if (
      user &&
      typeof user === "object" &&
      "userId" in user &&
      "email" in user &&
      "name" in user &&
      typeof user.userId === "number" &&
      typeof user.email === "string" &&
      typeof user.name === "string"
    ) {
      return { userId: user.userId, email: user.email, name: user.name };
    }
  } catch {
    clearSession();
  }
  return null;
}
