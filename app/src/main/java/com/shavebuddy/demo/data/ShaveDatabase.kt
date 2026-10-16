package com.shavebuddy.demo.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "equipment")
data class EquipmentEntity(@PrimaryKey val id: Int = 1, val name: String, val intervalDays: Int)

@Entity(tableName = "cycles", indices = [Index(value = ["activeSlot"], unique = true)])
data class CycleEntity(
    @PrimaryKey val id: String,
    val installedOn: String,
    val retiredOn: String? = null,
    val targetUses: Int?,
    val targetDays: Int?,
    val createdAt: Long,
    val activeSlot: Int? = 1,
)

@Entity(
    tableName = "events",
    foreignKeys = [ForeignKey(entity = CycleEntity::class, parentColumns = ["id"], childColumns = ["cycleId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("cycleId"), Index("localDate")],
)
data class EventEntity(
    @PrimaryKey val id: String,
    val cycleId: String,
    val localDate: String,
    val occurredAt: Long,
    val timeZone: String,
    val overallRating: Int? = null,
    val closenessRating: Int? = null,
    val smoothnessRating: Int? = null,
    val skinRating: Int? = null,
)

@Dao
interface ShaveDao {
    @Query("SELECT * FROM equipment WHERE id = 1") suspend fun equipment(): EquipmentEntity?
    @Query("SELECT * FROM cycles ORDER BY createdAt DESC") suspend fun cycles(): List<CycleEntity>
    @Query("SELECT * FROM events ORDER BY localDate DESC, occurredAt DESC") suspend fun events(): List<EventEntity>
    @Insert suspend fun insertEquipment(equipment: EquipmentEntity)
    @Update suspend fun updateEquipment(equipment: EquipmentEntity)
    @Insert suspend fun insertCycle(cycle: CycleEntity)
    @Update suspend fun updateCycle(cycle: CycleEntity)
    @Insert suspend fun insertEvent(event: EventEntity)
    @Query("UPDATE events SET overallRating = :overall, closenessRating = :closeness, smoothnessRating = :smoothness, skinRating = :skin WHERE id = :id")
    suspend fun updateRatings(id: String, overall: Int?, closeness: Int?, smoothness: Int?, skin: Int?): Int
    @Query("DELETE FROM events WHERE id = :id") suspend fun deleteEvent(id: String)
}

@Database(entities = [EquipmentEntity::class, CycleEntity::class, EventEntity::class], version = 2, exportSchema = true)
abstract class ShaveDatabase : RoomDatabase() {
    abstract fun dao(): ShaveDao
    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("overallRating", "closenessRating", "smoothnessRating", "skinRating").forEach { column ->
                    db.execSQL("ALTER TABLE events ADD COLUMN $column INTEGER")
                }
            }
        }

        fun open(context: Context, name: String = "shavebuddy.db"): ShaveDatabase =
            Room.databaseBuilder(context.applicationContext, ShaveDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2).build()
    }
}
