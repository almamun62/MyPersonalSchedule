import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

target = """            repository.populateInitialDataIfEmpty()
            val sem = repository.activeSemester.firstOrNull()
            if (sem != null) {
                repository.deduplicateCourses(sem.id)
            }"""

replacement = """            repository.populateInitialDataIfEmpty()
            val sem = repository.activeSemester.firstOrNull()
            if (sem != null) {
                // Fix for incorrect initial week count: update August 24 to August 31
                val aug24 = java.time.LocalDate.of(2026, 8, 24).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                if (sem.startDateMillis == aug24) {
                    val aug31 = java.time.LocalDate.of(2026, 8, 31).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    repository.updateSemester(sem.copy(startDateMillis = aug31))
                }
                repository.deduplicateCourses(sem.id)
            }"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Not found, using regex")
    pattern = r"            repository\.populateInitialDataIfEmpty\(\)\n            val sem = repository\.activeSemester\.firstOrNull\(\)\n            if \(sem != null\) {\n                repository\.deduplicateCourses\(sem\.id\)\n            }"
    content = re.sub(pattern, replacement, content)
    with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
        f.write(content)
    print("Regex fallback")
