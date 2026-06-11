package com.vrt.ui.components

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vrt.data.ResidentCard
import com.vrt.ui.BarcodeUtils
import com.vrt.ui.CardStatus
import com.vrt.ui.TabType
import com.vrt.ui.getCardStatus
import com.vrt.ui.theme.GorzowGreen
import java.io.File

@Composable
fun FullscreenCodeOverlay(
    card: ResidentCard,
    activeTab: TabType,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Force 100% brightness while this zoomed code overlay is displayed
    DisposableEffect(Unit) {
        val window = activity?.window
        val layoutParams = window?.attributes
        val originalBrightness = layoutParams?.screenBrightness ?: -1f
        layoutParams?.screenBrightness = 1.0f // 100% brightness
        window?.attributes = layoutParams

        onDispose {
            layoutParams?.screenBrightness = originalBrightness
            window?.attributes = layoutParams
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable { onDismiss() } // clicking backdrop dismisses it
            .testTag("fullscreen_code_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clickable(enabled = false) {} // prevent click-through dismissal
        ) {
            Text(
                text = "Pokaż do skanowania",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Kliknij gdziekolwiek na tło, aby powrócić",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, GorzowGreen, RoundedCornerShape(28.dp))
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (activeTab) {
                        TabType.QR -> {
                            val qrBitmap = remember(card.cardNumber) {
                                BarcodeUtils.generateQrCodeBitmap(card.cardNumber, 600)
                            }
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "Kod QR",
                                    modifier = Modifier
                                        .size(280.dp)
                                        .padding(8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = card.cardNumber,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                letterSpacing = 2.sp
                            )
                        }
                        TabType.BARCODE -> {
                            val barcodeBitmap = remember(card.cardNumber) {
                                BarcodeUtils.generateBarcodeBitmap(card.cardNumber, 800, 200)
                            }
                            if (barcodeBitmap != null) {
                                Image(
                                    bitmap = barcodeBitmap.asImageBitmap(),
                                    contentDescription = "Kod kreskowy",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .padding(vertical = 12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = card.cardNumber,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                letterSpacing = 3.sp
                            )
                        }
                        TabType.ORIGINAL -> {
                            if (card.cardScreenshotPath != null && File(card.cardScreenshotPath).exists()) {
                                AsyncImage(
                                    model = File(card.cardScreenshotPath),
                                    contentDescription = "Oryginalna karta",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 400.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Column(
                                    modifier = Modifier.padding(vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Brak kopii oryginalnej karty",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${card.firstName} ${card.lastName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    val status = getCardStatus(card.expirationDate)
                    val dateTextColor = when (status) {
                        CardStatus.ACTIVE -> Color.Gray
                        CardStatus.EXPIRING_SOON -> Color(0xFFC47B00) // dark orange for light bg
                        CardStatus.EXPIRED -> Color(0xFFB91C1C) // dark red for light bg
                    }
                    val statusLabel = when (status) {
                        CardStatus.ACTIVE -> "Karta ważna do: ${card.expirationDate}"
                        CardStatus.EXPIRING_SOON -> "Kończy ważność: ${card.expirationDate}"
                        CardStatus.EXPIRED -> "KARTA WYGASŁA: ${card.expirationDate}"
                    }
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = dateTextColor,
                        fontWeight = if (status != CardStatus.ACTIVE) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GorzowGreen),
                modifier = Modifier
                    .width(180.dp)
                    .height(48.dp)
                    .testTag("close_zoom_button")
            ) {
                Text(
                    text = "Zamknij",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}
