# Szkolny.eu Architecture & Legacy Source Analysis

This document provides a technical audit of the **szkolny.eu** Android client (`szkolny-android`), explaining its architecture, how it interacted with Librus Synergia, why it is outdated and broken today, and what architectural lessons can be applied to the new Android/iOS client.

---

## 1. Project Overview & Scope

- **Repository**: `szkolny-eu/szkolny-android` (Archived/Legacy)
- **Author**: Kuba Szczodrzyński (with community contributors)
- **Tech Stack**: Kotlin (early 1.3–1.4), Java 8, Android SDK (minSdk 21, targetSdk 29), custom `NavLib` navigation library, `im.wangchao.mhttp` networking client, SQLite / custom ORM, Gson.
- **Scope**: Multi-register client supporting Librus Synergia, Vulcan UONET+, MobiDziennik, and iDziennik.

---

## 2. Codebase Organization (Librus Module)

All Librus-specific logic in `szkolny-android` is located under:
`app/src/main/java/pl/szczodrzynski/edziennik/data/api/edziennik/librus/`

Key components:
```
librus/
├── DataLibrus.kt                  # State container holding session tokens & cookies
├── Librus.kt                      # Main orchestrator implementing EdziennikTask
├── LibrusFeatures.kt              # Feature flags for Librus (grades, timetable, etc.)
├── LibrusRecaptchaHelper.kt       # Legacy reCAPTCHA v2 dialog handling
├── login/
│   ├── LibrusLogin.kt             # Mode dispatcher (Email vs Synergia vs JST)
│   ├── LibrusLoginApi.kt          # Direct Synergia OAuth password grant attempt
│   ├── LibrusLoginPortal.kt       # Legacy portal login attempt
│   ├── LibrusLoginSynergia.kt     # Web scraping fallback
│   └── SynergiaTokenExtractor.kt  # Extracted child accounts from Portal API
└── data/
    ├── LibrusApi.kt               # HTTP requests to api.librus.pl
    ├── LibrusData.kt              # Data fetcher & parser
    ├── LibrusMessages.kt          # Wiadomości parser
    ├── LibrusPortal.kt            # Portal API endpoints
    └── LibrusSynergia.kt          # Direct Synergia HTML parsers
```

---

## 3. Why Szkolny.eu Is Outdated & Broken Today

### Issue 1: Reliance on the Dead OAuth Password Grant (`client_id=28`)
In `LibrusLoginApi.kt`:
```kotlin
private fun synergiaGetToken() {
    Request.builder()
        .url("https://api.librus.pl/OAuth/Token")
        .addParameter("grant_type", "password")
        .addParameter("username", data.apiLogin)
        .addParameter("password", data.apiPassword)
        .addParameter("client_id", "28")
        .addHeader("Authorization", "Basic ...")
        .post()
}
```
**Failure Cause**: Librus completely removed support for the `password` grant type on `/OAuth/Token` in 2026. Any request with `grant_type=password` immediately returns `HTTP 400 Bad Request` with `{"error": "unsupported_grant_type"}`. This broke Szkolny.eu's primary login method for all direct Synergia accounts.

### Issue 2: Legacy Portal CSRF and Flow Changes
In `LibrusLoginPortal.kt`, the portal login relied on an older version of `portal.librus.pl` before Librus introduced modern CSRF form tokens, Cloudflare bot management, and updated SSO redirect headers. As a result, the automated login fails to obtain the portal session.

### Issue 3: Monolithic, Android-Only Architecture
Szkolny.eu was written exclusively for Android using native Views, XML layouts, and custom Android-specific libraries:
- `NavLib`: A custom navigation library developed specifically for the app that is no longer maintained.
- Tight coupling to Android `Context`, `SharedPreferences`, and Android UI components inside data layer classes.
- Impossible to share code with iOS without rewriting from scratch.

### Issue 4: Mixed API & HTML Scraping Fallbacks
When API calls failed, Szkolny.eu fell back to scraping `synergia.librus.pl` HTML tables with regular expressions and JSoup. Today, Librus returns `403 Forbidden` or `Brak dostępu` to scrapers, rendering the fallback non-functional.

---

## 4. Valuable Logic to Salvage

Despite being broken at the network and UI layer, Szkolny.eu contains valuable domain logic that should be carried forward into the new client:

### A. Grade Average Calculations
Polish schools use customized weighted averages. Szkolny implemented robust handling for:
- Minus modifiers (e.g. `4-` = 3.75 or 3.66 depending on school config).
- Plus modifiers (e.g. `4+` = 4.5 or 4.33).
- Non-counting grades (e.g. `np` [nieprzygotowany], `bz` [brak zadania], `0` depending on school rules).
- Semester 1 vs Semester 2 vs Final calculation algorithms.

### B. Timetable Substitution Resolution
In `LibrusData.kt`, Szkolny properly handles lesson cancellation and substitution models:
- Pairing original teacher/subject (`OrgSubject`, `OrgTeacher`) with substitute teacher.
- Room change detection (`room_changed`).
- Parallel language/PE group matching based on the student's assigned group tag.

### C. Absence Categorization
Mapping Librus attendance types into standardized domain categories:
- Excused absence (`u`).
- Unexcused absence (`nb`).
- Late arrival (`sp`).
- School representation / exempt (`zw`).

---

## 5. Architectural Upgrade Path for the New Client

| Component | Szkolny.eu (Legacy) | New Client (2026+) |
|---|---|---|
| **Target Platforms** | Android only | **Android & iOS** |
| **Language & Framework**| Kotlin + Java (Android Views) | **Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP)** |
| **Networking** | `im.wangchao.mhttp` (abandoned) | **Ktor 3.x Client** (Multiplatform engine, CIO / Darwin / OkHttp) |
| **Authentication** | Dead `grant_type=password` | **Dual Flow**: Portal Rodzina Bearer + Gateway API 2.0 Browser Session |
| **Persistence** | SQLite / Android Room | **SQLDelight** or **Room Multiplatform** |
| **Architecture** | God classes (`DataLibrus`) | **Clean MVI / Clean Architecture** with immutable StateFlows |
| **Security** | Plain SharedPreferences | **Android KeyStore + iOS Keychain** hardware encryption |
