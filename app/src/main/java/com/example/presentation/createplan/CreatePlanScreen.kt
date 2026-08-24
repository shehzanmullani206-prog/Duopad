package com.example.presentation.createplan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CreatePlanScreen(
  onBackClick: () -> Unit,
  onPlanCreated: (String) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: CreatePlanViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val focusManager = LocalFocusManager.current

  Scaffold(
    topBar = {
      AppTopBar(
        title = "Create Plan",
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
          text = "New Shared Plan",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp
          ),
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Set up a shared digital planning notepad for you and your partner.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        AppTextField(
          value = uiState.planName,
          onValueChange = viewModel::onPlanNameChanged,
          label = "Plan Title *",
          placeholder = "e.g., Summer Trip 2026, Q3 Roadmap, Home Project",
          errorMessage = uiState.planNameError,
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Next
          ),
          testTag = "create_plan_name_input"
        )

        Spacer(modifier = Modifier.height(20.dp))

        AppTextField(
          value = uiState.planDescription,
          onValueChange = viewModel::onDescriptionChanged,
          label = "Description (Optional)",
          placeholder = "Add context, goals, or notes for this plan...",
          singleLine = false,
          maxLines = 4,
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              focusManager.clearFocus()
              viewModel.validateAndSubmit(onPlanCreated)
            }
          ),
          testTag = "create_plan_desc_input"
        )

        Spacer(modifier = Modifier.height(36.dp))

        PrimaryButton(
          text = "Create & Open Plan",
          icon = Icons.Default.Add,
          onClick = {
            focusManager.clearFocus()
            viewModel.validateAndSubmit(onPlanCreated)
          },
          modifier = Modifier.fillMaxWidth(),
          testTag = "create_plan_submit_button"
        )
      }
    }
  }
}
