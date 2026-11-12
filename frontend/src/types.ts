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
}

export interface AdminUserSummary {
  id: number;
  fullName: string;
  email: string;
  phoneNumber: string;
  loansLent: number;
  loansBorrowed: number;
  admin: boolean;
}

export interface AdminUserDetails extends Omit<AdminUserSummary, "loansLent" | "loansBorrowed"> {
  loansLent: Loan[];
  loansBorrowed: Loan[];
}

export interface Loan {
  id: number;
  loanAmount: number;
  amountPaid: number;
  balance: number;
  personName: string;
  phoneNumber: string;
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
  amountLent?: number;
  amountBorrowed?: number;
  dateLent?: string;
  dateBorrowed?: string;
  dueDate?: string;
  notes?: string;
}
