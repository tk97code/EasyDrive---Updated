package com.example.dacs31.ui.screen

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dacs31.data.AuthRepository
import kotlinx.coroutines.launch

class SignUpViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _name = MutableLiveData<TextFieldValue>()
    val name: LiveData<TextFieldValue> = _name

    private val _email = MutableLiveData<TextFieldValue>()
    val email: LiveData<TextFieldValue> = _email

    private val _password = MutableLiveData<TextFieldValue>()
    val password: LiveData<TextFieldValue> = _password

    private val _passwordVisible = MutableLiveData<Boolean>()
    val passwordVisible: LiveData<Boolean> = _passwordVisible

    private val _gender = MutableLiveData<String>()
    val gender: LiveData<String> = _gender

    private val _selectedRole = MutableLiveData<String>()
    val selectedRole: LiveData<String> = _selectedRole

    private val _isTermsAccepted = MutableLiveData<Boolean>()
    val isTermsAccepted: LiveData<Boolean> = _isTermsAccepted

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        _name.value = TextFieldValue()
        _email.value = TextFieldValue()
        _password.value = TextFieldValue()
        _passwordVisible.value = false
        _gender.value = ""
        _selectedRole.value = "Customer"
        _isTermsAccepted.value = false
        _errorMessage.value = null
        _isLoading.value = false
    }

    fun updateName(newValue: TextFieldValue) {
        _name.value = newValue
    }

    fun updateEmail(newValue: TextFieldValue) {
        _email.value = newValue
    }

    fun updatePassword(newValue: TextFieldValue) {
        _password.value = newValue
    }

    fun togglePasswordVisibility() {
        _passwordVisible.value = _passwordVisible.value?.not() ?: false
    }

    fun updateGender(newValue: String) {
        _gender.value = newValue
    }

    fun updateRole(newValue: String) {
        _selectedRole.value = newValue
    }

    fun updateTermsAccepted(newValue: Boolean) {
        _isTermsAccepted.value = newValue
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    fun signUp(onResult: (Result<String>) -> Unit) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = authRepository.register(
                email = _email.value?.text ?: "",
                password = _password.value?.text ?: "",
                fullName = _name.value?.text ?: "",
                role = _selectedRole.value ?: "Customer"
            )
            _isLoading.value = false
            if (result.isFailure) {
                val error = result.exceptionOrNull()
                _errorMessage.value = when {
                    error?.message?.contains("The email address is already in use") == true ->
                        "Email này đã được sử dụng. Vui lòng dùng email khác hoặc đăng nhập."
                    error?.message?.contains("CONFIGURATION_NOT_FOUND") == true ->
                        "Lỗi cấu hình reCAPTCHA. Vui lòng kiểm tra kết nối hoặc thử lại sau."
                    else -> error?.message ?: "Đăng ký thất bại. Vui lòng thử lại."
                }
            }
            onResult(result)
        }
    }
}

class SignUpViewModelFactory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignUpViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SignUpViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}