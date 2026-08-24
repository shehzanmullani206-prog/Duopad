package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.converter.DocumentJsonConverter
import com.example.data.local.dao.PlanDao
import com.example.data.local.entity.PlanEntity
import com.example.data.model.BlockType
import com.example.data.model.EditorBlock
import com.example.data.model.HeadingLevel
import com.example.data.model.RichSpan
import com.example.data.model.SharedPlanResponse
import com.example.data.network.CollaborationApiClient
import com.example.data.user.UserManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class PlanRepository(
  private val planDao: PlanDao,
  private val apiClient: CollaborationApiClient? = null,
  private val userManager: UserManager? = null,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

  val allPlans: Flow<List<PlanEntity>> = planDao.getAllPlans()

  fun observePlan(id: String): Flow<PlanEntity?> {
    return planDao.getPlanById(id)
  }

  suspend fun getPlan(id: String): PlanEntity? = withContext(ioDispatcher) {
    planDao.getPlanByIdOnce(id)
  }

  suspend fun createPlan(
    title: String,
    description: String = "",
    initialBlocks: List<EditorBlock>? = null,
    isShared: Boolean = false
  ): String = withContext(ioDispatcher) {
    val blocks = initialBlocks ?: listOf(
      EditorBlock(
        id = UUID.randomUUID().toString(),
        type = BlockType.PARAGRAPH,
        text = ""
      )
    )

    var planId = UUID.randomUUID().toString()
    var inviteCode = ""
    var ownerId = userManager?.userId ?: ""
    var ownerName = userManager?.displayName ?: "Owner"
    var revision = 1

    if (isShared && apiClient != null && userManager != null) {
      try {
        // Authenticate session if needed
        val sessionResult = apiClient.authenticate(
          userManager.serverBaseUrl,
          userManager.userId,
          userManager.displayName
        )
        if (sessionResult.isSuccess) {
          val session = sessionResult.getOrThrow()
          userManager.authToken = session.token
          val createResult = apiClient.createSharedPlan(
            userManager.serverBaseUrl,
            session.token,
            title,
            description,
            blocks
          )
          if (createResult.isSuccess) {
            val sharedPlan = createResult.getOrThrow()
            planId = sharedPlan.planId
            inviteCode = sharedPlan.inviteCode
            ownerId = sharedPlan.ownerId
            ownerName = sharedPlan.ownerName
            revision = sharedPlan.revision
          }
        }
      } catch (e: Exception) {
        // Fallback to local plan if server is temporarily unreachable
        if (inviteCode.isBlank()) {
          val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
          inviteCode = "DUO-" + (1..3).map { chars.random() }.joinToString("") + "-" + (1..2).map { chars.random() }.joinToString("")
        }
      }
    }

    val blocksJson = DocumentJsonConverter.blocksToJson(blocks)
    val now = System.currentTimeMillis()
    val entity = PlanEntity(
      id = planId,
      title = if (title.isBlank()) "Untitled Plan" else title.trim(),
      description = description.trim(),
      blocksJson = blocksJson,
      wordCount = calculateWordCount(blocks),
      charCount = calculateCharCount(blocks),
      isShared = isShared,
      inviteCode = inviteCode,
      ownerId = ownerId,
      ownerName = ownerName,
      partnerId = "",
      partnerName = "",
      revision = revision,
      createdAt = now,
      updatedAt = now
    )
    planDao.insertPlan(entity)
    planId
  }

  suspend fun joinSharedPlan(inviteCode: String): Result<String> = withContext(ioDispatcher) {
    if (inviteCode.isBlank()) {
      return@withContext Result.failure(IllegalArgumentException("Please enter a valid invite code"))
    }

    val sanitizedCode = inviteCode.trim().uppercase()

    // Check if we already have this plan locally
    val existingLocal = planDao.getPlanByInviteCode(sanitizedCode)
    if (existingLocal != null) {
      return@withContext Result.success(existingLocal.id)
    }

    if (apiClient != null && userManager != null) {
      try {
        // Authenticate session
        val authResult = apiClient.authenticate(
          userManager.serverBaseUrl,
          userManager.userId,
          userManager.displayName
        )
        val token = if (authResult.isSuccess) {
          authResult.getOrThrow().token.also { userManager.authToken = it }
        } else {
          userManager.authToken ?: ""
        }

        val joinResult = apiClient.joinSharedPlan(
          userManager.serverBaseUrl,
          token,
          sanitizedCode
        )

        if (joinResult.isSuccess) {
          val sharedPlan = joinResult.getOrThrow()
          val entity = PlanEntity(
            id = sharedPlan.planId,
            title = sharedPlan.title,
            description = sharedPlan.description,
            blocksJson = DocumentJsonConverter.blocksToJson(sharedPlan.blocks),
            wordCount = calculateWordCount(sharedPlan.blocks),
            charCount = calculateCharCount(sharedPlan.blocks),
            isShared = true,
            inviteCode = sharedPlan.inviteCode,
            ownerId = sharedPlan.ownerId,
            ownerName = sharedPlan.ownerName,
            partnerId = sharedPlan.partnerId ?: userManager.userId,
            partnerName = sharedPlan.partnerName ?: userManager.displayName,
            revision = sharedPlan.revision,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
          )
          planDao.insertPlan(entity)
          return@withContext Result.success(sharedPlan.planId)
        } else {
          return@withContext Result.failure(joinResult.exceptionOrNull() ?: Exception("Failed to join shared plan"))
        }
      } catch (e: Exception) {
        return@withContext Result.failure(e)
      }
    }

    Result.failure(Exception("Network client is not initialized"))
  }

  suspend fun savePlanContent(
    id: String,
    blocks: List<EditorBlock>,
    wordCount: Int,
    charCount: Int
  ) = withContext(ioDispatcher) {
    val blocksJson = DocumentJsonConverter.blocksToJson(blocks)
    planDao.updatePlanContent(
      id = id,
      blocksJson = blocksJson,
      wordCount = wordCount,
      charCount = charCount,
      updatedAt = System.currentTimeMillis()
    )
  }

  suspend fun updatePlanFromRemote(
    id: String,
    title: String,
    description: String,
    blocks: List<EditorBlock>,
    revision: Int,
    partnerId: String?,
    partnerName: String?
  ) = withContext(ioDispatcher) {
    val existing = planDao.getPlanByIdOnce(id)
    val blocksJson = DocumentJsonConverter.blocksToJson(blocks)
    val now = System.currentTimeMillis()
    val updated = PlanEntity(
      id = id,
      title = title,
      description = description,
      blocksJson = blocksJson,
      wordCount = calculateWordCount(blocks),
      charCount = calculateCharCount(blocks),
      isShared = true,
      inviteCode = existing?.inviteCode ?: "",
      ownerId = existing?.ownerId ?: "",
      ownerName = existing?.ownerName ?: "",
      partnerId = partnerId ?: existing?.partnerId ?: "",
      partnerName = partnerName ?: existing?.partnerName ?: "",
      revision = revision,
      createdAt = existing?.createdAt ?: now,
      updatedAt = now
    )
    planDao.insertPlan(updated)
  }

  suspend fun renamePlan(id: String, newTitle: String) = withContext(ioDispatcher) {
    if (newTitle.isNotBlank()) {
      planDao.updatePlanTitle(id, newTitle.trim(), System.currentTimeMillis())
    }
  }

  suspend fun updatePlanDescription(id: String, newDescription: String) = withContext(ioDispatcher) {
    planDao.updatePlanDescription(id, newDescription.trim(), System.currentTimeMillis())
  }

  suspend fun deletePlan(id: String) = withContext(ioDispatcher) {
    planDao.deletePlanById(id)
  }

  suspend fun ensureDefaultPlanExists(): String = withContext(ioDispatcher) {
    val count = planDao.getPlanCount()
    if (count == 0) {
      val defaultBlocks = listOf(
        EditorBlock(
          id = UUID.randomUUID().toString(),
          type = BlockType.HEADING,
          headingLevel = HeadingLevel.H1,
          text = "Summer 2026 Project Roadmap",
          spans = listOf(
            RichSpan(start = 0, end = 27, isBold = true, textColorHex = "#60A5FA")
          )
        ),
        EditorBlock(
          id = UUID.randomUUID().toString(),
          type = BlockType.QUOTE,
          text = "A shared digital workspace for planning, syncing, and executing our ideas."
        ),
        EditorBlock(
          id = UUID.randomUUID().toString(),
          type = BlockType.DIVIDER,
          text = ""
        ),
        EditorBlock(
          id = UUID.randomUUID().toString(),
          type = BlockType.HEADING,
          headingLevel = HeadingLevel.H2,
          text = "Milestones & Key Deliverables"
        ),
        EditorBlock(
          id = UUID.randomUUID().toString(),
          type = BlockType.CHECKLIST,
          text = "Complete Local Room Persistence & Auto-Save",
          isChecked = true
        ),
        EditorBlock(
          id = UUID.randomUUID().toString(),
          type = BlockType.CHECKLIST,
          text = "Implement 2-Person Realtime Sync & Invite Codes",
          isChecked = true
        ),
        EditorBlock(
          id = UUID.randomUUID().toString(),
          type = BlockType.CHECKLIST,
          text = "Verify Fast Operation-Based Edits & Rich Text Recovery",
          isChecked = false
        ),
        EditorBlock(
          id = UUID.randomUUID().toString(),
          type = BlockType.PARAGRAPH,
          text = "You can add new blocks, format text using the left toolbar, and changes will be synced in real-time."
        )
      )
      createPlan(
        title = "Summer 2026 Project Roadmap",
        description = "Collaborative planning notepad with real-time sync",
        initialBlocks = defaultBlocks,
        isShared = true
      )
    } else {
      ""
    }
  }

  private fun calculateWordCount(blocks: List<EditorBlock>): Int {
    return blocks.sumOf { block ->
      val text = block.text.trim()
      if (text.isEmpty()) 0 else text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
  }

  private fun calculateCharCount(blocks: List<EditorBlock>): Int {
    return blocks.sumOf { it.text.length }
  }

  companion object {
    @Volatile
    private var INSTANCE: PlanRepository? = null

    fun getInstance(
      context: Context,
      apiClient: CollaborationApiClient? = null,
      userManager: UserManager? = null
    ): PlanRepository {
      return INSTANCE ?: synchronized(this) {
        val database = AppDatabase.getInstance(context)
        val finalApiClient = apiClient ?: CollaborationApiClient()
        val finalUserManager = userManager ?: UserManager.getInstance(context)
        val instance = PlanRepository(database.planDao(), finalApiClient, finalUserManager)
        INSTANCE = instance
        instance
      }
    }
  }
}
