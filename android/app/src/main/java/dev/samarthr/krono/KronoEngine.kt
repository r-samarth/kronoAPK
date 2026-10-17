package dev.samarthr.krono

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max

// MARK: - 1. KRONO DESIGN TOKENS
object KronoTheme {
    val canvas = Color(0xFF0D0E11)
    val surface = Color(0xFF15171C)
    val surfaceHover = Color(0xFF1D2028)
    val border = Color(0x14FFFFFF)
    val inkPrimary = Color(0xFFF3F4F6)
    val inkMuted = Color(0xFF8A8F9E)
    val accent = Color(0xFFE05A47)
    val warning = Color(0xFFF59E0B)
    val critical = Color(0xFFEF4444)
    val completed = Color(0xFF10B981)

    fun deadlineColor(dueAtMillis: Long, isCompleted: Boolean, nowMillis: Long): Color {
        if (isCompleted) return completed
        val remainingMillis = dueAtMillis - nowMillis
        if (remainingMillis < 0) return critical
        if (remainingMillis < 3 * 60 * 60 * 1000L) return warning
        return inkMuted
    }
}

// MARK: - 2. TASK MODEL
data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val dueAtMillis: Long,
    val isCompleted: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun relativeDeadlineText(nowMillis: Long): String {
        if (isCompleted) return "Completed"

        val diffSeconds = (dueAtMillis - nowMillis) / 1000L
        if (diffSeconds < 0) {
            val mins = abs(diffSeconds) / 60
            return if (mins < 60) "Overdue ${mins}m" else "Overdue ${mins / 60}h"
        }

        val mins = diffSeconds / 60
        if (mins < 60) return "Due in ${max(1L, mins)}m"

        val hours = mins / 60
        if (hours < 24) return "Due in ${hours}h ${mins % 60}m"

        return SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            .format(Date(dueAtMillis))
    }
}

// MARK: - 3. QUICK DEADLINE PRESETS
enum class DeadlinePreset(val label: String) {
    ONE_HOUR("+1h"),
    THREE_HOURS("+3h"),
    TONIGHT("Tonight"),
    TOMORROW("Tomorrow");

    fun resolveMillis(): Long {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        return when (this) {
            ONE_HOUR -> now + 60 * 60 * 1000L
            THREE_HOURS -> now + 3 * 60 * 60 * 1000L
            TONIGHT -> {
                calendar.set(Calendar.HOUR_OF_DAY, 21)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                if (calendar.timeInMillis <= now) now + 6 * 60 * 60 * 1000L
                else calendar.timeInMillis
            }
            TOMORROW -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 10)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
        }
    }
}

// MARK: - 4. ANDROID DEADLINE NOTIFICATIONS
class DeadlineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationScheduler.ACTION_MONTHLY_SUMMARY) {
            NotificationScheduler.postMonthlySummary(
                context,
                intent.getLongExtra(NotificationScheduler.EXTRA_SUMMARY_MONTH_START, 0L)
            )
            return
        }

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Krono Reminder"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "A task deadline is approaching."
        val notificationId = intent.getIntExtra(EXTRA_ID, 1001)
        val channelId = NotificationScheduler.CHANNEL_ID

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Krono Deadline Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders before and at task deadlines"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        manager.notify(notificationId, notification)
    }

    companion object {
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_BODY = "EXTRA_BODY"
        const val EXTRA_ID = "EXTRA_ID"
    }
}

object NotificationScheduler {
    const val CHANNEL_ID = "krono_deadline_alerts"
    const val ACTION_MONTHLY_SUMMARY = "dev.samarthr.krono.MONTHLY_SUMMARY"
    const val EXTRA_SUMMARY_MONTH_START = "EXTRA_SUMMARY_MONTH_START"
    const val ACTION_MONTHLY_CLEANUP_COMPLETE = "dev.samarthr.krono.MONTHLY_CLEANUP_COMPLETE"
    const val EXTRA_CLEANUP_BEFORE = "EXTRA_CLEANUP_BEFORE"

    private const val MONTHLY_SUMMARY_REQUEST_CODE = 720031
    private const val MONTHLY_SUMMARY_NOTIFICATION_ID = 720032
    private const val STORAGE_PREFERENCES = "krono_local_storage_v1"
    private const val STORAGE_KEY = "tasks_json"
    private const val MINUTE_MILLIS = 60_000L

    fun syncNotifications(context: Context, tasks: List<TaskItem>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()

        tasks.forEach { task ->
            val warningRequestCode = requestCodeFor(task.id)
            val deadlineRequestCode = warningRequestCode + 1

            // Cancel old alarms first so edits, completion, and deletion are respected.
            cancelAlarm(context, alarmManager, warningRequestCode)
            cancelAlarm(context, alarmManager, deadlineRequestCode)

            if (!task.isCompleted && task.dueAtMillis > now) {
                val warningTime = task.dueAtMillis - 30 * 60 * 1000L
                if (warningTime > now) {
                    schedule(
                        context = context,
                        alarmManager = alarmManager,
                        requestCode = warningRequestCode,
                        triggerAtMillis = warningTime,
                        title = "Approaching Deadline (30m)",
                        body = task.title
                    )
                }

                schedule(
                    context = context,
                    alarmManager = alarmManager,
                    requestCode = deadlineRequestCode,
                    triggerAtMillis = task.dueAtMillis,
                    title = "Deadline Reached",
                    body = task.title
                )
            }
        }

        // Keep one month-end reminder scheduled, even when there are no active tasks.
        scheduleMonthlySummary(context)
    }

    /**
     * At 11:59 PM on the last calendar day, the receiver reads the latest local task
     * cache, sends the monthly summary, clears that month's completed tasks, and
     * schedules the next month. Counts are calculated at delivery time rather than
     * frozen when the alarm is first scheduled.
     */
    private fun scheduleMonthlySummary(context: Context) {
        val now = System.currentTimeMillis()
        val currentMonthStart = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var summaryMonthStart = currentMonthStart.clone() as Calendar
        summaryMonthStart.add(Calendar.MONTH, 1)
        var triggerAtMillis = summaryMonthStart.timeInMillis - MINUTE_MILLIS

        // If the month's 11:59 PM reminder has passed, target the following month.
        if (triggerAtMillis <= now) {
            summaryMonthStart.add(Calendar.MONTH, 1)
            currentMonthStart.add(Calendar.MONTH, 1)
            summaryMonthStart = currentMonthStart.clone() as Calendar
            summaryMonthStart.add(Calendar.MONTH, 1)
            triggerAtMillis = summaryMonthStart.timeInMillis - MINUTE_MILLIS
        }

        val monthStartMillis = currentMonthStart.timeInMillis
        val intent = Intent(context, DeadlineAlarmReceiver::class.java).apply {
            action = ACTION_MONTHLY_SUMMARY
            putExtra(EXTRA_SUMMARY_MONTH_START, monthStartMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MONTHLY_SUMMARY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun postMonthlySummary(context: Context, requestedMonthStartMillis: Long) {
        val now = System.currentTimeMillis()
        val currentMonthStart = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Older or malformed intents fall back to the current calendar month.
        val monthStartMillis = if (requestedMonthStartMillis > 0L) {
            requestedMonthStartMillis
        } else {
            currentMonthStart
        }
        val nextMonthStartMillis = Calendar.getInstance().apply {
            timeInMillis = monthStartMillis
            add(Calendar.MONTH, 1)
        }.timeInMillis

        val preferences = context.getSharedPreferences(STORAGE_PREFERENCES, Context.MODE_PRIVATE)
        val savedTasks = preferences.getString(STORAGE_KEY, null)
        val savedArray = try {
            JSONArray(savedTasks ?: "[]")
        } catch (_: Exception) {
            JSONArray()
        }

        var completedCount = 0
        var overdueCount = 0
        val remainingTasks = JSONArray()

        for (index in 0 until savedArray.length()) {
            val task = savedArray.optJSONObject(index) ?: continue
            val isCompleted = task.optBoolean("isCompleted", false)
            val dueAtMillis = task.optLong("dueAtMillis", Long.MAX_VALUE)
            val updatedAtMillis = task.optLong("updatedAtMillis", 0L)

            if (isCompleted &&
                updatedAtMillis >= monthStartMillis &&
                updatedAtMillis < nextMonthStartMillis
            ) {
                completedCount += 1
            }
            if (!isCompleted && dueAtMillis < now) {
                overdueCount += 1
            }

            // Clear completed tasks from the month that is ending (and any older ones).
            if (!(isCompleted && updatedAtMillis < nextMonthStartMillis)) {
                remainingTasks.put(task)
            }
        }

        preferences.edit().putString(STORAGE_KEY, remainingTasks.toString()).apply()

        // If the task screen is still in memory, update its observable list immediately too.
        context.sendBroadcast(
            Intent(ACTION_MONTHLY_CLEANUP_COMPLETE)
                .setPackage(context.packageName)
                .putExtra(EXTRA_CLEANUP_BEFORE, nextMonthStartMillis)
        )

        val monthName = SimpleDateFormat("MMMM", Locale.getDefault())
            .format(Date(monthStartMillis))
        val completedLabel = if (completedCount == 1) "task" else "tasks"
        val overdueLabel = if (overdueCount == 1) "overdue task" else "overdue tasks"
        val body = "Completed $completedCount $completedLabel this month. Still $overdueCount $overdueLabel pending."

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Krono Deadline Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Task deadline alerts and month-end summaries"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            context,
            MONTHLY_SUMMARY_NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Krono $monthName Summary")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        manager.notify(MONTHLY_SUMMARY_NOTIFICATION_ID, notification)

        // Schedule another one-time alarm for the next month.
        scheduleMonthlySummary(context)
    }

    fun cancelForTask(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val warningRequestCode = requestCodeFor(taskId)
        cancelAlarm(context, alarmManager, warningRequestCode)
        cancelAlarm(context, alarmManager, warningRequestCode + 1)
    }

    private fun requestCodeFor(taskId: String): Int = taskId.hashCode() and 0x3FFFFFFE

    private fun schedule(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        triggerAtMillis: Long,
        title: String,
        body: String
    ) {
        val intent = Intent(context, DeadlineAlarmReceiver::class.java).apply {
            putExtra(DeadlineAlarmReceiver.EXTRA_TITLE, title)
            putExtra(DeadlineAlarmReceiver.EXTRA_BODY, body)
            putExtra(DeadlineAlarmReceiver.EXTRA_ID, requestCode)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Fall back to an inexact alarm if exact-alarm access is not granted.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, DeadlineAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}

// MARK: - 5. PRIVATE ON-DEVICE TASK STORE
class TaskStore(context: Context) {
    private val appContext = context.applicationContext
    private val monthEndCleanupReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val cleanupBefore = intent.getLongExtra(NotificationScheduler.EXTRA_CLEANUP_BEFORE, 0L)
            if (cleanupBefore <= 0L) return

            tasks.removeAll { task ->
                task.isCompleted && task.updatedAtMillis < cleanupBefore
            }
            sortAndPersist()
        }
    }
    private val preferences = appContext.getSharedPreferences(
        "krono_local_storage_v1",
        Context.MODE_PRIVATE
    )
    private val storageKey = "tasks_json"

    val tasks = mutableStateListOf<TaskItem>()

    val activeCount: Int
        get() = tasks.count { !it.isCompleted }

    init {
        loadFromDeviceStorage()
        androidx.core.content.ContextCompat.registerReceiver(
            appContext,
            monthEndCleanupReceiver,
            IntentFilter(NotificationScheduler.ACTION_MONTHLY_CLEANUP_COMPLETE),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun close() {
        try {
            appContext.unregisterReceiver(monthEndCleanupReceiver)
        } catch (_: IllegalArgumentException) {
            // Already unregistered during Activity teardown.
        }
    }

    fun addTask(title: String, dueAtMillis: Long) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return

        tasks.add(
            0,
            TaskItem(
                title = trimmedTitle,
                dueAtMillis = dueAtMillis
            )
        )
        sortAndPersist()
    }

    fun toggleTask(task: TaskItem) {
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index == -1) return

        val current = tasks[index]
        tasks[index] = current.copy(
            isCompleted = !current.isCompleted,
            updatedAtMillis = System.currentTimeMillis()
        )
        sortAndPersist()
    }

    fun deleteTask(task: TaskItem) {
        NotificationScheduler.cancelForTask(appContext, task.id)
        tasks.removeAll { it.id == task.id }
        sortAndPersist()
    }

    private fun sortAndPersist() {
        val sortedTasks = tasks.sortedWith(
            compareBy<TaskItem> { it.isCompleted }.thenBy { it.dueAtMillis }
        )
        tasks.clear()
        tasks.addAll(sortedTasks)
        saveToDeviceStorage()
        NotificationScheduler.syncNotifications(appContext, tasks.toList())
    }

    private fun saveToDeviceStorage() {
        val array = JSONArray()
        tasks.forEach { task ->
            val jsonTask = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("dueAtMillis", task.dueAtMillis)
                put("isCompleted", task.isCompleted)
                put("updatedAtMillis", task.updatedAtMillis)
            }
            array.put(jsonTask)
        }
        preferences.edit().putString(storageKey, array.toString()).apply()
    }


    private fun pruneCompletedTasksFromPreviousMonths() {
        val currentMonthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        tasks.removeAll { task ->
            task.isCompleted && task.updatedAtMillis < currentMonthStart
        }
    }

    private fun loadFromDeviceStorage() {
        val raw = preferences.getString(storageKey, null)
        if (raw == null) {
            val now = System.currentTimeMillis()
            tasks.addAll(
                listOf(
                    TaskItem(
                        title = "Welcome to Krono — tap the checkbox to complete",
                        dueAtMillis = now + 90 * 60 * 1000L
                    ),
                    TaskItem(
                        title = "Set a deadline below to test reminders",
                        dueAtMillis = now + 5 * 60 * 60 * 1000L
                    )
                )
            )
            sortAndPersist()
            return
        }

        try {
            val jsonArray = JSONArray(raw)
            val loadedTasks = mutableListOf<TaskItem>()
            for (index in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(index)
                loadedTasks.add(
                    TaskItem(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        dueAtMillis = item.getLong("dueAtMillis"),
                        isCompleted = item.getBoolean("isCompleted"),
                        updatedAtMillis = item.optLong(
                            "updatedAtMillis",
                            System.currentTimeMillis()
                        )
                    )
                )
            }
            tasks.clear()
            tasks.addAll(loadedTasks)
            // If Krono missed a month-end while closed or after a reboot, prune
            // completed tasks from older months the next time the app opens.
            pruneCompletedTasksFromPreviousMonths()
            sortAndPersist()
        } catch (_: Exception) {
            // Do not crash the app if an old or damaged local cache cannot be parsed.
            tasks.clear()
            saveToDeviceStorage()
            NotificationScheduler.syncNotifications(appContext, emptyList())
        }
    }
}
