import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

conflict_state = """    private val _courseConflictAlert = MutableStateFlow<com.example.domain.CourseConflict?>(null)
    val courseConflictAlert = _courseConflictAlert.asStateFlow()

    fun dismissConflictAlert() {
        _courseConflictAlert.value = null
    }

    private val _clockTicker = MutableStateFlow(0L)"""

content = content.replace("    private val _clockTicker = MutableStateFlow(0L)", conflict_state)

add_course_replacement = """    fun addCourse(course: CourseEntity) {
        viewModelScope.launch {
            val sem = _uiState.value.activeSemester
            if (sem != null) {
                val allCourses = repository.getCoursesBySemesterSync(sem.id)
                val newList = allCourses + course
                val conflicts = com.example.domain.CourseConflictDetector.detectConflicts(newList, sem.totalWeeks)
                val thisConflict = conflicts.find { it.course1.name == course.name || it.course2.name == course.name }
                
                repository.insertCourse(course)
                rescheduleClassRemindersInternal()
                
                if (thisConflict != null) {
                    _courseConflictAlert.value = thisConflict
                }
            } else {
                repository.insertCourse(course)
                rescheduleClassRemindersInternal()
            }
        }
    }"""
content = re.sub(r'    fun addCourse\(course: CourseEntity\) \{\n        viewModelScope\.launch \{\n            repository\.insertCourse\(course\)\n            rescheduleClassRemindersInternal\(\)\n        \}\n    \}', add_course_replacement, content)

update_course_replacement = """    fun updateCourse(course: CourseEntity) {
        viewModelScope.launch {
            val sem = _uiState.value.activeSemester
            if (sem != null) {
                val allCourses = repository.getCoursesBySemesterSync(sem.id)
                val newList = allCourses.map { if (it.id == course.id) course else it }
                val conflicts = com.example.domain.CourseConflictDetector.detectConflicts(newList, sem.totalWeeks)
                val thisConflict = conflicts.find { it.course1.id == course.id || it.course2.id == course.id }

                repository.updateCourse(course)
                rescheduleClassRemindersInternal()
                
                if (thisConflict != null) {
                    _courseConflictAlert.value = thisConflict
                }
            } else {
                repository.updateCourse(course)
                rescheduleClassRemindersInternal()
            }
        }
    }"""
content = re.sub(r'    fun updateCourse\(course: CourseEntity\) \{\n        viewModelScope\.launch \{\n            repository\.updateCourse\(course\)\n            rescheduleClassRemindersInternal\(\)\n        \}\n    \}', update_course_replacement, content)

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
    f.write(content)

