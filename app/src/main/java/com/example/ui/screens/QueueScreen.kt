package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.PhotoUploadEntity
import com.example.data.model.UploadStatus
import com.example.ui.theme.M3Background
import com.example.ui.theme.M3OnPrimary
import com.example.ui.theme.M3OnSecondaryContainer
import com.example.ui.theme.M3OnSurface
import com.example.ui.theme.M3OnSurfaceVariant
import com.example.ui.theme.M3Outline
import com.example.ui.theme.M3Primary
import com.example.ui.theme.M3PrimaryContainer
import com.example.ui.theme.M3SecondaryContainer
import com.example.ui.theme.M3Surface
import com.example.ui.theme.M3SurfaceContainerHigh
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberContainer
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainer
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedContainer
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class QueueFilter {
    ALL, PENDING_AND_FAILED, SENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allPhotos by viewModel.allPhotos.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf(QueueFilter.ALL) }
    var showClearSentDialog by remember { mutableStateOf(false) }

    val filteredList = remember(allPhotos, selectedFilter) {
        when (selectedFilter) {
            QueueFilter.ALL -> allPhotos
            QueueFilter.PENDING_AND_FAILED -> allPhotos.filter { it.status == UploadStatus.PENDING || it.status == UploadStatus.FAILED || it.status == UploadStatus.UPLOADING }
            QueueFilter.SENT -> allPhotos.filter { it.status == UploadStatus.SENT }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = M3Background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "صف ارسال عکس‌ها (${allPhotos.size})",
                            fontWeight = FontWeight.Medium,
                            fontSize = 19.sp,
                            color = M3OnSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("queue_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = M3OnSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showClearSentDialog = true },
                            modifier = Modifier.testTag("clear_sent_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "پاکسازی عکس‌های ارسال شده",
                                tint = M3OnSurfaceVariant
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
            ) {
                // Filter Tabs (Styled with Professional Polish chips)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == QueueFilter.ALL,
                            onClick = { selectedFilter = QueueFilter.ALL },
                            label = { Text("همه (${allPhotos.size})", fontSize = 12.sp) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = M3SecondaryContainer,
                                selectedLabelColor = M3OnSecondaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == QueueFilter.ALL,
                                borderColor = M3Outline,
                                selectedBorderColor = M3Primary
                            )
                        )
                    }
                    item {
                        val pendingCount = allPhotos.count { it.status != UploadStatus.SENT }
                        FilterChip(
                            selected = selectedFilter == QueueFilter.PENDING_AND_FAILED,
                            onClick = { selectedFilter = QueueFilter.PENDING_AND_FAILED },
                            label = { Text("در انتظار و خطا ($pendingCount)", fontSize = 12.sp) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = M3SecondaryContainer,
                                selectedLabelColor = M3OnSecondaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == QueueFilter.PENDING_AND_FAILED,
                                borderColor = M3Outline,
                                selectedBorderColor = M3Primary
                            )
                        )
                    }
                    item {
                        val sentCount = allPhotos.count { it.status == UploadStatus.SENT }
                        FilterChip(
                            selected = selectedFilter == QueueFilter.SENT,
                            onClick = { selectedFilter = QueueFilter.SENT },
                            label = { Text("ارسال شده ($sentCount)", fontSize = 12.sp) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = M3SecondaryContainer,
                                selectedLabelColor = M3OnSecondaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == QueueFilter.SENT,
                                borderColor = M3Outline,
                                selectedBorderColor = M3Primary
                            )
                        )
                    }
                }

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = M3OnSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "هیچ عکسی در این بخش نیست",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = M3OnSurface
                            )
                            Text(
                                text = "با دوربین گوشی عکس جدیدی بگیرید تا به صورت خودکار شناسایی و ارسال شود.",
                                fontSize = 13.sp,
                                color = M3OnSurfaceVariant,
                                lineHeight = 19.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredList, key = { it.id }) { photo ->
                            PhotoQueueItem(
                                photo = photo,
                                onRetry = { viewModel.retryPhoto(photo.id, context) },
                                onDelete = { viewModel.deletePhoto(photo.id) }
                            )
                        }
                    }
                }
            }
        }

        // Clear sent confirmation dialog
        if (showClearSentDialog) {
            AlertDialog(
                onDismissRequest = { showClearSentDialog = false },
                shape = RoundedCornerShape(28.dp),
                containerColor = M3Surface,
                title = { Text("پاکسازی موارد ارسال شده", fontWeight = FontWeight.Bold, color = M3OnSurface) },
                text = { Text("آیا مایلید تاریخچه عکس‌های با موفقیت ارسال شده را از صف حذف کنید؟ (فایل‌های عکس اصلی در گوشی شما باقی می‌مانند)", color = M3OnSurfaceVariant) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearSentPhotos()
                            showClearSentDialog = false
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = M3Primary)
                    ) {
                        Text("پاکسازی")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearSentDialog = false }) {
                        Text("انصراف", color = M3Primary)
                    }
                }
            )
        }
    }
}

@Composable
fun PhotoQueueItem(
    photo: PhotoUploadEntity,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val formattedDate = remember(photo.creationTimestamp) {
        SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date(photo.creationTimestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("queue_item_${photo.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = M3Surface),
        border = BorderStroke(1.dp, M3Outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(Uri.parse(photo.uriString))
                    .crossfade(true)
                    .build(),
                contentDescription = photo.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(M3SurfaceContainerHigh)
            )

            // Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = photo.fileName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = M3OnSurface
                )

                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = M3OnSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusBadge(status = photo.status)
                    if (photo.retryCount > 0) {
                        Text(
                            text = "تلاش: ${photo.retryCount}",
                            fontSize = 11.sp,
                            color = M3OnSurfaceVariant
                        )
                    }
                }

                if (!photo.lastError.isNullOrBlank()) {
                    Text(
                        text = "خطا: ${photo.lastError}",
                        fontSize = 11.sp,
                        color = StatusRed,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (photo.status == UploadStatus.PENDING || photo.status == UploadStatus.FAILED) {
                    Button(
                        onClick = onRetry,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = M3Primary,
                            contentColor = M3OnPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تلاش", fontSize = 11.sp)
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = M3OnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: UploadStatus) {
    val (bg, fg, text) = when (status) {
        UploadStatus.PENDING -> Triple(StatusAmberContainer, StatusAmber, "در انتظار")
        UploadStatus.UPLOADING -> Triple(M3PrimaryContainer, M3Primary, "در حال ارسال...")
        UploadStatus.SENT -> Triple(StatusGreenContainer, StatusGreen, "ارسال شد")
        UploadStatus.FAILED -> Triple(StatusRedContainer, StatusRed, "ناموفق")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
