package com.example.presentation.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.user.UserManager
import com.example.ui.components.AppTextField
import com.example.ui.components.AppTopBar
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun SettingsScreen(
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val userManager = remember { UserManager.getInstance(context) }
  val currentUser by userManager.currentUser.collectAsStateWithLifecycle()

  var darkThemeLocked by remember { mutableStateOf(true) }
  var lineNumbersEnabled by remember { mutableStateOf(false) }
  var partnerAlertsEnabled by remember { mutableStateOf(true) }
  var autoSaveEnabled by remember { mutableStateOf(true) }

  var showNameDialog by remember { mutableStateOf(false) }
  var editedName by remember { mutableStateOf("") }

  var showServerDialog by remember { mutableStateOf(false) }
  var editedServerUrl by remember { mutableStateOf("") }

  Scaffold(
    topBar = {
      AppTopBar(
        title = "Settings",
        onNavigationClick = onBackClick
      )
    },
    containerColor = DarkCanvas,
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 600.dp)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
      ) {
        // Section: Profile
        SettingsSection(title = "PROFILE") {
          SettingsItem(
            icon = Icons.Default.Person,
            title = "Display Name",
            subtitle = "${currentUser.displayName} (Tap to change)",
            onClick = {
              editedName = currentUser.displayName
              showNameDialog = true
            },
            testTag = "settings_profile_name"
          )
          SettingsDivider()
          SettingsItem(
            icon = Icons.Default.EditNote,
            title = "User ID",
            subtitle = "${currentUser.userId.take(8)}... (Tap to copy)",
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("DuoPlan User ID", currentUser.userId)
              clipboard.setPrimaryClip(clip)
              Toast.makeText(context, "User ID copied", Toast.LENGTH_SHORT).show()
            },
            testTag = "settings_user_id"
          )
        }

        // Section: Sync & Network
        SettingsSection(title = "COLLABORATION SERVER") {
          SettingsItem(
            icon = Icons.Default.CloudQueue,
            title = "Server Endpoint",
            subtitle = userManager.serverBaseUrl,
            onClick = {
              editedServerUrl = userManager.serverBaseUrl
              showServerDialog = true
            },
            testTag = "settings_server_endpoint"
          )
        }

        // Section: Appearance
        SettingsSection(title = "APPEARANCE") {
          SettingsSwitchItem(
            icon = Icons.Default.DarkMode,
            title = "Obsidian Dark Theme",
            subtitle = "Optimized for minimal eye fatigue and high contrast",
            checked = darkThemeLocked,
            onCheckedChange = { darkThemeLocked = it },
            testTag = "settings_dark_theme_switch"
          )
        }

        // Section: Editor
        SettingsSection(title = "EDITOR") {
          SettingsSwitchItem(
            icon = Icons.Default.Tune,
            title = "Auto-Save Drafts",
            subtitle = "Save typing state periodically into local database",
            checked = autoSaveEnabled,
            onCheckedChange = { autoSaveEnabled = it },
            testTag = "settings_autosave_switch"
          )
          SettingsDivider()
          SettingsSwitchItem(
            icon = Icons.Default.EditNote,
            title = "Notepad Guidelines",
            subtitle = "Show subtle horizontal sheet dividers while writing",
            checked = lineNumbersEnabled,
            onCheckedChange = { lineNumbersEnabled = it },
            testTag = "settings_sheet_guidelines_switch"
          )
        }

        // Section: Notifications
        SettingsSection(title = "NOTIFICATIONS") {
          SettingsSwitchItem(
            icon = Icons.Default.Notifications,
            title = "Partner Edits Alert",
            subtitle = "Notify when your partner connects or edits in real time",
            checked = partnerAlertsEnabled,
            onCheckedChange = { partnerAlertsEnabled = it },
            testTag = "settings_partner_alerts_switch"
          )
        }

        // Section: About
        SettingsSection(title = "ABOUT") {
          SettingsItem(
            icon = Icons.Default.Info,
            title = "DuoPlan",
            subtitle = "Version 1.0.0 (Realtime 2-Person Collaboration)",
            onClick = {},
            testTag = "settings_about_info"
          )
          SettingsDivider()
          Box(
            modifier = Modifier.padding(16.dp)
          ) {
            Text(
              text = "A private two-person collaborative planning notepad with real-time sync, invite codes, rich block formatting, and local Room persistence.",
              style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
              color = TextTertiary
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }

  // Edit Name Dialog
  if (showNameDialog) {
    AlertDialog(
      onDismissRequest = { showNameDialog = false },
      containerColor = DarkSurfaceElevated,
      title = { Text("Change Display Name", color = TextPrimary) },
      text = {
        AppTextField(
          value = editedName,
          onValueChange = { editedName = it },
          label = "Your Name",
          placeholder = "e.g. Alex",
          singleLine = true,
          testTag = "settings_edit_name_input"
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (editedName.isNotBlank()) {
              userManager.updateDisplayName(editedName.trim())
              showNameDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
        ) {
          Text("Save", color = Color.White)
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showNameDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  // Edit Server URL Dialog
  if (showServerDialog) {
    AlertDialog(
      onDismissRequest = { showServerDialog = false },
      containerColor = DarkSurfaceElevated,
      title = { Text("Configure Server URL", color = TextPrimary) },
      text = {
        Column {
          Text(
            text = "Specify HTTP base URL for collaboration server (e.g. http://10.0.2.2:8080 or custom IP):",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(12.dp))
          AppTextField(
            value = editedServerUrl,
            onValueChange = { editedServerUrl = it },
            label = "Server URL",
            placeholder = "http://10.0.2.2:8080",
            singleLine = true,
            testTag = "settings_edit_server_input"
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (editedServerUrl.isNotBlank()) {
              userManager.serverBaseUrl = editedServerUrl.trim()
              showServerDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
        ) {
          Text("Save", color = Color.White)
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showServerDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
private fun SettingsSection(
  title: String,
  content: @Composable () -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp
      ),
      color = TextTertiary,
      modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
    )
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
    ) {
      Column { content() }
    }
  }
}

@Composable
private fun SettingsItem(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  testTag: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp)
      .testTag(testTag)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = AccentBlue,
      modifier = Modifier.size(22.dp)
    )
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Medium,
          fontSize = 15.sp
        ),
        color = TextPrimary
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
        color = TextSecondary
      )
    }
    Icon(
      imageVector = Icons.Default.ChevronRight,
      contentDescription = null,
      tint = TextTertiary,
      modifier = Modifier.size(20.dp)
    )
  }
}

@Composable
private fun SettingsSwitchItem(
  icon: ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag(testTag)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = AccentBlue,
      modifier = Modifier.size(22.dp)
    )
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Medium,
          fontSize = 15.sp
        ),
        color = TextPrimary
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
        color = TextSecondary
      )
    }
    Spacer(modifier = Modifier.width(8.dp))
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = TextPrimary,
        checkedTrackColor = AccentBlue,
        uncheckedThumbColor = TextSecondary,
        uncheckedTrackColor = DarkSurfaceHighlight
      )
    )
  }
}

@Composable
private fun SettingsDivider() {
  HorizontalDivider(
    color = DarkBorderSubtle,
    thickness = 1.dp,
    modifier = Modifier.padding(horizontal = 16.dp)
  )
}
