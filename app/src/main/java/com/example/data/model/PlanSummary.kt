package com.example.data.model

data class PlanSummary(
  val id: String,
  val title: String,
  val description: String = "",
  val lastModified: String = "Just now",
  val isCurrent: Boolean = false
)
