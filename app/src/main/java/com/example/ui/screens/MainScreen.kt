package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.M3Background
import com.example.ui.theme.M3OnPrimaryContainer
import com.example.ui.theme.M3OnSecondaryContainer
import com.example.ui.theme.M3OnSurface
import com.example.ui.theme.M3OnSurfaceVariant
import com.example.ui.theme.M3Outline
import com.example.ui.theme.M3Primary
import com.example.ui.theme.M3PrimaryContainer
import com.example.ui.theme.M3SecondaryContainer
import com.example.ui.theme.M3Surface
import com.example.ui.theme.M3SurfaceContainer
import com.example.ui.theme.M3SurfaceContainerHigh
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainer
import com.example.ui.theme.StatusGreenText
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedContainer
import com.example.ui.theme.StatusRedText
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.TestUploadState
import com.example.util.BatteryOptimizationHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isAutoUploadEnabled by viewModel.isAutoUploadEnabled.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnectedToInternet.collectAsStateWithLifecycle()
    val sentCount by viewModel.sentPhotosCount.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingPhotosCount.collectAsStateWithLifecycle()
    val lastUploadStatus by viewModel.lastUploadStatus.collectAsStateWithLifecycle()
    val lastUploadTime by viewModel.lastUploadTime.collectAsStateWithLifecycle()
    val isBatteryRestricted by viewModel.isBatteryRestricted.collectAsStateWithLifecycle()
    val testUploadState by viewModel.testUploadState.collectAsStateWithLifecycle()
    val botToken by viewModel.botToken.collectAsStateWithLifecycle()
    val chatId by viewModel.chatId.collectAsStateWithLifecycle()

    var showBatteryDialog by remember { mutableStateOf(false) }

    // Photo picker for sending test photo
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            if (botToken.isBlank() || chatId.isBlank()) {
                scope.launch {
                    snackbarHostState.showSnackbar("ابتدا در بخش تنظیمات، توکن ربات و شناسه چت را وارد کنید.")
                }
            } else {
                viewModel.sendTestPhoto(uri, context)
            }
        }
    }

    LaunchedEffect(testUploadState) {
        when (val state = testUploadState) {
            is TestUploadState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetTestUploadState()
            }
            is TestUploadState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetTestUploadState()
            }
            else -> {}
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = M3Background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "ارسال خودکار به تلگرام",
                            fontWeight = FontWeight.Medium,
                            fontSize = 20.sp,
                            color = M3OnSurface
                        )
                    },
                    actions = {
                        // Connection status pill in the header
                        Row(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) StatusGreenContainer else StatusRedContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("internet_status"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) StatusGreen else StatusRed)
                            )
                            Text(
                                text = if (isConnected) "متصل" else "قطع",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isConnected) StatusGreenText else StatusRedText
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = M3Background,
                        titleContentColor = M3OnSurface
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Hero Status Section (bg #EADDFF, rounded 28px, text #21005D)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = M3PrimaryContainer,
                        contentColor = M3OnPrimaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "وضعیت ارسال",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    color = M3OnPrimaryContainer
                                )
                                Text(
                                    text = if (isAutoUploadEnabled) "سرویس پس‌زمینه در حال اجراست" else "سرویس ارسال متوقف است",
                                    fontSize = 13.sp,
                                    color = M3OnPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }

                            Switch(
                                checked = isAutoUploadEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked && (botToken.isBlank() || chatId.isBlank())) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("لطفاً ابتدا توکن ربات و شناسه چت را در تنظیمات وارد کنید.")
                                        }
                                        onNavigateToSettings()
                                    } else {
                                        viewModel.toggleAutoUpload(isChecked, context)
                                    }
                                },
                                modifier = Modifier.testTag("auto_upload_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = M3Primary,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = M3Outline.copy(alpha = 0.6f)
                                )
                            )
                        }

                        // Stat Tiles Grid (2 columns, bg #FEF7FF, border #CAC4D0, rounded 16px)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Sent Count Card
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = M3Surface),
                                border = BorderStroke(1.dp, M3Outline)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp, horizontal = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "$sentCount",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp,
                                        color = M3Primary,
                                        modifier = Modifier.testTag("sent_photos_count")
                                    )
                                    Text(
                                        text = "تصویر ارسال شده",
                                        fontSize = 11.sp,
                                        color = M3OnSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Pending Count Card
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = M3Surface),
                                border = BorderStroke(1.dp, M3Outline)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp, horizontal = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "$pendingCount",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp,
                                        color = if (pendingCount > 0) StatusRed else M3OnSurfaceVariant,
                                        modifier = Modifier.testTag("pending_photos_count")
                                    )
                                    Text(
                                        text = "در انتظار (خطا)",
                                        fontSize = 11.sp,
                                        color = M3OnSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Privacy rule note
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(M3Surface.copy(alpha = 0.85f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "🔒 حریم خصوصی: عکس‌های قدیمی گالری ارسال نمی‌شوند. تنها عکس‌هایی که بعد از فعال‌سازی با دوربین ثبت شوند در صف قرار می‌گیرند.",
                                fontSize = 11.sp,
                                color = M3OnSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                // Section 2: Battery Optimization Card (bg white/surface, border #CAC4D0, rounded 28px)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = M3Surface),
                    border = BorderStroke(1.dp, M3Outline)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(M3SurfaceContainerHigh)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryAlert,
                                contentDescription = null,
                                tint = M3Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "بهینه‌سازی باتری",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = M3OnSurface
                            )
                        }

                        // Divider line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(M3Outline)
                        )

                        // Body
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "برای اطمینان از عملکرد صحیح در حالت Doze، محدودیت باتری را غیرفعال کنید.",
                                fontSize = 12.sp,
                                color = M3OnSurfaceVariant,
                                lineHeight = 18.sp,
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = { showBatteryDialog = true },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = M3Primary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .testTag("open_battery_prompt")
                                    .testTag("battery_settings_button")
                            ) {
                                Text(
                                    text = "تغییر تنظیمات",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Section 3: Action Buttons Grid (2 columns, bg #F3EDF7, rounded 24px)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Action 1: Send Test Photo
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(enabled = testUploadState !is TestUploadState.Uploading) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("send_test_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = M3SurfaceContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(M3SecondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                if (testUploadState is TestUploadState.Uploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = M3Primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        tint = M3OnSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (testUploadState is TestUploadState.Uploading) "در حال ارسال..." else "ارسال تست",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = M3OnSecondaryContainer
                            )
                        }
                    }

                    // Action 2: View Queue
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onNavigateToQueue() }
                            .testTag("queue_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = M3SurfaceContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(M3SecondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Queue,
                                    contentDescription = null,
                                    tint = M3OnSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "مشاهده صف",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = M3OnSecondaryContainer
                            )
                        }
                    }
                }

                // Secondary Row: Settings & Toggle Auto-Upload
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Settings Tile
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onNavigateToSettings() }
                            .testTag("settings_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = M3SurfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(M3SecondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = M3OnSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "تنظیمات ربات",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = M3OnSecondaryContainer
                            )
                        }
                    }

                    // Toggle Auto-Upload Tile
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable {
                                if (!isAutoUploadEnabled && (botToken.isBlank() || chatId.isBlank())) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("ابتدا توکن ربات و شناسه چت را در تنظیمات تکمیل کنید.")
                                    }
                                    onNavigateToSettings()
                                } else {
                                    viewModel.toggleAutoUpload(!isAutoUploadEnabled, context)
                                }
                            }
                            .testTag("toggle_upload_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAutoUploadEnabled) StatusRedContainer else M3SurfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isAutoUploadEnabled) StatusRed.copy(alpha = 0.2f) else M3SecondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = if (isAutoUploadEnabled) StatusRedText else M3OnSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = if (isAutoUploadEnabled) "توقف ارسال" else "روشن کردن",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isAutoUploadEnabled) StatusRedText else M3OnSecondaryContainer
                            )
                        }
                    }
                }

                // Section 4: Last Activity Card (bg #F7F2FA, rounded 16px)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = M3SurfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "آخرین فعالیت",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = M3OnSurfaceVariant
                            )
                            Text(
                                text = if (lastUploadTime > 0L) formatRelativeTime(lastUploadTime) else "",
                                fontSize = 10.sp,
                                color = M3OnSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (lastUploadTime > 0L) M3Primary else M3Outline)
                            )
                            Text(
                                text = formatLastUpload(lastUploadStatus, lastUploadTime),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = M3OnSurface,
                                modifier = Modifier.testTag("last_upload_status")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Battery Optimization Dialog
        if (showBatteryDialog) {
            AlertDialog(
                onDismissRequest = { showBatteryDialog = false },
                shape = RoundedCornerShape(28.dp),
                containerColor = M3Surface,
                title = {
                    Text(
                        text = "تنظیمات بهینه‌سازی باتری",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = M3OnSurface
                    )
                },
                text = {
                    Text(
                        text = "برای اینکه برنامه بتواند هنگام قفل بودن گوشی، خاموش بودن صفحه یا بسته بودن برنامه عکس‌های جدید را ارسال کند، باید مصرف باتری آن روی «نامحدود / بدون بهینه‌سازی» (Unrestricted) قرار گیرد.\n\nآیا مایلید به بخش تنظیمات باتری سیستم هدایت شوید؟",
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = M3OnSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showBatteryDialog = false
                            try {
                                context.startActivity(BatteryOptimizationHelper.createBatterySettingsIntent(context))
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(BatteryOptimizationHelper.createAppDetailsSettingsIntent(context))
                                } catch (_: Exception) {}
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = M3Primary),
                        modifier = Modifier.testTag("confirm_open_battery_settings")
                    ) {
                        Text("باز کردن تنظیمات")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatteryDialog = false }) {
                        Text("انصراف", color = M3Primary)
                    }
                }
            )
        }
    }
}

private fun formatLastUpload(status: String, timestamp: Long): String {
    if (timestamp <= 0L) return "هنوز ارسالی انجام نشده است"
    val timeStr = SimpleDateFormat("HH:mm - yyyy/MM/dd", Locale.getDefault()).format(Date(timestamp))
    return if (status.isNotBlank()) "وضعیت: $status ($timeStr)" else timeStr
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 60_000L) return "هم‌اکنون"
    val minutes = diff / 60_000L
    if (minutes < 60L) return "$minutes دقیقه پیش"
    val hours = minutes / 60L
    if (hours < 24L) return "$hours ساعت پیش"
    return SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(timestamp))
}
