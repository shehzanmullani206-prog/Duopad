package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ChangeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChangeDao {

  @Query("SELECT * FROM changes WHERE plan_id = :planId ORDER BY timestamp DESC")
  fun getChangesForPlan(planId: String): Flow<List<ChangeEntity>>

  @Query("SELECT * FROM changes WHERE plan_id = :planId AND is_acknowledged = 0 ORDER BY timestamp DESC")
  fun getUnacknowledgedChanges(planId: String): Flow<List<ChangeEntity>>

  @Query("SELECT * FROM changes WHERE plan_id = :planId AND (description LIKE '%' || :query || '%' OR new_content LIKE '%' || :query || '%' OR user_name LIKE '%' || :query || '%') ORDER BY timestamp DESC")
  fun searchChanges(planId: String, query: String): Flow<List<ChangeEntity>>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertChange(change: ChangeEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertChanges(changes: List<ChangeEntity>)

  @Query("UPDATE changes SET is_acknowledged = 1 WHERE plan_id = :planId")
  suspend fun markAllAcknowledged(planId: String)

  @Query("UPDATE changes SET is_acknowledged = 1 WHERE id = :changeId")
  suspend fun markChangeAcknowledged(changeId: String)

  @Query("SELECT EXISTS(SELECT 1 FROM changes WHERE operation_id = :operationId)")
  suspend fun isOperationRecorded(operationId: String): Boolean

  @Query("SELECT COUNT(*) FROM changes WHERE plan_id = :planId AND is_acknowledged = 0")
  fun countUnacknowledged(planId: String): Flow<Int>

  @Query("DELETE FROM changes WHERE plan_id = :planId")
  suspend fun deleteChangesForPlan(planId: String)
}
