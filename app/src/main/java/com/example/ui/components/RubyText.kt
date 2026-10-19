package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.PinyinEngine

/**
 * Single ruby unit: Displays tone-marked Pinyin above a character or ideograph.
 */
@Composable
fun RubyChar(
    char: Char,
    pinyin: String?,
    showPinyin: Boolean = true,
    textColor: Color = Color.Unspecified,
    pinyinColor: Color = Color.Unspecified,
    charSize: TextUnit = 14.sp,
    pinyinSize: TextUnit = 8.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(horizontal = 0.5.dp)
    ) {
        if (showPinyin && !pinyin.isNullOrBlank()) {
            Text(
                text = pinyin,
                fontSize = pinyinSize,
                lineHeight = pinyinSize * 1.1f,
                color = pinyinColor.takeOrElse { textColor.copy(alpha = 0.85f) },
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        } else if (showPinyin) {
            // Invisible placeholder for uniform baseline height
            Spacer(modifier = Modifier.height((pinyinSize.value + 1).dp))
        }

        Text(
            text = char.toString(),
            fontSize = charSize,
            lineHeight = charSize * 1.15f,
            color = textColor,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Horizontal Ruby Text: Renders inline characters with ruby Pinyin directly above each Hanzi character.
 * Ideal for headers, course detail cards, and form rows.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RubyText(
    text: String,
    showPinyin: Boolean = true,
    textColor: Color = Color.Unspecified,
    pinyinColor: Color = Color.Unspecified,
    charSize: TextUnit = 15.sp,
    pinyinSize: TextUnit = 9.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    modifier: Modifier = Modifier
) {
    val annotatedList = PinyinEngine.getAnnotatedText(text)

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.Center
    ) {
        annotatedList.forEach { (char, pinyin) ->
            RubyChar(
                char = char,
                pinyin = pinyin,
                showPinyin = showPinyin,
                textColor = textColor,
                pinyinColor = pinyinColor,
                charSize = charSize,
                pinyinSize = pinyinSize,
                fontWeight = fontWeight
            )
        }
    }
}

/**
 * Compact Ruby Text for Timetable Grid cards:
 * High-density layout tailored for vertical schedule cards.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompactRubyScheduleText(
    text: String,
    showPinyin: Boolean = true,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val annotatedList = PinyinEngine.getAnnotatedText(text)

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center,
        maxItemsInEachRow = 3
    ) {
        annotatedList.forEach { (char, pinyin) ->
            RubyChar(
                char = char,
                pinyin = pinyin,
                showPinyin = showPinyin,
                textColor = textColor,
                pinyinColor = textColor.copy(alpha = 0.9f),
                charSize = 11.sp,
                pinyinSize = 6.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun Color.takeOrElse(block: () -> Color): Color = if (this != Color.Unspecified) this else block()
