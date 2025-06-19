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
import hr.ferit.belic.parking3.viewmodel.RegistrationState
import hr.ferit.belic.parking3.viewmodel.RegistrationViewModel

@Composable
fun RegistrationScreen(
    onRegisterSuccess: () -> Unit,
    onSignInClick: () -> Unit,
    viewModel: RegistrationViewModel = viewModel()
) {
    val userData = viewModel.userData.collectAsState().value
    val registrationState = viewModel.registrationState.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Register",
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
            onClick = { viewModel.register(onRegisterSuccess) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = registrationState != RegistrationState.Loading
        ) {
            Text("Register", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Already registered? Sign in",
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable { onSignInClick() }
                .padding(top = 8.dp)
        )

        when (registrationState) {
            is RegistrationState.Loading -> Text("Registering...", fontSize = 16.sp)
            is RegistrationState.Success -> Text(
                "Registration successful!",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
            is RegistrationState.Error -> Text(
                registrationState.message,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.error
            )
            is RegistrationState.Idle -> {}
        }
    }
}