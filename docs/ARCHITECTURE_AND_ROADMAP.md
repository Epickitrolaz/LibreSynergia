# Cross-Platform Architecture & Implementation Roadmap

This document outlines the recommended architecture for building a clean, modern, offline-first client for Librus Synergia targeting **Android and iOS**.

---

## 1. Architectural Principles

1. **100% Logic & UI Sharing**: Use **Kotlin Multiplatform (KMP)** paired with **Compose Multiplatform (CMP)** to share domain logic, network pipelines, database storage, and UI across Android and iOS.
2. **Offline-First Resilience**: All screen data (grades, timetable, attendances, notices) is rendered directly from a local database cache (SQLDelight or Room Multiplatform). The network layer synchronizes deltas in the background.
3. **Dual-Backend Support**:
   - **Engine A (Portal Rodzina)**: Email/Password -> Bearer tokens for `api.librus.pl/3.0` (Supports multi-child switching out of the box).
   - **Engine B (Gateway 2.0)**: Direct school ID login -> Cookie session for `synergia.librus.pl/gateway/api/2.0`.
4. **Hardware-Backed Security**: Encrypt credentials, session tokens, and cached student records using Android Keystore on Android and the Keychain Services API on iOS.
5. **No HTML Scraping**: Rely exclusively on JSON APIs (`api.librus.pl/3.0`, `gateway/api/2.0`, and `wiadomosci.librus.pl/api`).

---

## 2. Recommended Technology Stack

```
                          ┌─────────────────────────────────────┐
                          │         Compose Multiplatform       │
                          │   (Shared UI: Android & iOS Views)  │
                          └──────────────────┬──────────────────┘
                                             │
                          ┌──────────────────▼──────────────────┐
                          │         MVI ViewModels / State      │
                          │        (Kotlinx Coroutines/Flow)    │
                          └──────────────────┬──────────────────┘
                                             │
                          ┌──────────────────▼──────────────────┐
                          │            Domain Layer             │
                          │   UseCases, GradeAverages, Models   │
                          └──────────┬────────────────┬─────────┘
                                     │                │
            ┌────────────────────────▼───┐        ┌───▼────────────────────────┐
            │       Local Database       │        │        Network Engine      │
            │   SQLDelight / Room KMP    │        │      Ktor 3.x Multiplatform │
            └────────────────────────────┘        └────────────────────────────┘
```

| Component | Selected Library | Purpose |
|---|---|---|
| **Language** | Kotlin 2.1+ | Shared codebase for commonMain, androidMain, iosMain |
| **UI Framework** | Compose Multiplatform (Jetpack Compose for iOS & Android) | Single declarative UI codebase |
| **HTTP Client** | Ktor 3.x (`ktor-client-core`, `ktor-client-cio`, `ktor-client-darwin`) | Multiplatform async HTTP client with cookies plugin |
| **Serialization** | `kotlinx.serialization.json` | High-performance, reflection-free JSON parsing |
| **Local Database** | SQLDelight or Room Multiplatform | Strongly typed SQLite database with migrations |
| **Key-Value & Secrets** | Multiplatform-Settings (`crypt` module) | Android KeyStore / iOS Keychain encrypted storage |
| **Dependency Injection** | Koin | Lightweight Kotlin Multiplatform dependency injection |
| **Date & Time** | `kotlinx-datetime` | Multiplatform timezone management (Europe/Warsaw) |

---

## 3. Project Directory Structure

```
librus-mobile/
├── .gitignore                     # Ignores references/, build artifacts, secrets
├── README.md                      # Project documentation and quickstart
├── AGENTS.md                      # Instructions for OpenCode agent
├── opencode.json                  # OpenCode AI configuration
├── docs/                          # Architecture & API specifications
│   ├── LIBRUS_API_GUIDE.md
│   ├── ENDPOINTS_REFERENCE.md
│   ├── QUIRKS_AND_EDGE_CASES.md
│   ├── SZKOLNY_ANALYSIS.md
│   └── ARCHITECTURE_AND_ROADMAP.md
├── references/                    # GITIGNORED reference codebases
│   ├── szkolny-android/           # Legacy Szkolny.eu client
│   ├── librus-synergia/           # Python async client & verified docs
│   ├── librus-sdk/                # TypeScript SDK & OpenAPI definitions
│   ├── librus-rs/                 # Rust client
│   └── openapi/                   # OpenAPI specs for v3, gateway 2.0, wiadomosci
└── src/                           # Mobile app source code (when ready)
    ├── commonMain/                # Shared Kotlin logic & Compose UI
    │   ├── kotlin/
    │   │   ├── auth/              # Dual-engine auth state machine
    │   │   ├── network/           # Ktor HTTP client & interceptors
    │   │   ├── data/              # Repositories & SQLDelight database
    │   │   ├── domain/            # Models, calculations, business rules
    │   │   └── ui/                # Compose Multiplatform screens & components
    ├── androidMain/               # Android entry point, MainActivity, Widgets
    └── iosMain/                   # iOS entry point, UIViewController wrapper
```

---

## 4. Phase-by-Phase Development Roadmap

### Phase 1: Authentication & Session Management
- [ ] Implement `PortalAuthClient` for Portal Rodzina (`portal.librus.pl`):
  - Form GET -> CSRF extraction -> Form POST -> Cookie preservation.
  - Fetch student list and individual Bearer tokens from `/api/v3/SynergiaAccounts`.
- [ ] Implement `GatewayAuthClient` for Direct Synergia credentials:
  - Redirection GET -> Ajax POST -> Redirect chain follower -> `oauth_token` + `DeviceCookie`.
- [ ] Implement secure credential storage and token refresh loop (`/refreshToken` every 2 hours).

### Phase 2: Domain Modeling & Local Persistence
- [ ] Define Kotlin data classes for all entities (Student, Grade, Attendance, TimetableEntry, Lesson, Homework, Notice, Message).
- [ ] Implement custom JSON deserializers to handle inconsistent ID types (integers vs strings) and object references (`ApiRef`).
- [ ] Set up SQLDelight database with tables for offline caching.

### Phase 3: Core Features UI (Compose Multiplatform)
- [ ] **Home Dashboard**:
  - Student profile card with multi-child dropdown switcher.
  - Lucky Number widget.
  - Today's upcoming timetable preview.
  - Recent grades card.
- [ ] **Grades View**:
  - Grouped by subject.
  - Color-coded badges based on category weights.
  - Detailed modal with teacher comments, date, category, and multiplier.
  - Average calculations (Semester 1, Semester 2, Proposed Final).
- [ ] **Timetable View**:
  - Day / Week horizontal pager.
  - Visual indicators for cancellations (strikethrough) and substitutions (accent color).
  - Bell schedule time markers.
- [ ] **Attendance View**:
  - Monthly calendar & list views.
  - Absence breakdown (excused vs unexcused percentage).

### Phase 4: Extended Features
- [ ] **Homework**:
  - Due date countdown.
  - Attachment download and viewer integration.
- [ ] **School Notices (Ogłoszenia)**:
  - Notice board feed with "Mark as Read" action.
- [ ] **Messages (Wiadomości)**:
  - Mailbox tabs (Inbox, Sent, Trash).
  - Unread badge counter.
  - Message reader with Base64 HTML decoding.
  - Secure attachment downloader.

### Phase 5: Notifications & Polish
- [ ] Background change detection polling (15-min intervals).
- [ ] System notifications for new grades and schedule changes.
- [ ] Biometric app lock (Face ID / Fingerprint).
- [ ] Dark mode & theme customization.
