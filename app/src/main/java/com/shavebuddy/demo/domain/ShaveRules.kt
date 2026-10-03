package com.shavebuddy.demo.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object ShaveRules {
    fun summarize(snapshot: ShaveSnapshot, today: LocalDate): ShaveSummary {
        val current = snapshot.cycles.firstOrNull { it.retiredOn == null }
        val uses = snapshot.events.count { it.cycleId == current?.id }
        val days = current?.let { ChronoUnit.DAYS.between(it.installedOn, today).coerceAtLeast(0) } ?: 0
        val last = snapshot.events.maxWithOrNull(compareBy<ShaveEvent> { it.localDate }.thenBy { it.occurredAt })?.localDate
        val next = snapshot.equipment?.intervalDays?.takeIf { it > 0 }?.let { last?.plusDays(it.toLong()) }
        val suggested = current?.let {
            (it.targetUses?.let { target -> uses >= target } ?: false) ||
                (it.targetDays?.let { target -> days >= target } ?: false)
        } ?: false
        return ShaveSummary(current, uses, days, last, next, suggested)
    }

    fun cycleForDate(cycles: List<Cycle>, date: LocalDate): Cycle? = cycles
        .filter { !date.isBefore(it.installedOn) && (it.retiredOn == null || date.isBefore(it.retiredOn)) }
        .maxWithOrNull(compareBy<Cycle> { it.installedOn }.thenBy { it.createdAt })

    fun validSettings(name: String, interval: Int, uses: Int?, days: Int?): Boolean =
        name.trim().isNotEmpty() && name.trim().length <= 60 && interval in 1..365 &&
            (uses == null || uses in 1..10000) && (days == null || days in 1..10000)
}
