with open('app/src/main/java/com/example/ui/theme/Translation.kt', 'r') as f:
    content = f.read()

# Replace the broken part
broken_marker = '    "classes scheduled this week" to "节课安排在本周")'
parts = content.split(broken_marker)
if len(parts) >= 2:
    part1 = parts[0]
    
    fixed_end = """    "classes scheduled this week" to "节课安排在本周",
    "Take a break and relax!" to "休息一下，放松心情！",
    "No classes today" to "今天没有课",
    "Advance Class Reminder" to "提前上课提醒",
    "Image" to "图片",
    "🛡️ Holiday Auto-Pause" to "🛡️ 节假日自动暂停",
    "Paste schedule text e.g.:\\n1619304040-计算机组成原理(国际学生)\\n1-10周,星期1,第3节-第5节明理楼B105\\n\\nOr CSV lines." to "粘贴课表文本例如：\\n1619304040-计算机组成原理(国际学生)\\n1-10周,星期1,第3节-第5节明理楼B105\\n\\n或者使用CSV格式。",
    "Class" to "课堂",
    "A scheduling conflict was detected between ${currentConflict.course1.name} and ${currentConflict.course2.name}. Please review your timetable." to "检测到 ${currentConflict.course1.name} 和 ${currentConflict.course2.name} 之间的课程冲突。请检查您的课表。",
    "Auto-saving locally" to "自动保存在本地",
    "No notes yet." to "暂无笔记。",
    "⏰ 15m Lead Time" to "⏰ 提前15分钟",
    "单双周 Parity" to "单双周 Parity",
    "调休 Make-up" to "调休 Make-up",
    "Reminder Time (Minutes Before)" to "提醒时间 (提前分钟数)",
    "sk-..." to "sk-...",
    "📍 Location Included" to "📍 包含地点",
    "Scheduling Conflict" to "课程冲突",
    "OK" to "确定",
    "Class in Progress: $ongoingCourseName" to "正在上课: $ongoingCourseName"
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
        f.write(part1 + fixed_end)
    print("Fixed Translation.kt")
else:
    print("Broken marker not found")

# Fix asAndroidPath
with open('app/src/main/java/com/example/ui/components/DrawingCanvasDialog.kt', 'r') as f:
    drawing_content = f.read()
drawing_content = drawing_content.replace('import androidx.compose.ui.graphics.StrokeJoin', 'import androidx.compose.ui.graphics.StrokeJoin\nimport androidx.compose.ui.graphics.asAndroidPath')
with open('app/src/main/java/com/example/ui/components/DrawingCanvasDialog.kt', 'w') as f:
    f.write(drawing_content)
print("Fixed DrawingCanvasDialog.kt")
