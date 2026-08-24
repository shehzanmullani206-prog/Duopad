package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plans")
data class PlanEntity(
  @PrimaryKey
  @ColumnInfo(name = "id")
  val id: String,

  @ColumnInfo(name = "title")
  val title: String,

  @ColumnInfo(name = "description")
  val description: String = "",

  @ColumnInfo(name = "blocks_json")
  val blocksJson: String = "[]",

  @ColumnInfo(name = "word_count")
  val wordCount: Int = 0,

  @ColumnInfo(name = "char_count")
  val charCount: Int = 0,

  @ColumnInfo(name = "is_shared")
  val isShared: Boolean = false,

  @ColumnInfo(name = "invite_code")
  val inviteCode: String = "",

  @ColumnInfo(name = "owner_id")
  val ownerId: String = "",

  @ColumnInfo(name = "owner_name")
  val ownerName: String = "",

  @ColumnInfo(name = "partner_id")
  val partnerId: String = "",

  @ColumnInfo(name = "partner_name")
  val partnerName: String = "",

  @ColumnInfo(name = "revision")
  val revision: Int = 1,

  @ColumnInfo(name = "created_at")
  val createdAt: Long = System.currentTimeMillis(),

  @ColumnInfo(name = "updated_at")
  val updatedAt: Long = System.currentTimeMillis()
)
