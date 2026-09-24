#!/usr/bin/env python3
import http.server
import socketserver
import os
import sys

PORT = 3000
APK_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), "app/build/outputs/apk/debug/app-debug.apk"))

HTML_CONTENT = """<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>课程表 - Course Schedule</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #4F46E5;
            --primary-bg: #EEF2FF;
            --bg-color: #F8FAFC;
            --surface: #FFFFFF;
            --surface-variant: #F1F5F9;
            --text-main: #0F172A;
            --text-muted: #64748B;
            --border: #E2E8F0;
            --shadow: 0 4px 14px 0 rgba(0, 0, 0, 0.06);
            --pill-active: #0F172A;
        }

        [data-theme="dark"] {
            --primary: #818CF8;
            --primary-bg: #1E1B4B;
            --bg-color: #0F172A;
            --surface: #1E293B;
            --surface-variant: #334155;
            --text-main: #F8FAFC;
            --text-muted: #94A3B8;
            --border: #334155;
            --shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.3);
            --pill-active: #38BDF8;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            -webkit-tap-highlight-color: transparent;
            font-family: 'Plus Jakarta Sans', system-ui, -apple-system, sans-serif;
        }

        body {
            background-color: #0B0F19;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            color: var(--text-main);
            overflow-x: hidden;
        }

        /* Mobile Device Shell Container */
        .device-container {
            width: 100%;
            max-width: 440px;
            height: 100vh;
            max-height: 920px;
            background: var(--bg-color);
            display: flex;
            flex-direction: column;
            position: relative;
            box-shadow: 0 25px 60px rgba(0, 0, 0, 0.5);
            border-radius: 28px;
            overflow: hidden;
            border: 1px solid rgba(255, 255, 255, 0.1);
        }

        @media (max-width: 480px) {
            .device-container {
                max-height: 100vh;
                border-radius: 0;
                border: none;
            }
        }

        /* APK Floating Download Banner */
        .apk-banner {
            background: linear-gradient(135deg, #3B82F6 0%, #1D4ED8 100%);
            color: #FFFFFF;
            padding: 8px 14px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-size: 12px;
            font-weight: 500;
            z-index: 100;
        }

        .apk-banner a {
            background: #FFFFFF;
            color: #1D4ED8;
            text-decoration: none;
            padding: 4px 10px;
            border-radius: 100px;
            font-weight: 700;
            font-size: 11px;
            display: inline-flex;
            align-items: center;
            gap: 4px;
            transition: all 0.2s;
        }

        .apk-banner a:hover {
            transform: scale(1.04);
            box-shadow: 0 2px 8px rgba(0,0,0,0.2);
        }

        /* App Top Bar */
        .top-bar {
            background: var(--surface);
            padding: 12px 16px 8px 16px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            border-bottom: 1px solid var(--border);
            z-index: 20;
        }

        .week-info {
            cursor: pointer;
        }

        .week-title {
            font-size: 19px;
            font-weight: 800;
            color: var(--text-main);
            display: flex;
            align-items: center;
            gap: 4px;
        }

        .week-date {
            font-size: 12px;
            color: var(--text-muted);
            margin-top: 1px;
            font-weight: 500;
        }

        .top-actions {
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .action-btn {
            width: 36px;
            height: 36px;
            border-radius: 50%;
            border: none;
            background: transparent;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            color: var(--text-main);
            transition: background 0.15s;
        }

        .action-btn:hover {
            background: var(--surface-variant);
        }

        .action-btn.ai-btn {
            background: #EBF3FF;
            color: #2563EB;
        }

        /* Weekday Header Strip */
        .weekday-strip {
            background: var(--surface);
            display: grid;
            grid-template-columns: 46px repeat(7, 1fr);
            padding: 6px 0;
            border-bottom: 1px solid var(--border);
            text-align: center;
            align-items: center;
        }

        .month-box {
            font-size: 12px;
            font-weight: 700;
            color: var(--text-muted);
            border-right: 1px solid var(--border);
        }

        .day-col-header {
            display: flex;
            flex-direction: column;
            align-items: center;
            cursor: pointer;
            padding: 2px 0;
            border-radius: 12px;
            transition: background 0.15s;
        }

        .day-col-header:hover {
            background: var(--surface-variant);
        }

        .day-name {
            font-size: 12px;
            font-weight: 600;
            color: var(--text-muted);
        }

        .day-num {
            font-size: 13px;
            font-weight: 700;
            color: var(--text-main);
            margin-top: 2px;
            width: 26px;
            height: 26px;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 13px;
        }

        .day-col-header.active .day-name {
            color: var(--text-main);
            font-weight: 800;
        }

        .day-col-header.active .day-num {
            background: var(--pill-active);
            color: #FFFFFF;
        }

        /* Timetable Scroll Area */
        .grid-scroll-container {
            flex: 1;
            overflow-y: auto;
            position: relative;
            background: var(--bg-color);
        }

        .timetable-grid {
            display: grid;
            grid-template-columns: 46px repeat(7, 1fr);
            min-height: 840px;
            position: relative;
        }

        /* Period Sidebar */
        .period-sidebar {
            display: flex;
            flex-direction: column;
            border-right: 1px solid var(--border);
            background: var(--surface);
        }

        .period-cell {
            height: 70px;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            border-bottom: 1px solid var(--border);
            font-size: 10px;
            color: var(--text-muted);
            padding: 2px;
            text-align: center;
        }

        .period-number {
            font-weight: 700;
            font-size: 13px;
            color: var(--text-main);
        }

        .period-time {
            font-size: 9px;
            opacity: 0.8;
            margin-top: 1px;
        }

        /* Day Columns */
        .day-column {
            position: relative;
            border-right: 1px solid rgba(226, 232, 240, 0.6);
            background-image: repeating-linear-gradient(
                to bottom,
                transparent,
                transparent 69px,
                var(--border) 69px,
                var(--border) 70px
            );
        }

        .day-column:last-child {
            border-right: none;
        }

        /* Course Cards */
        .course-card {
            position: absolute;
            left: 2px;
            right: 2px;
            border-radius: 8px;
            padding: 6px 4px;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            font-size: 11px;
            font-weight: 600;
            cursor: pointer;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
            transition: transform 0.15s, box-shadow 0.15s;
            overflow: hidden;
            word-break: break-all;
            line-height: 1.25;
        }

        .course-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        }

        .course-card .title {
            font-weight: 700;
            font-size: 11px;
            margin-bottom: 2px;
        }

        .course-card .room {
            font-size: 10px;
            opacity: 0.9;
            display: flex;
            align-items: center;
            gap: 2px;
        }

        .course-card .instructor {
            font-size: 9px;
            opacity: 0.8;
        }

        /* Bottom Navigation Bar */
        .bottom-nav {
            background: var(--surface);
            display: flex;
            justify-content: space-around;
            padding: 8px 12px 14px 12px;
            border-top: 1px solid var(--border);
            z-index: 20;
        }

        .nav-item {
            display: flex;
            flex-direction: column;
            align-items: center;
            font-size: 10px;
            font-weight: 600;
            color: var(--text-muted);
            cursor: pointer;
            gap: 3px;
        }

        .nav-item.active {
            color: var(--primary);
        }

        .nav-item svg {
            width: 22px;
            height: 22px;
            stroke-width: 2;
        }

        /* Modals & Bottom Sheets */
        .modal-overlay {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0, 0, 0, 0.5);
            display: none;
            align-items: center;
            justify-content: center;
            z-index: 200;
            backdrop-filter: blur(3px);
        }

        .modal-overlay.open {
            display: flex;
        }

        .sheet-box {
            background: var(--surface);
            width: 100%;
            max-width: 440px;
            border-radius: 20px 20px 0 0;
            position: fixed;
            bottom: 0;
            padding: 20px;
            box-shadow: 0 -10px 30px rgba(0,0,0,0.2);
            animation: slideUp 0.25s ease-out;
        }

        @keyframes slideUp {
            from { transform: translateY(100%); }
            to { transform: translateY(0); }
        }

        .sheet-handle {
            width: 36px;
            height: 4px;
            background: var(--text-muted);
            opacity: 0.3;
            border-radius: 2px;
            margin: 0 auto 16px auto;
        }

        .tool-grid {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 16px;
            text-align: center;
            margin-top: 16px;
        }

        .tool-item {
            display: flex;
            flex-direction: column;
            align-items: center;
            font-size: 11px;
            font-weight: 600;
            color: var(--text-main);
            gap: 8px;
            cursor: pointer;
        }

        .tool-icon {
            width: 48px;
            height: 48px;
            border-radius: 16px;
            background: var(--surface-variant);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
        }

        /* Dropdown Popup Menu */
        .dropdown-menu {
            position: absolute;
            top: 54px;
            right: 16px;
            background: var(--surface);
            border-radius: 14px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
            border: 1px solid var(--border);
            display: none;
            flex-direction: column;
            width: 200px;
            overflow: hidden;
            z-index: 100;
        }

        .dropdown-menu.open {
            display: flex;
        }

        .dropdown-item {
            padding: 12px 14px;
            font-size: 13px;
            font-weight: 500;
            color: var(--text-main);
            display: flex;
            align-items: center;
            gap: 10px;
            cursor: pointer;
            border-bottom: 1px solid var(--border);
        }

        .dropdown-item:last-child {
            border-bottom: none;
        }

        .dropdown-item:hover {
            background: var(--surface-variant);
        }
    </style>
</head>
<body>

<div class="device-container">
    <!-- Top Floating Notification for Preview & APK -->
    <div class="apk-banner">
        <span>🚀 课表运行中 · Android 原生已构建</span>
        <a href="/download-apk">⬇ 下载 APK</a>
    </div>

    <!-- App Top Bar (Screenshot 1) -->
    <div class="top-bar">
        <div class="week-info" onclick="toggleSheet()">
            <div class="week-title">
                第4周 周三
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M6 9l6 6 6-6"/></svg>
            </div>
            <div class="week-date">9/23/26 · 2026秋季学期</div>
        </div>
        <div class="top-actions">
            <!-- 1. AI Assistant -->
            <button class="action-btn ai-btn" title="AI Assistant" onclick="alert('AI 助教已准备就绪！可分析课表冲突、生成自习计划或答疑。')">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2 2 2 0 0 1-2-2V4a2 2 0 0 1 2-2zM4.93 4.93l1.41 1.41M19.07 4.93l-1.41 1.41M12 18a6 6 0 1 0 0-12 6 6 0 0 0 0 12z"/></svg>
            </button>
            <!-- 2. Add Course (+) -->
            <button class="action-btn" title="添加课程" onclick="openAddModal()">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
            </button>
            <!-- 3. Import Button -->
            <button class="action-btn" title="导入课程" onclick="toggleImportMenu(event)">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
            </button>
            <!-- 4. Share Button -->
            <button class="action-btn" title="分享课表" onclick="toggleShareMenu(event)">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"></path><polyline points="16 6 12 2 8 6"></polyline><line x1="12" y1="2" x2="12" y2="15"></line></svg>
            </button>
            <!-- 5. More Sheet -->
            <button class="action-btn" title="更多工具" onclick="toggleSheet()">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="1"></circle><circle cx="19" cy="12" r="1"></circle><circle cx="5" cy="12" r="1"></circle></svg>
            </button>
        </div>

        <!-- Import Dropdown (Screenshot 3) -->
        <div class="dropdown-menu" id="importMenu">
            <div class="dropdown-item" onclick="triggerImport('教务系统导入')">🎓 从教务导入</div>
            <div class="dropdown-item" onclick="triggerImport('口令导入')">🔑 分享口令导入</div>
            <div class="dropdown-item" onclick="triggerImport('Excel导入')">📊 Excel导入</div>
            <div class="dropdown-item" onclick="triggerImport('HTML导入')">💻 HTML导入</div>
            <div class="dropdown-item" onclick="triggerImport('备份导入')">📄 从备份导入</div>
            <div class="dropdown-item" onclick="triggerImport('拍照扫码')">📸 拍照 / 扫码导入</div>
        </div>

        <!-- Share Dropdown (Screenshot 4) -->
        <div class="dropdown-menu" id="shareMenu" style="right: 50px;">
            <div class="dropdown-item" onclick="exportCalendar()">📅 导出为日历文件 (.ics)</div>
            <div class="dropdown-item" onclick="alert('分享口令已生成: MAMUN-FALL-2026')">🔗 在线分享课表</div>
            <div class="dropdown-item" onclick="alert('系统分享面板已打开')">📱 分享 App</div>
        </div>
    </div>

    <!-- Weekday Header Strip (Screenshot 1) -->
    <div class="weekday-strip">
        <div class="month-box">9月</div>
        <div class="day-col-header" onclick="selectDay(1)">
            <span class="day-name">一</span>
            <span class="day-num">21</span>
        </div>
        <div class="day-col-header" onclick="selectDay(2)">
            <span class="day-name">二</span>
            <span class="day-num">22</span>
        </div>
        <div class="day-col-header active" onclick="selectDay(3)">
            <span class="day-name">三</span>
            <span class="day-num">23</span>
        </div>
        <div class="day-col-header" onclick="selectDay(4)">
            <span class="day-name">四</span>
            <span class="day-num">24</span>
        </div>
        <div class="day-col-header" onclick="selectDay(5)">
            <span class="day-name">五</span>
            <span class="day-num">25</span>
        </div>
        <div class="day-col-header" onclick="selectDay(6)">
            <span class="day-name">六</span>
            <span class="day-num">26</span>
        </div>
        <div class="day-col-header" onclick="selectDay(7)">
            <span class="day-name">日</span>
            <span class="day-num">27</span>
        </div>
    </div>

    <!-- Timetable 12 Periods Grid -->
    <div class="grid-scroll-container">
        <div class="timetable-grid">
            <!-- Period Sidebar (Periods 1 - 12) -->
            <div class="period-sidebar">
                <div class="period-cell"><span class="period-number">1</span><span class="period-time">08:00</span></div>
                <div class="period-cell"><span class="period-number">2</span><span class="period-time">08:50</span></div>
                <div class="period-cell"><span class="period-number">3</span><span class="period-time">09:50</span></div>
                <div class="period-cell"><span class="period-number">4</span><span class="period-time">10:40</span></div>
                <div class="period-cell"><span class="period-number">5</span><span class="period-time">13:30</span></div>
                <div class="period-cell"><span class="period-number">6</span><span class="period-time">14:20</span></div>
                <div class="period-cell"><span class="period-number">7</span><span class="period-time">15:20</span></div>
                <div class="period-cell"><span class="period-number">8</span><span class="period-time">16:10</span></div>
                <div class="period-cell"><span class="period-number">9</span><span class="period-time">18:30</span></div>
                <div class="period-cell"><span class="period-number">10</span><span class="period-time">19:20</span></div>
                <div class="period-cell"><span class="period-number">11</span><span class="period-time">20:10</span></div>
                <div class="period-cell"><span class="period-number">12</span><span class="period-time">21:00</span></div>
            </div>

            <!-- Monday (Col 1) -->
            <div class="day-column">
                <div class="course-card" style="top: 0px; height: 136px; background: #E0E7FF; color: #3730A3;" onclick="showDetail('高级操作系统', 'CS-401', '博学楼 302', '王教授', '08:00 - 09:35', '1-2节')">
                    <div class="title">高级操作系统</div>
                    <div class="room">📍 302</div>
                    <div class="instructor">王教授</div>
                </div>
                <div class="course-card" style="top: 280px; height: 136px; background: #DCFCE7; color: #166534;" onclick="showDetail('数据库系统原理', 'CS-202', '信科楼 204', '李老师', '13:30 - 15:05', '5-6节')">
                    <div class="title">数据库原理</div>
                    <div class="room">📍 204</div>
                    <div class="instructor">李老师</div>
                </div>
            </div>

            <!-- Tuesday (Col 2) -->
            <div class="day-column">
                <div class="course-card" style="top: 140px; height: 136px; background: #FEF3C7; color: #92400E;" onclick="showDetail('软件工程方法论', 'SE-301', '软件楼 401', '陈老师', '09:50 - 11:25', '3-4节')">
                    <div class="title">软件工程</div>
                    <div class="room">📍 401</div>
                    <div class="instructor">陈老师</div>
                </div>
            </div>

            <!-- Wednesday (Col 3 - Active Day) -->
            <div class="day-column" style="background-color: rgba(79, 70, 229, 0.03);">
                <div class="course-card" style="top: 0px; height: 136px; background: #E0E7FF; color: #3730A3;" onclick="showDetail('高级操作系统', 'CS-401', '博学楼 302', '王教授', '08:00 - 09:35', '1-2节')">
                    <div class="title">高级操作系统</div>
                    <div class="room">📍 302</div>
                    <div class="instructor">王教授</div>
                </div>
                <div class="course-card" style="top: 420px; height: 136px; background: #FCE7F3; color: #9D174D;" onclick="showDetail('计算机网络与通信', 'CS-305', '主楼 102', '张教授', '15:20 - 16:55', '7-8节')">
                    <div class="title">计算机网络</div>
                    <div class="room">📍 102</div>
                    <div class="instructor">张教授</div>
                </div>
            </div>

            <!-- Thursday (Col 4) -->
            <div class="day-column">
                <div class="course-card" style="top: 140px; height: 136px; background: #FEF3C7; color: #92400E;" onclick="showDetail('软件工程方法论', 'SE-301', '软件楼 401', '陈老师', '09:50 - 11:25', '3-4节')">
                    <div class="title">软件工程</div>
                    <div class="room">📍 401</div>
                    <div class="instructor">陈老师</div>
                </div>
                <div class="course-card" style="top: 560px; height: 136px; background: #E0F2FE; color: #0369A1;" onclick="showDetail('机器学习导论', 'AI-101', '理科楼 501', '赵教授', '18:30 - 20:05', '9-10节')">
                    <div class="title">机器学习导论</div>
                    <div class="room">📍 501</div>
                    <div class="instructor">赵教授</div>
                </div>
            </div>

            <!-- Friday (Col 5) -->
            <div class="day-column">
                <div class="course-card" style="top: 280px; height: 136px; background: #DCFCE7; color: #166534;" onclick="showDetail('数据库系统原理', 'CS-202', '信科楼 204', '李老师', '13:30 - 15:05', '5-6节')">
                    <div class="title">数据库原理</div>
                    <div class="room">📍 204</div>
                    <div class="instructor">李老师</div>
                </div>
            </div>

            <!-- Saturday (Col 6) -->
            <div class="day-column"></div>

            <!-- Sunday (Col 7) -->
            <div class="day-column"></div>
        </div>
    </div>

    <!-- Bottom Navigation Bar (Screenshot 1) -->
    <div class="bottom-nav">
        <div class="nav-item active">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
            <span>课表</span>
        </div>
        <div class="nav-item" onclick="alert('仪表盘：显示今日日程、上课提醒与免打扰状态')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
            <span>仪表盘</span>
        </div>
        <div class="nav-item" onclick="alert('作业与考试计划：已同步考期和提交截止日')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
            <span>待办考试</span>
        </div>
        <div class="nav-item" onclick="alert('所有已添课程管理')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path></svg>
            <span>课程</span>
        </div>
        <div class="nav-item" onclick="toggleSheet()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
            <span>工具</span>
        </div>
    </div>
</div>

<!-- 8-Tile Tools Bottom Sheet (Screenshot 5) -->
<div class="modal-overlay" id="toolsSheetOverlay" onclick="closeSheet(event)">
    <div class="sheet-box" onclick="event.stopPropagation()">
        <div class="sheet-handle"></div>
        <div style="font-weight: 700; font-size: 16px; margin-bottom: 8px;">快捷工具与设置</div>
        <div style="font-size: 13px; color: var(--text-muted); margin-bottom: 16px;">选择学期或配置节次时间</div>

        <div class="tool-grid">
            <div class="tool-item" onclick="alert('上课时间配置：支持修改1-12节的具体起止时间。已适配教务默认时刻。')">
                <div class="tool-icon">⏰</div>
                <span>上课时间</span>
            </div>
            <div class="tool-item" onclick="alert('课表设置：可开启周末7天视图、高亮今日、隐藏冲突。')">
                <div class="tool-icon">⚙️</div>
                <span>课表设置</span>
            </div>
            <div class="tool-item" onclick="alert('已添课程：可批量管理、编辑上课时间与冲突。')">
                <div class="tool-icon">📦</div>
                <span>已添课程</span>
            </div>
            <div class="tool-item" onclick="alert('桌面小组件：长按手机桌面即可添加课表微件！')">
                <div class="tool-icon">📱</div>
                <span>小组件</span>
            </div>
            <div class="tool-item" onclick="alert('关于我们：智能课表 v1.0\n专为高校学生打造。')">
                <div class="tool-icon">💬</div>
                <span>联系我们</span>
            </div>
            <div class="tool-item" onclick="alert('上课提醒：已启用课前15分钟自动弹窗与振动提醒。')">
                <div class="tool-icon">🔔</div>
                <span>上课提醒</span>
            </div>
            <div class="tool-item" onclick="toggleTheme()">
                <div class="tool-icon">🌙</div>
                <span>夜间模式</span>
            </div>
            <div class="tool-item" onclick="alert('简洁模式：已将网格间距调整为紧凑排版。')">
                <div class="tool-icon">🔲</div>
                <span>简洁模式</span>
            </div>
        </div>
    </div>
</div>

<script>
    function toggleSheet() {
        const overlay = document.getElementById('toolsSheetOverlay');
        overlay.classList.toggle('open');
    }

    function closeSheet(e) {
        document.getElementById('toolsSheetOverlay').classList.remove('open');
    }

    function toggleImportMenu(e) {
        e.stopPropagation();
        document.getElementById('shareMenu').classList.remove('open');
        document.getElementById('importMenu').classList.toggle('open');
    }

    function toggleShareMenu(e) {
        e.stopPropagation();
        document.getElementById('importMenu').classList.remove('open');
        document.getElementById('shareMenu').classList.toggle('open');
    }

    document.addEventListener('click', () => {
        document.getElementById('importMenu').classList.remove('open');
        document.getElementById('shareMenu').classList.remove('open');
    });

    function selectDay(day) {
        const headers = document.querySelectorAll('.day-col-header');
        headers.forEach((h, i) => {
            if (i === day - 1) h.classList.add('active');
            else h.classList.remove('active');
        });
    }

    function showDetail(title, code, room, teacher, time, section) {
        alert("📚 课程详情\\n\\n课程名称: " + title + "\\n课程代码: " + code + "\\n教室地点: " + room + "\\n任课教师: " + teacher + "\\n上课时间: " + time + " (" + section + ")\\n周次: 1-16周 (全周)");
    }

    function triggerImport(type) {
        alert("🚀 已触发 " + type + "\\n已载入 Fall 2026 课表预设！");
    }

    function openAddModal() {
        const name = prompt("输入要添加的课程名称 (例如: 线性代数):");
        if (name) {
            alert("已成功添加课程: " + name + "！");
        }
    }

    function exportCalendar() {
        window.location.href = '/download-ics';
    }

    function toggleTheme() {
        const current = document.documentElement.getAttribute('data-theme');
        if (current === 'dark') {
            document.documentElement.removeAttribute('data-theme');
        } else {
            document.documentElement.setAttribute('data-theme', 'dark');
        }
    }
</script>
</body>
</html>
"""

ICS_CONTENT = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//AI Studio//Course Schedule//CN
CALSCALE:GREGORIAN
METHOD:PUBLISH
BEGIN:VEVENT
SUMMARY:高级操作系统 (王教授)
LOCATION:博学楼 302
DTSTART:20260923T080000
DTEND:20260923T093500
RRULE:FREQ=WEEKLY;UNTIL=20261231T235959
DESCRIPTION:课程代码: CS-401\\n节次: 1-2节
END:VEVENT
BEGIN:VEVENT
SUMMARY:数据库原理 (李老师)
LOCATION:信科楼 204
DTSTART:20260923T133000
DTEND:20260923T150500
RRULE:FREQ=WEEKLY;UNTIL=20261231T235959
DESCRIPTION:课程代码: CS-202\\n节次: 5-6节
END:VEVENT
END:VCALENDAR
"""

class RequestHandler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/download-apk" or self.path == "/app-debug.apk":
            if os.path.exists(APK_PATH):
                self.send_response(200)
                self.send_header("Content-Type", "application/vnd.android.package-archive")
                self.send_header("Content-Disposition", 'attachment; filename="CourseSchedule.apk"')
                self.send_header("Content-Length", str(os.path.getsize(APK_PATH)))
                self.end_headers()
                with open(APK_PATH, "rb") as f:
                    self.wfile.write(f.read())
            else:
                self.send_response(404)
                self.end_headers()
                self.wfile.write(b"APK not ready yet. Please run compile_applet.")
            return

        if self.path == "/download-ics" or self.path == "/schedule.ics":
            self.send_response(200)
            self.send_header("Content-Type", "text/calendar; charset=utf-8")
            self.send_header("Content-Disposition", 'attachment; filename="courses.ics"')
            content_bytes = ICS_CONTENT.encode("utf-8")
            self.send_header("Content-Length", str(len(content_bytes)))
            self.end_headers()
            self.wfile.write(content_bytes)
            return

        # Default: return HTML UI
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        content_bytes = HTML_CONTENT.encode("utf-8")
        self.send_header("Content-Length", str(len(content_bytes)))
        self.end_headers()
        self.wfile.write(content_bytes)

    def log_message(self, format, *args):
        pass # suppress console noise

if __name__ == "__main__":
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), RequestHandler) as httpd:
        print(f"Web Preview Server running on port {PORT}...")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            pass
