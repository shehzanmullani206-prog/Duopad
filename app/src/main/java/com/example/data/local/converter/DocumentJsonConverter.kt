package com.example.data.local.converter

import com.example.data.model.BlockType
import com.example.data.model.EditorBlock
import com.example.data.model.HeadingLevel
import com.example.data.model.RichSpan
import com.example.data.model.TextAlignment
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object DocumentJsonConverter {

  fun blocksToJson(blocks: List<EditorBlock>): String {
    val jsonArray = JSONArray()
    for (block in blocks) {
      val blockObj = JSONObject()
      blockObj.put("id", block.id)
      blockObj.put("type", block.type.name)
      blockObj.put("text", block.text)
      blockObj.put("headingLevel", block.headingLevel.name)
      blockObj.put("alignment", block.alignment.name)
      blockObj.put("isChecked", block.isChecked)
      blockObj.put("numberIndex", block.numberIndex)
      blockObj.put("lineSpacing", block.lineSpacing.toDouble())
      blockObj.put("letterSpacing", block.letterSpacing.toDouble())
      blockObj.put("paragraphSpacingDp", block.paragraphSpacingDp)

      val spansArray = JSONArray()
      for (span in block.spans) {
        val spanObj = JSONObject()
        spanObj.put("start", span.start)
        spanObj.put("end", span.end)
        spanObj.put("isBold", span.isBold)
        spanObj.put("isItalic", span.isItalic)
        spanObj.put("isUnderline", span.isUnderline)
        spanObj.put("isStrikethrough", span.isStrikethrough)
        if (span.textColorHex != null) spanObj.put("textColorHex", span.textColorHex)
        if (span.highlightColorHex != null) spanObj.put("highlightColorHex", span.highlightColorHex)
        if (span.fontSizeSp != null) spanObj.put("fontSizeSp", span.fontSizeSp)
        spansArray.put(spanObj)
      }
      blockObj.put("spans", spansArray)
      jsonArray.put(blockObj)
    }
    return jsonArray.toString()
  }

  fun jsonToBlocks(json: String?): List<EditorBlock> {
    if (json.isNullOrBlank() || json == "[]") {
      return listOf(EditorBlock(id = UUID.randomUUID().toString(), text = ""))
    }

    val list = mutableListOf<EditorBlock>()
    try {
      val jsonArray = JSONArray(json)
      for (i in 0 until jsonArray.length()) {
        val blockObj = jsonArray.getJSONObject(i)
        val id = blockObj.optString("id", UUID.randomUUID().toString())
        val typeStr = blockObj.optString("type", BlockType.PARAGRAPH.name)
        val type = try { BlockType.valueOf(typeStr) } catch (e: Exception) { BlockType.PARAGRAPH }
        val text = blockObj.optString("text", "")
        val headingStr = blockObj.optString("headingLevel", HeadingLevel.NORMAL.name)
        val headingLevel = try { HeadingLevel.valueOf(headingStr) } catch (e: Exception) { HeadingLevel.NORMAL }
        val alignStr = blockObj.optString("alignment", TextAlignment.LEFT.name)
        val alignment = try { TextAlignment.valueOf(alignStr) } catch (e: Exception) { TextAlignment.LEFT }
        val isChecked = blockObj.optBoolean("isChecked", false)
        val numberIndex = blockObj.optInt("numberIndex", 1)
        val lineSpacing = blockObj.optDouble("lineSpacing", 1.4).toFloat()
        val letterSpacing = blockObj.optDouble("letterSpacing", 0.3).toFloat()
        val paragraphSpacingDp = blockObj.optInt("paragraphSpacingDp", 8)

        val spansList = mutableListOf<RichSpan>()
        val spansArray = blockObj.optJSONArray("spans")
        if (spansArray != null) {
          for (j in 0 until spansArray.length()) {
            val spanObj = spansArray.getJSONObject(j)
            val start = spanObj.optInt("start", 0)
            val end = spanObj.optInt("end", 0)
            val isBold = spanObj.optBoolean("isBold", false)
            val isItalic = spanObj.optBoolean("isItalic", false)
            val isUnderline = spanObj.optBoolean("isUnderline", false)
            val isStrikethrough = spanObj.optBoolean("isStrikethrough", false)
            val textColorHex = if (spanObj.has("textColorHex")) spanObj.getString("textColorHex") else null
            val highlightColorHex = if (spanObj.has("highlightColorHex")) spanObj.getString("highlightColorHex") else null
            val fontSizeSp = if (spanObj.has("fontSizeSp")) spanObj.getInt("fontSizeSp") else null

            spansList.add(
              RichSpan(
                start = start,
                end = end,
                isBold = isBold,
                isItalic = isItalic,
                isUnderline = isUnderline,
                isStrikethrough = isStrikethrough,
                textColorHex = textColorHex,
                highlightColorHex = highlightColorHex,
                fontSizeSp = fontSizeSp
              )
            )
          }
        }

        list.add(
          EditorBlock(
            id = id,
            type = type,
            text = text,
            spans = spansList,
            headingLevel = headingLevel,
            alignment = alignment,
            isChecked = isChecked,
            numberIndex = numberIndex,
            lineSpacing = lineSpacing,
            letterSpacing = letterSpacing,
            paragraphSpacingDp = paragraphSpacingDp
          )
        )
      }
    } catch (e: Exception) {
      return listOf(EditorBlock(id = UUID.randomUUID().toString(), text = ""))
    }

    return if (list.isEmpty()) listOf(EditorBlock(id = UUID.randomUUID().toString(), text = "")) else list
  }
}
