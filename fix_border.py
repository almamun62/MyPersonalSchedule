import re

with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'r') as f:
    content = f.read()

target = """                                border = BorderStroke(
                                    1.dp,
                                    if (isConflicted) Color(0xFFFF9500).copy(alpha = 0.5f)
                                    else Brush.linearGradient(
                                        listOf(Color(0x55FFFFFF), Color(0x11FFFFFF))
                                    )
                                ),"""

replacement = """                                border = if (isConflicted) {
                                    BorderStroke(1.dp, Color(0xFFFF9500).copy(alpha = 0.5f))
                                } else {
                                    BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0x55FFFFFF), Color(0x11FFFFFF))))
                                },"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'w') as f:
    f.write(content)

