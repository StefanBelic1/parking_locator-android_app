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

class SignInViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _userData = MutableStateFlow(UserData())
    val userData: StateFlow<UserData> = _userData.asStateFlow()
    private val _signInState = MutableStateFlow<SignInState>(SignInState.Idle)
    val signInState: StateFlow<SignInState> = _signInState.asStateFlow()

    fun updateEmail(email: String) {
        _userData.value = _userData.value.copy(email = email)
    }

    fun updatePassword(password: String) {
        _userData.value = _userData.value.copy(password = password)
    }

    fun signIn(onSignInSuccess: () -> Unit) {
        viewModelScope.launch {
            _signInState.value = SignInState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(
                    _userData.value.email,
                    _userData.value.password
                ).await()
                _signInState.value = SignInState.Success(result.user?.uid ?: "")
                onSignInSuccess()
            } catch (e: Exception) {
                _signInState.value = SignInState.Error(e.message ?: "Sign-in failed")
            }
        }
    }
}

sealed class SignInState {
    object Idle : SignInState()
    object Loading : SignInState()
    data class Success(val userId: String) : SignInState()
    data class Error(val message: String) : SignInState()
}