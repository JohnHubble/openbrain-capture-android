package com.hubble.openbrain.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class StatusCount(val status: ThoughtStatus, val count: Int)

@Dao
interface ThoughtDao {

    @Query("SELECT * FROM thoughts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Thought>>

    @Query("SELECT * FROM thoughts WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: ThoughtStatus): Flow<List<Thought>>

    @Query("SELECT status, COUNT(*) AS count FROM thoughts GROUP BY status")
    fun observeCounts(): Flow<List<StatusCount>>

    @Insert
    suspend fun insert(thought: Thought): Long

    @Query("UPDATE thoughts SET status = :status, attempts = attempts + 1, lastError = :error, ob1Id = :ob1Id WHERE id = :id")
    suspend fun updateStatus(id: Long, status: ThoughtStatus, error: String?, ob1Id: String?)

    @Query("SELECT * FROM thoughts WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun pending(): List<Thought>

    @Query("DELETE FROM thoughts")
    suspend fun clearAll()

    @Query("DELETE FROM thoughts WHERE status IN ('PENDING', 'FAILED')")
    suspend fun clearUnsent()
}
