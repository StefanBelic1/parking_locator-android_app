package hr.ferit.belic.parking3.model

data class ParkingData(
    val hours: Int = 0,
    val minutes: Int = 0,
    val seconds: Int = 0,
    val timeLeftInSeconds: Long = 0L,
    val isTimerRunning: Boolean = false
)

