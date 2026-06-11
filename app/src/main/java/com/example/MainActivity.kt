package com.example

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppDatabase
import com.example.data.ResidentCard
import com.example.data.ResidentCardRepository
import com.example.ui.*
import com.example.ui.theme.GorzowGreen
import com.example.ui.theme.GorzowGreenDark
import com.example.ui.theme.MyApplicationTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class TabType {
    QR, BARCODE, ORIGINAL
}

enum class CardStatus {
    ACTIVE, EXPIRING_SOON, EXPIRED
}

fun getCardStatus(expirationDateStr: String): CardStatus {
    val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return try {
        val expirationDate = format.parse(expirationDateStr) ?: return CardStatus.EXPIRED
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = calendar.time
        
        if (expirationDate.before(today)) {
            CardStatus.EXPIRED
        } else {
            calendar.time = today
            calendar.add(Calendar.DAY_OF_YEAR, 10)
            val tenDaysFromNow = calendar.time
            
            if (expirationDate.before(tenDaysFromNow) || expirationDate == tenDaysFromNow) {
                CardStatus.EXPIRING_SOON
            } else {
                CardStatus.ACTIVE
            }
        }
    } catch (e: Exception) {
        CardStatus.ACTIVE
    }
}

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

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
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
    val activity = context as? Activity

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

@Composable
fun CardReplica(
    card: ResidentCard,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val status = getCardStatus(card.expirationDate)
    val cardBgColor = when (status) {
        CardStatus.ACTIVE -> GorzowGreenDark
        CardStatus.EXPIRING_SOON -> Color(0xFFD97706) // Modern amber/orange
        CardStatus.EXPIRED -> Color(0xFFB91C1C) // Modern red
    }
    val badgeBgColor = when (status) {
        CardStatus.ACTIVE -> Color(0xFF1E560C) // deep active green
        CardStatus.EXPIRING_SOON -> Color(0xFF78350F) // deep amber/orange
        CardStatus.EXPIRED -> Color(0xFF7F1D1D) // deep red
    }
    val dotColor = when (status) {
        CardStatus.ACTIVE -> Color(0xFF2EFE59) // vibrant green
        CardStatus.EXPIRING_SOON -> Color(0xFFFBBF24) // vibrant orange/amber
        CardStatus.EXPIRED -> Color(0xFFFCA5A5) // bright desaturated red
    }
    val statusText = when (status) {
        CardStatus.ACTIVE -> "Aktywna"
        CardStatus.EXPIRING_SOON -> "Kończy ważność"
        CardStatus.EXPIRED -> "Wygasła"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .testTag("card_replica_box"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: Card Type & Active state badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = badgeBgColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(36.dp).testTag("edit_card_action")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edytuj kartę",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp).testTag("delete_card_action")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Usuń kartę",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body: Photo & Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Portrait circle frame
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (card.userPhotoPath != null && File(card.userPhotoPath).exists()) {
                        AsyncImage(
                            model = File(card.userPhotoPath),
                            contentDescription = "Zdjęcie użytkownika",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Domyślna sylwetka",
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Text(
                        text = "${card.firstName} ${card.lastName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Karta Mieszkańca",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Expiration Date & Resident Identification Number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Data ważności",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = card.expirationDate,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Numer ID",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = card.cardNumber,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

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
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
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
                    } else if (expirationDate.length < 10) {
                        Toast.makeText(context, "Podaj pełną datę np. 12.05.2027", Toast.LENGTH_SHORT).show()
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
