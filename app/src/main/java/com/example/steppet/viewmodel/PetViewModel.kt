package com.example.steppet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.steppet.data.local.PetEntity
import com.example.steppet.data.repository.PetRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * AndroidViewModel so we can get a Context for the PetRepository
 * without needing a custom Factory in Compose.
 */
class PetViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = PetRepository(application)

    /**
     * Exposes the single Pet as StateFlow so Compose can collect it.
     * Initial PetEntity() uses default values defined in PetEntity.
     */
    val pet: StateFlow<PetEntity> = repo
        .petFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            initialValue = PetEntity()
        )

    /** Feed the pet: reset hunger to 100 and boost health/happiness. */
    fun feedPet() {
        viewModelScope.launch {
            repo.setHunger(100)
            repo.changeHealth(+10)
            repo.changeHappiness(+10)
        }
    }

    /** Decrease hunger by [amount], clamped ≥ 0. */
    fun decreaseHunger(amount: Int = 1) {
        viewModelScope.launch {
            repo.setHunger(pet.value.hungerLevel - amount)
        }
    }

    /** Decrease health by [amount], clamped ≥ 0. */
    fun decreaseHealth(amount: Int = 1) {
        viewModelScope.launch {
            repo.changeHealth(-amount)
        }
    }

    /** Decrease happiness by [amount], clamped ≥ 0. */
    fun decreaseHappiness(amount: Int = 1) {
        viewModelScope.launch {
            repo.changeHappiness(-amount)
        }
    }
}


