package com.example.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.presentation.createplan.CreatePlanScreen
import com.example.presentation.editor.EditorScreen
import com.example.presentation.history.HistoryScreen
import com.example.presentation.joinplan.JoinPlanScreen
import com.example.presentation.planlist.PlanListScreen
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.welcome.WelcomeScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavHost(
  modifier: Modifier = Modifier,
  navController: NavHostController = rememberNavController(),
  startDestination: String = Screen.Welcome.route
) {
  NavHost(
    navController = navController,
    startDestination = startDestination,
    modifier = modifier
  ) {
    // 1. Welcome Screen
    composable(Screen.Welcome.route) {
      WelcomeScreen(
        onCreatePlanClick = {
          navController.navigate(Screen.CreatePlan.route)
        },
        onViewAllPlansClick = {
          navController.navigate(Screen.PlanList.route)
        },
        onJoinPlanClick = {
          navController.navigate(Screen.JoinPlan.route)
        },
        onQuickOpenEditor = {
          navController.navigate(Screen.PlanList.route)
        }
      )
    }

    // 2. Plan List (Documents) Screen
    composable(Screen.PlanList.route) {
      PlanListScreen(
        onPlanClick = { planId ->
          navController.navigate(Screen.Editor.createRoute(planId = planId))
        },
        onCreatePlanClick = {
          navController.navigate(Screen.CreatePlan.route)
        },
        onMenuClick = {
          // Can navigate or handle
        }
      )
    }

    // 3. Create Plan Screen
    composable(Screen.CreatePlan.route) {
      CreatePlanScreen(
        onBackClick = { navController.popBackStack() },
        onPlanCreated = { newPlanId ->
          navController.navigate(Screen.Editor.createRoute(planId = newPlanId)) {
            popUpTo(Screen.PlanList.route) { inclusive = false }
          }
        }
      )
    }

    // 4. Join Plan Screen
    composable(Screen.JoinPlan.route) {
      JoinPlanScreen(
        onBackClick = { navController.popBackStack() },
        onJoinSuccess = { code ->
          navController.navigate(Screen.Editor.createRoute(planTitle = "Plan $code", planDescription = "Joined via $code")) {
            popUpTo(Screen.Welcome.route) { inclusive = false }
          }
        }
      )
    }

    // 5. Main Planning Editor Screen
    composable(
      route = Screen.Editor.route,
      arguments = listOf(
        navArgument("planId") {
          type = NavType.StringType
          defaultValue = ""
          nullable = true
        },
        navArgument("planTitle") {
          type = NavType.StringType
          defaultValue = "Our Plan"
          nullable = true
        },
        navArgument("planDescription") {
          type = NavType.StringType
          defaultValue = ""
          nullable = true
        }
      )
    ) { backStackEntry ->
      val rawId = backStackEntry.arguments?.getString("planId") ?: ""
      val rawTitle = backStackEntry.arguments?.getString("planTitle") ?: "Our Plan"
      val rawDesc = backStackEntry.arguments?.getString("planDescription") ?: ""

      val decodedId = try {
        URLDecoder.decode(rawId, StandardCharsets.UTF_8.toString())
      } catch (e: Exception) {
        rawId
      }
      val decodedTitle = try {
        URLDecoder.decode(rawTitle, StandardCharsets.UTF_8.toString())
      } catch (e: Exception) {
        rawTitle
      }
      val decodedDesc = try {
        URLDecoder.decode(rawDesc, StandardCharsets.UTF_8.toString())
      } catch (e: Exception) {
        rawDesc
      }

      val planIdParam = if (decodedId.isNotBlank()) decodedId else null

      EditorScreen(
        planIdArg = planIdParam,
        planTitleArg = decodedTitle,
        planDescriptionArg = decodedDesc,
        onNavigateToPlanList = {
          navController.navigate(Screen.PlanList.route) {
            popUpTo(Screen.Welcome.route) { inclusive = false }
          }
        },
        onNavigateToCreatePlan = {
          navController.navigate(Screen.CreatePlan.route)
        },
        onNavigateToJoinPlan = {
          navController.navigate(Screen.JoinPlan.route)
        },
        onNavigateToHistory = {
          navController.navigate(Screen.History.route)
        },
        onNavigateToSettings = {
          navController.navigate(Screen.Settings.route)
        }
      )
    }

    // 6. History Screen
    composable(Screen.History.route) {
      HistoryScreen(
        onBackClick = { navController.popBackStack() }
      )
    }

    // 7. Settings Screen
    composable(Screen.Settings.route) {
      SettingsScreen(
        onBackClick = { navController.popBackStack() }
      )
    }
  }
}
