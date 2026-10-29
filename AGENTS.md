# OpenCode Agent Instructions — Librus Mobile Client

Guidance for OpenCode and other AI coding agents working in this repository.

---

## 1. Project Mission

You are building a clean, modern, offline-first **Android and iOS mobile client for Librus Synergia**, the primary electronic school register used in Poland.

The user will provide their own Android SDK, Xcode, and local build tools. Your responsibility is writing clean, correct, idiomatic cross-platform code, unit tests, and architecture.

---

## 2. Repository Layout

- `docs/`: **Primary specifications and intelligence.**
  - `docs/LIBRUS_API_GUIDE.md`: Deep dive into authentication (Portal Bearer vs Gateway 2.0 session), token refresh, and network rules.
  - `docs/ENDPOINTS_REFERENCE.md`: Complete endpoints and JSON schemas.
  - `docs/QUIRKS_AND_EDGE_CASES.md`: Critical pitfalls (inconsistent IDs, object references, base64 CDATA, timetable slots, kindergarten accounts).
  - `docs/SZKOLNY_ANALYSIS.md`: Exhaustive architectural breakdown of the legacy `szkolny.eu` app.
  - `docs/ARCHITECTURE_AND_ROADMAP.md`: Proposed cross-platform architecture and roadmap.
- `references/`: **Read-only reverse-engineering references (ignored by git).**
  - `references/szkolny-android`: Full legacy source of Szkolny.eu (use for grade calculations, domain rules, UI ideas).
  - `references/librus-synergia`: Python async client with verified reverse-engineered notes and parsers.
  - `references/librus-sdk`: TypeScript SDK with OpenAPI specifications (`references/openapi/`).
  - `references/librus-rs`: Rust client for Synergia & Wiadomości.
- `src/`: Application source code.

---

## 3. Strict Rules & Guardrails

1. **NEVER use the dead OAuth password grant**:
   `POST https://api.librus.pl/OAuth/Token` with `grant_type=password` was killed by Librus in 2026 and returns `unsupported_grant_type`. Do not attempt to use it.
2. **NEVER use HTML scraping**:
   Do not parse HTML tables from `synergia.librus.pl`. Use only the official JSON endpoints (`api.librus.pl/3.0`, `gateway/api/2.0`, and `wiadomosci.librus.pl/api`).
3. **DO NOT modify files in `references/`**:
   The `references/` directory is for inspection only. All new code goes into `src/`.
4. **Defensive JSON Parsing**:
   Entity IDs may arrive as integers or strings (`67891` vs `"41999"` vs `"t41685"`). Always parse them defensively.
5. **Handle Object References**:
   Subject, teacher, and classroom references arrive as `{"Id": 42, "Url": "..."}` or `null`. Resolve them using local cached lookup tables.
6. **DeviceCookie Persistence**:
   When implementing Gateway 2.0, always persist `DeviceCookie` (which lives on `Path=/OAuth`). Without it, Librus triggers Captcha challenges on new logins.
7. **Rate Limit Respect**:
   Cap concurrent connections to 4–6 per host. Polling should never be more frequent than 15 minutes.

---

## 4. Development Workflow with OpenCode

Run OpenCode directly in this project directory:
```bash
# Launch interactive TUI
opencode

# Or run non-interactively with a specific task
opencode run "Implement the PortalAuthClient in commonMain"
```

When building features, always refer to `docs/ENDPOINTS_REFERENCE.md` and check `references/` for how legacy or verified community libraries handled the domain logic.
