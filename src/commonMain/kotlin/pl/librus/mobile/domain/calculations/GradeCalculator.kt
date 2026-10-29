package pl.librus.mobile.domain.calculations

import pl.librus.mobile.domain.models.Grade
import pl.librus.mobile.domain.models.Subject
import pl.librus.mobile.domain.models.SubjectGradeGroup
import kotlin.math.roundToInt

object GradeCalculator {

    /**
     * Calculates the weighted average of a list of grades.
     * Only grades where countToAverage is true, weight > 0, and numericValue is not null are counted.
     */
    fun calculateWeightedAverage(grades: List<Grade>): Double? {
        val eligible = grades.filter { it.countToAverage && it.weight > 0 && it.numericValue != null }
        if (eligible.isEmpty()) return null

        val totalWeight = eligible.sumOf { it.weight }
        if (totalWeight <= 0) return null

        val weightedSum = eligible.sumOf { it.numericValue!! * it.weight }
        val rawAvg = weightedSum / totalWeight
        return roundToTwoDecimals(rawAvg)
    }

    /**
     * Groups grades by subject and calculates semester 1, semester 2, and overall year averages.
     */
    fun groupGradesBySubject(subjects: List<Subject>, grades: List<Grade>): List<SubjectGradeGroup> {
        val gradesBySubject = grades.groupBy { it.subjectId }

        return subjects.map { subject ->
            val subjectGrades = gradesBySubject[subject.id] ?: emptyList()

            val sem1Constituents = subjectGrades.filter { it.semester == 1 && it.isConstituent }
            val sem2Constituents = subjectGrades.filter { it.semester == 2 && it.isConstituent }

            val sem1Proposal = subjectGrades.find { it.semester == 1 && it.isSemesterProposal }
            val sem1Final = subjectGrades.find { it.semester == 1 && it.isSemester }
            val sem2Proposal = subjectGrades.find { it.semester == 2 && it.isSemesterProposal }
            val finalGrade = subjectGrades.find { it.isFinal }

            val sem1Avg = calculateWeightedAverage(sem1Constituents)
            val sem2Avg = calculateWeightedAverage(sem2Constituents)

            val yearAvg = when {
                sem1Avg != null && sem2Avg != null -> roundToTwoDecimals((sem1Avg + sem2Avg) / 2.0)
                sem1Avg != null -> sem1Avg
                sem2Avg != null -> sem2Avg
                else -> null
            }

            SubjectGradeGroup(
                subject = subject,
                semester1Grades = sem1Constituents,
                semester2Grades = sem2Constituents,
                semester1Proposal = sem1Proposal,
                semester1Final = sem1Final,
                semester2Proposal = sem2Proposal,
                finalGrade = finalGrade,
                semester1Average = sem1Avg,
                semester2Average = sem2Avg,
                yearAverage = yearAvg
            )
        }
    }

    /**
     * Calculates the overall GPA across all subjects that have an average.
     */
    fun calculateOverallGpa(groups: List<SubjectGradeGroup>): Double? {
        val validAverages = groups.mapNotNull { it.yearAverage ?: it.semester1Average ?: it.semester2Average }
        if (validAverages.isEmpty()) return null
        return roundToTwoDecimals(validAverages.average())
    }

    /**
     * Grade Simulator:
     * Predicts what the new average will be after adding hypothetical grades.
     */
    fun simulateNewAverage(
        currentGrades: List<Grade>,
        simulatedGrades: List<Pair<Double, Double>> // List of (gradeValue, weight)
    ): Double? {
        val eligible = currentGrades.filter { it.countToAverage && it.weight > 0 && it.numericValue != null }
        val currentSum = eligible.sumOf { it.numericValue!! * it.weight }
        val currentWeight = eligible.sumOf { it.weight }

        val simSum = simulatedGrades.sumOf { it.first * it.second }
        val simWeight = simulatedGrades.sumOf { it.second }

        val totalWeight = currentWeight + simWeight
        if (totalWeight <= 0) return null

        return roundToTwoDecimals((currentSum + simSum) / totalWeight)
    }

    /**
     * Predicts what grade is needed on an upcoming assignment with [weight] to reach [targetAverage].
     */
    fun calculateRequiredGrade(
        currentGrades: List<Grade>,
        weight: Double,
        targetAverage: Double
    ): Double? {
        val eligible = currentGrades.filter { it.countToAverage && it.weight > 0 && it.numericValue != null }
        val currentSum = eligible.sumOf { it.numericValue!! * it.weight }
        val currentWeight = eligible.sumOf { it.weight }

        // targetAverage = (currentSum + requiredGrade * weight) / (currentWeight + weight)
        // requiredGrade * weight = targetAverage * (currentWeight + weight) - currentSum
        // requiredGrade = (targetAverage * (currentWeight + weight) - currentSum) / weight
        if (weight <= 0) return null
        val required = (targetAverage * (currentWeight + weight) - currentSum) / weight
        return roundToTwoDecimals(required)
    }

    private fun roundToTwoDecimals(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }
}
