package com.example.steppet.data.cloud

import com.example.steppet.data.local.PetEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Einfache Hilfsklasse für Firestore‐Operationen rund um "users/{uid}/…".
 *
 * Enthält:
 *  • Pet‐Zustand speichern / laden
 *  • Schrittzahl speichern / laden
 */
object CloudRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Hilfsfunktion: aktuelle User‐UID (oder null, falls nicht eingeloggt)
    private fun currentUid(): String? = auth.currentUser?.uid

    // ------------------------------------------------------------------------------------------------
    // 1) PET‐DATEN IN FIRESTORE SPEICHERN / LADEN
    //    Pfad: users/{uid}/petState/latest
    // ------------------------------------------------------------------------------------------------

    /**
     * Speichert das gegebene PetEntity unter 'users/{uid}/petState/latest'.
     * Überschreibt dabei existierende Felder.
     */
    suspend fun savePetToCloud(pet: PetEntity) {
        val uid = currentUid() ?: return
        // Firestore‐Dokument‐Pfad: users/{uid}/petState/latest
        val docRef = firestore
            .collection("users")
            .document(uid)
            .collection("petState")
            .document("latest")

        // Mappe das PetEntity auf eine Map<String, Any>, damit Firestore es versteht
        val data: Map<String, Any> = mapOf(
            "id" to pet.id,
            "name" to pet.name,
            "hungerLevel" to pet.hungerLevel,
            "health" to pet.health,
            "happiness" to pet.happiness
        )
        // Schreibe (ersetze) das Dokument
        docRef.set(data).await()
    }

    /**
     * Liest aus Firestore das Dokument 'users/{uid}/petState/latest' und
     * wandelt es zurück in ein PetEntity. Gibt
     *   • null zurück, falls noch kein Dokument existiert oder
     *   • ein PetEntity mit den gespeicherten Feldern.
     */
    suspend fun getPetFromCloud(): PetEntity? {
        val uid = currentUid() ?: return null
        val docRef = firestore
            .collection("users")
            .document(uid)
            .collection("petState")
            .document("latest")

        val snapshot = docRef.get().await()
        if (!snapshot.exists()) return null

        // Lies die einzelnen Felder aus
        val id = (snapshot.getLong("id") ?: 0L)
        val name = (snapshot.getString("name") ?: "")
        val hunger = (snapshot.getLong("hungerLevel") ?: 100L).toInt()
        val health = (snapshot.getLong("health") ?: 100L).toInt()
        val happiness = (snapshot.getLong("happiness") ?: 100L).toInt()

        return PetEntity(
            id = id,
            name = name,
            hungerLevel = hunger,
            health = health,
            happiness = happiness
        )
    }

    // ------------------------------------------------------------------------------------------------
    // 2) SCHRITTZAHL IN FIRESTORE SPEICHERN / LADEN
    //    Pfad: users/{uid}/steps/{YYYY-MM-DD}
    // ------------------------------------------------------------------------------------------------

    /**
     * Speichert die übergebene Schrittzahl (count) unter
     *   users/{uid}/steps/{dateString}, z.B. dateString = "2025-06-01"
     */
    suspend fun saveStepsToCloud(uid: String, dateString: String, count: Int) {
        val docRef = firestore
            .collection("users")
            .document(uid)
            .collection("steps")
            .document(dateString)

        val data: Map<String, Any> = mapOf(
            "count" to count
        )
        docRef.set(data).await()
    }

    /**
     * Liest aus Firestore den Wert 'count' von
     *   users/{uid}/steps/{dateString}
     * zurück. Gibt null zurück, falls kein Dokument existiert oder Feld fehlt.
     */
    suspend fun getStepsFromCloud(uid: String, dateString: String): Int? {
        val docRef = firestore
            .collection("users")
            .document(uid)
            .collection("steps")
            .document(dateString)

        val snapshot = docRef.get().await()
        if (!snapshot.exists()) return null
        return snapshot.getLong("count")?.toInt()
    }
}



