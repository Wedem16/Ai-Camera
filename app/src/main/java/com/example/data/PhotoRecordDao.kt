package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoRecordDao {
    @Query("SELECT * FROM photo_records ORDER BY timestamp DESC")
    fun observeAllRecords(): Flow<List<PhotoRecord>>

    @Query("SELECT * FROM photo_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): PhotoRecord?

    @Query("SELECT COUNT(*) FROM photo_records")
    suspend fun getRecordCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: PhotoRecord): Long

    @Update
    suspend fun updateRecord(record: PhotoRecord)

    @Query("DELETE FROM photo_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)
}
