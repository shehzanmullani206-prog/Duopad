package com.example.data.model

data class ChangeHighlight(
  val blockId: String,
  val start: Int = 0,
  val end: Int = 0,
  val userId: String,
  val userName: String,
  val isPartner: Boolean,
  val changeId: String,
  val timestamp: Long = System.currentTimeMillis(),
  val action: ChangeAction = ChangeAction.REPLACE,
  val description: String = ""
)
