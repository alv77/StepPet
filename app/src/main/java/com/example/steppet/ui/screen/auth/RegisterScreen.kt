package com.example.steppet.ui.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.steppet.ui.screen.auth.AuthForm
import com.example.steppet.viewmodel.LoginViewModel

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    AuthForm(
        title = "Register new account",
        buttonText = "Register",
        onSubmit = {
            viewModel.register { success ->
                if (success) onRegisterSuccess()
            }
        },
        viewModel = viewModel
    )
}
