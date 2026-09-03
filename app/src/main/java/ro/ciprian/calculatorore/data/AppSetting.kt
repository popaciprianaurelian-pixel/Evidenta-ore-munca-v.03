package ro.ciprian.calculatorore.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey
    val id: Int = 1,

    val monthlyNormMinutes: Int = 160 * 60,

    val dailyStandardMinutes: Int = 8 * 60
)
