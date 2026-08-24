package com.example.data.model

enum class ChangeAction(val displayName: String) {
  INSERT("Inserted text"),
  DELETE("Deleted text"),
  REPLACE("Edited text"),
  FORMAT("Formatting"),
  BLOCK_CREATE("Added block"),
  BLOCK_DELETE("Deleted block"),
  CHECKLIST_UPDATE("Checklist item"),
  RENAME("Renamed plan")
}
