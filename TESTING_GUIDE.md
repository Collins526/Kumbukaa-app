# Kumbukaa Personal Loan Tracker Testing Guide

## 1. Start the application

Run the Spring Boot app from the project root:

```powershell
./mvnw.cmd spring-boot:run
```

The API should be available at:

```text
http://localhost:8080
```

### Environment setup

The app now reads its database settings from the local `.env` file or deployment environment variables. For local development, ensure the following are present:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-eu-north-1.pooler.supabase.com:6543/postgres?sslmode=require
SPRING_DATASOURCE_HOST=aws-0-eu-north-1.pooler.supabase.com
SPRING_DATASOURCE_PORT=6543
SPRING_DATASOURCE_DB=postgres
SPRING_DATASOURCE_USERNAME=postgres.cpnxdlffzjgvemoptbzz
SPRING_DATASOURCE_PASSWORD=<your-supabase-password>

# Optional but recommended for Supabase/pgbouncer compatibility to avoid
# prepared statement errors such as "prepared statement already exists"
SPRING_DATASOURCE_HIKARI_DATA_SOURCE_PROPERTIES_PREPARETHRESHOLD=0
```

---

## 2. Test tools

You can test using:

- `curl` from the terminal
- Postman / Insomnia / HTTP client
- VS Code REST Client extension

---

## 3. Auth endpoints

### Register a new user

POST `/api/auth/register`

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Alice Mwangi",
    "email": "alice@example.com",
    "phoneNumber": "+254700000000",
    "password": "SecurePassword123",
    "confirmPassword": "SecurePassword123"
  }'
```

Expected response:

```json
{
  "userId": 1,
  "email": "alice@example.com",
  "name": "Alice Mwangi",
  "message": "Registration successful",
  "token": "<jwt-token>",
  "refreshToken": "<refresh-token>"
}
```

### Login with email and password

POST `/api/auth/login`

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "SecurePassword123"
  }'
```

Expected response:

```json
{
  "userId": 1,
  "email": "alice@example.com",
  "name": "Alice Mwangi",
  "message": "Login successful",
  "token": "<jwt-token>",
  "refreshToken": "<refresh-token>"
}
```

Use the returned `token` value in the `Authorization: Bearer <jwt-token>` header for protected dashboard and loan endpoints. Access tokens expire after 24 hours, so refresh the login flow when the token expires.

### Request OTP for login

POST `/api/auth/request-otp`

```bash
curl -X POST http://localhost:8080/api/auth/request-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com"
  }'
```

Expected response:

```json
"OTP has been sent to the email."
```

The OTP is sent to the registered email address. The response no longer includes the OTP code.

### Forgot password

POST `/api/auth/forgot-password`

```bash
curl -X POST http://localhost:8080/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com"
  }'
```

Expected responses:

If the account exists:

```json
{
  "message": "a verification code has been sent"
}
```

If the account does not exist:

```json
{
  "message": "account does not exist"
}
```

### Verify OTP for password reset

POST `/api/auth/verify-otp`

```bash
curl -X POST http://localhost:8080/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "otp": "123456"
  }'
```

Expected response:

```json
{
  "resetToken": "<uuid-or-token>",
  "message": "OTP verified successfully."
}
```

### Reset password

POST `/api/auth/reset-password`

```bash
curl -X POST http://localhost:8080/api/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "resetToken": "<token-from-verify-otp>",
    "newPassword": "Password@123",
    "confirmPassword": "Password@123"
  }'
```

Expected response:

```json
{
  "message": "Password reset successfully."
}
```

New passwords must meet the project policy: minimum 8 characters, one uppercase, one lowercase, one number, and one special character.

### Login with OTP

POST `/api/auth/login-otp`

```bash
curl -X POST http://localhost:8080/api/auth/login-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "code": "123456"
  }'
```

Expected response:

```json
{
  "userId": 1,
  "email": "alice@example.com",
  "name": "Alice Mwangi",
  "message": "Login with OTP successful",
  "token": "<jwt-token>",
  "refreshToken": "<refresh-token>"
}
```

Use the returned `token` value in the `Authorization: Bearer <jwt-token>` header for protected dashboard and loan endpoints.

---



## 4. Dashboard endpoint

### GET /api/dashboard/summary

Request:

```bash
curl -H "Authorization: Bearer <jwt-token>" http://localhost:8080/api/dashboard/summary
```

Expected response:

```json
{
  "totalLent": 0.0,
  "totalBorrowed": 0.0,
  "amountOwedToMe": 0.0,
  "amountIOwe": 0.0,
  "activeLoansLent": 0,
  "activeLoansBorrowed": 0,
  "overdueLoans": 0
}
```

---

## 4. Loan Lent endpoints

### Create a loan lent record

POST `/api/loans-lent`

```bash
curl -X POST http://localhost:8080/api/loans-lent \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "personName": "John Doe",
    "phoneNumber": "+254700000000",
    "amountLent": 5000,
    "dateLent": "2026-06-01",
    "dueDate": "2026-07-01",
    "notes": "Personal loan"
  }'
```

Expected response: created `LoanLent` object with `id`, `amountPaid`, `balance`, and `status`.

### Get all lent loans

GET `/api/loans-lent`

```bash
curl http://localhost:8080/api/loans-lent
```

### Get one lent loan by id

GET `/api/loans-lent/{id}`

```bash
curl http://localhost:8080/api/loans-lent/1
```

### Update a lent loan

PUT `/api/loans-lent/{id}`

```bash
curl -X PUT http://localhost:8080/api/loans-lent/1 \
  -H "Content-Type: application/json" \
  -d '{
    "personName": "John Doe",
    "phoneNumber": "+254700000000",
    "amountLent": 5200,
    "dateLent": "2026-06-01",
    "dueDate": "2026-07-15",
    "notes": "Updated amount"
  }'
```

### Delete a lent loan

DELETE `/api/loans-lent/{id}`

```bash
curl -X DELETE http://localhost:8080/api/loans-lent/1
```

### Record payment for a lent loan

POST `/api/loans-lent/{id}/payment`

```bash
curl -X POST http://localhost:8080/api/loans-lent/1/payment \
  -H "Content-Type: application/json" \
  -d '{ "amount": 500 }'
```

Behavior:

- `amountPaid` increases by the payment amount
- `balance` decreases
- `status` updates to `PARTIALLY_PAID`, `PAID`, or `OVERDUE`

---

## 5. Loan Borrowed endpoints

### Create a loan borrowed record

POST `/api/loans-borrowed`

```bash
curl -X POST http://localhost:8080/api/loans-borrowed \
  -H "Content-Type: application/json" \
  -d '{
    "personName": "Mary Smith",
    "phoneNumber": "+254711111111",
    "amountBorrowed": 3000,
    "dateBorrowed": "2026-06-01",
    "dueDate": "2026-07-01",
    "notes": "Borrowed for rent"
  }'
```

### Get all borrowed loans

GET `/api/loans-borrowed`

```bash
curl http://localhost:8080/api/loans-borrowed
```

### Get one borrowed loan by id

GET `/api/loans-borrowed/{id}`

```bash
curl http://localhost:8080/api/loans-borrowed/1
```

### Update a borrowed loan

PUT `/api/loans-borrowed/{id}`

```bash
curl -X PUT http://localhost:8080/api/loans-borrowed/1 \
  -H "Content-Type: application/json" \
  -d '{
    "personName": "Mary Smith",
    "phoneNumber": "+254711111111",
    "amountBorrowed": 3000,
    "dateBorrowed": "2026-06-01",
    "dueDate": "2026-07-10",
    "notes": "Extended repayment"
  }'
```

### Delete a borrowed loan

DELETE `/api/loans-borrowed/{id}`

```bash
curl -X DELETE http://localhost:8080/api/loans-borrowed/1
```

### Record repayment for a borrowed loan

POST `/api/loans-borrowed/{id}/payment`

```bash
curl -X POST http://localhost:8080/api/loans-borrowed/1/payment \
  -H "Content-Type: application/json" \
  -d '{ "amount": 500 }'
```

Behavior:

- `amountPaid` increases by the payment amount
- `balance` decreases
- `status` updates automatically

---

## 6. Notes and validation

- `amountLent` and `amountBorrowed` must be greater than zero.
- Payment `amount` must be greater than zero.
- `status` updates based on balance and due date.
- If a loan is fully paid, the API returns `PAID`.
- If the due date is past and balance remains, the status becomes `OVERDUE`.

---

## 7. Postman collection

Use the above requests as individual Postman examples:

1. Create one `LoanLent` record
2. Create one `LoanBorrowed` record
3. Fetch `/api/dashboard/summary`
4. Update each loan
5. Record payment on each loan
6. Confirm balances and status updates

---

## 8. Admin Endpoints

This system uses a **single ADMIN account** with no super-admin role or admin management features. The default ADMIN is created automatically on first startup with default credentials that **must be changed** after first login.

### 8.1 Admin Login

**Endpoint:** `POST /api/auth/admin/login`

Description: Admin logs in to receive JWT access and refresh tokens.

Request:

```bash
curl -X POST http://localhost:8080/api/auth/admin/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@localhost",
    "password": "admin"
  }'
```

Expected response (first login):

```json
{
  "userId": 1,
  "email": "admin@localhost",
  "name": "Administrator",
  "message": "Admin login successful - password change required",
  "token": "<jwt-token>",
  "refreshToken": "<refresh-token>"
}
```

If password was already changed:

```json
{
  "userId": 1,
  "email": "admin@localhost",
  "name": "Administrator",
  "message": "Admin login successful",
  "token": "<jwt-token>",
  "refreshToken": "<refresh-token>"
}
```

---

### 8.2 Admin Profile

**Endpoint:** `GET /api/admin/profile`

Description: Returns the admin's profile information.

Request:

```bash
curl -X GET http://localhost:8080/api/admin/profile \
  -H "Authorization: Bearer <jwt-token>"
```

Expected response:

```json
{
  "id": 1,
  "email": "admin@localhost",
  "fullName": "Administrator",
  "phoneNumber": "0000000000"
}
```

---

### 8.3 Change Admin Password

**Endpoint:** `POST /api/admin/change-password`

Description: Admin changes their password. When `mustChangePassword` is true (on first login), the current password is not required.

Request (first password change — no current password required):

```bash
curl -X POST http://localhost:8080/api/admin/change-password \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "newPassword": "NewSecurePassword123"
  }'
```

Request (subsequent password changes — current password required):

```bash
curl -X POST http://localhost:8080/api/admin/change-password \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "NewSecurePassword123",
    "newPassword": "AnotherSecurePassword456"
  }'
```

Expected response:

```
Password updated
```

---

### 8.4 View All Users

**Endpoint:** `GET /api/admin/users`

Description: Lists all users in the system with their loans.

Request:

```bash
curl -X GET http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer <jwt-token>"
```

Expected response:

```json
[
  {
    "id": 2,
    "fullName": "Alice Mwangi",
    "email": "alice@example.com",
    "phoneNumber": "+254700000000",
    "loansLent": [...],
    "loansBorrowed": [...]
  }
]
```

---

### 8.5 Get User by ID

**Endpoint:** `GET /api/admin/users/{id}`

Description: Admin retrieves a single user with loan details.

Request:

```bash
curl -X GET http://localhost:8080/api/admin/users/2 \
  -H "Authorization: Bearer <jwt-token>"
```

Expected response:

```json
{
  "id": 2,
  "fullName": "Alice Mwangi",
  "email": "alice@example.com",
  "phoneNumber": "+254700000000",
  "loansLent": [...],
  "loansBorrowed": [...]
}
```

---

### 8.6 Reset User Password

**Endpoint:** `POST /api/admin/users/{id}/reset-password`

Description: Admin sets a new password for a user. Password is hashed with BCrypt.

Request:

```bash
curl -X POST http://localhost:8080/api/admin/users/2/reset-password \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{"password": "NewUserPassword123"}'
```

Expected response:

```json
{
  "message": "New password created",
  "password": "NewUserPassword123"
}
```

---

### 8.7 Delete User

**Endpoint:** `DELETE /api/admin/users/{id}`

Description: Admin removes a user from the system.

Request:

```bash
curl -X DELETE http://localhost:8080/api/admin/users/2 \
  -H "Authorization: Bearer <jwt-token>"
```

Expected response:

```json
{
  "message": "user deleted successfully"
}
```

---

### Default Admin Account

On first startup, if no admin exists, the system automatically creates:

- **Email:** `admin@localhost` (override with `app.admin.email` env var)
- **Password:** `admin` (override with `app.admin.password` env var)
- **Phone:** `0000000000` (override with `app.admin.phone` env var)
- **Status:** `mustChangePassword = true`

The admin must log in and change the password via `POST /api/admin/change-password`.

---

### Security

- All admin endpoints require a valid JWT with `ROLE_ADMIN` authority.
- Passwords are hashed with BCrypt (industry-standard).
- Legacy SHA-256 hashed passwords (from older deployments) are transparently migrated to BCrypt on login.
- The system enforces a **single admin account** — new admins cannot be created via the API once initialized.
- Admin operations are role-based: the `TokenAuthenticationFilter` reads the `roles` field from the `app_user` table and grants `ROLE_ADMIN` authority.

