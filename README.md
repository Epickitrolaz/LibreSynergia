# Librus Mobile — Cross-Platform Client for Librus Synergia

A modern, clean, offline-first mobile client for **Librus Synergia** targeting **Android and iOS**, built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform (CMP)**.

---

## 🚀 Key Highlights & Architecture

- **100% Shared UI & Logic**: One declarative codebase in `commonMain` powering both Android and iOS views.
- **English & Polish Support**: English by default, with Polish fully supported and selectable anytime in Settings.
- **Dual Authentication Engines**:
  - **Engine A (Portal Rodzina)**: Web CSRF login against `portal.librus.pl` -> child discovery via `/api/v3/SynergiaAccounts` -> scoped Bearer tokens for `api.librus.pl/3.0`. Supports multi-child switching.
  - **Engine B (Direct Synergia Gateway 2.0)**: Direct numeric login (`1234567u`) with automatic `X-Baner` anti-bot headers and cookie jar persistence (`oauth_token`, `DZIENNIKSID`, and `DeviceCookie` on `/OAuth`).
- **Seamless Login Persistence & Offline-First State**:
  - Remembers user credentials and caches complete app state (grades, schedule, attendance, announcements, lucky number).
  - **Zero-Flicker Launch**: The app opens directly into the student's dashboard using the cached state. Users never see a blocking "Logging in..." screen or full-screen spinner.
  - An unobtrusive top status banner (*"Signing in and updating data..."*) appears while a background task silently refreshes session tokens and synchronizes new changes.
- **Grade Simulator & Target Calculator**:
  - Interactive "What if...?" simulator to project your GPA before taking an exam.
  - Target goal solver ("What grade do I need?") that calculates the exact score required to reach an honors ribbon (e.g. 4.75).
- **Tryb Demo (Demo Mode)**: Instant 1-tap testing preloaded with authentic Polish school curriculum data, substitutions, cancellations, and grades without needing active school credentials.

---

## 📱 Features

1. **Dashboard**:
   - Student profile header with school name, class, and register number.
   - Multi-child dropdown switcher for parents.
   - Daily **Lucky Number** card with celebratory banner if today is your lucky day.
   - Quick GPA and attendance summary tiles.
   - Today's upcoming lessons preview.
   - Recent grades feed.
   - Unread school announcements alerts.

2. **Grades View**:
   - Tab switcher for **Semester 1**, **Semester 2**, and **Full Year**.
   - Overall weighted GPA counter.
   - Subjects grouped with weighted averages and color-coded badges based on assignment weights (Exam, Quiz, Oral Answer, Homework).
   - Detailed dialog modal displaying category, date, teacher name, multiplier, and teacher comments.

3. **Timetable / Schedule**:
   - Day-by-day horizontal switcher (Monday – Friday).
   - Period bell schedule time markers and classroom room badges.
   - **Substitutions**: Highlighted in amber, showing original teacher/subject struck through alongside substitution remarks.
   - **Cancellations**: Highlighted in red with strikethrough.
   - "Changes only" fast filter toggle.

4. **Attendance**:
   - Overall attendance rate percentage.
   - Detailed counter breakdown: *Present*, *Late*, *Excused*, *Unexcused*, and *Exempt*.
   - Chronological log of lesson attendance with status badges.

5. **School Notices**:
   - Official notice board with subject, full content, author, date, and "Mark as Read" action.

6. **Custom Client Feature: Grade Simulator**:
   - Predict average changes by adding hypothetical grades with custom weights.
   - Solve for the grade needed on upcoming assignments to hit target GPAs.

7. **Settings & Themes**:
   - Language selector: **English** (default) or **Polski**.
   - Theme options: **System**, **Light**, **Dark**, and **AMOLED** (true pitch-black background for OLED battery saving).
   - Account session details and sign-out.

---

## 🛠 Building & Testing

### Prerequisites
- JDK 21+ (`JAVA_HOME` pointing to OpenJDK 21)
- Android SDK 35+ with build-tools installed (`ANDROID_HOME=/opt/android-sdk`)
- macOS with Xcode 15+ (required only when packaging final iOS archive)

### 1. Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### 2. Verify iOS Shared Framework Compilation
```bash
./gradlew compileIosMainKotlinMetadata
```

### 3. Build Android Debug APK
```bash
./gradlew assembleDebug
```
The compiled APK will be generated at:
- `build/outputs/apk/debug/librus-mobile-debug.apk`
- Ready-to-install copy: `librus-mobile.apk` (in the project root)

---

## 📁 Repository Structure

```
librus-mobile/
├── AGENTS.md                      # Agent rules, branch workflows, and source attributions
├── README.md                      # This document (English default)
├── build.gradle.kts               # Root KMP build script (Compose Multiplatform 1.7.0)
├── settings.gradle.kts            # Project settings & plugin resolution
├── docs/                          # API intelligence & reverse-engineered specifications
│   ├── LIBRUS_API_GUIDE.md        # Guide to Portal Bearer vs Gateway 2.0 session flows
│   ├── ENDPOINTS_REFERENCE.md     # JSON endpoints and payload reference
│   ├── QUIRKS_AND_EDGE_CASES.md   # Pitfalls (IDs, object references, base64 CDATA)
│   ├── SZKOLNY_ANALYSIS.md        # Legacy szkolny.eu architectural audit
│   └── ARCHITECTURE_AND_ROADMAP.md# Blueprint and implementation roadmap
├── references/                    # [GITIGNORED] Read-only upstream reverse-engineering references
│   ├── szkolny-android/           # Legacy Szkolny.eu Android client
│   ├── librus-synergia/           # Python async client & reverse-engineering notes
│   ├── librus-sdk/                # Modern TypeScript SDK and OpenAPI schemas
│   └── librus-rs/                 # Rust client for Synergia & Wiadomości
└── src/
    ├── commonMain/                # Shared Compose UI, ViewModels, Auth & Domain logic
    │   └── kotlin/pl/librus/mobile/
    │       ├── App.kt             # Navigation root and Scaffold container
    │       ├── auth/              # Dual auth state machine (Portal & Gateway)
    │       ├── data/              # Repositories, mock providers & persistent storage
    │       ├── domain/            # Models (Grade, Timetable, Student) & GPA calculators
    │       ├── network/           # Ktor HTTP client, cookie storage & live API caller
    │       └── ui/                # Compose screens, components, theme & localization
    ├── androidMain/               # Android entry point (MainActivity) & SharedPreferences storage
    ├── iosMain/                   # iOS entry point (MainViewController) & NSUserDefaults storage
    └── commonTest/                # Multiplatform unit tests (Calculators, Cookies, Persistence)
```

---

## 🤖 AI Generation Disclosure

This project, including its source code, architecture, unit tests, and cross-platform mobile client implementation, was generated and developed with the assistance of **Artificial Intelligence (AI)**.

---

## 🌟 Credits & Acknowledgements

- **Szkolny.eu**:
  Special thanks and immense gratitude to **Kacper Ziubryniewicz** and the contributors of the original open-source **[szkolny.eu](https://github.com/szkolny-eu/szkolny-android)** application (GPL-3.0). Their pioneering reverse-engineering of Polish school registers, timetable substitution resolution rules, and weighted grade calculation logic served as an invaluable foundation and inspiration for this project.
- **Community Reverse Engineering**:
  Acknowledgements to **Karol Dylewski** (`librus-synergia` / `emsi/librus_pyapi`), the developers of `@librus-sdk/core`, and the community authors of `librus-rs` for documenting the modern Gateway 2.0 handshake and endpoint specifications.

---

## ⚖️ Legal Disclaimer

**The user takes full responsibility when using this application.**

- This software is an independent, third-party client and is **NOT affiliated with, endorsed by, or sponsored by Librus Sp. z o.o.**
- The developers, contributors, and authors of the AI models cannot be held responsible or liable for any account-related events, including but not limited to account suspensions, security challenges, IP blocks, data discrepancies, or service disruptions resulting from the use of this software.
- Always comply with your school's electronic register acceptable use policies.
