package com.example.presentation.joinplan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AppTextField
import com.example.ui.components.AppTopBar
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun JoinPlanScreen(
  onBackClick: () -> Unit,
  onJoinSuccess: (String) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: JoinPlanViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val focusManager = LocalFocusManager.current

  Scaffold(
    topBar = {
      AppTopBar(
        title = "Join Plan",
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
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 500.dp)
          .fillMaxWidth()
      ) {
        Text(
          text = "Enter Invite Code",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp
          ),
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Enter the 6-character plan code provided by your partner to access the shared notepad.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        AppTextField(
          value = uiState.inviteCode,
          onValueChange = viewModel::onInviteCodeChanged,
          label = "Partner Plan Code",
          placeholder = "e.g., DUO-7842",
          errorMessage = uiState.inviteCodeError,
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Done
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              focusManager.clearFocus()
              viewModel.validateAndJoin(onJoinSuccess)
            }
          ),
          testTag = "join_plan_code_input"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Info card about private 2-person collaboration
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
        ) {
          Row(verticalAlignment = Alignment.Top) {
            Icon(
              imageVector = Icons.Default.VpnKey,
              contentDescription = null,
              tint = AccentBlue,
              modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "End-to-End Private Pairing",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp
                ),
                color = TextPrimary
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Once connected, you and your partner have simultaneous real-time access to the planning notebook.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = TextTertiary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(36.dp))

        PrimaryButton(
          text = "Join & Open Plan",
          icon = Icons.Default.GroupAdd,
          onClick = {
            focusManager.clearFocus()
            viewModel.validateAndJoin(onJoinSuccess)
          },
          modifier = Modifier.fillMaxWidth(),
          testTag = "join_plan_submit_button"
        )
      }
    }
  }
}
