package com.example.presentation.createplan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DuoPlanApplication
import com.example.data.repository.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreatePlanUiState(
  val planName: String = "",
  val planDescription: String = "",
  val planNameError: String? = null,
  val isCreating: Boolean = false
)

class CreatePlanViewModel(
  application: Application,
  private val repository: PlanRepository = (application as DuoPlanApplication).planRepository
) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(CreatePlanUiState())
  val uiState: StateFlow<CreatePlanUiState> = _uiState.asStateFlow()

  fun onPlanNameChanged(name: String) {
    _uiState.update {
      it.copy(
        planName = name,
        planNameError = if (name.isNotBlank()) null else it.planNameError
      )
    }
  }

  fun onDescriptionChanged(description: String) {
    _uiState.update { it.copy(planDescription = description) }
  }

  fun validateAndSubmit(onSuccess: (String) -> Unit) {
    val currentName = _uiState.value.planName.trim()
    if (currentName.isEmpty()) {
      _uiState.update { it.copy(planNameError = "Please enter a plan title") }
      return
    }
    _uiState.update { it.copy(planNameError = null, isCreating = true) }
    viewModelScope.launch {
      val newPlanId = repository.createPlan(
        title = currentName,
        description = _uiState.value.planDescription.trim(),
        isShared = true
      )
      _uiState.update { it.copy(isCreating = false) }
      onSuccess(newPlanId)
    }
  }
}
