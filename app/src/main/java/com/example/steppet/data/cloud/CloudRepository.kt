package com.example.steppet.data.repository

import com.example.steppet.data.local.PetEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Einfache Hilfsklasse, die alle Firestore‐Operationen bündelt.
 *
 * Struktur in Firestore:
 *   Collection "users"
 *     Document "{uid}"
 *       Collection "petState"
 *         Document "latest"   → enthält PetEntity‐Felder
 *       Collection "steps"
 *         Document "{today}"  → enthält Feld "count": Int
 */
class CloudRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Speichere den aktuellen Pet‐State in Firestore unter users/{uid}/petState/latest.
     */
    suspend fun savePetToCloud(pet: PetEntity) {
        // 1. Prüfen, ob ein User eingeloggt ist
        val uid = auth.currentUser?.uid ?: return

        // 2. Pfad „users/{uid}/petState/latest“ aufbauen
        val docRef = firestore
            .collection("users")
            .document(uid)
            .collection("petState")
            .document("latest")

        // 3. Map der PetEntity‐Felder anlegen (Firestore speichert nur primitiv-Datentypen)
        val data = mapOf(
            "id" to pet.id,
            "name" to pet.name,
            "hungerLevel" to pet.hungerLevel,
            "health" to pet.health,
            "happiness" to pet.happiness
        )

        // 4. Schreibvorgang (in Kotlin: await(), damit wir in Coroutines sauber warten können)
        docRef.set(data).await()
    }

    /**
     * Lädt den Pet‐State aus Firestore (users/{uid}/petState/latest) und gibt ihn zurück.
     * Wenn noch kein Dokument existiert, liefert diese Funktion null.
     */
    suspend fun loadPetFromCloud(): PetEntity? {
        val uid = auth.currentUser?.uid ?: return null
        val docSnapshot = firestore
            .collection("users")
            .document(uid)
            .collection("petState")
            .document("latest")
            .get()
            .await()

        if (!docSnapshot.exists()) {
            return null
        }

        // Felder auslesen (Firestore speichert alle Zahlen als Long oder Double, also casten wir ggf.)
        val id = (docSnapshot.getLong("id") ?: 0L)
        val name = docSnapshot.getString("name") ?: "Your Pet"
        val hunger = (docSnapshot.getLong("hungerLevel") ?: 100L).toInt()
        val health = (docSnapshot.getLong("health") ?: 100L).toInt()
        val happiness = (docSnapshot.getLong("happiness") ?: 100L).toInt()

        return PetEntity(
            id = id,
            name = name,
            hungerLevel = hunger,
            health = health,
            happiness = happiness
        )
    }

    /**
     * Speichere die heutige Schrittzahl unter users/{uid}/steps/{today}. Dabei kann
     * {today} z.B. ein String wie "2025-05-31" sein oder ein Unix-Timestamp.
     *
     * @param todayId Ein eindeutiger String für das heutige Datum (z.B. "2025-05-31").
     * @param count Die Anzahl der Schritte (Int).
     */
    suspend fun saveStepsToCloud(todayId: String, count: Int) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = firestore
            .collection("users")
            .document(uid)
            .collection("steps")
            .document(todayId)

        val data = mapOf(
            "count" to count
        )

        docRef.set(data).await()
    }

    /**
     * Lädt die Schrittzahl für {today} und ruft callback(remoteCount) auf.
     * Wenn kein Dokument existiert, übergebe 0.
     */
    suspend fun loadStepsFromCloud(todayId: String): Int {
        val uid = auth.currentUser?.uid ?: return 0
        val docSnapshot = firestore
            .collection("users")
            .document(uid)
            .collection("steps")
            .document(todayId)
            .get()
            .await()

        return if (docSnapshot.exists()) {
            (docSnapshot.getLong("count") ?: 0L).toInt()
        } else {
            0
        }
    }
}


