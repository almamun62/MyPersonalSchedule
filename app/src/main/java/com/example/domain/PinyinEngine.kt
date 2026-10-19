package com.example.domain

/**
 * 100% Offline Hanzi-to-Pinyin Converter Engine.
 * Provides character-by-character tone-marked Pinyin annotations (拼音注音)
 * for Chinese course titles, teachers, and classrooms.
 */
object PinyinEngine {

    // Common Chinese university & course schedule phonetic dictionary with tone marks
    private val pinyinDictionary: Map<Char, String> = mapOf(
        // Course: 计算机组成原理(国际学生)
        '计' to "jì", '算' to "suàn", '机' to "jī", '组' to "zǔ", '成' to "chéng",
        '原' to "yuán", '理' to "lǐ", '国' to "guó", '际' to "jì", '学' to "xué", '生' to "shēng",

        // Teacher: 徐媛媛, 刘, 张, 王, 李, 赵, 陈, 杨, 黄, 周, 吴, 孙, 朱, 钱, 郑
        '徐' to "xú", '媛' to "yuán", '刘' to "liú", '张' to "zhāng", '王' to "wáng",
        '李' to "lǐ", '赵' to "zhào", '陈' to "chén", '杨' to "yáng", '黄' to "huáng",
        '周' to "zhōu", '吴' to "wú", '孙' to "sūn", '朱' to "zhū", '钱' to "qián", '郑' to "zhèng",
        '郭' to "guō", '马' to "mǎ", '高' to "gāo", '林' to "lín", '罗' to "luó", '梁' to "liáng",
        '宋' to "sòng", '唐' to "táng", '韩' to "hán", '曹' to "cáo", '许' to "xǔ", '邓' to "dèng",
        '老' to "lǎo", '师' to "shī", '教' to "jiào", '授' to "shòu",

        // Location: 明理楼B105, 软件机房1, 实验楼, 教学楼
        '明' to "míng", '楼' to "lóu", '软' to "ruǎn", '件' to "jiàn",
        '房' to "fáng", '实' to "shí", '验' to "yàn", '室' to "shì", '主' to "zhǔ",
        '图' to "tú", '书' to "shū", '馆' to "guǎn", '体' to "tǐ", '育' to "yù",
        '东' to "dōng", '西' to "xī", '南' to "nán", '北' to "běi", '中' to "zhōng",
        '阶' to "jiē", '梯' to "tī", '操' to "cāo", '场' to "chǎng",

        // Courses in screenshots:
        // 数据分析与机器学习
        '数' to "shù", '据' to "jù", '分' to "fēn", '析' to "xī", '与' to "yǔ",
        '器' to "qì", '习' to "xí",

        // 数据库原理及应用
        '库' to "kù", '及' to "jí", '应' to "yìng", '用' to "yòng",

        // 面向数据科学的编程语言
        '面' to "miàn", '向' to "xiàng", '科' to "kē", '的' to "de",
        '编' to "biān", '程' to "chéng", '语' to "yǔ", '言' to "yán",

        // 神经网络与深度学习导论
        '神' to "shén", '经' to "jīng", '网' to "wǎng", '络' to "luò",
        '深' to "shēn", '度' to "dù", '导' to "dǎo", '论' to "lùn",

        // Other common courses & general university terms:
        // 高等数学, 线性代数, 概率论与数理统计
        '等' to "děng", '线' to "xiàn", '性' to "xìng", '代' to "dài",
        '概' to "gài", '率' to "lǜ", '统' to "tǒng",

        // 大学英语, 马克思主义基本原理, 中国近代史纲要, 思想道德与法治
        '大' to "dà", '英' to "yīng", '克' to "kè", '思' to "sī", '义' to "yì",
        '基' to "jī", '本' to "běn", '近' to "jìn", '史' to "shǐ", '纲' to "gāng",
        '要' to "yào", '想' to "xiǎng", '道' to "dào", '德' to "dé", '法' to "fǎ", '治' to "zhì",

        // 操作系统, 数据结构, 计算机网络, 软件工程, 人工智能
        '操' to "cāo", '作' to "zuò", '系' to "xì", '结' to "jié", '构' to "gòu",
        '工' to "gōng", '人' to "rén", '智' to "zhì", '能' to "néng",

        // 离散数学, 算法设计与分析, 编译原理, 移动应用开发
        '离' to "lí", '散' to "sàn", '算' to "suàn", '法' to "fǎ", '设' to "shè",
        '开' to "kāi", '发' to "fā", '移' to "yí", '动' to "dòng",

        // Python, Java, C语言, Web前端, 毕业设计
        '前' to "qián", '端' to "duān", '后' to "hòu", '毕' to "bì", '业' to "yè",

        // Days & Time: 星期一至星期日, 周一, 第八周, 节
        '星' to "xīng", '期' to "qī", '一' to "yī", '二' to "èr", '三' to "sān",
        '四' to "sì", '五' to "wǔ", '六' to "liù", '日' to "rì", '天' to "tiān",
        '第' to "dì", '八' to "bā", '九' to "jiǔ", '十' to "shí", '节' to "jié",
        '周' to "zhōu", '上' to "shàng", '午' to "wǔ", '下' to "xià", '晚' to "wǎn",

        // Additional common Hanzi
        '文' to "wén", '化' to "huà", '历' to "lì", '地' to "dì", '物' to "wù",
        '化' to "huà", '生' to "shēng", '政' to "zhèng", '经' to "jīng", '管' to "guǎn",
        '综' to "zōng", '合' to "hé", '创' to "chuàng", '新' to "xīn", '创' to "chuàng",
        '实' to "shí", '践' to "jiàn", '训' to "xùn", '练' to "liàn", '测' to "cè",
        '考' to "kǎo", '试' to "shì", '作' to "zuò", '业' to "yè", '论' to "lùn",
        '文' to "wén", '答' to "dá", '辩' to "biàn", '讲' to "jiǎng", '座' to "zuò"
    )

    /**
     * Checks if a character is a Chinese Hanzi ideograph.
     */
    fun isChinese(c: Char): Boolean {
        val ub = Character.UnicodeBlock.of(c)
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
                ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
    }

    /**
     * Gets Pinyin representation for a single character.
     * Returns null if non-Chinese or if punctuation.
     */
    fun getPinyin(c: Char): String? {
        if (!isChinese(c)) return null
        return pinyinDictionary[c] ?: getFallbackPinyin(c)
    }

    /**
     * Converts a text string into an aligned list of (Character, Pinyin?) pairs.
     * Non-Chinese characters return null for Pinyin.
     */
    fun getAnnotatedText(text: String): List<Pair<Char, String?>> {
        return text.map { char ->
            val pinyin = if (isChinese(char)) {
                pinyinDictionary[char] ?: getFallbackPinyin(char)
            } else {
                null
            }
            Pair(char, pinyin)
        }
    }

    /**
     * Formats full Pinyin string with tone marks for text previews.
     */
    fun toFullPinyinString(text: String): String {
        val tokens = mutableListOf<String>()
        var pendingAscii = StringBuilder()

        for (c in text) {
            if (isChinese(c)) {
                if (pendingAscii.isNotEmpty()) {
                    tokens.add(pendingAscii.toString())
                    pendingAscii.clear()
                }
                tokens.add(pinyinDictionary[c] ?: getFallbackPinyin(c))
            } else {
                pendingAscii.append(c)
            }
        }
        if (pendingAscii.isNotEmpty()) {
            tokens.add(pendingAscii.toString())
        }
        return tokens.joinToString(" ")
    }

    private fun getFallbackPinyin(c: Char): String {
        // Fallback tone-neutral or generic marker for unlisted characters
        return ""
    }
}
