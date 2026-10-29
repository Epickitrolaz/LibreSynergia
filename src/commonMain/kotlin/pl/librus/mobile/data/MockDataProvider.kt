package pl.librus.mobile.data

import pl.librus.mobile.domain.models.*

object MockDataProvider {

    val mockStudents = listOf(
        ChildAccount(
            id = "1001",
            accountIdentifier = "1234567u",
            studentName = "Jan Kowalski",
            login = "1234567u",
            accessToken = "mock_token_jan",
            state = "active"
        ),
        ChildAccount(
            id = "1002",
            accountIdentifier = "1234568u",
            studentName = "Zofia Kowalska",
            login = "1234568u",
            accessToken = "mock_token_zofia",
            state = "active"
        )
    )

    fun getStudentProfile(child: ChildAccount): StudentProfile {
        return if (child.id == "1001") {
            StudentProfile(
                id = "1001",
                firstName = "Jan",
                lastName = "Kowalski",
                fullName = "Jan Kowalski",
                className = "3B",
                classRegisterNumber = 17,
                schoolName = "III Liceum Ogólnokształcące im. Adama Mickiewicza",
                isParentView = false,
                accountEmail = "jan.kowalski@librus.pl"
            )
        } else {
            StudentProfile(
                id = "1002",
                firstName = "Zofia",
                lastName = "Kowalska",
                fullName = "Zofia Kowalska",
                className = "1A",
                classRegisterNumber = 9,
                schoolName = "Szkoła Podstawowa nr 15",
                isParentView = false,
                accountEmail = "zofia.kowalska@librus.pl"
            )
        }
    }

    val luckyNumber = LuckyNumber(
        number = 17,
        date = "2026-10-29"
    )

    val subjects = listOf(
        Subject(id = "1", name = "Język polski", shortName = "POL", order = 1),
        Subject(id = "2", name = "Matematyka", shortName = "MAT", order = 2),
        Subject(id = "3", name = "Język angielski", shortName = "ANG", order = 3),
        Subject(id = "4", name = "Informatyka", shortName = "INF", order = 4),
        Subject(id = "5", name = "Fizyka", shortName = "FIZ", order = 5),
        Subject(id = "6", name = "Historia", shortName = "HIS", order = 6),
        Subject(id = "7", name = "Biologia", shortName = "BIO", order = 7),
        Subject(id = "8", name = "Chemia", shortName = "CHE", order = 8),
        Subject(id = "9", name = "Geografia", shortName = "GEO", order = 9),
        Subject(id = "10", name = "Wychowanie fizyczne", shortName = "WF", order = 10)
    )

    val categories = listOf(
        GradeCategory(id = "1", name = "Sprawdzian", shortName = "SPR", weight = 3.0, colorHex = "#D32F2F"),
        GradeCategory(id = "2", name = "Kartkówka", shortName = "KAR", weight = 2.0, colorHex = "#F57C00"),
        GradeCategory(id = "3", name = "Odpowiedź ustna", shortName = "ODP", weight = 2.0, colorHex = "#7B1FA2"),
        GradeCategory(id = "4", name = "Praca domowa", shortName = "DOM", weight = 1.0, colorHex = "#388E3C"),
        GradeCategory(id = "5", name = "Aktywność", shortName = "AKT", weight = 1.0, colorHex = "#1976D2"),
        GradeCategory(id = "6", name = "Projekt", shortName = "PRJ", weight = 2.0, colorHex = "#00796B")
    )

    val gradesJan = listOf(
        // Język polski
        Grade(id = "g1", grade = "5", subjectId = "1", subjectName = "Język polski", categoryId = "1", categoryName = "Sprawdzian", weight = 3.0, colorHex = "#D32F2F", semester = 1, date = "2026-10-10", teacherName = "mgr Anna Nowak", comment = "Lektura 'Dziady cz. III'"),
        Grade(id = "g2", grade = "4+", subjectId = "1", subjectName = "Język polski", categoryId = "2", categoryName = "Kartkówka", weight = 2.0, colorHex = "#F57C00", semester = 1, date = "2026-10-18", teacherName = "mgr Anna Nowak", comment = "Środki stylistyczne"),
        Grade(id = "g3", grade = "5", subjectId = "1", subjectName = "Język polski", categoryId = "5", categoryName = "Aktywność", weight = 1.0, colorHex = "#1976D2", semester = 1, date = "2026-10-24", teacherName = "mgr Anna Nowak"),

        // Matematyka
        Grade(id = "g4", grade = "4", subjectId = "2", subjectName = "Matematyka", categoryId = "1", categoryName = "Sprawdzian", weight = 3.0, colorHex = "#D32F2F", semester = 1, date = "2026-09-29", teacherName = "dr Piotr Wiśniewski", comment = "Funkcja kwadratowa"),
        Grade(id = "g5", grade = "5-", subjectId = "2", subjectName = "Matematyka", categoryId = "2", categoryName = "Kartkówka", weight = 2.0, colorHex = "#F57C00", semester = 1, date = "2026-10-12", teacherName = "dr Piotr Wiśniewski", comment = "Równania i nierówności"),
        Grade(id = "g6", grade = "5", subjectId = "2", subjectName = "Matematyka", categoryId = "6", categoryName = "Projekt", weight = 2.0, colorHex = "#00796B", semester = 1, date = "2026-10-20", teacherName = "dr Piotr Wiśniewski", comment = "Zastosowania geometrii"),

        // Język angielski
        Grade(id = "g7", grade = "6", subjectId = "3", subjectName = "Język angielski", categoryId = "1", categoryName = "Sprawdzian", weight = 3.0, colorHex = "#D32F2F", semester = 1, date = "2026-10-05", teacherName = "mgr John Smith", comment = "Unit 2: Conditionals & Future"),
        Grade(id = "g8", grade = "5+", subjectId = "3", subjectName = "Język angielski", categoryId = "3", categoryName = "Odpowiedź ustna", weight = 2.0, colorHex = "#7B1FA2", semester = 1, date = "2026-10-17", teacherName = "mgr John Smith", comment = "Płynna wypowiedź na temat technologii"),

        // Informatyka
        Grade(id = "g9", grade = "6", subjectId = "4", subjectName = "Informatyka", categoryId = "6", categoryName = "Projekt", weight = 3.0, colorHex = "#00796B", semester = 1, date = "2026-10-15", teacherName = "mgr Tomasz Lis", comment = "Programowanie w Kotlin"),
        Grade(id = "g10", grade = "5", subjectId = "4", subjectName = "Informatyka", categoryId = "2", categoryName = "Kartkówka", weight = 2.0, colorHex = "#F57C00", semester = 1, date = "2026-10-22", teacherName = "mgr Tomasz Lis", comment = "Struktury danych"),

        // Fizyka
        Grade(id = "g11", grade = "4-", subjectId = "5", subjectName = "Fizyka", categoryId = "1", categoryName = "Sprawdzian", weight = 3.0, colorHex = "#D32F2F", semester = 1, date = "2026-10-08", teacherName = "mgr Krzysztof Zając", comment = "Dynamika Newtona"),
        Grade(id = "g12", grade = "5", subjectId = "5", subjectName = "Fizyka", categoryId = "4", categoryName = "Praca domowa", weight = 1.0, colorHex = "#388E3C", semester = 1, date = "2026-10-19", teacherName = "mgr Krzysztof Zając"),

        // Historia
        Grade(id = "g13", grade = "5", subjectId = "6", subjectName = "Historia", categoryId = "1", categoryName = "Sprawdzian", weight = 3.0, colorHex = "#D32F2F", semester = 1, date = "2026-10-11", teacherName = "dr Barbara Kamińska", comment = "I Wojna Światowa"),
        Grade(id = "g14", grade = "4+", subjectId = "6", subjectName = "Historia", categoryId = "3", categoryName = "Odpowiedź ustna", weight = 2.0, colorHex = "#7B1FA2", semester = 1, date = "2026-10-25", teacherName = "dr Barbara Kamińska"),

        // Biologia
        Grade(id = "g15", grade = "5-", subjectId = "7", subjectName = "Biologia", categoryId = "1", categoryName = "Sprawdzian", weight = 3.0, colorHex = "#D32F2F", semester = 1, date = "2026-10-14", teacherName = "mgr Joanna Dąbrowska", comment = "Genetyka molekularna"),

        // Chemia
        Grade(id = "g16", grade = "4", subjectId = "8", subjectName = "Chemia", categoryId = "1", categoryName = "Sprawdzian", weight = 3.0, colorHex = "#D32F2F", semester = 1, date = "2026-10-16", teacherName = "mgr Marek Wójcik", comment = "Reakcje utleniania i redukcji"),

        // Geografia
        Grade(id = "g17", grade = "5", subjectId = "9", subjectName = "Geografia", categoryId = "1", categoryName = "Sprawdzian", weight = 3.0, colorHex = "#D32F2F", semester = 1, date = "2026-10-09", teacherName = "mgr Ewa Kozłowska", comment = "Klimaty kuli ziemskiej"),

        // WF
        Grade(id = "g18", grade = "6", subjectId = "10", subjectName = "Wychowanie fizyczne", categoryId = "5", categoryName = "Aktywność", weight = 1.0, colorHex = "#1976D2", semester = 1, date = "2026-10-21", teacherName = "mgr Robert Lewandowski", comment = "Test sprawnościowy")
    )

    val timetableWeek = listOf(
        TimetableDay(
            date = "2026-10-26",
            dayOfWeekName = "Poniedziałek",
            lessons = listOf(
                TimetableSlot(id = "t1", lessonNo = 1, date = "2026-10-26", subjectName = "Język polski", teacherName = "mgr Anna Nowak", classroom = "s. 204", hourFrom = "08:00", hourTo = "08:45"),
                TimetableSlot(id = "t2", lessonNo = 2, date = "2026-10-26", subjectName = "Matematyka", teacherName = "dr Piotr Wiśniewski", classroom = "s. 112", hourFrom = "08:55", hourTo = "09:40"),
                TimetableSlot(id = "t3", lessonNo = 3, date = "2026-10-26", subjectName = "Informatyka", teacherName = "mgr Tomasz Lis", classroom = "prac. kom. 1", hourFrom = "09:50", hourTo = "10:35"),
                TimetableSlot(id = "t4", lessonNo = 4, date = "2026-10-26", subjectName = "Język angielski", teacherName = "mgr John Smith", classroom = "s. 108", hourFrom = "10:45", hourTo = "11:30"),
                TimetableSlot(id = "t5", lessonNo = 5, date = "2026-10-26", subjectName = "Historia", teacherName = "dr Barbara Kamińska", classroom = "s. 215", hourFrom = "11:50", hourTo = "12:35"),
                TimetableSlot(id = "t6", lessonNo = 6, date = "2026-10-26", subjectName = "Wychowanie fizyczne", teacherName = "mgr Robert Lewandowski", classroom = "Hala sportowa", hourFrom = "12:45", hourTo = "13:30")
            )
        ),
        TimetableDay(
            date = "2026-10-27",
            dayOfWeekName = "Wtorek",
            lessons = listOf(
                TimetableSlot(id = "t7", lessonNo = 1, date = "2026-10-27", subjectName = "Biologia", teacherName = "mgr Joanna Dąbrowska", classroom = "s. 301", hourFrom = "08:00", hourTo = "08:45"),
                TimetableSlot(id = "t8", lessonNo = 2, date = "2026-10-27", subjectName = "Chemia", teacherName = "mgr Marek Wójcik", classroom = "prac. chem.", hourFrom = "08:55", hourTo = "09:40"),
                TimetableSlot(id = "t9", lessonNo = 3, date = "2026-10-27", subjectName = "Matematyka", teacherName = "dr Piotr Wiśniewski", classroom = "s. 112", hourFrom = "09:50", hourTo = "10:35"),
                TimetableSlot(
                    id = "t10", lessonNo = 4, date = "2026-10-27",
                    subjectName = "Informatyka", teacherName = "mgr Tomasz Lis", classroom = "prac. kom. 2",
                    hourFrom = "10:45", hourTo = "11:30",
                    isSubstitution = true,
                    orgSubjectName = "Fizyka", orgTeacherName = "mgr Krzysztof Zając",
                    substitutionNote = "Zastępstwo za nieobecnego p. Zająca"
                ),
                TimetableSlot(id = "t11", lessonNo = 5, date = "2026-10-27", subjectName = "Język angielski", teacherName = "mgr John Smith", classroom = "s. 108", hourFrom = "11:50", hourTo = "12:35")
            )
        ),
        TimetableDay(
            date = "2026-10-28",
            dayOfWeekName = "Środa",
            lessons = listOf(
                TimetableSlot(id = "t12", lessonNo = 1, date = "2026-10-28", subjectName = "Geografia", teacherName = "mgr Ewa Kozłowska", classroom = "s. 209", hourFrom = "08:00", hourTo = "08:45"),
                TimetableSlot(id = "t13", lessonNo = 2, date = "2026-10-28", subjectName = "Język polski", teacherName = "mgr Anna Nowak", classroom = "s. 204", hourFrom = "08:55", hourTo = "09:40"),
                TimetableSlot(id = "t14", lessonNo = 3, date = "2026-10-28", subjectName = "Fizyka", teacherName = "mgr Krzysztof Zając", classroom = "s. 115", hourFrom = "09:50", hourTo = "10:35"),
                TimetableSlot(id = "t15", lessonNo = 4, date = "2026-10-28", subjectName = "Historia", teacherName = "dr Barbara Kamińska", classroom = "s. 215", hourFrom = "10:45", hourTo = "11:30"),
                TimetableSlot(
                    id = "t16", lessonNo = 5, date = "2026-10-28",
                    subjectName = "Godzina wychowawcza", teacherName = "mgr Anna Nowak", classroom = "s. 204",
                    hourFrom = "11:50", hourTo = "12:35",
                    isCancelled = true
                )
            )
        ),
        TimetableDay(
            date = "2026-10-29",
            dayOfWeekName = "Czwartek (Dzisiaj)",
            lessons = listOf(
                TimetableSlot(id = "t17", lessonNo = 1, date = "2026-10-29", subjectName = "Matematyka", teacherName = "dr Piotr Wiśniewski", classroom = "s. 112", hourFrom = "08:00", hourTo = "08:45"),
                TimetableSlot(id = "t18", lessonNo = 2, date = "2026-10-29", subjectName = "Język angielski", teacherName = "mgr John Smith", classroom = "s. 108", hourFrom = "08:55", hourTo = "09:40"),
                TimetableSlot(id = "t19", lessonNo = 3, date = "2026-10-29", subjectName = "Język polski", teacherName = "mgr Anna Nowak", classroom = "s. 204", hourFrom = "09:50", hourTo = "10:35"),
                TimetableSlot(id = "t20", lessonNo = 4, date = "2026-10-29", subjectName = "Chemia", teacherName = "mgr Marek Wójcik", classroom = "prac. chem.", hourFrom = "10:45", hourTo = "11:30"),
                TimetableSlot(id = "t21", lessonNo = 5, date = "2026-10-29", subjectName = "Wychowanie fizyczne", teacherName = "mgr Robert Lewandowski", classroom = "Hala sportowa", hourFrom = "11:50", hourTo = "12:35")
            )
        ),
        TimetableDay(
            date = "2026-10-30",
            dayOfWeekName = "Piątek",
            lessons = listOf(
                TimetableSlot(id = "t22", lessonNo = 1, date = "2026-10-30", subjectName = "Informatyka", teacherName = "mgr Tomasz Lis", classroom = "prac. kom. 1", hourFrom = "08:00", hourTo = "08:45"),
                TimetableSlot(id = "t23", lessonNo = 2, date = "2026-10-30", subjectName = "Biologia", teacherName = "mgr Joanna Dąbrowska", classroom = "s. 301", hourFrom = "08:55", hourTo = "09:40"),
                TimetableSlot(id = "t24", lessonNo = 3, date = "2026-10-30", subjectName = "Geografia", teacherName = "mgr Ewa Kozłowska", classroom = "s. 209", hourFrom = "09:50", hourTo = "10:35"),
                TimetableSlot(id = "t25", lessonNo = 4, date = "2026-10-30", subjectName = "Język polski", teacherName = "mgr Anna Nowak", classroom = "s. 204", hourFrom = "10:45", hourTo = "11:30")
            )
        )
    )

    val attendances = listOf(
        Attendance(id = "a1", lessonNo = 1, date = "2026-10-28", subjectName = "Geografia", type = AttendanceType.PRESENCE),
        Attendance(id = "a2", lessonNo = 2, date = "2026-10-28", subjectName = "Język polski", type = AttendanceType.PRESENCE),
        Attendance(id = "a3", lessonNo = 3, date = "2026-10-28", subjectName = "Fizyka", type = AttendanceType.LATE),
        Attendance(id = "a4", lessonNo = 4, date = "2026-10-28", subjectName = "Historia", type = AttendanceType.PRESENCE),

        Attendance(id = "a5", lessonNo = 1, date = "2026-10-27", subjectName = "Biologia", type = AttendanceType.PRESENCE),
        Attendance(id = "a6", lessonNo = 2, date = "2026-10-27", subjectName = "Chemia", type = AttendanceType.PRESENCE),
        Attendance(id = "a7", lessonNo = 3, date = "2026-10-27", subjectName = "Matematyka", type = AttendanceType.PRESENCE),
        Attendance(id = "a8", lessonNo = 4, date = "2026-10-27", subjectName = "Informatyka", type = AttendanceType.PRESENCE),
        Attendance(id = "a9", lessonNo = 5, date = "2026-10-27", subjectName = "Język angielski", type = AttendanceType.PRESENCE),

        Attendance(id = "a10", lessonNo = 1, date = "2026-10-26", subjectName = "Język polski", type = AttendanceType.PRESENCE),
        Attendance(id = "a11", lessonNo = 2, date = "2026-10-26", subjectName = "Matematyka", type = AttendanceType.PRESENCE),
        Attendance(id = "a12", lessonNo = 3, date = "2026-10-26", subjectName = "Informatyka", type = AttendanceType.EXCUSED),
        Attendance(id = "a13", lessonNo = 4, date = "2026-10-26", subjectName = "Język angielski", type = AttendanceType.EXCUSED),

        Attendance(id = "a14", lessonNo = 1, date = "2026-10-23", subjectName = "Matematyka", type = AttendanceType.PRESENCE),
        Attendance(id = "a15", lessonNo = 2, date = "2026-10-23", subjectName = "Fizyka", type = AttendanceType.PRESENCE),
        Attendance(id = "a16", lessonNo = 3, date = "2026-10-23", subjectName = "Chemia", type = AttendanceType.UNEXCUSED)
    )

    val notices = listOf(
        SchoolNotice(
            id = "n1",
            subject = "Zebranie z rodzicami oraz dzień otwarty",
            content = "Szanowni Państwo, w najbliższy czwartek 5 listopada o godz. 17:30 odbędą się zebrania z wychowawcami klas oraz konsultacje indywidualne z nauczycielami wszystkich przedmiotów w salach lekcyjnych.",
            author = "Dyrektor Szkoły - dr Janusz Wiśniewski",
            date = "2026-10-28 14:10",
            isRead = false
        ),
        SchoolNotice(
            id = "n2",
            subject = "Olimpiada Informatyczna Juniorów - etap szkolny",
            content = "Informujemy, że w piątek 6 listopada w pracowni komputerowej nr 1 odbędą się eliminacje szkolne do Olimpiady Informatycznej. Uczniowie proszeni są o zgłaszanie się do nauczyciela informatyki do środy.",
            author = "mgr Tomasz Lis",
            date = "2026-10-25 10:30",
            isRead = true
        ),
        SchoolNotice(
            id = "n3",
            subject = "Dzień Edukacji Narodowej - plan uroczystości",
            content = "W związku z obchodami Dnia Edukacji Narodowej apel odbędzie się na auli szkolnej po 3. godzinie lekcyjnej. Obowiązuje strój galowy.",
            author = "Samorząd Uczniowski",
            date = "2026-10-12 09:00",
            isRead = true
        )
    )
}
