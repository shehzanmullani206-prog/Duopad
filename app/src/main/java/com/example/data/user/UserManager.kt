package com.example.data.user

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class UserManager private constructor(context: Context) {

  private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
    PREFS_NAME,
    Context.MODE_PRIVATE
  )

  private val _currentUser = MutableStateFlow(loadUser())
  val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

  val userId: String
    get() = _currentUser.value.userId

  val displayName: String
    get() = _currentUser.value.displayName

  var serverBaseUrl: String
    get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    set(value) {
      prefs.edit().putString(KEY_SERVER_URL, value.trim()).apply()
    }

  var authToken: String?
    get() = prefs.getString(KEY_AUTH_TOKEN, null)
    set(value) {
      prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()
    }

  private fun loadUser(): UserProfile {
    var storedId = prefs.getString(KEY_USER_ID, null)
    if (storedId.isNullOrBlank()) {
      storedId = UUID.randomUUID().toString()
      prefs.edit().putString(KEY_USER_ID, storedId).apply()
    }

    var storedName = prefs.getString(KEY_DISPLAY_NAME, null)
    if (storedName.isNullOrBlank()) {
      val randomNames = listOf("Alex", "Sam", "Jordan", "Taylor", "Morgan", "Casey", "Riley", "Avery")
      storedName = randomNames.random()
      prefs.edit().putString(KEY_DISPLAY_NAME, storedName).apply()
    }

    return UserProfile(
      userId = storedId,
      displayName = storedName,
      avatarPlaceholder = storedName.take(2).uppercase()
    )
  }

  fun updateDisplayName(newName: String) {
    val trimmed = newName.trim()
    if (trimmed.isNotBlank()) {
      prefs.edit().putString(KEY_DISPLAY_NAME, trimmed).apply()
      val updated = _currentUser.value.copy(
        displayName = trimmed,
        avatarPlaceholder = trimmed.take(2).uppercase()
      )
      _currentUser.value = updated
    }
  }

  fun getWsUrl(): String {
    val httpUrl = serverBaseUrl
    return if (httpUrl.startsWith("https://")) {
      httpUrl.replace("https://", "wss://").removeSuffix("/") + "/ws"
    } else {
      httpUrl.replace("http://", "ws://").removeSuffix("/") + "/ws"
    }
  }

  companion object {
    private const val PREFS_NAME = "duoplan_user_prefs"
    private const val KEY_USER_ID = "pref_user_id"
    private const val KEY_DISPLAY_NAME = "pref_display_name"
    private const val KEY_AUTH_TOKEN = "pref_auth_token"
    private const val KEY_SERVER_URL = "pref_server_url"

    // Default to Android Emulator loopback alias for host machine 8080
    const val DEFAULT_SERVER_URL = "http://10.0.2.2:8080"

    @Volatile
    private var INSTANCE: UserManager? = null

    fun getInstance(context: Context): UserManager {
      return INSTANCE ?: synchronized(this) {
        val instance = UserManager(context)
        INSTANCE = instance
        instance
      }
    }
  }
}
