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
enum class Satisfaction(val code: Int) {
    DISSATISFIED(1), NEUTRAL(2), SATISFIED(3);

    companion object {
        fun fromCode(code: Int?): Satisfaction? = code?.let { value -> entries.first { it.code == value } }
    }
}

data class ShaveRatings(
    val overall: Satisfaction? = null,
    val closeness: Satisfaction? = null,
    val smoothness: Satisfaction? = null,
    val skin: Satisfaction? = null,
)

data class ShaveEvent(
    val id: String,
    val cycleId: String,
    val localDate: LocalDate,
    val occurredAt: Long,
    val timeZone: String,
    val ratings: ShaveRatings = ShaveRatings(),
)
data class ShaveSnapshot(val equipment: Equipment? = null, val cycles: List<Cycle> = emptyList(), val events: List<ShaveEvent> = emptyList())
data class ShaveSummary(val current: Cycle?, val uses: Int, val days: Long, val lastDate: LocalDate?, val nextDate: LocalDate?, val replacementSuggested: Boolean)
