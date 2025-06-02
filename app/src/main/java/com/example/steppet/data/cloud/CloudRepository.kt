package com.example.steppet.data.cloud

import android.util.Log
import com.example.steppet.data.local.PetEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.steppet.data.model.StepStats

/**
 * Einfache Hilfsklasse für Firestore‐Operationen rund um "users/{uid}/…".
 *
 * Enthält:
 *  • Pet‐Zustand speichern / laden
 *  • Schrittzahl speichern / laden
 *  • Pet‐Dokument löschen / Schritte löschen
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
    suspend fun saveStepsToCloud(
        uid: String,
        dateString: String,
        count: Int,
        androidTotal: Int
    ) {
        val docRef = firestore
            .collection("users")
            .document(uid)
            .collection("steps")
            .document(dateString)

        val data: Map<String, Any> = mapOf(
            "count" to count,
            "android_total" to androidTotal
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

    // ------------------------------------------------------------------------------------------------
    // 3) PET‐DOKUMENT LÖSCHEN / ALLE SCHRITTE LÖSCHEN
    // ------------------------------------------------------------------------------------------------

    /**
     * Löscht in Firestore das Dokument 'users/{uid}/petState/latest'.
     */
    suspend fun deletePetInCloud() {
        val uid = currentUid() ?: return
        firestore
            .collection("users")
            .document(uid)
            .collection("petState")
            .document("latest")
            .delete()
            .await()
    }

    /**
     * Löscht alle Dokumente in 'users/{uid}/steps'.
     * Achtung: Firestore erlaubt kein Lösch‐Bulk per Query; wir
     * müssen einzeln alle Dokument‐IDs auslesen und dann löschen.
     */
    suspend fun deleteAllStepsInCloud() {
        val uid = currentUid() ?: return
        val stepsCollection = firestore
            .collection("users")
            .document(uid)
            .collection("steps")

        val snapshot = stepsCollection.get().await()
        for (doc in snapshot.documents) {
            stepsCollection.document(doc.id).delete().await()
        }
    }

    suspend fun getStepsLast7Days(): Map<String, Int> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyMap()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()

        val firestore = FirebaseFirestore.getInstance()
        val result = mutableMapOf<String, Int>()

        for (i in 6 downTo 0) {
            val date = today.minusDays(i.toLong())
            val dateString = date.format(formatter)

            try {
                val snapshot = firestore
                    .collection("users")
                    .document(uid)
                    .collection("steps")
                    .document(dateString)
                    .get()
                    .await()

                val count = snapshot.getLong("count")?.toInt() ?: 0
                result[dateString] = count

                Log.d("StepFetch", "[$dateString] count: $count (exists=${snapshot.exists()})")
            } catch (e: Exception) {
                Log.e("StepFetch", "Error fetching steps for $dateString: ${e.message}")
                result[dateString] = 0
            }
        }

        return result
    }

    suspend fun getGlobalStepStats(): StepStats? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val stepsCollection = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("steps")

        val snapshot = stepsCollection.get().await()
        if (snapshot.isEmpty) return null

        val stepsMap = snapshot.documents
            .mapNotNull { doc ->
                val count = doc.getLong("count")?.toInt() ?: return@mapNotNull null
                val date = doc.id // format assumed to be YYYY-MM-DD
                date to count
            }
            .sortedBy { it.first }  // sort by date
            .toMap()

        val values = stepsMap.values.toList()
        val nonZero = stepsMap.filterValues { it > 0 }

        val total = values.sum()
        val average = if (values.isNotEmpty()) total / values.size else 0
        val best = nonZero.maxByOrNull { it.value }
        val worst = nonZero.minByOrNull { it.value }

        // Calculate streak from latest to earliest
        val streak = stepsMap.entries.reversed().takeWhile { it.value > 0 }.count()

        return StepStats(
            totalSteps = total,
            averageSteps = average,
            bestDay = best?.key,
            bestCount = best?.value ?: 0,
            worstDay = worst?.key,
            worstCount = worst?.value ?: 0,
            currentStreak = streak
        )
    }

}
