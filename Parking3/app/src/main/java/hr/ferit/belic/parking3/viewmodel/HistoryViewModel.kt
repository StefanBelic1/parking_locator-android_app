package hr.ferit.belic.parking3.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import hr.ferit.belic.parking3.model.ParkingHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

class HistoryViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _historyState = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val historyState: StateFlow<HistoryState> = _historyState.asStateFlow()

    init {
        fetchHistory()
    }


    private fun fetchHistory() {
        viewModelScope.launch {
            _historyState.value = HistoryState.Loading
            try {
                val snapshot = db.collection("parkingHistory")
                    .get()
                    .await()
                val historyItems = snapshot.documents.mapNotNull { doc ->
                    val address = doc.getString("adress") ?: return@mapNotNull null
                    val timestamp = doc.getTimestamp("date") ?: return@mapNotNull null
                    ParkingHistoryItem(doc.id,address, timestamp.toDate().time)
                }
                _historyState.value = if (historyItems.isEmpty()) {
                    HistoryState.Empty
                } else {
                    HistoryState.Success(historyItems)
                }
            } catch (e: Exception) {
                _historyState.value = HistoryState.Error(e.message ?: "Failed to load history")
            }
        }
    }

    fun deleteHistoryItem(item: ParkingHistoryItem) {
        viewModelScope.launch {
            try {
                db.collection("parkingHistory").document(item.id).delete().await()
                Log.d("HistoryViewModel", "Deleted document ${item.id} for address: ${item.address}")
                fetchHistory()
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error deleting history item: ${e.message}", e)
                _historyState.value = HistoryState.Error("Failed to delete location: ${e.message}")
            }
        }
    }
}

sealed class HistoryState {
    object Loading : HistoryState()
    object Empty : HistoryState()
    data class Success(val items: List<ParkingHistoryItem>) : HistoryState()
    data class Error(val message: String) : HistoryState()
}