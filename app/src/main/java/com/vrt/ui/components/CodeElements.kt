package com.vrt.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vrt.data.ResidentCard
import com.vrt.ui.BarcodeUtils
import com.vrt.ui.TabType
import com.vrt.ui.theme.GorzowGreen
import java.io.File

@Composable
fun SingleScreenTabs(
    selectedTab: TabType,
    onTabSelected: (TabType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(100.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TabType.values().forEach { tab ->
            val isSelected = selectedTab == tab
            val label = when (tab) {
                TabType.QR -> "Kod QR"
                TabType.BARCODE -> "Kod kreskowy"
                TabType.ORIGINAL -> "Oryginał"
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (isSelected) GorzowGreen else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .testTag("visual_tab_${tab.name}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color.Black else Color.White
                )
            }
        }
    }
}

@Composable
fun ScannableContentFrame(
    card: ResidentCard,
    activeTab: TabType,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clickable { onClick() }
            .testTag("scannable_box"),
        // Force fully white layout on code viewing boxes for contrast and screen reader accessibility
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (activeTab) {
                TabType.QR -> {
                    val qrBitmap = remember(card.cardNumber) {
                        BarcodeUtils.generateQrCodeBitmap(card.cardNumber, 512)
                    }

                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Kod QR dla numeru ${card.cardNumber}",
                            modifier = Modifier
                                .size(220.dp)
                                .padding(8.dp)
                        )
                    } else {
                        CodeGenerationError()
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = card.cardNumber,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        letterSpacing = 2.sp
                    )
                }

                TabType.BARCODE -> {
                    val barcodeBitmap = remember(card.cardNumber) {
                        BarcodeUtils.generateBarcodeBitmap(card.cardNumber, 600, 150)
                    }

                    if (barcodeBitmap != null) {
                        Image(
                            bitmap = barcodeBitmap.asImageBitmap(),
                            contentDescription = "Kod kreskowy dla numeru ${card.cardNumber}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(vertical = 12.dp)
                        )
                    } else {
                        CodeGenerationError()
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = card.cardNumber,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
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
                                .heightIn(max = 300.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // Original Card Placeholder mimicking Google loyalty card additions
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Brak kopii oryginalnej karty",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Zrób lub załącz zrzut ekranu plastikowej karty, klikając ikonę edycji powyżej.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CodeGenerationError() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "Błąd generowania kodu",
            color = Color.Red,
            fontWeight = FontWeight.Bold
        )
    }
}
