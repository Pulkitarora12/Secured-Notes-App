# AuthTemplate

A plug-and-play Spring Boot + React authentication boilerplate. Drop it in, swap out the domain layer, and you have a production-ready auth system — JWT, OAuth2 (GitHub & Google), TOTP-based MFA, RBAC, email-based password reset, and audit logging — without building any of it from scratch.

---

## What's included

**Authentication**

- JWT-based stateless authentication
- OAuth2 login via GitHub and Google
- Password reset via email (token-based, 24hr expiry)
- BCrypt password encoding

**Two-factor authentication (TOTP)**

- Compatible with Google Authenticator, Authy, 1Password, and any TOTP app
- Per-user enable/disable with QR code setup flow
- TOTP secret stored on the user entity, never exposed in API responses

**User management**

- Two roles out of the box: `ROLE_USER` and `ROLE_ADMIN`
- Admins can lock accounts, expire credentials, enable/disable users, and force-update passwords
- Account and credentials expiry dates tracked per user

**Security config**

- CORS configured via `WebConfig` — frontend URL injected from environment
- Stateless sessions (no server-side session storage)
- Custom `AuthEntryPointJwt` for clean 401 JSON responses
- CSRF support available via `CookieCsrfTokenRepository` (currently disabled for API usage — see Configuration)

---

## AuthTemplate Gallary
<img width="1919" height="930" alt="Screenshot 2026-03-24 182327" src="https://github.com/user-attachments/assets/03ede6f1-c190-4ab5-8580-f6bf222d8b30" />
<img width="1893" height="926" alt="Screenshot 2026-03-24 182353" src="https://github.com/user-attachments/assets/7f9879ed-1eec-4585-b8d0-3f00e685cd95" />
<img width="1902" height="893" alt="Screenshot 2026-03-24 182437" src="https://github.com/user-attachments/assets/db1fe75b-3f0d-4902-a659-4a274801eba8" />
<img width="1901" height="903" alt="Screenshot 2026-03-24 182448" src="https://github.com/user-attachments/assets/33a6043e-3949-4e9c-b6a9-5617fc67f694" />
    
---

## Tech stack

| Layer    | Tech                                              |
| -------- | ------------------------------------------------- |
| Backend  | Spring Boot 4.x, Spring Security, Spring Data JPA |
| Auth     | JWT (jjwt 0.13), OAuth2 Client                    |
| 2FA      | googleauth 1.4.0 (TOTP)                           |
| Database | MySQL                                             |
| Email    | Spring Mail (Gmail SMTP)                          |
| Build    | Maven                                             |
| Java     | 21                                                |

---

## Getting started

### Prerequisites

- Java 21
- MySQL running locally
- A Gmail account with App Password enabled (for password reset emails)
- GitHub and/or Google OAuth app credentials

### 1. Clone the repo

```bash
git clone https://github.com/your-username/auth-template.git
cd auth-template/notes/notes
```

### 2. Create a `.env` file

Create `.env` inside `notes/notes/` (it's gitignored):

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

> To generate a secure JWT secret, run `GenerateSecret.java` — it prints a base64-encoded 256-bit key.

### 3. Run

```bash
mvn spring-boot:run
```

Server starts on `http://localhost:8080`. Frontend URL defaults to `http://localhost:3000` — change it in `application.yaml` or via env if needed.

---

## Seed data

On startup, the app creates two default users if they don't already exist:

| Username | Password    | Role  | Status                    |
| -------- | ----------- | ----- | ------------------------- |
| `user1`  | `password1` | USER  | Account locked by default |
| `admin`  | `adminPass` | ADMIN | Fully enabled             |

> When adapting this template for your own project, update or remove the seed data in `SecurityConfig.java` to match your use case.

---

## API reference

### Public endpoints (no auth required)

| Method | Endpoint                            | Description                                         |
| ------ | ----------------------------------- | --------------------------------------------------- |
| POST   | `/api/auth/public/signin`           | Login — returns JWT                                 |
| POST   | `/api/auth/public/signup`           | Register a new user                                 |
| POST   | `/api/auth/public/forgot-password`  | Send a password reset email                         |
| POST   | `/api/auth/public/reset-password`   | Reset password using a token                        |
| POST   | `/api/auth/public/verify-2fa-login` | Verify TOTP code during login (after receiving JWT) |

### Authenticated endpoints

| Method | Endpoint                    | Description                                      |
| ------ | --------------------------- | ------------------------------------------------ |
| GET    | `/api/auth/user`            | Get current user info                            |
| GET    | `/api/auth/username`        | Get current username                             |
| GET    | `/api/auth/user/2fa-status` | Check whether 2FA is enabled for current user    |
| POST   | `/api/auth/enable-2fa`      | Generate TOTP secret and return QR code URI      |
| POST   | `/api/auth/verify-2fa`      | Confirm TOTP setup with first code — enables 2FA |
| POST   | `/api/auth/disable-2fa`     | Disable 2FA for current user                     |

### Admin-only endpoints

| Method | Endpoint                                      | Description                    |
| ------ | --------------------------------------------- | ------------------------------ |
| GET    | `/api/admin/getusers`                         | List all users                 |
| GET    | `/api/admin/user/{id}`                        | Get a user by ID               |
| PUT    | `/api/admin/update-role`                      | Change a user's role           |
| PUT    | `/api/admin/update-lock-status`               | Lock or unlock an account      |
| PUT    | `/api/admin/update-expiry-status`             | Expire or restore an account   |
| PUT    | `/api/admin/update-enabled-status`            | Enable or disable an account   |
| PUT    | `/api/admin/update-credentials-expiry-status` | Expire or restore credentials  |
| PUT    | `/api/admin/update-password`                  | Force-update a user's password |

---

## Authentication flows

### Standard login (no 2FA)

1. `POST /api/auth/public/signin` with `{ username, password }`
2. Receive `{ username, roles, jwtToken }` in response
3. Include the token in all subsequent requests as `Authorization: Bearer <token>`

### Login with 2FA enabled

1. `POST /api/auth/public/signin` with `{ username, password }`
2. Receive JWT in response — but 2FA verification is still required
3. `POST /api/auth/public/verify-2fa-login?code=<totp>&jwtToken=<token>`
4. On success, the JWT is now valid for authenticated requests

### Setting up 2FA

1. `POST /api/auth/enable-2fa` — backend generates a TOTP secret and returns a QR code URI
2. User scans the QR code in Google Authenticator (or any TOTP app)
3. `POST /api/auth/verify-2fa?code=<totp>` — confirms setup with the first code and sets `isTwoFactorEnabled = true`

### Password reset

1. `POST /api/auth/public/forgot-password?email=<email>` — sends a reset link to the user's email
2. User clicks the link: `{frontend.url}/reset-password?token=<token>`
3. `POST /api/auth/public/reset-password?token=<token>&newPassword=<password>` — resets the password (token expires after 24 hours and is single-use)

### OAuth2 login (GitHub / Google)

Redirect the user to the standard Spring OAuth2 endpoint:

```
GET /oauth2/authorization/github
GET /oauth2/authorization/google
```

On success, the user is redirected to:

```
{frontend.url}/oauth2/redirect?token=<jwt>
```

Extract the token from the query param and store it for subsequent requests. New OAuth2 users are automatically registered with `ROLE_USER`.

---

## Using this as a template

The domain layer (notes + audit logs) is completely separate from the auth and security setup. To use this as a starting point for your own project, here's what to keep and what to remove.

### Step 1 — Delete the domain layer

**Notes feature:**

```
controller/NotesController.java
entity/Note.java
repository/NoteRepository.java
service/NoteService.java
service/impl/NoteServiceImpl.java
```

**Audit logs (delete if you don't need operation logging):**

```
controller/AuditLogController.java
entity/AuditLog.java
repository/AuditLogRepository.java
service/AuditLogService.java
service/impl/AuditLogServiceImpl.java
```

### Step 2 — Keep the auth core

```
security/                         # entire package — do not touch
controller/AuthController.java    # signin, signup, 2FA, forgot/reset password
controller/AdminController.java   # user management
controller/CsrfController.java    # CSRF token endpoint (if enabling CSRF)
entity/User.java
entity/Role.java
entity/AppRole.java
entity/PasswordResetToken.java
repository/UserRepository.java
repository/RoleRepository.java
repository/PasswordResetTokenRepository.java
service/UserService.java + impl
service/TotpService.java + impl
service/EmailService.java
dtos/UserDTO.java
```

### Step 3 — Update project identity

**`pom.xml`** — update `groupId`, `artifactId`, and `name`:

```xml
<groupId>com.your.company</groupId>
<artifactId>your-project</artifactId>
<name>your-project</name>
```

**Package rename** — global find & replace:

```
com.secure.notes → com.your.package
```

**`application.yaml`** — update `spring.application.name` and the datasource DB name.

**`SecurityConfig.java`** — update the `CommandLineRunner` seed data to match your use case. Remove `user1` if you don't need a locked test user.

### Step 4 — Add your own domain layer

Once the notes layer is removed, add your own entities, repositories, services, and controllers. The security filter chain, JWT handling, and OAuth2 flow will protect your new endpoints automatically.

To access the authenticated user inside your controllers, use `@AuthenticationPrincipal` the same way `NotesController` does:

```java
@GetMapping("/my-resource")
public ResponseEntity<?> getResource(@AuthenticationPrincipal UserDetails userDetails) {
    String username = userDetails.getUsername();
    // ...
}
```

---

## Configuration reference

### Environment variables

| Variable         | Description                                                           |
| ---------------- | --------------------------------------------------------------------- |
| `DB_NAME`        | MySQL database name                                                   |
| `DB_USERNAME`    | MySQL username                                                        |
| `DB_PASSWORD`    | MySQL password                                                        |
| `JWT_SECRET`     | Base64-encoded 256-bit secret (use `GenerateSecret.java` to generate) |
| `JWT_EXPIRATION` | Token expiry in milliseconds (e.g. `86400000` = 24 hours)             |
| `EMAIL`          | Gmail address for sending reset emails                                |
| `EMAIL_PASSWORD` | Gmail App Password (not your account password)                        |
| `GITHUB_CLIENT`  | GitHub OAuth app client ID                                            |
| `GITHUB_SECRET`  | GitHub OAuth app client secret                                        |
| `GOOGLE_CLIENT`  | Google OAuth app client ID                                            |
| `GOOGLE_SECRET`  | Google OAuth app client secret                                        |

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
│   ├── config/          # OAuth2 success handler, TOTP config
│   ├── request/         # Login/signup request DTOs
│   ├── response/        # Response DTOs
│   └── util/            # AuthUtil (resolves logged-in user)
└── dtos/                # UserDTO
```
