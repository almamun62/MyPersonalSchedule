package com.example.domain

object CalculatorEngine {
    data class GpaResult(
        val totalCredits: Double,
        val totalGradePoints: Double,
        val gpa: Double
    )

    fun calculateGpa(coursesWithGrades: List<Pair<Double, Double>>): GpaResult {
        var totalCredits = 0.0
        var totalPoints = 0.0

        coursesWithGrades.forEach { (credits, gradePoint) ->
            if (credits > 0 && gradePoint >= 0) {
                totalCredits += credits
                totalPoints += credits * gradePoint
            }
        }

        val gpa = if (totalCredits > 0) totalPoints / totalCredits else 0.0
        return GpaResult(totalCredits, totalPoints, gpa)
    }
}
