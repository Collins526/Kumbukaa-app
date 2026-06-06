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
