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

/**
 * PetRepository verwaltet sowohl den lokalen Room‐Datensatz als
 * auch die Synchronisation in die Cloud (Firestore) über CloudRepository.
 */
class PetRepository(context: Context) {

    // DAO für Room
    private val petDao = AppDatabase.getInstance(context).petDao()

    // Auth‐Instanz, um die UID des aktuellen Users zu bekommen
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val DEFAULT_ID = 0L
        private const val DEFAULT_HUNGER = 100
        private const val DEFAULT_HEALTH = 100
        private const val DEFAULT_HAPPINESS = 100
    }

    /**
     * Liefert einen Flow, der das Pet (ID = 0) aus Room zurückgibt,
     * oder bei null einen Default‐PetEntity.
     */
    fun petFlow(): Flow<PetEntity> =
        petDao.getPetFlow(DEFAULT_ID).map { dbEntity ->
            dbEntity ?: PetEntity(
                id = DEFAULT_ID,
                name = "Your Pet",
                hungerLevel = DEFAULT_HUNGER,
                health = DEFAULT_HEALTH,
                happiness = DEFAULT_HAPPINESS
            )
        }

    /**
     * Liest Pet einmalig (oder erzeugt einen Default), falls nichts in Room ist.
     */
    private suspend fun loadPet(): PetEntity =
        petDao.getPetOnce(DEFAULT_ID) ?: PetEntity(
            id = DEFAULT_ID,
            name = "Your Pet",
            hungerLevel = DEFAULT_HUNGER,
            health = DEFAULT_HEALTH,
            happiness = DEFAULT_HAPPINESS
        )

    /**
     * Schreibt Änderungen in Room und in Firestore (via CloudRepository).
     */
    private suspend fun upsertPet(pet: PetEntity) {
        // 1) Update in Room:
        petDao.upsert(pet)

        // 2) Critical Check (lösche in Room + in der Cloud, falls health/happiness == 0)
        checkCritical(pet)

        // 3) Wenn noch alles > 0, speichere den aktuellen Pet‐Zustand in Firestore
        //    Ein Objektaufruf, nicht CloudRepository()!
        CloudRepository.savePetToCloud(pet)
    }

    /**
     * Wenn Health oder Happiness auf 0 gesunken ist, lösche in Room und
     * speichere einen leeren Pet‐Zustand in der Cloud, damit Pet verschwindet.
     */
    private suspend fun checkCritical(pet: PetEntity) {
        if (pet.health == 0 || pet.happiness == 0) {
            // 1) aus Room löschen
            petDao.delete(pet)

            // 2) in Firestore “löschen” → wir speichern ein “leeres” PetEntity
            //    (wir nutzen denselben savePetToCloud-Aufruf, um Dokument zu überschreiben)
            val emptyPet = PetEntity(
                id = DEFAULT_ID,
                name = "",
                hungerLevel = 0,
                health = 0,
                happiness = 0
            )
            CloudRepository.savePetToCloud(emptyPet)
        }
    }

    /** Hunger direkt setzen (0–100), anschließend updaten. */
    suspend fun setHunger(level: Int) {
        val pet = loadPet().copy(
            hungerLevel = level.coerceIn(0, 100)
        )
        upsertPet(pet)
    }

    /** Health um delta ändern (0–100), anschließend updaten. */
    suspend fun changeHealth(delta: Int) {
        val old = loadPet()
        val pet = old.copy(
            health = (old.health + delta).coerceIn(0, 100)
        )
        upsertPet(pet)
    }

    /** Happiness um delta ändern (0–100), anschließend updaten. */
    suspend fun changeHappiness(delta: Int) {
        val old = loadPet()
        val pet = old.copy(
            happiness = (old.happiness + delta).coerceIn(0, 100)
        )
        upsertPet(pet)
    }

    /**
     * Gibt die aktuelle Hunger‐Stufe zurück (oder DEFAULT_HUNGER, falls kein Datensatz).
     * Wird zum Beispiel von einem Worker genutzt.
     */
    suspend fun getHunger(): Int =
        petDao.getPetOnce(DEFAULT_ID)?.hungerLevel ?: DEFAULT_HUNGER

    /**
     * Synchronisiert lokalen Pet‐Zustand mit dem in Firestore:
     *  • Falls in Firestore schon ein Dokument unter users/{uid}/petState/latest existiert,
     *    lies es aus und speichere es lokal in Room.
     *  • Falls kein Dokument existiert, lege in der Cloud einen Default‐Pet an.
     */
    suspend fun syncPetFromCloud() {
        val uid = auth.currentUser?.uid ?: return

        // 1) Aus Cloud laden (kann null sein, falls noch nichts dort steht)
        val cloudPet = CloudRepository.getPetFromCloud()

        // 2) Wenn nichts da war, erstelle eine Default‐Version und speichere sie in Firestore
        val petToSave: PetEntity = cloudPet ?: PetEntity(
            id = DEFAULT_ID,
            name = "Your Pet",
            hungerLevel = DEFAULT_HUNGER,
            health = DEFAULT_HEALTH,
            happiness = DEFAULT_HAPPINESS
        ).also { defaultPet ->
            // Speichere das Default‐Pet ebenfalls in Firestore (damit Collection/Doc angelegt wird)
            CloudRepository.savePetToCloud(defaultPet)
        }

        // 3) Aktualisiere Room mit dem Pet aus Cloud (oder dem Default‐Pet)
        petDao.upsert(petToSave)
    }
}









