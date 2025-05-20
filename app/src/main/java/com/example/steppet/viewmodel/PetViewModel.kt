package com.example.steppet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import com.example.steppet.data.repository.PetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Holds and updates the pet’s hunger state.
 */
class PetViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = PetRepository(application)

    // Hunger 0 (starving) to 100 (full)
    var hunger by mutableStateOf(0)
        private set

    init {
        // Load initial hunger
        viewModelScope.launch(Dispatchers.IO) {
            val saved = repo.getHunger()
            withContext(Dispatchers.Main) {
                hunger = saved
            }
        }
    }

    /** Feed the pet: set hunger back to full. */
    fun feedPet() {
        hunger = 100
        viewModelScope.launch(Dispatchers.IO) {
            repo.setHunger(100)
        }
    }

    /**
     * Optionally call this periodically (e.g. via a timer)
     * to decrease hunger over time:
     */
    fun decreaseHunger(by: Int = 1) {
        val new = (hunger - by).coerceAtLeast(0)
        hunger = new
        viewModelScope.launch(Dispatchers.IO) {
            repo.setHunger(new)
        }
    }
}
