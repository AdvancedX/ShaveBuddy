package com.shavebuddy.demo.domain

import java.time.LocalDate

data class Equipment(val name: String, val intervalDays: Int)
data class Cycle(
    val id: String,
    val installedOn: LocalDate,
    val retiredOn: LocalDate? = null,
    val targetUses: Int? = null,
    val targetDays: Int? = null,
    val createdAt: Long = 0,
)
data class ShaveEvent(val id: String, val cycleId: String, val localDate: LocalDate, val occurredAt: Long, val timeZone: String)
data class ShaveSnapshot(val equipment: Equipment? = null, val cycles: List<Cycle> = emptyList(), val events: List<ShaveEvent> = emptyList())
data class ShaveSummary(val current: Cycle?, val uses: Int, val days: Long, val lastDate: LocalDate?, val nextDate: LocalDate?, val replacementSuggested: Boolean)
