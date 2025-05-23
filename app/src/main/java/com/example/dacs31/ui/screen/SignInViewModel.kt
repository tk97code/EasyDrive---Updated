package com.example.dacs31.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dacs31.data.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignInViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _emailOrPhone = MutableLiveData<TextFieldValue>()
    val emailOrPhone: LiveData<TextFieldValue> = _emailOrPhone

    private val _password = MutableLiveData<TextFieldValue>()
    val password: LiveData<TextFieldValue> = _password

    private val _passwordVisible = MutableLiveData<Boolean>()
    val passwordVisible: LiveData<Boolean> = _passwordVisible

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        _emailOrPhone.value = TextFieldValue()
        _password.value = TextFieldValue()
        _passwordVisible.value = false
        _errorMessage.value = null
        _isLoading.value = false
    }

    fun updateEmailOrPhone(newValue: TextFieldValue) {
        _emailOrPhone.value = newValue
    }

    fun updatePassword(newValue: TextFieldValue) {
        _password.value = newValue
    }

    fun togglePasswordVisibility() {
        _passwordVisible.value = _passwordVisible.value?.not() ?: false
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    fun signIn(onResult: (Result<String>) -> Unit) {
        _isLoading.value = true
        _errorMessage.value = null

        CoroutineScope(Dispatchers.Main).launch {
            val result = authRepository.login(
                emailOrPhone.value?.text ?: "",
                password.value?.text ?: ""
            )
            _isLoading.value = false
            onResult(result)
        }
    }
}

class SignInViewModelFactory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignInViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SignInViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}