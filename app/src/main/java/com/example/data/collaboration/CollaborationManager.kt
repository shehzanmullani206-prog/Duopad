package com.example.data.collaboration

import android.content.Context
import android.util.Log
import com.example.data.local.converter.DocumentJsonConverter
import com.example.data.model.BlockType
import com.example.data.model.ChangeAction
import com.example.data.model.ChangeRecord
import com.example.data.model.CollaborationOperation
import com.example.data.model.ConnectionStatus
import com.example.data.model.OperationType
import com.example.data.model.PartnerStatus
import com.example.data.model.SharedPlanResponse
import com.example.data.user.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class CollaborationManager(
  private val context: Context,
  private val userManager: UserManager = UserManager.getInstance(context)
) {

  private val tag = "CollaborationManager"
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val okHttpClient = OkHttpClient.Builder()
    .readTimeout(0, TimeUnit.MILLISECONDS)
    .pingInterval(20, TimeUnit.SECONDS)
    .build()

  private var webSocket: WebSocket? = null
  private var currentPlanId: String? = null
  private var reconnectJob: Job? = null
  private var typingResetJob: Job? = null
  private val isManuallyClosed = AtomicBoolean(false)
  private var reconnectAttempts = 0

  private val _connectionStatus = MutableStateFlow(ConnectionStatus.NOT_CONNECTED)
  val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

  private val _partnerStatus = MutableStateFlow(PartnerStatus.SOLO)
  val partnerStatus: StateFlow<PartnerStatus> = _partnerStatus.asStateFlow()

  private val _remoteOperations = MutableSharedFlow<CollaborationOperation>(extraBufferCapacity = 64)
  val remoteOperations: SharedFlow<CollaborationOperation> = _remoteOperations.asSharedFlow()

  private val _incomingChanges = MutableSharedFlow<ChangeRecord>(extraBufferCapacity = 64)
  val incomingChanges: SharedFlow<ChangeRecord> = _incomingChanges.asSharedFlow()

  private val _authoritativeDocument = MutableSharedFlow<SharedPlanResponse>(extraBufferCapacity = 8)
  val authoritativeDocument: SharedFlow<SharedPlanResponse> = _authoritativeDocument.asSharedFlow()

  fun connectToPlan(planId: String, isShared: Boolean) {
    if (!isShared) {
      disconnect()
      _connectionStatus.value = ConnectionStatus.OFFLINE
      _partnerStatus.value = PartnerStatus.SOLO
      return
    }

    if (currentPlanId == planId && _connectionStatus.value == ConnectionStatus.CONNECTED) {
      return
    }

    currentPlanId = planId
    isManuallyClosed.set(false)
    reconnectAttempts = 0
    initiateConnection()
  }

  private fun initiateConnection() {
    val planId = currentPlanId ?: return
    if (isManuallyClosed.get()) return

    _connectionStatus.value = if (reconnectAttempts > 0) ConnectionStatus.RECONNECTING else ConnectionStatus.CONNECTING

    val wsUrl = userManager.getWsUrl()
    val token = userManager.authToken ?: ""

    val request = Request.Builder()
      .url(wsUrl)
      .build()

    webSocket?.close(1000, "Reconnecting")
    webSocket = okHttpClient.newWebSocket(request, createWebSocketListener(planId, token))
  }

  private fun createWebSocketListener(planId: String, token: String) = object : WebSocketListener() {
    override fun onOpen(ws: WebSocket, response: Response) {
      Log.d(tag, "WebSocket connection opened to server")
      _connectionStatus.value = ConnectionStatus.CONNECTED
      reconnectAttempts = 0

      // Send JOIN_ROOM message with token & planId
      val joinMsg = JSONObject().apply {
        put("type", "JOIN_ROOM")
        put("planId", planId)
        put("token", token)
        put("userId", userManager.userId)
        put("userName", userManager.displayName)
      }
      ws.send(joinMsg.toString())
    }

    override fun onMessage(ws: WebSocket, text: String) {
      handleIncomingMessage(text)
    }

    override fun onClosing(ws: WebSocket, code: Int, reason: String) {
      Log.d(tag, "WebSocket closing: $code / $reason")
    }

    override fun onClosed(ws: WebSocket, code: Int, reason: String) {
      Log.d(tag, "WebSocket closed: $code / $reason")
      if (!isManuallyClosed.get()) {
        scheduleReconnect()
      } else {
        _connectionStatus.value = ConnectionStatus.NOT_CONNECTED
      }
    }

    override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
      Log.w(tag, "WebSocket failure: ${t.message}")
      if (!isManuallyClosed.get()) {
        _connectionStatus.value = ConnectionStatus.ERROR
        scheduleReconnect()
      }
    }
  }

  private fun handleIncomingMessage(text: String) {
    try {
      val json = JSONObject(text)
      when (json.optString("type")) {
        "JOIN_ACK" -> {
          _connectionStatus.value = ConnectionStatus.CONNECTED
          val docJson = json.optJSONObject("document")
          if (docJson != null) {
            val response = parsePlanResponse(docJson)
            scope.launch {
              _authoritativeDocument.emit(response)
            }
            if (response.partnerId != null) {
              _partnerStatus.value = PartnerStatus.CONNECTED
            } else {
              _partnerStatus.value = PartnerStatus.WAITING
            }
          }
        }

        "REMOTE_OPERATION" -> {
          val opObj = json.optJSONObject("operation")
          if (opObj != null) {
            val op = parseOperation(opObj)
            // Filter out own echo operations
            if (op.userId != userManager.userId) {
              scope.launch {
                _remoteOperations.emit(op)
              }
            }
          }
          val changeObj = json.optJSONObject("changeRecord")
          if (changeObj != null) {
            val change = parseChangeRecord(changeObj, isPartner = true)
            if (change.userId != userManager.userId) {
              scope.launch {
                _incomingChanges.emit(change)
              }
            }
          }
        }

        "CHANGE_EVENT" -> {
          val changeObj = json.optJSONObject("changeRecord")
          if (changeObj != null) {
            val isPartner = changeObj.optString("userId") != userManager.userId
            val change = parseChangeRecord(changeObj, isPartner = isPartner)
            scope.launch {
              _incomingChanges.emit(change)
            }
          }
        }

        "OPERATION_ACK" -> {
          // Operation confirmed by server
          Log.d(tag, "Operation successfully acknowledged by server")
          val changeObj = json.optJSONObject("changeRecord")
          if (changeObj != null) {
            val change = parseChangeRecord(changeObj, isPartner = false)
            scope.launch {
              _incomingChanges.emit(change)
            }
          }
        }

        "PRESENCE" -> {
          val presObj = json.optJSONObject("presence")
          if (presObj != null) {
            val isOnline = presObj.optString("status") == "ONLINE"
            val userId = presObj.optString("userId")
            if (userId != userManager.userId) {
              _partnerStatus.value = if (isOnline) PartnerStatus.CONNECTED else PartnerStatus.WAITING
            }
          }
        }

        "TYPING" -> {
          val typingObj = json.optJSONObject("typing")
          if (typingObj != null) {
            val isTyping = typingObj.optBoolean("isTyping", false)
            val userId = typingObj.optString("userId")
            if (userId != userManager.userId) {
              if (isTyping) {
                _partnerStatus.value = PartnerStatus.TYPING
                typingResetJob?.cancel()
                typingResetJob = scope.launch {
                  delay(2500)
                  if (_partnerStatus.value == PartnerStatus.TYPING) {
                    _partnerStatus.value = PartnerStatus.CONNECTED
                  }
                }
              } else {
                _partnerStatus.value = PartnerStatus.CONNECTED
              }
            }
          }
        }

        "ERROR" -> {
          val errMsg = json.optString("error", "Unknown server error")
          Log.e(tag, "Server error: $errMsg")
        }
      }
    } catch (e: Exception) {
      Log.e(tag, "Error parsing WebSocket message", e)
    }
  }

  fun sendOperation(operation: CollaborationOperation) {
    val ws = webSocket
    if (ws != null && _connectionStatus.value == ConnectionStatus.CONNECTED) {
      val opObj = JSONObject().apply {
        put("operationId", operation.operationId)
        put("planId", operation.planId)
        put("userId", operation.userId)
        put("userName", operation.userName)
        put("type", operation.type.name)
        put("revision", operation.revision)
        put("payload", operation.payload)
        put("timestamp", operation.timestamp)
      }

      val msg = JSONObject().apply {
        put("type", "OPERATION")
        put("planId", operation.planId)
        put("operation", opObj)
      }

      ws.send(msg.toString())
    }
  }

  fun sendTyping(isTyping: Boolean) {
    val ws = webSocket
    val planId = currentPlanId
    if (ws != null && planId != null && _connectionStatus.value == ConnectionStatus.CONNECTED) {
      val msg = JSONObject().apply {
        put("type", "TYPING")
        put("planId", planId)
        put("typing", JSONObject().apply {
          put("userId", userManager.userId)
          put("userName", userManager.displayName)
          put("isTyping", isTyping)
        })
      }
      ws.send(msg.toString())
    }
  }

  private fun scheduleReconnect() {
    reconnectJob?.cancel()
    if (isManuallyClosed.get() || currentPlanId == null) return

    reconnectAttempts++
    val backoffMs = (1000L * (1L shl minOf(reconnectAttempts, 4))).coerceAtMost(15000L)

    _connectionStatus.value = ConnectionStatus.RECONNECTING
    reconnectJob = scope.launch {
      delay(backoffMs)
      if (!isManuallyClosed.get() && currentPlanId != null) {
        initiateConnection()
      }
    }
  }

  fun disconnect() {
    isManuallyClosed.set(true)
    reconnectJob?.cancel()
    typingResetJob?.cancel()
    currentPlanId = null
    webSocket?.close(1000, "Client disconnect")
    webSocket = null
    _connectionStatus.value = ConnectionStatus.NOT_CONNECTED
    _partnerStatus.value = PartnerStatus.SOLO
  }

  private fun parseOperation(opObj: JSONObject): CollaborationOperation {
    val typeStr = opObj.optString("type", OperationType.BLOCK_UPDATE.name)
    val type = try { OperationType.valueOf(typeStr) } catch (e: Exception) { OperationType.BLOCK_UPDATE }
    return CollaborationOperation(
      operationId = opObj.optString("operationId"),
      planId = opObj.optString("planId"),
      userId = opObj.optString("userId"),
      userName = opObj.optString("userName", ""),
      type = type,
      revision = opObj.optInt("revision", 0),
      payload = opObj.optString("payload", "{}"),
      timestamp = opObj.optLong("timestamp", System.currentTimeMillis())
    )
  }

  private fun parsePlanResponse(docJson: JSONObject): SharedPlanResponse {
    val blocksArray = docJson.optJSONArray("blocks")
    val blocks = if (blocksArray != null) {
      DocumentJsonConverter.jsonToBlocks(blocksArray.toString())
    } else {
      emptyList()
    }

    return SharedPlanResponse(
      planId = docJson.getString("planId"),
      title = docJson.getString("title"),
      description = docJson.optString("description", ""),
      inviteCode = docJson.optString("inviteCode", ""),
      ownerId = docJson.getString("ownerId"),
      ownerName = docJson.optString("ownerName", "Owner"),
      partnerId = if (docJson.isNull("partnerId")) null else docJson.optString("partnerId", null),
      partnerName = if (docJson.isNull("partnerName")) null else docJson.optString("partnerName", null),
      revision = docJson.optInt("revision", 1),
      blocks = blocks
    )
  }
}
