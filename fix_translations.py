import re

additions = {
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
    "Class in Progress: $ongoingCourseName": "正在上课: $ongoingCourseName"
}

with open('app/src/main/java/com/example/ui/theme/Translation.kt', 'r') as f:
    content = f.read()

new_lines = []
for k, v in additions.items():
    if f'"{k}"' not in content:
        new_lines.append(f'    "{k}" to "{v}",')

if new_lines:
    parts = content.rsplit(')', 1)
    if len(parts) == 2:
        new_content = parts[0]
        if not new_content.endswith(','):
            if new_content.endswith('\\n'):
                new_content = new_content[:-1] + ',\\n'
            else:
                new_content += ','
        new_content += '\\n' + '\\n'.join(new_lines) + '\\n)' + parts[1]
        with open('app/src/main/java/com/example/ui/theme/Translation.kt', 'w') as f:
            f.write(new_content)
        print("Added more translations")
