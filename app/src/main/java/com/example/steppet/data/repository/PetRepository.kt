package com.example.steppet.data.repository

import android.content.Context
import com.example.steppet.data.local.AppDatabase
import com.example.steppet.data.local.PetEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * PetRepository verwaltet sowohl den lokalen Room-Datensatz als
 * auch die Synchronisation in die Cloud (Firestore) über CloudRepository.
 */
class PetRepository(context: Context) {

    // DAO für Room
    private val petDao = AppDatabase.getInstance(context).petDao()

    // CloudRepository kümmert sich um Firestore-Aufrufe
    private val cloudRepo = CloudRepository()

    // Auth-Instanz, um die UID des aktuell eingeloggten Users zu bekommen
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val DEFAULT_ID = 0L
        private const val DEFAULT_HUNGER = 100
        private const val DEFAULT_HEALTH = 100
        private const val DEFAULT_HAPPINESS = 100
    }

    /**
     * Liefert einen Flow, der lokal immer den Pet-State zurückliefert.
     * Wenn in Room noch kein Datensatz existiert, wird ein Default-PetEntity zurückgegeben.
     */
    fun petFlow(): Flow<PetEntity> =
        petDao.getPetFlow(DEFAULT_ID)
            .map { dbEntity ->
                dbEntity ?: PetEntity(
                    id = DEFAULT_ID,
                    name = "Your Pet",
                    hungerLevel = DEFAULT_HUNGER,
                    health = DEFAULT_HEALTH,
                    happiness = DEFAULT_HAPPINESS
                )
            }

    /**
     * Lädt den Pet-Datensatz einmalig aus Room oder erstellt ein Default-PetEntity, falls keiner existiert.
     */
    private suspend fun loadPet(): PetEntity =
        petDao.getPetOnce(DEFAULT_ID)
            ?: PetEntity(
                id = DEFAULT_ID,
                name = "Your Pet",
                hungerLevel = DEFAULT_HUNGER,
                health = DEFAULT_HEALTH,
                happiness = DEFAULT_HAPPINESS
            )

    /**
     * Führt ein Upsert in Room aus und synchronisiert zuletzt in Firestore.
     *
     * @param pet Der vollständige PetEntity-Zustand, der gespeichert werden soll.
     */
    private suspend fun upsertPet(pet: PetEntity) {
        // 1) Lokal in Room upserten
        petDao.upsert(pet)

        // 2) Falls eingeloggt, in Firestore speichern
        if (auth.currentUser != null) {
            // Firestore-Aufruf darf nicht im Main-Thread laufen:
            withContext(Dispatchers.IO) {
                cloudRepo.savePetToCloud(pet)
            }
        }

        // 3) Prüfen, ob der Pet sterben soll (health==0 oder happiness==0)
        checkCritical(pet)
    }

    /**
     * Wenn health == 0 oder happiness == 0, lösche Pet sowohl in Room als auch in Firestore.
     */
    private suspend fun checkCritical(pet: PetEntity) {
        if (pet.health == 0 || pet.happiness == 0) {
            // 1) Lokal entfernen
            petDao.delete(pet)

            // 2) In Firestore kann man z.B. den Pet auf „leeres Dokument“ setzen
            //    oder alternativ das Dokument löschen. Hier nutzen wir savePetToCloud mit 0-Werten.
            if (auth.currentUser != null) {
                withContext(Dispatchers.IO) {
                    cloudRepo.savePetToCloud(
                        PetEntity(
                            id = DEFAULT_ID,
                            name = "",
                            hungerLevel = 0,
                            health = 0,
                            happiness = 0
                        )
                    )
                }
            }
        }
    }

    /**
     * Setzt den Hunger-Wert auf `level` (automatisch 0–100 gec­lamped). Danach in Room+Cloud speichern.
     */
    suspend fun setHunger(level: Int) {
        val pet = loadPet().copy(hungerLevel = level.coerceIn(0, 100))
        upsertPet(pet)
    }

    /**
     * Ändert den Health-Wert um `delta` (kann positiv oder negativ sein). Danach in Room+Cloud speichern.
     */
    suspend fun changeHealth(delta: Int) {
        val old = loadPet()
        val pet = old.copy(health = (old.health + delta).coerceIn(0, 100))
        upsertPet(pet)
    }

    /**
     * Ändert den Happiness-Wert um `delta` (kann positiv oder negativ sein). Danach in Room+Cloud speichern.
     */
    suspend fun changeHappiness(delta: Int) {
        val old = loadPet()
        val pet = old.copy(happiness = (old.happiness + delta).coerceIn(0, 100))
        upsertPet(pet)
    }

    /**
     * Gibt den aktuell gespeicherten Hunger-Wert zurück oder DEFAULT_HUNGER, falls kein Eintrag existiert.
     * (Benötigt u. a. der PetDecayWorker.)
     */
    suspend fun getHunger(): Int =
        petDao.getPetOnce(DEFAULT_ID)?.hungerLevel ?: DEFAULT_HUNGER

    /**
     * Synchronisiere unmittelbar nach Login den Pet-State aus Firestore nach Room.
     *
     * → Falls in Firestore bereits ein Dokument unter users/{uid}/petState/latest existiert,
     *   wird dieses in Room upser­tet.
     * → Falls noch kein Dokument existiert, wird ein Default-PetEntity in Room geschrieben.
     *
     * Diese Methode sollte man direkt nach erfolgreichem Login (z. B. in MainActivity) aufrufen.
     */
    suspend fun syncPetFromCloud() {
        // Nur, wenn eingeloggt
        if (auth.currentUser != null) {
            // Lade aus Firestore (kann null sein, falls noch kein Datensatz)
            val cloudPet = cloudRepo.loadPetFromCloud()
            val petToSave = cloudPet ?: PetEntity(
                id = DEFAULT_ID,
                name = "Your Pet",
                hungerLevel = DEFAULT_HUNGER,
                health = DEFAULT_HEALTH,
                happiness = DEFAULT_HAPPINESS
            )
            // Room upserten
            petDao.upsert(petToSave)
        }
    }
}








