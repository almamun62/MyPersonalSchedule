import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

topbar_target = """                TopAppBar(
                    title = {
                        Text(
                            text = "MySchedule",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                    )
                )"""

topbar_replacement = """                TopAppBar(
                    title = {
                        Text(
                            text = "MySchedule",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { coroutineScope.launch { pagerState.animateScrollToPage(ScheduleScreen.SETTINGS.ordinal) } }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                    )
                )"""

content = content.replace(topbar_target, topbar_replacement)

nav_target = """                    NavigationBarItem(
                        selected = currentScreen == ScheduleScreen.SETTINGS,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(ScheduleScreen.SETTINGS.ordinal) } },
                        icon = {
                            Icon(
                                if (currentScreen == ScheduleScreen.SETTINGS) Icons.Default.Tune else Icons.Outlined.Tune,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text(if (appLanguage == "zh") "设置" else "Settings") },
                        modifier = Modifier.testTag("nav_settings")
                    )"""

content = content.replace(nav_target, "")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
print("Patched MainActivity")
