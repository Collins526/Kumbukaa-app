export type LoanKind = "lent" | "borrowed";

export type LoanStatus =
  | "ACTIVE"
  | "PARTIALLY_PAID"
  | "PAID"
  | "OVERDUE";

export interface AuthResponse {
  userId: number;
  email: string;
  name: string;
  message: string;
  token: string;
  refreshToken: string;
}

export interface AdminProfile {
  id: number;
  fullName: string;
  email: string;
  mustChangePassword: boolean;
  accessRole: "ADMIN" | "SUPPORT";
}

export type AdminAccountRole = "USER" | "ADMIN" | "SUPPORT_AGENT";
export type AdminAccountStatus = "ACTIVE" | "SUSPENDED";
export type VerificationStatus = "PENDING" | "VERIFIED" | "REJECTED";
export type AnnouncementAudience =
  | "ALL_USERS"
  | "BORROWERS_WITH_OVERDUE"
  | "LENDERS_ONLY"
  | "ADMINISTRATORS";
export type AnnouncementChannel = "IN_APP" | "EMAIL" | "SMS";

export interface AdminUserSummary {
  id: number;
  fullName: string;
  email: string;
  phoneNumber: string;
  loansLent: number;
  loansBorrowed: number;
  admin: boolean;
  role: AdminAccountRole;
  status: AdminAccountStatus;
  verificationStatus: VerificationStatus;
  createdAt?: string;
  lastSeenAt?: string;
  lastDevice?: string;
}

export interface AdminUserDetails extends Omit<AdminUserSummary, "loansLent" | "loansBorrowed"> {
  loansLent: Loan[];
  loansBorrowed: Loan[];
}

export interface AdminOverview {
  totalAccounts: number;
  activeAccounts: number;
  totalLoans: number;
  totalLent: number;
  outstandingBalance: number;
  overdueLoans: number;
  overdueAmount: number;
  monthlyVolume: { label: string; lent: number; repaid: number }[];
  statusCounts: { active: number; overdue: number; repaid: number };
}

export interface AdminGlobalLoan {
  id: number;
  kind: "LENT" | "BORROWED";
  accountId: number;
  accountName: string;
  accountEmail: string;
  counterpartyName: string;
  counterpartyPhone: string;
  counterpartyEmail?: string;
  amount: number;
  amountPaid: number;
  balance: number;
  startedAt?: string;
  dueDate?: string;
  status: LoanStatus;
  installments: Loan["installments"];
}

export interface AdminAuditEntry {
  id: number;
  action: string;
  actorName: string;
  targetType?: string;
  targetId?: number;
  targetName?: string;
  details: string;
  createdAt: string;
}

export interface AdminNote {
  id: number;
  note: string;
  authorName: string;
  createdAt: string;
}

export interface AdminAnnouncement {
  id: number;
  title: string;
  message: string;
  audience: AnnouncementAudience | "TARGET_USER";
  channel: Exclude<AnnouncementChannel, "SMS">;
  targetUserId?: number;
  targetName?: string;
  createdAt: string;
  authorName: string;
  recipientCount: number;
}

export interface AdminNotification {
  id: number;
  title: string;
  message: string;
  createdAt: string;
  read: boolean;
}

export interface Loan {
  id: number;
  loanAmount: number;
  amountPaid: number;
  balance: number;
  personName: string;
  phoneNumber: string;
  counterpartyEmail?: string;
  dateLent?: string;
  dateBorrowed?: string;
  dueDate?: string;
  status: LoanStatus;
  notes?: string;
  installments: { amount: number; paymentDate?: string }[];
}

export interface DashboardSummary {
  totalLent: number;
  totalBorrowed: number;
  amountOwedToMe: number;
  amountIOwe: number;
  activeLoansLent: number;
  activeLoansBorrowed: number;
  overdueLoans: number;
}

export interface LoanInput {
  personName: string;
  phoneNumber: string;
  counterpartyEmail?: string;
  amountLent?: number;
  amountBorrowed?: number;
  dateLent?: string;
  dateBorrowed?: string;
  dueDate?: string;
  notes?: string;
}
