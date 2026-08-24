package com.example

import android.app.Application
import com.example.data.collaboration.CollaborationManager
import com.example.data.network.CollaborationApiClient
import com.example.data.repository.PlanRepository
import com.example.data.user.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DuoPlanApplication : Application() {

  val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  val userManager: UserManager by lazy {
    UserManager.getInstance(this)
  }

  val apiClient: CollaborationApiClient by lazy {
    CollaborationApiClient()
  }

  val collaborationManager: CollaborationManager by lazy {
    CollaborationManager(this, userManager)
  }

  val planRepository: PlanRepository by lazy {
    PlanRepository.getInstance(this, apiClient, userManager)
  }

  override fun onCreate() {
    super.onCreate()
    // Pre-seed default starter plan if database is empty
    applicationScope.launch {
      planRepository.ensureDefaultPlanExists()
    }
  }
}
