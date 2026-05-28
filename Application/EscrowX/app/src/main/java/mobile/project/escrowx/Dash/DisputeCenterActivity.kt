@file:Suppress("SpellCheckingInspection")
package mobile.project.escrowx.dash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class DisputeCenterActivity : ComponentActivity() {
    private val viewModel: DisputeViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                var currentFilter by remember { mutableStateOf(DisputeFilter.ALL) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Dispute Center", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)) },
                            navigationIcon = { IconButton(onClick = { finish() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                            actions = { IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Filled.HelpOutline, null) } }
                        )
                    }
                ) { padding ->
                    Column(modifier = Modifier.padding(padding)) {
                        FilterSection(currentFilter) { currentFilter = it }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(selectedFilter: DisputeFilter, onFilterSelected: (DisputeFilter) -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChipButton("All", selectedFilter == DisputeFilter.ALL) { onFilterSelected(DisputeFilter.ALL) }
        FilterChipButton("Complete", selectedFilter == DisputeFilter.COMPLETE) { onFilterSelected(DisputeFilter.COMPLETE) }
        FilterChipButton("Incomplete", selectedFilter == DisputeFilter.INCOMPLETE) { onFilterSelected(DisputeFilter.INCOMPLETE) }

    }
}

@Composable
private fun FilterChipButton(text: String, active: Boolean, onClick: () -> Unit) {
    // 2. Explicitly define Surface content to fix inference errors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(99.dp),
        color = if (active) Color(0xFF00236F) else Color.White,
        border = BorderStroke(1.dp, if (active) Color.Transparent else Color(0xFFC5C5D3))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (active) Color.White else Color.Black
        )
    }
}