package com.example.steppet.data.repository

import android.content.Context
import com.example.steppet.data.cloud.CloudRepository
import com.example.steppet.data.local.AppDatabase
import com.example.steppet.data.local.PetEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * PetRepository verwaltet den lokalen Room‐Datensatz und synchronisiert Änderungen in die Cloud (Firestore).
 *
 * • petFlow(): Flow, der das Pet (ID = 0) aus Room liefert oder einen Default‐Pet erstellt.
 * • loadPet(): Liest einmalig das Pet (oder erzeugt Default).
 * • upsertPet(), checkCritical(): Speichern in Room, kritische Fälle (Pet tot) behandeln, Cloud‐Sync.
 * • feedPetIfAllowed(stepsToday): Füttern mit +10 Hunger/Health/Happiness, bis max 10×/Tag (1 000 Schritte pro Feed).
 * • setHunger(), changeHealth(), changeHappiness(): Direkte Änderungen an den Statuswerten.
 * • getHunger(): Aktuellen Hunger aus Room.
 * • syncPetFromCloud(): Pet synchronisieren zwischen Cloud und lokal.
 *
 * Außerdem wird in SharedPreferences ("decay_prefs" → "last_decay_time") der letzte Decay‐Zeitpunkt gespeichert,
 * damit PetDecayWorker Hunger linear über 24 h (100 → 0) reduzieren kann.
 */
class PetRepository(context: Context) {

    // DAO für Room
    private val petDao = AppDatabase.getInstance(context).petDao()

    // FirebaseAuth, um die UID des aktuellen Users zu bekommen
    private val auth = FirebaseAuth.getInstance()

    // SharedPreferences für Decay‐Timestamp
    private val prefs = context.getSharedPreferences("decay_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val DEFAULT_ID = 0L
        private const val DEFAULT_HUNGER = 100
        private const val DEFAULT_HEALTH = 100
        private const val DEFAULT_HAPPINESS = 100

        // 24 Stunden in Millisekunden (1 440 Minuten)
        private const val MILLIS_IN_24H = 24 * 60 * 60 * 1000L
    }

    /**
     * Liefert einen Flow des einzelnen PetEntity (ID = 0).
     *
     * Wenn in Room noch kein Eintrag existiert, wird ein Default‐PetEntity mit
     * hungerLevel=100, health=100, happiness=100, feedsDoneToday=0, lastFeedDate=heute erstellt.
     * Gleichzeitig setzen wir in SharedPreferences ("last_decay_time") den aktuellen Timestamp,
     * damit der erste PetDecayWorker nicht sofort Hunger reduziert.
     */
    fun petFlow(): Flow<PetEntity> =
        petDao.getPetFlow(DEFAULT_ID).map { dbEntity ->
            if (dbEntity != null) {
                dbEntity
            } else {
                val todayString = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                val initialPet = PetEntity(
                    id = DEFAULT_ID,
                    name = "Your Pet",
                    hungerLevel = DEFAULT_HUNGER,
                    health = DEFAULT_HEALTH,
                    happiness = DEFAULT_HAPPINESS,
                    feedsDoneToday = 0,
                    lastFeedDate = todayString
                )
                // Erster Decay‐Zeitpunkt: jetzt
                prefs.edit().putLong("last_decay_time", System.currentTimeMillis()).apply()
                initialPet
            }
        }

    /**
     * Liest Pet einmalig aus Room (oder erzeugt einen Default‐PetEntity).
     */
    private suspend fun loadPet(): PetEntity =
        petDao.getPetOnce(DEFAULT_ID) ?: run {
            val todayString = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            PetEntity(
                id = DEFAULT_ID,
                name = "Your Pet",
                hungerLevel = DEFAULT_HUNGER,
                health = DEFAULT_HEALTH,
                happiness = DEFAULT_HAPPINESS,
                feedsDoneToday = 0,
                lastFeedDate = todayString
            )
        }

    /**
     * Schreibt das gegebene PetEntity in Room und in Firestore.
     *
     * • upsert in Room
     * • checkCritical: falls health ≤ 0 oder happiness ≤ 0, Pet löschen (Room) und leeres Pet in Cloud speichern
     * • Falls Pet noch lebt, save in Cloud (CloudRepository.savePetToCloud)
     */
    private suspend fun upsertPet(pet: PetEntity) {
        petDao.upsert(pet)
        checkCritical(pet)
        if (pet.health > 0 && pet.happiness > 0) {
            CloudRepository.savePetToCloud(pet)
        }
    }

    /**
     * Wenn health = 0 oder happiness = 0 → lösche Pet in Room und schreibe ein leeres PetEntity in Cloud.
     */
    private suspend fun checkCritical(pet: PetEntity) {
        if (pet.health == 0 || pet.happiness == 0) {
            petDao.delete(pet)
            val emptyPet = PetEntity(
                id = DEFAULT_ID,
                name = "",
                hungerLevel = 0,
                health = 0,
                happiness = 0,
                feedsDoneToday = 0,
                lastFeedDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            )
            CloudRepository.savePetToCloud(emptyPet)
        }
    }

    /**
     * Versucht, das Pet einmal zu füttern, falls heute noch Feed‐Chancen übrig sind.
     *
     * • Tagesreset: Wenn lastFeedDate != heute → feedsDoneToday = 0
     * • maxFeedsAllowed = (stepsToday / 1000).coerceIn(0, 10)
     *   (1 000 Schritte → 1 Feed, 2 000 Schritte → 2 Feeds … bis maximal 10 Feeds bei 10 000 Schritten)
     * • Pro Feed: hungerLevel += 10, health += 10, happiness += 10 (jeweils coerceAtMost(100))
     * • feedsDoneToday++, lastFeedDate = heute
     * • Reset des Decay‐Timers: SharedPreferences("last_decay_time") = jetzt
     */
    suspend fun feedPetIfAllowed(stepsToday: Int) = withContext(Dispatchers.IO) {
        val pet = loadPet()
        val todayString = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

        // 1) Tagesreset, falls ein neuer Tag
        val resetPet = if (pet.lastFeedDate != todayString) {
            pet.copy(
                feedsDoneToday = 0,
                lastFeedDate = todayString
            )
        } else {
            pet
        }

        // 2) Wie viele Feeds heute maximal erlaubt? (stepsToday / 1000), max = 10
        val maxFeedsAllowed = (stepsToday / 1000).coerceIn(0, 10)
        if (resetPet.feedsDoneToday >= maxFeedsAllowed) {
            // Keine Feeds mehr übrig → Abbrechen
            return@withContext
        }

        // 3) Fütterung durchführen: hunger +10, health +10, happiness +10
        val newHunger = (resetPet.hungerLevel + 10).coerceAtMost(100)
        val newHealth = (resetPet.health + 10).coerceAtMost(100)
        val newHappiness = (resetPet.happiness + 10).coerceAtMost(100)
        val newFeedsCount = resetPet.feedsDoneToday + 1

        val updated = resetPet.copy(
            hungerLevel = newHunger,
            health = newHealth,
            happiness = newHappiness,
            feedsDoneToday = newFeedsCount,
            lastFeedDate = todayString
        )

        // 4) Speichern in Room + Cloud
        upsertPet(updated)

        // 5) Reset des Decay‐Timers: Hunger decayed ab jetzt wieder über 24 h
        prefs.edit().putLong("last_decay_time", System.currentTimeMillis()).apply()
    }

    /**
     * Hunger direkt setzen (0–100), anschließend updaten.
     */
    suspend fun setHunger(level: Int) {
        val pet = loadPet().copy(
            hungerLevel = level.coerceIn(0, 100)
        )
        upsertPet(pet)
    }

    /**
     * Health um delta ändern (0–100), anschließend updaten.
     */
    suspend fun changeHealth(delta: Int) {
        val old = loadPet()
        val pet = old.copy(
            health = (old.health + delta).coerceIn(0, 100)
        )
        upsertPet(pet)
    }

    /**
     * Happiness um delta ändern (0–100), anschließend updaten.
     */
    suspend fun changeHappiness(delta: Int) {
        val old = loadPet()
        val pet = old.copy(
            happiness = (old.happiness + delta).coerceIn(0, 100)
        )
        upsertPet(pet)
    }

    /**
     * Gibt die aktuelle Hunger‐Stufe zurück (oder DEFAULT_HUNGER, falls kein Datensatz).
     * Wird beispielsweise vom DecayWorker genutzt.
     */
    suspend fun getHunger(): Int =
        petDao.getPetOnce(DEFAULT_ID)?.hungerLevel ?: DEFAULT_HUNGER

    /**
     * Synchronisiert lokalen Pet‐Zustand mit dem in Firestore:
     * • Wenn in Cloud unter users/{uid}/petState/latest schon ein Dokument existiert,
     *   lade es aus und schreibe es lokal in Room.
     * • Falls kein Dokument existiert, lege einen Default‐Pet an und speichere ihn in Cloud.
     */
    suspend fun syncPetFromCloud() {
        val uid = auth.currentUser?.uid ?: return

        // 1) Aus Cloud laden (kann null sein, falls noch nichts dort steht)
        val cloudPet = CloudRepository.getPetFromCloud()

        // 2) Wenn nichts da war, erstelle Default‐Pet und speichere in Cloud
        val petToSave: PetEntity = cloudPet ?: PetEntity(
            id = DEFAULT_ID,
            name = "Your Pet",
            hungerLevel = DEFAULT_HUNGER,
            health = DEFAULT_HEALTH,
            happiness = DEFAULT_HAPPINESS,
            feedsDoneToday = 0,
            lastFeedDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        ).also { defaultPet ->
            CloudRepository.savePetToCloud(defaultPet)
        }

        // 3) Aktualisiere Room mit Pet aus Cloud (oder dem Default‐Pet)
        petDao.upsert(petToSave)
    }
}
