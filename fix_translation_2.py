import re

with open('app/src/main/java/com/example/ui/theme/Translation.kt', 'r') as f:
    content = f.read()

# Let's just fix the method `val String.tr: String` manually
# Split by `val String.tr: String`
parts = content.split('val String.tr: String')
if len(parts) == 2:
    # First part is the map. Let's clean it up if it has weird stuff. But the weird stuff was added at the end of the file.
    pass

# Alternatively, I can just rewrite the end of the file from `    "gemini-1.5-flash" to "gemini-1.5-flash",`
start_marker = '    "gemini-1.5-flash" to "gemini-1.5-flash",'
parts = content.split(start_marker)

correct_ending = """    "gemini-1.5-flash" to "gemini-1.5-flash",
    "Beginner's guide & First time trail" to "新手指南与首次体验",
    "100% Offline-First Architecture" to "100% 离线优先架构",
    "AI Integration (BYOK)" to "AI 集成 (BYOK)",
    "Day Mode" to "日间模式",
    "Night Mode" to "夜间模式",
    "System Default" to "系统默认",
    "Base URL" to "接口地址 (Base URL)",
    "API Key" to "API 密钥",
    "Zero network calls, analytics, or remote tracking. All course schedules, exams, and tasks reside strictly in your device's encrypted Room SQLite database." to "零网络请求、零分析和零远程追踪。所有课表、考试和任务都严格保存在您设备的加密本地数据库中。",
    "Fall 2026 Term" to "2026秋季学期",
    "mins" to "分钟",
    "Term Name: " to "学期名称: ",
    "Semester Start Date: " to "学期开始日期: ",
    "Set Up" to "设置",
    "Edit Settings" to "编辑设置",
    "中文" to "中文"
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
    f.write(parts[0] + correct_ending)

print("Rewrote ending")
