package com.example.steppet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import com.example.steppet.data.repository.PetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages pet hunger state via Room.
 */
class PetViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = PetRepository(application)

    var hunger by mutableStateOf(0)
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val saved = repo.getHunger()
            hunger = saved
        }
    }

    fun feedPet() {
        hunger = 100
        viewModelScope.launch(Dispatchers.IO) {
            repo.setHunger(100)
        }
    }

    fun decreaseHunger(by: Int = 1) {
        val new = (hunger - by).coerceAtLeast(0)
        hunger = new
        viewModelScope.launch(Dispatchers.IO) {
            repo.setHunger(new)
        }
    }
}
