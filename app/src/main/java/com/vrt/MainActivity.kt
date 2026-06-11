package com.vrt

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.vrt.data.AppDatabase
import com.vrt.data.ResidentCard
import com.vrt.data.ResidentCardRepository
import com.vrt.ui.*
import com.vrt.ui.components.*
import com.vrt.ui.theme.GorzowGreen
import com.vrt.ui.theme.MyApplicationTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { ResidentCardRepository(database.residentCardDao) }
    private val viewModel by viewModels<ResidentCardViewModel> {
        ResidentCardViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Disable automatic color inversion by physical devices (e.g. Xiaomi MIUI Force Dark)
        // to preserve readable black text on the white barcode/QR scanning cards.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.decorView.isForceDarkAllowed = false
        }

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Display bottom brand stamp
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(bottom = 12.dp, top = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Karta Mieszkańca Gorzowa Wlkp. • #StądJestem",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                    }
                ) { innerPadding ->
                    val cards by viewModel.allCards.collectAsState()
                    val selectedCard by viewModel.selectedCard.collectAsState()

                    GorzowAppDashboard(
                        modifier = Modifier.padding(innerPadding),
                        cards = cards,
                        selectedCard = selectedCard,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

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
            // Elegant Gorzów Header Icon
            Spacer(modifier = Modifier.height(16.dp))
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
                    onClick = {
                        editingCard = null
                        showEditForm = true
                    },
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

            Spacer(modifier = Modifier.height(16.dp))

            if (cards.isEmpty()) {
                // Empty state view
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
                                onClick = {
                                    editingCard = null
                                    showEditForm = true
                                },
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
            } else {
                // Cards are available!
                // Carousel/Toggles of other cards (family members) if more than 1 card exists
                if (cards.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        cards.forEach { card ->
                            val isChosen = selectedCard?.id == card.id
                            Surface(
                                modifier = Modifier
                                    .clickable { viewModel.selectCard(card) }
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
                                            .background(if (isChosen) Color.Black.copy(alpha = 0.2f) else GorzowGreen.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (card.userPhotoPath != null) {
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
        if (cardToDelete != null) {
            AlertDialog(
                onDismissRequest = { cardToDelete = null },
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
                        text = "Czy na pewno chcesz usunąć kartę mieszkańca dla użytkownika ${cardToDelete?.firstName} ${cardToDelete?.lastName}? Operacja jest nieodwracalna.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            cardToDelete?.let { card ->
                                viewModel.deleteCard(card)
                                Toast.makeText(context, "Karta została usunięta", Toast.LENGTH_SHORT).show()
                            }
                            cardToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GorzowGreen),
                        modifier = Modifier.testTag("confirm_delete_button")
                    ) {
                        Text("Usuń", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { cardToDelete = null },
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
