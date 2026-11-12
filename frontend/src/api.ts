import type {
  AuthResponse,
  AdminProfile,
  AdminUserDetails,
  AdminUserSummary,
  DashboardSummary,
  Loan,
  LoanInput,
  LoanKind,
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

  adminUser(id: number) {
    return request<AdminUserDetails>(`/api/admin/users/${id}`);
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
