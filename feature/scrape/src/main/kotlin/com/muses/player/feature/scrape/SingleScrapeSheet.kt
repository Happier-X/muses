package com.muses.player.feature.scrape

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.muses.player.core.model.Song
import com.muses.player.core.ui.components.SaltTextButton
import com.muses.player.core.ui.theme.LocalSaltColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleScrapeSheet(
    song: Song,
    onDismiss: () -> Unit,
    viewModel: SingleScrapeViewModel = hiltViewModel(),
) {
    val salt = LocalSaltColors.current
    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(song.id) {
        viewModel.search(song)
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.reset()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = salt.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("刮削 - ${song.title}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = salt.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(12.dp))
            when (val s = state) {
                is SingleScrapeState.Searching -> {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("正在搜索候选…", fontSize = 13.sp, color = salt.text2)
                            Text(s.songTitle, fontSize = 12.sp, color = salt.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                is SingleScrapeState.HasCandidates -> {
                    val candidate = s.candidates[s.selectedIndex]
                    // 原/新对比
                    Column(
                        Modifier.fillMaxWidth().background(salt.surface1, RoundedCornerShape(10.dp)).border(0.5.dp, salt.surface2, RoundedCornerShape(10.dp)).padding(12.dp),
                    ) {
                        Text("原：${candidate.currentTitle} · ${candidate.currentArtist ?: "未知艺术家"}${candidate.currentAlbum?.let { " · $it" } ?: ""}", fontSize = 12.sp, color = salt.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!candidate.currentLyrics.isNullOrBlank()) Text("原歌词：有（${candidate.currentLyrics!!.length}字）", fontSize = 11.sp, color = salt.text2)
                        Spacer(Modifier.height(6.dp))
                        val newTitle = candidate.resolvedTitle() ?: "—"
                        val newArtist = candidate.resolvedArtist() ?: "—"
                        val newAlbum = candidate.resolvedAlbum()
                        val hasText = candidate.resolvedTitle() != null || candidate.resolvedArtist() != null || candidate.resolvedAlbum() != null
                        val hasLyrics = !candidate.resolvedLyrics().isNullOrBlank()
                        Text(
                            buildString {
                                append("新："); append(newTitle); append(" · "); append(newArtist)
                                if (newAlbum != null) { append(" · "); append(newAlbum) }
                                if (!hasText) append("（无文本变更）")
                                if (hasLyrics) append(" · 歌词有")
                            },
                            fontSize = 13.sp,
                            color = if (candidate.editTitle != null || candidate.editArtist != null || candidate.editAlbum != null || candidate.editLyrics != null) salt.primary else salt.text,
                            fontWeight = if (candidate.editTitle != null || candidate.editArtist != null || candidate.editAlbum != null) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        if (candidate.confidence != null) {
                            Spacer(Modifier.height(4.dp))
                            Box(Modifier.background(salt.primary.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(candidate.confidence!!, fontSize = 10.sp, color = salt.primary)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // 封面
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (candidate.coverUrl != null) {
                            AsyncImage(model = candidate.coverUrl, contentDescription = "封面", modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)).background(salt.surface2), contentScale = ContentScale.Crop)
                            Spacer(Modifier.size(12.dp))
                            Text("封面已命中，点击可大图预览（后续）", fontSize = 12.sp, color = salt.text2, modifier = Modifier.weight(1f))
                        } else {
                            Box(Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)).background(salt.surface2), contentAlignment = Alignment.Center) { Text("无封面", fontSize = 11.sp, color = salt.text2) }
                            Spacer(Modifier.size(12.dp))
                            Text("未命中封面", fontSize = 12.sp, color = salt.text2)
                        }
                    }
                    // 歌词摘要
                    if (!candidate.resolvedLyrics().isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("歌词预览：", fontSize = 12.sp, color = salt.text2)
                        Text(candidate.resolvedLyrics()!!.take(120).replace("\n", " "), fontSize = 11.sp, color = salt.text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.background(salt.surface2, RoundedCornerShape(6.dp)).padding(8.dp).fillMaxWidth())
                    }
                    Spacer(Modifier.height(12.dp))
                    SingleScrapeEditFields(candidate = candidate, onConfirmEdit = { t, a, al, l -> viewModel.updateCandidateEdit(t, a, al, l) })
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { viewModel.reset(); onDismiss() }, modifier = Modifier.weight(1f)) { Text("取消") }
                        Button(onClick = { viewModel.applySelected { viewModel.reset(); onDismiss() } }, modifier = Modifier.weight(1f)) { Text("应用") }
                    }
                }
                is SingleScrapeState.Empty -> {
                    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.reason, fontSize = 14.sp, color = salt.text2)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.search(song) }) { Text("重试") }
                        Spacer(Modifier.height(8.dp))
                        SaltTextButton(text = "关闭", onClick = { viewModel.reset(); onDismiss() })
                    }
                }
                is SingleScrapeState.Writing -> {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("正在写回…", fontSize = 13.sp, color = salt.text2)
                        }
                    }
                }
                is SingleScrapeState.Success -> {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Text("已更新", fontSize = 14.sp, color = salt.primary)
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun SingleScrapeEditFields(
    candidate: PreviewCandidate,
    onConfirmEdit: (String?, String?, String?, String?) -> Unit,
) {
    val salt = LocalSaltColors.current
    var title by remember(candidate.songId) { mutableStateOf(candidate.resolvedTitle() ?: candidate.currentTitle) }
    var artist by remember(candidate.songId) { mutableStateOf(candidate.resolvedArtist() ?: candidate.currentArtist.orEmpty()) }
    var album by remember(candidate.songId) { mutableStateOf(candidate.resolvedAlbum() ?: candidate.currentAlbum.orEmpty()) }
    var lyrics by remember(candidate.songId) { mutableStateOf(candidate.resolvedLyrics() ?: candidate.currentLyrics.orEmpty()) }
    var expanded by remember { mutableStateOf(false) }
    Column {
        SaltTextButton(text = if (expanded) "收起编辑" else "编辑", onClick = { expanded = !expanded })
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("歌手") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = album, onValueChange = { album = it }, label = { Text("专辑") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = lyrics, onValueChange = { lyrics = it }, label = { Text("歌词（粘贴 LRC/TTML）") }, modifier = Modifier.fillMaxWidth().height(100.dp), maxLines = 5)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                SaltTextButton(text = "应用编辑", onClick = {
                    val outTitle = title.trim().takeIf { it.isNotEmpty() && it != candidate.matchedTitle }
                    val outArtist = artist.trim().takeIf { it.isNotEmpty() && it != candidate.matchedArtist }
                    val outAlbum = album.trim().takeIf { it.isNotEmpty() && it != candidate.matchedAlbum }
                    val outLyrics = lyrics.trim().takeIf { it.isNotEmpty() && it != candidate.matchedLyrics } ?: lyrics.trim().takeIf { it.isNotEmpty() && it != candidate.currentLyrics }
                    onConfirmEdit(outTitle, outArtist, outAlbum, outLyrics)
                    expanded = false
                })
            }
        }
    }
}
