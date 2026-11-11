package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.M3Background
import com.example.ui.theme.M3OnPrimary
import com.example.ui.theme.M3OnSurface
import com.example.ui.theme.M3OnSurfaceVariant
import com.example.ui.theme.M3Primary
import com.example.ui.theme.M3SurfaceContainer
import com.example.ui.theme.StatusRed

@Composable
fun LockScreen(
    onUnlock: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("app_lock_screen"),
        color = M3Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(M3Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "قفل برنامه",
                        tint = M3Primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "برنامه قفل است",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = M3OnSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "جهت حفظ حریم خصوصی، برای ورود رمز عبور خود را وارد کنید",
                    fontSize = 14.sp,
                    color = M3OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // PIN dots indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val maxDigits = maxOf(4, enteredPin.length)
                    for (i in 0 until maxDigits.coerceAtMost(6)) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) M3Primary else M3SurfaceContainer)
                                .border(
                                    width = 1.5.dp,
                                    color = if (isFilled) M3Primary else M3OnSurfaceVariant.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = StatusRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Keypad
            Keypad(
                onDigitClick = { digit ->
                    if (enteredPin.length < 8) {
                        val newPin = enteredPin + digit
                        enteredPin = newPin
                        errorMessage = null
                        if (newPin.length >= 4) {
                            if (onUnlock(newPin)) {
                                errorMessage = null
                            }
                        }
                    }
                },
                onBackspaceClick = {
                    if (enteredPin.isNotEmpty()) {
                        enteredPin = enteredPin.dropLast(1)
                        errorMessage = null
                    }
                },
                onUnlockClick = {
                    if (enteredPin.length < 4) {
                        errorMessage = "رمز عبور حداقل ۴ رقم است"
                    } else {
                        val success = onUnlock(enteredPin)
                        if (!success) {
                            errorMessage = "رمز عبور اشتباه است"
                            enteredPin = ""
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Keypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onUnlockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keypadRows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keypadRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { digit ->
                    KeypadButton(
                        text = digit,
                        onClick = { onDigitClick(digit) },
                        modifier = Modifier.testTag("keypad_digit_$digit")
                    )
                }
            }
        }

        // Bottom row: Backspace, 0, Unlock
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Backspace button
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .clickable { onBackspaceClick() }
                    .testTag("keypad_backspace"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "پاک کردن رقم",
                    tint = M3OnSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Digit 0
            KeypadButton(
                text = "0",
                onClick = { onDigitClick("0") },
                modifier = Modifier.testTag("keypad_digit_0")
            )

            // Unlock / Submit button
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(M3Primary)
                    .clickable { onUnlockClick() }
                    .testTag("keypad_unlock_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ورود",
                    color = M3OnPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(M3SurfaceContainer)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = M3OnSurface
        )
    }
}
