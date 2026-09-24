import re

with open('app/src/main/java/com/example/ui/theme/Translation.kt', 'r') as f:
    content = f.read()

target = """    "Save Notes" to "保存笔记","""
replacement = """    "Save Notes" to "保存笔记",
    "Notes" to "笔记",
    "Global Notebook" to "全局笔记本",
    "Search notes or tags..." to "搜索笔记或标签...",
    "All Courses" to "所有课程",
    "No notes found" to "未找到笔记",
    "Unknown Course" to "未知课程",
    "Add Note" to "添加笔记",
    "Please add a course first." to "请先添加课程。",
    "Link to Course" to "关联到课程",
    "Tags (comma separated)" to "标签（用逗号分隔）",
    "e.g. Midterm, Important" to "例如: 期中, 重要",
    "Note Content" to "笔记内容",
    "Tags (optional)" to "标签 (选填)","""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/theme/Translation.kt', 'w') as f:
    f.write(content)
print("Updated Translation.kt")
