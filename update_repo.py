import re

with open('app/src/main/java/com/example/data/repository/ScheduleRepository.kt', 'r') as f:
    content = f.read()

replacement = """    suspend fun populateInitialDataIfEmpty() {
        val existing = database.semesterDao().getActiveSemesterSync()
        if (existing == null) {
            // Fall 2026 Academic Term (Starts Aug 31, 2026)
            val startDate = java.time.LocalDate.of(2026, 8, 31)
            val startMillis = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

            val semId = database.semesterDao().insertSemester(
                com.example.data.model.SemesterEntity(
                    name = "2026-2027 Fall Semester",
                    startDateMillis = startMillis,
                    totalWeeks = 20,
                    isActive = true
                )
            )"""

content = re.sub(r'    suspend fun populateInitialDataIfEmpty\(\) \{[\s\S]*?isActive = true\n                \)\n            \)', replacement, content)

with open('app/src/main/java/com/example/data/repository/ScheduleRepository.kt', 'w') as f:
    f.write(content)
