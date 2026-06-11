package com.vrt.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vrt.data.ResidentCard
import com.vrt.ui.ResidentCardViewModel
import com.vrt.ui.isValidDate
import com.vrt.ui.theme.GorzowGreen
import java.io.File

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CardFormOverlay(
    editingCard: ResidentCard?,
    onDismiss: () -> Unit,
    onSave: (
        firstName: String,
        lastName: String,
        cardNumber: String,
        expirationDate: String,
        userPhotoPath: String?,
        cardScreenshotPath: String?,
        isPrimary: Boolean
    ) -> Unit,
    viewModel: ResidentCardViewModel
) {
    val context = LocalContext.current
    var firstName by remember { mutableStateOf(editingCard?.firstName ?: "") }
    var lastName by remember { mutableStateOf(editingCard?.lastName ?: "") }
    var cardNumber by remember { mutableStateOf(editingCard?.cardNumber ?: "") }
    var expirationDate by remember { mutableStateOf(editingCard?.expirationDate ?: "") }
    var userPhotoPath by remember { mutableStateOf(editingCard?.userPhotoPath) }
    var cardScreenshotPath by remember { mutableStateOf(editingCard?.cardScreenshotPath) }
    var isPrimary by remember { mutableStateOf(editingCard?.isPrimary ?: (editingCard == null)) }

    // Media system pickers with zero-permissions modern APIs
    val userPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                val path = viewModel.copyImageToLocalStorage(context, it, "user_photo")
                if (path != null) {
                    userPhotoPath = path
                } else {
                    Toast.makeText(context, "Błąd zapisu zdjęcia", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    val cardScreenshotPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                val path = viewModel.copyImageToLocalStorage(context, it, "card_screenshot")
                if (path != null) {
                    cardScreenshotPath = path
                } else {
                    Toast.makeText(context, "Błąd zapisu zrzutu", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    // Form Overlay popup using custom full screen Surface
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("form_overlay"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (editingCard == null) "Nowa Karta" else "Edytuj Dane",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss, modifier = Modifier.testTag("form_close")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Wróć", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Form Inputs
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("Imię") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_first_name"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = GorzowGreen,
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = GorzowGreen,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Nazwisko") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_last_name"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = GorzowGreen,
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = GorzowGreen,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = cardNumber,
                onValueChange = { input ->
                    // Card IDs should only contain numeric values
                    if (input.all { it.isDigit() }) {
                        cardNumber = input
                    }
                },
                label = { Text("Numer Karty Mieszkańca (cyfry)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_card_number"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = GorzowGreen,
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = GorzowGreen,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = expirationDate,
                onValueChange = { input ->
                    // Auto-formatter to helper format dd.MM.yyyy
                    if (input.length <= 10) {
                        val formatted = input.replace("[^\\d\\.]".toRegex(), "")
                        expirationDate = formatted
                    }
                },
                placeholder = { Text("np. 12.05.2027") },
                label = { Text("Data ważności (dd.MM.yyyy)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_expiration_date"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = GorzowGreen,
                    unfocusedLabelColor = Color.Gray,
                    focusedBorderColor = GorzowGreen,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Attachment Row 1: Optional Photo of the user
            Text(
                text = "Zdjęcie użytkownika (opcjonalnie)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(80.dp)
                        .clickable {
                            userPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                ) {
                    if (userPhotoPath != null && File(userPhotoPath!!).exists()) {
                        AsyncImage(
                            model = File(userPhotoPath!!),
                            contentDescription = "Załączone zdjęcie",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Button(
                        onClick = {
                            userPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Rozpocznij wybór", color = Color.White)
                    }
                    if (userPhotoPath != null) {
                        Text(
                            text = "Zdjęcie załączone",
                            color = GorzowGreen,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Attachment Row 2: Optional Original card snapshot
            Text(
                text = "Oryginalna Karta / Zrzut ekranu (opcjonalnie)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(80.dp)
                        .clickable {
                            cardScreenshotPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                ) {
                    if (cardScreenshotPath != null && File(cardScreenshotPath!!).exists()) {
                        AsyncImage(
                            model = File(cardScreenshotPath!!),
                            contentDescription = "Załączona karta originalna",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Button(
                        onClick = {
                            cardScreenshotPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Wybierz zrzut", color = Color.White)
                    }
                    if (cardScreenshotPath != null) {
                        Text(
                            text = "Zrzut załączony",
                            color = GorzowGreen,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Is primary user check switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Główna Karta", style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Ustaw jako moją domyślną kartę", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                }

                Switch(
                    checked = isPrimary,
                    onCheckedChange = { isPrimary = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GorzowGreen,
                        checkedTrackColor = GorzowGreen.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("switch_primary")
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Submit Buttons
            Button(
                onClick = {
                    if (firstName.isBlank() || lastName.isBlank() || cardNumber.isBlank() || expirationDate.isBlank()) {
                        Toast.makeText(context, "Uzupełnij wszystkie wymagane pola!", Toast.LENGTH_SHORT).show()
                    } else if (!isValidDate(expirationDate)) {
                        Toast.makeText(context, "Podaj poprawną datę w formacie dd.MM.yyyy (np. 12.05.2027)!", Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(firstName, lastName, cardNumber, expirationDate, userPhotoPath, cardScreenshotPath, isPrimary)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_card_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GorzowGreen)
            ) {
                Text(
                    text = "Zapisz Kartę",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
