package hr.ferit.belic.parking3.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import hr.ferit.belic.parking3.viewmodel.SignInState
import hr.ferit.belic.parking3.viewmodel.SignInViewModel

@Composable
fun SignInScreen(
    onSignInSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: SignInViewModel = viewModel()
) {
    val userData = viewModel.userData.collectAsState().value
    val signInState = viewModel.signInState.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sign In",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = userData.email,
            onValueChange = { value: String -> viewModel.updateEmail(value) },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = userData.password,
            onValueChange = { value: String -> viewModel.updatePassword(value) },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        Button(
            onClick = { viewModel.signIn(onSignInSuccess) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = signInState != SignInState.Loading
        ) {
            Text("Sign In", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Don't have an account? Register",
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable { onRegisterClick() }
                .padding(top = 8.dp)
        )

        when (signInState) {
            is SignInState.Loading -> Text("Signing in...", fontSize = 16.sp)
            is SignInState.Success -> Text(
                "Sign-in successful!",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
            is SignInState.Error -> Text(
                signInState.message,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.error
            )
            is SignInState.Idle -> {}
        }
    }
}