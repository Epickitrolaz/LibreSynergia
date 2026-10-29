# Antigravity & OpenCode Agent Instructions — Librus Mobile Client

Guidance for Antigravity, OpenCode, and other AI coding agents working in this repository.

---

## 1. Project Mission

You are building a clean, modern, offline-first **Android and iOS mobile client for Librus Synergia**, the primary electronic school register used in Poland.

The user will provide their own Android SDK, Xcode, and local build tools. Your responsibility is writing clean, correct, idiomatic cross-platform code, unit tests, and architecture.

---

## 2. Git & Branching Policy

1. **Active Working Branch**:
   - **The default working branch is `dev`. All ongoing development occurs on `dev` (or short-lived feature branches cut from `dev`).**
   - The `main` branch is reserved for stable releases.
2. **Push Code to Remote After Every Session**:
   - **Always push your committed changes to the remote repository (`git push origin <branch>`) after each session.**
3. **Verification on Every Modification**:
   - **After EACH modification, the build MUST be tested for all available platforms** (e.g. `./gradlew testDebugUnitTest`, `./gradlew compileIosMainKotlinMetadata`, `./gradlew assembleDebug`).
4. **Commit Every Change**:
   - **Every change must be committed to git immediately after verification.** Keep git commits frequent, focused, and accompanied by clear commit messages.
5. **Do Not Commit Reference & Cache Files**:
   - Do NOT commit files in `references/`, build artifacts (`build/`, `.gradle/`), local sdk paths (`local.properties`), or secrets.

---

## 3. Original Reference Files & Intelligence Sources

The following files and folders have been present in this repository from the start as reference material. **Use them as reference if needed, but DO NOT modify or commit them**:

- `references/szkolny-android/`:
  - **Source**: Legacy Szkolny.eu Android application (GPL-3.0 by Kacper Ziubryniewicz & contributors, `https://github.com/szkolny-eu/szkolny-android`).
  - **Use for**: Reverse engineering domain logic, weighted average grade calculations, timetable substitutions/cancellations, absence classification, and UI ideas.
- `references/librus-synergia/`:
  - **Source**: Reverse-engineered Python client (MIT, by Karol Dylewski / `emsi/librus_pyapi`, `https://github.com/karoldylewski/librus-synergia`).
  - **Use for**: Modern Gateway 2.0 auth handshake, redirect handling, and endpoint documentation.
- `references/librus-sdk/`:
  - **Source**: Modern TypeScript SDK and CLI (`@librus-sdk/core`).
  - **Use for**: Verified flows for `portal.librus.pl` (Portal Rodzina) and Gateway 2.0 (`X-Baner` anti-bot header calculation, `SynergiaAccounts`, `Auth/TokenInfo`).
- `references/librus-rs/`:
  - **Source**: Community Rust client for Synergia & Wiadomości (`https://github.com/0x00000FF/librus-rs`).
- `references/openapi/`:
  - **Source**: Synthesized OpenAPI 3.0 JSON specifications for Librus API 3.0, Gateway 2.0, and Wiadomości.
- `docs/`:
  - Pre-synthesized architectural documentation (`LIBRUS_API_GUIDE.md`, `ENDPOINTS_REFERENCE.md`, `QUIRKS_AND_EDGE_CASES.md`, `SZKOLNY_ANALYSIS.md`, `ARCHITECTURE_AND_ROADMAP.md`).

---

## 4. Strict Rules & Guardrails

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

## 5. Development Workflow

Run commands directly in this project directory:
```bash
# Verify unit tests and multiplatform compilation
./gradlew testDebugUnitTest
./gradlew compileIosMainKotlinMetadata

# Build debug APK
./gradlew assembleDebug
```
