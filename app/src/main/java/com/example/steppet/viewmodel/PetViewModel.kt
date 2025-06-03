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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * • pet: StateFlow<PetEntity>, das UI abonnieren kann.
 * • feedPet(stepsToday): Versucht, das Pet zu füttern, wenn heute noch Feeds übrig.
 * • canFeed(stepsToday): Gibt true zurück, wenn noch ein Feed heute möglich ist.
 * • remainingFeeds(stepsToday): Anzahl verbleibender Feeds heute.
 */
class PetViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = PetRepository(application)

    /**
     * Exposes das einzelne Pet als StateFlow, damit Compose es collecten kann.
     */
    val pet: StateFlow<PetEntity> = repo
        .petFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            initialValue = PetEntity()
        )

    /**
     * Versucht, das Pet zu füttern, falls heute noch Feed‐Chancen übrig sind.
     * Übergibt die aktuelle Schrittzahl (stepsToday) ans Repository.
     */
    fun feedPet(stepsToday: Int) {
        viewModelScope.launch {
            repo.feedPetIfAllowed(stepsToday)
        }
    }

    /**
     * Gibt zurück, ob aktuell ein weiterer Feed heute möglich ist:
     *   (stepsToday / 1000) > feedsDoneToday ?
     */
    fun canFeed(stepsToday: Int): Boolean {
        val current = pet.value
        val todayString = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        // Wenn lastFeedDate != heute, zählen wir feedsDoneToday als 0
        val feedsDone = if (current.lastFeedDate != todayString) 0 else current.feedsDoneToday
        val maxAllowed = (stepsToday / 1000).coerceIn(0, 10)
        return feedsDone < maxAllowed
    }

    /**
     * Wie viele Feeds sind heute insgesamt noch übrig? (für UI-Label)
     */
    fun remainingFeeds(stepsToday: Int): Int {
        val current = pet.value
        val todayString = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val feedsDone = if (current.lastFeedDate != todayString) 0 else current.feedsDoneToday
        val maxAllowed = (stepsToday / 1000).coerceIn(0, 10)
        return (maxAllowed - feedsDone).coerceAtLeast(0)
    }
}
