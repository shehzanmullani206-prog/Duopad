package com.example.presentation.welcome

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun WelcomeScreen(
  onCreatePlanClick: () -> Unit,
  onViewAllPlansClick: () -> Unit,
  onJoinPlanClick: () -> Unit,
  onQuickOpenEditor: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 480.dp)
        .fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Minimalist Notebook Brand Icon
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(RoundedCornerShape(18.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, DarkBorder, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.EditNote,
          contentDescription = null,
          tint = AccentBlue,
          modifier = Modifier.size(36.dp)
        )
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Title
      Text(
        text = "DuoPlan",
        style = MaterialTheme.typography.displayLarge.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 32.sp
        ),
        color = TextPrimary,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Tagline
      Text(
        text = "Plan together. Think together.",
        style = MaterialTheme.typography.bodyLarge.copy(
          fontSize = 17.sp,
          fontWeight = FontWeight.Normal
        ),
        color = TextSecondary,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(36.dp))

      // Action Buttons
      PrimaryButton(
        text = "Create Plan",
        onClick = onCreatePlanClick,
        icon = Icons.Default.Add,
        modifier = Modifier.fillMaxWidth(),
        testTag = "welcome_create_plan_button"
      )

      Spacer(modifier = Modifier.height(14.dp))

      SecondaryButton(
        text = "My Plans",
        onClick = onViewAllPlansClick,
        icon = Icons.Default.Description,
        modifier = Modifier.fillMaxWidth(),
        testTag = "welcome_my_plans_button"
      )

      Spacer(modifier = Modifier.height(14.dp))

      SecondaryButton(
        text = "Join Plan",
        onClick = onJoinPlanClick,
        icon = Icons.Default.GroupAdd,
        modifier = Modifier.fillMaxWidth(),
        testTag = "welcome_join_plan_button"
      )

      Spacer(modifier = Modifier.height(28.dp))

      // Quick jump to scratchpad/editor
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .border(1.dp, DarkBorderSubtle, RoundedCornerShape(10.dp))
          .background(DarkSurface)
          .clickable(onClick = onQuickOpenEditor)
          .padding(horizontal = 16.dp, vertical = 10.dp)
          .testTag("welcome_quick_scratchpad_button")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Open Active Plan",
            style = MaterialTheme.typography.labelLarge.copy(
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium
            ),
            color = TextSecondary
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "→",
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
            color = AccentBlue
          )
        }
      }
    }
  }
}
