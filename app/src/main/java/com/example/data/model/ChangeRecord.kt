package com.example.data.model

data class ChangeRecord(
  val changeId: String,
  val operationId: String,
  val planId: String,
  val userId: String,
  val userName: String,
  val action: ChangeAction,
  val blockId: String? = null,
  val blockType: BlockType? = null,
  val oldContent: String = "",
  val newContent: String = "",
  val description: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val revision: Int = 1,
  val isAcknowledged: Boolean = false,
  val isPartner: Boolean = false
)
