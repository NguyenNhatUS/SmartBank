# SmartBank — Mini Banking System

A mini banking REST API built with **Java Spring Boot**, implementing full authentication and authorization using **Spring Security + JWT**.

---

## Tech Stack

- **Backend**: Java 24, Spring Boot 3.x, Spring Security 6, Spring AOP
- **Database**: MySQL, JPA (Hibernate)
- **Caching & Rate Limiting**: Spring Cache, Redis
- **Scheduling**: Spring Scheduling
- **Testing**: JUnit 5, Mockito
- **DevOps**: Docker

---

## Key Features

### 1. Security & Performance
- **JWT Authentication**: Secure login/logout with Access Tokens and Refresh Token Rotation.
- **Redis Caching**: Optimized performance for account and customer data access.
- **AOP Rate Limiting**: Custom `@RateLimit` annotation powered by Redis to prevent brute-force and API abuse (e.g., login 5 req/min, transactions 10 req/min).

### 2. Transaction Management
- **Basic Operations**: Deposit, Withdraw, and Transfer between accounts.
- **Advanced Filtering**: Search transaction history by date range, amount, type, and account number.
- **Bill Payments**: Simulate utility bill payments to various billers.
- **Recurring Transfers**: Schedule automatic transfers (Daily, Weekly, Monthly) with automated background processing.

### 3. Accounts & Savings
- **Multiple Account Types**: Support for Checking and Savings accounts.
- **Interest Engine**: Automatic monthly interest calculation and credit for savings accounts.
- **Term Deposits**: Fixed-term savings with maturity date enforcement to prevent premature withdrawals.

---

## Project Structure

```
src/main/java/com/smartbank/
│
├── config/
│   ├── RedisConfig.java
│   └── SecurityConfig.java
│
├── controller/
│   ├── AccountController.java
│   ├── AuthController.java
│   ├── CustomerController.java
│   ├── TransactionController.java
│   └── RecurringTransferController.java
│
├── service/
│   ├── AccountService.java
│   ├── AuthService.java
│   ├── CustomUserDetailsService.java
│   ├── CustomerService.java
│   ├── TransactionService.java
│   ├── ScheduledTaskService.java
│   └── InterestService.java
│
├── repository/
│   ├── AccountRepository.java
│   ├── CustomerRepository.java
│   ├── EmployeeRepository.java
│   ├── RefreshTokenRepository.java
│   ├── TransactionRepository.java
│   └── RecurringTransferRepository.java
│
├── entity/
│   ├── enums/
│   │   ├── AccountStatus.java
│   │   ├── AccountType.java
│   │   ├── CustomerStatus.java
│   │   ├── Role.java
│   │   ├── ErrorCode.java
│   │   ├── TransactionType.java
│   │   └── Frequency.java
│   │
│   ├── Account.java
│   ├── Customer.java
│   ├── Employee.java
│   ├── RefreshToken.java
│   ├── Transaction.java
│   └── RecurringTransfer.java
│
├── dto/
│   ├── request/
│   │   ├── AccountCreateRequest.java
│   │   ├── BillPaymentRequest.java
│   │   ├── TransactionFilterRequest.java
│   │   ├── RecurringTransferRequest.java
│   │   └── ... (existing auth requests)
│
├── security/
│   ├── JwtAuthenticationFilter.java
│   ├── JwtTokenProvider.java
│   ├── RateLimit.java
│   └── RateLimitAspect.java
│
└── SmartBankApplication.java
```

---

## API Reference

### Authentication (Rate Limited)

| Method | Endpoint | Description | Limit |
|--------|----------|-------------|-------|
| POST | `/auth/register` | Register a new customer | 3 req/min |
| POST | `/auth/login` | Login and get tokens | 5 req/min |
| POST | `/auth/refresh` | Refresh access token | - |

### Transactions

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/transactions/deposit` | Deposit funds |
| POST | `/api/v1/transactions/withdraw` | Withdraw funds |
| POST | `/api/v1/transactions/transfer` | Transfer between accounts |
| POST | `/api/v1/transactions/pay-bill` | Pay a utility bill |
| GET | `/api/v1/transactions/filter` | Search transaction history |

### Accounts & Savings

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/accounts/my` | Get own accounts |
| POST | `/api/v1/accounts/my` | Open new account (Checking/Savings) |
| POST | `/api/v1/recurring-transfers` | Schedule a recurring transfer |

---

## Configuration

### Environment Variables (.env)
```env
# Database
DB_URL=jdbc:mysql://localhost:3306/SmartBank
DB_USERNAME=root
DB_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=your_super_secret_key_at_least_32_chars
JWT_EXPIRATION=900000
```

---

## Running the Application

### 1. Prerequisites
- JDK 24
- MySQL 8.x
- Redis 7.x

### 2. Build & Run
```bash
mvn clean install
mvn spring-boot:run
```

### 3. Background Tasks
- **Interest Calculation**: Automatically runs at 2 AM on the 1st of every month.
- **Recurring Transfers**: Automatically runs at 1 AM every day.

---

## License
MIT License
