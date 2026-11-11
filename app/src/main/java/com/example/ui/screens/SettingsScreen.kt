package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.M3Background
import com.example.ui.theme.M3OnPrimary
import com.example.ui.theme.M3OnSurface
import com.example.ui.theme.M3OnSurfaceVariant
import com.example.ui.theme.M3Outline
import com.example.ui.theme.M3Primary
import com.example.ui.theme.M3SecondaryContainer
import com.example.ui.theme.M3Surface
import com.example.ui.theme.M3SurfaceContainerHigh
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainer
import com.example.ui.theme.StatusGreenText
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedContainer
import com.example.ui.theme.StatusRedText
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.TestConnectionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val savedToken by viewModel.botToken.collectAsStateWithLifecycle()
    val savedChatId by viewModel.chatId.collectAsStateWithLifecycle()
    val testState by viewModel.testConnectionState.collectAsStateWithLifecycle()

    var tokenInput by remember(savedToken) { mutableStateOf(savedToken) }
    var chatIdInput by remember(savedChatId) { mutableStateOf(savedChatId) }
    var isTokenVisible by remember { mutableStateOf(false) }

    val isChatIdValid = chatIdInput.trim().isEmpty() || chatIdInput.trim().matches(Regex("^-?\\d+$"))

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = M3Background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "تنظیمات ربات تلگرام",
                            fontWeight = FontWeight.Medium,
                            fontSize = 19.sp,
                            color = M3OnSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = M3OnSurface
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info Card (Professional Polish styled guide card)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = M3SurfaceContainerHigh),
                    border = BorderStroke(1.dp, M3Outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(M3SecondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = null,
                                    tint = M3Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "راهنمای اتصال به تلگرام",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = M3OnSurface
                            )
                        }
                        Text(
                            text = "۱. در تلگرام به ربات @BotFather پیام داده و با دستور newbot/ یک ربات بسازید تا Bot Token دریافت کنید.\n" +
                                    "۲. در ربات ساخته‌شده خود دکمه Start را بزنید.\n" +
                                    "۳. برای دریافت Chat ID شخصی خود می‌توانید از ربات @userinfobot استفاده کنید (مثال: 123456789).\n" +
                                    "۴. برای کانال یا سوپرگروه، ربات را ادمین کرده و شناسه عددی آن را وارد کنید (مثال: 1001234567890-).",
                            fontSize = 12.sp,
                            lineHeight = 20.sp,
                            color = M3OnSurfaceVariant
                        )
                    }
                }

                // Bot Token Input
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = {
                        tokenInput = it
                        viewModel.resetTestConnectionState()
                    },
                    label = { Text("توکن ربات (Bot Token)") },
                    placeholder = { Text("123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = M3Primary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                            Icon(
                                imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isTokenVisible) "مخفی‌سازی توکن" else "نمایش توکن",
                                tint = M3OnSurfaceVariant
                            )
                        }
                    },
                    visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bot_token_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = M3Primary,
                        unfocusedBorderColor = M3Outline,
                        focusedLabelColor = M3Primary,
                        unfocusedContainerColor = M3Surface,
                        focusedContainerColor = M3Surface
                    )
                )

                // Chat ID Input
                OutlinedTextField(
                    value = chatIdInput,
                    onValueChange = {
                        chatIdInput = it
                        viewModel.resetTestConnectionState()
                    },
                    label = { Text("شناسه عددی چت (Chat ID)") },
                    placeholder = { Text("123456789 یا -1001234567890") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Numbers, contentDescription = null, tint = M3Primary)
                    },
                    isError = !isChatIdValid,
                    supportingText = {
                        if (!isChatIdValid) {
                            Text(
                                text = "شناسه چت باید فقط عددی باشد (مثلاً 123456789 یا 1001234567890-)",
                                color = StatusRed
                            )
                        } else {
                            Text("شناسه کاربر، گروه یا کانال مقصد", color = M3OnSurfaceVariant)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat_id_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = M3Primary,
                        unfocusedBorderColor = M3Outline,
                        focusedLabelColor = M3Primary,
                        unfocusedContainerColor = M3Surface,
                        focusedContainerColor = M3Surface
                    )
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Save Button
                    Button(
                        onClick = {
                            if (tokenInput.isBlank() || chatIdInput.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("لطفاً هم توکن و هم شناسه چت را وارد کنید.")
                                }
                                return@Button
                            }
                            val success = viewModel.saveSettings(tokenInput, chatIdInput)
                            scope.launch {
                                if (success) {
                                    snackbarHostState.showSnackbar("تنظیمات با موفقیت ذخیره شد.")
                                } else {
                                    snackbarHostState.showSnackbar("خطا: شناسه چت باید فقط شامل ارقام عددی باشد.")
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("save_settings_button"),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = M3Primary,
                            contentColor = M3OnPrimary
                        ),
                        enabled = isChatIdValid && tokenInput.isNotBlank() && chatIdInput.isNotBlank()
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ذخیره", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }

                    // Test Connection Button
                    OutlinedButton(
                        onClick = {
                            viewModel.testConnection(tokenInput, chatIdInput)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("test_connection_button"),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, M3Outline),
                        enabled = testState !is TestConnectionState.Loading && isChatIdValid && tokenInput.isNotBlank() && chatIdInput.isNotBlank()
                    ) {
                        if (testState is TestConnectionState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = M3Primary)
                        } else {
                            Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp), tint = M3Primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("بررسی اتصال", color = M3Primary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                }

                // Test Connection Result Feedback
                AnimatedVisibility(visible = testState !is TestConnectionState.Idle) {
                    when (val state = testState) {
                        is TestConnectionState.Success -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(StatusGreenContainer)
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = state.message,
                                        color = StatusGreenText,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        is TestConnectionState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(StatusRedContainer)
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = StatusRed,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = state.message,
                                        color = StatusRedText,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        else -> {}
                    }
                }

                // Security & Privacy Card (PIN Lock)
                AppLockSettingsCard(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState
                )

                // Notification Settings Card
                NotificationSettingsCard()

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun AppLockSettingsCard(
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()

    var showSetPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDisablePinDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_lock_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = M3SurfaceContainerHigh),
        border = BorderStroke(1.dp, M3Outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isAppLockEnabled) StatusGreenContainer else M3SecondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = if (isAppLockEnabled) StatusGreen else M3Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "امنیت و قفل برنامه (PIN)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = M3OnSurface
                        )
                        Text(
                            text = if (isAppLockEnabled) "قفل با رمز عبور فعال است" else "قفل برنامه غیرفعال است",
                            fontSize = 12.sp,
                            color = if (isAppLockEnabled) StatusGreenText else M3OnSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = "با فعال‌سازی قفل، هنگام باز کردن برنامه، مشاهده عکس‌ها و تغییر تنظیمات، ورود رمز عبور الزامی خواهد بود.",
                fontSize = 12.sp,
                lineHeight = 19.sp,
                color = M3OnSurfaceVariant
            )

            if (!isAppLockEnabled) {
                Button(
                    onClick = { showSetPinDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("enable_pin_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = M3Primary,
                        contentColor = M3OnPrimary
                    )
                ) {
                    Text("فعال‌سازی قفل با PIN", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showChangePinDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("change_pin_button"),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, M3Outline)
                    ) {
                        Text("تغییر PIN", fontSize = 13.sp, color = M3OnSurface)
                    }

                    OutlinedButton(
                        onClick = { showDisablePinDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("disable_pin_button"),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.5f))
                    ) {
                        Text("غیرفعال‌سازی", fontSize = 13.sp, color = StatusRed)
                    }
                }
            }
        }
    }

    // Dialog: Set New PIN
    if (showSetPinDialog) {
        var pin1 by remember { mutableStateOf("") }
        var pin2 by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            title = { Text("تنظیم رمز عبور جدید", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("یک رمز عبور ۴ تا ۸ رقمی تعیین کنید:", fontSize = 13.sp, color = M3OnSurfaceVariant)
                    OutlinedTextField(
                        value = pin1,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) pin1 = it },
                        label = { Text("رمز عبور جدید (PIN)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_pin_input")
                    )
                    OutlinedTextField(
                        value = pin2,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) pin2 = it },
                        label = { Text("تکرار رمز عبور") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("confirm_pin_input")
                    )
                    if (err != null) {
                        Text(err.orEmpty(), color = StatusRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pin1.length < 4) {
                            err = "رمز عبور باید حداقل ۴ رقم باشد."
                        } else if (pin1 != pin2) {
                            err = "رمزهای عبور واردشده با یکدیگر مطابقت ندارند."
                        } else {
                            viewModel.enableAppLock(pin1)
                            showSetPinDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("قفل برنامه با موفقیت فعال شد.")
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_set_pin_button")
                ) {
                    Text("تأیید و ذخیره")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSetPinDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Dialog: Change PIN
    if (showChangePinDialog) {
        var currentPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmNewPin by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("تغییر رمز عبور", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) currentPin = it },
                        label = { Text("رمز عبور فعلی") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("current_pin_input")
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) newPin = it },
                        label = { Text("رمز عبور جدید") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_pin_change_input")
                    )
                    OutlinedTextField(
                        value = confirmNewPin,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) confirmNewPin = it },
                        label = { Text("تکرار رمز عبور جدید") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("confirm_new_pin_input")
                    )
                    if (err != null) {
                        Text(err.orEmpty(), color = StatusRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length < 4) {
                            err = "رمز عبور جدید باید حداقل ۴ رقم باشد."
                        } else if (newPin != confirmNewPin) {
                            err = "رمزهای جدید واردشده یکسان نیستند."
                        } else {
                            val success = viewModel.changePin(currentPin, newPin)
                            if (success) {
                                showChangePinDialog = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("رمز عبور با موفقیت تغییر یافت.")
                                }
                            } else {
                                err = "رمز عبور فعلی نادرست است."
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_change_pin_button")
                ) {
                    Text("تغییر رمز")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showChangePinDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Dialog: Disable PIN Lock
    if (showDisablePinDialog) {
        var currentPin by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDisablePinDialog = false },
            title = { Text("غیرفعال‌سازی قفل برنامه", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("برای غیرفعال کردن قفل، رمز عبور فعلی خود را وارد کنید:", fontSize = 13.sp, color = M3OnSurfaceVariant)
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) currentPin = it },
                        label = { Text("رمز عبور فعلی") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("disable_pin_current_input")
                    )
                    if (err != null) {
                        Text(err.orEmpty(), color = StatusRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.disableAppLock(currentPin)
                        if (success) {
                            showDisablePinDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("قفل برنامه غیرفعال شد.")
                            }
                        } else {
                            err = "رمز عبور واردشده نادرست است."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                    modifier = Modifier.testTag("confirm_disable_pin_button")
                ) {
                    Text("غیرفعال‌سازی")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDisablePinDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun NotificationSettingsCard() {
    val context = androidx.compose.ui.platform.LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notification_settings_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = M3SurfaceContainerHigh),
        border = BorderStroke(1.dp, M3Outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
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
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = M3Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "تنظیمات اعلان‌های برنامه",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = M3OnSurface
                    )
                    Text(
                        text = "شفاف و استاندارد در سیستم اندروید",
                        fontSize = 12.sp,
                        color = M3OnSurfaceVariant
                    )
                }
            }

            Text(
                text = "این برنامه هیچ اعلان پنهان یا غیر استانداردی ندارد. می‌توانید کانال‌های اعلان، صدا و نحوه نمایش اعلان‌ها را از طریق تنظیمات رسمی اندروید تنظیم کنید.",
                fontSize = 12.sp,
                lineHeight = 19.sp,
                color = M3OnSurfaceVariant
            )

            OutlinedButton(
                onClick = {
                    try {
                        val intent = android.content.Intent().apply {
                            action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("open_notification_settings_button"),
                shape = CircleShape,
                border = BorderStroke(1.dp, M3Outline)
            ) {
                Text("مدیریت اعلان‌ها در تنظیمات دستگاه", fontSize = 13.sp, color = M3Primary, fontWeight = FontWeight.Medium)
            }
        }
    }
}
