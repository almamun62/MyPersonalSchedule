package com.example.domain

import com.example.data.model.CourseEntity

/**
 * Translation engine designed specifically for international students at Chinese universities.
 *
 * Automatically translates Chinese course names, classroom locations, building codes,
 * and department/faculty affiliations into English, with support for English-only and
 * Bilingual (English + Chinese) formats.
 */
object ScheduleTranslationEngine {

    enum class TranslationMode {
        ENGLISH_ONLY,
        BILINGUAL,
        ORIGINAL
    }

    data class TranslationResult(
        val translatedCourses: List<CourseEntity>,
        val translatedCourseCount: Int,
        val translatedClassroomCount: Int,
        val mode: TranslationMode
    )

    /**
     * Checks if a string contains any Chinese characters (Han script).
     */
    fun containsChinese(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return text.any { char ->
            Character.UnicodeScript.of(char.code) == Character.UnicodeScript.HAN
        }
    }

    /**
     * Translates a single course based on the given TranslationMode.
     */
    fun translateCourse(
        course: CourseEntity,
        mode: TranslationMode = TranslationMode.ENGLISH_ONLY
    ): CourseEntity {
        if (mode == TranslationMode.ORIGINAL) {
            return course
        }

        val englishName = translateCourseName(course.name)
        val englishClassroom = translateClassroom(course.classroom)
        val englishInstructor = translateInstructor(course.instructor)
        val englishNotes = translateNotes(course.notes)

        val finalName = when (mode) {
            TranslationMode.BILINGUAL -> {
                if (containsChinese(course.name) && englishName != course.name) {
                    val cleanOrig = course.name.replace(Regex("""[\[\(（【]国际学生[\]\)）】]"""), "").trim()
                    "$englishName ($cleanOrig)"
                } else {
                    englishName
                }
            }
            else -> englishName
        }

        val finalClassroom = when (mode) {
            TranslationMode.BILINGUAL -> {
                if (containsChinese(course.classroom) && englishClassroom != course.classroom) {
                    "$englishClassroom (${course.classroom})"
                } else {
                    englishClassroom
                }
            }
            else -> englishClassroom
        }

        return course.copy(
            name = finalName,
            classroom = finalClassroom,
            instructor = englishInstructor,
            notes = englishNotes
        )
    }

    /**
     * Translates a list of courses and provides translation statistics.
     */
    fun translateCourses(
        courses: List<CourseEntity>,
        mode: TranslationMode = TranslationMode.ENGLISH_ONLY
    ): TranslationResult {
        if (mode == TranslationMode.ORIGINAL) {
            return TranslationResult(courses, 0, 0, mode)
        }

        var courseCount = 0
        var classroomCount = 0

        val translated = courses.map { course ->
            val wasNameChinese = containsChinese(course.name)
            val wasClassroomChinese = containsChinese(course.classroom)

            val updated = translateCourse(course, mode)
            if (wasNameChinese && updated.name != course.name) courseCount++
            if (wasClassroomChinese && updated.classroom != course.classroom) classroomCount++
            updated
        }

        return TranslationResult(
            translatedCourses = translated,
            translatedCourseCount = courseCount,
            translatedClassroomCount = classroomCount,
            mode = mode
        )
    }

    /**
     * Translates Chinese course title to English.
     */
    fun translateCourseName(rawName: String): String {
        var clean = rawName.trim()
        if (clean.isEmpty()) return clean

        // Check if international student tag is present
        val isInternational = clean.contains("国际学生") || clean.contains("留学生")
        clean = clean.replace(Regex("""[\[\(（【]国际学生[\]\)）】]"""), "")
            .replace(Regex("""[\[\(（【]留学生[\]\)）】]"""), "")
            .replace(Regex("""[\[\(（【]\d+[\]\)）】]"""), "") // Remove class section tags like [2001]
            .trim()

        // 1. Direct dictionary match
        val directMatch = COURSE_DICTIONARY[clean]
        if (directMatch != null) {
            return if (isInternational) "$directMatch (Intl)" else directMatch
        }

        // 2. Partial / Compound phrase matching
        var translated = clean
        for ((cn, en) in COURSE_COMPOUND_DICTIONARY) {
            if (translated.contains(cn)) {
                translated = translated.replace(cn, en)
            }
        }

        // 3. Suffix normalization
        for ((suffixCn, suffixEn) in COURSE_SUFFIX_DICTIONARY) {
            if (translated.endsWith(suffixCn)) {
                val stem = translated.removeSuffix(suffixCn).trim()
                val stemTrans = COURSE_DICTIONARY[stem] ?: stem
                translated = "$stemTrans $suffixEn"
                break
            }
        }

        // Fallback for remaining Chinese characters
        if (containsChinese(translated)) {
            // Apply token replacement
            for ((tokenCn, tokenEn) in GENERAL_TOKEN_DICTIONARY) {
                translated = translated.replace(tokenCn, tokenEn)
            }
        }

        // Cleanup any extra spaces
        translated = translated.replace(Regex("""\s+"""), " ").trim()

        return if (isInternational && !translated.contains("(Intl)", ignoreCase = true) && !translated.contains("International", ignoreCase = true)) {
            "$translated (Intl)"
        } else {
            translated
        }
    }

    /**
     * Translates university campus classroom locations, building names, and lab rooms.
     */
    fun translateClassroom(rawRoom: String): String {
        var clean = rawRoom.trim()
        if (clean.isEmpty()) return "TBD"
        if (clean == "待定" || clean.equals("tbd", ignoreCase = true)) return "TBD"

        for ((cn, en) in CLASSROOM_DICTIONARY) {
            clean = clean.replace(cn, en)
        }

        // Replace room number patterns e.g. "软件机房1" -> "Software Lab 1"
        clean = clean.replace(Regex("""Software Lab\s*(\d+)"""), "Software Lab $1")
            .replace(Regex("""Computer Lab\s*(\d+)"""), "Computer Lab $1")
            .replace(Regex("""Teaching Bldg\s*(\d+)"""), "Teaching Bldg $1")

        // Clean extra spacing
        clean = clean.replace(Regex("""\s+"""), " ").trim()
        return clean
    }

    /**
     * Translates faculty, department, or instructor details.
     */
    fun translateInstructor(rawInstructor: String): String {
        var clean = rawInstructor.trim()
        if (clean.isEmpty()) return ""

        for ((cn, en) in DEPARTMENT_DICTIONARY) {
            clean = clean.replace(cn, en)
        }

        for ((cn, en) in INSTRUCTOR_TITLE_DICTIONARY) {
            clean = clean.replace(cn, en)
        }

        // Common instructors from Mamun's schedule
        clean = clean.replace("张剑", "Zhang Jian")
            .replace("古新宇", "Gu Xinyu")
            .replace("李老师", "Prof. Li")
            .replace("王老师", "Prof. Wang")
            .replace("张老师", "Prof. Zhang")
            .replace("刘老师", "Prof. Liu")
            .replace("陈老师", "Prof. Chen")
            .replace("赵老师", "Prof. Zhao")

        return clean.trim()
    }

    /**
     * Translates notes field if it contains Chinese text.
     */
    private fun translateNotes(rawNotes: String): String {
        var notes = rawNotes
        for ((cn, en) in NOTES_DICTIONARY) {
            notes = notes.replace(cn, en)
        }
        return notes
    }

    // ---------------------------------------------------------------------------------------------
    // DICTIONARIES
    // ---------------------------------------------------------------------------------------------

    private val COURSE_DICTIONARY = mapOf(
        // Computer Science & Software Engineering Core
        "计算机组成原理" to "Computer Organization and Architecture",
        "桌面应用程序设计" to "Desktop Application Design",
        "面向数据科学的编程语言" to "Programming Languages for Data Science",
        "数据库原理及应用" to "Database Principles & Applications",
        "数据库原理与应用" to "Database Principles & Applications",
        "数据库原理" to "Database Principles",
        "神经网络与深度学习导论" to "Introduction to Neural Networks & Deep Learning",
        "神经网络与深度学习" to "Neural Networks & Deep Learning",
        "数据分析与机器学习" to "Data Analysis & Machine Learning",
        "数据分析和机器学习" to "Data Analysis & Machine Learning",
        "生产实习及工程实践" to "Production Internship & Engineering Practice",
        "生产实习与工程实践" to "Production Internship & Engineering Practice",
        "计算机系统与程序设计竞赛" to "Computer Systems & Programming Contest",
        "算法与程序设计综合训练" to "Comprehensive Training in Algorithms & Programming",
        "数据库原理及应用课程设计" to "Database Principles Course Design",
        "操作系统" to "Operating Systems",
        "计算机网络" to "Computer Networks",
        "数据结构与算法" to "Data Structures & Algorithms",
        "数据结构" to "Data Structures",
        "算法设计与分析" to "Algorithm Design & Analysis",
        "软件工程" to "Software Engineering",
        "编译原理" to "Compiler Principles",
        "离散数学" to "Discrete Mathematics",
        "形式语言与自动机" to "Formal Languages & Automata",
        "人工智能导论" to "Introduction to Artificial Intelligence",
        "人工智能" to "Artificial Intelligence",
        "机器学习" to "Machine Learning",
        "深度学习" to "Deep Learning",
        "大数据技术" to "Big Data Technology",
        "大数据科学导论" to "Introduction to Big Data Science",
        "云计算与大数据" to "Cloud Computing & Big Data",
        "云计算" to "Cloud Computing",
        "网络空间安全" to "Cyberspace Security",
        "信息安全导论" to "Introduction to Information Security",
        "信息安全概论" to "Overview of Information Security",
        "信息安全" to "Information Security",
        "数字逻辑" to "Digital Logic",
        "数字逻辑与数字系统" to "Digital Logic & Systems",
        "微机原理与接口技术" to "Microcomputer Principles & Interfaces",
        "微机原理" to "Microcomputer Principles",
        "嵌入式系统设计" to "Embedded System Design",
        "嵌入式系统" to "Embedded Systems",
        "物联网工程导论" to "Introduction to IoT Engineering",
        "物联网导论" to "Introduction to IoT",
        "计算机图形学" to "Computer Graphics",
        "数字图像处理" to "Digital Image Processing",
        "分布式系统" to "Distributed Systems",
        "并行计算" to "Parallel Computing",

        // Programming Languages & Web
        "面向对象程序设计" to "Object-Oriented Programming",
        "面向对象程序设计(Java)" to "Object-Oriented Programming (Java)",
        "面向对象程序设计(C++)" to "Object-Oriented Programming (C++)",
        "Java语言程序设计" to "Java Programming",
        "Java程序设计" to "Java Programming",
        "Python语言程序设计" to "Python Programming",
        "Python程序设计" to "Python Programming",
        "C语言程序设计" to "C Programming",
        "C++语言程序设计" to "C++ Programming",
        "C++程序设计" to "C++ Programming",
        "Web前端开发" to "Web Frontend Development",
        "Web开发技术" to "Web Development Technology",
        "Web程序设计" to "Web Programming",
        "移动应用开发" to "Mobile Application Development",
        "Android移动应用开发" to "Android App Development",
        "跨平台移动开发" to "Cross-Platform Mobile Development",

        // Mathematics & Natural Sciences
        "高等数学" to "Advanced Mathematics",
        "高等数学A" to "Advanced Mathematics A",
        "高等数学B" to "Advanced Mathematics B",
        "微积分" to "Calculus",
        "线性代数" to "Linear Algebra",
        "概率论与数理统计" to "Probability & Mathematical Statistics",
        "复变函数与积分变换" to "Complex Functions & Integral Transforms",
        "大学物理" to "College Physics",
        "大学物理实验" to "College Physics Experiments",
        "普通化学" to "General Chemistry",

        // General Education & Humanities
        "大学英语" to "College English",
        "大学英语I" to "College English I",
        "大学英语II" to "College English II",
        "大学英语III" to "College English III",
        "大学英语IV" to "College English IV",
        "学术英语写作" to "Academic English Writing",
        "学术英语交流" to "Academic English Communication",
        "体育" to "Physical Education (P.E.)",
        "体育I" to "Physical Education I",
        "体育II" to "Physical Education II",
        "体育III" to "Physical Education III",
        "体育IV" to "Physical Education IV",
        "中国近现代史纲要" to "Modern Chinese History Outline",
        "中国近代史纲要" to "Modern Chinese History Outline",
        "思想道德与法治" to "Ideological Morality & Rule of Law",
        "毛泽东思想和中国特色社会主义理论体系概论" to "Mao Zedong Thought & Socialist Theory",
        "马克思主义基本原理" to "Basic Principles of Marxism",
        "形势与政策" to "Situation and Policy",
        "军事理论" to "Military Theory",
        "军事技能训练" to "Military Training",
        "创新创业基础" to "Innovation & Entrepreneurship Fundamentals",
        "大学生职业发展与就业指导" to "Career Development & Job Guidance",
        "大学生心理健康教育" to "College Student Mental Health Education"
    )

    private val COURSE_COMPOUND_DICTIONARY = listOf(
        "计算机组成原理" to "Computer Organization",
        "桌面应用程序设计" to "Desktop Application Design",
        "面向数据科学" to "For Data Science",
        "编程语言" to "Programming Languages",
        "数据库原理" to "Database Principles",
        "神经网络" to "Neural Networks",
        "深度学习" to "Deep Learning",
        "机器学习" to "Machine Learning",
        "数据分析" to "Data Analysis",
        "生产实习" to "Production Internship",
        "工程实践" to "Engineering Practice",
        "程序设计竞赛" to "Programming Contest",
        "综合训练" to "Comprehensive Training",
        "课程设计" to "Course Project",
        "操作系统" to "Operating Systems",
        "计算机网络" to "Computer Networks",
        "数据结构" to "Data Structures",
        "软件工程" to "Software Engineering",
        "人工智能" to "Artificial Intelligence",
        "高等数学" to "Advanced Math",
        "线性代数" to "Linear Algebra",
        "概率论" to "Probability",
        "数理统计" to "Statistics",
        "大学物理" to "College Physics",
        "大学英语" to "College English"
    )

    private val COURSE_SUFFIX_DICTIONARY = listOf(
        "课程设计" to "Course Project",
        "综合训练" to "Comprehensive Training",
        "综合实训" to "Practical Training",
        "实训" to "Practical Training",
        "实验" to "Lab",
        "实习" to "Internship",
        "导论" to "Introduction",
        "概论" to "Overview",
        "原理" to "Principles",
        "基础" to "Fundamentals",
        "及应用" to "& Applications",
        "与应用" to "& Applications",
        "设计" to "Design",
        "实践" to "Practice"
    )

    private val GENERAL_TOKEN_DICTIONARY = listOf(
        "及" to " & ",
        "与" to " & ",
        "和" to " & ",
        "（" to " (",
        "）" to ")",
        "【" to " [",
        "】" to "]",
        "国际学生" to "Intl",
        "留学生" to "Intl"
    )

    private val CLASSROOM_DICTIONARY = listOf(
        // Specific Campus Buildings
        "明理楼" to "Mingli Hall ",
        "明德楼" to "Mingde Hall ",
        "文理楼" to "Arts & Science Hall ",
        "信工楼" to "Info Engineering Bldg ",
        "信息工程楼" to "Info Engineering Bldg ",
        "计算机楼" to "CS Building ",
        "主楼" to "Main Building ",
        "逸夫楼" to "Yifu Building ",
        "科技楼" to "Science & Tech Bldg ",
        "实验楼" to "Lab Building ",
        "实验中心" to "Exp Center ",
        "图书馆" to "Library ",
        "体育馆" to "Gymnasium ",

        // Generic Rooms & Facilities
        "软件机房" to "Software Lab ",
        "机房" to "Computer Lab ",
        "语音室" to "Language Lab ",
        "多媒体教室" to "Multimedia Room ",
        "多媒体" to "Multimedia ",
        "阶梯教室" to "Lecture Hall ",
        "报告厅" to "Auditorium ",
        "一号教学楼" to "Teaching Bldg 1 ",
        "二号教学楼" to "Teaching Bldg 2 ",
        "三号教学楼" to "Teaching Bldg 3 ",
        "1号教学楼" to "Teaching Bldg 1 ",
        "2号教学楼" to "Teaching Bldg 2 ",
        "3号教学楼" to "Teaching Bldg 3 ",
        "教学楼" to "Teaching Bldg ",

        // Campus Zones
        "东区" to "East Campus ",
        "西区" to "West Campus ",
        "南区" to "South Campus ",
        "北区" to "North Campus ",
        "待定" to "TBD"
    )

    private val DEPARTMENT_DICTIONARY = listOf(
        "计算机学院" to "School of Computer Science",
        "计算机与科学技术学院" to "School of Computer Science & Technology",
        "软件学院" to "School of Software",
        "人工智能学院" to "School of Artificial Intelligence",
        "数据科学学院" to "School of Data Science",
        "信息工程学院" to "School of Information Engineering",
        "电子工程学院" to "School of Electronic Engineering",
        "理学院" to "School of Science",
        "数理学院" to "School of Mathematics & Physics",
        "国际教育学院" to "College of International Education",
        "外国语学院" to "School of Foreign Languages",
        "经济管理学院" to "School of Economics & Management",
        "体育部" to "Department of Physical Education"
    )

    private val INSTRUCTOR_TITLE_DICTIONARY = listOf(
        "教授" to "Prof. ",
        "副教授" to "Assoc. Prof. ",
        "讲师" to "Lecturer ",
        "助教" to "Teaching Assistant ",
        "老师" to "Prof. ",
        "博士" to "Dr. "
    )

    private val NOTES_DICTIONARY = listOf(
        "上课时间暂未确定" to "Class time TBD",
        "时间未定" to "Time TBD",
        "地点未定" to "Location TBD",
        "待定" to "TBD",
        "第一周开始" to "Starts week 1",
        "隔周上课" to "Bi-weekly class"
    )
}
