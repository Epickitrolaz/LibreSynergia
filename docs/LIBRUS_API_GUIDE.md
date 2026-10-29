# Librus Synergia API & Intelligence Guide

This document is the definitive technical reference for interfacing with the Librus educational platform (Librus Synergia) without relying on deprecated scraping or dead OAuth endpoints.

---

## 1. Executive Summary & Ecosystem Overview

Librus Synergia is the dominant electronic school register ("e-dziennik") in Poland. Over time, the platform has evolved into multiple separate subsystems and APIs:

| Subsystem | Primary Host | Auth Mechanism | Intended Consumer | Data Scope |
|---|---|---|---|---|
| **Konto LIBRUS (Portal Rodzina)** | `portal.librus.pl` | Web session cookie (`LibrusPortal_User`), CSRF form post | Parents & Students | Multi-student management, account linking, single sign-on |
| **Synergia API 3.0** | `api.librus.pl/3.0` | `Authorization: Bearer <accessToken>` | Official mobile app & Portal widget | Scoped to a single student profile (`accessToken` obtained via Portal) |
| **Synergia Gateway API 2.0** | `synergia.librus.pl/gateway/api/2.0` | Session cookies (`oauth_token`, `DZIENNIKSID`, `DeviceCookie`) | Modern web interface (React SPA) | Scoped to single school-issued account |
| **Wiadomości (Messages)** | `wiadomosci.librus.pl/api` | Session cookies (`PHPSESSID`, `wiadomosci_sid`) bootstrapped from Synergia | Web messenger | Internal school messaging, attachments, recipient directory |
| **BFF API (Experimental)** | `testbff.librus.pl/v1` | `x-zse-authorization: <accessToken>` | Next-gen mobile BFF | Inbox & aggregated feeds |
| **Legacy Synergia Web** | `synergia.librus.pl` | PHP session cookie | Old PHP portal | **DEPRECATED**: HTML scraping; prone to 403 / anti-bot blocks |

---

## 2. Authentication Architectures

There are **two valid production authentication flows** in 2026. Custom clients must support either or both.

```
                    ┌────────────────────────────────────────────────────────┐
                    │               User Credentials Input                   │
                    └───────────┬────────────────────────────────┬───────────┘
                                │                                │
             Portal Account (Email + Pass)         School Account (Numeric ID + Pass)
                                │                                │
                                ▼                                ▼
               ┌─────────────────────────────────┐  ┌───────────────────────────────────┐
               │    1. GET /konto-librus/login   │  │ 1. GET /loguj/portalRodzina       │
               │       Extract CSRF Token        │  │    Extract 302 Location           │
               └────────────────┬────────────────┘  └─────────────────┬─────────────────┘
                                │                                     │
                                ▼                                     ▼
               ┌─────────────────────────────────┐  ┌───────────────────────────────────┐
               │ 2. POST /konto-librus/login/act │  │ 2. POST /OAuth/Authorization?c=46 │
               │    Get portal session cookie    │  │    action=login&login=...&pass=...│
               └────────────────┬────────────────┘  └─────────────────┬─────────────────┘
                                │                                     │
                                ▼                                     ▼
               ┌─────────────────────────────────┐  ┌───────────────────────────────────┐
               │ 3. GET /api/v3/SynergiaAccounts │  │ 3. Follow redirect chain to       │
               │    Get students + accessTokens  │  │    synergia.librus.pl             │
               └────────────────┬────────────────┘  └─────────────────┬─────────────────┘
                                │                                     │
                                ▼                                     ▼
                     Bearer Token Obtained                oauth_token Cookie Obtained
                                │                                     │
                                ▼                                     ▼
                     `api.librus.pl/3.0/*`               `gateway/api/2.0/*`
```

---

### Flow A: Portal Rodzina (Multi-Child Bearer Flow) — RECOMMENDED FOR MOBILE

Used when the user logs in with an email address registered in **Konto LIBRUS (Portal Rodzina)**. This flow is superior for mobile apps because:
1. It supports multiple children under one parent account without re-entering passwords.
2. It uses standard HTTP `Authorization: Bearer <token>` headers instead of complex cookie jars.
3. Tokens are clean, stateless, and child-scoped.

#### Step 1: Fetch Login Page & Extract CSRF
- **Request**: `GET https://portal.librus.pl/konto-librus/login`
- **Headers**:
  ```http
  User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15
  Accept: text/html,application/xhtml+xml
  ```
- **Action**: Parse the HTML response and extract the CSRF token input field (typically `name="_token"` or `name="csrf_token"`).

#### Step 2: Submit Credentials
- **Request**: `POST https://portal.librus.pl/konto-librus/login/action`
- **Headers**:
  ```http
  Content-Type: application/x-www-form-urlencoded
  Referer: https://portal.librus.pl/konto-librus/login
  ```
- **Form Body**:
  ```
  _token=<CSRF_TOKEN>&email=<USER_EMAIL>&password=<USER_PASSWORD>
  ```
- **Response**: 302 Redirect to `/rodzina/home` or `/api/v3/Me`.
- **Result**: The session cookie (`LibrusPortal_User` or similar) is stored in the cookie jar.

#### Step 3: Fetch Linked Student Profiles & Child Bearer Tokens
- **Request**: `GET https://portal.librus.pl/api/v3/SynergiaAccounts`
- **Headers**: Authenticated via the Portal session cookie.
- **Response Structure**:
  ```json
  [
    {
      "id": 123456,
      "accountIdentifier": "1234567u",
      "group": "synergia",
      "accessToken": "eyJ0eXAiOiJKV1QiLC...",
      "login": "1234567u",
      "studentName": "Jan Kowalski",
      "state": "active",
      "scopes": ["read", "write"]
    },
    {
      "id": 123457,
      "accountIdentifier": "7654321u",
      "group": "synergia",
      "accessToken": "eyJ0eXAiOiJKV1QiLC...",
      "login": "7654321u",
      "studentName": "Anna Kowalska",
      "state": "active",
      "scopes": ["read", "write"]
    }
  ]
  ```

#### Step 4: Making Authenticated API Calls
For each child, use their respective `accessToken`:
```http
GET https://api.librus.pl/3.0/Grades HTTP/1.1
Host: api.librus.pl
Authorization: Bearer eyJ0eXAiOiJKV1QiLC...
Accept: application/json
```

---

### Flow B: Direct Synergia Gateway API 2.0 (Cookie-Backed Flow)

Used when the user only has a school-issued student or parent login (e.g. `1234567u` or `1234567`) and password, without a Portal email account.

#### Step 1: Initiate OAuth Redirection
- **Request**: `GET https://synergia.librus.pl/loguj/portalRodzina`
- **Important**: Do **not** automatically follow redirects.
- **Header Received**:
  ```http
  Location: https://api.librus.pl/OAuth/Authorization?client_id=46&response_type=code&scope=mydata&state=<STATE>
  ```

#### Step 2: Load Authorization Page
- **Request**: `GET https://api.librus.pl/OAuth/Authorization?client_id=46&response_type=code&scope=mydata&state=<STATE>`
- Sets preliminary session cookies on `api.librus.pl`.

#### Step 3: Submit Credentials via Ajax Post
- **Request**: `POST https://api.librus.pl/OAuth/Authorization?client_id=46`
- **Headers**:
  ```http
  Content-Type: application/x-www-form-urlencoded; charset=UTF-8
  Origin: https://api.librus.pl
  Referer: https://api.librus.pl/OAuth/Authorization?client_id=46...
  X-Requested-With: XMLHttpRequest
  Sec-Fetch-Mode: cors
  Sec-Fetch-Site: same-origin
  User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36
  ```
- **Body**:
  ```
  action=login&login=<LOGIN>&pass=<PASSWORD>
  ```
- **Response**:
  ```json
  {"status": "ok", "goTo": "/OAuth/Authorization/2FA?client_id=46"}
  ```

#### Step 4: Follow the Redirect Chain
Follow the `goTo` URL (`https://api.librus.pl/OAuth/Authorization/2FA?client_id=46`) and subsequent `Location` redirects (up to 10 hops), maintaining cookies across domains.
At the end of the chain, the response sets the following critical cookies for `synergia.librus.pl`:
- `oauth_token`: Main bearer session cookie.
- `DZIENNIKSID` / `SDZIENNIKSID`: Session companion identifiers.
- `DeviceCookie` on `api.librus.pl` with `Path=/OAuth`: 1-year persistent cookie that marks the client as recognized and prevents Captcha challenges on subsequent logins.

#### Step 5: Calling Gateway API 2.0
- Base URL: `https://synergia.librus.pl/gateway/api/2.0/<Resource>`
- Headers: Do **not** send an `Authorization` header. Requests are authenticated entirely via the cookie jar.

---

### Why szkolny.eu Broke: The Dead Password Grant (Anti-Pattern)

Older apps (including szkolny.eu and legacy py-librus-api) relied on:
```http
POST https://api.librus.pl/OAuth/Token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic <CLIENT_SECRET>

grant_type=password&client_id=28&username=...&password=...
```
🚨 **Librus permanently disabled this grant type.** Since early 2026, it returns:
```json
{"error": "unsupported_grant_type"}
```
**Never attempt to revive `grant_type=password` against `/OAuth/Token`.**

---

## 3. Session Lifecycles & Keep-Alive Strategies

| Token / Cookie | Expiry Duration | Refresh Mechanism | What Happens When Expired |
|---|---|---|---|
| **Portal Bearer Token** (`api.librus.pl/3.0`) | ~24 hours | Call `portal.librus.pl/api/v3/SynergiaAccounts` with stored Portal session | Returns `401 Unauthorized`. App re-fetches `SynergiaAccounts`. If Portal cookie died, app re-authenticates via stored credentials. |
| **Gateway `oauth_token`** | 10 min sliding, max ~24h | `GET https://synergia.librus.pl/refreshToken` every 2 hours | Returns `401`. App re-runs the 4-step login chain. |
| **`DeviceCookie`** | 1 year (`Max-Age=31536000`) | Re-issued during login on `Path=/OAuth` | Store persistently in Keychain / EncryptedSharedPreferences! Without it, consecutive logins may trigger Cloudflare/Librus Captchas. |
| **Wiadomości Session** | 30–60 minutes | `GET https://synergia.librus.pl/wiadomosci3` | Returns `401`/`403`. Bootstrap again immediately. |

---

## 4. The Messages Subsystem (`wiadomosci.librus.pl`)

The messaging module is separated from the core gradebook:
1. **Bootstrap**: Before querying message endpoints, execute:
   `GET https://synergia.librus.pl/wiadomosci3`
   This sets the session cookies for `wiadomosci.librus.pl`.
   - *Note*: If the school has disabled the messages module, this returns `Brak dostępu`. The app must gracefully disable the Messages tab for this student.
2. **Read vs Peek**:
   - `GET /api/<mailbox>/messages`: Lists messages. Does **not** mark them as read.
   - `GET /api/<mailbox>/messages/<id>`: Fetches full message body and **marks it as read** in the official register!
   - To show an unread badge without marking messages read, fetch message lists only and query `/api/inbox/unreadMessagesCount`.
3. **Content Encoding**:
   - Message bodies are wrapped in CDATA and encoded in Base64 or escaped HTML (`&lt;br&gt;`, `\n`). Always decode Base64 and sanitize HTML before rendering.

---

## 5. Network Best Practices & Defensive Engineering

1. **Connection Pooling & Rate Limiting**:
   - Librus enforces strict rate limiting and concurrent connection caps.
   - Restrict simultaneous requests to a **maximum of 4 to 6 connections** per host.
   - For background sync, space polls out by **at least 15 minutes**.
2. **Caching Reference Data**:
   - Subject names, teacher rosters, classrooms, grade categories, and attendance types rarely change throughout the school semester.
   - Cache them locally with a TTL of 24 hours. When an unknown ID is encountered in an active fetch (e.g. new subject), trigger a background refresh of that lookup table.
3. **Timezone Handling**:
   - All dates, timetable lesson slots, and lucky numbers operate in **Poland Time (Europe/Warsaw, UTC+1 / UTC+2)**. Always parse and project timestamps using Europe/Warsaw, regardless of the user's phone timezone.
