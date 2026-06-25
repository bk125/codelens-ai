# ⬡ CodeLens AI — Code Review Platform

A full-stack AI-powered code review platform built with **Spring Boot** on the backend and **Angular 18** on the frontend. Uses **Groq API (LLaMA 3.3 70B)** for blazing-fast AI responses — no billing required.

---

## 🏗️ Architecture

```
ai-code-reviewer/
├── backend/     # Spring Boot 3.3 + Spring Security + JWT + MySQL
└── frontend/    # Angular 18 (standalone components) + Monaco Editor
```

---

## ⚙️ Prerequisites

| Tool | Version |
|------|---------|
| Java | 21+ |
| Maven | 3.9+ |
| Node.js | 20+ |
| npm | 10+ |
| MySQL | 8.0+ |

---

## 🚀 Quick Start

### 1. MySQL Setup
```sql
CREATE DATABASE code_reviewer_db;
```
Or use Docker:
```bash
docker-compose up -d
```

### 2. Get a free Groq API Key
Go to https://console.groq.com/keys — free, no credit card needed.

### 3. Backend Setup
```bash
cd backend

# Set your values in application.properties or as env vars:
# groq.api.key=your-groq-api-key
# spring.datasource.password=your-mysql-password

mvn spring-boot:run
```
Backend starts at: `http://localhost:8080`

### 4. Frontend Setup
```bash
cd frontend
npm install
ng serve
```
Frontend starts at: `http://localhost:4200`

---

## 🔑 Configuration

Edit `backend/src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/code_reviewer_db
spring.datasource.username=root
spring.datasource.password=yourpassword

# Groq API (free at https://console.groq.com/keys)
groq.api.key=your-groq-api-key-here
groq.api.url=https://api.groq.com/openai/v1/chat/completions
groq.api.model=llama-3.3-70b-versatile

# JWT
app.jwt.secret=your-base64-secret
app.jwt.expiration=86400000
```

---

## 📡 API Reference

All endpoints except `/api/v1/auth/**` require `Authorization: Bearer <token>`.

### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login, returns JWT |

### Reviews
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/reviews` | Submit code for review |
| GET | `/api/v1/reviews/history` | Get all reviews for current user |
| GET | `/api/v1/reviews/{id}` | Get a single review |
| POST | `/api/v1/reviews/{id}/re-review` | Re-review updated code |
| GET | `/api/v1/reviews/stats` | Get user stats (total, avg score) |
| DELETE | `/api/v1/reviews/{id}` | Delete a review |

---

## 🧱 Backend Package Structure

```
com.codereviewer/
├── controller/     CodeReviewController, AuthController
├── service/        CodeReviewService, AiReviewService, AuthService
├── repository/     CodeReviewRepository, UserRepository
├── model/          CodeReview, User
├── dto/            Request/Response DTOs
├── security/       JwtUtil, JwtAuthFilter
├── config/         SecurityConfig, AiConfig, CorsConfig
├── exception/      GlobalExceptionHandler + custom exceptions
└── util/           JsonParserUtil (extracts JSON from AI response)
```

---

## 🎨 Frontend Structure

```
src/app/
├── core/
│   ├── models/         review.model.ts, auth.model.ts
│   ├── services/       review-api.service.ts, auth.service.ts, theme.service.ts
│   ├── interceptors/   auth.interceptor.ts, error.interceptor.ts
│   └── guards/         auth.guard.ts
├── features/
│   ├── auth/           login, register
│   ├── review/         review-page (Monaco editor + file upload), review-result (diff view)
│   ├── history/        history with clickable detail panel
│   └── playground/     built-in API testing UI
└── shared/components/
    ├── monaco-editor/  VS Code-grade editor with custom dark/light theme
    ├── diff-viewer/    Side-by-side diff (original vs optimized)
    └── file-upload/    Drag-and-drop file reader (.java/.js/.py)
```

---

## ✨ Features

- **AI Code Review** — Score (0-10), issues by severity, improvements, best practices, optimized code
- **Multi-language** — Java, JavaScript, Python
- **Monaco Editor** — VS Code-quality syntax highlighting in the browser
- **Side-by-side Diff** — Visual comparison of original vs AI-optimized code
- **File Upload** — Drag-and-drop `.java`, `.js`, `.py` files (single file, 500 KB max)
- **Dark / Light Theme** — Toggle with persistence across sessions
- **JWT Authentication** — Register, login, secure all review endpoints
- **Review History** — All past reviews with clickable detail panel
- **API Playground** — Built-in UI to test all backend REST endpoints live

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.3 |
| AI Provider | Groq API (LLaMA 3.3 70B Versatile) |
| Auth | Spring Security + JWT (JJWT 0.12.6) |
| Database | MySQL 8 + Spring Data JPA |
| Frontend | Angular 18 (Standalone Components) |
| Code Editor | Monaco Editor (VS Code engine) |
| Styling | SCSS with CSS Variables (dark + light themes) |
