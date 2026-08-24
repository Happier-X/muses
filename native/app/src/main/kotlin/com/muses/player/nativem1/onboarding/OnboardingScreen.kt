package com.muses.player.nativem1.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.muses.player.core.model.SourceType

/** 首次启动引导页 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val step by viewModel.step.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("欢迎使用 Muses") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep(
                    onAddLocal = { viewModel.goToStep(OnboardingStep.ADD_LOCAL) },
                    onAddWebdav = { viewModel.goToStep(OnboardingStep.ADD_WEBDAV) },
                    onSkip = { viewModel.complete(onComplete) },
                )
                OnboardingStep.ADD_LOCAL -> AddLocalStep(
                    viewModel = viewModel,
                    onNext = { viewModel.goToStep(OnboardingStep.ADD_WEBDAV) },
                    onBack = { viewModel.goToStep(OnboardingStep.WELCOME) },
                )
                OnboardingStep.ADD_WEBDAV -> AddWebdavStep(
                    viewModel = viewModel,
                    onComplete = { viewModel.complete(onComplete) },
                    onBack = { viewModel.goToStep(OnboardingStep.WELCOME) },
                    isSaving = isSaving,
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(
    onAddLocal: () -> Unit,
    onAddWebdav: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "添加你的音乐",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "添加本地目录或 WebDAV 服务器来开始浏览音乐",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onAddLocal,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("添加本地目录")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onAddWebdav,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Cloud, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("添加 WebDAV 服务器")
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "稍后再说",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp).let { mod ->
                mod.then(Modifier.padding(8.dp))
            },
        )
        OutlinedButton(onClick = onSkip) {
            Text("跳过")
        }
    }
}

@Composable
private fun AddLocalStep(
    viewModel: OnboardingViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val localPath by viewModel.localPath.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "添加本地音乐目录",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "输入本地音乐目录的路径",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = localPath,
            onValueChange = { viewModel.updateLocalPath(it) },
            label = { Text("目录路径") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("/storage/emulated/0/Music") },
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                viewModel.saveLocalSource()
                onNext()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = localPath.isNotBlank(),
        ) {
            Text("保存并继续")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("返回")
        }
    }
}

@Composable
private fun AddWebdavStep(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    isSaving: Boolean,
) {
    val webdavUrl by viewModel.webdavUrl.collectAsState()
    val webdavUsername by viewModel.webdavUsername.collectAsState()
    val webdavPassword by viewModel.webdavPassword.collectAsState()
    val webdavName by viewModel.webdavName.collectAsState()
    val testState by viewModel.testState.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Cloud,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "添加 WebDAV 服务器",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = webdavName,
            onValueChange = { viewModel.updateWebdavName(it) },
            label = { Text("服务器名称") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = webdavUrl,
            onValueChange = { viewModel.updateWebdavUrl(it) },
            label = { Text("服务器地址") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://dav.example.com/music/") },
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = webdavUsername,
            onValueChange = { viewModel.updateWebdavUsername(it) },
            label = { Text("用户名") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = webdavPassword,
            onValueChange = { viewModel.updateWebdavPassword(it) },
            label = { Text("密码") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        Spacer(Modifier.height(12.dp))

        // 测试连接状态
        when (testState) {
            is TestState.Testing -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("正在测试连接…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            is TestState.Success -> {
                Text(
                    "连接成功 ✓",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            is TestState.Failure -> {
                Text(
                    (testState as TestState.Failure).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            is TestState.Idle -> { /* no-op */ }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.saveWebdavSource(onComplete) },
            modifier = Modifier.fillMaxWidth(),
            enabled = webdavUrl.isNotBlank() && !isSaving,
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("保存并开始扫描")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("返回")
        }
    }
}
