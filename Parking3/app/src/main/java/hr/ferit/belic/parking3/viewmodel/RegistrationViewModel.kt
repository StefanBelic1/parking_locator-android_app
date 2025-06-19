package hr.ferit.belic.parking3.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import hr.ferit.belic.parking3.model.UserData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegistrationViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _userData = MutableStateFlow(UserData())
    val userData: StateFlow<UserData> = _userData.asStateFlow()
    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState.asStateFlow()

    fun updateEmail(email: String) {
        _userData.value = _userData.value.copy(email = email)
    }

    fun updatePassword(password: String) {
        _userData.value = _userData.value.copy(password = password)
    }

    fun register(onRegisterSuccess: () -> Unit) {
        viewModelScope.launch {
            _registrationState.value = RegistrationState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(
                    _userData.value.email,
                    _userData.value.password
                ).await()
                _registrationState.value = RegistrationState.Success(result.user?.uid ?: "")
                onRegisterSuccess()
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error(e.message ?: "Registration failed")
            }
        }
    }
}

sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()
    data class Success(val userId: String) : RegistrationState()
    data class Error(val message: String) : RegistrationState()
}