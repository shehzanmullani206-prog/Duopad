package com.example.presentation.editor.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PartnerStatus
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun SharePlanDialog(
  planTitle: String,
  inviteCode: String,
  ownerName: String,
  partnerName: String,
  partnerStatus: PartnerStatus,
  currentUserName: String,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var copied by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = DarkSurfaceElevated,
    modifier = Modifier
      .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
      .testTag("share_plan_dialog"),
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Group,
          contentDescription = null,
          tint = AccentBlue,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Plan Collaboration",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp
          ),
          color = TextPrimary
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp)
      ) {
        Text(
          text = "Collaborate in real-time with one partner. Share the invite code below to let them join:",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Invite Code Card
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCanvas)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .clickable {
              if (inviteCode.isNotBlank()) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("DuoPlan Invite Code", inviteCode)
                clipboard.setPrimaryClip(clip)
                copied = true
                Toast.makeText(context, "Invite code copied to clipboard", Toast.LENGTH_SHORT).show()
              }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("dialog_invite_code_card")
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                text = "INVITE CODE",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.2.sp
                ),
                color = TextTertiary
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = if (inviteCode.isNotBlank()) inviteCode else "GENERATING...",
                style = MaterialTheme.typography.titleLarge.copy(
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  fontSize = 20.sp,
                  letterSpacing = 2.sp
                ),
                color = AccentBlue,
                modifier = Modifier.testTag("dialog_invite_code_text")
              )
            }

            IconButton(
              onClick = {
                if (inviteCode.isNotBlank()) {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = ClipData.newPlainText("DuoPlan Invite Code", inviteCode)
                  clipboard.setPrimaryClip(clip)
                  copied = true
                  Toast.makeText(context, "Invite code copied", Toast.LENGTH_SHORT).show()
                }
              },
              modifier = Modifier.testTag("dialog_copy_invite_code_button")
            ) {
              Icon(
                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = "Copy Code",
                tint = if (copied) AccentEmerald else TextSecondary,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Participants Section (Strict 2-Person Limit)
        Text(
          text = "Participants (Max 2)",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold
          ),
          color = TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Participant 1: Creator / Owner
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(AccentBlue.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = ownerName.take(1).uppercase().ifBlank { "O" },
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = AccentBlue
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (ownerName.isNotBlank()) ownerName else "Owner",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
              color = TextPrimary
            )
            Text(
              text = "Creator",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = TextTertiary
            )
          }

          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(AccentEmerald)
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Participant 2: Partner
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          val hasPartner = partnerName.isNotBlank() || partnerStatus == PartnerStatus.CONNECTED
          val isOnline = partnerStatus == PartnerStatus.CONNECTED || partnerStatus == PartnerStatus.TYPING

          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(if (hasPartner) AccentEmerald.copy(alpha = 0.2f) else DarkBorderSubtle),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (hasPartner) (partnerName.take(1).uppercase().ifBlank { "P" }) else "?",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = if (hasPartner) AccentEmerald else TextTertiary
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (hasPartner) (if (partnerName.isNotBlank()) partnerName else "Partner") else "Waiting for partner...",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
              color = if (hasPartner) TextPrimary else TextSecondary
            )
            Text(
              text = if (hasPartner) (if (isOnline) "Online" else "Offline") else "Share code above to invite",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = if (isOnline) AccentEmerald else TextTertiary
            )
          }

          if (hasPartner) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isOnline) AccentEmerald else TextTertiary)
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("dialog_close_share_button")
      ) {
        Text("Done", color = Color.White)
      }
    }
  )
}
