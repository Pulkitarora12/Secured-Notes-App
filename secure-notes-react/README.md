# AuthTemplate

AuthTemplate is a plug-and-play Spring Boot + React authentication boilerplate with JWT, OAuth2 (GitHub & Google), TOTP-based MFA, RBAC, email-based password reset, and audit logging — everything you need to bootstrap secure auth without starting from scratch.

---

## What's inside

**Auth**

- JWT-based stateless authentication
- OAuth2 login via GitHub and Google
- Password reset via email (token-based, 24hr expiry)
- BCrypt password encoding
- Two-factor authentication (TOTP) via Google Authenticator

**User management**

- Two roles: `ROLE_USER` and `ROLE_ADMIN`
- Admins can lock accounts, expire credentials, enable/disable users, and force-update passwords
- Account & credentials expiry dates tracked per user

**Notes**

- Each user owns their own notes (isolated by `ownerUsername`)
- Full CRUD — create, read, update, delete
- Every operation is audit-logged with timestamp and username

**Security config**

- CSRF protection via `CookieCsrfTokenRepository` (disabled only for public auth routes)
- CORS configured via `WebConfig` — frontend URL injected from env
- Stateless sessions (no server-side session storage)
- Custom `AuthEntryPointJwt` for clean 401 JSON responses

---

## Tech stack

| Layer    | Tech                                              |
| -------- | ------------------------------------------------- |
| Backend  | Spring Boot 4.x, Spring Security, Spring Data JPA |
| Auth     | JWT (jjwt 0.13), OAuth2 Client                    |
| Database | MySQL                                             |
| Email    | Spring Mail (Gmail SMTP)                          |
| Build    | Maven                                             |
| Java     | 21                                                |

---

## Getting started

### Prerequisites

- Java 21
- MySQL running locally
- A Gmail account with App Password (for email)
- GitHub and/or Google OAuth app credentials

### Environment variables

Create a `.env` file in `notes/notes/` (it's gitignored):

```env
DB_NAME=your_db_name
DB_USERNAME=your_mysql_user
DB_PASSWORD=your_mysql_password

JWT_SECRET=your_base64_encoded_256bit_secret
JWT_EXPIRATION=86400000

EMAIL=your_gmail@gmail.com
EMAIL_PASSWORD=your_gmail_app_password

GITHUB_CLIENT=your_github_client_id
GITHUB_SECRET=your_github_client_secret

GOOGLE_CLIENT=your_google_client_id
GOOGLE_SECRET=your_google_client_secret
```

> To generate a JWT secret, run `GenerateSecret.java` — it prints a secure base64-encoded 256-bit key.

### Run

```bash
cd notes/notes
mvn spring-boot:run
```

Server starts on `http://localhost:8080`.

Frontend URL defaults to `http://localhost:3000` — change it in `application.yaml` or via env if needed.

---

## API overview

### Public (no auth required)

| Method | Endpoint                           | Description        |
| ------ | ---------------------------------- | ------------------ |
| POST   | `/api/auth/public/signin`          | Login, returns JWT |
| POST   | `/api/auth/public/signup`          | Register new user  |
| POST   | `/api/auth/public/forgot-password` | Send reset email   |
| POST   | `/api/auth/public/reset-password`  | Reset with token   |
| GET    | `/api/csrf-token`                  | Get CSRF token     |

### Authenticated

| Method | Endpoint             | Description           |
| ------ | -------------------- | --------------------- |
| GET    | `/api/auth/user`     | Get current user info |
| GET    | `/api/auth/username` | Get current username  |
| GET    | `/api/notes`         | Get your notes        |
| POST   | `/api/notes`         | Create a note         |
| PUT    | `/api/notes/{id}`    | Update a note         |
| DELETE | `/api/notes/{id}`    | Delete a note         |

### Admin only

| Method | Endpoint                                      | Description            |
| ------ | --------------------------------------------- | ---------------------- |
| GET    | `/api/admin/getusers`                         | List all users         |
| GET    | `/api/admin/user/{id}`                        | Get user by ID         |
| PUT    | `/api/admin/update-role`                      | Change user role       |
| PUT    | `/api/admin/update-lock-status`               | Lock/unlock account    |
| PUT    | `/api/admin/update-expiry-status`             | Expire account         |
| PUT    | `/api/admin/update-enabled-status`            | Enable/disable account |
| PUT    | `/api/admin/update-credentials-expiry-status` | Expire credentials     |
| PUT    | `/api/admin/update-password`                  | Force update password  |
| GET    | `/api/audit`                                  | All audit logs         |
| GET    | `/api/audit/note/{id}`                        | Audit logs for a note  |

---

## Two-factor authentication

2FA is implemented using TOTP (Time-based One-Time Passwords), compatible with Google Authenticator and any other TOTP app (Authy, 1Password, etc.).

**Setup flow**

1. User enables 2FA — backend generates a TOTP secret and returns a QR code URI
2. User scans the QR code in Google Authenticator
3. User confirms setup by submitting a valid 6-digit code
4. `isTwoFactorEnabled` is set to `true` on the user — subsequent logins require a TOTP code

**Login flow with 2FA enabled**

1. Submit username + password as usual
2. If 2FA is enabled, the signin response signals that a TOTP code is required
3. Submit the 6-digit code from Google Authenticator to complete authentication and receive the JWT

**Relevant endpoints**
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/2fa/enable` | Generate secret + QR code URI |
| POST | `/api/auth/2fa/verify-setup` | Confirm setup with first TOTP code |
| POST | `/api/auth/2fa/disable` | Turn off 2FA |
| POST | `/api/auth/2fa/verify` | Verify TOTP code during login |

The `twoFactorSecret` is stored on the `User` entity and never exposed in API responses. The `isTwoFactorEnabled` flag is visible in `/api/auth/user` so the frontend can conditionally show the 2FA prompt.

---

## OAuth2 flow

After a successful OAuth2 login (GitHub or Google), the user is redirected to:

```
{frontend.url}/oauth2/redirect?token=<jwt>
```

The frontend should extract the token from the query param and store it for subsequent requests.

---

## Seed data

On startup, the app creates two default users if they don't exist:

| Username | Password    | Role  | Notes                     |
| -------- | ----------- | ----- | ------------------------- |
| `user1`  | `password1` | USER  | Account locked by default |
| `admin`  | `adminPass` | ADMIN | Fully enabled             |

---

## Project structure

```
notes/
├── controller/          # REST controllers
├── entity/              # JPA entities (User, Note, Role, AuditLog, PasswordResetToken)
├── repository/          # Spring Data repositories
├── service/             # Service interfaces + implementations
├── security/
│   ├── jwt/             # JWT filter, utils, entry point
│   ├── services/        # UserDetailsImpl, UserDetailsServiceImpl
│   ├── config/          # OAuth2 success handler
│   ├── request/         # Login/signup request DTOs
│   └── response/        # Response DTOs
└── dtos/                # UserDTO
```

---

## Using this as an auth template

The notes feature is just the domain layer — the entire auth and security setup is independent of it. To use this as a starting point for your own project, here's what to keep and what to remove.

### What to delete

**Domain-specific files — delete these entirely:**

```
controller/NotesController.java
entity/Note.java
repository/NoteRepository.java
service/NoteService.java
service/impl/NoteServiceImpl.java
```

**Audit log (optional) — delete if you don't need operation logging:**

```
controller/AuditLogController.java
entity/AuditLog.java
repository/AuditLogRepository.java
service/AuditLogService.java
service/impl/AuditLogServiceImpl.java
```

### What to keep (the auth core)

```
security/                        # entire package — don't touch
controller/AuthController.java   # signin, signup, forgot/reset password
controller/AdminController.java  # user management endpoints
controller/CsrfController.java   # CSRF token endpoint
entity/User.java
entity/Role.java
entity/AppRole.java
entity/PasswordResetToken.java
repository/UserRepository.java
repository/RoleRepository.java
repository/PasswordResetTokenRepository.java
service/UserService.java + impl
service/EmailService.java
dtos/UserDTO.java
```

### What to update

**`SecurityConfig.java`** — remove the notes-related permit rules if any, and update the `CommandLineRunner` seed data to match your use case.

**`application.yaml`** — update `spring.application.name` and the datasource DB name.

**`pom.xml`** — update `groupId`, `artifactId`, and `name` to match your project.

**Package rename** — do a global find & replace on `com.secure.notes` → `com.your.package`.

### What to add

Once the notes layer is removed, add your own domain entities, repositories, services, and controllers. The security filter chain, JWT handling, and OAuth2 flow will protect your new endpoints automatically — just follow the same patterns used in `NotesController` for accessing the authenticated user via `@AuthenticationPrincipal`.

---
