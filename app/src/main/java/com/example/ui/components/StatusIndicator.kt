package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionStatus
import com.example.data.model.PartnerStatus
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ConnectionStatusBadge(
  status: ConnectionStatus = ConnectionStatus.OFFLINE,
  modifier: Modifier = Modifier
) {
  val dotColor = when (status) {
    ConnectionStatus.CONNECTED -> AccentEmerald
    ConnectionStatus.CONNECTING,
    ConnectionStatus.RECONNECTING -> AccentAmber
    ConnectionStatus.ERROR -> AccentRose
    ConnectionStatus.OFFLINE,
    ConnectionStatus.NOT_CONNECTED -> TextTertiary
  }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .clip(RoundedCornerShape(20.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp)
      .testTag("connection_status_badge")
  ) {
    Box(
      modifier = Modifier
        .size(6.dp)
        .clip(CircleShape)
        .background(dotColor)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = status.label,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
      color = TextSecondary
    )
  }
}

@Composable
fun PartnerStatusBadge(
  partnerStatus: PartnerStatus = PartnerStatus.SOLO,
  modifier: Modifier = Modifier
) {
  val dotColor = when (partnerStatus) {
    PartnerStatus.CONNECTED -> AccentBlue
    PartnerStatus.TYPING -> AccentEmerald
    PartnerStatus.WAITING -> AccentAmber
    PartnerStatus.OFFLINE,
    PartnerStatus.SOLO -> TextTertiary
  }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .clip(RoundedCornerShape(20.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp)
      .testTag("partner_status_badge")
  ) {
    Box(
      modifier = Modifier
        .size(6.dp)
        .clip(CircleShape)
        .background(dotColor)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = partnerStatus.label,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
      color = TextSecondary
    )
  }
}
