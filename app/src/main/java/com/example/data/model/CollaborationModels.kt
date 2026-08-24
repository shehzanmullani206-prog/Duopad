package com.example.data.model

data class UserProfile(
  val userId: String,
  val displayName: String,
  val avatarPlaceholder: String = ""
)

data class UserSession(
  val token: String,
  val user: UserProfile,
  val expiresAt: Long = 0L
)

enum class OperationType {
  BLOCK_UPDATE,
  BLOCK_INSERT,
  BLOCK_DELETE,
  BLOCK_FORMAT,
  RENAME,
  FULL_SYNC
}

data class CollaborationOperation(
  val operationId: String,
  val planId: String,
  val userId: String,
  val userName: String = "",
  val type: OperationType,
  val revision: Int = 0,
  val payload: String = "{}",
  val timestamp: Long = System.currentTimeMillis(),
  val changeRecord: ChangeRecord? = null
)

data class PresenceUpdate(
  val userId: String,
  val userName: String,
  val isOnline: Boolean,
  val isPartner: Boolean
)

data class TypingUpdate(
  val userId: String,
  val userName: String,
  val isTyping: Boolean
)

data class SharedPlanResponse(
  val planId: String,
  val title: String,
  val description: String,
  val inviteCode: String,
  val ownerId: String,
  val ownerName: String,
  val partnerId: String?,
  val partnerName: String?,
  val revision: Int,
  val blocks: List<EditorBlock>
)
