package com.example.dacs31.ui.screen

import androidx.lifecycle.ViewModel
import com.example.dacs31.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    suspend fun getCurrentUser() = withContext(Dispatchers.IO) {
        authRepository.getCurrentUser()
    }

    suspend fun getUserRole() = withContext(Dispatchers.IO) {
        authRepository.getUserRole()
    }
}