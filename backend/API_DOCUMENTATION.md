# Kumbukaa Personal Loan Tracking API Documentation

## Overview
This API is a personal loan tracking system with two main loan types:

- `LoanLent`: money you have lent to other people.
- `LoanBorrowed`: money you have borrowed from other people.

The API also exposes a dashboard summary endpoint.

All endpoints are available under `/api`.

---

## Auth Endpoints

### POST /api/auth/register

Register a new user.

#### Request body

```json
{
  "fullName": "Alice Mwangi",
  "email": "alice@example.com",
  "phoneNumber": "+254700000000",
  "password": "SecurePassword123",
  "confirmPassword": "SecurePassword123"
}
```

#### Response
- `201 Created`
- Returns user details and a success message.

### POST /api/auth/login

Login with email and password.

#### Request body

```json
{
  "email": "alice@example.com",
  "password": "SecurePassword123"
}
```

#### Response
- `200 OK`
- Returns user details and a success message.

### POST /api/auth/request-otp

Request an OTP code for login.

#### Request body

```json
{
  "email": "alice@example.com"
}
```

#### Response
- `200 OK`
- Returns a generated OTP code for testing.

### POST /api/auth/login-otp

Login using an OTP code.

#### Request body

```json
{
  "email": "alice@example.com",
  "code": "123456"
}
```

#### Response
- `200 OK`
- Returns user details and a success message.

### POST /api/auth/forgot-password

Request a password-reset code using exactly one account contact:

```json
{"email":"alice@example.com"}
```

or

```json
{"phoneNumber":"+254700000000"}
```

Email codes use the configured email provider. Phone codes are sent by Twilio
and require `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, and `TWILIO_FROM_NUMBER`.

### POST /api/auth/verify-otp

Verify a password-reset code using the same contact field used to request it,
plus the code:

```json
{"phoneNumber":"+254700000000","otp":"123456"}
```

For email recovery, send `email` instead of `phoneNumber`. A successful
verification returns a short-lived reset token for `POST /api/auth/reset-password`.

---

## Entities

### LoanLent

Fields:
- `id`: Long
- `personName`: String
- `phoneNumber`: String
- `amountLent`: Double
- `amountPaid`: Double
- `balance`: Double
- `dateLent`: LocalDate
- `dueDate`: LocalDate
- `status`: `ACTIVE` | `PARTIALLY_PAID` | `PAID` | `OVERDUE`
- `notes`: String
- `createdAt`: LocalDateTime
- `updatedAt`: LocalDateTime

### LoanBorrowed

Fields:
- `id`: Long
- `personName`: String
- `phoneNumber`: String
- `amountBorrowed`: Double
- `amountPaid`: Double
- `balance`: Double
- `dateBorrowed`: LocalDate
- `dueDate`: LocalDate
- `status`: `ACTIVE` | `PARTIALLY_PAID` | `PAID` | `OVERDUE`
- `notes`: String
- `createdAt`: LocalDateTime
- `updatedAt`: LocalDateTime

## Admin control center

All `/api/admin/**` routes require a bearer token. Admin and support accounts may
read the overview, user list/details, global loans, audit log, announcement list,
and user notes. `GET /api/admin/profile` returns `accessRole` as `ADMIN` or
`SUPPORT`. Admin role values stored as either `ADMIN` or `ROLE_ADMIN` are
normalized to the same authority. All admin writes below require admin access;
support is read-only.

`GET /api/admin/users` and `GET /api/admin/users/{id}` include:

```json
{
  "role": "USER",
  "status": "ACTIVE",
  "verificationStatus": "PENDING",
  "lastSeenAt": "2026-11-18T10:00:00",
  "lastDevice": "browser user-agent"
}
```

Role/status/KYC writes use `PATCH /api/admin/users/{id}/role` with
`{"role":"USER|ADMIN|SUPPORT_AGENT"}`, `/status` with
`{"status":"ACTIVE|SUSPENDED"}`, and `/verification` with
`{"verificationStatus":"PENDING|VERIFIED|REJECTED"}`. An administrator cannot
demote or suspend their own administrator account. Suspended users cannot log in
or use existing access tokens.

### Dashboard and global loan data

- `GET /api/admin/overview` returns
  `{"totalAccounts":0,"activeAccounts":0,"totalLoans":0,"totalLent":0,"outstandingBalance":0,"overdueLoans":0,"overdueAmount":0,"monthlyVolume":[{"label":"2026-11","lent":0,"repaid":0}],"statusCounts":{"active":0,"overdue":0,"repaid":0}}`.
  `activeAccounts` means signed in within seven days.
- `GET /api/admin/loans` returns rows shaped as
  `{"id":1,"kind":"LENT","accountId":1,"accountName":"Alice","accountEmail":"alice@example.com","counterpartyName":"Bob","counterpartyPhone":"+254700000000","counterpartyEmail":null,"amount":100,"amountPaid":0,"balance":100,"startedAt":"2026-11-01","dueDate":"2026-11-30","status":"ACTIVE","installments":[]}`.
  `kind` is `LENT` or `BORROWED`; status is derived from balance and due date
  where applicable. Installments contain `amount` and `paymentDate`.

### Audit and internal notes

- `GET /api/admin/audit-log` returns rows
  `{"id":1,"action":"ACCOUNT_SUSPENDED","targetType":"USER","actorName":"Admin","targetId":2,"targetName":"Bob","details":"Account status changed to SUSPENDED","createdAt":"2026-11-18T10:00:00"}`.
  `targetType` is `USER`, `LOAN`, `ANNOUNCEMENT`, or `ACCOUNT`; user timeline
  entries can be selected by `targetType=USER`.
- `GET /api/admin/users/{id}/notes` returns an array of
  `{"id":1,"note":"Follow up","authorName":"Admin","createdAt":"2026-11-18T10:00:00"}`.
- `POST /api/admin/users/{id}/notes` accepts `{"note":"Follow up"}` and returns
  the created note.

### Announcements and notifications

- `GET /api/admin/announcements` returns rows
  `{"id":1,"title":"Notice","message":"Text","audience":"ALL_USERS","channel":"IN_APP","createdAt":"2026-11-18T10:00:00","authorName":"Admin","recipientCount":3,"targetUserId":null,"targetName":null}`.
- `POST /api/admin/announcements` accepts
  `{"title":"Notice","message":"Text","audience":"ALL_USERS","channel":"IN_APP"}`.
  Audiences: `ALL_USERS`, `BORROWERS_WITH_OVERDUE`, `LENDERS_ONLY`,
  `ADMINISTRATORS`; channels: `IN_APP`, `EMAIL`. `SMS` is intentionally
  unavailable and returns HTTP 400. For an individual message use
  `{"title":"Notice","message":"Text","audience":"ALL_USERS","channel":"IN_APP","targetUserId":2}`;
  `targetUserId` must refer to an existing account and takes precedence over
  `audience` (including `ALL_USERS`); only that account receives the message.
  Its response/list includes `targetUserId` and a persisted `targetName`, and
  uses audience `TARGET_USER`. Email delivery uses
  the configured SMTP/Resend provider.
- `GET /api/notifications` returns the current user's rows
  `{"id":1,"title":"Notice","message":"Text","createdAt":"2026-11-18T10:00:00","read":false}`.
- `POST /api/notifications/{id}/read` marks only the signed-in user's
  notification as read.

### Overdue reminders

- `POST /api/admin/loans/{kind}/{id}/remind` sends an email and returns
  `{"sent":1}`. `{kind}` is required and must be `LENT` or `BORROWED`.
- `POST /api/admin/reminders/overdue` sends email reminders for all overdue
  loan rows and returns `{"sent":N}`. Both routes require stored
  `counterpartyEmail`; missing email results in HTTP 409. Reminder delivery is
  email-only.

Loan create/read DTOs accept and expose optional `counterpartyEmail`, used for
reminders.

---

## Dashboard

### GET /api/dashboard/summary

Returns a summary of loan totals and active/overdue counts.

#### Response

```json
{
  "totalLent": 50000.0,
  "totalBorrowed": 20000.0,
  "amountOwedToMe": 30000.0,
  "amountIOwe": 10000.0,
  "activeLoansLent": 12,
  "activeLoansBorrowed": 5,
  "overdueLoans": 3
}
```

---

## Loan Lent Endpoints

### POST /api/loans-lent

Create a new loan record for money you lent.

#### Request body

```json
{
  "personName": "John Doe",
  "phoneNumber": "+254700000000",
  "amountLent": 5000,
  "dateLent": "2026-06-01",
  "dueDate": "2026-08-01",
  "notes": "Loan for car repair"
}
```

#### Response
- `201 Created`
- Returns created `LoanLent` object.

### GET /api/loans-lent

Get all lent loan records.

#### Response
- `200 OK`
- Returns array of `LoanLent` objects.

### GET /api/loans-lent/{id}

Get details for a single lent loan.

#### Response
- `200 OK` with `LoanLent` object if found.
- `404 Not Found` if not found.

### PUT /api/loans-lent/{id}

Update an existing lent loan record.

#### Request body

```json
{
  "personName": "John Doe",
  "phoneNumber": "+254700000000",
  "amountLent": 5200,
  "dateLent": "2026-06-01",
  "dueDate": "2026-08-10",
  "notes": "Updated loan amount"
}
```

#### Response
- `200 OK` with updated `LoanLent`.
- `404 Not Found` if loan id does not exist.

### DELETE /api/loans-lent/{id}

Delete a lent loan record.

#### Response
- `204 No Content`

### POST /api/loans-lent/{id}/payment

Record a payment received on a lent loan.

#### Request body

```json
{
  "amount": 500
}
```

#### Behavior
- `amountPaid` increases by the payment amount.
- `balance` decreases accordingly.
- `status` updates automatically.

#### Response
- `200 OK` with updated `LoanLent`.
- `400 Bad Request` for invalid payment.
- `409 Conflict` if loan is already fully paid.

---

## Loan Borrowed Endpoints

### POST /api/loans-borrowed

Create a new loan record for money you borrowed.

#### Request body

```json
{
  "personName": "Mary Smith",
  "phoneNumber": "+254711111111",
  "amountBorrowed": 3000,
  "dateBorrowed": "2026-06-01",
  "dueDate": "2026-07-01",
  "notes": "Borrowed for rent"
}
```

#### Response
- `201 Created`
- Returns created `LoanBorrowed` object.

### GET /api/loans-borrowed

Get all borrowed loan records.

#### Response
- `200 OK`
- Returns array of `LoanBorrowed` objects.

### GET /api/loans-borrowed/{id}

Get details for a single borrowed loan.

#### Response
- `200 OK` with `LoanBorrowed` object if found.
- `404 Not Found` if not found.

### PUT /api/loans-borrowed/{id}

Update an existing borrowed loan record.

#### Request body

```json
{
  "personName": "Mary Smith",
  "phoneNumber": "+254711111111",
  "amountBorrowed": 3000,
  "dateBorrowed": "2026-06-01",
  "dueDate": "2026-07-05",
  "notes": "Extended repayment date"
}
```

#### Response
- `200 OK` with updated `LoanBorrowed`.
- `404 Not Found` if loan id does not exist.

### DELETE /api/loans-borrowed/{id}

Delete a borrowed loan record.

#### Response
- `204 No Content`

### POST /api/loans-borrowed/{id}/payment

Record a repayment for a borrowed loan.

#### Request body

```json
{
  "amount": 500
}
```

#### Behavior
- `amountPaid` increases by the payment amount.
- `balance` decreases accordingly.
- `status` updates automatically.

#### Response
- `200 OK` with updated `LoanBorrowed`.
- `400 Bad Request` for invalid payment.
- `409 Conflict` if loan is already fully paid.

---

## Notes

- `status` is derived from `balance`, `amountPaid`, and `dueDate`.
- `PAID` means the loan balance is zero.
- `OVERDUE` means the `dueDate` is before the current date and the balance is still positive.
- `PARTIALLY_PAID` means some payment has been made but balance remains.
- `ACTIVE` means the loan is current and has not received any payment yet.

---

## Health Endpoints

### GET /
### GET /health

Check the health status of the backend API.

#### Response
- `200 OK` if the backend is healthy.
- `503 Service Unavailable` if there is an issue.

```json
{
  "status": "UP",
  "timestamp": "2026-06-10T11:40:00Z",
  "message": "Backend is healthy"
}
```

---

## Admin Endpoints

Admin write endpoints require a user with admin access, typically authenticated via a JWT token. Stored role values `ADMIN` and `ROLE_ADMIN` are treated equivalently; support agents can read admin data but cannot perform admin writes.

### POST /api/admin/login

Login as an admin user.

#### Request body

```json
{
  "email": "admin@example.com",
  "password": "SecurePassword123"
}
```

#### Response
- `200 OK`
- Returns user details, access token, and refresh token.

### GET /api/admin/users

Get all user and admin accounts with account and loan-count summaries. Requires admin privileges.

#### Response
- `200 OK`
- Returns an array of `UserSummaryDto` objects.
- `403 Forbidden` if not an admin.

### GET /api/admin/users/{id}

Get one account, including its lent and borrowed loans and payment installments. Requires admin privileges.

#### Response
- `200 OK` with a `UserDetailDto`.
- `403 Forbidden` if not an admin.
- `404 Not Found` if the account does not exist.

### POST /api/admin/users/{id}/reset-password

Reset a user's password. Requires admin privileges.

#### Request body

```json
{
  "password": "NewSecurePassword123"
}
```

#### Response
- `200 OK` with `ResetPasswordResponse` containing the new password.
- `400 Bad Request` if password is missing or blank.
- `403 Forbidden` if not an admin.
- `404 Not Found` if the account does not exist.

### DELETE /api/admin/users/{id}

Delete a user from the system. Requires admin privileges.

#### Response
- `200 OK` with a success message.
- `403 Forbidden` if not an admin.
- `404 Not Found` if the account does not exist.
- `409 Conflict` if the signed-in administrator attempts to delete their own account.

### POST /api/admins

Create a new administrator. Requires an authenticated administrator; the new admin must change their password after signing in.

#### Request body

```json
{
  "fullName": "Admin User",
  "email": "admin2@example.com",
  "phoneNumber": "+254722000000",
  "password": "SecurePassword123",
  "confirmPassword": "SecurePassword123"
}
```

#### Response
- `201 Created`
- Returns created admin user details along with access token and refresh token.
- `400 Bad Request` if any fields are blank or passwords do not match.
- `401 Unauthorized` if no valid access token is supplied.
- `403 Forbidden` if the caller is not an administrator.
- `409 Conflict` if the email address is already registered.

### GET /api/admin/profile

Get the signed-in administrator's profile and whether a password change is required. Requires admin privileges.

### POST /api/admin/change-password

Change the signed-in administrator's password. The current password is required unless the profile indicates `mustChangePassword: true`. A successful change clears that flag.
