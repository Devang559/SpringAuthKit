# SpringAuthKit

SpringAuthKit is a production-oriented, reusable **Spring Boot** authentication and
authorization library. It wires together a complete auth stack behind a small set of
auto-configured conventions, so you get registration, email/SMS-style verification,
password reset, login, JWT access/refresh-token rotation, logout, OTP, roles and a
secure filter chain with almost no code in your application.

It is designed to be **dropped into a Spring Boot 3.x application**: add the dependency,
configure it under `spring.authkit.*`, and the library takes care of users, tokens, OTPs,
password hashing, schema management and security.

---

## Features

- **Identifier-based authentication** — log in with email, username or phone.
- **Password hashing & policy** — BCrypt by default with a configurable policy
  (min length, upper/lower/number/special).
- **JWT access & refresh tokens** — short-lived access tokens, long-lived refresh tokens
  with single-use **rotation** and revocation on logout.
- **Email verification** — new accounts are created `PENDING_VERIFICATION` (when enabled)
  and an OTP gates login until the address is confirmed.
- **OTP** — generic OTP issuance/verification (email) with rate limiting and expiry.
- **Password reset** — send a reset OTP, then consume it to set a new password.
- **Roles** — configurable allowed roles and a default role assigned to new users.
- **Account lockout** — optional temporary lockout after repeated failed logins.
- **Pluggable email delivery** — no email provider is wired in by default; supply your
  own `EmailService` bean to send real emails.
- **Schema modes** - `managed` (auto-provision tables/columns via Hibernate) and an
  `external` hook for applications that manage their own DDL (V2).

---

## Requirements

- Java 17+
- Spring Boot 3.5.x (parent managed by the library)
- A relational database (PostgreSQL in production; H2/SQLite also work)
- (Optional) Spring Mail or your own `EmailService` implementation to deliver mail

---

## Installation

Maven:

```xml
<dependency>
    <groupId>io.github.devang559</groupId>
    <artifactId>spring-authkit</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

> Until a release is published, install the library locally first:
> `mvn install -DskipTests -Dmaven.javadoc.skip=true -Dmaven.source.skip=true`

---

## 1-minute quick start

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

```yaml
# src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: update      # SpringAuthKit works with any ddl-auto
  authkit:
    jwt:
      secret: "${AUTHKIT_JWT_SECRET:a--very-long-random-secret-string-0123456789}"
      access-token-expiration: 15m
      refresh-token-expiration: 7d
    email:
      verification-required: true
      otp-expiration: 5m
      max-attempts: 5
    password:
      min-length: 8
      require-number: true
      require-lowercase: true
    roles:
      allowed: [USER, ADMIN]
      default: USER
```

That’s it — the controller, security filter chain, JWT filter, JPA repositories, services
and beans are all auto-configured.

---

## Endpoints

All endpoints live under `/auth` and are public by default (see `spring.authkit.security.public-endpoints`).

| Method | Path              | Request body                     | Result                                  |
|--------|-------------------|----------------------------------|-----------------------------------------|
| POST   | `/auth/register`          | `RegisterRequest`          | 201 — registers a user, sends a verification OTP |
| POST   | `/auth/login`             | `LoginRequest`             | 200 `LoginResponse` (access + refresh tokens) |
| POST   | `/auth/refresh`           | `RefreshRequest`           | 200 `LoginResponse` (rotated tokens) |
| POST   | `/auth/logout`            | `RefreshRequest`           | 200 — revokes the refresh token |
| POST   | `/auth/send-otp`          | `SendOtpRequest`           | 200 — re/sends a verification or reset OTP |
| POST   | `/auth/verify-otp`        | `VerifyOtpRequest`         | 200 — verifies an OTP (verify-otp with `EMAIL_VERIFICATION` also activates the account) |
| POST   | `/auth/resend-otp`        | `ResendOtpRequest`         | 200 — invalidates prior OTPs and sends a new one |
| POST   | `/auth/forgot-password`   | `ForgotPasswordRequest`    | 200 — sends a password-reset OTP |
| POST   | `/auth/reset-password`   | `ResetPasswordRequest`     | 200 — verifies the reset OTP and sets the new password |

A bearer access token (`Authorization: Bearer <token>`) is required for any other path;
`/auth/**` and the configured public endpoints are excluded automatically.

### Typical flow

1. `POST /auth/register` → account created in `PENDING_VERIFICATION`, a verification OTP is issued.
2. `POST /auth/verify-otp` with `{ email, otp, purpose: "EMAIL_VERIFICATION" }` → email marked verified, account becomes `ACTIVE`.
3. `POST /auth/login` → returns `accessToken` + `refreshToken`.
4. Call protected APIs with `Authorization: Bearer <accessToken>`.
5. When the access token expires, `POST /auth/refresh` with the refresh token for new tokens.
6. `POST /auth/logout` to revoke the refresh token.

---

## Configuration reference (`spring.authkit.*`)

| Property | Default | Description |
|---|---|---|
| `enabled` | `true` | Master switch. Disables all auto-configuration when `false`. |
| `authentication.identifier` | `email` | One of `email`, `username`, `phone`. |
| `schema.mode` | `managed` | `managed` (library provisions schema via Hibernate) or `external` (library validates schema on boot). |
| `user.mode` | `authkit` | `authkit` (library manages `AuthUser`) or `managed` (your own user entity; V2). |
| `user.{email,username,phone,first-name,last-name,display-name,avatar}.{enabled,required}` | – | Which user fields exist and whether they are required. At least one identifier must be enabled. |
| `password.min-length` | `8` | Minimum password length (≥ 8). |
| `password.require-{uppercase,lowercase,number,special-character}` | `false, true, true, false` | Password complexity rules. |
| `jwt.enabled` | `true` | Enable JWT tokens. |
| `jwt.secret` | – | HMAC secret (≥ 32 chars). Bind `AUTHKIT_JWT_SECRET` in production. |
| `jwt.issuer` | `spring-authkit` | JWT `iss` claim. |
| `jwt.access-token-expiration` | `15m` | Access token TTL. |
| `jwt.refresh-token-expiration` | `7d` | Refresh token TTL. |
| `email.verification-required` | `true` | Require email verification before login. |
| `email.otp-enabled` | `true` | Enable OTP issuance. |
| `email.otp-length` | `6` | OTP length (4–12). |
| `email.otp-expiration` | `5m` | OTP validity window. |
| `email.max-attempts` | `5` | Max OTP attempts before failure. |
| `roles.enabled` | `true` | Enable role assignment/checks. |
| `roles.allowed` | `[USER, ADMIN, MODERATOR]` | Allowed role names. |
| `roles.default` | `USER` | Role assigned on registration. |
| `permissions.enabled` | `true` | Enable permission model. |
| `rate-limit.enabled` | `true` | Enable login/OTP rate limiting. |
| `rate-limit.login{max-attempts,window}` | `5 / 1m` | Login rate limit. |
| `rate-limit.otp{max-attempts,window}` | `5 / 5m` | OTP rate limit. |
| `security.account-lockout-enabled` | `true` | Lock accounts after repeated failures. |
| `security.max-failed-attempts` | `5` | Failures before lockout. |
| `security.lockout-duration` | `15m` | Lockout window. |
| `security.public-endpoints` | `/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/webjars/**` | Paths that bypass authentication. |

---

## Security model

- Spring Security is configured fully by `AuthKitSecurityAutoConfiguration`. It provides a
  single `SecurityFilterChain` (stateless, no form login) with a
  `AuthKitJwtAuthenticationFilter` that reads `Authorization: Bearer <access-token>`,
  resolves the user and populates `SecurityContextHolder`.
- Provide your own `SecurityFilterChain` bean to take full control; SpringAuthKit backs off.
- Tokens: an **access token** (short-lived, carries `roles`/`permissions`) and a **refresh
  token** (stored hashed server-side, rotated on every use, revocable). Reusing a revoked
  refresh token is rejected.
- `PasswordEncoder` defaults to BCrypt. Override the `PasswordEncoder` bean to change it.

Protected handlers in a controller:

```java
@RestController
@RequestMapping("/api")
class NoteController {

    @GetMapping("/notes")
    Map<String, Object> notes(@AuthenticationPrincipal AuthKitUser principal) {
        return Map.of(
            "user", principal.getId(),
            "roles", principal.getRoleNames(),
            "allowed", principal.getAuthorities());
    }
}
```

---

## Extending & overriding

SpringAuthKit registers its beans with `@ConditionalOnMissingBean`, so override any of them:

| Bean | Override to… |
|---|---|
| `PasswordEncoder` | Use Argon2/argon2id or a custom scheme. |
| `EmailService` | Hook up Spring Mail, AWS SES, SendGrid. |
| `RateLimiter` | Back rate limiting with Redis, Caffeine, etc. |
| `OtpGenerator` | Change OTP format/length logic. |
| `SecurityFilterChain` | Bring your own filter chain / additional protected paths. |
| `AuthKitUserRepository` / repositories | Provide a custom repository implementation. |

Example — real email:

```java
@Configuration
class MailConfig {
    @Bean
    EmailService authKitEmailService(JavaMailSender mailSender,
            AuthKitConfig config) {
        return (destination, otp) -> {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(destination);
            msg.setSubject("Your SpringAuthKit code");
            msg.setText("Verification code: " + otp);
            mailSender.send(msg);
        };
    }
}
```

---

## Schema management

- `managed` (default): the library expects the persistence schema to be managed by Hibernate
  (`spring.jpa.hibernate.ddl-auto`) or your own migrations. Entities live in
  `io.github.devang559.authkit.{user,role,token,otp,permission,lifecycle}`.
  Tables: `auth_users`, `auth_roles`, `auth_user_roles`, `auth_role_permissions`,
  `auth_permissions`, `auth_refresh_tokens`, `auth_otps`.
- `external`: applications that manage their own DDL can opt in; the library will validate
  expected schema state on startup and fail fast with `MigrationRequiredException` when
  required columns/tables are absent.

---

## Architecture

```
web/dto            →  web/controller      →  authentication services
                      (AuthKitController)       (Registration, Authentication,
                                                 EmailVerification, PasswordReset)
                                                    │
                                                    ▼
                          security (JWT filter + SecurityFilterChain)
                          ┌───────────────────────────────────────────┐
                          │ AuthKitConfig (validated)  AuthKitProperties │
                          └───────────────┬───────────────┬───────────────┘
                                          │               │
                               jwt (JwtService)   otp (OtpService)  password (PasswordService)
                                                  rate-limit (RateLimiter)  email (EmailService, pluggable)
                                                  role (RoleService)  permission (PermissionService)
                                                  token (TokenService, RefreshToken)
                                                  user (AuthUserService, AuthUser entity)
```

- `AuthKitConfig` is an immutable, validated view of `AuthKitProperties` (`spring.authkit.*`).
- All authentication state (password hashes, refresh-token hashes, OTP hashes) is persisted;
  OTP codes and refresh tokens are hashed before storage.
- `@Transactional` is applied on all mutating service methods.

---

## Testing the library

```bash
./mvnw test
```

The test suite uses an in-memory H2 database and a capturing `EmailService`, and exercises
the full registration → verification → login → protected-resource → refresh → logout flow
plus config validation and error handling. See `src/test/java/io/github/devang559/authkit`.

---

## Project structure

```
pom.xml
README.md
examples/
└── README.md
└── spring-authkit-demo/
    ├── pom.xml
    └── src/main/java/io/github/devang559/authkit/example/
    │   ├── AuthKitDemoApplication.java
    │   └── NoteController.java          # protected /api/notes endpoint
    └── src/main/resources/
        └── application.yml
src/main/java/io/github/devang559/authkit/
├── audit/                   (AuditEvent, AuditEventType, AuditLog, AuditLogRepository)
├── authentication/          (PrincipalIdentifier, AuthenticationIdentifierResolver,
│                             DefaultAuthenticationIdentifierResolver, RegistrationService,
│                             AuthenticationService, EmailVerificationService, PasswordResetService)
├── autoconfigure/           (AuthKitAutoConfiguration, AuthKitSecurityAutoConfiguration)
├── config/                  (AuthKitConfig, AuthKitProperties, IdentifierType, SchemaMode, UserMode)
├── email/                   (EmailService, DefaultEmailService)
├── exception/               (AuthKitException + 15 typed subclasses)
├── jwt/                     (JwtService, JwtProperties)
├── lifecycle/               (AccountStatus)
├── otp/                     (Otp, OtpPolicy, OtpPurpose, OtpGenerator,
│                             SecureRandomOtpGenerator, OtpRepository, OtpService)
├── password/                (PasswordService, PasswordPolicyValidator)
├── permission/              (AuthPermission, AuthPermissionRepository, PermissionService)
├── ratelimit/               (RateLimiter, RateLimitPolicy, InMemoryRateLimiter)
├── role/                    (AuthRole, AuthRoleRepository, RoleService)
├── schema/                  (AuthKitSchemaManager, SchemaState, SchemaDiff, DbProduct)
├── security/                (AuthKitJwtAuthenticationFilter)
├── token/                   (RefreshToken, RefreshTokenRepository, TokenPair,
│                             TokenService, TokenType)
├── user/                    (AuthUser, AuthUserRepository, AuthUserService,
│                            AuthKitUser, AuthKitUserDetailsService, UserField)
├── util/                    (HashUtils)
└── web/
    ├── AuthKitErrorHandler.java        (@RestControllerAdvice)
    ├── controller/AuthKitController.java
    └── dto/                            (RegisterRequest, LoginRequest, LoginResponse,
                                         RefreshRequest, MessageResponse, ErrorResponse,
                                         SendOtpRequest, VerifyOtpRequest, ResendOtpRequest,
                                         ForgotPasswordRequest, ResetPasswordRequest)
src/main/resources/
└── META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
src/test/java/com/example/authkitdemo/
├── AuthKitTestApplication.java      (@SpringBootTest bootstrap)
├── SmokeTest.java
└── api/TestController.java          (secured /api/test endpoint)
src/test/java/io/github/devang559/authkit/
├── AuthKitConfigTest.java
├── AuthKitControllerTest.java       (full HTTP flow via MockMvc)
└── support/
    ├── CapturingEmailService.java
    └── TestEmailConfig.java
src/test/resources/application.yml
```

---

## Example app

A complete, runnable example is provided under `examples/spring-authkit-demo`. See
[`examples/README.md`](examples/README.md) for run instructions.
