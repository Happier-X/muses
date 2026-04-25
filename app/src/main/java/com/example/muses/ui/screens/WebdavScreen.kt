package com.example.muses.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.muses.R
import com.example.muses.data.model.AudioTrack
import com.example.muses.data.model.WebdavConfig
import com.example.muses.data.repository.WebdavItem
import com.example.muses.ui.theme.MusesTheme
import com.example.muses.ui.viewmodel.WebdavUiState
import com.example.muses.ui.viewmodel.WebdavViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebdavScreen(
    modifier: Modifier = Modifier,
    viewModel: WebdavViewModel = viewModel(),
    onTrackClick: (AudioTrack) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_webdav)) },
                actions = {
                    if (uiState is WebdavUiState.Browsing || uiState is WebdavUiState.EmptyDirectory) {
                        IconButton(onClick = { viewModel.disconnect() }) {
                            Icon(
                                imageVector = Icons.Default.LinkOff,
                                contentDescription = stringResource(R.string.webdav_disconnect)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is WebdavUiState.NotConfigured -> {
                ConfigFormContent(
                    onConnect = { config -> viewModel.connect(config) },
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                )
            }
            is WebdavUiState.Connecting -> {
                ConnectingContent(modifier = Modifier.fillMaxSize().padding(innerPadding))
            }
            is WebdavUiState.Browsing -> {
                DirectoryContent(
                    config = state.config,
                    currentPath = state.currentPath,
                    items = state.items,
                    onItemClick = { item ->
                        if (item.isCollection) {
                            viewModel.browsePath(item.href)
                        } else {
                            val audioTrack = viewModel.toAudioTrack(item)
                            if (audioTrack != null) {
                                onTrackClick(audioTrack)
                            }
                        }
                    },
                    onGoToParent = { viewModel.goToParent() },
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                )
            }
            is WebdavUiState.EmptyDirectory -> {
                DirectoryContent(
                    config = state.config,
                    currentPath = state.currentPath,
                    items = emptyList(),
                    onItemClick = {},
                    onGoToParent = { viewModel.goToParent() },
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                )
            }
            is WebdavUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = {
                        val config = state.config
                        if (config != null) {
                            viewModel.connect(config)
                        } else {
                            viewModel.disconnect()
                        }
                    },
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun ConfigFormContent(
    onConnect: (WebdavConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.webdav_configure_prompt),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = serverUrl,
            onValueChange = {
                serverUrl = it
                urlError = null
            },
            label = { Text(stringResource(R.string.webdav_server_url)) },
            placeholder = { Text(stringResource(R.string.webdav_url_hint)) },
            isError = urlError != null,
            supportingText = urlError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.webdav_username)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.webdav_password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (serverUrl.isNotBlank()) {
                        onConnect(WebdavConfig(serverUrl, username, password))
                    }
                }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (serverUrl.isBlank()) {
                    urlError = context.getString(R.string.url_cannot_be_empty)
                } else if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                    urlError = context.getString(R.string.url_must_be_http)
                } else {
                    onConnect(WebdavConfig(serverUrl, username, password))
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.webdav_connect))
        }
    }
}

@Composable
private fun ConnectingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.webdav_loading),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun DirectoryContent(
    config: WebdavConfig,
    currentPath: String,
    items: List<WebdavItem>,
    onItemClick: (WebdavItem) -> Unit,
    onGoToParent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Connection status bar
        Text(
            text = stringResource(R.string.webdav_connected, config.serverUrl),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Path breadcrumb
        if (currentPath.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onGoToParent, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.webdav_parent_dir),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.webdav_directory) + ": $currentPath",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (items.isEmpty()) {
            EmptyDirContent(Modifier.fillMaxWidth().weight(1f))
        }

        if (items.isEmpty() && currentPath.isNotEmpty()) {
            EmptyDirContent(Modifier.fillMaxWidth().weight(1f))
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                // Parent directory entry (if in subdirectory)
                if (currentPath.isNotEmpty()) {
                    item(key = "__parent__") {
                        ParentDirItem(onClick = onGoToParent)
                    }
                }

                items(items = items, key = { it.href }) { item ->
                    WebdavListItem(
                        item = item,
                        onClick = { onItemClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDirContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Text(
            text = stringResource(R.string.webdav_empty),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ParentDirItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.webdav_parent_dir)) },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.SubdirectoryArrowRight,
                contentDescription = null
            )
        },
        modifier = modifier.clickable(onClick = onClick)
    )
}

@Composable
fun WebdavListItem(
    item: WebdavItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    ListItem(
        headlineContent = {
            Text(
                text = item.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            val info = when {
                item.isCollection -> stringResource(R.string.webdav_item_directory)
                item.contentType != null -> item.contentType
                else -> ""
            }
            if (info.isNotBlank()) {
                Text(
                    text = info,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = if (item.isCollection) Icons.Default.Folder else Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        },
        modifier = modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.webdav_error, message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRetry) {
                Text(stringResource(R.string.webdav_connect))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun WebdavScreenPreview() {
    MusesTheme {
        WebdavScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun WebdavListItemPreview() {
    MusesTheme {
        WebdavListItem(
            item = WebdavItem(
                href = "/music/song.mp3",
                displayName = "song.mp3",
                contentType = "audio/mpeg",
                contentLength = 5_000_000L,
                isCollection = false
            )
        )
    }
}
