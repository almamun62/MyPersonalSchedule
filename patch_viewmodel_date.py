import re
with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

target = """        val semStartDate = if (sem != null) {
            LocalDate.ofEpochDay(sem.startDateMillis / (24 * 60 * 60 * 1000))
        } else {
            today
        }"""

replacement = """        val semStartDate = if (sem != null) {
            var date = LocalDate.ofEpochDay(sem.startDateMillis / (24 * 60 * 60 * 1000))
            if (date == LocalDate.of(2026, 8, 24)) {
                date = LocalDate.of(2026, 8, 31)
                // Auto-migrate in DB if we detect the old date
                viewModelScope.launch(Dispatchers.IO) {
                    repository.insertSemester(sem.copy(startDateMillis = date.toEpochDay() * 24 * 60 * 60 * 1000))
                }
            }
            date
        } else {
            today
        }"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
        f.write(content)
    print("Patched ViewModel date")
else:
    print("Target not found")
