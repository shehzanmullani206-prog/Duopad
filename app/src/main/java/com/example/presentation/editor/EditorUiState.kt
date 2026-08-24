package com.example.presentation.editor

import androidx.compose.ui.text.TextRange
import com.example.data.model.BlockType
import com.example.data.model.ConnectionStatus
import com.example.data.model.DocumentModel
import com.example.data.model.EditorBlock
import com.example.data.model.FormattingState
import com.example.data.model.HeadingLevel
import com.example.data.model.PartnerStatus
import com.example.data.model.TextAlignment

enum class SaveStatus(val label: String) {
  SAVED("Saved"),
  SAVING("Saving..."),
  UNSAVED("Unsaved changes"),
  ERROR("Save error")
}

data class EditorUiState(
  val document: DocumentModel = DocumentModel(),
  val activeBlockId: String? = null,
  val activeSelection: TextRange = TextRange.Zero,
  val isToolbarExpanded: Boolean = true,
  val isToolbarVisible: Boolean = true,
  val formattingState: FormattingState = FormattingState(),
  val connectionStatus: ConnectionStatus = ConnectionStatus.OFFLINE,
  val partnerStatus: PartnerStatus = PartnerStatus.SOLO,
  val saveStatus: SaveStatus = SaveStatus.SAVED,
  val lastSavedTime: String = "Saved locally",
  val wordCount: Int = 0,
  val charCount: Int = 0,
  val isShared: Boolean = false,
  val inviteCode: String = "",
  val ownerId: String = "",
  val ownerName: String = "",
  val partnerId: String = "",
  val partnerName: String = "",
  val currentUserId: String = "",
  val currentUserName: String = "",
  val isShareDialogOpen: Boolean = false,
  val revision: Int = 1,
  val isLoading: Boolean = false
) {
  val planId: String get() = document.id
  val planTitle: String get() = document.title
  val planDescription: String get() = document.description
  val blocks: List<EditorBlock> get() = document.blocks
}
