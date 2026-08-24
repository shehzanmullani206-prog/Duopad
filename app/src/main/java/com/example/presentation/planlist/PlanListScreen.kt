package com.example.presentation.planlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.PlanEntity
import com.example.ui.components.AppTextField
import com.example.ui.components.EmptyState
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlanListScreen(
  onPlanClick: (String) -> Unit,
  onCreatePlanClick: () -> Unit,
  onMenuClick: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: PlanListViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val focusManager = LocalFocusManager.current

  Scaffold(
    topBar = {
      PlanListTopBar(
        searchQuery = uiState.searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onMenuClick = onMenuClick,
        totalPlansCount = uiState.plans.size
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = onCreatePlanClick,
        containerColor = AccentBlue,
        contentColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.testTag("plan_list_fab_create_plan")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "New Plan",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.SemiBold,
              fontSize = 15.sp
            )
          )
        }
      }
    },
    containerColor = DarkCanvas,
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      if (uiState.plans.isEmpty() && uiState.searchQuery.isBlank()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(
            modifier = Modifier.widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            EmptyState(
              icon = Icons.Default.EditNote,
              title = "No plans yet",
              description = "Create a new shared planning document to get started.",
              testTag = "plan_list_empty_state"
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
              onClick = onCreatePlanClick,
              modifier = Modifier.testTag("plan_list_create_first_plan_btn")
            ) {
              Text(
                text = "+ Create First Plan",
                color = AccentBlue,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
              )
            }
          }
        }
      } else if (uiState.plans.isEmpty() && uiState.searchQuery.isNotBlank()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          EmptyState(
            icon = Icons.Default.Search,
            title = "No matching plans",
            description = "No documents matched \"${uiState.searchQuery}\". Try a different search keyword.",
            testTag = "plan_list_no_search_results"
          )
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier
            .fillMaxSize()
            .testTag("plan_list_lazy_column")
        ) {
          items(
            items = uiState.plans,
            key = { it.id }
          ) { plan ->
            PlanItemCard(
              plan = plan,
              onClick = { onPlanClick(plan.id) },
              onRenameClick = { viewModel.onRequestRename(plan) },
              onDeleteClick = { viewModel.onRequestDelete(plan) }
            )
          }
        }
      }
    }
  }

  // Rename Dialog
  uiState.planToRename?.let { plan ->
    RenamePlanDialog(
      initialTitle = plan.title,
      onDismiss = viewModel::onDismissRename,
      onConfirm = { newTitle ->
        viewModel.onConfirmRename(plan.id, newTitle)
      }
    )
  }

  // Delete Confirmation Dialog
  uiState.planToDelete?.let { plan ->
    DeletePlanDialog(
      planTitle = plan.title,
      onDismiss = viewModel::onDismissDelete,
      onConfirm = {
        viewModel.onConfirmDelete(plan.id)
      }
    )
  }
}

@Composable
private fun PlanListTopBar(
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  onMenuClick: () -> Unit,
  totalPlansCount: Int
) {
  var isSearchActive by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(DarkCanvas)
      .statusBarsPadding()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .testTag("plan_list_top_bar")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onMenuClick,
        modifier = Modifier
          .size(44.dp)
          .testTag("plan_list_menu_button")
      ) {
        Icon(
          imageVector = Icons.Default.Menu,
          contentDescription = "Open Navigation Menu",
          tint = TextPrimary,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(4.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "My Plans",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp
          ),
          color = TextPrimary
        )
        Text(
          text = "$totalPlansCount document${if (totalPlansCount == 1) "" else "s"}",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
          color = TextTertiary
        )
      }

      IconButton(
        onClick = {
          isSearchActive = !isSearchActive
          if (!isSearchActive) onSearchQueryChange("")
        },
        modifier = Modifier
          .size(44.dp)
          .testTag("plan_list_toggle_search_button")
      ) {
        Icon(
          imageVector = if (isSearchActive) Icons.Default.Clear else Icons.Default.Search,
          contentDescription = if (isSearchActive) "Close Search" else "Search Plans",
          tint = if (isSearchActive) AccentBlue else TextSecondary,
          modifier = Modifier.size(22.dp)
        )
      }
    }

    if (isSearchActive) {
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
          Text(
            text = "Search by plan title or description...",
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary
          )
        },
        singleLine = true,
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
          )
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(
              onClick = { onSearchQueryChange("") },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear Search",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        },
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = DarkSurfaceElevated,
          unfocusedContainerColor = DarkSurface,
          focusedBorderColor = AccentBlue,
          unfocusedBorderColor = DarkBorder,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary,
          cursorColor = AccentBlue
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("plan_list_search_input")
      )
    }
  }
}

@Composable
private fun PlanItemCard(
  plan: PlanEntity,
  onClick: () -> Unit,
  onRenameClick: () -> Unit,
  onDeleteClick: () -> Unit
) {
  var menuExpanded by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp)
      .testTag("plan_item_card_${plan.id}")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Document Notepad Icon
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(DarkSurfaceHighlight)
          .border(1.dp, DarkBorderSubtle, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.EditNote,
          contentDescription = null,
          tint = AccentBlue,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      // Title, Description, and Timestamp
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = plan.title,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
          ),
          color = TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.testTag("plan_item_title_${plan.id}")
        )

        if (plan.description.isNotBlank()) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = plan.description,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = formatRelativeTimestamp(plan.updatedAt),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = TextTertiary
          )
          if (plan.wordCount > 0) {
            Text(
              text = "  •  ${plan.wordCount} words",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
              color = TextTertiary
            )
          }
        }
      }

      // Context Action Menu Button (Rename, Delete)
      Box {
        IconButton(
          onClick = { menuExpanded = true },
          modifier = Modifier
            .size(36.dp)
            .testTag("plan_item_menu_btn_${plan.id}")
        ) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Plan Options",
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
          )
        }

        DropdownMenu(
          expanded = menuExpanded,
          onDismissRequest = { menuExpanded = false },
          modifier = Modifier
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
        ) {
          DropdownMenuItem(
            text = { Text("Rename", color = TextPrimary) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.DriveFileRenameOutline,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(18.dp)
              )
            },
            onClick = {
              menuExpanded = false
              onRenameClick()
            },
            modifier = Modifier.testTag("plan_menu_rename_${plan.id}")
          )
          DropdownMenuItem(
            text = { Text("Delete", color = AccentRose) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = AccentRose,
                modifier = Modifier.size(18.dp)
              )
            },
            onClick = {
              menuExpanded = false
              onDeleteClick()
            },
            modifier = Modifier.testTag("plan_menu_delete_${plan.id}")
          )
        }
      }
    }
  }
}

@Composable
private fun RenamePlanDialog(
  initialTitle: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit
) {
  var newTitle by remember { mutableStateOf(initialTitle) }
  var isError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
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
          placeholder = "e.g., Q4 Roadmap",
          errorMessage = if (isError) "Title cannot be empty" else null,
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              if (newTitle.isNotBlank()) onConfirm(newTitle.trim())
            }
          ),
          testTag = "rename_plan_input"
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (newTitle.isNotBlank()) {
            onConfirm(newTitle.trim())
          } else {
            isError = true
          }
        },
        modifier = Modifier.testTag("rename_plan_confirm_button")
      ) {
        Text("Save", color = AccentBlue, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("rename_plan_cancel_button")
      ) {
        Text("Cancel", color = TextSecondary)
      }
    },
    modifier = Modifier.testTag("rename_plan_dialog")
  )
}

@Composable
private fun DeletePlanDialog(
  planTitle: String,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
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
        text = "Are you sure you want to delete \"$planTitle\"? This action will permanently remove this plan and its content from your local device.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary
      )
    },
    confirmButton = {
      TextButton(
        onClick = onConfirm,
        colors = ButtonDefaults.textButtonColors(contentColor = AccentRose),
        modifier = Modifier.testTag("delete_plan_confirm_button")
      ) {
        Text("Delete", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("delete_plan_cancel_button")
      ) {
        Text("Cancel", color = TextSecondary)
      }
    },
    modifier = Modifier.testTag("delete_plan_dialog")
  )
}

private fun formatRelativeTimestamp(timestamp: Long): String {
  val now = System.currentTimeMillis()
  val diff = now - timestamp
  if (diff < 60_000) return "Updated just now"
  val minutes = diff / 60_000
  if (minutes < 60) return "Updated ${minutes}m ago"
  val hours = minutes / 60
  if (hours < 24) return "Updated ${hours}h ago"
  val days = hours / 24
  if (days < 7) return "Updated ${days}d ago"

  val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
  return "Updated ${sdf.format(Date(timestamp))}"
}
