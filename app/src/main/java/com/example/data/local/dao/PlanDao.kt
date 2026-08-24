package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

  @Query("SELECT * FROM plans ORDER BY updated_at DESC")
  fun getAllPlans(): Flow<List<PlanEntity>>

  @Query("SELECT * FROM plans WHERE id = :id LIMIT 1")
  fun getPlanById(id: String): Flow<PlanEntity?>

  @Query("SELECT * FROM plans WHERE id = :id LIMIT 1")
  suspend fun getPlanByIdOnce(id: String): PlanEntity?

  @Query("SELECT COUNT(*) FROM plans")
  suspend fun getPlanCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPlan(plan: PlanEntity)

  @Update
  suspend fun updatePlan(plan: PlanEntity)

  @Query("UPDATE plans SET title = :title, updated_at = :updatedAt WHERE id = :id")
  suspend fun updatePlanTitle(id: String, title: String, updatedAt: Long = System.currentTimeMillis())

  @Query("UPDATE plans SET description = :description, updated_at = :updatedAt WHERE id = :id")
  suspend fun updatePlanDescription(id: String, description: String, updatedAt: Long = System.currentTimeMillis())

  @Query("UPDATE plans SET blocks_json = :blocksJson, word_count = :wordCount, char_count = :charCount, updated_at = :updatedAt WHERE id = :id")
  suspend fun updatePlanContent(
    id: String,
    blocksJson: String,
    wordCount: Int,
    charCount: Int,
    updatedAt: Long = System.currentTimeMillis()
  )

  @Query("UPDATE plans SET is_shared = :isShared, invite_code = :inviteCode, owner_id = :ownerId, owner_name = :ownerName, partner_id = :partnerId, partner_name = :partnerName, revision = :revision, updated_at = :updatedAt WHERE id = :id")
  suspend fun updatePlanSharing(
    id: String,
    isShared: Boolean,
    inviteCode: String,
    ownerId: String,
    ownerName: String,
    partnerId: String,
    partnerName: String,
    revision: Int,
    updatedAt: Long = System.currentTimeMillis()
  )

  @Query("UPDATE plans SET partner_id = :partnerId, partner_name = :partnerName, updated_at = :updatedAt WHERE id = :id")
  suspend fun updatePlanPartner(
    id: String,
    partnerId: String,
    partnerName: String,
    updatedAt: Long = System.currentTimeMillis()
  )

  @Query("SELECT * FROM plans WHERE invite_code = :inviteCode LIMIT 1")
  suspend fun getPlanByInviteCode(inviteCode: String): PlanEntity?

  @Query("DELETE FROM plans WHERE id = :id")
  suspend fun deletePlanById(id: String)
}
