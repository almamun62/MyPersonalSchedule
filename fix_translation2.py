with open('app/src/main/java/com/example/ui/theme/Translation.kt', 'r') as f:
    content = f.read()

import re
# Regex match from "classes scheduled this week" to the end of the file.
content = re.sub(r'"classes scheduled this week" to "节课安排在本周"\).*', '', content, flags=re.DOTALL)

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
    "Class in Progress: $ongoingCourseName" to "正在上课: $ongoingCourseName",
    "Conflict Center" to "冲突中心",
    "Editing Note" to "编辑笔记",
    "Write something..." to "写点什么...",
    "[Handwritten Canvas]" to "[手写画布]",
    "Copy & Close" to "复制并关闭",
    "Close" to "关闭",
    "JSON PAYLOAD" to "JSON 数据载荷"
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
    f.write(content + fixed_end)
print("Fixed Translation.kt")
