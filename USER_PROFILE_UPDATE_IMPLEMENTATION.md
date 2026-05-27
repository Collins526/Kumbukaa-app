# User Profile Update & Role Selection Flow - Implementation Guide

## Overview
This implementation adds the ability for users to:
1. Register without selecting a role
2. Log in with their email and password
3. Update their account profile
4. Choose to become a lender or borrower (or both, through separate calls)

## Flow: Register → Login → Update Account → Choose Role

### Step 1: Register a New User (No Role Required)
**Endpoint:** `POST /api/auth/register`

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "phoneNumber": "+254712345678",
  "password": "SecurePassword123",
  "confirmPassword": "SecurePassword123"
}
```

**Response:**
```json
"User registered successfully. Choose to lend or borrow after login."
```

Note: The `role` field is no longer required during registration.

---

### Step 2: Login to Get JWT Token
**Endpoint:** `POST /api/auth/login`

```json
{
  "email": "john@example.com",
  "password": "SecurePassword123"
}
```

**Response:**
```json
{
  "id": 1,
  "userId": 1,
  "email": "john@example.com",
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "isVerified": true,
  "message": "Login successful"
}
```

**Important:** Save the `token` value - you'll use it in the Authorization header for authenticated requests.

---

### Step 3: Update User Account Profile
**Endpoint:** `PUT /api/users/profile/update`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "John Updated Name",
  "phoneNumber": "+254787654321",
  "email": "newemail@example.com",
  "username": "johnupdated"
}
```

**Response:**
```json
{
  "id": 1,
  "name": "John Updated Name",
  "email": "newemail@example.com",
  "phoneNumber": "+254787654321",
  "password": "$2a$10...",
  "role": "BORROWER"
}
```

---

### Step 4: Get Current User Profile
**Endpoint:** `GET /api/users/profile`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Response:**
```json
{
  "id": 1,
  "name": "John Updated Name",
  "email": "newemail@example.com",
  "phoneNumber": "+254787654321",
  "password": "$2a$10...",
  "role": null
}
```

---

### Step 5: Choose Your Role (Lender or Borrower)
**Endpoint:** `POST /api/users/role/set`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "role": "BORROWER"
}
```

**Alternative:**
```json
{
  "role": "LENDER"
}
```

**Response:**
```json
{
  "id": 1,
  "name": "John Updated Name",
  "email": "newemail@example.com",
  "phoneNumber": "+254787654321",
  "password": "$2a$10...",
  "role": "BORROWER"
}
```

This endpoint will:
- Set the user's role to BORROWER or LENDER
- Automatically create the corresponding Borrower or Lender record
- Return the updated user with the role set

---

### Alternative Flow: Get User by ID (Requires JWT)
**Endpoint:** `GET /api/users/{id}`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Response:**
```json
{
  "id": 1,
  "name": "John Updated Name",
  "email": "newemail@example.com",
  "phoneNumber": "+254787654321",
  "password": "$2a$10...",
  "role": "BORROWER"
}
```

---

## Files Created/Modified

### New Files:
1. **UserController.java** - REST controller for user endpoints
   - `GET /api/users/profile` - Get current logged-in user's profile
   - `PUT /api/users/profile/update` - Update user profile
   - `GET /api/users/{id}` - Get specific user by ID

2. **UserService.java** - Business logic for user operations
   - `updateProfile()` - Update user information with validation
   - `getUserProfile()` - Retrieve user profile
   - `findByEmail()` - Find user by email
   - `emailExists()` - Check if email exists

3. **UpdateProfileRequest.java** - DTO for profile update requests
   - `name` - User's full name
   - `phoneNumber` - User's phone number
   - `email` - User's email address
   - `username` - User's username

### Modified Files:
1. **UserRepository.java** - Added query methods
   - `findByEmail(String email)` - Find user by email
   - `existsByEmail(String email)` - Check if email exists

---

## Security Features

✅ **JWT Token Authentication**
- All user endpoints require a valid JWT token in the Authorization header
- Token is obtained after successful login
- Token includes user ID and email as claims

✅ **Validation**
- Email format validation
- Email uniqueness validation (no duplicates)
- Required field checks

✅ **Authorization**
- Users can only update their own profile (identified by JWT token)
- Auth records are updated in sync with User records

---

## Testing the Flow

### Using Postman:

1. **Register**
   - POST to `/api/auth/register`
   - Include name, email, phone, password, role

2. **Login**
   - POST to `/api/auth/login`
   - Include email and password
   - Copy the `token` from response

3. **Update Profile**
   - PUT to `/api/users/profile/update`
   - Add header: `Authorization: Bearer {paste_token_here}`
   - Update any fields you want to change

4. **Verify Updates**
   - GET to `/api/users/profile`
   - Add header: `Authorization: Bearer {token}`
   - Verify your changes are reflected

---

## Error Handling

| Status | Error | Cause |
|--------|-------|-------|
| 401 | "Authorization token required" | Missing or malformed Authorization header |
| 401 | "Invalid or expired token" | Token is invalid or has expired |
| 400 | "User not found" | User ID from token doesn't exist |
| 400 | "Invalid email format" | Email doesn't match email pattern |
| 400 | "Email already exists" | Email is already registered by another user |

---

## Next Steps

- Test the complete flow with your Postman collection
- Add more profile fields as needed (avatar, bio, etc.)
- Consider adding email verification for email changes
- Add password change endpoint if needed
