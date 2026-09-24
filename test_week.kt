import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit

fun main() {
    val semesterStartDate = LocalDate.of(2026, 8, 31)
    val targetDate = LocalDate.of(2026, 9, 16) // or now
    val semMonday = semesterStartDate.minusDays((semesterStartDate.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    val daysBetween = ChronoUnit.DAYS.between(semMonday, targetDate)
    
    println("semMonday: $semMonday")
    println("targetDate: $targetDate")
    println("daysBetween: $daysBetween")
    println("week: " + ((daysBetween / 7).toInt() + 1))
}
