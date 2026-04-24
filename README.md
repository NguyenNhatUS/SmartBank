# SmartBank — Mini Banking System

A mini banking REST API built with **Java Spring Boot**, implementing full authentication and authorization using **Spring Security + JWT**.

---

## Tech Stack

- **Backend**: Java 17, Spring Boot 3.x, Spring Security 6
- **Database**: MySQL, JPA (Hibernate)
- **Caching**: Spring Cache, Redis
- **Testing**: JUnit 5, Mockito
- **DevOps**: Docker


---

## Project Structure

```
src/main/java/com/smartbank/
│
├── config/
│   ├── CacheConfig.java
│   └── SecurityConfig.java
│
├── controller/
│   ├── AccountController.java
│   ├── AuthController.java
│   ├── CustomerController.java
│   └── TransactionController.java
│
├── service/
│   ├── AccountService.java
│   ├── AuthService.java
│   ├── CustomUserDetailsService.java
│   ├── CustomerService.java
│   └── TransactionService.java
│
├── repository/
│   ├── AccountRepository.java
│   ├── CustomerRepository.java
│   ├── EmployeeRepository.java
│   ├── RefreshTokenRepository.java
│   └── TransactionRepository.java
│
├── entity/
│   ├── enums/
│   │   ├── AccountStatus.java
│   │   ├── AccountType.java
│   │   ├── CustomerStatus.java
│   │   ├── Role.java
│   │   ├── ErrorCode.java
│   │   └── TransactionType.java
│   │
│   ├── Account.java
│   ├── Customer.java
│   ├── Employee.java
│   ├── RefreshToken.java
│   └── Transaction.java
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
├── mapper/
│   ├── AccountMapper.java
│   ├── CustomerMapper.java
│   └── TransactionMapper.java
│
├── security/
│   ├── JwtAuthenticationFilter.java
│   └── JwtTokenProvider.java
│
├── exception/
│   ├── CustomAccessDeniedHandler.java
│   ├── CustomAuthenticationEntryPoint.java
│   ├── GlobalExceptionHandler.java
│   └── AppException.java
│
└── SmartBankApplication.java
```

---

## Installation

### 1. Clone the repository

```bash
git clone https://github.com/your-username/smartbank
cd smartbank
```

### 2. Create the database

```sql
CREATE DATABASE smartbank;
```


### 3 Configuration

#### 3.1. Create `application.properties`

Create the file at `src/main/resources/application.properties`. All sensitive values are read from environment variables — do not hardcode them directly.

```properties
# ── Datasource ───────────────────────────────────────────────────────────────
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# ── JPA / Hibernate ──────────────────────────────────────────────────────────
spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO}
spring.jpa.show-sql=${JPA_SHOW_SQL}
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.open-in-view=false

# ── Redis ────────────────────────────────────────────────────────────────────
spring.data.redis.host=${REDIS_HOST}
spring.data.redis.port=${REDIS_PORT}
spring.data.redis.password=${REDIS_PASSWORD}
spring.data.redis.timeout=${REDIS_TIMEOUT}

# ── Cache ────────────────────────────────────────────────────────────────────
spring.cache.type=redis
spring.cache.redis.time-to-live=${CACHE_TTL}
spring.cache.redis.cache-null-values=false

# ── JWT ──────────────────────────────────────────────────────────────────────
jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION}
jwt.refresh-expiration=${JWT_REFRESH_EXPIRATION}
```

---

#### 3.2. Create `.env`

Create a `.env` file at the project root. Copy the template below and fill in your own values.

```env
# ── Database ──────────────────────────────────────────────────────────────────
DB_URL=jdbc:mysql://mysql:3306/SmartBank?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

# ── JPA ───────────────────────────────────────────────────────────────────────
JPA_DDL_AUTO=update
JPA_SHOW_SQL=false

# ── Redis ─────────────────────────────────────────────────────────────────────
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password
REDIS_TIMEOUT=2000ms

# ── Cache ─────────────────────────────────────────────────────────────────────
CACHE_TTL=10m

# ── JWT ───────────────────────────────────────────────────────────────────────
# Must be at least 32 characters
JWT_SECRET=your_jwt_secret_key
JWT_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000
```
#### 3.3. Run with Docker

Once `application.properties` and `.env` are in place, start the full stack with:

```bash
# First run or after code changes
docker compose up --build

# Subsequent runs (no code changes)
docker compose up
```

The server will be available at `http://localhost:8080`.

## API Reference

### Authentication

#### POST `/auth/register`

Register a new **Customer** account. The role is automatically set to `CUSTOMER` — it cannot be specified in the request.

**Request body:**
```json
{
  "username": "nhat_nguyen",
  "password": "password123",
  "fullName": "Nguyen Le Duc Nhat",
  "email": "nldnhat@example.com",
  "phone": "0344722405"
}
```

**Response:**
```json
{
  "message": "Register successful"
}
```

---

#### POST `/auth/login`

Log in for both Employees and Customers. The system looks up the username in the `employees` table first, then falls back to `customers`.

**Request body:**
```json
{
  "username": "nhat_nguyen",
  "password": "password123"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "role": "CUSTOMER"
}
```

---

#### POST `/auth/refresh`

Obtain a new access token using a refresh token. **Refresh Token Rotation** is applied — the old refresh token is immediately revoked and a new one is issued.

**Request body:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "7c9e6679-7425-40de-944b-e07fc1f90ae7"
}
```

---

#### POST `/api/auth/logout`

Log out — deletes the refresh token from the database. Requires a valid access token in the header.

**Header:**
```
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "message": "Logout successful"
}
```

---

### Admin — Employee Management

> Requires role `ADMIN`.

#### POST `/admin/employees`

Create a new Employee or Admin account. Only users with the `ADMIN` role can call this endpoint.

**Header:**
```
Authorization: Bearer <admin_access_token>
```

**Request body:**
```json
{
  "username": "emp01",
  "password": "emp_password",
  "fullName": "Nguyen Van A",
  "role": "EMPLOYEE"
}
```

> `role` accepts: `EMPLOYEE` or `ADMIN`.

---

### Account

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/api/accounts/my` | CUSTOMER | Get own account list |
| POST | `/api/accounts/my` | CUSTOMER | Open a new account |
| GET | `/api/accounts/{id}` | EMPLOYEE, ADMIN | Get any account by ID |
| GET | `/api/accounts` | EMPLOYEE, ADMIN | Get all accounts |

---

### Transaction

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/transactions` | CUSTOMER, EMPLOYEE, ADMIN | Perform a transaction |
| GET | `/api/transactions/{id}` | CUSTOMER, EMPLOYEE, ADMIN | Get transaction by ID |

---

### Customer

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/api/customers` | ADMIN | Get all customers |
| GET | `/api/customers/{id}` | ADMIN | Get customer by ID |
| PUT | `/api/customers/{id}` | ADMIN | Update customer info |
| DELETE | `/api/customers/{id}` | ADMIN | Delete a customer |

---

## Authorization Summary

| Endpoint pattern | Allowed roles |
|-----------------|---------------|
| `/auth/**` | Public |
| `/api/accounts/my` | CUSTOMER |
| `/admin/**` | ADMIN |
| `/api/transactions/**` | CUSTOMER, EMPLOYEE, ADMIN |
| `/api/accounts/**` | EMPLOYEE, ADMIN |
| `/api/customers/**` | ADMIN |

---

## Security Design

### JWT Flow

```
Client --[POST /auth/login]--> Server
Server --> { accessToken (15 min), refreshToken (7 days) }

Client --[Request + Bearer accessToken]--> Server
Server validates token --> returns data

When access token expires:
Client --[POST /auth/refresh + refreshToken]--> Server
Server --> { new accessToken, new refreshToken } + revokes old refresh token
```

### Refresh Token Rotation

Each time `/auth/refresh` is called, the old refresh token is marked `revoked = true` in the database. Any attempt to reuse a revoked token results in `401 Unauthorized`. This mechanism helps detect stolen tokens.

### Dual User Source

The system maintains two separate user tables — `employees` and `customers`. `CustomUserDetailsService` resolves users in order: employees → customers. This design reflects the domain model of a banking system where staff and customers have fundamentally different roles and data structures.

---

## Running Tests

```bash
./mvnw test
```

Test coverage includes:
- `AuthServiceTest` — login, register, refresh, logout
- `JwtTokenProviderTest` — generate, validate, parse token
