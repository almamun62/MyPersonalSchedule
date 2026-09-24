package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.data.model.CourseEntity
import com.example.data.model.WeekRule
import com.example.domain.ScheduleTranslationEngine
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

data class ImportResult(
    val courses: List<CourseEntity>,
    val fileName: String,
    val rowCount: Int,
    val error: String? = null,
    val wasTranslated: Boolean = false,
    val translatedCount: Int = 0
)

object ScheduleImportHelper {

    val SAMPLE_CSV_TEMPLATE = """
        Course Name,Course Code,Classroom,Instructor,Day,Start Period,End Period,Start Time,End Time,Week Rule,Custom Weeks,Color
        Advanced Mathematics,MATH-101,Teaching Bldg 1-101,Prof. Zhang,1,1,2,08:00,09:35,ALL,,#337DFF
        College English IV,ENG-204,Liberal Arts 302,Dr. Wang,2,3,4,09:50,11:25,ALL,,#34C759
        Computer Architecture,CS-301,Science Hall 405,Prof. Li,3,3,5,09:50,12:15,CUSTOM,1-10,#FF9500
        Data Structures & Algorithms,CS-202,Lab Bldg 208,Dr. Chen,4,6,7,14:30,16:05,ALL,,#AF52DE
        Machine Learning Seminar,AI-401,Auditorium 2,Prof. Liu,5,8,9,16:20,17:55,ODD,,#FF2D55
        University Physics Exp,PHYS-102,Exp Center 501,Dr. Zhao,5,10,12,19:00,21:25,EVEN,,#00B4D8
    """.trimIndent()

    val SAMPLE_TSV_TEMPLATE = "Course Name\tCourse Code\tClassroom\tInstructor\tDay\tStart Period\tEnd Period\tStart Time\tEnd Time\tWeek Rule\tCustom Weeks\tColor\n" +
            "Advanced Mathematics\tMATH-101\tTeaching Bldg 1-101\tProf. Zhang\t1\t1\t2\t08:00\t09:35\tALL\t\t#337DFF\n" +
            "College English IV\tENG-204\tLiberal Arts 302\tDr. Wang\t2\t3\t4\t09:50\t11:25\tALL\t\t#34C759\n" +
            "Computer Architecture\tCS-301\tScience Hall 405\tProf. Li\t3\t3\t5\t09:50\t12:15\tCUSTOM\t1-10\t#FF9500\n" +
            "Data Structures & Algorithms\tCS-202\tLab Bldg 208\tDr. Chen\t4\t6\t7\t14:30\t16:05\tALL\t\t#AF52DE\n" +
            "Machine Learning Seminar\tAI-401\tAuditorium 2\tProf. Liu\t5\t8\t9\t16:20\t17:55\tODD\t\t#FF2D55\n" +
            "University Physics Exp\tPHYS-102\tExp Center 501\tDr. Zhao\t5\t10\t12\t19:00\t21:25\tEVEN\t\t#00B4D8"

    val SAMPLE_TEXT_FORMAT_TEMPLATE = """
        1619304040-Computer Architecture [CS-301]
        1-10周,星期1,第3节-第5节,明理楼B105,张教授

        1619310040-Desktop Application Design [CS-302]
        9-12周,15-18周,星期1,第6节-第7节,明理楼软件机房1,李老师

        1619312040-Data Analysis & Machine Learning [AI-201]
        1-11周,星期3,第8节-第9节,明德楼B301,王教授
    """.trimIndent()

    val MAMUN_SCHEDULE_INFO = "2026-2027学年 秋季学期 • MD. ABDULLAH AL MAMUN (202338060087)"

    val MAMUN_SCHEDULE_RAW_TEXT = """
        1619304040-计算机组成原理(国际学生)[2001]
        1-10周,星期1,第3节-第5节明理楼B105

        1619310040-桌面应用程序设计(国际学生)[2001]
        9-12周,15-18周,星期1,第6节-第7节明理楼软件机房1

        1619313040-面向数据科学的编程语言(国际学生)[2001]
        1-9周,星期1,第8节-第9节明理楼B402

        1619304040-计算机组成原理(国际学生)[2001]
        3-5周,星期1,第10节-第12节明理楼B105

        1619310040-桌面应用程序设计(国际学生)[2001]
        9-12周,15-18周,星期3,第1节-第2节明理楼软件机房1

        1619304040-计算机组成原理(国际学生)[2001]
        1-2周,6-10周,星期3,第3节-第5节明理楼B105

        1619307040-数据库原理及应用(国际学生)[2001]
        18周,星期3,第3节-第5节明理楼软件机房1

        1619311040-神经网络与深度学习导论(国际学生)[2001]
        1-11周,星期3,第3节-第4节明德楼A304

        1619313040-面向数据科学的编程语言(国际学生)[2001]
        1-9周,星期3,第6节-第7节明理楼B402

        1619312040-数据分析与机器学习(国际学生)[2001]
        1-11周,星期3,第8节-第9节明德楼B301

        1619312040-数据分析与机器学习(国际学生)[2001]
        3周,星期3,第10节-第11节明德楼B301

        1619312040-数据分析与机器学习(国际学生)[2001]
        1-11周,星期5,第1节-第2节明德楼B301

        1619307040-数据库原理及应用(国际学生)[2001]
        1-12周,15-17周,星期5,第3节-第5节明理楼软件机房1

        1619307040-数据库原理及应用(国际学生)[2001]
        18周,星期5,第3节-第5节明理楼软件机房1

        1619311040-神经网络与深度学习导论(国际学生)[2001]
        1-11周,星期5,第8节-第9节明德楼A304
    """.trimIndent()

    fun getMamunFall2026Schedule(
        semesterId: Long,
        autoTranslateToEnglish: Boolean = false,
        bilingual: Boolean = false
    ): List<CourseEntity> {
        val rawCourses = listOf(
            // 1. 桌面应用程序设计(国际学生) [1619310040]
            CourseEntity(
                semesterId = semesterId,
                name = "桌面应用程序设计(国际学生)",
                code = "1619310040",
                classroom = "明理楼软件机房1",
                instructor = "计算机学院",
                dayOfWeek = 1, // Monday
                startPeriod = 6,
                endPeriod = 7,
                startTime = "14:30",
                endTime = "16:05",
                weekRule = WeekRule.CUSTOM,
                customWeeks = "9-12,15-18",
                colorHex = 0xFF2563EB
            ),
            CourseEntity(
                semesterId = semesterId,
                name = "桌面应用程序设计(国际学生)",
                code = "1619310040",
                classroom = "明理楼软件机房1",
                instructor = "计算机学院",
                dayOfWeek = 3, // Wednesday
                startPeriod = 1,
                endPeriod = 2,
                startTime = "08:00",
                endTime = "09:35",
                weekRule = WeekRule.CUSTOM,
                customWeeks = "9-12,15-18",
                colorHex = 0xFF2563EB
            ),

            // 2. 数据分析与机器学习(国际学生) [1619312040]
            CourseEntity(
                semesterId = semesterId,
                name = "数据分析与机器学习(国际学生)",
                code = "1619312040",
                classroom = "明德楼B301",
                instructor = "人工智能学院",
                dayOfWeek = 5, // Friday
                startPeriod = 1,
                endPeriod = 2,
                startTime = "08:00",
                endTime = "09:35",
                weekRule = WeekRule.CUSTOM,
                customWeeks = "1-11",
                colorHex = 0xFF0D9488
            ),
            CourseEntity(
                semesterId = semesterId,
                name = "数据分析与机器学习(国际学生)",
                code = "1619312040",
                classroom = "明德楼B301",
                instructor = "人工智能学院",
                dayOfWeek = 3, // Wednesday
                startPeriod = 8,
                endPeriod = 9,
                startTime = "15:20",
                endTime = "17:55",
                weekRule = WeekRule.CUSTOM,
                customWeeks = "1-11",
                colorHex = 0xFF0D9488
            ),
            CourseEntity(
                semesterId = semesterId,
                name = "数据分析与机器学习(国际学生)",
                code = "1619312040",
                classroom = "明德楼B301",
                instructor = "人工智能学院",
                dayOfWeek = 3, // Wednesday
                startPeriod = 10,
                endPeriod = 11,
                startTime = "18:30",
                endTime = "20:35",
                weekRule = WeekRule.CUSTOM,
                customWeeks = "3",
                colorHex = 0xFF0D9488
            ),

            // 3. 计算机组成原理(国际学生) [1619304040]
            CourseEntity(
                semesterId = semesterId,
                name = "计算机组成原理(国际学生)",
                code = "1619304040",
                classroom = "明理楼B105",
                instructor = "计算机学院",
                dayOfWeek = 1, // Monday
                startPeriod = 3,
                endPeriod = 5,
                startTime = "09:50",
                endTime = "12:15",
                weekRule = WeekRule.CUSTOM,
                customWeeks = "1-10",
                colorHex = 0xFFE11D48
            ),
            CourseEntity(
                semesterId = semesterId,
                name = "计算机组成原理(国际学生)",
                code = "1619304040",
                classroom = "明理楼B105",
                instructor = "计算机学院",
                dayOfWeek = 3, // Wednesday
                startPeriod = 3,
                endPeriod = 5,
                startTime = "09:50",
                endTime = "12:15",
                weekRule = WeekRule.CUSTOM,
                customWeeks = "1-2,6-10", // ⚠️ CONFLICTS with 神经网络与深度学习导论 on Wed Sec 3-4!
                colorHex = 0xFFE11D48
            ),
            CourseEntity(
                semesterId = semesterId,
                name = "计算机组成原理(国际学生)",
                code = "1619304040",
                classroom = "明理楼B105",
                instructor = "计算机学院",
                dayOfWeek = 1, // Monday
                startPeriod = 10,
                endPeriod = 12,
                startTime = "18:30",
                endTime = "21:25",
                weekRule = WeekRule.CUSTOM,
                customWeeks = "3-5",
                colorHex = 0xFFE11D48
            ),

            // 4. 数据库原理及应用(国际学生) [1619307040]
            CourseEntity(
                semesterId = semesterId,
                name = "数据库原理及应用(国际学生)",
                code = "1619307040",
                classroom = "明理楼软件机房1",
                instructor = "计算机学院",
                dayOfWeek = 5, // Friday
                startPeriod = 3,
                endPeriod = 5,
                startTime = "09:50",
                endTime = "12:15",
                weekRule = WeekRule.CUSTOM,
                customWeeks = "1-12,15-17",
                colorHex = 0xFF7C3AED
            ),
            CourseEntity(
                semesterId = semesterId,
                name = "数据库原理及应用(国际学生)",
                code = "1619307040",
                classroom = "明理楼软件机房1",
                instructor = "计算机学院",
                dayOfWeek = 5, // Friday
                startPeriod = 3,
                endPeriod = 5,
                startTime = "09:50",
                endTime = "12:15",
                weekRule = WeekRule.CUSTOM,
                customWeeks = "18",
                colorHex = 0xFF7C3AED
            ),
            CourseEntity(
                semesterId = semesterId,
                name = "数据库原理及应用(国际学生)",
                code = "1619307040",
                classroom = "明理楼软件机房1",
                instructor = "计算机学院",
                dayOfWeek = 3, // Wednesday
                startPeriod = 3,
                endPeriod = 5,
                startTime = "09:50",
                endTime = "12:15",
                weekRule = WeekRule.CUSTOM,
                customWeeks = "18", // Only Week 18, so no conflict with Comp Org!
                colorHex = 0xFF7C3AED
            ),

            // 5. 神经网络与深度学习导论(国际学生) [1619311040]
            CourseEntity(
                semesterId = semesterId,
                name = "神经网络与深度学习导论(国际学生)",
                code = "1619311040",
                classroom = "明德楼A304",
                instructor = "人工智能学院",
                dayOfWeek = 3, // Wednesday
                startPeriod = 3,
                endPeriod = 4,
                startTime = "09:50",
                endTime = "11:25",
                weekRule = WeekRule.CUSTOM,
                customWeeks = "1-11", // ⚠️ CONFLICTS with 计算机组成原理 on Wed Sec 3-4 (Weeks 1-2, 6-10)!
                colorHex = 0xFFEA580C
            ),
            CourseEntity(
                semesterId = semesterId,
                name = "神经网络与深度学习导论(国际学生)",
                code = "1619311040",
                classroom = "明德楼A304",
                instructor = "人工智能学院",
                dayOfWeek = 5, // Friday
                startPeriod = 8,
                endPeriod = 9,
                startTime = "15:20",
                endTime = "17:55",
                weekRule = WeekRule.CUSTOM,
                customWeeks = "1-11",
                colorHex = 0xFFEA580C
            ),

            // 6. 面向数据科学的编程语言(国际学生) [1619313040]
            CourseEntity(
                semesterId = semesterId,
                name = "面向数据科学的编程语言(国际学生)",
                code = "1619313040",
                classroom = "明理楼B402",
                instructor = "数据科学学院",
                dayOfWeek = 1, // Monday
                startPeriod = 8,
                endPeriod = 9,
                startTime = "15:20",
                endTime = "17:55",
                weekRule = WeekRule.CUSTOM,
                customWeeks = "1-9",
                colorHex = 0xFF16A34A
            ),
            CourseEntity(
                semesterId = semesterId,
                name = "面向数据科学的编程语言(国际学生)",
                code = "1619313040",
                classroom = "明理楼B402",
                instructor = "数据科学学院",
                dayOfWeek = 3, // Wednesday
                startPeriod = 6,
                endPeriod = 7,
                startTime = "14:30",
                endTime = "16:05",
                weekRule = WeekRule.CUSTOM,
                customWeeks = "1-9",
                colorHex = 0xFF16A34A
            )
        )
        return if (autoTranslateToEnglish) {
            val mode = if (bilingual) ScheduleTranslationEngine.TranslationMode.BILINGUAL else ScheduleTranslationEngine.TranslationMode.ENGLISH_ONLY
            ScheduleTranslationEngine.translateCourses(rawCourses, mode).translatedCourses
        } else {
            rawCourses
        }
    }

    fun getMamunUnscheduledCourses(
        semesterId: Long,
        autoTranslateToEnglish: Boolean = false,
        bilingual: Boolean = false
    ): List<CourseEntity> {
        val rawCourses = listOf(
            CourseEntity(
                semesterId = semesterId,
                name = "生产实习及工程实践(国际学生)",
                code = "1619317060",
                classroom = "待定",
                instructor = "张剑, 古新宇",
                dayOfWeek = 0,
                startPeriod = 0,
                endPeriod = 0,
                startTime = "待定",
                endTime = "待定",
                weekRule = WeekRule.ALL,
                customWeeks = "",
                colorHex = 0xFF64748B,
                notes = "上课时间暂未确定"
            ),
            CourseEntity(
                semesterId = semesterId,
                name = "计算机系统与程序设计竞赛（国际学生）",
                code = "1623303020",
                classroom = "待定",
                instructor = "张剑, 古新宇",
                dayOfWeek = 0,
                startPeriod = 0,
                endPeriod = 0,
                startTime = "待定",
                endTime = "待定",
                weekRule = WeekRule.ALL,
                customWeeks = "",
                colorHex = 0xFF64748B,
                notes = "上课时间暂未确定"
            )
        )
        return if (autoTranslateToEnglish) {
            val mode = if (bilingual) ScheduleTranslationEngine.TranslationMode.BILINGUAL else ScheduleTranslationEngine.TranslationMode.ENGLISH_ONLY
            ScheduleTranslationEngine.translateCourses(rawCourses, mode).translatedCourses
        } else {
            rawCourses
        }
    }

    private val STANDARD_START_TIMES = listOf(
        "08:00", "08:50", "09:50", "10:40", "11:30",
        "14:30", "15:20", "16:20", "17:10",
        "19:00", "19:50", "20:40"
    )

    private val STANDARD_END_TIMES = listOf(
        "08:45", "09:35", "10:35", "11:25", "12:15",
        "15:15", "16:05", "17:05", "17:55",
        "19:45", "20:35", "21:25"
    )

    private val DEFAULT_PALETTE = listOf(
        0xFF337DFF, // System Blue
        0xFF00B4D8, // Cyan
        0xFF34C759, // Green
        0xFFFF9500, // Orange
        0xFFFF2D55, // Pink
        0xFFAF52DE, // Purple
        0xFF5856D6, // Indigo
        0xFFFFCC00  // Yellow
    )

    fun shareCsvTemplate(context: Context) {
        try {
            val cacheDir = context.cacheDir
            val templateFile = File(cacheDir, "schedule_template.csv")
            templateFile.writeText(SAMPLE_CSV_TEMPLATE)

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                templateFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Course Schedule Template")
                putExtra(Intent.EXTRA_TEXT, "Here is the course schedule import template (CSV/Excel compatible).")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share or Open CSV Template"))
        } catch (e: Exception) {
            // Fallback to text share
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Course Schedule Template")
                putExtra(Intent.EXTRA_TEXT, SAMPLE_CSV_TEMPLATE)
            }
            context.startActivity(Intent.createChooser(intent, "Share Template"))
        }
    }

    fun copyTemplateToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Course Schedule Template", SAMPLE_CSV_TEMPLATE)
        clipboard.setPrimaryClip(clip)
    }

    fun copyTsvToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Course Schedule TSV", SAMPLE_TSV_TEMPLATE)
        clipboard.setPrimaryClip(clip)
    }

    fun copyTextFormatToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("University Course Format", SAMPLE_TEXT_FORMAT_TEMPLATE)
        clipboard.setPrimaryClip(clip)
    }

    fun getFileName(context: Context, uri: Uri): String {
        var name = "uploaded_file"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex) ?: name
                }
            }
        } catch (_: Exception) {}
        return name
    }

    fun parseUri(
        context: Context,
        uri: Uri,
        semesterId: Long,
        autoTranslateToEnglish: Boolean = false,
        bilingual: Boolean = false
    ): ImportResult {
        val fileName = getFileName(context, uri)
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return ImportResult(emptyList(), fileName, 0, "Could not open file stream")

            // Check if PDF (Magic: %PDF = 0x25, 0x50, 0x44, 0x46 or .pdf extension)
            val isPdf = fileName.endsWith(".pdf", ignoreCase = true) ||
                    (bytes.size >= 4 && bytes[0] == 0x25.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x44.toByte() && bytes[3] == 0x46.toByte())

            if (isPdf) {
                val pdfCourses = PdfScheduleHelper.parsePdfText(
                    context = context,
                    uri = uri,
                    semesterId = semesterId,
                    autoTranslateToEnglish = autoTranslateToEnglish,
                    bilingual = bilingual
                )
                return if (pdfCourses.isNotEmpty()) {
                    ImportResult(
                        courses = pdfCourses,
                        fileName = fileName,
                        rowCount = pdfCourses.size,
                        error = null,
                        wasTranslated = autoTranslateToEnglish,
                        translatedCount = if (autoTranslateToEnglish) pdfCourses.size else 0
                    )
                } else {
                    ImportResult(
                        courses = emptyList(),
                        fileName = fileName,
                        rowCount = 0,
                        error = "PDF contains graphical timetable table. You can use the Photo OCR tab to scan this schedule page with AI Vision!"
                    )
                }
            }

            // Check if XLSX (Zip file magic number: 0x50, 0x4B, 0x03, 0x04)
            val isXlsx = bytes.size >= 4 &&
                    bytes[0] == 0x50.toByte() &&
                    bytes[1] == 0x4B.toByte() &&
                    bytes[2] == 0x03.toByte() &&
                    bytes[3] == 0x04.toByte()

            val rows = if (isXlsx) {
                parseXlsx(ByteArrayInputStream(bytes))
            } else {
                parseCsvText(String(bytes, Charsets.UTF_8))
            }

            val rawCourses = if (isMatrixTimetable(rows)) {
                parseMatrixTimetable(rows, semesterId)
            } else if (isXlsx) {
                parseRowsToCourses(rows, semesterId)
            } else {
                val text = String(bytes, Charsets.UTF_8)
                val parsed = parseTextToCourses(text, semesterId, autoTranslateToEnglish = false)
                if (parsed.isNotEmpty()) parsed else parseRowsToCourses(rows, semesterId)
            }

            val (finalCourses, wasTrans, count) = if (autoTranslateToEnglish && rawCourses.isNotEmpty()) {
                val mode = if (bilingual) ScheduleTranslationEngine.TranslationMode.BILINGUAL else ScheduleTranslationEngine.TranslationMode.ENGLISH_ONLY
                val res = ScheduleTranslationEngine.translateCourses(rawCourses, mode)
                Triple(res.translatedCourses, res.translatedCourseCount > 0 || res.translatedClassroomCount > 0, res.translatedCourseCount + res.translatedClassroomCount)
            } else {
                Triple(rawCourses, false, 0)
            }

            ImportResult(
                courses = finalCourses,
                fileName = fileName,
                rowCount = if (finalCourses.isNotEmpty()) finalCourses.size else rows.size,
                error = if (finalCourses.isEmpty()) "No valid courses found in file" else null,
                wasTranslated = wasTrans,
                translatedCount = count
            )
        } catch (e: Exception) {
            ImportResult(emptyList(), fileName, 0, "Failed to parse: ${e.localizedMessage}")
        }
    }

    fun parseTextToCourses(
        rawText: String,
        semesterId: Long,
        autoTranslateToEnglish: Boolean = false,
        bilingual: Boolean = false
    ): List<CourseEntity> {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return emptyList()

        val rawCourses = when {
            trimmed.contains("星期") || trimmed.contains("周") || trimmed.contains("节") || trimmed.contains("16193") ||
                trimmed.contains("Monday", ignoreCase = true) || trimmed.contains("Period", ignoreCase = true) ||
                trimmed.contains("Weeks", ignoreCase = true) || trimmed.contains("Week", ignoreCase = true) -> {
                val blockParsed = parseChineseScheduleFormat(rawText, semesterId)
                if (blockParsed.isNotEmpty()) {
                    blockParsed
                } else {
                    val rows = parseCsvText(rawText)
                    val csvParsed = parseRowsToCourses(rows, semesterId)
                    if (csvParsed.isNotEmpty()) csvParsed else blockParsed
                }
            }
            else -> {
                val rows = parseCsvText(rawText)
                val csvParsed = parseRowsToCourses(rows, semesterId)
                if (csvParsed.isNotEmpty()) csvParsed else parseChineseScheduleFormat(rawText, semesterId)
            }
        }

        return if (autoTranslateToEnglish && rawCourses.isNotEmpty()) {
            val mode = if (bilingual) ScheduleTranslationEngine.TranslationMode.BILINGUAL else ScheduleTranslationEngine.TranslationMode.ENGLISH_ONLY
            ScheduleTranslationEngine.translateCourses(rawCourses, mode).translatedCourses
        } else {
            rawCourses
        }
    }

    fun parseChineseScheduleFormat(rawText: String, semesterId: Long): List<CourseEntity> {
        val courses = mutableListOf<CourseEntity>()
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var currentCourseName = ""
        var currentCourseCode = ""
        var currentInstructor = ""

        val weekRegex = Regex("""(?:weeks?[:\s]+)([0-9]+(?:-[0-9]+)?(?:,\s*[0-9]+(?:-[0-9]+)?)*)|(?:周次[:\s]*|第\s*)?([0-9]+(?:-[0-9]+)?(?:,\s*[0-9]+(?:-[0-9]+)?)*)\s*(?:周|weeks?\b)""", RegexOption.IGNORE_CASE)
        val dayRegex = Regex("""(星期[1-7一二三四五六日天]|周[1-7一二三四五六日天]|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday|Mon\b|Tue\b|Wed\b|Thu\b|Fri\b|Sat\b|Sun\b)""", RegexOption.IGNORE_CASE)
        val periodRegex = Regex("""(?:第(\d+)节?[-~到至]第?(\d+)节?|第(\d+)节|第(\d+)[-~到至](\d+)节?|(\d+)[-~到至](\d+)节|(\d+)节|periods?\s*(\d+)(?:\s*[-~到至]\s*(\d+))?)""", RegexOption.IGNORE_CASE)

        for (line in lines) {
            // Check if line contains course title or code e.g. "1619304040-计算机组成原理(国际学生)[2001]"
            if (line.contains("-") && (line.contains("(") || line.contains("（") || line.contains("[") || !line.contains("节"))) {
                val parts = line.split("-", limit = 2)
                if (parts.size == 2 && parts[0].trim().all { it.isDigit() }) {
                    currentCourseCode = parts[0].trim()
                    currentCourseName = parts[1].replace(Regex("""\[.*?\]|【.*?】"""), "").trim()
                } else if (!line.contains("节") && !line.contains("周") && !dayRegex.containsMatchIn(line)) {
                    currentCourseName = line.replace(Regex("""\[.*?\]|【.*?】"""), "").trim()
                }
            } else if (!line.contains("节") && !line.contains("周") && !dayRegex.containsMatchIn(line) && !periodRegex.containsMatchIn(line) && line.length in 2..45) {
                currentCourseName = line.replace(Regex("""\[.*?\]|【.*?】"""), "").trim()
            }

            // Check if line has schedule information
            if (dayRegex.containsMatchIn(line) || periodRegex.containsMatchIn(line)) {
                var nameForEntry = currentCourseName
                var codeForEntry = currentCourseCode

                // Check if inline course name e.g. "1619304040-计算机组成原理(国际学生) 1-10周..."
                if (line.contains("-") && line.contains("节")) {
                    val candidate = line.split("-", limit = 2)
                    if (candidate.size == 2 && candidate[0].trim().all { it.isDigit() }) {
                        codeForEntry = candidate[0].trim()
                        val rem = candidate[1]
                        val matchDay = dayRegex.find(rem)
                        val matchWeek = weekRegex.find(rem)
                        val firstToken = minOf(
                            matchDay?.range?.first ?: rem.length,
                            matchWeek?.range?.first ?: rem.length
                        )
                        if (firstToken in 1 until rem.length) {
                            nameForEntry = rem.substring(0, firstToken).replace(Regex("""\[.*?\]|【.*?】"""), "").trim().trim(',', ' ')
                        }
                    }
                }

                if (nameForEntry.isBlank()) {
                    nameForEntry = "Course ${courses.size + 1}"
                }

                // Extract Weeks
                val weekMatches = weekRegex.findAll(line).mapNotNull {
                    it.groupValues[1].ifEmpty { it.groupValues[2] }.trim(',', ' ').takeIf { s -> s.isNotBlank() }
                }.joinToString(",")
                val customWeeks = if (weekMatches.isNotBlank()) weekMatches else "1-18"

                // Extract Day
                val dayMatch = dayRegex.find(line)?.groupValues?.get(1) ?: "星期1"
                val dayOfWeek = parseDayOfWeek(dayMatch)

                // Extract Periods
                val periodMatch = periodRegex.find(line)
                val (startPeriod, endPeriod) = if (periodMatch != null) {
                    when {
                        periodMatch.groupValues[1].isNotBlank() -> {
                            val s = periodMatch.groupValues[1].toInt()
                            val e = periodMatch.groupValues[2].toIntOrNull() ?: s
                            Pair(s, e)
                        }
                        periodMatch.groupValues[3].isNotBlank() -> {
                            val s = periodMatch.groupValues[3].toInt()
                            Pair(s, s)
                        }
                        periodMatch.groupValues[4].isNotBlank() -> {
                            val s = periodMatch.groupValues[4].toInt()
                            val e = periodMatch.groupValues[5].toIntOrNull() ?: s
                            Pair(s, e)
                        }
                        periodMatch.groupValues[6].isNotBlank() -> {
                            val s = periodMatch.groupValues[6].toInt()
                            val e = periodMatch.groupValues[7].toIntOrNull() ?: s
                            Pair(s, e)
                        }
                        periodMatch.groupValues[8].isNotBlank() -> {
                            val s = periodMatch.groupValues[8].toInt()
                            Pair(s, s)
                        }
                        periodMatch.groupValues[9].isNotBlank() -> {
                            val s = periodMatch.groupValues[9].toInt()
                            val e = periodMatch.groupValues[10].toIntOrNull() ?: (s + 1)
                            Pair(s, e)
                        }
                        else -> Pair(1, 2)
                    }
                } else Pair(1, 2)

                // Extract Classroom
                var classroom = ""
                var instructorForEntry = currentInstructor
                if (line.contains("Room", ignoreCase = true)) {
                    val after = line.substring(line.indexOf("Room", ignoreCase = true) + 4).trim()
                    val match = Regex("""^[:\s]*([A-Za-z0-9\-_]+)""").find(after)
                    if (match != null) {
                        classroom = match.groupValues[1]
                    }
                }
                if (classroom.isBlank() && periodMatch != null) {
                    val afterPeriod = line.substring(periodMatch.range.last + 1).trim()
                    val tokens = afterPeriod.split(',', '，', ';', '；').map { it.trim() }.filter { it.isNotEmpty() }
                    if (tokens.isNotEmpty()) {
                        classroom = tokens[0]
                        if (tokens.size > 1 && instructorForEntry.isBlank()) {
                            instructorForEntry = tokens[1]
                        }
                    }
                }
                if (classroom.isBlank()) {
                    classroom = if (nameForEntry.contains("机房") || nameForEntry.contains("程序")) "软件机房" else "教学楼"
                }

                val sp = startPeriod.coerceIn(1, 12)
                val ep = endPeriod.coerceIn(sp, 12)
                val startTime = STANDARD_START_TIMES.getOrElse(sp - 1) { "08:00" }
                val endTime = STANDARD_END_TIMES.getOrElse(ep - 1) { "09:35" }

                courses.add(
                    CourseEntity(
                        semesterId = semesterId,
                        name = nameForEntry,
                        code = codeForEntry,
                        classroom = classroom,
                        instructor = instructorForEntry,
                        dayOfWeek = dayOfWeek,
                        startPeriod = sp,
                        endPeriod = ep,
                        startTime = startTime,
                        endTime = endTime,
                        weekRule = WeekRule.CUSTOM,
                        customWeeks = customWeeks,
                        colorHex = DEFAULT_PALETTE[courses.size % DEFAULT_PALETTE.size]
                    )
                )
            }
        }

        return courses
    }

    private fun parseCsvText(text: String): List<List<String>> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()

        // Auto-detect delimiter from first line
        val firstLine = lines.first()
        val commaCount = firstLine.count { it == ',' }
        val tabCount = firstLine.count { it == '\t' }
        val semiCount = firstLine.count { it == ';' }
        val delimiter = when {
            tabCount > commaCount && tabCount > semiCount -> '\t'
            semiCount > commaCount && semiCount > tabCount -> ';'
            else -> ','
        }

        return lines.map { line -> parseCsvLine(line, delimiter) }
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delimiter && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    private fun parseXlsx(inputStream: InputStream): List<List<String>> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name == "xl/sharedStrings.xml" || (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml"))) {
                    entries[name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val sharedStrings = mutableListOf<String>()
        entries["xl/sharedStrings.xml"]?.let { bytes ->
            try {
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
                var eventType = parser.eventType
                var inSi = false
                val currentSi = StringBuilder()
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    val tagName = parser.name
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            if (tagName == "si") {
                                inSi = true
                                currentSi.clear()
                            } else if (inSi && tagName == "t") {
                                currentSi.append(parser.nextText())
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (tagName == "si") {
                                sharedStrings.add(currentSi.toString())
                                inSi = false
                            }
                        }
                    }
                    eventType = parser.next()
                }
            } catch (_: Exception) {}
        }

        val sheetKey = entries.keys.firstOrNull { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
            ?: return emptyList()
        val sheetBytes = entries[sheetKey] ?: return emptyList()

        val rows = mutableListOf<List<String>>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(sheetBytes), "UTF-8")

            var eventType = parser.eventType
            var currentRowCells = mutableMapOf<Int, String>()
            var currentCellCol = 0
            var currentCellType = ""
            var inV = false
            var inIs = false
            var inT = false
            val cellText = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tagName) {
                            "row" -> {
                                currentRowCells = mutableMapOf()
                            }
                            "c" -> {
                                val ref = parser.getAttributeValue(null, "r") ?: ""
                                currentCellCol = columnLettersToIndex(ref)
                                currentCellType = parser.getAttributeValue(null, "t") ?: ""
                                cellText.clear()
                            }
                            "v" -> inV = true
                            "is" -> inIs = true
                            "t" -> inT = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inV || (inIs && inT)) {
                            cellText.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (tagName) {
                            "v" -> inV = false
                            "is" -> inIs = false
                            "t" -> inT = false
                            "c" -> {
                                val rawVal = cellText.toString().trim()
                                val finalVal = when (currentCellType) {
                                    "s" -> {
                                        val idx = rawVal.toIntOrNull()
                                        if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else rawVal
                                    }
                                    else -> rawVal
                                }
                                currentRowCells[currentCellCol] = finalVal
                            }
                            "row" -> {
                                if (currentRowCells.isNotEmpty()) {
                                    val maxCol = currentRowCells.keys.maxOrNull() ?: -1
                                    if (maxCol >= 0) {
                                        val rowList = (0..maxCol).map { col -> currentRowCells[col] ?: "" }
                                        rows.add(rowList)
                                    }
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}

        return rows
    }

    private fun columnLettersToIndex(ref: String): Int {
        var col = 0
        for (ch in ref) {
            if (ch in 'A'..'Z') {
                col = col * 26 + (ch - 'A' + 1)
            } else if (ch in 'a'..'z') {
                col = col * 26 + (ch - 'a' + 1)
            } else {
                break
            }
        }
        return (col - 1).coerceAtLeast(0)
    }

    fun isMatrixTimetable(rawText: String): Boolean = isMatrixTimetable(parseCsvText(rawText))

    fun parseMatrixTimetable(rawText: String, semesterId: Long): List<CourseEntity> =
        parseMatrixTimetable(parseCsvText(rawText), semesterId)

    fun isMatrixTimetable(rows: List<List<String>>): Boolean {
        if (rows.isEmpty()) return false
        val headerRow = rows.firstOrNull { row -> row.count { it.isNotBlank() } >= 3 } ?: return false
        var dayCount = 0
        for (cell in headerRow) {
            val lower = cell.lowercase().trim()
            if (lower.contains("周一") || lower.contains("周二") || lower.contains("周三") ||
                lower.contains("周四") || lower.contains("周五") || lower.contains("周六") || lower.contains("周日") ||
                lower.contains("星期一") || lower.contains("星期二") || lower.contains("星期三") ||
                lower.contains("星期四") || lower.contains("星期五") || lower.contains("星期六") || lower.contains("星期日") ||
                lower == "mon" || lower.startsWith("monday") || lower == "tue" || lower.startsWith("tuesday") ||
                lower == "wed" || lower.startsWith("wednesday") || lower == "thu" || lower.startsWith("thursday") ||
                lower == "fri" || lower.startsWith("friday") || lower == "sat" || lower.startsWith("saturday") ||
                lower == "sun" || lower.startsWith("sunday")
            ) {
                dayCount++
            }
        }
        return dayCount >= 3
    }

    fun parseMatrixTimetable(rows: List<List<String>>, semesterId: Long): List<CourseEntity> {
        if (rows.isEmpty()) return emptyList()
        val headerRowIndex = rows.indexOfFirst { row ->
            row.count { cell ->
                val lower = cell.lowercase().trim()
                lower.contains("周一") || lower.contains("周二") || lower.contains("周三") ||
                        lower.contains("mon") || lower.startsWith("monday")
            } >= 1
        }
        if (headerRowIndex == -1) return emptyList()

        val headerRow = rows[headerRowIndex]
        val colToDayMap = mutableMapOf<Int, Int>() // colIndex -> dayOfWeek (1..7)

        headerRow.forEachIndexed { colIdx, cell ->
            val lower = cell.lowercase().trim()
            val day = when {
                lower.contains("周一") || lower.contains("星期一") || lower.startsWith("mon") -> 1
                lower.contains("周二") || lower.contains("星期二") || lower.startsWith("tue") -> 2
                lower.contains("周三") || lower.contains("星期三") || lower.startsWith("wed") -> 3
                lower.contains("周四") || lower.contains("星期四") || lower.startsWith("thu") -> 4
                lower.contains("周五") || lower.contains("星期五") || lower.startsWith("fri") -> 5
                lower.contains("周六") || lower.contains("星期六") || lower.startsWith("sat") -> 6
                lower.contains("周日") || lower.contains("星期日") || lower.startsWith("sun") -> 7
                else -> null
            }
            if (day != null) {
                colToDayMap[colIdx] = day
            }
        }

        if (colToDayMap.isEmpty()) return emptyList()

        val rawEntries = mutableListOf<CourseEntity>()
        var periodCounter = 1

        for (r in (headerRowIndex + 1) until rows.size) {
            val row = rows[r]
            if (row.all { it.isBlank() }) continue

            val firstCell = row.firstOrNull()?.trim() ?: ""
            val periodMatch = Regex("""\b(\d{1,2})\b""").find(firstCell)
            val currentPeriod = periodMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1, 12)
                ?: periodCounter.coerceIn(1, 12)
            periodCounter = (currentPeriod + 1).coerceAtMost(12)

            for ((colIdx, dayOfWeek) in colToDayMap) {
                if (colIdx >= row.size) continue
                val cellText = row[colIdx].trim()
                if (cellText.isBlank()) continue

                val lines = cellText.split("\n", "\r", ";").map { it.trim() }.filter { it.isNotBlank() }
                if (lines.isEmpty()) continue

                var courseName = lines[0]
                var classroom = "TBA"
                var instructor = ""
                var weekRule = WeekRule.ALL
                var customWeeks = ""

                val parenMatch = Regex("""^(.*?)\s*[\(（](.*?)[\)）]$""").find(courseName)
                if (parenMatch != null) {
                    val candidateName = parenMatch.groupValues[1].trim()
                    val inside = parenMatch.groupValues[2].trim()
                    if (candidateName.isNotBlank()) {
                        courseName = candidateName
                        classroom = inside
                    }
                }

                for (line in lines.drop(1)) {
                    val weekMatch = Regex("""(\d{1,2}(?:-\d{1,2})?(?:,\s*\d{1,2}(?:-\d{1,2})?)*)\s*周?""").find(line)
                    if (weekMatch != null && (line.contains("周") || line.contains("w") || line.contains("-"))) {
                        customWeeks = weekMatch.groupValues[1]
                        weekRule = WeekRule.CUSTOM
                        continue
                    }
                    if (line.contains("单周")) {
                        weekRule = WeekRule.ODD
                        continue
                    }
                    if (line.contains("双周")) {
                        weekRule = WeekRule.EVEN
                        continue
                    }
                    if (line.contains("楼") || line.contains("室") || line.contains("房") || line.contains("Room") || line.contains("Bldg") || line.contains("Lab")) {
                        classroom = line
                        continue
                    }
                    if (line.contains("教") || line.contains("师") || line.contains("Prof") || line.contains("Dr.") || instructor.isEmpty()) {
                        instructor = line
                        continue
                    }
                }

                val startTime = when (currentPeriod) {
                    1 -> "08:00"; 3 -> "09:50"; 6 -> "14:30"; 8 -> "16:20"; 10 -> "19:00"
                    else -> String.format("%02d:00", 7 + currentPeriod)
                }
                val endTime = when (currentPeriod) {
                    2 -> "09:35"; 4 -> "11:25"; 5 -> "12:15"; 7 -> "16:05"; 9 -> "17:55"; 12 -> "21:25"
                    else -> String.format("%02d:45", 7 + currentPeriod)
                }

                rawEntries.add(
                    CourseEntity(
                        semesterId = semesterId,
                        name = courseName,
                        code = "",
                        classroom = classroom,
                        instructor = instructor,
                        dayOfWeek = dayOfWeek,
                        startPeriod = currentPeriod,
                        endPeriod = currentPeriod,
                        startTime = startTime,
                        endTime = endTime,
                        weekRule = weekRule,
                        customWeeks = customWeeks,
                        colorHex = DEFAULT_PALETTE[rawEntries.size % DEFAULT_PALETTE.size]
                    )
                )
            }
        }

        val mergedCourses = mutableListOf<CourseEntity>()
        val grouped = rawEntries.groupBy { "${it.dayOfWeek}_${it.name}_${it.classroom}_${it.customWeeks}_${it.weekRule}" }

        for ((_, list) in grouped) {
            val sorted = list.sortedBy { it.startPeriod }
            var current: CourseEntity? = null

            for (item in sorted) {
                if (current == null) {
                    current = item
                } else if (item.startPeriod == current.endPeriod + 1) {
                    current = current.copy(
                        endPeriod = item.endPeriod,
                        endTime = item.endTime
                    )
                } else {
                    mergedCourses.add(current)
                    current = item
                }
            }
            if (current != null) {
                mergedCourses.add(current)
            }
        }

        return mergedCourses
    }

    fun parseRowsToCourses(rows: List<List<String>>, semesterId: Long): List<CourseEntity> {
        if (rows.isEmpty()) return emptyList()

        val nonBlankRows = rows.filter { row -> row.any { it.isNotBlank() } }
        if (nonBlankRows.isEmpty()) return emptyList()

        val firstRow = nonBlankRows.first()
        val isHeader = isHeaderRow(firstRow)

        val headerMap = if (isHeader) detectHeaderColumns(firstRow) else null
        val dataRows = if (isHeader) nonBlankRows.drop(1) else nonBlankRows

        val courses = mutableListOf<CourseEntity>()

        dataRows.forEachIndexed { idx, row ->
            try {
                val course = if (headerMap != null) {
                    parseCourseWithHeaderMap(row, headerMap, semesterId, idx)
                } else {
                    parseCoursePositional(row, semesterId, idx)
                }
                if (course != null && course.name.isNotBlank()) {
                    courses.add(course)
                }
            } catch (_: Exception) {}
        }

        return courses
    }

    private fun isHeaderRow(row: List<String>): Boolean {
        val keywords = listOf(
            "course", "name", "课程", "科目", "code", "代码", "room", "classroom", "教室", "地点",
            "teacher", "instructor", "教师", "老师", "day", "星期", "周", "period", "section", "节",
            "time", "时间", "rule", "周次"
        )
        val matches = row.count { token ->
            val lower = token.lowercase()
            keywords.any { lower.contains(it) }
        }
        return matches >= 2
    }

    private fun detectHeaderColumns(headerRow: List<String>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        headerRow.forEachIndexed { index, title ->
            val lower = title.lowercase()
            when {
                lower.contains("code") || lower.contains("代码") -> map["code"] = index
                lower.contains("room") || lower.contains("class") || lower.contains("教室") || lower.contains("地点") -> map["room"] = index
                lower.contains("teacher") || lower.contains("instructor") || lower.contains("教师") || lower.contains("老师") -> map["instructor"] = index
                lower.contains("day") || lower.contains("星期") || lower.contains("周") && !lower.contains("周次") -> map["day"] = index
                lower.contains("start period") || lower.contains("开始节") || lower.contains("起始节") -> map["start_period"] = index
                lower.contains("end period") || lower.contains("结束节") -> map["end_period"] = index
                lower.contains("period") || lower.contains("section") || lower.contains("节次") || lower.contains("节") -> map["period"] = index
                lower.contains("start time") || lower.contains("开始时间") -> map["start_time"] = index
                lower.contains("end time") || lower.contains("结束时间") -> map["end_time"] = index
                lower.contains("custom") || lower.contains("周次") || (lower.contains("week") && !lower.contains("rule")) -> map["custom_weeks"] = index
                lower.contains("rule") || lower.contains("单双") -> map["rule"] = index
                lower.contains("color") || lower.contains("颜色") -> map["color"] = index
                lower.contains("course") || lower.contains("name") || lower.contains("课程") || lower.contains("科目") -> map["name"] = index
            }
        }
        return map
    }

    private fun parseCourseWithHeaderMap(
        row: List<String>,
        headerMap: Map<String, Int>,
        semesterId: Long,
        index: Int
    ): CourseEntity? {
        val name = headerMap["name"]?.let { row.getOrNull(it) }?.trim() ?: row.firstOrNull()?.trim() ?: return null
        if (name.isBlank()) return null

        val code = headerMap["code"]?.let { row.getOrNull(it) }?.trim() ?: ""
        val room = headerMap["room"]?.let { row.getOrNull(it) }?.trim() ?: ""
        val instructor = headerMap["instructor"]?.let { row.getOrNull(it) }?.trim() ?: ""

        val dayStr = headerMap["day"]?.let { row.getOrNull(it) }?.trim() ?: "1"
        val dayOfWeek = parseDayOfWeek(dayStr)

        var startPeriod = 1
        var endPeriod = 2

        if (headerMap.containsKey("start_period")) {
            startPeriod = headerMap["start_period"]?.let { row.getOrNull(it) }?.toIntOrNull() ?: 1
            endPeriod = headerMap["end_period"]?.let { row.getOrNull(it) }?.toIntOrNull() ?: (startPeriod + 1)
        } else if (headerMap.containsKey("period")) {
            val periodStr = headerMap["period"]?.let { row.getOrNull(it) }?.trim() ?: ""
            val (sp, ep) = parsePeriodsFromText(periodStr)
            startPeriod = sp
            endPeriod = ep
        }

        startPeriod = startPeriod.coerceIn(1, 12)
        endPeriod = endPeriod.coerceIn(startPeriod, 12)

        val defaultStartTime = STANDARD_START_TIMES.getOrElse(startPeriod - 1) { "08:00" }
        val defaultEndTime = STANDARD_END_TIMES.getOrElse(endPeriod - 1) { "09:35" }

        val startTime = headerMap["start_time"]?.let { row.getOrNull(it) }?.takeIf { it.isNotBlank() } ?: defaultStartTime
        val endTime = headerMap["end_time"]?.let { row.getOrNull(it) }?.takeIf { it.isNotBlank() } ?: defaultEndTime

        val ruleStr = headerMap["rule"]?.let { row.getOrNull(it) }?.trim() ?: "ALL"
        var (weekRule, parsedCustomWeeks) = parseWeekRuleAndWeeks(ruleStr)

        val customWeeksCol = headerMap["custom_weeks"]?.let { row.getOrNull(it) }?.trim() ?: ""
        val customWeeks = if (customWeeksCol.isNotBlank()) {
            weekRule = WeekRule.CUSTOM
            customWeeksCol
        } else {
            parsedCustomWeeks
        }

        val colorStr = headerMap["color"]?.let { row.getOrNull(it) }?.trim() ?: ""
        val colorHex = parseColor(colorStr, index)

        return CourseEntity(
            semesterId = semesterId,
            name = name,
            code = code,
            classroom = room,
            instructor = instructor,
            dayOfWeek = dayOfWeek,
            startPeriod = startPeriod,
            endPeriod = endPeriod,
            startTime = startTime,
            endTime = endTime,
            weekRule = weekRule,
            customWeeks = customWeeks,
            colorHex = colorHex
        )
    }

    private fun parseCoursePositional(row: List<String>, semesterId: Long, index: Int): CourseEntity? {
        if (row.isEmpty()) return null
        val name = row[0].trim()
        if (name.isBlank()) return null

        val code = row.getOrNull(1)?.trim() ?: ""
        val classroom = row.getOrNull(2)?.trim() ?: ""
        val instructor = row.getOrNull(3)?.trim() ?: ""
        val dayOfWeek = parseDayOfWeek(row.getOrNull(4)?.trim() ?: "1")

        var startPeriod = row.getOrNull(5)?.toIntOrNull() ?: 1
        var endPeriod = row.getOrNull(6)?.toIntOrNull() ?: (startPeriod + 1)

        startPeriod = startPeriod.coerceIn(1, 12)
        endPeriod = endPeriod.coerceIn(startPeriod, 12)

        val defaultStartTime = STANDARD_START_TIMES.getOrElse(startPeriod - 1) { "08:00" }
        val defaultEndTime = STANDARD_END_TIMES.getOrElse(endPeriod - 1) { "09:35" }

        val startTime = row.getOrNull(7)?.takeIf { it.isNotBlank() } ?: defaultStartTime
        val endTime = row.getOrNull(8)?.takeIf { it.isNotBlank() } ?: defaultEndTime

        val (weekRule, customWeeks) = parseWeekRuleAndWeeks(row.getOrNull(9)?.trim() ?: "ALL")
        val colorHex = parseColor(row.getOrNull(10)?.trim() ?: "", index)

        return CourseEntity(
            semesterId = semesterId,
            name = name,
            code = code,
            classroom = classroom,
            instructor = instructor,
            dayOfWeek = dayOfWeek,
            startPeriod = startPeriod,
            endPeriod = endPeriod,
            startTime = startTime,
            endTime = endTime,
            weekRule = weekRule,
            customWeeks = customWeeks,
            colorHex = colorHex
        )
    }

    private fun parseDayOfWeek(input: String): Int {
        val trimmed = input.trim().lowercase()
        return when {
            trimmed == "1" || trimmed == "mon" || trimmed == "monday" || trimmed == "一" ||
                trimmed.contains("周一") || trimmed.contains("星期一") || trimmed.contains("礼拜一") ||
                trimmed.contains("周1") || trimmed.contains("星期1") -> 1
            trimmed == "2" || trimmed == "tue" || trimmed == "tuesday" || trimmed == "二" ||
                trimmed.contains("周二") || trimmed.contains("星期二") || trimmed.contains("礼拜二") ||
                trimmed.contains("周2") || trimmed.contains("星期2") -> 2
            trimmed == "3" || trimmed == "wed" || trimmed == "wednesday" || trimmed == "三" ||
                trimmed.contains("周三") || trimmed.contains("星期三") || trimmed.contains("礼拜三") ||
                trimmed.contains("周3") || trimmed.contains("星期3") -> 3
            trimmed == "4" || trimmed == "thu" || trimmed == "thursday" || trimmed == "四" ||
                trimmed.contains("周四") || trimmed.contains("星期四") || trimmed.contains("礼拜四") ||
                trimmed.contains("周4") || trimmed.contains("星期4") -> 4
            trimmed == "5" || trimmed == "fri" || trimmed == "friday" || trimmed == "五" ||
                trimmed.contains("周五") || trimmed.contains("星期五") || trimmed.contains("礼拜五") ||
                trimmed.contains("周5") || trimmed.contains("星期5") -> 5
            trimmed == "6" || trimmed == "sat" || trimmed == "saturday" || trimmed == "六" ||
                trimmed.contains("周六") || trimmed.contains("星期六") || trimmed.contains("礼拜六") ||
                trimmed.contains("周6") || trimmed.contains("星期6") -> 6
            trimmed == "7" || trimmed == "sun" || trimmed == "sunday" || trimmed == "日" || trimmed == "天" ||
                trimmed.contains("周日") || trimmed.contains("周天") || trimmed.contains("星期日") || trimmed.contains("星期天") ||
                trimmed.contains("周7") || trimmed.contains("星期7") -> 7
            else -> {
                val digit = Regex("""[1-7]""").find(trimmed)?.value?.toIntOrNull()
                digit ?: input.toIntOrNull()?.coerceIn(1, 7) ?: 1
            }
        }
    }

    private fun parsePeriodsFromText(text: String): Pair<Int, Int> {
        val digits = Regex("""\d+""").findAll(text).map { it.value.toInt() }.toList()
        return when {
            digits.size >= 2 -> Pair(digits[0], digits[1])
            digits.size == 1 -> Pair(digits[0], digits[0] + 1)
            else -> Pair(1, 2)
        }
    }

    private fun parseWeekRuleAndWeeks(text: String): Pair<WeekRule, String> {
        val trimmed = text.trim()
        val upper = trimmed.uppercase()
        return when {
            upper.contains("ODD") || upper.contains("单") -> Pair(WeekRule.ODD, "")
            upper.contains("EVEN") || upper.contains("双") -> Pair(WeekRule.EVEN, "")
            Regex("""\d+""").containsMatchIn(trimmed) -> {
                val cleaned = trimmed.replace("周", "").replace("weeks", "").replace("week", "").replace(" ", "").trim()
                Pair(WeekRule.CUSTOM, cleaned)
            }
            else -> Pair(WeekRule.ALL, "")
        }
    }

    private fun parseColor(colorStr: String, index: Int): Long {
        if (colorStr.isNotBlank()) {
            try {
                val hex = if (colorStr.startsWith("#")) colorStr else "#$colorStr"
                return android.graphics.Color.parseColor(hex).toLong()
            } catch (_: Exception) {}
        }
        return DEFAULT_PALETTE[index % DEFAULT_PALETTE.size]
    }
}
