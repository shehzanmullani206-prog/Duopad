package com.example.presentation.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
  object Welcome : Screen("welcome")
  object PlanList : Screen("plans")
  object CreatePlan : Screen("create_plan")
  object JoinPlan : Screen("join_plan")
  object History : Screen("history")
  object Settings : Screen("settings")

  object Editor : Screen("editor?planId={planId}&planTitle={planTitle}&planDescription={planDescription}") {
    fun createRoute(
      planId: String? = null,
      planTitle: String = "Our Plan",
      planDescription: String = ""
    ): String {
      val encodedId = if (planId != null) URLEncoder.encode(planId, StandardCharsets.UTF_8.toString()) else ""
      val encodedTitle = URLEncoder.encode(planTitle, StandardCharsets.UTF_8.toString())
      val encodedDesc = URLEncoder.encode(planDescription, StandardCharsets.UTF_8.toString())
      return "editor?planId=$encodedId&planTitle=$encodedTitle&planDescription=$encodedDesc"
    }
  }
}
