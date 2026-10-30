package pl.librus.mobile.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Subject(
    val id: String,
    val name: String,
    val shortName: String = "",
    val order: Int = 0
)

@Serializable
data class GradeCategory(
    val id: String,
    val name: String,
    val shortName: String = "",
    val weight: Double = 1.0,
    val colorHex: String = "#7E57C2",
    val countToAverage: Boolean = true
)

@Serializable
data class GradeComment(
    val id: String,
    val text: String,
    val gradeId: String = ""
)

@Serializable
data class Grade(
    val id: String,
    val grade: String,
    val subjectId: String,
    val subjectName: String,
    val categoryId: String? = null,
    val categoryName: String = "",
    val weight: Double = 1.0,
    val colorHex: String = "#7E57C2",
    val countToAverage: Boolean = true,
    val semester: Int = 1,
    val date: String = "",
    val addDate: String = "",
    val teacherName: String = "",
    val comment: String? = null,
    val isConstituent: Boolean = true,
    val isSemesterProposal: Boolean = false,
    val isSemester: Boolean = false,
    val isFinalProposal: Boolean = false,
    val isFinal: Boolean = false
) {
    val numericValue: Double?
        get() = parseGradeValue(grade)

    val effectiveColorHex: String
        get() = getGradeColorHex(grade, colorHex)

    companion object {
        fun getGradeColorHex(rawGrade: String, fallbackHex: String = "#7E57C2"): String {
            val trimmed = rawGrade.trim()
            if (trimmed.isEmpty()) return fallbackHex

            // If it's a standard school grade with optional plus/minus (e.g. "6", "5+", "4-")
            if (trimmed.matches(Regex("""^[+-]?[1-6][+-]?$"""))) {
                val digit = trimmed.firstOrNull { it.isDigit() }
                return when (digit) {
                    '6' -> "#1E88E5" // Blue (6)
                    '5' -> "#2E7D32" // Green (5)
                    '4' -> "#689F38" // Light Green (4)
                    '3' -> "#F57F17" // Yellow (3)
                    '2' -> "#FB8C00" // Orange (2)
                    '1' -> "#D32F2F" // Red (1)
                    else -> fallbackHex
                }
            }

            // Decimal numbers (e.g. calculated averages like 4.75, 3.25)
            val numeric = parseGradeValue(trimmed)
            if (numeric != null) {
                return when {
                    numeric >= 5.5 -> "#1E88E5" // Blue (6)
                    numeric >= 4.5 -> "#2E7D32" // Green (5)
                    numeric >= 3.5 -> "#689F38" // Light Green (4)
                    numeric >= 2.5 -> "#F57F17" // Yellow (3)
                    numeric >= 1.5 -> "#FB8C00" // Orange (2)
                    else -> "#D32F2F"           // Red (1)
                }
            }

            return when {
                trimmed == "+" -> "#2E7D32"
                trimmed == "-" -> "#D32F2F"
                trimmed.equals("np", ignoreCase = true) || trimmed.equals("bz", ignoreCase = true) -> "#757575"
                else -> fallbackHex
            }
        }

        fun parseGradeValue(raw: String): Double? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null

            // Pluses and minuses
            return when {
                trimmed == "6" -> 6.0
                trimmed == "6-" || trimmed == "-6" -> 5.75
                trimmed == "5+" || trimmed == "+5" -> 5.5
                trimmed == "5" -> 5.0
                trimmed == "5-" || trimmed == "-5" -> 4.75
                trimmed == "4+" || trimmed == "+4" -> 4.5
                trimmed == "4" -> 4.0
                trimmed == "4-" || trimmed == "-4" -> 3.75
                trimmed == "3+" || trimmed == "+3" -> 3.5
                trimmed == "3" -> 3.0
                trimmed == "3-" || trimmed == "-3" -> 2.75
                trimmed == "2+" || trimmed == "+2" -> 2.5
                trimmed == "2" -> 2.0
                trimmed == "2-" || trimmed == "-2" -> 1.75
                trimmed == "1+" || trimmed == "+1" -> 1.5
                trimmed == "1" -> 1.0
                trimmed.matches(Regex("""^\d+(\.\d+)?$""")) -> trimmed.toDoubleOrNull()
                else -> null
            }
        }
    }
}

@Serializable
data class SubjectGradeGroup(
    val subject: Subject,
    val semester1Grades: List<Grade> = emptyList(),
    val semester2Grades: List<Grade> = emptyList(),
    val semester1Proposal: Grade? = null,
    val semester1Final: Grade? = null,
    val semester2Proposal: Grade? = null,
    val finalGrade: Grade? = null,
    val semester1Average: Double? = null,
    val semester2Average: Double? = null,
    val yearAverage: Double? = null
) {
    val allGrades: List<Grade>
        get() = semester1Grades + semester2Grades + listOfNotNull(semester1Proposal, semester1Final, semester2Proposal, finalGrade)
}
