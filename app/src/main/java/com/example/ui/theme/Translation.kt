package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

val LocalAppLanguage = compositionLocalOf { "en" }

val String.tr: String
    @Composable
    get() {
        val lang = LocalAppLanguage.current
        if (!lang.startsWith("zh")) return this
        return when (this) {
            "Timetable" -> "课表"
            "Dashboard" -> "首页"
            "Courses" -> "课程"
            "Tasks & Exams" -> "任务与考试"
            "Tasks" -> "任务"
            "Exams" -> "考试"
            "Import" -> "导入"
            "Settings" -> "设置"
            "More" -> "更多"
            "Add Course" -> "添加课程"
            "Edit Course" -> "编辑课程"
            "Save" -> "保存"
            "Cancel" -> "取消"
            "Delete" -> "删除"
            "Classroom" -> "教室"
            "Instructor" -> "教师"
            "Teacher" -> "教师"
            "Time" -> "时间"
            "Start Time" -> "开始时间"
            "End Time" -> "结束时间"
            "Monday" -> "星期一"
            "Tuesday" -> "星期二"
            "Wednesday" -> "星期三"
            "Thursday" -> "星期四"
            "Friday" -> "星期五"
            "Saturday" -> "星期六"
            "Sunday" -> "星期日"
            "Mon" -> "周一"
            "Tue" -> "周二"
            "Wed" -> "周三"
            "Thu" -> "周四"
            "Fri" -> "周五"
            "Sat" -> "周六"
            "Sun" -> "周日"
            "Lunch Break" -> "午餐午休"
            "Dinner Break" -> "晚餐休息"
            "Break Time" -> "课间休息"
            "Reset" -> "重置"
            "Confirm" -> "确定"
            "Close" -> "关闭"
            else -> this
        }
    }
