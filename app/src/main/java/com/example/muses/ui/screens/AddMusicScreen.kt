package com.example.muses.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.muses.R
import com.example.muses.data.model.AudioTrack
import com.example.muses.ui.theme.MusesTheme
import com.example.muses.ui.viewmodel.WebdavViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMusicScreen(
    modifier: Modifier = Modifier,
    hasPermission: Boolean = true,
    requestPermission: () -> Unit = {},
    onTrackClick: (AudioTrack) -> Unit = {},
    onTracksAdded: (List<AudioTrack>) -> Unit = {},
    onTracksRemoved: (List<String>) -> Unit = {},
    onFolderSelected: (Uri) -> Unit = {},
    addedDirectoryPaths: Set<String> = emptySet(),
    webdavViewModel: WebdavViewModel,
    navigationIcon: @Composable () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { onFolderSelected(it) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_add_music)) },
                navigationIcon = navigationIcon,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.add_music_local)) },
                    icon = { Icon(Icons.Default.FolderOpen, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.add_music_webdav)) },
                    icon = { Icon(Icons.Default.Cloud, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> LocalFolderTab(
                    onPickFolder = { folderPickerLauncher.launch(null) }
                )
                1 -> WebdavScreen(
                    viewModel = webdavViewModel,
                    onTrackClick = onTrackClick,
                    onTracksAdded = onTracksAdded,
                    onTracksRemoved = onTracksRemoved,
                    addedDirectoryPaths = addedDirectoryPaths
                )
            }
        }
    }
}

@Composable
private fun LocalFolderTab(
    onPickFolder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.add_music_local_prompt),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.add_music_local_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onPickFolder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.add_music_pick_folder))
        }
    }
}