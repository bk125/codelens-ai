# ⬡ CodeLens AI

> An AI-powered developer platform for code review, explanation, migration, and security scanning — built with Spring Boot, Angular 18, and Ollama Gemma 3.

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen?style=flat-square)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-18-red?style=flat-square)](https://angular.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

---

## What is CodeLens AI?

CodeLens AI is a full-stack developer productivity platform that uses AI to help you write, understand, and secure code faster. Instead of copying code into ChatGPT, CodeLens gives you purpose-built tools that are structured, saved per user, and designed around real developer workflows.

---

## Modules

### ⬡ Code Reviewer
Paste any code and get an instant structured review — a 0-10 score, severity-graded issues (critical / warning / info), actionable improvements, best practices, and a complete AI-optimized version shown in a side-by-side Monaco diff view. Language is auto-detected by AI — no manual selection.

### 💡 Code Explainer
Drop in any code and get a plain-English breakdown — what it does, how each function works, parameters and return values, time/space complexity, real-world use cases, and key patterns used. Useful for understanding legacy code or unfamiliar libraries instantly.

### 🚀 Project Migration
Upload a ZIP of your entire project and migrate every code file to a new language automatically. Non-code files (configs, README, assets) are copied as-is. AI generates a professional migration plan with step-by-step checklist, dependency changes, testing strategy, and common pitfalls. Download the complete migrated project as a ZIP.

### 🛡 Security Scanner
Connect your GitHub account and paste any repo URL. CodeLens fetches up to 50 code files, scans each one for real exploitable vulnerabilities using source-to-sink static analysis, and produces a structured security report with CWE IDs, exploit scenarios, severity scores, and code-level fixes. Framework-aware — understands Spring Data JPA, Hibernate, typed parameters, and Spring Security so it doesn't generate false positives.

### ◎ Review History
Every review, explanation, and migration saved automatically per user. Browse past results, reload any item instantly, and re-review updated code against previous scores.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.3, Java 21 |
| AI Provider | Ollama Cloud (Gemma 3 12B) |
| Auth | Spring Security + JWT (JJWT 0.12.6) |
| Database | MySQL 8 + Spring Data JPA |
| Frontend | Angular 18 (Standalone Components) |
| Code Editor | Monaco Editor (VS Code engine) |
| Styling | SCSS + CSS Variables (dark / light theme) |
| Async Processing | Spring @Async + ThreadPoolTaskExecutor |
| GitHub Integration | OAuth 2.0 + AES-256-GCM token encryption |

---

## Architecture

```
codelens-ai/
├── backend/                          # Spring Boot 3.3
│   └── src/main/java/com/codereviewer/
│       ├── config/                   # SecurityConfig, AsyncConfig, CorsConfig
│       ├── controller/               # REST controllers per module
│       ├── service/                  # Business logic + AI calls
│       ├── model/                    # JPA entities
│       ├── repository/               # Spring Data repositories
│       ├── dto/                      # Request / response DTOs (manual builders, no Lombok)
│       ├── security/                 # JwtUtil, JwtAuthFilter, AesEncryptionUtil
│       ├── exception/                # GlobalExceptionHandler + custom exceptions
│       └── util/                     # JsonParserUtil (4-stage AI response repair)
│
└── frontend/                         # Angular 18
    └── src/app/
        ├── core/
        │   ├── models/               # TypeScript interfaces per module
        │   ├── services/             # HTTP services per module
        │   ├── guards/               # authGuard
        │   └── interceptors/         # JWT attach + error handling
        ├── features/
        │   ├── auth/                 # Login, Register, Forgot/Reset Password
        │   ├── review/               # Review page + result + diff view
        │   ├── history/              # Paginated history with detail panel
        │   ├── explainer/            # Code explainer with function breakdown
        │   ├── migration/            # ZIP upload, async progress, download
        │   └── security-scan/        # GitHub OAuth, repo scan, security report
        └── shared/components/
            ├── monaco-editor/        # VS Code editor with real-time lang detection
            ├── diff-viewer/          # Side-by-side original vs optimized
            └── file-upload/          # Drag-and-drop, 16+ extensions
```

---

## Prerequisites

| Tool | Version |
|---|---|
| Java | 21+ |
| Maven | 3.9+ |
| Node.js | 20+ |
| npm | 10+ |
| MySQL | 8.0+ |

---

## Local Setup

### 1. Clone the repo

```bash
git clone https://github.com/bk125/codelens-ai.git
cd codelens-ai
```

### 2. Configure the backend

Copy the example properties file and fill in your values:

```bash
cp backend/src/main/resources/application.properties.example \
   backend/src/main/resources/application.properties
```

Edit `application.properties` with your:
- MySQL password
- Ollama Cloud API key (get one at [ollama.com](https://ollama.com))
- JWT secret (any Base64 string, 32+ chars)
- Gmail app password (for OTP emails)
- GitHub OAuth credentials (for Security Scan — register at [github.com/settings/developers](https://github.com/settings/developers))

### 3. Create the database

```sql
CREATE DATABASE code_reviewer_db;
```

Spring Boot will create all tables automatically on first run (`spring.jpa.hibernate.ddl-auto=update`).

### 4. Run the backend

```bash
cd backend
mvn spring-boot:run
```

Backend starts at `http://localhost:8080`

### 5. Run the frontend

```bash
cd frontend
npm install
ng serve
```

Frontend starts at `http://localhost:4200`

### 6. Open the landing page

Go to `http://localhost:8080` to see the landing page, or `http://localhost:4200` to go directly to the app.

---

## API Reference

All endpoints except `/api/v1/auth/**` and `/api/v1/github/callback` require `Authorization: Bearer <token>`.

### Auth
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Register (sends OTP email) |
| POST | `/api/v1/auth/verify` | Verify email OTP |
| POST | `/api/v1/auth/login` | Login, returns JWT |
| POST | `/api/v1/auth/forgot-password` | Send reset OTP |
| POST | `/api/v1/auth/reset-password` | Reset password with OTP |

### Code Review
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/reviews` | Submit code for review |
| GET | `/api/v1/reviews/history` | Get review history |
| GET | `/api/v1/reviews/{id}` | Get single review |
| GET | `/api/v1/reviews/stats` | Get user stats |
| DELETE | `/api/v1/reviews/{id}` | Delete review |

### Code Explainer
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/explainer` | Explain code |
| GET | `/api/v1/explainer/history` | Get explanation history |
| GET | `/api/v1/explainer/{id}` | Get single explanation |
| DELETE | `/api/v1/explainer/{id}` | Delete explanation |

### Project Migration
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/migration/start` | Upload ZIP and start migration |
| GET | `/api/v1/migration/{id}/status` | Poll migration progress |
| GET | `/api/v1/migration/{id}/download` | Download migrated ZIP |
| GET | `/api/v1/migration/history` | Get migration history |
| DELETE | `/api/v1/migration/{id}` | Delete migration |

### Security Scan
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/github/authorize` | Get GitHub OAuth URL |
| GET | `/api/v1/github/callback` | OAuth callback (public) |
| GET | `/api/v1/github/status` | Check GitHub connection |
| DELETE | `/api/v1/github/disconnect` | Disconnect GitHub |
| POST | `/api/v1/security-scan/start` | Start repo scan |
| GET | `/api/v1/security-scan/{id}/status` | Poll scan progress |
| GET | `/api/v1/security-scan/history` | Get scan history |
| DELETE | `/api/v1/security-scan/{id}` | Delete scan |

---

## Key Engineering Decisions

**No Lombok** — all entities use manual getters, setters, and inner builder classes. Explicit over magic.

**AI response repair** — `JsonParserUtil` uses a 4-stage repair pipeline to handle Gemma's inconsistent JSON output for large code files: tree-based fix → raw string repair → deep repair → fallback.

**Two-step review for large code** — files over 150 lines use a two-step approach: Step 1 gets the analysis JSON (score, issues, improvements) and Step 2 gets the optimized code as plain text. This avoids JSON escaping failures on large files.

**Source-to-sink security analysis** — the security scan prompt enforces framework awareness (Spring Data JPA, Hibernate, typed parameters) to eliminate false positives before they reach the user.

**AES-256-GCM token encryption** — GitHub OAuth tokens stored encrypted at rest, deriving the AES key from the JWT secret so no new config value is required.

**Same async pattern everywhere** — migration and security scan both use `@Async("migrationExecutor")` + 3-second frontend polling, making the pattern consistent and easy to debug.

---

## GitHub OAuth Setup (Security Scan)

1. Go to `https://github.com/settings/developers`
2. Click **New OAuth App**
3. Set callback URL to `http://localhost:8080/api/v1/github/callback`
4. Copy Client ID and Client Secret into `application.properties`

---

## Built by

**Brijesh Kumar** — Full Stack Developer  
[LinkedIn](https://www.linkedin.com/in/brijesh-kumar-56aa1b184/) · [bk71315@gmail.com](mailto:bk71315@gmail.com)

---

## License

MIT
