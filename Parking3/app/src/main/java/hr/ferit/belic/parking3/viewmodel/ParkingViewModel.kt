package hr.ferit.belic.parking3.viewmodel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import hr.ferit.belic.parking3.R
import hr.ferit.belic.parking3.model.ParkingData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.os.CountDownTimer

class ParkingViewModel : ViewModel() {
    private val _parkingData = MutableStateFlow(ParkingData())
    val parkingData: StateFlow<ParkingData> = _parkingData.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private var countDownTimer: CountDownTimer? = null

    fun updateHours(hours: Int) {
        _parkingData.value = _parkingData.value.copy(hours = hours)
    }

    fun updateMinutes(minutes: Int) {
        _parkingData.value = _parkingData.value.copy(minutes = minutes)
    }

    fun updateSeconds(seconds: Int) {
        _parkingData.value = _parkingData.value.copy(seconds = seconds)
    }

    fun startTimer(context: Context) {
        val hours = _parkingData.value.hours
        val minutes = _parkingData.value.minutes
        val seconds = _parkingData.value.seconds

        if (hours == 0 && minutes == 0 && seconds == 0) return

        val totalSeconds = (hours * 3600 + minutes * 60 + seconds).toLong()
        _parkingData.value = _parkingData.value.copy(
            timeLeftInSeconds = totalSeconds,
            isTimerRunning = true
        )

        sendNotification(
            context,
            "Parking Timer Started",
            "Timer set for $hours hours, $minutes minutes, $seconds seconds"
        )

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(totalSeconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _parkingData.value = _parkingData.value.copy(
                    timeLeftInSeconds = millisUntilFinished / 1000
                )
            }

            override fun onFinish() {
                _parkingData.value = _parkingData.value.copy(
                    isTimerRunning = false,
                    timeLeftInSeconds = 0
                )
                sendNotification(
                    context,
                    "Parking Timer Expired",
                    "Your parking time has ended!"
                )
            }
        }.start()
    }

    fun cancelTimer() {
        countDownTimer?.cancel()
        _parkingData.value = _parkingData.value.copy(
            isTimerRunning = false,
            timeLeftInSeconds = 0
        )
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                auth.signOut()
                onLogoutSuccess()
            } catch (e: Exception) {

            }
        }
    }

    private fun sendNotification(context: Context, title: String, message: String) {
        val channelId = "parking_timer_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Parking Timer",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for parking timer updates"
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ikona)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(System.currentTimeMillis().toInt(), notification)
            } catch (e: SecurityException) {

            }
        }
    }

    override fun onCleared() {
        countDownTimer?.cancel()
        super.onCleared()
    }
}