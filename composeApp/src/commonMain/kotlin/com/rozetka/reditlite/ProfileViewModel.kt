package com.rozetka.reditlite



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozetka.reditlite.data.SecureStorageManager
import com.rozetka.reditlite.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel(
    private val storageManager: SecureStorageManager,
    private val database: AppDatabase
) : ViewModel() {

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            storageManager.clearTokens()
            database.redditPostDao().clearAll()
            withContext(Dispatchers.Main) {
                onLogoutComplete()
            }
        }
    }
}