import os
import re

TRANSLATION_FILE = "app/src/main/java/com/example/ui/theme/Translation.kt"

# 1. Create the Translation.kt file
translation_content = """package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppLanguage = staticCompositionLocalOf { "en" }

val UI_TRANSLATIONS = mapOf(
    "1-12 Periods" to "1-12 节课",
    "1. Personalize & Display" to "1. 个性化与显示",
    "100% Offline & Private: No schedule or personal data ever leaves your device." to "100%离线与隐私：所有数据存储在本地。",
    "100% Offline-First Architecture" to "100% 离线优先架构",
    "15-Min Class Reminder" to "15分钟课前提醒",
    "15-Minute Advance Alerts" to "15分钟提前提醒",
    "15-Minute Advance Reminder" to "15分钟提前提醒",
    "15-Minute Class Reminder System" to "15分钟上课提醒系统",
    "2. Chinese University Schedule Setup" to "2. 中国大学课表设置",
    "3. Smart Silence & Reminders" to "3. 智能静音与提醒",
    "5-Min Nag Loop" to "5分钟循环提醒",
    "8 Courses • 15 Weekly Class Blocks • Includes Practicum & Contest" to "8门课程 • 15个每周课程块 • 包含实训和竞赛",
    "AI Integration (BYOK)" to "AI 集成 (自带密钥)",
    "API Key" to "API 密钥",
    "APP LANGUAGE (语言)" to "APP LANGUAGE (语言)",
    "Academic Semester Settings" to "学期设置",
    "Add" to "添加",
    "Add Course" to "添加课程",
    "Add Exam" to "添加考试",
    "Add Exam Countdown" to "添加考试倒计时",
    "Add Holiday / Make-up (调休)" to "添加节假日/调休",
    "Add Homework Task" to "添加作业任务",
    "Add Override" to "添加覆盖",
    "Add Picture" to "添加图片",
    "Add Task" to "添加任务",
    "Advance alert with classroom location & course title" to "带教室和课程名的提前提醒",
    "All Clear ✓" to "一切就绪 ✓",
    "All classes auto-paused per academic calendar." to "所有课程已根据校历暂停。",
    "All tasks completed! Ready for upcoming lectures." to "所有任务已完成！准备迎接接下来的课程。",
    "Allowed Materials / Notes" to "允许携带物品 / 备注",
    "App Limitations & Rules" to "应用限制与规则",
    "Appearance & Theme" to "外观与主题",
    "Ask Gemini..." to "询问AI...",
    "Auto-DND during class" to "上课期间自动免打扰",
    "Auto-DND on Class Start" to "上课时自动开启免打扰",
    "Auto-Translate to English" to "自动翻译成英语",
    "Auto-pauses on holidays or substitutes weekend schedules" to "节假日自动暂停或周末补课调休",
    "Automate your daily lecture routine so you never get interrupted during lectures or arrive late to a classroom." to "自动化日常课程提醒，让您在上课时不受打扰，也不会迟到。",
    "Automatic DND During Class" to "上课期间自动免打扰",
    "Automatically silences distractions and keeps DND active through consecutive lectures" to "自动静音干扰，并在连续上课时保持免打扰开启",
    "Back" to "返回",
    "Base URL" to "接口地址 (Base URL)",
    "Beginner's guide & First time trail" to "新手指南与初次体验",
    "Bilingual (EN + 中文)" to "双语 (EN + 中文)",
    "Bilingual (EN+中文)" to "双语 (EN+中文)",
    "Bilingual Course Names" to "双语课程名称",
    "Built specifically for university academic timetables in China (supporting 1-12/13 periods, bilingual translations, and single/double weeks 单双周)." to "专为中国大学课表设计（支持1-12/13节课、双语翻译以及单双周）。",
    "CHINESE DETECTED" to "检测到中文",
    "CSV Template" to "CSV 模板",
    "Cancel" to "取消",
    "Category / Associated Course" to "类别 / 关联课程",
    "Chat" to "AI助手",
    "Choose your preferred language and personalize your appearance." to "选择首选语言并个性化外观。",
    "Clear" to "清除",
    "Close" to "关闭",
    "Code (e.g. CS401)" to "课程代码 (例: CS401)",
    "Colliding Sessions:" to "时间冲突的课程:",
    "Color Tag" to "颜色标签",
    "Completed" to "已完成",
    "Conflict" to "冲突",
    "Conflicts" to "冲突",
    "Course Name *" to "课程名称 *",
    "Current" to "当前",
    "Custom Weeks (e.g. 1-8,10-16)" to "自定义周数 (例: 1-8,10-16)",
    "DISPLAY MODE" to "显示模式",
    "DND" to "免打扰",
    "Dashboard" to "仪表盘",
    "Date (YYYY-MM-DD)" to "日期 (YYYY-MM-DD)",
    "Day of the Week" to "星期",
    "Delete" to "删除",
    "Delivers alerts with classroom building & room code 15m prior" to "提前15分钟发送含教学楼和教室号的提醒",
    "Details" to "详情",
    "Details →" to "详情 →",
    "Display Mode" to "显示模式",
    "Do Not Disturb & Automation Engine" to "免打扰与自动化引擎",
    "Do Not Disturb is Active" to "免打扰已激活",
    "Draw with Pen" to "手写笔记",
    "End Period (1-12)" to "结束节数 (1-12)",
    "End Time" to "结束时间",
    "English" to "英文",
    "English Only" to "仅限英文",
    "Estimated Study Time (For Workload Chart)" to "预计学习时间 (用于工作量图表)",
    "Exams" to "考试",
    "Excel, CSV, or University Timetable format" to "Excel、CSV 或大学课表格式",
    "Export PDF" to "导出 PDF",
    "Fall 2026 Class Schedule (Mamun)" to "2026秋季课表 (Mamun)",
    "Full Week Grid" to "完整周视图",
    "Gemini is thinking..." to "AI 正在思考...",
    "Got it!" to "知道了！",
    "Grant" to "授权",
    "Grant Access" to "授予访问权限",
    "Great news! No schedule conflicts detected across all weeks." to "好消息！没有检测到任何周次的课程冲突。",
    "HOW WOULD YOU LIKE TO START?" to "您想如何开始？",
    "Holiday & Make-up Engine (调休)" to "节假日与调休引擎",
    "Holiday (No Classes)" to "节假日 (无课)",
    "Holiday auto-pause active • Triggers 15 mins before lectures" to "节假日自动暂停已激活 • 课前15分钟触发",
    "How to Use MySchedule" to "如何使用 MySchedule",
    "How to use MySchedule" to "如何使用 MySchedule",
    "HyperOS Accent Color" to "HyperOS 强调色",
    "I've Granted It" to "我已授权",
    "INTL STUDENT" to "国际学生",
    "Import" to "导入",
    "Import / Presets" to "导入 / 预设",
    "Import File" to "导入文件",
    "Import Schedule" to "导入课表",
    "Import your class timetable via Excel, CSV, or load your Fall 2026 course schedule in one tap." to "通过 Excel、CSV 导入课表，或一键加载您的2026秋季课表。",
    "Instructor" to "教师",
    "International Student Translation" to "国际学生翻译",
    "LIVE CARD PREVIEW" to "卡片实时预览",
    "Load Fall 2026 Preset" to "加载 2026 秋季预设",
    "Load Fall 2026 Schedule" to "加载 2026 秋季课表",
    "Load My Schedule" to "加载我的课表",
    "Location & course alert via Room DB" to "基于Room数据库的位置和课程提醒",
    "Manual Diagnostic & Test Actions" to "手动诊断与测试操作",
    "Model Name" to "模型名称",
    "Morning to Night" to "早到晚",
    "Most Used Applications" to "最常用的应用",
    "MySchedule" to "我的日程",
    "Name (e.g. Mid-Autumn Break)" to "名称 (例: 中秋节假期)",
    "No Schedule Loaded Yet" to "尚未加载课表",
    "No courses loaded yet. Select a file or preset above." to "尚未加载课程。请在上方选择文件或预设。",
    "No holiday overrides set. Tap + to add holidays or weekend make-up days." to "未设置节假日调休。点击 + 添加节假日或周末补课。",
    "No scheduled classes today! Perfect time for self-study or lab prep." to "今天没有课！正是自习或准备实验的好时机。",
    "No scheduled classes tomorrow. Enjoy your free time!" to "明天没有课。享受你的空闲时间吧！",
    "Notification Policy Access" to "通知权限访问",
    "Open Class Notebook" to "打开课堂笔记",
    "Open Settings" to "打开设置",
    "POPULAR" to "热门",
    "PRIMARY ACCENT COLOR (SOFT-GLOW BORDER)" to "主强调色 (柔光边框)",
    "Parsing timetable..." to "正在解析课表...",
    "Paste" to "粘贴",
    "Paste My Schedule" to "粘贴我的课表",
    "Pending Tasks" to "待办任务",
    "Personalize MySchedule" to "个性化 MySchedule",
    "Priority" to "优先级",
    "Provide your own API Key to enable contextual AI features. You can use Google Gemini (default), or point it to any OpenAI-compatible provider (e.g., DeepSeek, Minimax)." to "提供您的 API 密钥以启用 AI 功能。您可以使用 Google Gemini (默认)，或配置为兼容 OpenAI 的服务商 (例: DeepSeek, Minimax)。",
    "Queries your Room SQLite database to compute upcoming classes, factoring in academic week parity and holiday overrides. Delivers a high-priority banner 15 minutes before the lecture begins." to "查询您的本地 Room 数据库以计算即将到来的课程，并考虑单双周及节假日调休情况。提前15分钟发送重要提醒。",
    "Quick Actions & Templates" to "快捷操作与模板",
    "Re-sync Alarms" to "重新同步闹钟",
    "Record Voice" to "录制语音",
    "Replace Current Courses" to "替换当前课程",
    "Room *" to "教室 *",
    "Sample CSV" to "CSV 示例",
    "Save" to "保存",
    "Save Notes" to "保存笔记",
    "Save Settings" to "保存设置",
    "Schedule Conflict" to "课程冲突",
    "Schedule Import & Template" to "课表导入与模板",
    "Screen Time & Focus" to "屏幕时间与专注",
    "Seat Number (optional)" to "座位号 (选填)",
    "Send Message" to "发送消息",
    "Settings" to "设置",
    "Shows course names in English + Chinese (e.g., Computer Systems 计算机系统) for easy navigation." to "以中英双语显示课程名称 (例: Computer Systems 计算机系统)。",
    "Silences notifications during scheduled lectures" to "在上课期间静音所有通知",
    "Skip" to "跳过",
    "Start Period (1-12)" to "开始节数 (1-12)",
    "Start Time" to "开始时间",
    "Supports .xlsx, .csv, and exported school timetables" to "支持 .xlsx、.csv 及学校导出的课表",
    "Tap to Select Excel, CSV, or Text File" to "点击选择 Excel、CSV 或文本文件",
    "Task / Homework Title *" to "任务 / 作业标题 *",
    "Tasks" to "任务",
    "Template" to "模板",
    "Test 15m Alert" to "测试15分钟提醒",
    "Test Nag Loop" to "测试循环提醒",
    "Test Sticky Alert" to "测试持久提醒",
    "Time Conflict Detected" to "检测到时间冲突",
    "Timetable" to "时间表",
    "Timetable to Follow:" to "要遵循的课表 (调休):",
    "To track your phone usage and block distracting apps during class, you must grant Usage Access permission in your device settings." to "为了记录您的手机使用情况并在上课期间拦截干扰应用，您必须在设备设置中授予“使用情况访问权限”。",
    "Today's Schedule" to "今日日程",
    "Tomorrow's Preview" to "明日预览",
    "Total Weeks in Term" to "学期总周数",
    "Translation" to "翻译",
    "Try Sample" to "尝试示例",
    "Type" to "类型",
    "Understood" to "明白了",
    "Unscheduled" to "未安排时间",
    "Upcoming lectures monitored in Room database" to "已在本地数据库中监控即将到来的课程",
    "Upload Excel (.xlsx) / CSV schedules or get templates" to "上传 Excel (.xlsx) / CSV 课表或获取模板",
    "Usage" to "屏幕使用",
    "Usage Access Required" to "需要使用情况访问权限",
    "Week" to "周",
    "Week Alternation" to "单双周交替",
    "Week Rule (Academic Filtering)" to "周规则 (课程过滤)",
    "Weekend Make-up (调休)" to "周末调休",
    "Weekend Shift" to "周末调班",
    "Weekly Analysis & Insights" to "每周分析与洞察",
    "Workload" to "工作量",
    "Write, sketch, or record voice memos" to "书写、涂鸦或录音",
    "Zero network calls, analytics, or remote tracking. All course schedules, exams, and tasks reside strictly in your device's encrypted Room SQLite database." to "零网络请求、零分析和零远程追踪。所有课表、考试和任务都严格保存在您设备的加密本地数据库中。",
    "gemini-1.5-flash" to "gemini-1.5-flash",
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
with open(TRANSLATION_FILE, 'w', encoding='utf-8') as f:
    f.write(translation_content)

# 2. Modify files to append .tr to the targeted strings
files = [
    "app/src/main/java/com/example/ui/screens/ChatScreen.kt",
    "app/src/main/java/com/example/ui/screens/DashboardScreen.kt",
    "app/src/main/java/com/example/ui/screens/SettingsScreen.kt",
    "app/src/main/java/com/example/ui/screens/TasksExamsScreen.kt",
    "app/src/main/java/com/example/ui/screens/TimetableScreen.kt",
    "app/src/main/java/com/example/ui/screens/UsageScreen.kt",
    "app/src/main/java/com/example/ui/components/BootstartOnboardingDialog.kt",
    "app/src/main/java/com/example/ui/components/ClassNotebookDialog.kt",
    "app/src/main/java/com/example/ui/components/HyperIsland.kt",
    "app/src/main/java/com/example/ui/components/ScheduleDialogs.kt",
    "app/src/main/java/com/example/ui/components/SpreadsheetImportDialog.kt",
    "app/src/main/java/com/example/ui/components/TutorialDialog.kt"
]

import_statement = "import com.example.ui.theme.tr\n"

for fp in files:
    try:
        with open(fp, 'r', encoding='utf-8') as f:
            content = f.read()
            
        if "com.example.ui.theme.tr" not in content:
            # find first import
            content = re.sub(r'(import [^\n]+\n)', r'\1' + import_statement, content, count=1)
            
        # Regex to replace Text("...") or Text(text = "...")
        # Be careful not to replace things with string interpolation
        def replacer(match):
            prefix = match.group(1) # e.g. 'Text(' or 'Text(text = '
            string_content = match.group(2) # e.g. 'Dashboard'
            if "$" in string_content or string_content.startswith("nav_") or string_content.startswith("http"):
                return match.group(0) # Do not replace
            return f'{prefix}"{string_content}".tr'

        content = re.sub(r'(Text\s*\(\s*(?:text\s*=\s*)?)"([^"]+)"', replacer, content)
        
        # also translate contentDescription = "..."
        def desc_replacer(match):
            prefix = match.group(1) # 'contentDescription = '
            string_content = match.group(2)
            if "$" in string_content or string_content.startswith("nav_") or string_content.startswith("http"):
                return match.group(0)
            return f'{prefix}"{string_content}".tr'

        content = re.sub(r'(contentDescription\s*=\s*)"([^"]+)"', desc_replacer, content)
        
        with open(fp, 'w', encoding='utf-8') as f:
            f.write(content)
            
    except Exception as e:
        print(f"Error on {fp}: {e}")

print("Translation injected!")
