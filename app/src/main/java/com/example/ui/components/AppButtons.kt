package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun PrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: ImageVector? = null,
  testTag: String = "primary_button"
) {
  Button(
    onClick = onClick,
    modifier = modifier
      .height(50.dp)
      .testTag(testTag),
    enabled = enabled,
    shape = RoundedCornerShape(12.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = AccentBlue,
      contentColor = DarkCanvas,
      disabledContainerColor = DarkSurfaceHighlight,
      disabledContentColor = TextTertiary
    ),
    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
  ) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(20.dp)
      )
    }
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
      )
    )
  }
}

@Composable
fun SecondaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: ImageVector? = null,
  testTag: String = "secondary_button"
) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier
      .height(50.dp)
      .testTag(testTag),
    enabled = enabled,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, DarkBorder),
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = DarkSurfaceElevated,
      contentColor = TextPrimary,
      disabledContainerColor = DarkCanvas,
      disabledContentColor = TextTertiary
    ),
    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
  ) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = TextSecondary
      )
    }
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        color = TextPrimary
      )
    )
  }
}

@Composable
fun AppIconButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  tint: Color = TextSecondary,
  isSelected: Boolean = false,
  testTag: String = "app_icon_button"
) {
  IconButton(
    onClick = onClick,
    modifier = modifier
      .size(44.dp)
      .testTag(testTag),
    colors = IconButtonDefaults.iconButtonColors(
      containerColor = if (isSelected) DarkSurfaceHighlight else Color.Transparent,
      contentColor = if (isSelected) AccentBlue else tint
    )
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = if (isSelected) AccentBlue else tint,
      modifier = Modifier.size(20.dp)
    )
  }
}
