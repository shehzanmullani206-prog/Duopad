package com.example.presentation.joinplan

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

data class JoinPlanUiState(
  val inviteCode: String = "",
  val inviteCodeError: String? = null,
  val isJoining: Boolean = false
)

class JoinPlanViewModel(
  application: Application,
  private val repository: PlanRepository = (application as DuoPlanApplication).planRepository
) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(JoinPlanUiState())
  val uiState: StateFlow<JoinPlanUiState> = _uiState.asStateFlow()

  fun onInviteCodeChanged(code: String) {
    // Clean uppercase and hyphen formatting
    val sanitized = code.uppercase().filter { it.isLetterOrDigit() || it == '-' }
    _uiState.update {
      it.copy(
        inviteCode = sanitized,
        inviteCodeError = if (sanitized.isNotBlank()) null else it.inviteCodeError
      )
    }
  }

  fun validateAndJoin(onSuccess: (String) -> Unit) {
    val code = _uiState.value.inviteCode.trim()
    if (code.isEmpty()) {
      _uiState.update { it.copy(inviteCodeError = "Please enter an invite code") }
      return
    }
    if (code.length < 4) {
      _uiState.update { it.copy(inviteCodeError = "Invite code must be at least 4 characters") }
      return
    }

    _uiState.update { it.copy(inviteCodeError = null, isJoining = true) }

    viewModelScope.launch {
      val result = repository.joinSharedPlan(code)
      _uiState.update { it.copy(isJoining = false) }

      if (result.isSuccess) {
        val planId = result.getOrThrow()
        onSuccess(planId)
      } else {
        val message = result.exceptionOrNull()?.message ?: "Could not find plan with code '$code'"
        _uiState.update { it.copy(inviteCodeError = message) }
      }
    }
  }
}
