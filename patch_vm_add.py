import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

target1 = """    fun addCourse(course: CourseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val sem = _uiState.value.activeSemester
            if (sem != null) {
                val allCourses = repository.getCoursesBySemesterSync(sem.id)
                val newList = allCourses + course
                val conflicts = com.example.domain.CourseConflictDetector.detectConflicts(newList, sem.totalWeeks)
                val thisConflict = conflicts.find { it.course1.name == course.name || it.course2.name == course.name }
                
                repository.insertCourse(course)
                rescheduleClassRemindersInternal()
            }
        }
    }"""

replacement1 = """    fun addCourse(course: CourseEntity, onSuccess: () -> Unit = {}, onConflict: (String) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val sem = _uiState.value.activeSemester
            if (sem != null) {
                val allCourses = repository.getCoursesBySemesterSync(sem.id)
                val newList = allCourses + course
                val conflicts = com.example.domain.CourseConflictDetector.detectConflicts(newList, sem.totalWeeks)
                // Filter out self-conflicts if id is the same (though this is new course, id is 0)
                val thisConflict = conflicts.find { it.course1.id == course.id && it.course2.id == course.id } ?: conflicts.find { (it.course1.name == course.name && it.course1.dayOfWeek == course.dayOfWeek) || (it.course2.name == course.name && it.course2.dayOfWeek == course.dayOfWeek) }
                // Actually the conflict detector returns conflicts where either course is the new one.
                val actualConflict = conflicts.find { it.course1 === course || it.course2 === course } ?: conflicts.find { it.course1.name == course.name || it.course2.name == course.name }

                if (actualConflict != null) {
                    val otherCourse = if (actualConflict.course1 === course || actualConflict.course1.name == course.name) actualConflict.course2 else actualConflict.course1
                    launch(Dispatchers.Main) {
                        onConflict("Scheduling conflict detected with ${otherCourse.name}. Please choose a different time.")
                    }
                } else {
                    repository.insertCourse(course)
                    rescheduleClassRemindersInternal()
                    launch(Dispatchers.Main) {
                        onSuccess()
                    }
                }
            }
        }
    }"""

target2 = """    fun updateCourse(course: CourseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCourse(course)
            rescheduleClassRemindersInternal()
        }
    }"""

replacement2 = """    fun updateCourse(course: CourseEntity, onSuccess: () -> Unit = {}, onConflict: (String) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val sem = _uiState.value.activeSemester
            if (sem != null) {
                val allCourses = repository.getCoursesBySemesterSync(sem.id).filter { it.id != course.id }
                val newList = allCourses + course
                val conflicts = com.example.domain.CourseConflictDetector.detectConflicts(newList, sem.totalWeeks)
                val actualConflict = conflicts.find { it.course1 === course || it.course2 === course } ?: conflicts.find { it.course1.name == course.name || it.course2.name == course.name }
                
                if (actualConflict != null) {
                    val otherCourse = if (actualConflict.course1 === course || actualConflict.course1.name == course.name) actualConflict.course2 else actualConflict.course1
                    launch(Dispatchers.Main) {
                        onConflict("Scheduling conflict detected with ${otherCourse.name}. Please choose a different time.")
                    }
                } else {
                    repository.updateCourse(course)
                    rescheduleClassRemindersInternal()
                    launch(Dispatchers.Main) {
                        onSuccess()
                    }
                }
            }
        }
    }"""

if target1 in content:
    content = content.replace(target1, replacement1)
    print("Replaced addCourse")
else:
    print("target1 not found")

if target2 in content:
    content = content.replace(target2, replacement2)
    print("Replaced updateCourse")
else:
    print("target2 not found")

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
    f.write(content)
