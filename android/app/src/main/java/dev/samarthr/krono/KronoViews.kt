package dev.samarthr.krono

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun KronoAndroidScreen(store: TaskStore) {
    val context = LocalContext.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var newTitle by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf<DeadlinePreset?>(DeadlinePreset.THREE_HOURS) }
    var customDeadlineMillis by remember { mutableStateOf<Long?>(null) }
    var showCompletedOnly by remember { mutableStateOf(false) }
    var showMonthlyHistory by remember { mutableStateOf(false) }
    var showLocalInfo by remember { mutableStateOf(false) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val visibleTasks = store.tasks.filter { task ->
        if (showCompletedOnly) task.isCompleted else !task.isCompleted
    }

    fun submitTask() {
        val title = newTitle.trim()
        if (title.isEmpty()) return

        val deadline = customDeadlineMillis
            ?: selectedPreset?.resolveMillis()
            ?: (System.currentTimeMillis() + 3 * 60 * 60 * 1000L)

        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        store.addTask(title, deadline)
        newTitle = ""
        customDeadlineMillis = null
        selectedPreset = DeadlinePreset.THREE_HOURS
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun openNativeDateTimePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(year, month, dayOfMonth, hourOfDay, minute, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        if (calendar.timeInMillis <= System.currentTimeMillis()) {
                            Toast.makeText(context, "Choose a future deadline", Toast.LENGTH_SHORT).show()
                        } else {
                            customDeadlineMillis = calendar.timeInMillis
                            selectedPreset = null
                        }
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
        }.show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KronoTheme.canvas)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Shared Krono header: logo, creator watermark, monthly archive and local actions.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "Krono logo",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "KRONO // MOBILE",
                        color = KronoTheme.accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.25.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "${store.activeCount} Active Tasks",
                        color = KronoTheme.inkPrimary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            runCatching {
                                uriHandler.openUri("https://github.com/r-samarth/Krono")
                            }
                        }
                        .padding(horizontal = 2.dp, vertical = 3.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = "Built by Sam",
                        color = KronoTheme.inkMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = "Open source",
                        color = KronoTheme.inkMuted.copy(alpha = 0.72f),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                HeaderIconButton(
                    imageVector = Icons.Outlined.History,
                    contentDescription = "Monthly notification history",
                    onClick = { showMonthlyHistory = true }
                )
                HeaderIconButton(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Local storage information",
                    onClick = { showLocalInfo = true }
                )
                HeaderIconButton(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Refresh local tasks",
                    onClick = {
                        store.refreshLocalState()
                        nowMillis = System.currentTimeMillis()
                    }
                )
            }

            // Keep the existing Active/Logbook switch, just below the header.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(KronoTheme.surface)
                        .border(1.dp, KronoTheme.border, CircleShape)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterPillButton(
                        title = "Active",
                        isSelected = !showCompletedOnly,
                        onClick = { showCompletedOnly = false }
                    )
                    FilterPillButton(
                        title = "Logbook",
                        isSelected = showCompletedOnly,
                        onClick = { showCompletedOnly = true }
                    )
                }
            }

            // Deadline status legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = KronoTheme.critical, label = "Overdue")
                LegendDot(color = KronoTheme.warning, label = "Under 3h")
                LegendDot(color = KronoTheme.completed, label = "Completed")
            }

            if (visibleTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = KronoTheme.completed,
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = if (showCompletedOnly) "No completed tasks yet" else "Zero pending tasks. Clear skies.",
                            color = KronoTheme.inkMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 4.dp,
                        bottom = 190.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visibleTasks, key = { it.id }) { task ->
                        TaskRowView(
                            task = task,
                            nowMillis = nowMillis,
                            onToggle = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                store.toggleTask(task)
                            },
                            onDelete = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                store.deleteTask(task)
                            }
                        )
                    }
                }
            }
        }

        // Bottom-anchored task composer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .shadow(24.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(KronoTheme.surface)
                .border(1.dp, KronoTheme.border, RoundedCornerShape(22.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DeadlinePreset.entries.forEach { preset ->
                    val isSelected = selectedPreset == preset && customDeadlineMillis == null
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) KronoTheme.accent.copy(alpha = 0.88f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                selectedPreset = preset
                                customDeadlineMillis = null
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = preset.label,
                            color = if (isSelected) Color.White else KronoTheme.inkMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (customDeadlineMillis != null) {
                    Text(
                        text = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                            .format(Date(customDeadlineMillis!!)),
                        color = KronoTheme.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { openNativeDateTimePicker() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = "Pick custom deadline",
                        tint = if (customDeadlineMillis != null) KronoTheme.accent else KronoTheme.inkMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 3.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (newTitle.isEmpty()) {
                        Text(
                            text = "New task with deadline...",
                            color = KronoTheme.inkMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    BasicTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        singleLine = true,
                        cursorBrush = SolidColor(KronoTheme.accent),
                        textStyle = TextStyle(
                            color = KronoTheme.inkPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitTask() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val canSubmit = newTitle.trim().isNotEmpty()
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            if (canSubmit) KronoTheme.accent
                            else KronoTheme.accent.copy(alpha = 0.35f)
                        )
                        .clickable(enabled = canSubmit) { submitTask() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create task",
                        tint = Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
        }
    }

    if (showMonthlyHistory) {
        MonthlySummaryHistoryDialog(onDismiss = { showMonthlyHistory = false })
    }

    if (showLocalInfo) {
        AlertDialog(
            onDismissRequest = { showLocalInfo = false },
            containerColor = KronoTheme.surface,
            title = {
                Text(
                    text = "Krono on this device",
                    color = KronoTheme.inkPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Your tasks, reminders and monthly summary history are stored privately on this Android device. No account or cloud connection is used. Uninstalling Krono or clearing its app data may erase this information.",
                    color = KronoTheme.inkMuted
                )
            },
            confirmButton = {
                TextButton(onClick = { showLocalInfo = false }) {
                    Text("Done", color = KronoTheme.accent)
                }
            }
        )
    }
}

@Composable
private fun HeaderIconButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(KronoTheme.surface)
            .border(1.dp, KronoTheme.border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = KronoTheme.inkMuted,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun MonthlySummaryHistoryDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var summaries by remember { mutableStateOf(emptyList<MonthlySummaryRecord>()) }

    LaunchedEffect(Unit) {
        summaries = NotificationScheduler.loadMonthlySummaries(context)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(KronoTheme.canvas)
                .border(1.dp, KronoTheme.border, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "MONTHLY ARCHIVE",
                        color = KronoTheme.accent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.25.sp
                    )
                    Text(
                        text = "Notification History",
                        color = KronoTheme.inkPrimary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Done",
                    color = KronoTheme.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(KronoTheme.border)
            )

            if (summaries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = KronoTheme.accent,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No monthly summaries yet",
                            color = KronoTheme.inkPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Krono will save this month's summary here and update it as your tasks change. Previous Android notifications cannot be imported retroactively, so history begins when this version is installed.",
                            color = KronoTheme.inkMuted,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(top = 14.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(summaries, key = { it.monthStartMillis }) { summary ->
                        MonthlySummaryCard(summary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlySummaryCard(summary: MonthlySummaryRecord) {
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        .format(Date(summary.monthStartMillis))
    val updatedText = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        .format(Date(summary.savedAtMillis))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KronoTheme.surface)
            .border(1.dp, KronoTheme.border, RoundedCornerShape(16.dp))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = monthName,
                color = KronoTheme.inkPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${summary.progressPercentage}%",
                color = KronoTheme.completed,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MonthlyMetricTile(
                modifier = Modifier.weight(1f),
                label = "COMPLETED",
                count = summary.completedCount,
                color = KronoTheme.completed
            )
            MonthlyMetricTile(
                modifier = Modifier.weight(1f),
                label = "PENDING OVERDUE",
                count = summary.pendingCount,
                color = KronoTheme.warning
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Progress", color = KronoTheme.inkMuted, fontSize = 11.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text("Completion estimate", color = KronoTheme.inkMuted, fontSize = 10.sp)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(KronoTheme.border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(summary.progressPercentage.coerceIn(0, 100) / 100f)
                        .fillMaxHeight()
                        .background(KronoTheme.completed)
                )
            }
        }

        Text(
            text = summary.notificationTitle,
            color = KronoTheme.inkPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = summary.notificationBody,
            color = KronoTheme.inkMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
        Text(
            text = "Last updated $updatedText",
            color = KronoTheme.inkMuted.copy(alpha = 0.8f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun MonthlyMetricTile(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    color: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(KronoTheme.canvas)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = label,
            color = KronoTheme.inkMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = count.toString(),
            color = color,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun TaskRowView(
    task: TaskItem,
    nowMillis: Long,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val signalColor = KronoTheme.deadlineColor(task.dueAtMillis, task.isCompleted, nowMillis)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(KronoTheme.surface)
            .border(1.dp, KronoTheme.border, RoundedCornerShape(15.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SemanticCheckbox(
            isCompleted = task.isCompleted,
            urgencyColor = signalColor,
            onToggle = onToggle
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = task.title,
                color = if (task.isCompleted) KronoTheme.inkMuted else KronoTheme.inkPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(signalColor)
                )
                Text(
                    text = task.relativeDeadlineText(nowMillis),
                    color = signalColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
                .clickable { onDelete() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete task",
                tint = KronoTheme.inkMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SemanticCheckbox(
    isCompleted: Boolean,
    urgencyColor: Color,
    onToggle: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isCompleted) KronoTheme.completed else Color.White.copy(alpha = 0.03f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "checkboxBackground"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isCompleted) KronoTheme.completed else urgencyColor.copy(alpha = 0.75f),
        label = "checkboxBorder"
    )

    Box(
        modifier = Modifier
            .size(23.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(backgroundColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(7.dp))
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = KronoTheme.canvas,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun FilterPillButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (isSelected) Color.White.copy(alpha = 0.10f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 7.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) KronoTheme.inkPrimary else KronoTheme.inkMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            color = KronoTheme.inkMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
