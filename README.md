# 🔐 authSpring - Spring Boot JWT Authentication & Authorization Service

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-blue.svg)](https://spring.io/projects/spring-security)
[![JWT](https://img.shields.io/badge/JWT-JJWT%200.12.3-blueviolet.svg)](https://github.com/jwtk/jjwt)
[![MySQL](https://img.shields.io/badge/Database-MySQL-blue.svg)](https://www.mysql.com/)
[![Build](https://img.shields.io/badge/Build-Maven-red.svg)](https://maven.apache.org/)

**authSpring** is a modular, secure, and production-ready authentication and authorization microservice built with **Spring Boot**, **Spring Security**, and **JSON Web Tokens (JWT)**. It provides user management capabilities, password hashing using **BCrypt**, stateless token-based authentication via **JJWT (HMAC-SHA256)**, and database persistence powered by **Spring Data JPA** and **MySQL**.

---

## 📑 Table of Contents

- [Key Features](#-key-features)
- [Architecture & Workflow](#-architecture--workflow)
  - [High-Level Architecture](#high-level-architecture)
  - [Authentication Flow (Mermaid Sequence)](#authentication-flow)
- [Project Directory Structure](#-project-directory-structure)
- [Tech Stack & Dependencies](#-tech-stack--dependencies)
- [Prerequisites](#-prerequisites)
- [Configuration Guide](#-configuration-guide)
  - [Application Properties Template](#application-properties-template)
  - [JWT Secret Key Generation](#jwt-secret-key-generation)
- [Database Setup](#-database-setup)
  - [Using Local MySQL](#1-using-local-mysql)
  - [Using Docker (Recommended for quick start)](#2-using-docker-recommended)
- [Build and Run](#-build-and-run)
  - [Run with Maven Wrapper](#run-with-maven-wrapper)
  - [Package & Run as Executable JAR](#package--run-as-executable-jar)
  - [Run Tests](#run-tests)
- [API Documentation & Endpoints](#-api-documentation--endpoints)
  - [1. User Registration](#1-user-registration)
  - [2. User Login](#2-user-login)
  - [3. Accessing Protected Endpoints](#3-accessing-protected-endpoints)
- [Security Implementation Details](#-security-implementation-details)
- [Analysis & Recommended Improvements](#-analysis--recommended-improvements)
- [Troubleshooting & FAQs](#-troubleshooting--faqs)
- [License](#-license)

---

## ✨ Key Features

- **Stateless Token-Based Authentication**: Issues signed HMAC-SHA256 JWT tokens upon valid login credentials.
- **Strong Password Hashing**: Leverages `BCryptPasswordEncoder` with salt to guarantee password security at rest.
- **Duplicate Credential Protection**: Validates unique constraints for both `username` and `email` prior to user persistence.
- **Role-Based Foundation**: Pre-configured `UserRole` enumeration (`CLIENT`, `ADMIN`) defaulting new users to the `CLIENT` role.
- **Clean Layered Architecture**: Strict separation of concerns following industry standards (Controller ➔ Service ➔ Repository ➔ Entity).
- **Stateless Spring Security Filter Chain**: CSRF protection disabled for REST endpoints, with public access to `/api/auth/**` and authenticated access required for all other routes.
- **Monitoring & Observability**: Integrated with `spring-boot-starter-actuator` for health checks and metrics.

---

## 🏗️ Architecture & Workflow

### High-Level Architecture

```
                      +---------------------------------------+
                      |             HTTP Client               |
                      |   (Browser / Postman / Mobile App)    |
                      +-------------------+-------------------+
                                          |
                                          | REST Requests (JSON)
                                          v
+---------------------------------------------------------------------------------+
|                               Spring Boot Context                               |
|                                                                                 |
|   +-------------------------------------------------------------------------+   |
|   |                       Spring Security Filter Chain                      |   |
|   |  - CSRF: Disabled                                                       |   |
|   |  - JwtAuthentificationFilter (Inspects Authorization: Bearer <token>)    |   |
|   |  - Permit: /api/auth/** | Require Authentication: Any other request     |   |
|   +-------------------------------------+-----------------------------------+   |
|                                         |                                       |
|                                         v                                       |
|   +-------------------------------------------------------------------------+   |
|   |                       AuthController (REST API)                         |   |
|   |  - POST /api/auth/register                                              |   |
|   |  - POST /api/auth/login                                                 |   |
|   +-------------------------------------+-----------------------------------+   |
|                                         |                                       |
|                                         v                                       |
|   +-------------------------------------------------------------------------+   |
|   |                       AuthService / AuthServiceImpl                     |   |
|   |  - Check existing username/email                                        |   |
|   |  - Hash password with BCryptPasswordEncoder                             |   |
|   |  - Request JWT generation from JwtService                               |   |
|   +-------------------+---------------------------------+-------------------+   |
|                       |                                 |                       |
|                       v                                 v                       |
|   +-----------------------------+     +-----------------------------+           |
|   |         JwtService          |     |       UserRepository        |           |
|   |  - Signs JWT using HS256    |     |  - Spring Data JPA CRUD     |           |
|   |  - Uses jwt.SECRET_KEY      |     |  - findByUsername / Email   |           |
|   +-----------------------------+     +--------------+--------------+           |
+------------------------------------------------------|--------------------------+
                                                       |
                                                       v
                                            +---------------------+
                                            |   MySQL Database    |
                                            |   Table: user       |
                                            +---------------------+
```

---

### Authentication Flow

#### User Registration Flow

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client / Frontend
    participant Controller as AuthController
    participant Service as AuthServiceImpl
    participant Repo as UserRepository
    participant Encoder as PasswordEncoder (BCrypt)
    participant DB as MySQL Database

    Client->>Controller: POST /api/auth/register (username, password, email)
    Controller->>Service: register(registerRequestDto)
    Service->>Repo: findByUsername(username)
    alt Username exists
        Service-->>Client: 400/500 "Username already exists"
    end
    Service->>Repo: findByEmail(email)
    alt Email exists
        Service-->>Client: 400/500 "Email already exists"
    end
    Service->>Encoder: encode(rawPassword)
    Encoder-->>Service: hashedPassword
    Service->>Repo: save(newUser with role CLIENT)
    Repo->>DB: INSERT INTO user (...)
    DB-->>Repo: Saved entity
    Service-->>Controller: RegisterResponseDto (username, email)
    Controller-->>Client: 200 OK (username, email)
```

#### User Login Flow & JWT Generation

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client / Frontend
    participant Controller as AuthController
    participant Service as AuthServiceImpl
    participant Repo as UserRepository
    participant Encoder as PasswordEncoder (BCrypt)
    participant JWT as JwtService
    participant DB as MySQL Database

    Client->>Controller: POST /api/auth/login (username, password)
    Controller->>Service: login(loginRequestDto)
    Service->>Repo: findByUsername(username)
    Repo->>DB: SELECT * FROM user WHERE username = ?
    DB-->>Repo: User Record
    alt User not found
        Service-->>Client: 400/500 "Username not found!"
    end
    Service->>Encoder: matches(rawPassword, user.getPassword())
    alt Password invalid
        Service-->>Client: 400/500 "Invalid password!"
    end
    Service->>JWT: generateToken(user)
    JWT->>JWT: Jwts.builder().setSubject(email).signWith(Key, HS256)
    JWT-->>Service: Signed JWT Token String
    Service-->>Controller: LoginResponseDto (username, token)
    Controller-->>Client: 200 OK (username, token)
```

---

## 📁 Project Directory Structure

```
authSpring/
├── .gitignore                                      # Git ignore specifications
├── pom.xml                                         # Maven dependencies and build plugins
├── mvnw                                            # Linux/macOS Maven wrapper script
├── mvnw.cmd                                        # Windows Maven wrapper batch script
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/example/authspring/
    │   │       ├── AuthSpringApplication.java      # Application entrypoint (Spring Boot main class)
    │   │       ├── Config/
    │   │       │   └── SecurityConfig.java         # Spring Security filter chain & BCrypt bean definition
    │   │       ├── controller/
    │   │       │   └── AuthController.java         # REST endpoints for register and login
    │   │       ├── dtos/
    │   │       │   ├── request/
    │   │       │   │   ├── LoginRequestDto.java    # Login payload schema (username, password)
    │   │       │   │   └── RegisterRequestDto.java # Registration payload schema (username, email, password)
    │   │       │   └── response/
    │   │       │       ├── LoginResponseDto.java   # Login response schema (username, token)
    │   │       │       └── RegisterResponseDto.java# Registration response schema (username, email)
    │   │       ├── entity/
    │   │       │   ├── User.java                   # JPA entity representing the `user` table
    │   │       │   └── enums/
    │   │       │       └── UserRole.java           # Roles enum: ADMIN, CLIENT
    │   │       ├── repository/
    │   │       │   └── UserRepository.java         # Data access layer extending JpaRepository
    │   │       ├── security/
    │   │       │   ├── JwtAuthentificationFilter.java # OncePerRequestFilter for Bearer token inspection
    │   │       │   └── JwtService.java             # JWT generation and cryptographic signing helper
    │   │       └── service/
    │   │           ├── AuthService.java            # Service contract for registration and login
    │   │           └── impl/
    │   │               └── AuthServiceImpl.java    # Business logic implementation
    │   └── resources/
    │       ├── application.properties.example      # Example configuration template
    │       └── application.properties              # Active configuration (Git-ignored)
    └── test/
        └── java/
            └── com/example/authspring/
                └── AuthSpringApplicationTests.java # Context loading unit test
```

---

## 🛠️ Tech Stack & Dependencies

| Component | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Java | 17 (LTS) | Core programming language |
| **Framework** | Spring Boot | 4.0.6 | Core framework, dependency injection, autoconfiguration |
| **Security** | Spring Security | 6.x+ | Authorization and filter chain management |
| **JWT Library** | JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) | 0.12.3 | JSON Web Token generation, serialization, and signing |
| **ORM / Data** | Spring Data JPA / Hibernate | Bundled | Object-Relational Mapping & persistence repository |
| **Database Driver** | MySQL Connector/J | 8.x+ | MySQL JDBC driver |
| **Boilerplate Reduction** | Project Lombok | Bundled | Getters, setters, constructors, annotations |
| **Monitoring** | Spring Boot Actuator | Bundled | Application health and metrics endpoints |
| **Build Tool** | Apache Maven | 3.8+ / Wrapper | Build lifecycle and dependency management |

---

## ⚙️ Prerequisites

Before running the application, make sure you have the following installed:

1. **Java Development Kit (JDK) 17** or later:
   ```bash
   java -version
   ```
2. **Maven 3.8+** (Optional, as the project includes the Maven Wrapper `./mvnw`).
3. **MySQL 8.0+** running locally, OR **Docker** to run a containerized database.

---

## 🔧 Configuration Guide

> [!IMPORTANT]
> The active configuration file `src/main/resources/application.properties` is ignored by `.gitignore` to prevent sensitive credentials and secret keys from being leaked to version control. You **must** create this file before starting the application.

A starter template is provided at [`src/main/resources/application.properties.example`](file:///home/hnabil/Desktop/authSpring/src/main/resources/application.properties.example).

### Application Properties Template

Create `src/main/resources/application.properties` and populate it with your environment settings:

```properties
# ===================================================================
# Server Configuration
# ===================================================================
server.port=8080

# ===================================================================
# Database Configuration (MySQL)
# ===================================================================
spring.datasource.url=jdbc:mysql://localhost:3306/auth_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=rootpassword
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# ===================================================================
# JPA / Hibernate Configuration
# ===================================================================
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# ===================================================================
# JWT Configuration
# MUST be at least 256 bits (32 ASCII characters or Base64 encoded)
# ===================================================================
jwt.SECRET_KEY=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970

# ===================================================================
# Actuator Monitoring (Health & Info)
# ===================================================================
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

### JWT Secret Key Generation

> [!WARNING]
> JJWT requires a minimum secret key length of **256 bits (32 bytes)** for HMAC-SHA256 (`HS256`). If your key is shorter, JJWT will throw a `WeakKeyException` during startup or token generation.

You can generate a secure random 256-bit secret key in your terminal using:

```bash
# Using OpenSSL (Generates a 32-byte hex string)
openssl rand -hex 32
```

---

## 🗄️ Database Setup

### 1. Using Local MySQL

Log into MySQL and create the database specified in your `application.properties`:

```sql
CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

With `spring.jpa.hibernate.ddl-auto=update`, Hibernate will automatically create the `user` table when the application boots.

### 2. Using Docker (Recommended)

If you prefer not to install MySQL directly on your host machine, run a container with:

```bash
docker run --name mysql-auth-container \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=auth_db \
  -p 3306:3306 \
  -d mysql:8.0
```

---

## 🚀 Build and Run

### Run with Maven Wrapper

To run the application directly from source:

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows (Command Prompt / PowerShell)
mvnw.cmd spring-boot:run
```

Once started, the application will listen on `http://localhost:8080`.

### Package & Run as Executable JAR

To compile, run tests, and package a standalone fat JAR:

```bash
# Build JAR
./mvnw clean package

# Run the packaged JAR
java -jar target/authSpring-0.0.1-SNAPSHOT.jar
```

### Run Tests

Execute unit and integration tests:

```bash
./mvnw test
```

---

## 📡 API Documentation & Endpoints

Base URL: `http://localhost:8080`

| HTTP Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Registers a new user account | ❌ No |
| `POST` | `/api/auth/login` | Authenticates user & issues JWT | ❌ No |
| `GET` | `/actuator/health` | Application health status | ❌ No |

---

### 1. User Registration

Registers a new user, hashes their password, and assigns the default role `CLIENT`.

- **Endpoint**: `POST /api/auth/register`
- **Headers**:
  ```http
  Content-Type: application/json
  ```
- **Request Body**:
  ```json
  {
    "username": "john_doe",
    "email": "john@example.com",
    "password": "SecurePassword123!"
  }
  ```

#### cURL Example:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "SecurePassword123!"
  }'
```

#### Successful Response (`200 OK`):
```json
{
  "username": "john_doe",
  "email": "john@example.com"
}
```

#### Error Scenarios:
- **Username already exists**: Throws `RuntimeException("Username already exists")`
- **Email already exists**: Throws `RuntimeException("Email already exists")`

---

### 2. User Login

Validates the user's username and password against the database. If valid, returns a signed JWT.

- **Endpoint**: `POST /api/auth/login`
- **Headers**:
  ```http
  Content-Type: application/json
  ```
- **Request Body**:
  ```json
  {
    "username": "john_doe",
    "password": "SecurePassword123!"
  }
  ```

#### cURL Example:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "SecurePassword123!"
  }'
```

#### Successful Response (`200 OK`):
```json
{
  "username": "john_doe",
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIn0.tWp6lRj_E87Y_kY..."
}
```

#### Error Scenarios:
- **Empty username**: Throws `RuntimeException("Usernamae is Empty!")`
- **User not found**: Throws `RuntimeException("Username not found!")`
- **Invalid password**: Throws `RuntimeException("Invalid password!")`

---

### 3. Accessing Protected Endpoints

For future protected routes (or any route outside `/api/auth/**`), pass the token in the `Authorization` HTTP header with the `Bearer ` prefix:

```http
Authorization: Bearer <your_jwt_token_here>
```

#### cURL Example:
```bash
curl -X GET http://localhost:8080/api/some-protected-endpoint \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## 🔒 Security Implementation Details

1. **Password Encoding**:
   The `SecurityConfig` registers a `@Bean` for `PasswordEncoder` utilizing `BCryptPasswordEncoder`. BCrypt automatically creates a cryptographic salt and hashes passwords before they touch the database.
2. **JWT Generation**:
   The `JwtService` creates compact JWT tokens signed with `SignatureAlgorithm.HS256`. The payload subject is set to the user's email address (`user.getEmail()`).
3. **HTTP Security Configuration**:
   - `csrf(csrf -> csrf.disable())`: Cross-Site Request Forgery is safely disabled for stateless REST APIs.
   - `authorizeHttpRequests`: Routes matching `/api/auth/**` are open to the public; all other endpoints require an authenticated user.
4. **Token Interception Filter**:
   `JwtAuthentificationFilter` extends Spring's `OncePerRequestFilter`, checking incoming requests for an `Authorization: Bearer <token>` header.

---

## 💡 Analysis & Recommended Improvements

The current codebase establishes a clear authentication skeleton. The following enhancements are recommended to bring the project to enterprise production standards:

```mermaid
flowchart TD
    subgraph Current Status
        A[Basic Register & Login] --> B[Raw RuntimeExceptions]
        A --> C[Incomplete JWT Filter]
        A --> D[Token lacks Expiration & Claims]
    end

    subgraph Recommended Roadmap
        B --> E[Global Exception Handler @RestControllerAdvice]
        C --> F[Complete JwtAuthenticationFilter with SecurityContextHolder]
        D --> G[Add Expiration, IssuedAt & User Role Claims]
        E --> H[Input Validation Bean Validation @Valid]
        F --> I[Refresh Tokens & Blacklisting]
    end
```

### 1. Complete `JwtAuthentificationFilter`
* **Current status**: Extracts the `Bearer` token string, but does not yet validate the token or populate Spring's `SecurityContextHolder`.
* **Recommendation**: Parse the token using `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)`, extract the subject, load the user via `UserDetailsService`, and set `SecurityContextHolder.getContext().setAuthentication(authToken)`.

### 2. Add Expiration & Claims to `JwtService`
* **Current status**: Tokens are generated without an expiration date (`.setExpiration(...)`) or issued-at date (`.setIssuedAt(...)`).
* **Recommendation**: Add standard claims:
  ```java
  public String generateToken(User user) {
      long expirationTimeMs = 1000 * 60 * 60 * 24; // 24 hours
      return Jwts.builder()
              .subject(user.getEmail())
              .claim("role", user.getRole().name())
              .issuedAt(new Date())
              .expiration(new Date(System.currentTimeMillis() + expirationTimeMs))
              .signWith(getSigningKey(), SignatureAlgorithm.HS256)
              .compact();
  }
  ```

### 3. Centralized Exception Handling (`@RestControllerAdvice`)
* **Current status**: Service methods throw generic `RuntimeException` which returns HTTP 500 Internal Server Error to clients.
* **Recommendation**: Create custom exceptions (e.g., `UserAlreadyExistsException`, `BadCredentialsException`) and map them to HTTP 400/401/409 using a `@RestControllerAdvice` class with standardized JSON error responses.

### 4. Input Validation
* **Current status**: Controller accepts raw DTOs without field validations.
* **Recommendation**: Include `spring-boot-starter-validation` in `pom.xml` and annotate DTOs with `@NotBlank`, `@Email`, and `@Size(min = 6)`:
  ```java
  public class RegisterRequestDto {
      @NotBlank(message = "Username is required")
      private String username;

      @Email(message = "Invalid email address format")
      @NotBlank(message = "Email is required")
      private String email;

      @Size(min = 8, message = "Password must be at least 8 characters")
      private String password;
  }
  ```

### 5. Add Cross-Origin Resource Sharing (CORS)
* If frontend single-page applications (React, Angular, Vue, Next.js) will consume this API, configure a `CorsConfigurationSource` bean in `SecurityConfig`.

---

## ❓ Troubleshooting & FAQs

<details>
<summary><b>1. Error: <code>java.security.InvalidKeyException: The specified key byte array is 160 bits...</code></b></summary>

**Cause**: JJWT requires that keys used with `HS256` are at least 256 bits (32 characters).
**Solution**: Make sure `jwt.SECRET_KEY` in `application.properties` has at least 32 characters. Run `openssl rand -hex 32` to generate a valid key.
</details>

<details>
<summary><b>2. Error: <code>CommunicationsLinkException: Cannot connect to MySQL server</code></b></summary>

**Cause**: The MySQL service is not running or the connection parameters in `application.properties` are incorrect.
**Solution**: Ensure MySQL is running on port 3306 and verify your username, password, and database name. If using Docker, verify with `docker ps`.
</details>

<details>
<summary><b>3. Why is <code>application.properties</code> missing when I clone the repository?</b></summary>

**Cause**: `application.properties` contains sensitive passwords and secret keys and is intentionally listed in `.gitignore`.
**Solution**: Copy `src/main/resources/application.properties.example` to `src/main/resources/application.properties` and fill in your local credentials.
</details>

---

## 📄 License

This project is licensed under the terms of the MIT License. Feel free to use, modify, and distribute it for personal or commercial projects.
