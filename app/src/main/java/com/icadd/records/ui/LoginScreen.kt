package com.icadd.records.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.icadd.records.AppViewModel

@Composable
fun LoginScreen(vm: AppViewModel, onLoggedIn: () -> Unit) {
    var isRegister by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(24.dp).widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("ICADD Records", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(if (isRegister) "Create account" else "Sign in", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))

            if (isRegister) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it }, label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            if (vm.authError != null) {
                Text(vm.authError ?: "", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (isRegister) vm.register(email, password, name) { ok -> if (ok) onLoggedIn() }
                    else vm.login(email, password) { ok -> if (ok) onLoggedIn() }
                },
                enabled = !vm.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (vm.loading) "Please wait..." else if (isRegister) "Register" else "Sign in")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { isRegister = !isRegister }) {
                Text(if (isRegister) "Already have an account? Sign in" else "New employee? Register")
            }
            if (isRegister) {
                Text(
                    "First person to register becomes admin. After that, an admin adds you before you can register.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
