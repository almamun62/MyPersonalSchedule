import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target_enum = """    CHAT("AI Chat"),
    USAGE("Usage"),"""
replacement_enum = """    CHAT("AI Chat"),
    NOTES("Notes"),
    USAGE("Usage"),"""
content = content.replace(target_enum, replacement_enum)

target_nav_item = """                    NavigationBarItem(
                        selected = currentScreen == ScheduleScreen.USAGE,"""
replacement_nav_item = """                    NavigationBarItem(
                        selected = currentScreen == ScheduleScreen.NOTES,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(ScheduleScreen.NOTES.ordinal) } },
                        icon = {
                            Icon(
                                if (currentScreen == ScheduleScreen.NOTES) Icons.Default.MenuBook else Icons.Outlined.MenuBook,
                                contentDescription = "Notes"
                            )
                        },
                        label = { Text(if (appLanguage == "zh") "笔记" else "Notes") },
                        modifier = Modifier.testTag("nav_notes")
                    )
                    NavigationBarItem(
                        selected = currentScreen == ScheduleScreen.USAGE,"""
content = content.replace(target_nav_item, replacement_nav_item)

target_pager = """                        ScheduleScreen.USAGE.ordinal -> {
                            UsageScreen(state = state, viewModel = viewModel)
                        }"""
replacement_pager = """                        ScheduleScreen.NOTES.ordinal -> {
                            com.example.ui.screens.NotesScreen(state = state, viewModel = viewModel)
                        }
                        ScheduleScreen.USAGE.ordinal -> {
                            UsageScreen(state = state, viewModel = viewModel)
                        }"""
content = content.replace(target_pager, replacement_pager)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
print("Updated MainActivity.kt navigation")
