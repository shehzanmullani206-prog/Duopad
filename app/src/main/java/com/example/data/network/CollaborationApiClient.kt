package com.example.data.network

import com.example.data.local.converter.DocumentJsonConverter
import com.example.data.model.BlockType
import com.example.data.model.ChangeAction
import com.example.data.model.ChangeRecord
import com.example.data.model.EditorBlock
import com.example.data.model.SharedPlanResponse
import com.example.data.model.UserProfile
import com.example.data.model.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CollaborationApiClient(
  private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(8, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .writeTimeout(10, TimeUnit.SECONDS)
    .build()
) {

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun authenticate(
    baseUrl: String,
    userId: String,
    displayName: String
  ): Result<UserSession> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("userId", userId)
        put("displayName", displayName)
      }

      val request = Request.Builder()
        .url("${baseUrl.removeSuffix("/")}/api/auth/session")
        .post(payload.toString().toRequestBody(jsonMediaType))
        .build()

      val response = okHttpClient.newCall(request).execute()
      val body = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("Server returned status ${response.code}: $body"))
      }

      val json = JSONObject(body)
      val token = json.getString("token")
      val expiresAt = json.optLong("expiresAt", 0L)
      val userObj = json.getJSONObject("user")

      val user = UserProfile(
        userId = userObj.getString("userId"),
        displayName = userObj.getString("displayName"),
        avatarPlaceholder = userObj.optString("avatarPlaceholder", "")
      )

      Result.success(UserSession(token = token, user = user, expiresAt = expiresAt))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun createSharedPlan(
    baseUrl: String,
    token: String,
    title: String,
    description: String,
    blocks: List<EditorBlock>
  ): Result<SharedPlanResponse> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("title", title)
        put("description", description)
        put("blocks", JSONArray(DocumentJsonConverter.blocksToJson(blocks)))
      }

      val request = Request.Builder()
        .url("${baseUrl.removeSuffix("/")}/api/plans/create")
        .addHeader("Authorization", "Bearer $token")
        .post(payload.toString().toRequestBody(jsonMediaType))
        .build()

      val response = okHttpClient.newCall(request).execute()
      val body = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("Create plan failed: $body"))
      }

      val json = JSONObject(body)
      val planObj = json.getJSONObject("plan")
      Result.success(parseSharedPlan(planObj))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun joinSharedPlan(
    baseUrl: String,
    token: String,
    inviteCode: String
  ): Result<SharedPlanResponse> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("inviteCode", inviteCode.trim().uppercase())
      }

      val request = Request.Builder()
        .url("${baseUrl.removeSuffix("/")}/api/plans/join")
        .addHeader("Authorization", "Bearer $token")
        .post(payload.toString().toRequestBody(jsonMediaType))
        .build()

      val response = okHttpClient.newCall(request).execute()
      val body = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        val errorMsg = try {
          JSONObject(body).optString("error", "Failed to join plan (${response.code})")
        } catch (e: Exception) {
          "Failed to join plan (${response.code})"
        }
        return@withContext Result.failure(Exception(errorMsg))
      }

      val json = JSONObject(body)
      val planObj = json.getJSONObject("plan")
      Result.success(parseSharedPlan(planObj))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getSharedPlan(
    baseUrl: String,
    token: String,
    planId: String
  ): Result<SharedPlanResponse> = withContext(Dispatchers.IO) {
    try {
      val request = Request.Builder()
        .url("${baseUrl.removeSuffix("/")}/api/plans/$planId")
        .addHeader("Authorization", "Bearer $token")
        .get()
        .build()

      val response = okHttpClient.newCall(request).execute()
      val body = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        val errorMsg = try {
          JSONObject(body).optString("error", "Plan not found")
        } catch (e: Exception) {
          "Plan not found (${response.code})"
        }
        return@withContext Result.failure(Exception(errorMsg))
      }

      val json = JSONObject(body)
      val planObj = json.getJSONObject("plan")
      Result.success(parseSharedPlan(planObj))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getPlanHistory(
    baseUrl: String,
    token: String,
    planId: String
  ): Result<List<ChangeRecord>> = withContext(Dispatchers.IO) {
    try {
      val request = Request.Builder()
        .url("${baseUrl.removeSuffix("/")}/api/plans/$planId/history")
        .header("Authorization", "Bearer $token")
        .get()
        .build()

      val response = okHttpClient.newCall(request).execute()
      val body = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        val errorMsg = try {
          JSONObject(body).optString("error", "Failed to fetch history")
        } catch (e: Exception) {
          "Failed to fetch history (${response.code})"
        }
        return@withContext Result.failure(Exception(errorMsg))
      }

      val json = JSONObject(body)
      val historyArray = json.optJSONArray("history") ?: JSONArray()
      val records = mutableListOf<ChangeRecord>()

      for (i in 0 until historyArray.length()) {
        val item = historyArray.getJSONObject(i)
        val actionStr = item.optString("action", ChangeAction.REPLACE.name)
        val action = try { ChangeAction.valueOf(actionStr) } catch (e: Exception) { ChangeAction.REPLACE }
        val blockTypeStr = item.optString("blockType", "")
        val blockType = try { if (blockTypeStr.isNotBlank()) BlockType.valueOf(blockTypeStr) else null } catch (e: Exception) { null }

        records.add(
          ChangeRecord(
            changeId = item.optString("changeId", item.optString("operationId")),
            operationId = item.optString("operationId"),
            planId = item.optString("planId", planId),
            userId = item.optString("userId"),
            userName = item.optString("userName", "Collaborator"),
            action = action,
            blockId = if (item.isNull("blockId")) null else item.optString("blockId", null),
            blockType = blockType,
            oldContent = item.optString("oldContent", ""),
            newContent = item.optString("newContent", ""),
            description = item.optString("description", ""),
            timestamp = item.optLong("timestamp", System.currentTimeMillis()),
            revision = item.optInt("revision", 1),
            isAcknowledged = item.optBoolean("isAcknowledged", false),
            isPartner = false
          )
        )
      }

      Result.success(records)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun acknowledgeHistory(
    baseUrl: String,
    token: String,
    planId: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("token", token)
      }
      val request = Request.Builder()
        .url("${baseUrl.removeSuffix("/")}/api/plans/$planId/history/acknowledge")
        .header("Authorization", "Bearer $token")
        .post(payload.toString().toRequestBody(jsonMediaType))
        .build()

      val response = okHttpClient.newCall(request).execute()
      if (response.isSuccessful) {
        Result.success(Unit)
      } else {
        Result.failure(Exception("Failed to acknowledge history (${response.code})"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun parseSharedPlan(planObj: JSONObject): SharedPlanResponse {
    val planId = planObj.getString("planId")
    val title = planObj.getString("title")
    val description = planObj.optString("description", "")
    val inviteCode = planObj.optString("inviteCode", "")
    val ownerId = planObj.getString("ownerId")
    val ownerName = planObj.optString("ownerName", "Owner")
    val partnerId = if (planObj.isNull("partnerId")) null else planObj.optString("partnerId", null)
    val partnerName = if (planObj.isNull("partnerName")) null else planObj.optString("partnerName", null)
    val revision = planObj.optInt("revision", 1)

    val blocksRaw = planObj.optJSONArray("blocks")
    val blocks = if (blocksRaw != null) {
      DocumentJsonConverter.jsonToBlocks(blocksRaw.toString())
    } else {
      emptyList()
    }

    return SharedPlanResponse(
      planId = planId,
      title = title,
      description = description,
      inviteCode = inviteCode,
      ownerId = ownerId,
      ownerName = ownerName,
      partnerId = partnerId,
      partnerName = partnerName,
      revision = revision,
      blocks = blocks
    )
  }
}
