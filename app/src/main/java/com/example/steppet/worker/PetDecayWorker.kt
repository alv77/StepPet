package com.example.steppet.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.steppet.data.repository.PetRepository
import androidx.work.ListenableWorker.Result

class PetDecayWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repo = PetRepository(context)

    override suspend fun doWork(): Result {
        return try {
            // 1) Hunger aus DB laden
            val currentHunger = repo.getHunger()
            // 2) Hunger um 1 reduzieren (wird intern geclamped auf >=0)
            repo.setHunger(currentHunger - 1)

            // 3) Health & Happiness jeweils um 1 reduzieren
            repo.changeHealth(-1)
            repo.changeHappiness(-1)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

