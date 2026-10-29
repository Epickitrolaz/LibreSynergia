# Librus Mobile — Android & iOS Client for Librus Synergia

An environment and development workspace for building a modern, offline-first mobile client (Android and iOS) for **Librus Synergia**, developed with the assistance of **OpenCode**.

---

## 🚀 Environment & Quickstart

This workspace has been configured with **OpenCode** installed and ready in the environment.

### 1. Launching OpenCode
To start coding with OpenCode inside this repository:
```bash
cd /home/d4f76066/librus-mobile

# Launch interactive OpenCode TUI:
opencode

# Or invoke OpenCode non-interactively for a specific task:
opencode run "Scaffold the Kotlin Multiplatform shared domain models"
```

OpenCode will automatically read `opencode.json` and `AGENTS.md` to follow the architecture, domain models, and API rules established in this workspace.

---

## 📁 Repository Structure

```
librus-mobile/
├── .gitignore                     # Ignores references, build artifacts, secrets
├── README.md                      # This document
├── AGENTS.md                      # AI agent prompt and development guidelines
├── opencode.json                  # OpenCode CLI configuration
├── .env.example                   # Template for Portal and Gateway credentials
├── docs/                          # Clean, synthesized intelligence & specifications
│   ├── LIBRUS_API_GUIDE.md        # Complete guide to Portal vs Gateway authentication
│   ├── ENDPOINTS_REFERENCE.md     # Detailed JSON endpoints & request/response schemas
│   ├── QUIRKS_AND_EDGE_CASES.md   # Documented pitfalls (IDs, references, Base64, etc.)
│   ├── SZKOLNY_ANALYSIS.md        # In-depth architectural audit of the legacy szkolny.eu app
│   └── ARCHITECTURE_AND_ROADMAP.md# Proposed cross-platform mobile architecture & roadmap
├── references/                    # [GITIGNORED] Cloned reference codebases & raw data
│   ├── szkolny-android/           # Full source of legacy Szkolny.eu Android app
│   ├── librus-synergia/           # Python async client & verified reverse-engineering notes
│   ├── librus-sdk/                # TypeScript SDK for Portal & Gateway 2.0
│   ├── librus-rs/                 # Rust client for Synergia & Wiadomości
│   └── openapi/                   # Generated OpenAPI 3.0 JSON specifications
└── src/                           # Mobile client source code (commonMain, androidMain, iosMain)
```

---

## 🧠 Key Intelligence Summary

### 1. Two Working Authentication Flows (2026+)
- **Flow A: Portal Rodzina (Bearer Tokens — Recommended for Mobile)**:
  - Login at `https://portal.librus.pl/konto-librus/login` with email and password.
  - Query `https://portal.librus.pl/api/v3/SynergiaAccounts` to discover all linked child accounts and their unique `accessToken`s.
  - Query `https://api.librus.pl/3.0/*` with `Authorization: Bearer <accessToken>`.
  - Seamless child switching: simply swap the Bearer token in the local client state.
- **Flow B: Direct Synergia Gateway API 2.0 (Cookie Session)**:
  - For school-issued numeric logins (`1234567u`).
  - Follow the 4-step web redirect chain starting at `https://synergia.librus.pl/loguj/portalRodzina`.
  - Maintain the cookie jar (`oauth_token`, `DZIENNIKSID`, and `DeviceCookie`).
  - Send requests to `https://synergia.librus.pl/gateway/api/2.0/*` (no `Authorization` header).
  - Refresh session every 2 hours via `GET https://synergia.librus.pl/refreshToken`.

### 2. The Dead OAuth Password Grant (`grant_type=password`)
Legacy clients (such as Szkolny.eu and older community libraries) authenticated via `POST https://api.librus.pl/OAuth/Token` using `grant_type=password&client_id=28`. **Librus permanently killed this endpoint**, returning `unsupported_grant_type`. The new client must **never** attempt this flow.

### 3. The `DeviceCookie` Rule
Librus issues `DeviceCookie` on domain `api.librus.pl` with `Path=/OAuth` and a 1-year lifetime. **Always persist this cookie with its exact path.** Losing it causes Librus to treat subsequent logins as an unrecognized device, triggering reCAPTCHA challenges.

### 4. Why Szkolny.eu Broke & What to Salvage
Szkolny.eu was an Android-only app that broke because:
1. It depended on the dead OAuth password grant (`grant_type=password`).
2. Its portal login lacked modern CSRF form handling.
3. It relied on HTML scraping fallbacks that now return `403 Forbidden`.

However, Szkolny.eu provides excellent domain logic that should be reused:
- Weighted grade average calculations (accounting for `+`, `-`, and non-counting flags).
- Timetable substitution and cancellation resolution (`OrgSubject`, `OrgTeacher`, `room_changed`).
- Absence status classification (excused vs unexcused).

See `docs/SZKOLNY_ANALYSIS.md` for full details.

---

## 🛠 Target Mobile Architecture

We recommend **Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP)**:
- **Shared Codebase**: 100% of data models, database, networking, and UI code shared across Android and iOS.
- **Networking**: Ktor 3.x with HttpCookies and ContentNegotiation.
- **Persistence**: SQLDelight or Room Multiplatform for offline-first caching.
- **Security**: Hardware-backed credential encryption via Android KeyStore and iOS Keychain.

Full architectural blueprint and phased roadmap available in `docs/ARCHITECTURE_AND_ROADMAP.md`.

---

## 🔒 Git & References Policy

As requested:
- All upstream references (`szkolny-android`, `librus-synergia`, `librus-sdk`, `librus-rs`, and OpenAPI dumps) are stored in `references/` and strictly ignored in `.gitignore`.
- Build tools, SDKs, and local gradle/cocoapods caches are excluded.
- Credentials (`.env`, `.env.*`) are excluded.
