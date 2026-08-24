package com.example.presentation.editor.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockType
import com.example.data.model.DocumentModel
import com.example.data.model.EditorBlock
import com.example.data.model.HeadingLevel
import com.example.data.model.RichSpan
import com.example.data.model.TextAlignment

object RichTextEngine {

  fun buildAnnotatedStringForBlock(block: EditorBlock): AnnotatedString {
    val text = block.text
    if (text.isEmpty()) return AnnotatedString("")

    return buildAnnotatedString {
      append(text)
      val textLength = text.length

      // Apply base block heading style if not covered by custom font size
      val defaultBaseSize = when (block.headingLevel) {
        HeadingLevel.H1 -> 26.sp
        HeadingLevel.H2 -> 22.sp
        HeadingLevel.H3 -> 19.sp
        HeadingLevel.H4 -> 17.sp
        HeadingLevel.H5 -> 15.sp
        HeadingLevel.NORMAL -> 16.sp
      }

      val defaultBaseWeight = when (block.headingLevel) {
        HeadingLevel.H1, HeadingLevel.H2 -> FontWeight.Bold
        HeadingLevel.H3 -> FontWeight.SemiBold
        HeadingLevel.H4 -> FontWeight.Medium
        HeadingLevel.H5, HeadingLevel.NORMAL -> FontWeight.Normal
      }

      if (block.headingLevel != HeadingLevel.NORMAL) {
        addStyle(
          SpanStyle(
            fontSize = defaultBaseSize,
            fontWeight = defaultBaseWeight
          ),
          0,
          textLength
        )
      }

      // Apply individual spans
      block.spans.forEach { span ->
        val safeStart = span.start.coerceIn(0, textLength)
        val safeEnd = span.end.coerceIn(safeStart, textLength)
        if (safeStart < safeEnd) {
          val weight = if (span.isBold) FontWeight.Bold else null
          val style = if (span.isItalic) FontStyle.Italic else null
          val decoration = when {
            span.isUnderline && span.isStrikethrough -> TextDecoration.combine(
              listOf(TextDecoration.Underline, TextDecoration.LineThrough)
            )
            span.isUnderline -> TextDecoration.Underline
            span.isStrikethrough -> TextDecoration.LineThrough
            else -> null
          }

          val color = span.textColorHex?.let {
            try {
              Color(android.graphics.Color.parseColor(it))
            } catch (e: Exception) {
              null
            }
          }

          val background = span.highlightColorHex?.let {
            try {
              Color(android.graphics.Color.parseColor(it))
            } catch (e: Exception) {
              null
            }
          }

          val fontSize = span.fontSizeSp?.sp

          addStyle(
            SpanStyle(
              fontWeight = weight,
              fontStyle = style,
              textDecoration = decoration,
              color = color ?: Color.Unspecified,
              background = background ?: Color.Transparent,
              fontSize = fontSize ?: androidx.compose.ui.unit.TextUnit.Unspecified
            ),
            safeStart,
            safeEnd
          )
        }
      }
    }
  }

  fun applySpanToRange(
    block: EditorBlock,
    start: Int,
    end: Int,
    transform: (current: RichSpan) -> RichSpan
  ): EditorBlock {
    val textLen = block.text.length
    val safeStart = start.coerceIn(0, textLen)
    val safeEnd = end.coerceIn(safeStart, textLen)

    if (safeStart >= safeEnd) {
      return block
    }

    // Check if entire range has a span or needs toggling
    val existingSpans = block.spans.toMutableList()
    val newSpans = mutableListOf<RichSpan>()

    // For spans outside the range, keep them
    // For overlapping spans, split and apply transform
    val newSegment = transform(
      RichSpan(
        start = safeStart,
        end = safeEnd,
        isBold = false,
        isItalic = false,
        isUnderline = false,
        isStrikethrough = false
      )
    )

    // Simplified clean span merger
    var applied = false
    for (s in existingSpans) {
      if (s.end <= safeStart || s.start >= safeEnd) {
        newSpans.add(s)
      } else {
        // Overlap: split
        if (s.start < safeStart) {
          newSpans.add(s.copy(end = safeStart))
        }
        val overlappedSegment = transform(s.copy(start = maxOf(s.start, safeStart), end = minOf(s.end, safeEnd)))
        newSpans.add(overlappedSegment)
        applied = true
        if (s.end > safeEnd) {
          newSpans.add(s.copy(start = safeEnd))
        }
      }
    }

    if (!applied) {
      newSpans.add(newSegment)
    }

    return block.copy(spans = consolidateSpans(newSpans))
  }

  private fun consolidateSpans(spans: List<RichSpan>): List<RichSpan> {
    return spans.filter { it.start < it.end }.sortedBy { it.start }
  }

  fun countWordsAndChars(blocks: List<EditorBlock>): Pair<Int, Int> {
    var words = 0
    var chars = 0
    for (block in blocks) {
      if (block.type == BlockType.DIVIDER) continue
      val text = block.text.trim()
      if (text.isNotEmpty()) {
        val blockWords = text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        words += blockWords
        chars += block.text.length
      }
    }
    return Pair(words, chars)
  }
}
