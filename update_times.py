import re

with open('app/src/main/java/com/example/util/ScheduleImportHelper.kt', 'r') as f:
    content = f.read()

new_starts = """    private val STANDARD_START_TIMES = listOf(
        "08:00", "08:50", "09:50", "10:40", "11:30",
        "14:30", "15:20", "16:20", "17:10",
        "19:00", "19:50", "20:40"
    )"""

new_ends = """    private val STANDARD_END_TIMES = listOf(
        "08:45", "09:35", "10:35", "11:25", "12:15",
        "15:15", "16:05", "17:05", "17:55",
        "19:45", "20:35", "21:25"
    )"""

content = re.sub(r'    private val STANDARD_START_TIMES = listOf\([\s\S]*?\)', new_starts, content)
content = re.sub(r'    private val STANDARD_END_TIMES = listOf\([\s\S]*?\)', new_ends, content)

with open('app/src/main/java/com/example/util/ScheduleImportHelper.kt', 'w') as f:
    f.write(content)
