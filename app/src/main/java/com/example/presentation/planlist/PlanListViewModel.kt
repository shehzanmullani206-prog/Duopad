package com.example.presentation.planlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DuoPlanApplication
import com.example.data.local.entity.PlanEntity
import com.example.data.repository.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanListUiState(
  val plans: List<PlanEntity> = emptyList(),
  val searchQuery: String = "",
  val planToRename: PlanEntity? = null,
  val planToDelete: PlanEntity? = null,
  val isLoading: Boolean = false
)

class PlanListViewModel(
  application: Application,
  private val repository: PlanRepository = (application as DuoPlanApplication).planRepository
) : AndroidViewModel(application) {

  private val _searchQuery = MutableStateFlow("")
  private val _planToRename = MutableStateFlow<PlanEntity?>(null)
  private val _planToDelete = MutableStateFlow<PlanEntity?>(null)

  val uiState: StateFlow<PlanListUiState> = combine(
    repository.allPlans,
    _searchQuery,
    _planToRename,
    _planToDelete
  ) { allPlans, query, toRename, toDelete ->
    val filteredPlans = if (query.isBlank()) {
      allPlans
    } else {
      allPlans.filter {
        it.title.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
      }
    }
    PlanListUiState(
      plans = filteredPlans,
      searchQuery = query,
      planToRename = toRename,
      planToDelete = toDelete,
      isLoading = false
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = PlanListUiState(isLoading = true)
  )

  fun onSearchQueryChanged(query: String) {
    _searchQuery.value = query
  }

  fun onRequestRename(plan: PlanEntity) {
    _planToRename.value = plan
  }

  fun onDismissRename() {
    _planToRename.value = null
  }

  fun onConfirmRename(planId: String, newTitle: String) {
    if (newTitle.isNotBlank()) {
      viewModelScope.launch {
        repository.renamePlan(planId, newTitle.trim())
        _planToRename.value = null
      }
    }
  }

  fun onRequestDelete(plan: PlanEntity) {
    _planToDelete.value = plan
  }

  fun onDismissDelete() {
    _planToDelete.value = null
  }

  fun onConfirmDelete(planId: String) {
    viewModelScope.launch {
      repository.deletePlan(planId)
      _planToDelete.value = null
    }
  }
}
