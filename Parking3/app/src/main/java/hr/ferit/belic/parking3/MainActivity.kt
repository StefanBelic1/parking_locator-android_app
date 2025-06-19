package hr.ferit.belic.parking3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import hr.ferit.belic.parking3.ui.theme.Parking3Theme
import hr.ferit.belic.parking3.view.HistoryScreen
import hr.ferit.belic.parking3.view.ParkingScreen
import hr.ferit.belic.parking3.view.RegistrationScreen
import hr.ferit.belic.parking3.view.SignInScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Parking3Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val startDestination = if (auth.currentUser != null) "parking" else "sign_in"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("sign_in") {
            SignInScreen(
                onSignInSuccess = {
                    navController.navigate("parking") {
                        popUpTo("sign_in") { inclusive = true }
                    }
                },
                onRegisterClick = { navController.navigate("registration") }
            )
        }
        composable("registration") {
            RegistrationScreen(
                onRegisterSuccess = {
                    navController.navigate("parking") {
                        popUpTo("sign_in") { inclusive = true }
                    }
                },
                onSignInClick = { navController.navigate("sign_in") }
            )
        }
        composable("parking") {
            ParkingScreen(navController = navController)
        }
        composable("history") {
            HistoryScreen(navController = navController)
        }
    }
}