package ro.ciprian.calculatorore.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WorkType {
    ZI,
    NOAPTE,
    LIBER,
    CONCEDIU,
    MEDICAL,
    ALTA
}

@Entity(tableName = "work_entries")
data class WorkEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val date: String,

    val startMinutes: Int = 0,

    val endMinutes: Int = 0,

    val breakMinutes: Int = 0,

    val totalMinutes: Int = 0,

    val overtimeMinutes: Int = 0,

    val type: String = WorkType.ZI.name,

    val notes: String = ""
)
