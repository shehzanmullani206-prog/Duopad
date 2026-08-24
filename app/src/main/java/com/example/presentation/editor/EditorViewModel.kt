package com.example.presentation.editor

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DuoPlanApplication
import com.example.data.collaboration.CollaborationManager
import com.example.data.local.converter.DocumentJsonConverter
import com.example.data.model.BlockType
import com.example.data.model.CollaborationOperation
import com.example.data.model.DocumentModel
import com.example.data.model.EditorBlock
import com.example.data.model.FormattingState
import com.example.data.model.HeadingLevel
import com.example.data.model.OperationType
import com.example.data.model.RichSpan
import com.example.data.model.TextAlignment
import com.example.data.repository.PlanRepository
import com.example.data.user.UserManager
import com.example.presentation.editor.engine.RichTextEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class EditorViewModel(
  application: Application,
  private val repository: PlanRepository = (application as DuoPlanApplication).planRepository,
  private val collaborationManager: CollaborationManager = (application as DuoPlanApplication).collaborationManager,
  private val userManager: UserManager = (application as DuoPlanApplication).userManager
) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(
    EditorUiState(
      isLoading = true,
      currentUserId = userManager.userId,
      currentUserName = userManager.displayName
    )
  )
  val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

  private val undoStack = mutableListOf<DocumentModel>()
  private val redoStack = mutableListOf<DocumentModel>()

  private var autoSaveJob: Job? = null
  private var typingJob: Job? = null
  private var currentLoadedPlanId: String? = null

  init {
    // Observe connection & partner status
    viewModelScope.launch {
      collaborationManager.connectionStatus.collect { status ->
        _uiState.update { it.copy(connectionStatus = status) }
      }
    }

    viewModelScope.launch {
      collaborationManager.partnerStatus.collect { status ->
        _uiState.update { it.copy(partnerStatus = status) }
      }
    }

    // Observe incoming remote operations from partner
    viewModelScope.launch {
      collaborationManager.remoteOperations.collect { op ->
        applyRemoteOperation(op)
      }
    }

    // Observe authoritative initial document on join
    viewModelScope.launch {
      collaborationManager.authoritativeDocument.collect { doc ->
        applyAuthoritativeDocument(doc)
      }
    }

    // Observe user profile changes
    viewModelScope.launch {
      userManager.currentUser.collect { user ->
        _uiState.update {
          it.copy(
            currentUserId = user.userId,
            currentUserName = user.displayName
          )
        }
      }
    }

    updateCountsAndHistoryState()
  }

  fun loadPlan(planIdArg: String?, initialTitle: String? = null, initialDesc: String? = null) {
    if (!planIdArg.isNullOrBlank() && planIdArg == currentLoadedPlanId) {
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }

      if (!planIdArg.isNullOrBlank()) {
        val plan = repository.getPlan(planIdArg)
        if (plan != null) {
          currentLoadedPlanId = plan.id
          val blocks = DocumentJsonConverter.jsonToBlocks(plan.blocksJson)
          val doc = DocumentModel(
            id = plan.id,
            title = plan.title,
            description = plan.description,
            blocks = blocks
          )
          undoStack.clear()
          redoStack.clear()
          _uiState.update {
            it.copy(
              document = doc,
              activeBlockId = blocks.firstOrNull()?.id,
              activeSelection = TextRange.Zero,
              saveStatus = SaveStatus.SAVED,
              lastSavedTime = formatSavedTimestamp(plan.updatedAt),
              isShared = plan.isShared,
              inviteCode = plan.inviteCode,
              ownerId = plan.ownerId,
              ownerName = plan.ownerName,
              partnerId = plan.partnerId,
              partnerName = plan.partnerName,
              revision = plan.revision,
              isLoading = false
            )
          }
          updateCountsAndHistoryState()
          collaborationManager.connectToPlan(plan.id, plan.isShared)
          return@launch
        }
      }

      // If no valid plan was found or planId was null, try to load the default plan or create one
      val defaultId = repository.ensureDefaultPlanExists()
      val fallbackPlan = if (defaultId.isNotBlank()) {
        repository.getPlan(defaultId)
      } else {
        null
      }

      if (fallbackPlan != null) {
        currentLoadedPlanId = fallbackPlan.id
        val blocks = DocumentJsonConverter.jsonToBlocks(fallbackPlan.blocksJson)
        val doc = DocumentModel(
          id = fallbackPlan.id,
          title = fallbackPlan.title,
          description = fallbackPlan.description,
          blocks = blocks
        )
        undoStack.clear()
        redoStack.clear()
        _uiState.update {
          it.copy(
            document = doc,
            activeBlockId = blocks.firstOrNull()?.id,
            activeSelection = TextRange.Zero,
            saveStatus = SaveStatus.SAVED,
            lastSavedTime = formatSavedTimestamp(fallbackPlan.updatedAt),
            isShared = fallbackPlan.isShared,
            inviteCode = fallbackPlan.inviteCode,
            ownerId = fallbackPlan.ownerId,
            ownerName = fallbackPlan.ownerName,
            partnerId = fallbackPlan.partnerId,
            partnerName = fallbackPlan.partnerName,
            revision = fallbackPlan.revision,
            isLoading = false
          )
        }
        updateCountsAndHistoryState()
        collaborationManager.connectToPlan(fallbackPlan.id, fallbackPlan.isShared)
      } else {
        // Create new plan if title is provided
        val title = if (!initialTitle.isNullOrBlank()) initialTitle else "Our Plan"
        val desc = initialDesc ?: ""
        val newId = repository.createPlan(title, desc, isShared = true)
        val newPlan = repository.getPlan(newId)
        currentLoadedPlanId = newId
        val doc = DocumentModel(
          id = newId,
          title = title,
          description = desc,
          blocks = listOf(EditorBlock(id = UUID.randomUUID().toString(), text = ""))
        )
        undoStack.clear()
        redoStack.clear()
        _uiState.update {
          it.copy(
            document = doc,
            activeBlockId = doc.blocks.firstOrNull()?.id,
            activeSelection = TextRange.Zero,
            saveStatus = SaveStatus.SAVED,
            lastSavedTime = "Saved just now",
            isShared = newPlan?.isShared ?: true,
            inviteCode = newPlan?.inviteCode ?: "",
            ownerId = newPlan?.ownerId ?: userManager.userId,
            ownerName = newPlan?.ownerName ?: userManager.displayName,
            revision = newPlan?.revision ?: 1,
            isLoading = false
          )
        }
        updateCountsAndHistoryState()
        collaborationManager.connectToPlan(newId, true)
      }
    }
  }

  fun openShareDialog() {
    _uiState.update { it.copy(isShareDialogOpen = true) }
  }

  fun closeShareDialog() {
    _uiState.update { it.copy(isShareDialogOpen = false) }
  }

  private fun applyRemoteOperation(op: CollaborationOperation) {
    val currentDoc = _uiState.value.document
    if (op.planId != currentDoc.id) return

    val blocks = currentDoc.blocks.toMutableList()
    var updatedTitle = currentDoc.title
    val payloadJson = try { JSONObject(op.payload) } catch (e: Exception) { JSONObject() }

    when (op.type) {
      OperationType.BLOCK_UPDATE -> {
        val blockId = payloadJson.optString("blockId")
        val text = payloadJson.optString("text")
        val index = blocks.indexOfFirst { it.id == blockId }
        if (index != -1) {
          val block = blocks[index]
          val spansJson = payloadJson.optJSONArray("spans")
          val spans = if (spansJson != null) {
            val dummyJson = JSONObject().put("id", blockId).put("text", text).put("spans", spansJson)
            DocumentJsonConverter.jsonToBlocks("[$dummyJson]").firstOrNull()?.spans ?: block.spans
          } else {
            block.spans
          }
          blocks[index] = block.copy(text = text, spans = spans)
        }
      }

      OperationType.BLOCK_INSERT -> {
        val blockObj = payloadJson.optJSONObject("block")
        val targetIndex = payloadJson.optInt("targetIndex", blocks.size).coerceIn(0, blocks.size)
        if (blockObj != null) {
          val inserted = DocumentJsonConverter.jsonToBlocks("[$blockObj]").firstOrNull()
          if (inserted != null) {
            blocks.add(targetIndex, inserted)
          }
        }
      }

      OperationType.BLOCK_DELETE -> {
        val blockId = payloadJson.optString("blockId")
        blocks.removeAll { it.id == blockId }
        if (blocks.isEmpty()) {
          blocks.add(EditorBlock(id = UUID.randomUUID().toString(), text = ""))
        }
      }

      OperationType.BLOCK_FORMAT -> {
        val blockId = payloadJson.optString("blockId")
        val blockObj = payloadJson.optJSONObject("block")
        val index = blocks.indexOfFirst { it.id == blockId }
        if (index != -1 && blockObj != null) {
          val formatted = DocumentJsonConverter.jsonToBlocks("[$blockObj]").firstOrNull()
          if (formatted != null) {
            blocks[index] = formatted
          }
        }
      }

      OperationType.RENAME -> {
        updatedTitle = payloadJson.optString("title", updatedTitle)
      }

      OperationType.FULL_SYNC -> {
        val blocksArr = payloadJson.optJSONArray("blocks")
        if (blocksArr != null) {
          val syncedBlocks = DocumentJsonConverter.jsonToBlocks(blocksArr.toString())
          blocks.clear()
          blocks.addAll(syncedBlocks)
        }
      }
    }

    val updatedDoc = currentDoc.copy(
      title = updatedTitle,
      blocks = renumberListBlocks(blocks)
    )

    _uiState.update {
      it.copy(
        document = updatedDoc,
        revision = op.revision,
        saveStatus = SaveStatus.SAVED,
        lastSavedTime = "Synced from ${op.userName.ifBlank { "partner" }}"
      )
    }
    updateCountsAndHistoryState()

    // Persist changes to Room DB
    viewModelScope.launch {
      val (words, chars) = RichTextEngine.countWordsAndChars(updatedDoc.blocks)
      repository.savePlanContent(updatedDoc.id, updatedDoc.blocks, words, chars)
      if (updatedTitle != currentDoc.title) {
        repository.renamePlan(updatedDoc.id, updatedTitle)
      }
    }
  }

  private fun applyAuthoritativeDocument(docResponse: com.example.data.model.SharedPlanResponse) {
    if (docResponse.planId != _uiState.value.planId) return

    val updatedDoc = _uiState.value.document.copy(
      title = docResponse.title,
      description = docResponse.description,
      blocks = if (docResponse.blocks.isNotEmpty()) docResponse.blocks else _uiState.value.blocks
    )

    _uiState.update {
      it.copy(
        document = updatedDoc,
        isShared = true,
        inviteCode = docResponse.inviteCode,
        ownerId = docResponse.ownerId,
        ownerName = docResponse.ownerName,
        partnerId = docResponse.partnerId ?: "",
        partnerName = docResponse.partnerName ?: "",
        revision = docResponse.revision,
        saveStatus = SaveStatus.SAVED,
        lastSavedTime = "Synced with cloud"
      )
    }
    updateCountsAndHistoryState()

    viewModelScope.launch {
      val (words, chars) = RichTextEngine.countWordsAndChars(updatedDoc.blocks)
      repository.savePlanContent(updatedDoc.id, updatedDoc.blocks, words, chars)
    }
  }

  fun renamePlan(newTitle: String) {
    if (newTitle.isBlank()) return
    val currentDoc = _uiState.value.document
    val updatedDoc = currentDoc.copy(title = newTitle.trim())
    _uiState.update {
      it.copy(document = updatedDoc, saveStatus = SaveStatus.SAVING)
    }

    emitOperation(
      type = OperationType.RENAME,
      payload = JSONObject().put("title", newTitle.trim()).toString()
    )

    viewModelScope.launch {
      repository.renamePlan(currentDoc.id, newTitle.trim())
      _uiState.update {
        it.copy(
          saveStatus = SaveStatus.SAVED,
          lastSavedTime = "Saved just now"
        )
      }
    }
  }

  fun deleteCurrentPlan(onDeleted: () -> Unit) {
    val planId = _uiState.value.planId
    collaborationManager.disconnect()
    viewModelScope.launch {
      repository.deletePlan(planId)
      onDeleted()
    }
  }

  private fun scheduleAutoSave() {
    _uiState.update { it.copy(saveStatus = SaveStatus.UNSAVED) }
    autoSaveJob?.cancel()
    autoSaveJob = viewModelScope.launch {
      delay(500)
      performSave()
    }
  }

  private suspend fun performSave() {
    val state = _uiState.value
    _uiState.update { it.copy(saveStatus = SaveStatus.SAVING) }
    try {
      repository.savePlanContent(
        id = state.planId,
        blocks = state.blocks,
        wordCount = state.wordCount,
        charCount = state.charCount
      )
      _uiState.update {
        it.copy(
          saveStatus = SaveStatus.SAVED,
          lastSavedTime = "Saved just now"
        )
      }
    } catch (e: Exception) {
      _uiState.update { it.copy(saveStatus = SaveStatus.ERROR) }
    }
  }

  private fun emitOperation(type: OperationType, payload: String) {
    val state = _uiState.value
    if (!state.isShared) return

    val newRev = state.revision + 1
    _uiState.update { it.copy(revision = newRev) }

    val op = CollaborationOperation(
      operationId = UUID.randomUUID().toString(),
      planId = state.planId,
      userId = userManager.userId,
      userName = userManager.displayName,
      type = type,
      revision = newRev,
      payload = payload,
      timestamp = System.currentTimeMillis()
    )

    collaborationManager.sendOperation(op)
  }

  private fun notifyTyping() {
    typingJob?.cancel()
    collaborationManager.sendTyping(true)
    typingJob = viewModelScope.launch {
      delay(2000)
      collaborationManager.sendTyping(false)
    }
  }

  private fun saveSnapshot() {
    if (undoStack.size > 50) {
      undoStack.removeAt(0)
    }
    undoStack.add(_uiState.value.document)
    redoStack.clear()
    updateCountsAndHistoryState()
  }

  private fun updateCountsAndHistoryState() {
    val (words, chars) = RichTextEngine.countWordsAndChars(_uiState.value.blocks)
    _uiState.update {
      it.copy(
        wordCount = words,
        charCount = chars,
        formattingState = it.formattingState.copy(
          canUndo = undoStack.isNotEmpty(),
          canRedo = redoStack.isNotEmpty()
        )
      )
    }
  }

  fun onBlockFocusChanged(blockId: String, selection: TextRange = TextRange.Zero) {
    val block = _uiState.value.blocks.find { it.id == blockId }
    _uiState.update {
      it.copy(
        activeBlockId = blockId,
        activeSelection = selection,
        formattingState = computeToolbarState(block, selection, it.formattingState)
      )
    }
  }

  fun onBlockSelectionChanged(blockId: String, selection: TextRange) {
    val block = _uiState.value.blocks.find { it.id == blockId }
    _uiState.update {
      it.copy(
        activeBlockId = blockId,
        activeSelection = selection,
        formattingState = computeToolbarState(block, selection, it.formattingState)
      )
    }
  }

  private fun computeToolbarState(
    block: EditorBlock?,
    selection: TextRange,
    currentState: FormattingState
  ): FormattingState {
    if (block == null) return currentState

    var isBold = false
    var isItalic = false
    var isUnderline = false
    var isStrikethrough = false
    var textColor = "#F1F3F7"
    var highlightColor: String? = null
    var fontSize = when (block.headingLevel) {
      HeadingLevel.H1 -> 26
      HeadingLevel.H2 -> 22
      HeadingLevel.H3 -> 19
      HeadingLevel.H4 -> 17
      HeadingLevel.H5 -> 15
      HeadingLevel.NORMAL -> 16
    }

    if (selection.start < selection.end) {
      block.spans.forEach { span ->
        if (span.end > selection.start && span.start < selection.end) {
          if (span.isBold) isBold = true
          if (span.isItalic) isItalic = true
          if (span.isUnderline) isUnderline = true
          if (span.isStrikethrough) isStrikethrough = true
          if (span.textColorHex != null) textColor = span.textColorHex
          if (span.highlightColorHex != null) highlightColor = span.highlightColorHex
          if (span.fontSizeSp != null) fontSize = span.fontSizeSp
        }
      }
    } else {
      val cursor = selection.start
      block.spans.forEach { span ->
        if (cursor in span.start..span.end) {
          if (span.isBold) isBold = true
          if (span.isItalic) isItalic = true
          if (span.isUnderline) isUnderline = true
          if (span.isStrikethrough) isStrikethrough = true
          if (span.textColorHex != null) textColor = span.textColorHex
          if (span.highlightColorHex != null) highlightColor = span.highlightColorHex
          if (span.fontSizeSp != null) fontSize = span.fontSizeSp
        }
      }
    }

    return currentState.copy(
      isBold = isBold,
      isItalic = isItalic,
      isUnderline = isUnderline,
      isStrikethrough = isStrikethrough,
      headingLevel = block.headingLevel,
      blockType = block.type,
      alignment = block.alignment,
      textColorHex = textColor,
      highlightColorHex = highlightColor,
      fontSizeSp = fontSize,
      letterSpacing = block.letterSpacing,
      lineSpacing = block.lineSpacing,
      paragraphSpacingDp = block.paragraphSpacingDp,
      canUndo = undoStack.isNotEmpty(),
      canRedo = redoStack.isNotEmpty()
    )
  }

  fun onBlockTextChanged(blockId: String, newText: String) {
    val blocks = _uiState.value.blocks.toMutableList()
    val index = blocks.indexOfFirst { it.id == blockId }
    if (index == -1) return

    val oldBlock = blocks[index]
    if (oldBlock.text == newText) return

    saveSnapshot()
    notifyTyping()

    val lengthDelta = newText.length - oldBlock.text.length
    val updatedSpans = if (lengthDelta != 0 && newText.isNotEmpty()) {
      oldBlock.spans.mapNotNull { span ->
        val safeEnd = (span.end + lengthDelta).coerceIn(span.start, newText.length)
        if (span.start < newText.length) {
          span.copy(end = safeEnd)
        } else null
      }
    } else if (newText.isEmpty()) {
      emptyList()
    } else {
      oldBlock.spans
    }

    val updatedBlock = oldBlock.copy(text = newText, spans = updatedSpans)
    blocks[index] = updatedBlock

    _uiState.update {
      it.copy(document = it.document.copy(blocks = renumberListBlocks(blocks)))
    }
    updateCountsAndHistoryState()
    scheduleAutoSave()

    // Emit operation to partner
    val payload = JSONObject().apply {
      put("blockId", blockId)
      put("text", newText)
      put("spans", DocumentJsonConverter.blocksToJson(listOf(updatedBlock)))
    }.toString()
    emitOperation(OperationType.BLOCK_UPDATE, payload)
  }

  fun onEnterPressed(blockId: String, cursorPosition: Int) {
    saveSnapshot()
    val blocks = _uiState.value.blocks.toMutableList()
    val index = blocks.indexOfFirst { it.id == blockId }
    if (index == -1) return

    val currentBlock = blocks[index]

    if (currentBlock.text.isEmpty() && (currentBlock.type == BlockType.BULLET_LIST ||
          currentBlock.type == BlockType.NUMBERED_LIST ||
          currentBlock.type == BlockType.CHECKLIST ||
          currentBlock.type == BlockType.QUOTE)
    ) {
      val converted = currentBlock.copy(
        type = BlockType.PARAGRAPH,
        headingLevel = HeadingLevel.NORMAL
      )
      blocks[index] = converted
      _uiState.update {
        it.copy(document = it.document.copy(blocks = renumberListBlocks(blocks)))
      }
      scheduleAutoSave()

      val payload = JSONObject().apply {
        put("blockId", blockId)
        put("block", JSONObject(DocumentJsonConverter.blocksToJson(listOf(converted)).removeSurrounding("[", "]")))
      }.toString()
      emitOperation(OperationType.BLOCK_FORMAT, payload)
      return
    }

    val beforeText = currentBlock.text.substring(0, cursorPosition.coerceIn(0, currentBlock.text.length))
    val afterText = currentBlock.text.substring(cursorPosition.coerceIn(0, currentBlock.text.length))

    val beforeSpans = currentBlock.spans.filter { it.start < cursorPosition }.map {
      it.copy(end = minOf(it.end, cursorPosition))
    }
    val afterSpans = currentBlock.spans.filter { it.end > cursorPosition }.map {
      it.copy(
        start = maxOf(0, it.start - cursorPosition),
        end = it.end - cursorPosition
      )
    }

    val nextType = when (currentBlock.type) {
      BlockType.HEADING -> BlockType.PARAGRAPH
      BlockType.DIVIDER -> BlockType.PARAGRAPH
      else -> currentBlock.type
    }
    val nextHeading = if (nextType == BlockType.PARAGRAPH) HeadingLevel.NORMAL else currentBlock.headingLevel

    val updatedCurrentBlock = currentBlock.copy(text = beforeText, spans = beforeSpans)
    val newBlock = EditorBlock(
      id = UUID.randomUUID().toString(),
      type = nextType,
      text = afterText,
      spans = afterSpans,
      headingLevel = nextHeading,
      alignment = currentBlock.alignment,
      lineSpacing = currentBlock.lineSpacing,
      letterSpacing = currentBlock.letterSpacing,
      paragraphSpacingDp = currentBlock.paragraphSpacingDp,
      isChecked = false
    )

    blocks[index] = updatedCurrentBlock
    blocks.add(index + 1, newBlock)

    _uiState.update {
      it.copy(
        document = it.document.copy(blocks = renumberListBlocks(blocks)),
        activeBlockId = newBlock.id,
        activeSelection = TextRange.Zero
      )
    }
    updateCountsAndHistoryState()
    scheduleAutoSave()

    // Emit operations: update current block + insert new block
    val updatePayload = JSONObject().apply {
      put("blockId", currentBlock.id)
      put("text", beforeText)
    }.toString()
    emitOperation(OperationType.BLOCK_UPDATE, updatePayload)

    val insertPayload = JSONObject().apply {
      put("targetIndex", index + 1)
      put("block", JSONObject(DocumentJsonConverter.blocksToJson(listOf(newBlock)).removeSurrounding("[", "]")))
    }.toString()
    emitOperation(OperationType.BLOCK_INSERT, insertPayload)
  }

  fun onBackspaceAtStart(blockId: String) {
    val blocks = _uiState.value.blocks.toMutableList()
    val index = blocks.indexOfFirst { it.id == blockId }
    if (index <= 0) return

    saveSnapshot()
    val currentBlock = blocks[index]
    val previousBlock = blocks[index - 1]

    if (previousBlock.type == BlockType.DIVIDER) {
      val removedId = previousBlock.id
      blocks.removeAt(index - 1)
      _uiState.update {
        it.copy(document = it.document.copy(blocks = renumberListBlocks(blocks)))
      }
      scheduleAutoSave()
      emitOperation(OperationType.BLOCK_DELETE, JSONObject().put("blockId", removedId).toString())
      return
    }

    val prevLength = previousBlock.text.length
    val combinedText = previousBlock.text + currentBlock.text
    val shiftedSpans = currentBlock.spans.map {
      it.copy(start = it.start + prevLength, end = it.end + prevLength)
    }
    val combinedSpans = previousBlock.spans + shiftedSpans

    val mergedBlock = previousBlock.copy(text = combinedText, spans = combinedSpans)
    blocks[index - 1] = mergedBlock
    blocks.removeAt(index)

    _uiState.update {
      it.copy(
        document = it.document.copy(blocks = renumberListBlocks(blocks)),
        activeBlockId = mergedBlock.id,
        activeSelection = TextRange(prevLength)
      )
    }
    updateCountsAndHistoryState()
    scheduleAutoSave()

    // Emit merge operations
    val updatePayload = JSONObject().apply {
      put("blockId", mergedBlock.id)
      put("text", combinedText)
    }.toString()
    emitOperation(OperationType.BLOCK_UPDATE, updatePayload)
    emitOperation(OperationType.BLOCK_DELETE, JSONObject().put("blockId", currentBlock.id).toString())
  }

  fun toggleChecklist(blockId: String) {
    saveSnapshot()
    val blocks = _uiState.value.blocks.toMutableList()
    val index = blocks.indexOfFirst { it.id == blockId }
    if (index != -1) {
      val item = blocks[index]
      val updated = item.copy(isChecked = !item.isChecked)
      blocks[index] = updated
      _uiState.update {
        it.copy(document = it.document.copy(blocks = blocks))
      }
      scheduleAutoSave()

      val payload = JSONObject().apply {
        put("blockId", blockId)
        put("block", JSONObject(DocumentJsonConverter.blocksToJson(listOf(updated)).removeSurrounding("[", "]")))
      }.toString()
      emitOperation(OperationType.BLOCK_FORMAT, payload)
    }
  }

  private fun renumberListBlocks(blocks: List<EditorBlock>): List<EditorBlock> {
    var currentNum = 1
    return blocks.map { block ->
      if (block.type == BlockType.NUMBERED_LIST) {
        val numbered = block.copy(numberIndex = currentNum)
        currentNum++
        numbered
      } else {
        currentNum = 1
        block
      }
    }
  }

  // --- Formatting Actions ---

  fun toggleBold() {
    applySpanToggle { span -> span.copy(isBold = !span.isBold) }
  }

  fun toggleItalic() {
    applySpanToggle { span -> span.copy(isItalic = !span.isItalic) }
  }

  fun toggleUnderline() {
    applySpanToggle { span -> span.copy(isUnderline = !span.isUnderline) }
  }

  fun toggleStrikethrough() {
    applySpanToggle { span -> span.copy(isStrikethrough = !span.isStrikethrough) }
  }

  fun setTextColor(colorHex: String) {
    applySpanToggle { span -> span.copy(textColorHex = colorHex) }
  }

  fun toggleHighlight(colorHex: String = "#FDE047") {
    applySpanToggle { span ->
      val newHighlight = if (span.highlightColorHex == colorHex) null else colorHex
      span.copy(highlightColorHex = newHighlight)
    }
  }

  fun setFontSize(sizeSp: Int) {
    applySpanToggle { span -> span.copy(fontSizeSp = sizeSp) }
  }

  private fun applySpanToggle(transform: (RichSpan) -> RichSpan) {
    val activeId = _uiState.value.activeBlockId ?: _uiState.value.blocks.firstOrNull()?.id ?: return
    val selection = _uiState.value.activeSelection

    saveSnapshot()
    val blocks = _uiState.value.blocks.toMutableList()
    val index = blocks.indexOfFirst { it.id == activeId }
    if (index == -1) return

    val currentBlock = blocks[index]
    val start = if (selection.start < selection.end) selection.start else 0
    val end = if (selection.start < selection.end) selection.end else currentBlock.text.length

    if (currentBlock.text.isNotEmpty()) {
      val updatedBlock = RichTextEngine.applySpanToRange(currentBlock, start, end, transform)
      blocks[index] = updatedBlock
      _uiState.update {
        it.copy(
          document = it.document.copy(blocks = blocks),
          formattingState = computeToolbarState(updatedBlock, selection, it.formattingState)
        )
      }
      scheduleAutoSave()

      val payload = JSONObject().apply {
        put("blockId", activeId)
        put("block", JSONObject(DocumentJsonConverter.blocksToJson(listOf(updatedBlock)).removeSurrounding("[", "]")))
      }.toString()
      emitOperation(OperationType.BLOCK_FORMAT, payload)
    }
  }

  // --- Block Formatting Actions ---

  fun setHeadingLevel(level: HeadingLevel) {
    val activeId = _uiState.value.activeBlockId ?: _uiState.value.blocks.firstOrNull()?.id ?: return
    saveSnapshot()
    val blocks = _uiState.value.blocks.toMutableList()
    val index = blocks.indexOfFirst { it.id == activeId }
    if (index == -1) return

    val current = blocks[index]
    val newLevel = if (current.headingLevel == level) HeadingLevel.NORMAL else level
    val newType = if (newLevel == HeadingLevel.NORMAL) BlockType.PARAGRAPH else BlockType.HEADING

    val updated = current.copy(
      headingLevel = newLevel,
      type = newType
    )
    blocks[index] = updated

    _uiState.update {
      it.copy(
        document = it.document.copy(blocks = blocks),
        formattingState = computeToolbarState(blocks[index], it.activeSelection, it.formattingState)
      )
    }
    scheduleAutoSave()

    val payload = JSONObject().apply {
      put("blockId", activeId)
      put("block", JSONObject(DocumentJsonConverter.blocksToJson(listOf(updated)).removeSurrounding("[", "]")))
    }.toString()
    emitOperation(OperationType.BLOCK_FORMAT, payload)
  }

  fun setBlockType(type: BlockType) {
    val activeId = _uiState.value.activeBlockId ?: _uiState.value.blocks.firstOrNull()?.id ?: return
    saveSnapshot()
    val blocks = _uiState.value.blocks.toMutableList()
    val index = blocks.indexOfFirst { it.id == activeId }
    if (index == -1) return

    val current = blocks[index]
    val newType = if (current.type == type) BlockType.PARAGRAPH else type
    val newHeading = if (newType == BlockType.HEADING) HeadingLevel.H1 else HeadingLevel.NORMAL

    val updated = current.copy(
      type = newType,
      headingLevel = newHeading,
      isChecked = false
    )
    blocks[index] = updated

    _uiState.update {
      it.copy(
        document = it.document.copy(blocks = renumberListBlocks(blocks)),
        formattingState = computeToolbarState(blocks[index], it.activeSelection, it.formattingState)
      )
    }
    scheduleAutoSave()

    val payload = JSONObject().apply {
      put("blockId", activeId)
      put("block", JSONObject(DocumentJsonConverter.blocksToJson(listOf(updated)).removeSurrounding("[", "]")))
    }.toString()
    emitOperation(OperationType.BLOCK_FORMAT, payload)
  }

  fun insertDivider() {
    val activeId = _uiState.value.activeBlockId ?: _uiState.value.blocks.lastOrNull()?.id ?: return
    saveSnapshot()
    val blocks = _uiState.value.blocks.toMutableList()
    val index = blocks.indexOfFirst { it.id == activeId }
    val insertAt = if (index != -1) index + 1 else blocks.size

    val divider = EditorBlock(
      id = UUID.randomUUID().toString(),
      type = BlockType.DIVIDER,
      text = ""
    )
    val nextParagraph = EditorBlock(
      id = UUID.randomUUID().toString(),
      type = BlockType.PARAGRAPH,
      text = ""
    )

    blocks.add(insertAt, divider)
    blocks.add(insertAt + 1, nextParagraph)

    _uiState.update {
      it.copy(
        document = it.document.copy(blocks = renumberListBlocks(blocks)),
        activeBlockId = nextParagraph.id,
        activeSelection = TextRange.Zero
      )
    }
    scheduleAutoSave()

    val dividerPayload = JSONObject().apply {
      put("targetIndex", insertAt)
      put("block", JSONObject(DocumentJsonConverter.blocksToJson(listOf(divider)).removeSurrounding("[", "]")))
    }.toString()
    emitOperation(OperationType.BLOCK_INSERT, dividerPayload)

    val paragraphPayload = JSONObject().apply {
      put("targetIndex", insertAt + 1)
      put("block", JSONObject(DocumentJsonConverter.blocksToJson(listOf(nextParagraph)).removeSurrounding("[", "]")))
    }.toString()
    emitOperation(OperationType.BLOCK_INSERT, paragraphPayload)
  }

  fun setAlignment(alignment: TextAlignment) {
    val activeId = _uiState.value.activeBlockId ?: _uiState.value.blocks.firstOrNull()?.id ?: return
    saveSnapshot()
    val blocks = _uiState.value.blocks.toMutableList()
    val index = blocks.indexOfFirst { it.id == activeId }
    if (index == -1) return

    val updated = blocks[index].copy(alignment = alignment)
    blocks[index] = updated
    _uiState.update {
      it.copy(
        document = it.document.copy(blocks = blocks),
        formattingState = it.formattingState.copy(alignment = alignment)
      )
    }
    scheduleAutoSave()

    val payload = JSONObject().apply {
      put("blockId", activeId)
      put("block", JSONObject(DocumentJsonConverter.blocksToJson(listOf(updated)).removeSurrounding("[", "]")))
    }.toString()
    emitOperation(OperationType.BLOCK_FORMAT, payload)
  }

  fun adjustLetterSpacing(delta: Float) {
    val activeId = _uiState.value.activeBlockId ?: _uiState.value.blocks.firstOrNull()?.id ?: return
    saveSnapshot()
    val blocks = _uiState.value.blocks.toMutableList()
    val index = blocks.indexOfFirst { it.id == activeId }
    if (index == -1) return

    val newSpacing = (blocks[index].letterSpacing + delta).coerceIn(0.0f, 2.0f)
    val updated = blocks[index].copy(letterSpacing = newSpacing)
    blocks[index] = updated
    _uiState.update {
      it.copy(
        document = it.document.copy(blocks = blocks),
        formattingState = it.formattingState.copy(letterSpacing = newSpacing)
      )
    }
    scheduleAutoSave()

    val payload = JSONObject().apply {
      put("blockId", activeId)
      put("block", JSONObject(DocumentJsonConverter.blocksToJson(listOf(updated)).removeSurrounding("[", "]")))
    }.toString()
    emitOperation(OperationType.BLOCK_FORMAT, payload)
  }

  fun setLineSpacing(multiplier: Float) {
    val activeId = _uiState.value.activeBlockId ?: _uiState.value.blocks.firstOrNull()?.id ?: return
    saveSnapshot()
    val blocks = _uiState.value.blocks.toMutableList()
    val index = blocks.indexOfFirst { it.id == activeId }
    if (index == -1) return

    val newSpacing = multiplier.coerceIn(1.0f, 2.5f)
    val updated = blocks[index].copy(lineSpacing = newSpacing)
    blocks[index] = updated
    _uiState.update {
      it.copy(
        document = it.document.copy(blocks = blocks),
        formattingState = it.formattingState.copy(lineSpacing = newSpacing)
      )
    }
    scheduleAutoSave()

    val payload = JSONObject().apply {
      put("blockId", activeId)
      put("block", JSONObject(DocumentJsonConverter.blocksToJson(listOf(updated)).removeSurrounding("[", "]")))
    }.toString()
    emitOperation(OperationType.BLOCK_FORMAT, payload)
  }

  fun setParagraphSpacing(spacingDp: Int) {
    val activeId = _uiState.value.activeBlockId ?: _uiState.value.blocks.firstOrNull()?.id ?: return
    saveSnapshot()
    val blocks = _uiState.value.blocks.toMutableList()
    val index = blocks.indexOfFirst { it.id == activeId }
    if (index == -1) return

    val newSpacing = spacingDp.coerceIn(2, 32)
    val updated = blocks[index].copy(paragraphSpacingDp = newSpacing)
    blocks[index] = updated
    _uiState.update {
      it.copy(
        document = it.document.copy(blocks = blocks),
        formattingState = it.formattingState.copy(paragraphSpacingDp = newSpacing)
      )
    }
    scheduleAutoSave()

    val payload = JSONObject().apply {
      put("blockId", activeId)
      put("block", JSONObject(DocumentJsonConverter.blocksToJson(listOf(updated)).removeSurrounding("[", "]")))
    }.toString()
    emitOperation(OperationType.BLOCK_FORMAT, payload)
  }

  fun toggleToolbarExpanded() {
    _uiState.update { it.copy(isToolbarExpanded = !it.isToolbarExpanded) }
  }

  fun toggleToolbarVisible() {
    _uiState.update { it.copy(isToolbarVisible = !it.isToolbarVisible) }
  }

  fun undo() {
    if (undoStack.isNotEmpty()) {
      val currentDoc = _uiState.value.document
      val previousDoc = undoStack.removeAt(undoStack.size - 1)
      redoStack.add(currentDoc)

      _uiState.update {
        it.copy(document = previousDoc)
      }
      updateCountsAndHistoryState()
      scheduleAutoSave()

      val payload = JSONObject().apply {
        put("blocks", DocumentJsonConverter.blocksToJson(previousDoc.blocks))
      }.toString()
      emitOperation(OperationType.FULL_SYNC, payload)
    }
  }

  fun redo() {
    if (redoStack.isNotEmpty()) {
      val currentDoc = _uiState.value.document
      val nextDoc = redoStack.removeAt(redoStack.size - 1)
      undoStack.add(currentDoc)

      _uiState.update {
        it.copy(document = nextDoc)
      }
      updateCountsAndHistoryState()
      scheduleAutoSave()

      val payload = JSONObject().apply {
        put("blocks", DocumentJsonConverter.blocksToJson(nextDoc.blocks))
      }.toString()
      emitOperation(OperationType.FULL_SYNC, payload)
    }
  }

  override fun onCleared() {
    super.onCleared()
    collaborationManager.disconnect()
    if (_uiState.value.saveStatus == SaveStatus.UNSAVED || _uiState.value.saveStatus == SaveStatus.SAVING) {
      val state = _uiState.value
      viewModelScope.launch {
        repository.savePlanContent(
          id = state.planId,
          blocks = state.blocks,
          wordCount = state.wordCount,
          charCount = state.charCount
        )
      }
    }
  }

  private fun formatSavedTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 60_000) return "Saved just now"
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return "Saved at ${sdf.format(Date(timestamp))}"
  }
}
