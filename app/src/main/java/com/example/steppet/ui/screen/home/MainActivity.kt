package com.example.steppet.ui.screen.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.steppet.data.cloud.CloudRepository
import com.example.steppet.data.local.AppDatabase
import com.example.steppet.data.repository.PetRepository
import com.example.steppet.logic.StepTrackerManager
import com.example.steppet.ui.navigation.AppNavGraph
import com.example.steppet.ui.theme.StepPetTheme
import com.example.steppet.viewmodel.LoginViewModel
import com.example.steppet.viewmodel.StepTrackerViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val petRepo = PetRepository(this)

        setContent {
            StepPetTheme {
                val loginVM: LoginViewModel = viewModel()
                val stepsVM: StepTrackerViewModel = viewModel()
                val navController = rememberNavController()

                // Sync data once user is authenticated
                if (auth.currentUser != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        petRepo.syncPetFromCloud()
                    }
                    StepTrackerManager.loadStepsFromCloud {
                        StepTrackerManager.onStepsLoaded(it)
                    }
                }

                AppNavGraph(
                    navController = navController,
                    loginVM = loginVM,
                    stepsVM = stepsVM
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        StepTrackerManager.start(this)
    }

    override fun onStop() {
        super.onStop()
        StepTrackerManager.stop()
    }
}
