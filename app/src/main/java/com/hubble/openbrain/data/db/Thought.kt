package com.hubble.openbrain.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ThoughtStatus { PENDING, SENT, FAILED }

@Entity(tableName = "thoughts")
data class Thought(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAt: Long,
    val durationMs: Long = 0,
    val status: ThoughtStatus = ThoughtStatus.PENDING,
    val attempts: Int = 0,
    val lastError: String? = null,
    val ob1Id: String? = null,
)
