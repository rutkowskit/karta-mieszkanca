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
