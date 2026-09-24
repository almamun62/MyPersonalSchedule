import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

conflict_code = """    private val _courseConflictAlert = MutableStateFlow<com.example.domain.CourseConflict?>(null)
    val courseConflictAlert = _courseConflictAlert.asStateFlow()

    fun dismissConflictAlert() {
        _courseConflictAlert.value = null
    }

    private val _clockTicker"""

content = content.replace("    private val _clockTicker", conflict_code)

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
    f.write(content)
