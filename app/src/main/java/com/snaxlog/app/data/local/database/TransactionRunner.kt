package com.snaxlog.app.data.local.database

import androidx.room.withTransaction
import javax.inject.Inject

/**
 * Runs a block of suspending database operations atomically.
 *
 * Abstracts Room's [withTransaction] so repositories can be unit tested
 * with mocked DAOs and a pass-through fake runner.
 */
interface TransactionRunner {
    suspend operator fun <T> invoke(block: suspend () -> T): T
}

/**
 * Production implementation backed by Room's [withTransaction].
 */
class RoomTransactionRunner @Inject constructor(
    private val database: SnaxlogDatabase
) : TransactionRunner {
    override suspend operator fun <T> invoke(block: suspend () -> T): T =
        database.withTransaction(block)
}
