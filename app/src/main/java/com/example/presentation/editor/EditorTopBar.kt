package com.example.presentation.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionStatus
import com.example.data.model.PartnerStatus
import com.example.ui.components.ConnectionStatusBadge
import com.example.ui.components.PartnerStatusBadge
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun EditorTopBar(
  planTitle: String,
  connectionStatus: ConnectionStatus,
  partnerStatus: PartnerStatus,
  saveStatus: SaveStatus,
  lastSavedTime: String,
  isShared: Boolean = false,
  onMenuClick: () -> Unit,
  onShareClick: () -> Unit,
  onHistoryClick: () -> Unit,
  onSettingsClick: () -> Unit,
  onRenameClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onToggleToolbar: () -> Unit,
  isToolbarVisible: Boolean,
  modifier: Modifier = Modifier
) {
  var moreMenuExpanded by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(DarkCanvas)
      .drawBehind {
        drawLine(
          color = DarkBorderSubtle,
          start = Offset(0f, size.height),
          end = Offset(size.width, size.height),
          strokeWidth = 1.dp.toPx()
        )
      }
      .statusBarsPadding()
      .padding(horizontal = 8.dp, vertical = 6.dp)
      .testTag("editor_top_bar")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Menu button to open side drawer
      IconButton(
        onClick = onMenuClick,
        modifier = Modifier
          .size(44.dp)
          .testTag("editor_menu_button")
      ) {
        Icon(
          imageVector = Icons.Default.Menu,
          contentDescription = "Open Navigation Menu",
          tint = TextPrimary,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(4.dp))

      // Current Plan Title and Auto-save indicator
      Column(
        modifier = Modifier
          .weight(1f)
          .clickable(onClick = onRenameClick),
        verticalArrangement = Arrangement.Center
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = planTitle,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.SemiBold,
              fontSize = 16.sp
            ),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("editor_plan_title_text")
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(top = 1.dp)
        ) {
          // Save Status Text & Color
          val (saveText, saveColor) = when (saveStatus) {
            SaveStatus.SAVED -> Pair(lastSavedTime, TextTertiary)
            SaveStatus.SAVING -> Pair("Saving...", AccentBlue)
            SaveStatus.UNSAVED -> Pair("Unsaved", TextSecondary)
            SaveStatus.ERROR -> Pair("Save failed", AccentRose)
          }

          Text(
            text = saveText,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = saveColor,
            maxLines = 1,
            modifier = Modifier.testTag("editor_save_status_text")
          )
        }
      }

      Spacer(modifier = Modifier.width(4.dp))

      // Partner Status Badge (Clickable to open Share dialog)
      Box(
        modifier = Modifier.clickable(onClick = onShareClick)
      ) {
        PartnerStatusBadge(
          partnerStatus = partnerStatus,
          modifier = Modifier.padding(end = 4.dp)
        )
      }

      // Share / Invite button
      IconButton(
        onClick = onShareClick,
        modifier = Modifier
          .size(38.dp)
          .testTag("editor_share_button")
      ) {
        Icon(
          imageVector = Icons.Default.GroupAdd,
          contentDescription = "Share Plan",
          tint = if (isShared) AccentBlue else TextSecondary,
          modifier = Modifier.size(19.dp)
        )
      }

      // Toggle Left Toolbar action
      IconButton(
        onClick = onToggleToolbar,
        modifier = Modifier
          .size(38.dp)
          .testTag("editor_toggle_toolbar_button")
      ) {
        Icon(
          imageVector = Icons.Default.ViewSidebar,
          contentDescription = "Toggle Toolbar",
          tint = if (isToolbarVisible) AccentBlue else TextSecondary,
          modifier = Modifier.size(19.dp)
        )
      }

      // Quick History shortcut
      IconButton(
        onClick = onHistoryClick,
        modifier = Modifier
          .size(38.dp)
          .testTag("editor_quick_history_button")
      ) {
        Icon(
          imageVector = Icons.Default.History,
          contentDescription = "View History",
          tint = TextSecondary,
          modifier = Modifier.size(19.dp)
        )
      }

      // Overflow More menu (Rename, Delete, Settings)
      Box {
        IconButton(
          onClick = { moreMenuExpanded = true },
          modifier = Modifier
            .size(38.dp)
            .testTag("editor_more_options_button")
        ) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "More Options",
            tint = TextSecondary,
            modifier = Modifier.size(19.dp)
          )
        }

        DropdownMenu(
          expanded = moreMenuExpanded,
          onDismissRequest = { moreMenuExpanded = false },
          modifier = Modifier
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
        ) {
          DropdownMenuItem(
            text = { Text("Invite Partner", color = TextPrimary) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.GroupAdd,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(18.dp)
              )
            },
            onClick = {
              moreMenuExpanded = false
              onShareClick()
            },
            modifier = Modifier.testTag("editor_menu_invite_partner")
          )
          DropdownMenuItem(
            text = { Text("Rename Plan", color = TextPrimary) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.DriveFileRenameOutline,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(18.dp)
              )
            },
            onClick = {
              moreMenuExpanded = false
              onRenameClick()
            },
            modifier = Modifier.testTag("editor_menu_rename_plan")
          )
          DropdownMenuItem(
            text = { Text("Settings", color = TextPrimary) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
              )
            },
            onClick = {
              moreMenuExpanded = false
              onSettingsClick()
            },
            modifier = Modifier.testTag("editor_menu_settings")
          )
          DropdownMenuItem(
            text = { Text("Delete Plan", color = AccentRose) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = AccentRose,
                modifier = Modifier.size(18.dp)
              )
            },
            onClick = {
              moreMenuExpanded = false
              onDeleteClick()
            },
            modifier = Modifier.testTag("editor_menu_delete_plan")
          )
        }
      }
    }
  }
}
