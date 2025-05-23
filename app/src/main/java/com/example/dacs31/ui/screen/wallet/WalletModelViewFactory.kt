package com.example.dacs31.ui.screen.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dacs31.data.AuthRepository

class WalletModelViewFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WalletModelView::class.java)) {
            return WalletModelView(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}