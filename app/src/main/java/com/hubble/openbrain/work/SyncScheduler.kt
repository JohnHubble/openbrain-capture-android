package com.hubble.openbrain.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {

    const val UNIQUE_WORK = "ob1-sync"

    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }
}
