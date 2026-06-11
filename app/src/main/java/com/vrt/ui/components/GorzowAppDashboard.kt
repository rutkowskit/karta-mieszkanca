package com.vrt.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vrt.data.ResidentCard
import com.vrt.ui.ResidentCardViewModel
import com.vrt.ui.TabType
import com.vrt.ui.theme.GorzowGreen
import java.io.File

@Composable
fun GorzowAppDashboard(
    modifier: Modifier = Modifier,
    cards: List<ResidentCard>,
    selectedCard: ResidentCard?,
    viewModel: ResidentCardViewModel
) {
    var activeTab by remember { mutableStateOf(TabType.QR) }
    var showEditForm by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<ResidentCard?>(null) }
    var cardToDelete by remember { mutableStateOf<ResidentCard?>(null) }
    var showZoomedCode by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Extracted component: Header row with brand and add CTA action
            DashboardHeader(
                onAddCardClick = {
                    editingCard = null
                    showEditForm = true
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (cards.isEmpty()) {
                // Extracted component: Clean Empty state container
                EmptyStatePlaceholder(
                    onAddCardClick = {
                        editingCard = null
                        showEditForm = true
                    }
                )
            } else {
                // Extracted component: Carousel view if family or multiples are present
                if (cards.size > 1) {
                    FamilyMemberSelector(
                        cards = cards,
                        selectedCardId = selectedCard?.id,
                        onSelectCard = { card -> viewModel.selectCard(card) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Render active selected Card replica
                selectedCard?.let { activeCard ->
                    CardReplica(
                        card = activeCard,
                        onEditClick = {
                            editingCard = activeCard
                            showEditForm = true
                        },
                        onDeleteClick = {
                            cardToDelete = activeCard
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tab bar for selecting what to show: QR / Barcode / Original card photo
                    SingleScreenTabs(
                        selectedTab = activeTab,
                        onTabSelected = { activeTab = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scannable Frame
                    ScannableContentFrame(
                        card = activeCard,
                        activeTab = activeTab,
                        onClick = { showZoomedCode = true }
                    )
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }

        // Full Screen Add/Edit Form Overlay Composable for seamless user input journeys
        if (showEditForm) {
            CardFormOverlay(
                editingCard = editingCard,
                onDismiss = { showEditForm = false },
                onSave = { firstName, lastName, num, exDate, picPath, scrPath, isPrimary ->
                    viewModel.saveCard(
                        id = editingCard?.id ?: 0,
                        firstName = firstName,
                        lastName = lastName,
                        cardNumber = num,
                        expirationDate = exDate,
                        userPhotoPath = picPath,
                        cardScreenshotPath = scrPath,
                        isPrimary = isPrimary
                    )
                    showEditForm = false
                    Toast.makeText(context, "Zapisano pomyślnie!", Toast.LENGTH_SHORT).show()
                },
                viewModel = viewModel
            )
        }

        // Elegant Pop-up Alert Dialog confirming card deletion
        cardToDelete?.let { card ->
            DeleteConfirmationDialog(
                card = card,
                onConfirm = {
                    viewModel.deleteCard(card)
                    Toast.makeText(context, "Karta została usunięta", Toast.LENGTH_SHORT).show()
                    cardToDelete = null
                },
                onDismiss = { cardToDelete = null }
            )
        }

        // Fully immersive Zoomed Code overlay with 100% active screen brightness
        if (showZoomedCode && selectedCard != null) {
            FullscreenCodeOverlay(
                card = selectedCard,
                activeTab = activeTab,
                onDismiss = { showZoomedCode = false }
            )
        }
    }
}

/**
 * Renders the top branding header of the app and handles the main action CTA to add cards.
 */
@Composable
fun DashboardHeader(
    onAddCardClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "GORZÓW",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = GorzowGreen,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "#StądJestem",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        FilledIconButton(
            onClick = onAddCardClick,
            modifier = Modifier.testTag("add_card_button"),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = GorzowGreen
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Dodaj kartę",
                tint = Color.Black
            )
        }
    }
}

/**
 * Placeholder displayed when there are no entries stored locally yet. Guides users on initial onboarding.
 */
@Composable
fun EmptyStatePlaceholder(
    onAddCardClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ContactPage,
                    contentDescription = null,
                    tint = GorzowGreen,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Brak Karty Mieszkańca",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Uzupełnij swoje dane, aby wyświetlić i zapisać cyfrową Kartę Mieszkańca Gorzowa Wielkopolskiego.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onAddCardClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("initial_add_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GorzowGreen)
                ) {
                    Text(
                        text = "Dodaj pierwszą kartę",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

/**
 * A horizontal selector display allowing seamless user toggle between different resident cards.
 * Extremely helpful for multi-child or familial usage.
 */
@Composable
fun FamilyMemberSelector(
    cards: List<ResidentCard>,
    selectedCardId: Int?,
    onSelectCard: (ResidentCard) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        cards.forEach { card ->
            val isChosen = selectedCardId == card.id
            Surface(
                modifier = Modifier
                    .clickable { onSelectCard(card) }
                    .testTag("member_tab_${card.id}"),
                shape = RoundedCornerShape(20.dp),
                color = if (isChosen) GorzowGreen else MaterialTheme.colorScheme.surfaceVariant,
                border = if (isChosen) null else BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isChosen) Color.Black.copy(alpha = 0.2f)
                                else GorzowGreen.copy(alpha = 0.3f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (card.userPhotoPath != null && File(card.userPhotoPath).exists()) {
                            AsyncImage(
                                model = File(card.userPhotoPath),
                                contentDescription = null,
                                modifier = Modifier.clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isChosen) Color.Black else GorzowGreen
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = card.firstName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isChosen) Color.Black else Color.White
                    )
                }
            }
        }
    }
}

/**
 * Modal Alert informing users before deletion occurs to protect user data from unintended finger slipping.
 */
@Composable
fun DeleteConfirmationDialog(
    card: ResidentCard,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Potwierdzenie usunięcia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Text(
                text = "Czy na pewno chcesz usunąć kartę mieszkańca dla użytkownika ${card.firstName} ${card.lastName}? Operacja jest nieodwracalna.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = GorzowGreen),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("Usuń", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_delete_button")
            ) {
                Text("Anuluj", color = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.testTag("delete_confirmation_dialog")
    )
}
