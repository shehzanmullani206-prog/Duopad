package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentRose
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun AppTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  placeholder: String = "",
  errorMessage: String? = null,
  singleLine: Boolean = true,
  maxLines: Int = if (singleLine) 1 else 4,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  testTag: String = "app_text_field"
) {
  val isError = errorMessage != null

  Column(modifier = modifier) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp
      ),
      color = TextSecondary,
      modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
    )
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = Modifier
        .fillMaxWidth()
        .testTag(testTag),
      placeholder = {
        Text(
          text = placeholder,
          style = MaterialTheme.typography.bodyLarge,
          color = TextTertiary
        )
      },
      isError = isError,
      singleLine = singleLine,
      maxLines = maxLines,
      shape = RoundedCornerShape(12.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = DarkSurface,
        unfocusedContainerColor = DarkSurface,
        disabledContainerColor = DarkSurface,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedBorderColor = AccentBlue,
        unfocusedBorderColor = DarkBorder,
        errorBorderColor = AccentRose,
        cursorColor = AccentBlue
      ),
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions
    )
    if (isError && errorMessage != null) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = errorMessage,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
        color = AccentRose,
        modifier = Modifier.padding(start = 4.dp)
      )
    }
  }
}
