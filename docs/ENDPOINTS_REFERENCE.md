# Librus Synergia Endpoints & Data Models Reference

This document maps all documented endpoints available in Librus Synergia (Gateway API 2.0, Synergia API 3.0, and Wiadomości).

---

## 1. Core Endpoints Summary

Base URLs:
- **API 3.0 (Bearer Token)**: `https://api.librus.pl/3.0/`
- **Gateway API 2.0 (Cookie Session)**: `https://synergia.librus.pl/gateway/api/2.0/`
- **Wiadomości API**: `https://wiadomosci.librus.pl/api/`

| Category | Endpoint | HTTP | Gateway 2.0 | API 3.0 | Description |
|---|---|---|:---:|:---:|---|
| **Identity** | `/Me` | GET | Yes | Yes | Current user, student details, and account info |
| **School** | `/Schools` | GET | Yes | Yes | School name, address, contact, principal |
| **Class** | `/Classes` | GET | Yes | Yes | Grade level, section letter, class tutor ID |
| **Settings** | `/Units` | GET | Yes | Yes | Grading scales, bell schedule, point system config |
| **Directory** | `/Users` or `/Users/{id}` | GET | Yes | Yes | Names and roles of teachers, students, admins |
| **Directory** | `/Subjects` or `/Subjects/{id}` | GET | Yes | Yes | Subject IDs and names (e.g. Matematyka, Język polski) |
| **Directory** | `/Classrooms` or `/{id}` | GET | Yes | Yes | Classroom room numbers and building designations |
| **Grades** | `/Grades` | GET | Yes | Yes | All standard numeric grades |
| **Grades** | `/GradeCategories` | GET | Yes | Yes | Weights, category names (e.g. Sprawdzian, Kartkówka) |
| **Grades** | `/GradeComments` | GET | Yes | Yes | Teacher comments attached to specific grades |
| **Grades** | `/BehaviourGrades` | GET | Yes | Yes | Formal behaviour mark (wzorowe, bardzo dobre...) |
| **Grades** | `/BehaviourGradePoints` | GET | Yes | Yes | Positive/negative behaviour points log |
| **Grades** | `/PointGrades` | GET | Yes | Yes | Point-based grading system scores (0–100%) |
| **Grades** | `/DescriptiveGrades` | GET | Yes | Yes | Descriptive evaluation for primary school |
| **Attendance** | `/Attendances` | GET | Yes | Yes | Attendance marks (absent, late, excused, etc.) |
| **Attendance** | `/AttendanceTypes` | GET | Yes | Yes | Lookup dictionary of attendance statuses |
| **Timetable** | `/Timetables` | GET | Yes | Yes | Weekly schedule with substitutions & cancellations |
| **Timetable** | `/TimetableEntries` | GET | Yes | Yes | Standing static timetable (plan lekcji) |
| **Lessons** | `/Realizations` | GET | Yes | Yes | Completed lessons with topics and attendance IDs |
| **Lessons** | `/PlannedLessons` | GET | Yes | Yes | Planned topics and upcoming assignments |
| **Homework** | `/HomeWorks` | GET | Yes | Yes | Active and past homework assignments |
| **Homework** | `/HomeWorks/{id}/Attachments` | GET | Yes | Yes | Downloadable homework file attachments |
| **Announcements** | `/SchoolNotices` | GET | Yes | Yes | Notice board (ogłoszenia szkolne) |
| **Announcements** | `/SchoolNotices/{id}/Read` | POST | Yes | Yes | Acknowledge/mark notice as read |
| **Lucky Number**| `/LuckyNumbers` | GET | Yes | Yes | Daily lucky student register number |
| **Justifications**| `/Justifications` | GET/POST| Yes | Yes | Parent justification submissions |
| **Messages** | `/api/inbox/unreadMessagesCount` | GET | Wiadomości | Wiadomości | Unread badge counts per folder |
| **Messages** | `/api/{folder}/messages` | GET | Wiadomości | Wiadomości | List messages (inbox, outbox, trash, archive) |
| **Messages** | `/api/{folder}/messages/{id}` | GET | Wiadomości | Wiadomości | Message content (marks as read) |
| **Push** | `/PushConfigurations` | GET/POST| Yes | Yes | Mobile push notifications registration |

---

## 2. Detailed Schemas & Response Samples

### 2.1 `/Me` (Identity)
```json
{
  "Me": {
    "Account": {
      "Id": 12345,
      "UserId": 67890,
      "FirstName": "Piotr",
      "LastName": "Kowalski",
      "Email": "parent@example.com",
      "GroupId": 2,
      "IsAdmin": false
    },
    "User": {
      "Id": 67891,
      "FirstName": "Jan",
      "LastName": "Kowalski",
      "Class": {
        "Id": 456,
        "Url": "https://synergia.librus.pl/gateway/api/2.0/Classes/456"
      }
    },
    "Class": {
      "Id": 456,
      "Url": "https://synergia.librus.pl/gateway/api/2.0/Classes/456"
    },
    "Refresh": 900
  }
}
```
> **Key Insight**: `Me.Account` is the credential owner (parent if logged in as parent), while `Me.User` is the student! Display `Me.User` in the student profile header.

---

### 2.2 `/Grades` (Grades)
```json
{
  "Grades": [
    {
      "Id": 987654,
      "Lesson": { "Id": 111, "Url": ".../Lessons/111" },
      "Subject": { "Id": 42, "Url": ".../Subjects/42" },
      "Category": { "Id": 15, "Url": ".../GradeCategories/15" },
      "Grade": "5",
      "Date": "2026-10-14",
      "AddDate": "2026-10-14 11:22:33",
      "Semester": 1,
      "IsConstituent": true,
      "IsSemester": false,
      "IsSemesterProposal": false,
      "IsFinal": false,
      "IsFinalProposal": false,
      "User": { "Id": 67891 },
      "AddedBy": { "Id": 1002, "Url": ".../Users/1002" },
      "Comments": [
        { "Id": 501, "Url": ".../GradeComments/501" }
      ]
    }
  ]
}
```

#### Grade Categories Lookup (`/GradeCategories`)
```json
{
  "GradeCategories": [
    {
      "Id": 15,
      "Name": "Sprawdzian",
      "Short": "SPR",
      "Weight": 3,
      "Color": { "RGB": "FF0000" },
      "CountToAverage": true
    }
  ]
}
```

---

### 2.3 `/Timetables` (Timetable with Substitutions)
The timetable endpoint returns nested slots grouped by day and period:
```json
{
  "Timetable": {
    "2026-10-15": {
      "1": [
        {
          "LessonNo": 1,
          "TimetableEntry": { "Id": 5001 },
          "Subject": { "Id": 42, "Url": ".../Subjects/42" },
          "Teacher": { "Id": 1002, "Url": ".../Users/1002" },
          "Classroom": { "Id": 204, "Url": ".../Classrooms/204" },
          "IsCancelled": false,
          "IsSubstitution": false,
          "HourFrom": "08:00",
          "HourTo": "08:45"
        }
      ],
      "2": [
        {
          "LessonNo": 2,
          "TimetableEntry": { "Id": 5002 },
          "Subject": { "Id": 43, "Url": ".../Subjects/43" },
          "Teacher": { "Id": 1005, "Url": ".../Users/1005" },
          "Classroom": { "Id": 208, "Url": ".../Classrooms/208" },
          "IsCancelled": false,
          "IsSubstitution": true,
          "OrgSubject": { "Id": 44, "Url": ".../Subjects/44" },
          "OrgTeacher": { "Id": 1003, "Url": ".../Users/1003" },
          "SubstitutionNote": "Zastępstwo za p. Nowaka",
          "HourFrom": "08:55",
          "HourTo": "09:40"
        }
      ]
    }
  }
}
```

---

### 2.4 `/Attendances` & `/AttendanceTypes`
```json
{
  "Attendances": [
    {
      "Id": "t41685",
      "LessonNo": 3,
      "Date": "2026-10-14",
      "AddDate": "2026-10-14 10:15:00",
      "Type": { "Id": 2, "Url": ".../AttendanceTypes/2" },
      "Lesson": { "Id": 112 },
      "Subject": { "Id": 42 }
    }
  ]
}
```

Attendance Types Lookup (`/AttendanceTypes`):
```json
{
  "AttendanceTypes": [
    { "Id": 1, "Name": "Obecność", "Short": "ob", "IsPresence": true, "IsAbsence": false, "Order": 1 },
    { "Id": 2, "Name": "Nieobecność nieusprawiedliwiona", "Short": "nb", "IsPresence": false, "IsAbsence": true, "Order": 2 },
    { "Id": 3, "Name": "Nieobecność usprawiedliwiona", "Short": "u", "IsPresence": false, "IsAbsence": true, "Order": 3 },
    { "Id": 4, "Name": "Spóźnienie", "Short": "sp", "IsPresence": true, "IsAbsence": false, "Order": 4 },
    { "Id": 5, "Name": "Zwolnienie", "Short": "zw", "IsPresence": false, "IsAbsence": false, "Order": 5 }
  ]
}
```

---

### 2.5 `/LuckyNumbers` (Szczęśliwy Numerek)
```json
{
  "LuckyNumbers": {
    "LuckyNumber": 17,
    "LuckyNumberDay": "2026-10-15"
  }
}
```

---

### 2.6 Wiadomości API (`https://wiadomosci.librus.pl/api`)

#### Unread Count
`GET https://wiadomosci.librus.pl/api/inbox/unreadMessagesCount`
```json
{
  "data": {
    "inbox": 3,
    "notes": 0,
    "alerts": 1,
    "substitutions": 0,
    "absences": 0,
    "justifications": 0,
    "trash": 0
  }
}
```

#### Message List
`GET https://wiadomosci.librus.pl/api/inbox/messages?page=1&limit=20`
```json
{
  "total": 45,
  "data": [
    {
      "id": "1002345",
      "topic": "Zebranie z rodzicami",
      "sender": "Jan Kowalski (Wychowawca)",
      "date": "2026-10-12 18:30:00",
      "read": false,
      "hasAttachment": true
    }
  ]
}
```

#### Message Details (Marks message as read on server)
`GET https://wiadomosci.librus.pl/api/inbox/messages/1002345`
```json
{
  "data": {
    "id": "1002345",
    "topic": "Zebranie z rodzicami",
    "sender": "Jan Kowalski (Wychowawca)",
    "sendDate": "2026-10-12 18:30:00",
    "readDate": "2026-10-15 14:20:00",
    "content": "PGRpdiBjbGFzcz0ibXNnLWNvbnRlbnQiPlphcHJhc3phbSB3c3p5c3RraWNoLi4uPC9kaXY+",
    "attachments": [
      {
        "id": 9981,
        "name": "harmonogram.pdf",
        "size": 245000,
        "url": "/api/attachments/9981"
      }
    ]
  }
}
```
*Note: The `content` field is Base64-encoded UTF-8 HTML string.*
