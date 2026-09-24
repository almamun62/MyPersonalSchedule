import re

with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'r') as f:
    content = f.read()

target_add = """            AddCourseDialog(
                semesterId = state.activeSemester.id,
                onDismiss = { showAddCourseDialog = false },
                onConfirm = { newCourse ->
                    viewModel.addCourse(newCourse)
                    showAddCourseDialog = false
                }
            )"""

replacement_add = """            AddCourseDialog(
                semesterId = state.activeSemester.id,
                onDismiss = { showAddCourseDialog = false },
                onConfirm = { newCourse ->
                    viewModel.addCourse(newCourse, onSuccess = {
                        showAddCourseDialog = false
                    }, onConflict = { msg ->
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                    })
                }
            )"""

target_edit = """            EditCourseDialog(
                course = it,
                onDismiss = { courseToEdit = null },
                onConfirm = { updatedCourse ->
                    viewModel.updateCourse(updatedCourse)
                    courseToEdit = null
                }
            )"""

replacement_edit = """            EditCourseDialog(
                course = it,
                onDismiss = { courseToEdit = null },
                onConfirm = { updatedCourse ->
                    viewModel.updateCourse(updatedCourse, onSuccess = {
                        courseToEdit = null
                    }, onConflict = { msg ->
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                    })
                }
            )"""

if target_add in content:
    content = content.replace(target_add, replacement_add)
    print("Replaced AddCourseDialog")
if target_edit in content:
    content = content.replace(target_edit, replacement_edit)
    print("Replaced EditCourseDialog")

with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'w') as f:
    f.write(content)
