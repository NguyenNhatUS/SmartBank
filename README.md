# SmartBank — Mini Banking System API Documentation

This document provides a comprehensive guide to the primary API endpoints of the **SmartBank** system, detailing their request formats (Request Payload), expected success responses (Response Payload), authorization requirements, and error structures.

---

## 🛠️ Tech Stack & Architecture
- **Core Framework**: Java Spring Boot 3.x, Spring Security 6
- **Database**: MySQL 8.0, Hibernate JPA
- **Caching**: Redis (via Spring Cache abstractions)
- **Authentication**: Stateless JWT with Access Token + Refresh Token Rotation
- **Containerization**: Docker Compose

---

## 📁 Project Structure

```
src/main/java/com/SmartBank/
│
├── SmartBankApplication.java
│
├── config/
│   ├── RedisConfig.java
│   └── SecurityConfig.java
│
├── controller/
│   ├── AccountController.java
│   ├── AuthController.java
│   ├── CustomerController.java
│   └── TransactionController.java
│
├── dto/
│   ├── request/
│   │   ├── AccountCreateRequest.java
│   │   ├── CreateEmployeeRequest.java
│   │   ├── CustomerRequest.java
│   │   ├── DepositWithDrawRequest.java
│   │   ├── LoginRequest.java
│   │   ├── RefreshRequest.java
│   │   ├── RegisterRequest.java
│   │   └── TransferRequest.java
│   │
│   └── response/
│       ├── AccountResponse.java
│       ├── CustomerAccountResponse.java
│       ├── CustomerResponse.java
│       ├── ErrorResponse.java
│       ├── LoginResponse.java
│       └── TransactionResponse.java
│
├── entity/
│   ├── Account.java
│   ├── Customer.java
│   ├── Employee.java
│   ├── RefreshToken.java
│   ├── Transaction.java
│   └── enums/
│       ├── AccountStatus.java
│       ├── AccountType.java
│       ├── CustomerStatus.java
│       ├── ErrorCode.java
│       ├── Role.java
│       └── TransactionType.java
│
├── exception/
│   ├── AppException.java
│   ├── CustomAccessDeniedHandler.java
│   ├── CustomAuthenticationEntryPoint.java
│   └── GlobalExceptionHandler.java
│
├── mapper/
│   ├── AccountMapper.java
│   ├── CustomerMapper.java
│   └── TransactionMapper.java
│
├── repository/
│   ├── AccountRepository.java
│   ├── CustomerRepository.java
│   ├── EmployeeRepository.java
│   ├── RefreshTokenRepository.java
│   └── TransactionRepository.java
│
├── security/
│   ├── JwtAuthenticationFilter.java
│   ├── JwtTokenProvider.java
│   ├── RateLimit.java
│   └── RateLimitAspect.java
│
└── service/
    ├── AccountService.java
    ├── AuthService.java
    ├── CustomUserDetailsService.java
    ├── CustomerService.java
    └── TransactionService.java
```

---

## 🔑 Authentication & Authorization

All secure endpoints require the client to supply a JSON Web Token (JWT) in the HTTP headers:
```http
Authorization: Bearer <your_access_token>
```

### Role-Based Access Control (RBAC)
- `CUSTOMER`: Allowed to manage self-service accounts (`/api/v1/accounts/my`) and perform operations like deposit, withdraw, or transfer (`/api/v1/transactions/**`).
- `EMPLOYEE`: Allowed to inspect customer accounts, view transaction logs, and freeze/close accounts.
- `ADMIN`: Has full administration control over accounts, customer profiles, and system employees.

---

## 📌 Core API Endpoints

### 1. Authentication Endpoints
Implemented in: [AuthController.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/controller/AuthController.java)

#### Customer Self-Registration
- **Endpoint**: `POST /auth/register`
- **Access**: Public
- **Request Body** ([RegisterRequest.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/request/RegisterRequest.java)):
  ```json
  {
    "username": "nguyen_nhat",
    "email": "nhat.nguyen@example.com",
    "password": "SecurePassword123"
  }
  ```
- **Response**:
  ```json
  {
    "message": "Register successful"
  }
  ```

#### Login (Employee & Customer)
- **Endpoint**: `POST /auth/login`
- **Access**: Public
- **Request Body** ([LoginRequest.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/request/LoginRequest.java)):
  ```json
  {
    "username": "nguyen_nhat",
    "password": "SecurePassword123"
  }
  ```
- **Response** ([LoginResponse.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/response/LoginResponse.java)):
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJuZ3V5ZW5fbmhhdCIs...",
    "refreshToken": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "username": "nguyen_nhat",
    "role": "CUSTOMER"
  }
  ```

#### Refresh Access Token (Refresh Token Rotation)
- **Endpoint**: `POST /auth/refresh`
- **Access**: Public
- **Request Body** ([RefreshRequest.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/request/RefreshRequest.java)):
  ```json
  {
    "refreshToken": "7c9e6679-7425-40de-944b-e07fc1f90ae7"
  }
  ```
- **Response** ([LoginResponse.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/response/LoginResponse.java)):
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.new_jwt_content...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "username": "nguyen_nhat",
    "role": "CUSTOMER"
  }
  ```
  *(Note: The old refresh token is marked as revoked immediately upon use to prevent replay attacks.)*

#### Logout
- **Endpoint**: `POST /api/auth/logout`
- **Access**: Authenticated users
- **Response**:
  ```json
  {
    "message": "Logout successful"
  }
  ```

---

### 2. Administrator & Employee Management Endpoints
Implemented in: [AuthController.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/controller/AuthController.java)

#### Create System Employee/Admin
- **Endpoint**: `POST /admin/employees`
- **Access**: `ADMIN`
- **Request Body** ([CreateEmployeeRequest.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/request/CreateEmployeeRequest.java)):
  ```json
  {
    "username": "staff_member",
    "password": "StaffPassword123",
    "role": "EMPLOYEE"
  }
  ```
  *(Supported roles: `EMPLOYEE`, `ADMIN`)*
- **Response**: `201 Created` (No response body)

---

### 3. Customer Profile Management Endpoints
Implemented in: [CustomerController.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/controller/CustomerController.java)

> **Note**: These endpoints are utilized by administrators to manage customer profile details.

#### Create Customer Profile
- **Endpoint**: `POST /api/v1/customers`
- **Access**: `ADMIN`
- **Request Body** ([CustomerRequest.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/request/CustomerRequest.java)):
  ```json
  {
    "fullName": "Nguyen Nhat",
    "email": "nhat.nguyen@example.com",
    "phone": "0987654321",
    "address": "123 Le Loi Street, District 1, HCMC",
    "dateOfBirth": "1995-10-15"
  }
  ```
- **Response** ([CustomerResponse.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/response/CustomerResponse.java)):
  ```json
  {
    "id": 1,
    "fullName": "Nguyen Nhat",
    "email": "nhat.nguyen@example.com",
    "phone": "0987654321",
    "address": "123 Le Loi Street, District 1, HCMC",
    "dateOfBirth": "1995-10-15",
    "status": "ACTIVE",
    "createdAt": "2026-05-21T15:00:00",
    "totalAccounts": 0
  }
  ```

#### Update Customer Profile
- **Endpoint**: `PUT /api/v1/customers/{id}`
- **Access**: `ADMIN`
- **Request Body** ([CustomerRequest.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/request/CustomerRequest.java)):
  ```json
  {
    "fullName": "Nguyen Nhat (Updated)",
    "email": "nhat.nguyen.new@example.com",
    "phone": "0987654321",
    "address": "456 Nguyen Hue Street, District 1, HCMC",
    "dateOfBirth": "1995-10-15"
  }
  ```
- **Response** ([CustomerResponse.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/response/CustomerResponse.java)):
  ```json
  {
    "id": 1,
    "fullName": "Nguyen Nhat (Updated)",
    "email": "nhat.nguyen.new@example.com",
    "phone": "0987654321",
    "address": "456 Nguyen Hue Street, District 1, HCMC",
    "dateOfBirth": "1995-10-15",
    "status": "ACTIVE",
    "createdAt": "2026-05-21T15:00:00",
    "totalAccounts": 1
  }
  ```

#### Delete Customer Profile (Soft Delete)
- **Endpoint**: `DELETE /api/v1/customers/{id}`
- **Access**: `ADMIN`
- **Response**: `204 No Content` (Customer's status is toggled to `LOCKED` in the database)

---

### 4. Account Management Endpoints
Implemented in: [AccountController.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/controller/AccountController.java)

#### Create a Personal Bank Account
- **Endpoint**: `POST /api/v1/accounts/my`
- **Access**: `CUSTOMER`
- **Request Body** ([AccountCreateRequest.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/request/AccountCreateRequest.java)):
  ```json
  {
    "customerId": 1,
    "type": "CHECKING"
  }
  ```
  *(Supported account types: `SAVINGS`, `CHECKING`)*
- **Response** ([AccountResponse.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/response/AccountResponse.java)):
  ```json
  {
    "id": 12,
    "accountNumber": "1537284903",
    "type": "CHECKING",
    "balance": 0.0,
    "status": "ACTIVE",
    "createdAt": "2026-05-21T15:05:00",
    "customerId": 1,
    "customerName": "Nguyen Nhat"
  }
  ```

#### Get List of My Accounts
- **Endpoint**: `GET /api/v1/accounts/my`
- **Access**: `CUSTOMER`
- **Response**:
  ```json
  [
    {
      "id": 12,
      "accountNumber": "1537284903",
      "type": "CHECKING",
      "balance": 1500000.0,
      "status": "ACTIVE",
      "createdAt": "2026-05-21T15:05:00",
      "customerId": 1,
      "customerName": "Nguyen Nhat"
    }
  ]
  ```

#### Freeze Bank Account
- **Endpoint**: `PATCH /api/v1/accounts/{id}/freeze`
- **Access**: `EMPLOYEE` or `ADMIN`
- **Response** ([AccountResponse.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/response/AccountResponse.java)):
  ```json
  {
    "id": 12,
    "accountNumber": "1537284903",
    "type": "CHECKING",
    "balance": 1500000.0,
    "status": "FROZEN",
    "createdAt": "2026-05-21T15:05:00",
    "customerId": 1,
    "customerName": "Nguyen Nhat"
  }
  ```

---

### 5. Transaction Endpoints
Implemented in: [TransactionController.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/controller/TransactionController.java)

#### Deposit Funds
- **Endpoint**: `POST /api/v1/transactions/deposit`
- **Access**: `CUSTOMER`, `EMPLOYEE`, or `ADMIN`
- **Request Body** ([DepositWithDrawRequest.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/request/DepositWithDrawRequest.java)):
  ```json
  {
    "accountNumber": "1537284903",
    "amount": 500000,
    "description": "Cash deposit at counter"
  }
  ```
  *(Note: The minimum deposit amount is 1,000 VNĐ, and the maximum is 500,000,000 VNĐ per transaction)*
- **Response** ([TransactionResponse.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/response/TransactionResponse.java)):
  ```json
  {
    "id": 101,
    "transactionCode": "TXN1716283948293",
    "type": "DEPOSIT",
    "amount": 500000,
    "description": "Cash deposit at counter",
    "createdAt": "2026-05-21T15:10:00",
    "sourceAccountNumber": "1537284903",
    "targetAccountNumber": null
  }
  ```

#### Withdraw Funds
- **Endpoint**: `POST /api/v1/transactions/withdraw`
- **Access**: `CUSTOMER`, `EMPLOYEE`, or `ADMIN`
- **Request Body** ([DepositWithDrawRequest.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/request/DepositWithDrawRequest.java)):
  ```json
  {
    "accountNumber": "1537284903",
    "amount": 200000,
    "description": "Cash withdrawal at ATM"
  }
  ```
- **Response** ([TransactionResponse.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/response/TransactionResponse.java)):
  ```json
  {
    "id": 102,
    "transactionCode": "TXN1716283959182",
    "type": "WITHDRAW",
    "amount": 200000,
    "description": "Cash withdrawal at ATM",
    "createdAt": "2026-05-21T15:12:00",
    "sourceAccountNumber": "1537284903",
    "targetAccountNumber": null
  }
  ```

#### Internal Account Transfer
- **Endpoint**: `POST /api/v1/transactions/transfer`
- **Access**: `CUSTOMER`, `EMPLOYEE`, or `ADMIN`
- **Request Body** ([TransferRequest.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/request/TransferRequest.java)):
  ```json
  {
    "sourceAccountNumber": "1537284903",
    "targetAccountNumber": "9876543210",
    "amount": 100000,
    "description": "Fund transfer for lunch payment"
  }
  ```
- **Response** ([TransactionResponse.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/response/TransactionResponse.java)):
  ```json
  {
    "id": 103,
    "transactionCode": "TXN1716283969234",
    "type": "TRANSFER",
    "amount": 100000,
    "description": "Fund transfer for lunch payment",
    "createdAt": "2026-05-21T15:15:00",
    "sourceAccountNumber": "1537284903",
    "targetAccountNumber": "9876543210"
  }
  ```

---

## ❌ Global Exception Handling

In case of errors, the application returns a unified JSON error payload. The specific error codes are cataloged in [ErrorCode.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/entity/enums/ErrorCode.java).

#### Error Response Format ([ErrorResponse.java](file:///d:/SmartBank_Project/SmartBank/src/main/java/com/SmartBank/dto/response/ErrorResponse.java)):
```json
{
  "code": 1402,
  "message": "Account is inactive",
  "path": "/api/v1/transactions/withdraw",
  "timestamp": "2026-05-21T15:18:24.123"
}
```

#### Common Error Codes:
- `1002`: Unauthorized (Invalid or expired token)
- `1100`: Username already exists (The login username is taken)
- `1200`: Customer not found (The specified customer profile ID does not exist)
- `1400`: Account not found (The account number does not exist)
- `1402`: Account is inactive (The account is either frozen or closed)
- `1501`: Invalid transaction amount (Insufficient balance or out of allowed transaction limits)
- `1504`: Too many requests (Rate limit exceeded)
