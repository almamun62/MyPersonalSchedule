import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = """        com.example.ui.components.SmartPill(
            isDndActive = state.isDndActive,
            currentWeek = state.currentAcademicWeek,
            modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter)
        )"""

replacement = """        val ongoingCourse = state.todayCoursesWithStatus.find { it.status == com.example.domain.CourseClassStatus.ACTIVE }?.course?.name

        androidx.compose.animation.AnimatedVisibility(
            visible = state.isDndActive || ongoingCourse != null,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(),
            modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter)
        ) {
            com.example.ui.components.SmartPill(
                isDndActive = state.isDndActive,
                currentWeek = state.currentAcademicWeek,
                ongoingCourseName = ongoingCourse
            )
        }"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Target not found")
