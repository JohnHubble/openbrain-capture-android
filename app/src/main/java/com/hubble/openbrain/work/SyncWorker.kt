package com.hubble.openbrain.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hubble.openbrain.data.api.OB1Client
import com.hubble.openbrain.data.repo.ThoughtRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: ThoughtRepository,
    private val client: OB1Client,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val pending = repository.pending()
        if (pending.isEmpty()) return Result.success()
        Log.i(TAG, "Syncing ${pending.size} thoughts")

        var sawRetriable = false
        for (thought in pending) {
            when (val result = client.captureThought(thought.text)) {
                is OB1Client.CaptureResult.Success -> {
                    Log.i(TAG, "Thought ${thought.id} sent")
                    repository.markSent(thought.id, ob1Id = null)
                }
                is OB1Client.CaptureResult.Failure -> {
                    Log.w(TAG, "Thought ${thought.id} failed: ${result.message} (retriable=${result.retriable})")
                    if (result.retriable) {
                        sawRetriable = true
                    } else {
                        repository.markFailed(thought.id, result.message)
                    }
                }
            }
        }
        return if (sawRetriable) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "SyncWorker"
    }
}
