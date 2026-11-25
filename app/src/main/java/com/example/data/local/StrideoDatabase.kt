package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.RoutePoint
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = UserProfile.LOCAL_PROFILE_ID,
    val displayName: String,
    val birthDay: Int,
    val birthMonth: Int,
    val birthYear: Int,
    val email: String,
    val photoUrl: String?,
    val heightCm: Double?,
    val weightKg: Double?,
    val unitDistance: String,
    val unitWeight: String,
    val use24HourClock: Boolean,
    val notificationsEnabled: Boolean,
    val privateProfile: Boolean,
    val timezone: String?,
    val signedIn: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val startedAtEpochMs: Long,
    val durationSec: Long,
    val movingDurationSec: Long,
    val distanceMeters: Double,
    val avgSpeedMps: Double,
    val avgPaceSecPerKm: Double,
    val caloriesBurned: Long,
    val elevationGainMeters: Double,
    val manualEntry: Boolean,
    val pointCount: Long,
    val routePoints: List<RoutePoint>,
    val notes: String?,
    val locationName: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "health_metrics")
data class HealthMetricEntity(
    @PrimaryKey val dateKey: String,
    val steps: Long,
    val caloriesBurned: Long,
    val waterMl: Long,
    val weightKg: Double?,
    val heightCm: Double?,
    val sleepMinutes: Long?,
    val sleepQuality: Long?,
    val restingHeartRate: Long?,
    val systolicBp: Long?,
    val diastolicBp: Long?,
    val caloriesConsumed: Long?,
    val moodLevel: Long?,
    val energyLevel: Long?,
    val notes: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "hydration_logs")
data class HydrationLogEntity(
    @PrimaryKey val id: String,
    val dateKey: String,
    val amountMl: Long,
    val loggedAtEpochMs: Long,
    val label: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "sleep_logs")
data class SleepLogEntity(
    @PrimaryKey val id: String,
    val dateKey: String,
    val bedtimeLabel: String,
    val wakeTimeLabel: String,
    val durationMinutes: Long,
    val qualityRating: Long,
    val notes: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "fitness_goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val metricType: String,
    val targetValue: Double,
    val unit: String,
    val period: String,
    val active: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

class StrideoConverters {
    @TypeConverter
    fun fromRoutePoints(points: List<RoutePoint>): String {
        val arr = JSONArray()
        for (p in points) {
            arr.put(
                JSONObject().apply {
                    put("lat", p.lat)
                    put("lng", p.lng)
                    put("alt", p.alt)
                    put("acc", p.acc)
                    put("ts", p.ts)
                }
            )
        }
        return arr.toString()
    }

    @TypeConverter
    fun toRoutePoints(raw: String): List<RoutePoint> {
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = ArrayList<RoutePoint>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                list.add(
                    RoutePoint(
                        lat = obj.optDouble("lat", 0.0),
                        lng = obj.optDouble("lng", 0.0),
                        alt = obj.optDouble("alt", 0.0),
                        acc = obj.optDouble("acc", 0.0),
                        ts = obj.optLong("ts", 0L)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}

@Dao
interface StrideoDao {
    // --- Profile ---
    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    fun observeProfile(id: String = UserProfile.LOCAL_PROFILE_ID): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    suspend fun getProfileOnce(id: String = UserProfile.LOCAL_PROFILE_ID): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Query("UPDATE user_profile SET signedIn = :signedIn, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun updateSignedIn(signedIn: Boolean, updatedAt: Long, id: String = UserProfile.LOCAL_PROFILE_ID)

    @Query("DELETE FROM user_profile")
    suspend fun clearProfiles()

    // --- Activities ---
    @Query("SELECT * FROM activities ORDER BY startedAtEpochMs DESC")
    fun observeActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities ORDER BY startedAtEpochMs DESC")
    suspend fun getAllActivitiesOnce(): List<ActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivity(activity: ActivityEntity)

    @Query("UPDATE activities SET title = :title, notes = :notes, locationName = :locationName, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun updateActivityMetadata(
        id: String,
        title: String,
        notes: String?,
        locationName: String?,
        updatedAt: Long
    )

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivity(id: String)

    @Query("DELETE FROM activities")
    suspend fun clearActivities()

    // --- Health Metrics ---
    @Query("SELECT * FROM health_metrics ORDER BY dateKey DESC")
    fun observeHealthMetrics(): Flow<List<HealthMetricEntity>>

    @Query("SELECT * FROM health_metrics ORDER BY dateKey DESC")
    suspend fun getAllHealthMetricsOnce(): List<HealthMetricEntity>

    @Query("SELECT * FROM health_metrics WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getHealthMetricByDate(dateKey: String): HealthMetricEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHealthMetric(metric: HealthMetricEntity)

    @Query("DELETE FROM health_metrics")
    suspend fun clearHealthMetrics()

    // --- Hydration Logs ---
    @Query("SELECT * FROM hydration_logs ORDER BY loggedAtEpochMs DESC")
    fun observeHydrationLogs(): Flow<List<HydrationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHydrationLog(log: HydrationLogEntity)

    @Query("DELETE FROM hydration_logs WHERE id = :id")
    suspend fun deleteHydrationLog(id: String)

    @Query("DELETE FROM hydration_logs")
    suspend fun clearHydrationLogs()

    // --- Sleep Logs ---
    @Query("SELECT * FROM sleep_logs ORDER BY dateKey DESC, createdAtEpochMs DESC")
    fun observeSleepLogs(): Flow<List<SleepLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepLog(log: SleepLogEntity)

    @Query("DELETE FROM sleep_logs WHERE id = :id")
    suspend fun deleteSleepLog(id: String)

    @Query("DELETE FROM sleep_logs")
    suspend fun clearSleepLogs()

    // --- Goals ---
    @Query("SELECT * FROM fitness_goals")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT COUNT(*) FROM fitness_goals")
    suspend fun countGoals(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: GoalEntity)

    @Query("DELETE FROM fitness_goals WHERE id = :id")
    suspend fun deleteGoal(id: String)

    @Query("DELETE FROM fitness_goals")
    suspend fun clearGoals()
}

@Database(
    entities = [
        UserProfileEntity::class,
        ActivityEntity::class,
        HealthMetricEntity::class,
        HydrationLogEntity::class,
        SleepLogEntity::class,
        GoalEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(StrideoConverters::class)
abstract class StrideoDatabase : RoomDatabase() {
    abstract fun strideoDao(): StrideoDao

    companion object {
        @Volatile
        private var INSTANCE: StrideoDatabase? = null

        fun getInstance(context: Context): StrideoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StrideoDatabase::class.java,
                    "strideo_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
