package com.muses.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var isRegister by mutableStateOf(false)

    suspend fun login(username: String, password: String): Boolean {
        isLoading = true
        error = null
        return try {
            authRepository.login(username, password)
            true
        } catch (e: Exception) {
            error = e.message
            false
        } finally {
            isLoading = false
        }
    }

    suspend fun register(username: String, password: String): Boolean {
        isLoading = true
        error = null
        return try {
            authRepository.register(username, password)
            true
        } catch (e: Exception) {
            error = e.message
            false
        } finally {
            isLoading = false
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Muses", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("用户名") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            modifier = Modifier.fillMaxWidth()
        )

        if (viewModel.error != null) {
            Text(
                text = viewModel.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.viewModelScope.launch {
                    val success = if (viewModel.isRegister) {
                        viewModel.register(username, password)
                    } else {
                        viewModel.login(username, password)
                    }
                    if (success) onLoginSuccess()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isLoading
        ) {
            Text(if (viewModel.isRegister) "注册" else "登录")
        }

        TextButton(onClick = { viewModel.isRegister = !viewModel.isRegister }) {
            Text(if (viewModel.isRegister) "已有账号？登录" else "没有账号？注册")
        }
    }
}
