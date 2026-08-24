package com.example.data.model

import java.util.UUID

enum class HeadingLevel(val label: String, val defaultSizeSp: Int) {
  NORMAL("Normal", 16),
  H1("H1", 26),
  H2("H2", 22),
  H3("H3", 19),
  H4("H4", 17),
  H5("H5", 15)
}

enum class TextAlignment(val label: String) {
  LEFT("Left"),
  CENTER("Center"),
  RIGHT("Right"),
  JUSTIFY("Justify")
}

enum class BlockType(val label: String) {
  PARAGRAPH("Paragraph"),
  HEADING("Heading"),
  BULLET_LIST("Bullet List"),
  NUMBERED_LIST("Numbered List"),
  CHECKLIST("Checklist"),
  QUOTE("Quote"),
  DIVIDER("Divider")
}

enum class SpacingPreset(val label: String, val multiplier: Float) {
  COMPACT("Compact", 1.2f),
  NORMAL("Normal", 1.5f),
  RELAXED("Relaxed", 1.8f)
}

data class RichSpan(
  val start: Int,
  val end: Int,
  val isBold: Boolean = false,
  val isItalic: Boolean = false,
  val isUnderline: Boolean = false,
  val isStrikethrough: Boolean = false,
  val textColorHex: String? = null,
  val highlightColorHex: String? = null,
  val fontSizeSp: Int? = null
)

data class EditorBlock(
  val id: String = UUID.randomUUID().toString(),
  val type: BlockType = BlockType.PARAGRAPH,
  val text: String = "",
  val spans: List<RichSpan> = emptyList(),
  val headingLevel: HeadingLevel = HeadingLevel.NORMAL,
  val alignment: TextAlignment = TextAlignment.LEFT,
  val isChecked: Boolean = false,
  val numberIndex: Int = 1,
  val lineSpacing: Float = 1.4f,
  val letterSpacing: Float = 0.3f,
  val paragraphSpacingDp: Int = 8
)

data class FormattingState(
  val isBold: Boolean = false,
  val isItalic: Boolean = false,
  val isUnderline: Boolean = false,
  val isStrikethrough: Boolean = false,
  val headingLevel: HeadingLevel = HeadingLevel.NORMAL,
  val blockType: BlockType = BlockType.PARAGRAPH,
  val alignment: TextAlignment = TextAlignment.LEFT,
  val textColorHex: String = "#F1F3F7",
  val highlightColorHex: String? = null,
  val fontSizeSp: Int = 16,
  val letterSpacing: Float = 0.3f,
  val lineSpacing: Float = 1.4f,
  val paragraphSpacingDp: Int = 8,
  val canUndo: Boolean = false,
  val canRedo: Boolean = false
)

data class DocumentModel(
  val id: String = "plan_doc_1",
  val title: String = "Our Plan",
  val description: String = "",
  val blocks: List<EditorBlock> = listOf(
    EditorBlock(text = "")
  )
)
