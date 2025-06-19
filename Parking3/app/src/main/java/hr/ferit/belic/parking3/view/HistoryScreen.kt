package hr.ferit.belic.parking3.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import hr.ferit.belic.parking3.viewmodel.HistoryState
import hr.ferit.belic.parking3.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import hr.ferit.belic.parking3.model.ParkingHistoryItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController, viewModel: HistoryViewModel = viewModel()) {
    val historyState by viewModel.historyState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<ParkingHistoryItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Parking History") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = historyState) {
                is HistoryState.Loading -> {
                    Text(
                        text = "Loading history...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
                is HistoryState.Empty -> {
                    Text(
                        text = "No parking history available",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
                is HistoryState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(state.items) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                showDeleteDialog = item
                                            }
                                        )
                                    },
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = item.address,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Saved on: ${
                                            if (item.date > 0) {
                                                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(item.date))
                                            } else {
                                                "Unknown date"
                                            }
                                        }",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
                is HistoryState.Error -> {
                    LaunchedEffect(state.message) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(state.message)
                        }
                    }
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        showDeleteDialog?.let { itemToDelete ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text("Delete Location")
                },
                text = {
                    Text("Are you sure you want to delete the location at ${itemToDelete.address}?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteHistoryItem(itemToDelete)
                            showDeleteDialog = null
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Location deleted")
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = null }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}