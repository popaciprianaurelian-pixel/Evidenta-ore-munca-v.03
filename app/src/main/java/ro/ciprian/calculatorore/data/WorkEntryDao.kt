package ro.ciprian.calculatorore.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkEntryDao {

    @Query(
        "SELECT * FROM work_entries " +
        "ORDER BY date DESC, startMinutes DESC"
    )
    fun observeAll(): Flow<List<WorkEntry>>

    @Query(
        "SELECT * FROM work_entries " +
        "WHERE date BETWEEN :from AND :to " +
        "ORDER BY date ASC, startMinutes ASC"
    )
    fun observeMonth(
        from: String,
        to: String
    ): Flow<List<WorkEntry>>

    @Insert
    suspend fun insert(entry: WorkEntry): Long

    @Update
    suspend fun update(entry: WorkEntry)

    @Delete
    suspend fun delete(entry: WorkEntry)

    @Query(
        "SELECT * FROM work_entries " +
        "WHERE id = :id LIMIT 1"
    )
    suspend fun getById(id: Long): WorkEntry?
}
