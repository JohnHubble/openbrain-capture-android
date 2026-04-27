package com.hubble.openbrain.data.repo

import com.hubble.openbrain.data.db.StatusCount
import com.hubble.openbrain.data.db.Thought
import com.hubble.openbrain.data.db.ThoughtDao
import com.hubble.openbrain.data.db.ThoughtStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito
import java.util.concurrent.atomic.AtomicLong

/**
 * Repository pass-through tests. The DAO is faked in-process so we exercise the repository's
 * status-transition contract without bringing up Room/SQLite. `insertPending` is excluded
 * because it triggers WorkManager via SyncScheduler and is not unit-testable on JVM.
 */
class ThoughtRepositoryTest {

    private class FakeDao : ThoughtDao {
        val rows = mutableMapOf<Long, Thought>()
        private val nextId = AtomicLong(1)
        data class StatusUpdate(val id: Long, val status: ThoughtStatus, val error: String?, val ob1Id: String?)
        val updates = mutableListOf<StatusUpdate>()
        var clearAllCount = 0
        var clearUnsentCount = 0

        override fun observeAll(): Flow<List<Thought>> = flowOf(rows.values.toList())
        override fun observeByStatus(status: ThoughtStatus): Flow<List<Thought>> =
            flowOf(rows.values.filter { it.status == status })
        override fun observeCounts(): Flow<List<StatusCount>> =
            flowOf(rows.values.groupingBy { it.status }.eachCount().map { StatusCount(it.key, it.value) })

        override suspend fun insert(thought: Thought): Long {
            val id = nextId.getAndIncrement()
            rows[id] = thought.copy(id = id)
            return id
        }

        override suspend fun updateStatus(id: Long, status: ThoughtStatus, error: String?, ob1Id: String?) {
            updates += StatusUpdate(id, status, error, ob1Id)
            rows[id]?.let {
                rows[id] = it.copy(status = status, lastError = error, ob1Id = ob1Id, attempts = it.attempts + 1)
            }
        }

        override suspend fun pending(): List<Thought> =
            rows.values.filter { it.status == ThoughtStatus.PENDING || it.status == ThoughtStatus.FAILED }

        override suspend fun clearAll() {
            clearAllCount++
            rows.clear()
        }

        override suspend fun clearUnsent() {
            clearUnsentCount++
            rows.entries.removeIf { (_, t) -> t.status == ThoughtStatus.PENDING || t.status == ThoughtStatus.FAILED }
        }
    }

    private fun seed(dao: FakeDao, vararg thoughts: Thought) {
        for (t in thoughts) {
            val id = if (t.id == 0L) dao.rows.keys.maxOrNull()?.plus(1) ?: 1L else t.id
            dao.rows[id] = t.copy(id = id)
        }
    }

    // Context is only used by insertPending(); the methods under test don't touch it. A
    // Mockito stub is sufficient — no method calls are made on it.
    private fun repo(dao: ThoughtDao): ThoughtRepository =
        ThoughtRepository(Mockito.mock(Context::class.java), dao)

    @Test
    fun `markSent records SENT status with ob1Id and clears error`() = runTest {
        val dao = FakeDao()
        seed(dao, Thought(id = 7, text = "x", createdAt = 0, status = ThoughtStatus.PENDING, attempts = 1, lastError = "prior"))
        repo(dao).markSent(id = 7, ob1Id = "remote-42")
        assertEquals(1, dao.updates.size)
        val u = dao.updates.first()
        assertEquals(ThoughtStatus.SENT, u.status)
        assertEquals("remote-42", u.ob1Id)
        assertNull(u.error)
        assertEquals(ThoughtStatus.SENT, dao.rows[7]!!.status)
    }

    @Test
    fun `markFailed records FAILED status with error and increments attempts`() = runTest {
        val dao = FakeDao()
        seed(dao, Thought(id = 3, text = "x", createdAt = 0, status = ThoughtStatus.PENDING, attempts = 0))
        repo(dao).markFailed(id = 3, error = "HTTP 500")
        val row = dao.rows[3]!!
        assertEquals(ThoughtStatus.FAILED, row.status)
        assertEquals("HTTP 500", row.lastError)
        assertEquals(1, row.attempts)
    }

    @Test
    fun `retry path - markFailed then markSent walks status PENDING - FAILED - SENT`() = runTest {
        val dao = FakeDao()
        seed(dao, Thought(id = 9, text = "x", createdAt = 0, status = ThoughtStatus.PENDING))
        val r = repo(dao)
        r.markFailed(9, "boom")
        assertEquals(ThoughtStatus.FAILED, dao.rows[9]!!.status)
        r.markSent(9, "remote-9")
        val row = dao.rows[9]!!
        assertEquals(ThoughtStatus.SENT, row.status)
        assertNull(row.lastError)
        assertEquals("remote-9", row.ob1Id)
        assertEquals(2, row.attempts) // one per updateStatus call
    }

    @Test
    fun `pending returns rows in PENDING or FAILED status, excludes SENT`() = runTest {
        val dao = FakeDao()
        seed(
            dao,
            Thought(id = 1, text = "p", createdAt = 1, status = ThoughtStatus.PENDING),
            Thought(id = 2, text = "f", createdAt = 2, status = ThoughtStatus.FAILED),
            Thought(id = 3, text = "s", createdAt = 3, status = ThoughtStatus.SENT),
        )
        val pending = repo(dao).pending()
        assertEquals(setOf(1L, 2L), pending.map { it.id }.toSet())
    }

    @Test
    fun `clearUnsent removes pending and failed but keeps sent`() = runTest {
        val dao = FakeDao()
        seed(
            dao,
            Thought(id = 1, text = "p", createdAt = 1, status = ThoughtStatus.PENDING),
            Thought(id = 2, text = "s", createdAt = 2, status = ThoughtStatus.SENT),
        )
        repo(dao).clearUnsent()
        assertEquals(setOf(2L), dao.rows.keys)
        assertEquals(1, dao.clearUnsentCount)
    }

    @Test
    fun `clearAll wipes the table`() = runTest {
        val dao = FakeDao()
        seed(
            dao,
            Thought(id = 1, text = "a", createdAt = 1, status = ThoughtStatus.SENT),
            Thought(id = 2, text = "b", createdAt = 2, status = ThoughtStatus.PENDING),
        )
        repo(dao).clearAll()
        assertEquals(0, dao.rows.size)
        assertEquals(1, dao.clearAllCount)
    }
}
