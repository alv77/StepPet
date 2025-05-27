package com.example.steppet.ui.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.example.steppet.viewmodel.LoginViewModel

@Composable
fun AuthForm(
    title: String,
    buttonText: String,
    onSubmit: () -> Unit,
    viewModel: LoginViewModel
) {
    val user by remember { derivedStateOf { viewModel.username } }
    val pass by remember { derivedStateOf { viewModel.password } }
    val error by remember { derivedStateOf { viewModel.errorMessage } }

    // When loginSuccess flips true, invoke our callback
    LaunchedEffect(viewModel.loginSuccess) {
        if (viewModel.loginSuccess) onSubmit()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = user,
            onValueChange = viewModel::onUsernameChange,
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = pass,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))

        Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) {
            Text(buttonText)
        }
    }
}
