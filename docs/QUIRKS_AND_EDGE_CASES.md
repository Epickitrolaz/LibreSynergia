# Librus Synergia API Quirks & Edge Cases

When developing a client against Librus Synergia, engineers encounter numerous non-standard API designs and edge cases that cause standard parsers to fail. This document catalogs all known quirks and their mitigation strategies.

---

## 1. Highly Inconsistent Identifier Types

Librus does **not** adhere to consistent ID types across endpoints or even within the same endpoint:

| Entity | ID Example | Raw JSON Type | Parser Requirement |
|---|---|---|---|
| User ID | `67891` | Integer | Parse as Int or Long |
| Subject ID | `42` | Integer | Parse as Int |
| Timetable Lesson ID | `"41999"` | String (numeric) | Normalize to String or parse safely |
| Attendance ID | `"t41685"` | String (alphanumeric prefix) | Must be treated as String |
| School Notice ID | `"LID-NBOARD-NOTICE-123"` | String (UUID/prefix) | Must be treated as String |
| Kindergarten User ID | `"LID-AUTH-USER-89123"` | String | String |

**Mitigation in Data Models**:
Store entity IDs as `String` in the domain layer, or use custom serializers that accept both integer and string JSON tokens (e.g. `JsonElement` / polymorphic deserialization in Kotlinx Serialization or Jackson).

---

## 2. Object References Instead of Scalar Foreign Keys

When an endpoint refers to another entity (e.g. Subject or Teacher in a Grade record), it returns an object rather than a bare ID:

```json
"Subject": {
  "Id": 42,
  "Url": "https://synergia.librus.pl/gateway/api/2.0/Subjects/42"
}
```

Sometimes `Id` is missing and only `Url` is present, or the object is `null`.

**Mitigation**:
Implement a generic reference wrapper:
```kotlin
@Serializable
data class ApiRef(
    val Id: String? = null,
    val Url: String? = null
) {
    val resolvedId: String?
        get() = Id ?: Url?.substringAfterLast('/')
}
```

---

## 3. Timetable Structure (Slot Arrays by Period)

The timetable endpoint `/Timetables` does not return a flat list of lessons. It returns an object keyed by date, then keyed by period number (1-based lesson index), with an array of lessons per period:

```json
{
  "Timetable": {
    "2026-10-15": {
      "1": [ { "LessonNo": 1, "Subject": { "Id": 42 } } ],
      "2": [
        { "LessonNo": 2, "Subject": { "Id": 43 }, "Group": "Grupa 1" },
        { "LessonNo": 2, "Subject": { "Id": 44 }, "Group": "Grupa 2" }
      ]
    }
  }
}
```

### Critical Lesson Flags:
- `IsCancelled` (`bool`): The lesson was cancelled.
- `IsSubstitution` (`bool`): The lesson has a substitution. When `true`, fields `OrgSubject`, `OrgTeacher`, and `SubstitutionNote` specify the original class that was replaced.
- Parallel groups: A student might have language groups or PE split by gender. The app must match the student's assigned group or display both with group badges.

---

## 4. Message Body Base64 & HTML Escaping

Message contents returned by `https://wiadomosci.librus.pl/api/*` have three layers of encoding:
1. Base64 encoding.
2. Escaped HTML entities (`&lt;div&gt;`, `&amp;nbsp;`, `&quot;`).
3. Legacy Windows-1250 vs UTF-8 characters on older school servers.

**Mitigation**:
```kotlin
fun decodeMessageContent(raw: String): String {
    val decodedBytes = try {
        Base64.decode(raw)
    } catch (e: Exception) {
        raw.toByteArray(Charsets.UTF_8)
    }
    val html = String(decodedBytes, Charsets.UTF_8)
    return sanitizeHtml(html)
}
```

---

## 5. The `DeviceCookie` Path Trap

When logging into Synergia Gateway API 2.0 via `https://api.librus.pl/OAuth/Authorization`, Librus sets:
```http
Set-Cookie: DeviceCookie=abc123xyz; Path=/OAuth; Max-Age=31536000; Secure; HttpOnly
```

⚠️ Standard cookie storage implementations (like default OkHttp or URLSession jars) that query cookies for host `api.librus.pl` with root path `/` will **omit** this cookie on subsequent requests to `/` or other paths unless specifically requested for the `/OAuth` path.

If `DeviceCookie` is lost, Librus flags subsequent login attempts from that IP as "unrecognized device" and presents a reCAPTCHA challenge, completely breaking automated login.

**Mitigation**:
Explicitly persist all cookies regardless of path, and ensure requests to `/OAuth/*` always carry `DeviceCookie`.

---

## 6. Kindergarten (Przedszkole) Accounts

Preschool and kindergarten accounts enrolled in Librus do not use standard lesson periods:
- A request to `/Timetables` returns `HTTP 403 Forbidden`.
- The child profile has a kindergarten identifier (`LID-AUTH-USER-...`).
- Activities are scheduled in time ranges rather than numeric periods (`LessonNo = null`).

**Mitigation**:
When `/Timetables` returns 403, check if the account is associated with a kindergarten unit and switch timetable display to time-block mode rather than crashing.

---

## 7. Dynamic Grading Systems & Scales

Different Polish schools configure different grading systems in Librus:
- **Standard (1–6)**: With pluses (`+`) and minuses (`-`). Different schools configure pluses as `+0.5`, `+0.25`, or `+0.33` in average calculations.
- **Point System (0–100%)**: Some schools grade solely by points scored out of max points.
- **Descriptive Grading (Oceny opisowe)**: Used in grades 1–3 of primary school.

The grading rules and weights are defined in the `/Units` and `/GradeCategories` endpoints. A client app should not hardcode a single fixed formula for grade averages; it should respect the weights and arithmetic defined in `/Units`.
