SmartBank — Mini Banking System
A mini banking REST API built with Java Spring Boot, implementing full authentication and authorization using Spring Security + JWT.

Tech Stack

Java 17
Spring Boot 3.x
Spring Security 6
MySQL
JPA / Hibernate
JUnit 5 + Mockito


Project Structure
src/main/java/com/smartbank/
├── controller/
│   └── AuthController.java
├── service/
│   ├── AuthService.java
│   └── CustomUserDetailsService.java
├── security/
│   ├── JwtUtil.java
│   ├── JwtAuthenticationFilter.java
│   ├── SecurityConfig.java
│   ├── CustomAuthenticationEntryPoint.java
│   └── CustomAccessDeniedHandler.java
├── entity/
│   ├── Employee.java
│   ├── Customer.java
│   └── RefreshToken.java
├── repository/
│   ├── EmployeeRepository.java
│   ├── CustomerRepository.java
│   └── RefreshTokenRepository.java
└── dto/
    └── ...

Installation
1. Clone the repository
bashgit clone https://github.com/NguyenNhatUS/SmartBank
cd SmartBank
2. Create the database
   CREATE DATABASE smartbank;
3. Configure application.properties
This file is not included in the repository as it contains sensitive credentials. Create it at:
src/main/resources/application.properties
Example configuration:
properties# ==================== Database ====================
spring.datasource.url=jdbc:mysql://localhost:3306/smartbank
spring.datasource.username=root
spring.datasource.password=your_mysql_password

# ==================== JPA ====================
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# ==================== JWT ====================
# Secret key — must be at least 256 bits (32 characters) for HS256
jwt.secret=your-secret-key-must-be-at-least-256-bits-long-for-hs256

# Access token TTL: 15 minutes (in milliseconds)
jwt.expiration=900000

# Refresh token TTL: 7 days (in milliseconds)
jwt.refresh-expiration=604800000

Note: Make sure jwt.secret is at least 32 characters long to avoid startup errors.

4. Run the application
bash./mvnw spring-boot:run
The server runs at http://localhost:8080 by default.

API Reference
Authentication
POST /auth/register
Register a new Customer account. The role is automatically set to CUSTOMER — it cannot be specified in the request.
Request body:
json{
  "username": "john_doe",
  "password": "password123",
  "fullName": "John Doe",
  "email": "john@example.com",
  "phone": "0901234567"
}
Response:
json{
  "message": "Register successful"
}

POST /auth/login
Log in for both Employees and Customers. The system looks up the username in the employees table first, then falls back to customers.
Request body:
json{
  "username": "john_doe",
  "password": "password123"
}
Response:
json{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "role": "CUSTOMER"
}

POST /auth/refresh
Obtain a new access token using a refresh token. Refresh Token Rotation is applied — the old refresh token is immediately revoked and a new one is issued.
Request body:
json{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
Response:
json{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "7c9e6679-7425-40de-944b-e07fc1f90ae7"
}

POST /api/auth/logout
Log out — deletes the refresh token from the database. Requires a valid access token in the header.
Header:
Authorization: Bearer <access_token>
Response:
json{
  "message": "Logout successful"
}

Admin — Employee Management

Requires role ADMIN.

POST /admin/employees
Create a new Employee or Admin account. Only users with the ADMIN role can call this endpoint.
Header:
Authorization: Bearer <admin_access_token>
Request body:
json{
  "username": "emp01",
  "password": "emp_password",
  "fullName": "Nguyen Van A",
  "role": "EMPLOYEE"
}

role accepts: EMPLOYEE or ADMIN.


Account
MethodEndpointRoleDescriptionGET/api/accounts/myCUSTOMERGet own account listPOST/api/accounts/myCUSTOMEROpen a new accountGET/api/accounts/{id}EMPLOYEE, ADMINGet any account by IDGET/api/accountsEMPLOYEE, ADMINGet all accounts

Transaction
MethodEndpointRoleDescriptionPOST/api/transactionsCUSTOMER, EMPLOYEE, ADMINPerform a transactionGET/api/transactions/{id}CUSTOMER, EMPLOYEE, ADMINGet transaction by ID

Customer
MethodEndpointRoleDescriptionGET/api/customersADMINGet all customersGET/api/customers/{id}ADMINGet customer by IDPUT/api/customers/{id}ADMINUpdate customer infoDELETE/api/customers/{id}ADMINDelete a customer

Authorization Summary
Endpoint patternAllowed roles/auth/**Public/api/accounts/myCUSTOMER/admin/**ADMIN/api/transactions/**CUSTOMER, EMPLOYEE, ADMIN/api/accounts/**EMPLOYEE, ADMIN/api/customers/**ADMIN

Security Design
JWT Flow
Client --[POST /auth/login]--> Server
Server --> { accessToken (15 min), refreshToken (7 days) }

Client --[Request + Bearer accessToken]--> Server
Server validates token --> returns data

When access token expires:
Client --[POST /auth/refresh + refreshToken]--> Server
Server --> { new accessToken, new refreshToken } + revokes old refresh token
Refresh Token Rotation
Each time /auth/refresh is called, the old refresh token is marked revoked = true in the database. Any attempt to reuse a revoked token results in 401 Unauthorized. This mechanism helps detect stolen tokens.
Dual User Source
The system maintains two separate user tables — employees and customers. CustomUserDetailsService resolves users in order: employees → customers. This design reflects the domain model of a banking system where staff and customers have fundamentally different roles and data structures.

Running Tests
bash./mvnw test
Test coverage includes:

AuthServiceTest — login, register, refresh, logout
JwtUtilTest — generate, validate, parse token
