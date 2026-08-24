package com.example.presentation.editor

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockType
import com.example.data.model.EditorBlock
import com.example.data.model.FormattingState
import com.example.data.model.HeadingLevel
import com.example.data.model.TextAlignment
import com.example.presentation.editor.engine.RichTextEngine
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EditorPaper
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun EditorNotepadArea(
  planTitle: String,
  planDescription: String,
  blocks: List<EditorBlock>,
  activeBlockId: String?,
  formattingState: FormattingState,
  onBlockTextChanged: (String, String) -> Unit,
  onBlockFocusChanged: (String, TextRange) -> Unit,
  onBlockSelectionChanged: (String, TextRange) -> Unit,
  onEnterPressed: (String, Int) -> Unit,
  onBackspaceAtStart: (String) -> Unit,
  onToggleChecklist: (String) -> Unit,
  wordCount: Int,
  charCount: Int,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(EditorPaper)
      .imePadding()
  ) {
    // Scrollable writing sheet
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .verticalScroll(scrollState)
        .padding(horizontal = 20.dp, vertical = 16.dp)
        .testTag("editor_writing_sheet")
    ) {
      // Document Title banner matching the planning notepad style
      Text(
        text = planTitle.uppercase(),
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 2.sp,
          fontSize = 18.sp
        ),
        color = TextPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 10.dp)
          .testTag("editor_document_title_header")
      )

      if (planDescription.isNotBlank()) {
        Text(
          text = planDescription,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontStyle = FontStyle.Italic,
            fontSize = 13.sp
          ),
          color = TextTertiary,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
        )
      }

      // Notebook horizontal separator rule
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(DarkBorderSubtle)
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Blocks container
      if (blocks.isEmpty() || (blocks.size == 1 && blocks[0].text.isEmpty() && blocks[0].type == BlockType.PARAGRAPH)) {
        // Render empty state initial block with placeholder
        SingleBlockView(
          block = blocks.firstOrNull() ?: EditorBlock(),
          showPlaceholder = true,
          onTextChanged = { text -> onBlockTextChanged(blocks.firstOrNull()?.id ?: "", text) },
          onFocusChanged = { sel -> onBlockFocusChanged(blocks.firstOrNull()?.id ?: "", sel) },
          onSelectionChanged = { sel -> onBlockSelectionChanged(blocks.firstOrNull()?.id ?: "", sel) },
          onEnter = { cursor -> onEnterPressed(blocks.firstOrNull()?.id ?: "", cursor) },
          onBackspaceAtStart = { onBackspaceAtStart(blocks.firstOrNull()?.id ?: "") },
          onToggleChecklist = { onToggleChecklist(blocks.firstOrNull()?.id ?: "") }
        )
      } else {
        Column(
          verticalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          blocks.forEach { block ->
            SingleBlockView(
              block = block,
              showPlaceholder = false,
              onTextChanged = { text -> onBlockTextChanged(block.id, text) },
              onFocusChanged = { sel -> onBlockFocusChanged(block.id, sel) },
              onSelectionChanged = { sel -> onBlockSelectionChanged(block.id, sel) },
              onEnter = { cursor -> onEnterPressed(block.id, cursor) },
              onBackspaceAtStart = { onBackspaceAtStart(block.id) },
              onToggleChecklist = { onToggleChecklist(block.id) }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(100.dp))
    }

    // Bottom Stats Bar (Word count, char count, phase status)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(DarkSurface)
        .drawBehind {
          drawLine(
            color = DarkBorderSubtle,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx()
          )
        }
        .padding(horizontal = 16.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "$wordCount words  •  $charCount characters",
        style = MaterialTheme.typography.labelSmall,
        color = TextTertiary
      )
      Spacer(modifier = Modifier.weight(1f))
      Text(
        text = "Rich Text Planning Notepad",
        style = MaterialTheme.typography.labelSmall,
        color = TextTertiary
      )
    }
  }
}

@Composable
private fun SingleBlockView(
  block: EditorBlock,
  showPlaceholder: Boolean,
  onTextChanged: (String) -> Unit,
  onFocusChanged: (TextRange) -> Unit,
  onSelectionChanged: (TextRange) -> Unit,
  onEnter: (Int) -> Unit,
  onBackspaceAtStart: () -> Unit,
  onToggleChecklist: () -> Unit
) {
  // If Divider block, render horizontal visual separator
  if (block.type == BlockType.DIVIDER) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp),
      contentAlignment = Alignment.Center
    ) {
      HorizontalDivider(
        color = DarkBorder,
        thickness = 1.5.dp,
        modifier = Modifier.fillMaxWidth(0.9f)
      )
    }
    return
  }

  val annotatedText = remember(block.text, block.spans, block.headingLevel) {
    RichTextEngine.buildAnnotatedStringForBlock(block)
  }

  var textFieldValue by remember(block.id) {
    mutableStateOf(
      TextFieldValue(
        annotatedString = annotatedText,
        selection = TextRange(block.text.length)
      )
    )
  }

  // Keep internal text state synchronized with block updates
  LaunchedEffect(annotatedText) {
    if (textFieldValue.text != block.text) {
      textFieldValue = textFieldValue.copy(
        annotatedString = annotatedText,
        selection = TextRange(textFieldValue.selection.start.coerceIn(0, block.text.length))
      )
    }
  }

  val textAlign = when (block.alignment) {
    TextAlignment.LEFT -> TextAlign.Left
    TextAlignment.CENTER -> TextAlign.Center
    TextAlignment.RIGHT -> TextAlign.Right
    TextAlignment.JUSTIFY -> TextAlign.Justify
  }

  val baseFontSize = when (block.headingLevel) {
    HeadingLevel.H1 -> 24.sp
    HeadingLevel.H2 -> 20.sp
    HeadingLevel.H3 -> 18.sp
    HeadingLevel.H4 -> 16.sp
    HeadingLevel.H5 -> 15.sp
    HeadingLevel.NORMAL -> 16.sp
  }

  val baseFontWeight = when (block.headingLevel) {
    HeadingLevel.H1, HeadingLevel.H2 -> FontWeight.Bold
    HeadingLevel.H3 -> FontWeight.SemiBold
    HeadingLevel.H4 -> FontWeight.Medium
    HeadingLevel.H5, HeadingLevel.NORMAL -> FontWeight.Normal
  }

  val baseTextStyle = TextStyle(
    fontSize = baseFontSize,
    lineHeight = (baseFontSize.value * block.lineSpacing).sp,
    letterSpacing = block.letterSpacing.sp,
    fontWeight = baseFontWeight,
    color = if (block.type == BlockType.CHECKLIST && block.isChecked) TextTertiary else TextPrimary,
    textDecoration = if (block.type == BlockType.CHECKLIST && block.isChecked) TextDecoration.LineThrough else TextDecoration.None,
    textAlign = textAlign
  )

  val paddingBottom = block.paragraphSpacingDp.dp

  when (block.type) {
    BlockType.PARAGRAPH, BlockType.HEADING -> {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = paddingBottom)
      ) {
        if (block.text.isEmpty() && showPlaceholder) {
          Text(
            text = "Start writing your plan...",
            style = baseTextStyle.copy(color = TextTertiary),
            modifier = Modifier.padding(start = 2.dp, top = 2.dp)
          )
        }

        BasicTextField(
          value = textFieldValue,
          onValueChange = { newValue ->
            val prevText = textFieldValue.text
            textFieldValue = newValue
            if (newValue.text != prevText) {
              // Check if user pressed Enter
              if (newValue.text.contains("\n")) {
                val enterIndex = newValue.text.indexOf("\n")
                onEnter(enterIndex)
              } else {
                onTextChanged(newValue.text)
              }
            }
            if (newValue.selection != textFieldValue.selection) {
              onSelectionChanged(newValue.selection)
            }
          },
          textStyle = baseTextStyle,
          cursorBrush = SolidColor(AccentBlue),
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
          modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
              if (focusState.isFocused) {
                onFocusChanged(textFieldValue.selection)
              }
            }
            .onPreviewKeyEvent { event ->
              if (event.key == Key.Backspace && textFieldValue.selection.start == 0 && textFieldValue.selection.end == 0) {
                onBackspaceAtStart()
                true
              } else {
                false
              }
            }
            .testTag("editor_block_${block.id}")
        )
      }
    }

    BlockType.BULLET_LIST -> {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = paddingBottom),
        verticalAlignment = Alignment.Top
      ) {
        Text(
          text = "•",
          style = baseTextStyle.copy(fontWeight = FontWeight.Bold, color = AccentBlue),
          modifier = Modifier.padding(end = 10.dp, start = 4.dp)
        )
        Box(modifier = Modifier.weight(1f)) {
          if (block.text.isEmpty()) {
            Text(
              text = "List item...",
              style = baseTextStyle.copy(color = TextTertiary)
            )
          }
          BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
              val prevText = textFieldValue.text
              textFieldValue = newValue
              if (newValue.text != prevText) {
                if (newValue.text.contains("\n")) {
                  val enterIndex = newValue.text.indexOf("\n")
                  onEnter(enterIndex)
                } else {
                  onTextChanged(newValue.text)
                }
              }
              if (newValue.selection != textFieldValue.selection) {
                onSelectionChanged(newValue.selection)
              }
            },
            textStyle = baseTextStyle,
            cursorBrush = SolidColor(AccentBlue),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
              .fillMaxWidth()
              .onFocusChanged { focusState ->
                if (focusState.isFocused) onFocusChanged(textFieldValue.selection)
              }
              .onPreviewKeyEvent { event ->
                if (event.key == Key.Backspace && textFieldValue.selection.start == 0 && textFieldValue.selection.end == 0) {
                  onBackspaceAtStart()
                  true
                } else false
              }
              .testTag("editor_bullet_block_${block.id}")
          )
        }
      }
    }

    BlockType.NUMBERED_LIST -> {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = paddingBottom),
        verticalAlignment = Alignment.Top
      ) {
        Text(
          text = "${block.numberIndex}.",
          style = baseTextStyle.copy(fontWeight = FontWeight.Medium, color = AccentBlue),
          modifier = Modifier.padding(end = 10.dp, start = 2.dp)
        )
        Box(modifier = Modifier.weight(1f)) {
          if (block.text.isEmpty()) {
            Text(
              text = "Numbered item...",
              style = baseTextStyle.copy(color = TextTertiary)
            )
          }
          BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
              val prevText = textFieldValue.text
              textFieldValue = newValue
              if (newValue.text != prevText) {
                if (newValue.text.contains("\n")) {
                  val enterIndex = newValue.text.indexOf("\n")
                  onEnter(enterIndex)
                } else {
                  onTextChanged(newValue.text)
                }
              }
              if (newValue.selection != textFieldValue.selection) {
                onSelectionChanged(newValue.selection)
              }
            },
            textStyle = baseTextStyle,
            cursorBrush = SolidColor(AccentBlue),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
              .fillMaxWidth()
              .onFocusChanged { focusState ->
                if (focusState.isFocused) onFocusChanged(textFieldValue.selection)
              }
              .onPreviewKeyEvent { event ->
                if (event.key == Key.Backspace && textFieldValue.selection.start == 0 && textFieldValue.selection.end == 0) {
                  onBackspaceAtStart()
                  true
                } else false
              }
              .testTag("editor_numbered_block_${block.id}")
          )
        }
      }
    }

    BlockType.CHECKLIST -> {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = paddingBottom),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Interactive Checklist Box
        Box(
          modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (block.isChecked) AccentBlue else DarkSurface)
            .border(
              width = 1.5.dp,
              color = if (block.isChecked) AccentBlue else DarkBorder,
              shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onToggleChecklist)
            .testTag("checklist_checkbox_${block.id}"),
          contentAlignment = Alignment.Center
        ) {
          if (block.isChecked) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Completed",
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(modifier = Modifier.weight(1f)) {
          if (block.text.isEmpty()) {
            Text(
              text = "Checklist item...",
              style = baseTextStyle.copy(color = TextTertiary)
            )
          }
          BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
              val prevText = textFieldValue.text
              textFieldValue = newValue
              if (newValue.text != prevText) {
                if (newValue.text.contains("\n")) {
                  val enterIndex = newValue.text.indexOf("\n")
                  onEnter(enterIndex)
                } else {
                  onTextChanged(newValue.text)
                }
              }
              if (newValue.selection != textFieldValue.selection) {
                onSelectionChanged(newValue.selection)
              }
            },
            textStyle = baseTextStyle,
            cursorBrush = SolidColor(AccentBlue),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
              .fillMaxWidth()
              .onFocusChanged { focusState ->
                if (focusState.isFocused) onFocusChanged(textFieldValue.selection)
              }
              .onPreviewKeyEvent { event ->
                if (event.key == Key.Backspace && textFieldValue.selection.start == 0 && textFieldValue.selection.end == 0) {
                  onBackspaceAtStart()
                  true
                } else false
              }
              .testTag("editor_checklist_block_${block.id}")
          )
        }
      }
    }

    BlockType.QUOTE -> {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = paddingBottom)
          .clip(RoundedCornerShape(8.dp))
          .background(DarkSurfaceElevated)
          .border(
            width = 1.dp,
            color = DarkBorderSubtle,
            shape = RoundedCornerShape(8.dp)
          )
          .drawBehind {
            // Distinct left accent bar
            drawRect(
              color = AccentBlue,
              topLeft = Offset(0f, 0f),
              size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height)
            )
          }
          .padding(start = 16.dp, end = 14.dp, top = 10.dp, bottom = 10.dp)
      ) {
        if (block.text.isEmpty()) {
          Text(
            text = "Quote or planning note...",
            style = baseTextStyle.copy(color = TextTertiary, fontStyle = FontStyle.Italic)
          )
        }
        BasicTextField(
          value = textFieldValue,
          onValueChange = { newValue ->
            val prevText = textFieldValue.text
            textFieldValue = newValue
            if (newValue.text != prevText) {
              if (newValue.text.contains("\n")) {
                val enterIndex = newValue.text.indexOf("\n")
                onEnter(enterIndex)
              } else {
                onTextChanged(newValue.text)
              }
            }
            if (newValue.selection != textFieldValue.selection) {
              onSelectionChanged(newValue.selection)
            }
          },
          textStyle = baseTextStyle.copy(fontStyle = FontStyle.Italic),
          cursorBrush = SolidColor(AccentBlue),
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
          modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
              if (focusState.isFocused) onFocusChanged(textFieldValue.selection)
            }
            .onPreviewKeyEvent { event ->
              if (event.key == Key.Backspace && textFieldValue.selection.start == 0 && textFieldValue.selection.end == 0) {
                onBackspaceAtStart()
                true
              } else false
            }
            .testTag("editor_quote_block_${block.id}")
        )
      }
    }

    BlockType.DIVIDER -> {
      // Handled above
    }
  }
}
