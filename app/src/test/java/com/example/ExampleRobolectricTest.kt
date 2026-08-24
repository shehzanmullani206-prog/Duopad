package com.example

import android.content.Context
import androidx.compose.ui.text.TextRange
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BlockType
import com.example.data.model.HeadingLevel
import com.example.presentation.createplan.CreatePlanViewModel
import com.example.presentation.editor.EditorViewModel
import com.example.presentation.joinplan.JoinPlanViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DuoPlan", appName)
  }

  @Test
  fun `create plan validation succeeds with non-empty name`() {
    val viewModel = CreatePlanViewModel()
    viewModel.onPlanNameChanged("Team Retrospective")
    var capturedName = ""
    val success = viewModel.validateAndSubmit { name, _ -> capturedName = name }
    assertTrue(success)
    assertEquals("Team Retrospective", capturedName)
  }

  @Test
  fun `create plan validation fails with empty name`() {
    val viewModel = CreatePlanViewModel()
    viewModel.onPlanNameChanged("   ")
    var captured = false
    val success = viewModel.validateAndSubmit { _, _ -> captured = true }
    assertFalse(success)
    assertFalse(captured)
  }

  @Test
  fun `join plan validation requires at least 4 characters`() {
    val viewModel = JoinPlanViewModel()
    viewModel.onInviteCodeChanged("AB")
    var captured = false
    val success = viewModel.validateAndJoin { captured = true }
    assertFalse(success)
    assertFalse(captured)

    viewModel.onInviteCodeChanged("DUO-1234")
    val successValid = viewModel.validateAndJoin { captured = true }
    assertTrue(successValid)
    assertTrue(captured)
  }

  @Test
  fun `editor viewModel rich text block operations and undo redo work`() {
    val viewModel = EditorViewModel()
    val initialBlockId = viewModel.uiState.value.blocks.first().id

    // Text typing and word counting
    viewModel.onBlockTextChanged(initialBlockId, "Planning sprint goals for this quarter")
    assertEquals(6, viewModel.uiState.value.wordCount)

    // Heading change
    viewModel.setHeadingLevel(HeadingLevel.H2)
    assertEquals(HeadingLevel.H2, viewModel.uiState.value.blocks.first().headingLevel)
    assertEquals(BlockType.HEADING, viewModel.uiState.value.blocks.first().type)

    // Enter pressed creates second block
    viewModel.onEnterPressed(initialBlockId, "Planning sprint goals for this quarter".length)
    assertEquals(2, viewModel.uiState.value.blocks.size)

    val secondBlockId = viewModel.uiState.value.blocks[1].id
    viewModel.onBlockTextChanged(secondBlockId, "Review backlog items")
    viewModel.setBlockType(BlockType.CHECKLIST)
    assertEquals(BlockType.CHECKLIST, viewModel.uiState.value.blocks[1].type)
    assertFalse(viewModel.uiState.value.blocks[1].isChecked)

    // Toggle checklist
    viewModel.toggleChecklist(secondBlockId)
    assertTrue(viewModel.uiState.value.blocks[1].isChecked)

    // Formatting bold on selection
    viewModel.onBlockSelectionChanged(secondBlockId, TextRange(0, 6)) // "Review"
    viewModel.toggleBold()
    assertTrue(viewModel.uiState.value.blocks[1].spans.any { it.isBold })

    // Undo stack test
    assertTrue(viewModel.uiState.value.formattingState.canUndo)
    viewModel.undo()
    // Redo stack test
    assertTrue(viewModel.uiState.value.formattingState.canRedo)
    viewModel.redo()
  }
}
