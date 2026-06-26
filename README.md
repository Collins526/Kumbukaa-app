# Kumbukaa Lending App Documentation

## 1. Project Overview
Kumbukaa Lending App is a Spring Boot REST API built to manage and track personal loans. It is designed to track two primary scenarios:
- **Loans Lent**: Money you have lent out to other individuals.
- **Loans Borrowed**: Money you have borrowed from other individuals.

It provides functionalities to create, update, retrieve, and delete loan records, record partial or full payments, calculate remaining balances and statuses (ACTIVE, PARTIALLY_PAID, PAID, OVERDUE), and visualize dashboard summaries. It also includes comprehensive user authentication and an admin panel.

## 2. Technology Stack
- **Framework**: Spring Boot 3.5.15
- **Language**: Java 21
- **Database**: PostgreSQL (v42.7.3)
- **Data Access**: Spring Data JPA / Hibernate
- **Security**: Spring Security with JWT (JSON Web Tokens)
- **Build Tool**: Maven
- **Additional Libraries**: Lombok (Boilerplate reduction), Spring Boot Mail (Email & OTP processing)

## 3. Project Structure

The project follows a standard layered Spring MVC architecture. Below is a breakdown of the `src/main/java/com/kumbukaa` directory:

- **`config`**: Contains application configuration classes (e.g., `SecurityConfig`, `CorsConfig`, `JwtTokenProvider`, `DotenvEnvironmentPostProcessor`).
- **`controller`**: REST API endpoints mapping the HTTP requests to backend services (e.g., `AuthController`, `LoanLentController`, `LoanBorrowedController`, `DashboardController`, `AdminController`).
- **`service`**: Contains the core business logic (e.g., `AuthService`, `LoanLentService`, `DashboardService`, `AdminService`, `EmailService`).
- **`repository`**: Spring Data JPA interfaces for database interaction (`UserRepository`, `LoanLentRepository`, `LoanBorrowedRepository`, `OtpCodeRepository`).
- **`entity`**: JPA Data models representing the database tables (`User`, `LoanLent`, `LoanBorrowed`, `LoanPayment`, `OtpCode`).
- **`dto`**: Data Transfer Objects used to pass data between the client and server without exposing internal entities.
- **`enums`**: Enumerations used across the project (e.g., Loan statuses like `ACTIVE`, `PAID`, `OVERDUE`).
- **`mapper`**: Utilities for mapping between Entities and DTOs.
- **`event` & `listener`**: Spring application events and listeners (e.g., for async processing or notifications).
- **`util`**: Shared helper and utility classes.

## 4. Key Entities

- **`User`**: Represents a registered user. Holds basic details (name, email, phone) and login credentials.
- **`LoanLent`**: A record of money the user has lent. It tracks the borrower's info, amount lent, amount paid, balance, dates, and status.
- **`LoanBorrowed`**: A record of money the user has borrowed. It tracks the lender's info, amount borrowed, amount paid, balance, dates, and status.
- **`LoanPayment`**: A record of individual payments/installments made towards a specific loan.
- **`OtpCode`**: Used to track One-Time Passwords for the OTP authentication flow.

## 5. Security & Authentication
The API utilizes stateless authentication using **JWT (JSON Web Tokens)**:
- Users can register and login using their email and password.
- An OTP-based login flow is also supported via `request-otp` and `login-otp`.
- Protected endpoints require an `Authorization: Bearer <jwt-token>` header.
- Admin endpoints check for `ROLE_ADMIN` authority on the JWT token.
- Passwords are securely hashed using BCrypt.

## 6. Core Features & Endpoints

### 6.1 Authentication (`/api/auth`)
Handles user registration, standard login, and OTP-based login.
- `POST /register`: Register a new user.
- `POST /login`: Standard email/password login.
- `POST /request-otp` / `POST /login-otp`: OTP flow.

### 6.2 Loans Lent (`/api/loans-lent`)
Track money owed to the user.
- `GET`, `POST`, `PUT`, `DELETE` operations for managing lent loan records.
- `POST /{id}/payment`: Records an incoming payment, reducing the balance and automatically recalculating the status.

### 6.3 Loans Borrowed (`/api/loans-borrowed`)
Track money the user owes to others.
- `GET`, `POST`, `PUT`, `DELETE` operations for borrowed loans.
- `POST /{id}/payment`: Records an outgoing payment toward the borrowed loan.

### 6.4 Dashboard (`/api/dashboard`)
- `GET /summary`: Returns an aggregated view of the user's financial standing, including total lent, total borrowed, amounts remaining, active loans, and overdue counts.

### 6.5 Admin Management (`/api/admin`)
- Accessible only by the default admin (credentials initialized dynamically or via env).
- Allows the admin to view all users, reset user passwords, delete users, and view system metrics.

## 7. How to Run

1. **Environment Variables**: Configure PostgreSQL credentials and JWT secrets via the `.env` file (see `.env.example`).
2. **Start the App**:
   ```bash
   ./mvnw spring-boot:run
   ```
   (Or `./mvnw.cmd` on Windows)
3. **Verify Health**: The API should be running on `http://localhost:8080`. Check `http://localhost:8080/health` or `http://localhost:8080/` to ensure the server is UP.

## 8. Reference Documents
For more granular details, refer to the other markdown files in the project root:
- **`API_DOCUMENTATION.md`**: Complete definitions of API endpoints, request bodies, and expected responses.
- **`TESTING_GUIDE.md`**: Step-by-step cURL commands and workflows for testing the application (including auth, loans, and admin flows).
