package com.focuslock.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_apps ORDER BY addedAt ASC")
    fun observeAll(): Flow<List<BlockedApp>>

    @Query("SELECT * FROM blocked_apps ORDER BY addedAt ASC")
    suspend fun getAll(): List<BlockedApp>

    @Query("SELECT COUNT(*) FROM blocked_apps")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: BlockedApp)

    @Query("DELETE FROM blocked_apps WHERE packageName = :pkg")
    suspend fun delete(pkg: String)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules WHERE id = 1 LIMIT 1")
    fun observe(): Flow<Schedule?>

    @Query("SELECT * FROM schedules WHERE id = 1 LIMIT 1")
    suspend fun get(): Schedule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(schedule: Schedule)
}

@Dao
interface UnlockCounterDao {
    @Query("SELECT unlockCount FROM unlock_counters WHERE dateKey = :dateKey LIMIT 1")
    fun observeCount(dateKey: String): Flow<Int?>

    @Query("SELECT unlockCount FROM unlock_counters WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getCount(dateKey: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(counter: UnlockCounter)
}

@Dao
interface ActiveGrantDao {
    @Query("SELECT * FROM active_grants")
    fun observeAll(): Flow<List<ActiveGrant>>

    @Query("SELECT * FROM active_grants")
    suspend fun getAll(): List<ActiveGrant>

    @Query("SELECT * FROM active_grants WHERE packageName = :pkg LIMIT 1")
    suspend fun get(pkg: String): ActiveGrant?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(grant: ActiveGrant)

    @Query("DELETE FROM active_grants WHERE packageName = :pkg")
    suspend fun delete(pkg: String)

    @Query("DELETE FROM active_grants WHERE expiresAt <= :now")
    suspend fun deleteExpired(now: Long)
}

@Dao
interface PendingWaitDao {
    @Query("SELECT * FROM pending_waits LIMIT 1")
    fun observe(): Flow<PendingWait?>

    @Query("SELECT * FROM pending_waits WHERE packageName = :pkg LIMIT 1")
    suspend fun get(pkg: String): PendingWait?

    @Query("SELECT * FROM pending_waits LIMIT 1")
    suspend fun getAny(): PendingWait?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(wait: PendingWait)

    @Query("DELETE FROM pending_waits WHERE packageName = :pkg")
    suspend fun delete(pkg: String)

    @Query("DELETE FROM pending_waits")
    suspend fun deleteAll()
}

@Dao
interface EventLogDao {
    @Insert
    suspend fun insert(log: EventLog)

    @Query("SELECT * FROM event_logs WHERE dateKey = :dateKey ORDER BY timestamp DESC")
    fun observeForDate(dateKey: String): Flow<List<EventLog>>
}
