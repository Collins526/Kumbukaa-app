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

Admin endpoints require a user with `ROLE_ADMIN` authority, typically authenticated via a JWT token.

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

Get a list of all users along with their loan statistics. Requires admin privileges.

#### Response
- `200 OK`
- Returns an array of `UserAdminDto` objects.
- `403 Forbidden` if not an admin.

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

### DELETE /api/admin/users/{id}

Delete a user from the system. Requires admin privileges.

#### Response
- `204 No Content`
- `403 Forbidden` if not an admin.

### POST /api/admins

Create a new admin user.

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
