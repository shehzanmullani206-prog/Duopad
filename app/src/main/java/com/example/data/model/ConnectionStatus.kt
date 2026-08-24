package com.example.data.model

enum class ConnectionStatus(val label: String) {
  OFFLINE("Offline"),
  NOT_CONNECTED("Not connected"),
  CONNECTING("Connecting..."),
  CONNECTED("Connected"),
  RECONNECTING("Reconnecting..."),
  ERROR("Connection Error")
}

enum class PartnerStatus(val label: String) {
  SOLO("Solo mode"),
  WAITING("Waiting for partner"),
  CONNECTED("Partner online"),
  OFFLINE("Partner offline"),
  TYPING("Partner is typing...")
}
