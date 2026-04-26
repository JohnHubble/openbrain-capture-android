package com.hubble.openbrain.data.repo

import android.content.Context
import com.hubble.openbrain.data.db.Thought
import com.hubble.openbrain.data.db.ThoughtDao
import com.hubble.openbrain.data.db.ThoughtStatus
import com.hubble.openbrain.work.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThoughtRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ThoughtDao,
) {

    fun observeAll(): Flow<List<Thought>> = dao.observeAll()

    fun observeByStatus(status: ThoughtStatus): Flow<List<Thought>> = dao.observeByStatus(status)

    fun observeCounts(): Flow<Map<ThoughtStatus, Int>> =
        dao.observeCounts().map { rows -> rows.associate { it.status to it.count } }

    suspend fun insertPending(
        text: String,
        createdAt: Long = System.currentTimeMillis(),
        durationMs: Long = 0,
    ): Long {
        val id = dao.insert(
            Thought(text = text, createdAt = createdAt, durationMs = durationMs),
        )
        SyncScheduler.enqueue(context)
        return id
    }

    suspend fun markSent(id: Long, ob1Id: String?) {
        dao.updateStatus(id, ThoughtStatus.SENT, null, ob1Id)
    }

    suspend fun markFailed(id: Long, error: String) {
        dao.updateStatus(id, ThoughtStatus.FAILED, error, null)
    }

    suspend fun pending(): List<Thought> = dao.pending()

    suspend fun clearAll() = dao.clearAll()

    suspend fun clearUnsent() = dao.clearUnsent()
}
