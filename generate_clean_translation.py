import re

with open('app/src/main/java/com/example/ui/theme/Translation.kt', 'r') as f:
    content = f.read()

# Extract all valid key-value pairs
pattern = re.compile(r'^\s*"((?:\\.|[^"\\])*)"\s*to\s*"((?:\\.|[^"\\])*)",?$', re.MULTILINE)
matches = pattern.findall(content)

# Use a dict to keep unique and latest
translations = {}
for k, v in matches:
    translations[k] = v

# Add the ones we missed manually in case the regex missed them
extra = {
    "classes scheduled this week": "节课安排在本周",
    "Take a break and relax!": "休息一下，放松心情！",
    "No classes today": "今天没有课",
    "Advance Class Reminder": "提前上课提醒",
    "Image": "图片",
    "🛡️ Holiday Auto-Pause": "🛡️ 节假日自动暂停",
    "Paste schedule text e.g.:\\n1619304040-计算机组成原理(国际学生)\\n1-10周,星期1,第3节-第5节明理楼B105\\n\\nOr CSV lines.": "粘贴课表文本例如：\\n1619304040-计算机组成原理(国际学生)\\n1-10周,星期1,第3节-第5节明理楼B105\\n\\n或者使用CSV格式。",
    "Class": "课堂",
    "A scheduling conflict was detected between ${currentConflict.course1.name} and ${currentConflict.course2.name}. Please review your timetable.": "检测到 ${currentConflict.course1.name} 和 ${currentConflict.course2.name} 之间的课程冲突。请检查您的课表。",
    "Auto-saving locally": "自动保存在本地",
    "No notes yet.": "暂无笔记。",
    "⏰ 15m Lead Time": "⏰ 提前15分钟",
    "单双周 Parity": "单双周 Parity",
    "调休 Make-up": "调休 Make-up",
    "Reminder Time (Minutes Before)": "提醒时间 (提前分钟数)",
    "sk-...": "sk-...",
    "📍 Location Included": "📍 包含地点",
    "Scheduling Conflict": "课程冲突",
    "OK": "确定",
    "Class in Progress: $ongoingCourseName": "正在上课: $ongoingCourseName",
    "Conflict Center": "冲突中心",
    "Editing Note": "编辑笔记",
    "Write something...": "写点什么...",
    "[Handwritten Canvas]": "[手写画布]",
    "Copy & Close": "复制并关闭",
    "Close": "关闭",
    "JSON PAYLOAD": "JSON 数据载荷"
}

for k, v in extra.items():
    translations[k] = v

new_content = """package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppLanguage = staticCompositionLocalOf { "en" }

val UI_TRANSLATIONS = mapOf(
"""

for k, v in sorted(translations.items()):
    new_content += f'    "{k}" to "{v}",\n'

new_content += """)

val String.tr: String
    @Composable
    get() {
        val lang = LocalAppLanguage.current
        if (lang != "zh") return this
        return UI_TRANSLATIONS[this] ?: this
    }
"""

with open('app/src/main/java/com/example/ui/theme/Translation.kt', 'w') as f:
    f.write(new_content)
print("Cleaned up Translation.kt")
