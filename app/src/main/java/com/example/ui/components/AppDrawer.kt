package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun AppDrawerSheet(
  currentPlanTitle: String,
  onNavigateToEditor: () -> Unit,
  onNavigateToPlanList: () -> Unit,
  onNavigateToCreatePlan: () -> Unit,
  onNavigateToJoinPlan: () -> Unit,
  onNavigateToHistory: () -> Unit,
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier
) {
  ModalDrawerSheet(
    drawerContainerColor = DarkSurface,
    drawerContentColor = TextPrimary,
    modifier = modifier
      .width(300.dp)
      .fillMaxHeight()
      .testTag("app_drawer_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
      // Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceHighlight),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.EditNote,
            contentDescription = null,
            tint = AccentBlue,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "DuoPlan",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            ),
            color = TextPrimary
          )
          Text(
            text = "Private Collaborative Notepad",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
          )
        }
      }

      HorizontalDivider(color = DarkBorderSubtle, thickness = 1.dp)
      Spacer(modifier = Modifier.height(16.dp))

      // Current plan section
      Text(
        text = "ACTIVE DOCUMENT",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 1.sp
        ),
        color = TextTertiary,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
      )

      DrawerMenuItem(
        icon = Icons.Default.EditNote,
        label = currentPlanTitle,
        isSelected = true,
        onClick = onNavigateToEditor,
        testTag = "drawer_item_current_plan"
      )

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider(color = DarkBorderSubtle, thickness = 1.dp)
      Spacer(modifier = Modifier.height(16.dp))

      // Actions section
      Text(
        text = "DOCUMENTS",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 1.sp
        ),
        color = TextTertiary,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
      )

      DrawerMenuItem(
        icon = Icons.Default.Description,
        label = "My Plans",
        isSelected = false,
        onClick = onNavigateToPlanList,
        testTag = "drawer_item_my_plans"
      )

      DrawerMenuItem(
        icon = Icons.Default.Add,
        label = "Create Plan",
        isSelected = false,
        onClick = onNavigateToCreatePlan,
        testTag = "drawer_item_create_plan"
      )

      DrawerMenuItem(
        icon = Icons.Default.GroupAdd,
        label = "Join Plan",
        isSelected = false,
        onClick = onNavigateToJoinPlan,
        testTag = "drawer_item_join_plan"
      )

      DrawerMenuItem(
        icon = Icons.Default.History,
        label = "History",
        isSelected = false,
        onClick = onNavigateToHistory,
        testTag = "drawer_item_history"
      )

      Spacer(modifier = Modifier.weight(1f))

      HorizontalDivider(color = DarkBorderSubtle, thickness = 1.dp)
      Spacer(modifier = Modifier.height(12.dp))

      DrawerMenuItem(
        icon = Icons.Default.Settings,
        label = "Settings",
        isSelected = false,
        onClick = onNavigateToSettings,
        testTag = "drawer_item_settings"
      )
    }
  }
}

@Composable
private fun DrawerMenuItem(
  icon: ImageVector,
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  testTag: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(if (isSelected) DarkSurfaceHighlight else Color.Transparent)
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 12.dp)
      .testTag(testTag)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = if (isSelected) AccentBlue else TextSecondary,
      modifier = Modifier.size(20.dp)
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        fontSize = 14.sp
      ),
      color = if (isSelected) TextPrimary else TextSecondary
    )
  }
}
