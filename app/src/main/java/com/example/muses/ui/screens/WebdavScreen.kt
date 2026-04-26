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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.VisualTransformation
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

@Composable
fun WebdavScreen(
    modifier: Modifier = Modifier,
    viewModel: WebdavViewModel = viewModel(),
    onTrackClick: (AudioTrack) -> Unit = {},
    onTracksAdded: (List<AudioTrack>) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var addDirTarget by remember { mutableStateOf<WebdavItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.addedTracks.collect { tracks ->
            if (tracks.isNotEmpty()) {
                onTracksAdded(tracks)
            }
        }
    }

    if (addDirTarget != null) {
        AddDirectoryDialog(
            directoryName = addDirTarget!!.displayName,
            onDismiss = { addDirTarget = null },
            onAddRecursive = {
                viewModel.addDirectory(addDirTarget!!.href, recursive = true)
                addDirTarget = null
            },
            onAddFlat = {
                viewModel.addDirectory(addDirTarget!!.href, recursive = false)
                addDirTarget = null
            }
        )
    }

    WebdavContent(
        modifier = modifier,
        uiState = uiState,
        viewModel = viewModel,
        onTrackClick = onTrackClick,
        onAddDirTarget = { addDirTarget = it }
    )
}

@Composable
private fun WebdavContent(
    modifier: Modifier = Modifier,
    uiState: WebdavUiState,
    viewModel: WebdavViewModel,
    onTrackClick: (AudioTrack) -> Unit,
    onAddDirTarget: (WebdavItem) -> Unit
) {
    when (val state = uiState) {
        is WebdavUiState.NotConfigured -> {
            ConfigFormContent(
                initialConfig = viewModel.lastConfig,
                onConnect = { config -> viewModel.connect(config) },
                modifier = modifier
            )
        }
        is WebdavUiState.Connecting -> {
            ConnectingContent(modifier = modifier)
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
                onDirectoryLongClick = { item ->
                    if (item.isCollection) {
                        onAddDirTarget(item)
                    }
                },
                onGoToParent = { viewModel.goToParent() },
                modifier = modifier
            )
        }
        is WebdavUiState.EmptyDirectory -> {
            DirectoryContent(
                config = state.config,
                currentPath = state.currentPath,
                items = emptyList(),
                onItemClick = {},
                onGoToParent = { viewModel.goToParent() },
                modifier = modifier
            )
        }
        is WebdavUiState.AddingDirectory -> {
            AddingDirectoryContent(
                path = state.targetPath,
                modifier = modifier
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
                onChangeSettings = { viewModel.disconnect() },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun ConfigFormContent(
    initialConfig: WebdavConfig?,
    onConnect: (WebdavConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var serverUrl by remember { mutableStateOf(initialConfig?.serverUrl ?: "") }
    var username by remember { mutableStateOf(initialConfig?.username ?: "") }
    var password by remember { mutableStateOf(initialConfig?.password ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }
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
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) stringResource(R.string.webdav_hide_password) else stringResource(R.string.webdav_show_password)
                    )
                }
            },
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
    modifier: Modifier = Modifier,
    onDirectoryLongClick: (WebdavItem) -> Unit = {}
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
                        onClick = { onItemClick(item) },
                        onAddClick = if (item.isCollection) {{ onDirectoryLongClick(item) }} else null
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
    onClick: () -> Unit = {},
    onAddClick: (() -> Unit)? = null
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
        trailingContent = if (onAddClick != null) {
            {
                IconButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.webdav_add_directory)
                    )
                }
            }
        } else null,
        modifier = modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onChangeSettings: () -> Unit,
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
            Button(onClick = onRetry) {
                Text(stringResource(R.string.webdav_connect))
            }
            OutlinedButton(onClick = onChangeSettings) {
                Text(stringResource(R.string.webdav_change_settings))
            }
        }
    }
}

@Composable
private fun AddDirectoryDialog(
    directoryName: String,
    onDismiss: () -> Unit,
    onAddRecursive: () -> Unit,
    onAddFlat: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.webdav_add_directory)) },
        text = {
            Text(stringResource(R.string.webdav_add_directory_desc, directoryName))
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAddFlat) {
                    Text(stringResource(R.string.webdav_add_flat))
                }
                Button(onClick = onAddRecursive) {
                    Text(stringResource(R.string.webdav_add_recursive))
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun AddingDirectoryContent(
    path: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.webdav_adding_directory, path),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
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
