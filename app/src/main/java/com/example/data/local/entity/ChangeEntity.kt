package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "changes",
  indices = [
    Index(value = ["plan_id", "timestamp"]),
    Index(value = ["operation_id"], unique = false)
  ]
)
data class ChangeEntity(
  @PrimaryKey
  @ColumnInfo(name = "id")
  val id: String,

  @ColumnInfo(name = "operation_id")
  val operationId: String,

  @ColumnInfo(name = "plan_id")
  val planId: String,

  @ColumnInfo(name = "user_id")
  val userId: String,

  @ColumnInfo(name = "user_name")
  val userName: String,

  @ColumnInfo(name = "action")
  val action: String,

  @ColumnInfo(name = "block_id")
  val blockId: String? = null,

  @ColumnInfo(name = "block_type")
  val blockType: String? = null,

  @ColumnInfo(name = "old_content")
  val oldContent: String = "",

  @ColumnInfo(name = "new_content")
  val newContent: String = "",

  @ColumnInfo(name = "description")
  val description: String = "",

  @ColumnInfo(name = "timestamp")
  val timestamp: Long = System.currentTimeMillis(),

  @ColumnInfo(name = "revision")
  val revision: Int = 1,

  @ColumnInfo(name = "is_acknowledged")
  val isAcknowledged: Boolean = false,

  @ColumnInfo(name = "is_partner")
  val isPartner: Boolean = false
)
