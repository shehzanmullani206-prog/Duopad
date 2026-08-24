package com.example.presentation.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BlockType
import com.example.data.model.FormattingState
import com.example.data.model.HeadingLevel
import com.example.data.model.TextAlignment
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.ToolbarBackground
import com.example.ui.theme.ToolbarBorder

@Composable
fun EditorLeftToolbar(
  formattingState: FormattingState,
  isExpanded: Boolean,
  onToggleExpanded: () -> Unit,
  onToggleBold: () -> Unit,
  onToggleItalic: () -> Unit,
  onToggleUnderline: () -> Unit,
  onToggleStrikethrough: () -> Unit,
  onHeadingSelect: (HeadingLevel) -> Unit,
  onBlockTypeSelect: (BlockType) -> Unit,
  onInsertDivider: () -> Unit,
  onAlignmentSelect: (TextAlignment) -> Unit,
  onTextColorSelect: (String) -> Unit,
  onToggleHighlight: (String) -> Unit,
  onFontSizeSelect: (Int) -> Unit,
  onAdjustLetterSpacing: (Float) -> Unit,
  onLineSpacingSelect: (Float) -> Unit,
  onParagraphSpacingSelect: (Int) -> Unit,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onHistoryClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showColorPicker by remember { mutableStateOf(false) }
  var showHighlightPicker by remember { mutableStateOf(false) }
  var showFontSizePicker by remember { mutableStateOf(false) }
  var showHeadingsPicker by remember { mutableStateOf(false) }
  var showSpacingDialog by remember { mutableStateOf(false) }

  val toolbarWidth = if (isExpanded) 56.dp else 44.dp

  Surface(
    color = ToolbarBackground,
    modifier = modifier
      .width(toolbarWidth)
      .fillMaxHeight()
      .drawBehind {
        drawLine(
          color = ToolbarBorder,
          start = Offset(size.width, 0f),
          end = Offset(size.width, size.height),
          strokeWidth = 1.dp.toPx()
        )
      }
      .testTag("editor_left_toolbar")
  ) {
    Column(
      modifier = Modifier
        .fillMaxHeight()
        .verticalScroll(rememberScrollState())
        .padding(vertical = 8.dp, horizontal = if (isExpanded) 6.dp else 2.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      // 1. Expand / Collapse Mini Button
      ToolbarIconButton(
        icon = if (isExpanded) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
        contentDescription = if (isExpanded) "Collapse Toolbar" else "Expand Toolbar",
        onClick = onToggleExpanded,
        testTag = "toolbar_expand_collapse_button"
      )

      ToolbarDivider()

      // 2. Undo & Redo & History
      ToolbarIconButton(
        icon = Icons.AutoMirrored.Filled.Undo,
        contentDescription = "Undo",
        onClick = onUndo,
        enabled = formattingState.canUndo,
        testTag = "toolbar_undo_button"
      )

      ToolbarIconButton(
        icon = Icons.AutoMirrored.Filled.Redo,
        contentDescription = "Redo",
        onClick = onRedo,
        enabled = formattingState.canRedo,
        testTag = "toolbar_redo_button"
      )

      ToolbarIconButton(
        icon = Icons.Default.History,
        contentDescription = "History",
        onClick = onHistoryClick,
        testTag = "toolbar_history_button"
      )

      ToolbarDivider()

      // 3. Text Formatting (B, I, U, S)
      ToolbarTextButton(
        label = "B",
        fontWeight = FontWeight.Bold,
        contentDescription = "Bold",
        isSelected = formattingState.isBold,
        onClick = onToggleBold,
        testTag = "toolbar_bold_button"
      )

      ToolbarTextButton(
        label = "I",
        fontStyle = FontStyle.Italic,
        contentDescription = "Italic",
        isSelected = formattingState.isItalic,
        onClick = onToggleItalic,
        testTag = "toolbar_italic_button"
      )

      ToolbarTextButton(
        label = "U",
        hasUnderline = true,
        contentDescription = "Underline",
        isSelected = formattingState.isUnderline,
        onClick = onToggleUnderline,
        testTag = "toolbar_underline_button"
      )

      ToolbarTextButton(
        label = "S",
        hasStrikethrough = true,
        contentDescription = "Strikethrough",
        isSelected = formattingState.isStrikethrough,
        onClick = onToggleStrikethrough,
        testTag = "toolbar_strikethrough_button"
      )

      ToolbarDivider()

      // 4. Headings & Typography (H1-H5 selector)
      ToolbarTextButton(
        label = if (formattingState.headingLevel != HeadingLevel.NORMAL) formattingState.headingLevel.label else "H",
        fontWeight = FontWeight.Bold,
        contentDescription = "Headings H1-H5",
        isSelected = formattingState.headingLevel != HeadingLevel.NORMAL,
        onClick = { showHeadingsPicker = true },
        testTag = "toolbar_headings_popup_button"
      )

      // Font Size Selector
      ToolbarIconButton(
        icon = Icons.Default.FormatSize,
        contentDescription = "Font Size Selector",
        onClick = { showFontSizePicker = true },
        isSelected = formattingState.fontSizeSp != 16,
        testTag = "toolbar_font_size_button"
      )

      ToolbarDivider()

      // 5. Planning Block Elements: Bullet, Numbered, Checklist, Quote, Divider
      ToolbarIconButton(
        icon = Icons.AutoMirrored.Filled.FormatListBulleted,
        contentDescription = "Bullet List",
        onClick = { onBlockTypeSelect(BlockType.BULLET_LIST) },
        isSelected = formattingState.blockType == BlockType.BULLET_LIST,
        testTag = "toolbar_bullet_list_button"
      )

      ToolbarIconButton(
        icon = Icons.Default.FormatListNumbered,
        contentDescription = "Numbered List",
        onClick = { onBlockTypeSelect(BlockType.NUMBERED_LIST) },
        isSelected = formattingState.blockType == BlockType.NUMBERED_LIST,
        testTag = "toolbar_numbered_list_button"
      )

      ToolbarIconButton(
        icon = Icons.Default.CheckBox,
        contentDescription = "Interactive Checklist",
        onClick = { onBlockTypeSelect(BlockType.CHECKLIST) },
        isSelected = formattingState.blockType == BlockType.CHECKLIST,
        testTag = "toolbar_checklist_button"
      )

      ToolbarIconButton(
        icon = Icons.Default.FormatQuote,
        contentDescription = "Quote Block",
        onClick = { onBlockTypeSelect(BlockType.QUOTE) },
        isSelected = formattingState.blockType == BlockType.QUOTE,
        testTag = "toolbar_quote_button"
      )

      ToolbarIconButton(
        icon = Icons.Default.HorizontalRule,
        contentDescription = "Insert Divider",
        onClick = onInsertDivider,
        testTag = "toolbar_divider_button"
      )

      ToolbarDivider()

      // 6. Color & User Highlight Palette
      ToolbarIconButton(
        icon = Icons.Default.Palette,
        contentDescription = "Text Color Palette",
        onClick = { showColorPicker = true },
        isSelected = formattingState.textColorHex != "#F1F3F7",
        testTag = "toolbar_color_palette_button"
      )

      ToolbarIconButton(
        icon = Icons.Default.FormatColorFill,
        contentDescription = "Highlight Text",
        onClick = { showHighlightPicker = true },
        isSelected = formattingState.highlightColorHex != null,
        testTag = "toolbar_highlight_button"
      )

      ToolbarDivider()

      // 7. Paragraph Alignment (Left, Center, Right, Justify)
      ToolbarIconButton(
        icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
        contentDescription = "Align Left",
        onClick = { onAlignmentSelect(TextAlignment.LEFT) },
        isSelected = formattingState.alignment == TextAlignment.LEFT,
        testTag = "toolbar_align_left_button"
      )

      ToolbarIconButton(
        icon = Icons.Default.FormatAlignCenter,
        contentDescription = "Align Center",
        onClick = { onAlignmentSelect(TextAlignment.CENTER) },
        isSelected = formattingState.alignment == TextAlignment.CENTER,
        testTag = "toolbar_align_center_button"
      )

      ToolbarIconButton(
        icon = Icons.AutoMirrored.Filled.FormatAlignRight,
        contentDescription = "Align Right",
        onClick = { onAlignmentSelect(TextAlignment.RIGHT) },
        isSelected = formattingState.alignment == TextAlignment.RIGHT,
        testTag = "toolbar_align_right_button"
      )

      ToolbarIconButton(
        icon = Icons.Default.FormatAlignJustify,
        contentDescription = "Align Justify",
        onClick = { onAlignmentSelect(TextAlignment.JUSTIFY) },
        isSelected = formattingState.alignment == TextAlignment.JUSTIFY,
        testTag = "toolbar_align_justify_button"
      )

      ToolbarDivider()

      // 8. Line, Letter, and Paragraph Spacing Controls
      ToolbarIconButton(
        icon = Icons.Default.FormatLineSpacing,
        contentDescription = "Spacing Controls",
        onClick = { showSpacingDialog = true },
        testTag = "toolbar_spacing_button"
      )
    }
  }

  // --- POPUPS & DIALOGS ---

  // Headings H1-H5 Dialog
  if (showHeadingsPicker) {
    HeadingsDialog(
      currentLevel = formattingState.headingLevel,
      onHeadingSelected = {
        onHeadingSelect(it)
        showHeadingsPicker = false
      },
      onDismiss = { showHeadingsPicker = false }
    )
  }

  // Font Size Dialog
  if (showFontSizePicker) {
    FontSizeDialog(
      currentSize = formattingState.fontSizeSp,
      onSizeSelected = {
        onFontSizeSelect(it)
        showFontSizePicker = false
      },
      onDismiss = { showFontSizePicker = false }
    )
  }

  // Text Color Palette Dialog
  if (showColorPicker) {
    ColorPickerDialog(
      currentColor = formattingState.textColorHex,
      onColorSelected = {
        onTextColorSelect(it)
        showColorPicker = false
      },
      onDismiss = { showColorPicker = false }
    )
  }

  // Highlight Palette Dialog (User formatting highlight)
  if (showHighlightPicker) {
    HighlightPickerDialog(
      currentHighlight = formattingState.highlightColorHex,
      onHighlightSelected = {
        onToggleHighlight(it)
        showHighlightPicker = false
      },
      onClearHighlight = {
        if (formattingState.highlightColorHex != null) {
          onToggleHighlight(formattingState.highlightColorHex)
        }
        showHighlightPicker = false
      },
      onDismiss = { showHighlightPicker = false }
    )
  }

  // Spacing Controls Dialog (Letter, Line, Paragraph Spacing)
  if (showSpacingDialog) {
    SpacingDialog(
      formattingState = formattingState,
      onAdjustLetterSpacing = onAdjustLetterSpacing,
      onLineSpacingSelect = onLineSpacingSelect,
      onParagraphSpacingSelect = onParagraphSpacingSelect,
      onDismiss = { showSpacingDialog = false }
    )
  }
}

@Composable
private fun ToolbarDivider() {
  HorizontalDivider(
    color = DarkBorderSubtle,
    thickness = 1.dp,
    modifier = Modifier
      .padding(vertical = 4.dp, horizontal = 4.dp)
      .fillMaxWidth()
  )
}

@Composable
private fun ToolbarIconButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isSelected: Boolean = false,
  enabled: Boolean = true,
  testTag: String
) {
  Box(
    modifier = modifier
      .size(40.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(if (isSelected) DarkSurfaceHighlight else Color.Transparent)
      .border(
        width = 1.dp,
        color = if (isSelected) AccentBlue else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
      )
      .clickable(enabled = enabled, onClick = onClick)
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = when {
        !enabled -> TextTertiary
        isSelected -> AccentBlue
        else -> TextSecondary
      },
      modifier = Modifier.size(19.dp)
    )
  }
}

@Composable
private fun ToolbarTextButton(
  label: String,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  fontWeight: FontWeight = FontWeight.Normal,
  fontStyle: FontStyle = FontStyle.Normal,
  hasUnderline: Boolean = false,
  hasStrikethrough: Boolean = false,
  isSelected: Boolean = false,
  testTag: String
) {
  Box(
    modifier = modifier
      .size(40.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(if (isSelected) DarkSurfaceHighlight else Color.Transparent)
      .border(
        width = 1.dp,
        color = if (isSelected) AccentBlue else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
      )
      .clickable(onClick = onClick)
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge.copy(
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        fontSize = if (label.length > 1) 12.sp else 14.sp
      ),
      color = if (isSelected) AccentBlue else TextSecondary,
      textAlign = TextAlign.Center
    )
  }
}

@Composable
private fun HeadingsDialog(
  currentLevel: HeadingLevel,
  onHeadingSelected: (HeadingLevel) -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = DarkSurfaceElevated,
      border = BorderStroke(1.dp, DarkBorder),
      modifier = Modifier.padding(16.dp)
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = "Block Heading Style",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))

        HeadingLevel.values().forEach { level ->
          val isSelected = currentLevel == level
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) DarkSurfaceHighlight else Color.Transparent)
              .border(1.dp, if (isSelected) AccentBlue else Color.Transparent, RoundedCornerShape(10.dp))
              .clickable { onHeadingSelected(level) }
              .padding(horizontal = 14.dp, vertical = 10.dp)
              .testTag("heading_option_${level.name}")
          ) {
            Text(
              text = level.label,
              style = when (level) {
                HeadingLevel.H1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp)
                HeadingLevel.H2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                HeadingLevel.H3 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                HeadingLevel.H4 -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp)
                HeadingLevel.H5 -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal, fontSize = 14.sp)
                HeadingLevel.NORMAL -> MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp)
              },
              color = if (isSelected) AccentBlue else TextPrimary,
              modifier = Modifier.weight(1f)
            )
            if (isSelected) {
              Text("✓", color = AccentBlue, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FontSizeDialog(
  currentSize: Int,
  onSizeSelected: (Int) -> Unit,
  onDismiss: () -> Unit
) {
  val sizes = listOf(
    14 to "Small (14 sp)",
    16 to "Normal (16 sp)",
    20 to "Large (20 sp)",
    24 to "Extra Large (24 sp)"
  )

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = DarkSurfaceElevated,
      border = BorderStroke(1.dp, DarkBorder),
      modifier = Modifier.padding(16.dp)
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = "Text Font Size",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))

        sizes.forEach { (sizeSp, label) ->
          val isSelected = currentSize == sizeSp
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) DarkSurfaceHighlight else Color.Transparent)
              .border(1.dp, if (isSelected) AccentBlue else Color.Transparent, RoundedCornerShape(10.dp))
              .clickable { onSizeSelected(sizeSp) }
              .padding(horizontal = 14.dp, vertical = 10.dp)
              .testTag("font_size_option_$sizeSp")
          ) {
            Text(
              text = label,
              style = MaterialTheme.typography.bodyLarge.copy(fontSize = sizeSp.sp),
              color = if (isSelected) AccentBlue else TextPrimary,
              modifier = Modifier.weight(1f)
            )
            if (isSelected) {
              Text("✓", color = AccentBlue, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ColorPickerDialog(
  currentColor: String,
  onColorSelected: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val colors = listOf(
    "#F1F3F7" to "Default",
    "#FFFFFF" to "Pure White",
    "#94A3B8" to "Muted Slate",
    "#60A5FA" to "Slate Blue",
    "#34D399" to "Emerald Green",
    "#FBBF24" to "Amber Gold",
    "#FB923C" to "Warm Orange",
    "#F87171" to "Coral Red",
    "#C084FC" to "Purple"
  )

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = DarkSurfaceElevated,
      border = BorderStroke(1.dp, DarkBorder),
      modifier = Modifier.padding(16.dp)
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Text Color",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          // Row 1
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            colors.take(5).forEach { (hex, name) ->
              ColorCircle(hex = hex, isSelected = currentColor.equals(hex, ignoreCase = true)) {
                onColorSelected(hex)
              }
            }
          }
          // Row 2
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            colors.drop(5).forEach { (hex, name) ->
              ColorCircle(hex = hex, isSelected = currentColor.equals(hex, ignoreCase = true)) {
                onColorSelected(hex)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "Select active font color",
          style = MaterialTheme.typography.labelSmall,
          color = TextTertiary
        )
      }
    }
  }
}

@Composable
private fun ColorCircle(
  hex: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val color = Color(android.graphics.Color.parseColor(hex))
  Box(
    modifier = Modifier
      .size(38.dp)
      .clip(CircleShape)
      .background(color)
      .border(
        width = if (isSelected) 2.5.dp else 1.dp,
        color = if (isSelected) AccentBlue else DarkBorder,
        shape = CircleShape
      )
      .clickable(onClick = onClick)
      .testTag("color_item_$hex"),
    contentAlignment = Alignment.Center
  ) {
    if (isSelected) {
      Text(
        text = "✓",
        color = if (hex == "#FFFFFF" || hex == "#F1F3F7") Color.Black else Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp
      )
    }
  }
}

@Composable
private fun HighlightPickerDialog(
  currentHighlight: String?,
  onHighlightSelected: (String) -> Unit,
  onClearHighlight: () -> Unit,
  onDismiss: () -> Unit
) {
  val highlights = listOf(
    "#FDE047" to "Yellow",
    "#86EFAC" to "Green",
    "#93C5FD" to "Blue",
    "#FDBA74" to "Orange",
    "#F472B6" to "Pink"
  )

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = DarkSurfaceElevated,
      border = BorderStroke(1.dp, DarkBorder),
      modifier = Modifier.padding(16.dp)
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Text Highlight",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Clear highlight option
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(DarkSurface)
              .border(1.dp, if (currentHighlight == null) AccentBlue else DarkBorder, CircleShape)
              .clickable(onClick = onClearHighlight)
              .testTag("highlight_none"),
            contentAlignment = Alignment.Center
          ) {
            Text("✕", color = TextTertiary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }

          highlights.forEach { (hex, _) ->
            val color = Color(android.graphics.Color.parseColor(hex))
            val isSelected = currentHighlight.equals(hex, ignoreCase = true)
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                  width = if (isSelected) 2.5.dp else 1.dp,
                  color = if (isSelected) AccentBlue else DarkBorder,
                  shape = CircleShape
                )
                .clickable { onHighlightSelected(hex) }
                .testTag("highlight_item_$hex"),
              contentAlignment = Alignment.Center
            ) {
              if (isSelected) {
                Text("✓", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "Normal user text highlight",
          style = MaterialTheme.typography.labelSmall,
          color = TextTertiary
        )
      }
    }
  }
}

@Composable
private fun SpacingDialog(
  formattingState: FormattingState,
  onAdjustLetterSpacing: (Float) -> Unit,
  onLineSpacingSelect: (Float) -> Unit,
  onParagraphSpacingSelect: (Int) -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = DarkSurfaceElevated,
      border = BorderStroke(1.dp, DarkBorder),
      modifier = Modifier.padding(16.dp)
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = "Spacing & Typography Controls",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = TextPrimary
        )

        // 1. Line Spacing Presets
        Column {
          Text(
            text = "Line Spacing",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val presets = listOf(
              1.2f to "Compact",
              1.5f to "Normal",
              1.8f to "Relaxed"
            )
            presets.forEach { (multiplier, label) ->
              val isSelected = Math.abs(formattingState.lineSpacing - multiplier) < 0.05f
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSelected) DarkSurfaceHighlight else DarkSurface)
                  .border(1.dp, if (isSelected) AccentBlue else DarkBorderSubtle, RoundedCornerShape(8.dp))
                  .clickable { onLineSpacingSelect(multiplier) }
                  .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                  color = if (isSelected) AccentBlue else TextPrimary
                )
              }
            }
          }
        }

        // 2. Letter Spacing Controls
        Column {
          Text(
            text = "Letter Spacing (${String.format("%.1f", formattingState.letterSpacing)} sp)",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = { onAdjustLetterSpacing(-0.1f) },
              modifier = Modifier.size(36.dp).background(DarkSurface, RoundedCornerShape(8.dp))
            ) {
              Text("-", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Text(
              text = "${(formattingState.letterSpacing * 10).toInt() / 10f}",
              color = TextPrimary,
              style = MaterialTheme.typography.labelLarge
            )
            IconButton(
              onClick = { onAdjustLetterSpacing(0.1f) },
              modifier = Modifier.size(36.dp).background(DarkSurface, RoundedCornerShape(8.dp))
            ) {
              Text("+", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
          }
        }

        // 3. Paragraph Spacing Presets
        Column {
          Text(
            text = "Paragraph Spacing",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val pSpacings = listOf(
              4 to "Tight",
              8 to "Normal",
              16 to "Spacious"
            )
            pSpacings.forEach { (dpVal, label) ->
              val isSelected = formattingState.paragraphSpacingDp == dpVal
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSelected) DarkSurfaceHighlight else DarkSurface)
                  .border(1.dp, if (isSelected) AccentBlue else DarkBorderSubtle, RoundedCornerShape(8.dp))
                  .clickable { onParagraphSpacingSelect(dpVal) }
                  .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                  color = if (isSelected) AccentBlue else TextPrimary
                )
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          Text(
            text = "Done",
            color = AccentBlue,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier
              .clickable(onClick = onDismiss)
              .padding(8.dp)
          )
        }
      }
    }
  }
}
