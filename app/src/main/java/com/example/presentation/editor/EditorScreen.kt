package com.example.presentation.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AppDrawerSheet
import com.example.ui.components.AppTextField
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentRose
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
  planIdArg: String?,
  planTitleArg: String?,
  planDescriptionArg: String?,
  onNavigateToPlanList: () -> Unit,
  onNavigateToCreatePlan: () -> Unit,
  onNavigateToJoinPlan: () -> Unit,
  onNavigateToHistory: () -> Unit,
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: EditorViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val coroutineScope = rememberCoroutineScope()

  var showRenameDialog by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }

  LaunchedEffect(planIdArg, planTitleArg, planDescriptionArg) {
    viewModel.loadPlan(planIdArg, planTitleArg, planDescriptionArg)
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      AppDrawerSheet(
        currentPlanTitle = uiState.planTitle,
        onNavigateToEditor = {
          coroutineScope.launch { drawerState.close() }
        },
        onNavigateToPlanList = {
          coroutineScope.launch {
            drawerState.close()
            onNavigateToPlanList()
          }
        },
        onNavigateToCreatePlan = {
          coroutineScope.launch {
            drawerState.close()
            onNavigateToCreatePlan()
          }
        },
        onNavigateToJoinPlan = {
          coroutineScope.launch {
            drawerState.close()
            onNavigateToJoinPlan()
          }
        },
        onNavigateToHistory = {
          coroutineScope.launch {
            drawerState.close()
            onNavigateToHistory()
          }
        },
        onNavigateToSettings = {
          coroutineScope.launch {
            drawerState.close()
            onNavigateToSettings()
          }
        }
      )
    },
    modifier = modifier.fillMaxSize()
  ) {
    Scaffold(
      topBar = {
        EditorTopBar(
          planTitle = uiState.planTitle,
          connectionStatus = uiState.connectionStatus,
          partnerStatus = uiState.partnerStatus,
          saveStatus = uiState.saveStatus,
          lastSavedTime = uiState.lastSavedTime,
          isShared = uiState.isShared,
          onMenuClick = {
            coroutineScope.launch { drawerState.open() }
          },
          onShareClick = viewModel::openShareDialog,
          onHistoryClick = onNavigateToHistory,
          onSettingsClick = onNavigateToSettings,
          onRenameClick = { showRenameDialog = true },
          onDeleteClick = { showDeleteDialog = true },
          onToggleToolbar = viewModel::toggleToolbarVisible,
          isToolbarVisible = uiState.isToolbarVisible
        )
      },
      containerColor = DarkCanvas,
      modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
      if (uiState.isLoading) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(color = AccentBlue)
        }
      } else {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .testTag("editor_main_layout")
        ) {
          // Left Toolbar with visibility animation
          AnimatedVisibility(
            visible = uiState.isToolbarVisible,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut()
          ) {
            EditorLeftToolbar(
              formattingState = uiState.formattingState,
              isExpanded = uiState.isToolbarExpanded,
              onToggleExpanded = viewModel::toggleToolbarExpanded,
              onToggleBold = viewModel::toggleBold,
              onToggleItalic = viewModel::toggleItalic,
              onToggleUnderline = viewModel::toggleUnderline,
              onToggleStrikethrough = viewModel::toggleStrikethrough,
              onHeadingSelect = viewModel::setHeadingLevel,
              onBlockTypeSelect = viewModel::setBlockType,
              onInsertDivider = viewModel::insertDivider,
              onAlignmentSelect = viewModel::setAlignment,
              onTextColorSelect = viewModel::setTextColor,
              onToggleHighlight = viewModel::toggleHighlight,
              onFontSizeSelect = viewModel::setFontSize,
              onAdjustLetterSpacing = viewModel::adjustLetterSpacing,
              onLineSpacingSelect = viewModel::setLineSpacing,
              onParagraphSpacingSelect = viewModel::setParagraphSpacing,
              onUndo = viewModel::undo,
              onRedo = viewModel::redo,
              onHistoryClick = onNavigateToHistory,
              modifier = Modifier.fillMaxHeight()
            )
          }

          // Notepad Document Area
          EditorNotepadArea(
            planTitle = uiState.planTitle,
            planDescription = uiState.planDescription,
            blocks = uiState.blocks,
            activeBlockId = uiState.activeBlockId,
            formattingState = uiState.formattingState,
            onBlockTextChanged = viewModel::onBlockTextChanged,
            onBlockFocusChanged = viewModel::onBlockFocusChanged,
            onBlockSelectionChanged = viewModel::onBlockSelectionChanged,
            onEnterPressed = viewModel::onEnterPressed,
            onBackspaceAtStart = viewModel::onBackspaceAtStart,
            onToggleChecklist = viewModel::toggleChecklist,
            wordCount = uiState.wordCount,
            charCount = uiState.charCount,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }

  // Rename Plan Dialog
  if (showRenameDialog) {
    var newTitle by remember { mutableStateOf(uiState.planTitle) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showRenameDialog = false },
      containerColor = DarkSurfaceElevated,
      title = {
        Text(
          text = "Rename Plan",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = TextPrimary
        )
      },
      text = {
        Column {
          Text(
            text = "Enter a new title for this plan document:",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(12.dp))
          AppTextField(
            value = newTitle,
            onValueChange = {
              newTitle = it
              isError = it.isBlank()
            },
            label = "Plan Title",
            placeholder = "e.g., Summer Trip 2026",
            errorMessage = if (isError) "Title cannot be empty" else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
              capitalization = KeyboardCapitalization.Sentences,
              imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
              onDone = {
                if (newTitle.isNotBlank()) {
                  viewModel.renamePlan(newTitle.trim())
                  showRenameDialog = false
                }
              }
            ),
            testTag = "editor_rename_plan_input"
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (newTitle.isNotBlank()) {
              viewModel.renamePlan(newTitle.trim())
              showRenameDialog = false
            } else {
              isError = true
            }
          },
          modifier = Modifier.testTag("editor_rename_plan_confirm_button")
        ) {
          Text("Save", color = AccentBlue, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showRenameDialog = false },
          modifier = Modifier.testTag("editor_rename_plan_cancel_button")
        ) {
          Text("Cancel", color = TextSecondary)
        }
      },
      modifier = Modifier.testTag("editor_rename_dialog")
    )
  }

  // Delete Plan Dialog
  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      containerColor = DarkSurfaceElevated,
      title = {
        Text(
          text = "Delete Plan?",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = TextPrimary
        )
      },
      text = {
        Text(
          text = "Are you sure you want to delete \"${uiState.planTitle}\"? This plan and all its formatted blocks will be removed from your local database.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteDialog = false
            viewModel.deleteCurrentPlan {
              onNavigateToPlanList()
            }
          },
          colors = ButtonDefaults.textButtonColors(contentColor = AccentRose),
          modifier = Modifier.testTag("editor_delete_plan_confirm_button")
        ) {
          Text("Delete", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showDeleteDialog = false },
          modifier = Modifier.testTag("editor_delete_plan_cancel_button")
        ) {
          Text("Cancel", color = TextSecondary)
        }
      },
      modifier = Modifier.testTag("editor_delete_dialog")
    )
  }

  // Share / Collaborate Dialog
  if (uiState.isShareDialogOpen) {
    com.example.presentation.editor.dialogs.SharePlanDialog(
      planTitle = uiState.planTitle,
      inviteCode = uiState.inviteCode,
      ownerName = uiState.ownerName,
      partnerName = uiState.partnerName,
      partnerStatus = uiState.partnerStatus,
      currentUserName = uiState.currentUserName,
      onDismiss = viewModel::closeShareDialog
    )
  }
}
