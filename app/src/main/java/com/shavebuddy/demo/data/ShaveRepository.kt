package com.shavebuddy.demo.data

import androidx.room.withTransaction
import com.shavebuddy.demo.domain.*
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShaveRepository(private val database: ShaveDatabase, private val fixedClock: Clock? = null) {
    private val clock: Clock get() = fixedClock ?: Clock.systemDefaultZone()
    private val dao = database.dao()

    val snapshots: Flow<ShaveSnapshot> = database.invalidationTracker
        .createFlow("equipment", "cycles", "events", emitInitialState = true).map { read() }

    suspend fun read(): ShaveSnapshot = database.withTransaction {
        ShaveSnapshot(
            dao.equipment()?.let { Equipment(it.name, it.intervalDays) },
            dao.cycles().map { Cycle(it.id, LocalDate.parse(it.installedOn), it.retiredOn?.let(LocalDate::parse), it.targetUses, it.targetDays, it.createdAt) },
            dao.events().map { ShaveEvent(it.id, it.cycleId, LocalDate.parse(it.localDate), it.occurredAt, it.timeZone) },
        )
    }

    suspend fun setup(name: String, installedOn: LocalDate, uses: Int?, days: Int?, interval: Int) = database.withTransaction {
        require(ShaveRules.validSettings(name, interval, uses, days))
        require(!installedOn.isAfter(LocalDate.now(clock)))
        require(dao.equipment() == null)
        dao.insertEquipment(EquipmentEntity(name = name.trim(), intervalDays = interval))
        dao.insertCycle(CycleEntity(UUID.randomUUID().toString(), installedOn.toString(), targetUses = uses, targetDays = days, createdAt = clock.millis()))
    }

    suspend fun addEvent(date: LocalDate, adjustFirstInstallation: Boolean = false) = database.withTransaction {
        require(!date.isAfter(LocalDate.now(clock)))
        if (adjustFirstInstallation) {
            val first = requireNotNull(dao.cycles().minWithOrNull(compareBy<CycleEntity> { it.installedOn }.thenBy { it.createdAt }))
            if (date.isBefore(LocalDate.parse(first.installedOn))) {
                dao.updateCycle(first.copy(installedOn = date.toString()))
            }
        }
        val cycle = requireNotNull(ShaveRules.cycleForDate(read().cycles, date))
        val instant = date.atTime(LocalTime.now(clock)).atZone(clock.zone).toInstant()
        dao.insertEvent(EventEntity(UUID.randomUUID().toString(), cycle.id, date.toString(), instant.toEpochMilli(), clock.zone.id))
    }

    suspend fun deleteEvent(id: String) = database.withTransaction { dao.deleteEvent(id) }

    suspend fun replaceBlade() = database.withTransaction {
        val current = requireNotNull(dao.cycles().firstOrNull { it.activeSlot == 1 })
        val today = LocalDate.now(clock).toString()
        dao.updateCycle(current.copy(retiredOn = today, activeSlot = null))
        dao.insertCycle(CycleEntity(UUID.randomUUID().toString(), today, targetUses = current.targetUses, targetDays = current.targetDays, createdAt = clock.millis()))
    }

    suspend fun updateSettings(name: String, uses: Int?, days: Int?, interval: Int) = database.withTransaction {
        require(ShaveRules.validSettings(name, interval, uses, days))
        val equipment = requireNotNull(dao.equipment())
        val current = requireNotNull(dao.cycles().firstOrNull { it.activeSlot == 1 })
        dao.updateEquipment(equipment.copy(name = name.trim(), intervalDays = interval))
        dao.updateCycle(current.copy(targetUses = uses, targetDays = days))
    }
}
