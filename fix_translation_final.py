import re

with open('app/src/main/java/com/example/ui/theme/Translation.kt', 'r') as f:
    content = f.read()

# I will replace the end of the file correctly.
marker = '"中文" to "中文"'

# first, split by marker
parts = content.split(marker)
if len(parts) >= 2:
    part1 = parts[0]
    
    ending = """    "中文" to "中文",
    "classes scheduled this week" to "节课安排在本周"
)

val String.tr: String
    @Composable
    get() {
        val lang = LocalAppLanguage.current
        if (lang != "zh") return this
        return UI_TRANSLATIONS[this] ?: this
    }
"""
    with open('app/src/main/java/com/example/ui/theme/Translation.kt', 'w') as f:
        f.write(part1 + ending)
    print("Fixed Translation.kt ending")
else:
    print("Marker not found")
