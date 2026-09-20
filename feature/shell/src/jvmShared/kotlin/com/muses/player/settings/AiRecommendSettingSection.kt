package com.muses.player.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ai.AI_API_KEY_SOURCE_ID
import com.muses.player.core.ai.AiChatClient
import com.muses.player.core.ai.AiProviderPreset
import com.muses.player.core.ai.AiRecommendConfig
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.ui.components.MusesButton
import com.muses.player.core.ui.components.MusesTextField
import com.muses.player.core.ui.components.SettingsBlockTitle
import com.muses.player.core.ui.components.SettingsIcon
import com.muses.player.core.ui.icons.TablerIcons
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 设置页「AI 推荐」分组（首页「猜你喜欢」的配置面）。
 *
 * 安全口径：
 * - **API Key 不回显**：已保存时只显示「已保存（留空不修改）」，输入框永远为空开始；
 * - Key 存进 `CredentialsRepository`（安卓 Keystore / 桌面 DPAPI 密文），不进 DataStore；
 * - 服务地址/模型落在 DataStore（非敏感），预设为空时自动用服务商默认值。
 *
 * 「保存」与「测试连接」都会先落盘再取值，避免测到上一轮配置。
 */
@Composable
fun AiRecommendSettingSection() {
    val settingsRepository = koinInject<SettingsRepository>()
    val credentialsRepository = koinInject<CredentialsRepository>()
    val chatClient = koinInject<AiChatClient>()
    val scope = rememberCoroutineScope()
    val scheme = MiuixTheme.colorScheme

    val enabled by settingsRepository.aiRecommendEnabled.collectAsState(initial = false)
    val providerKey by settingsRepository.aiProviderKey.collectAsState(initial = AiProviderPreset.DEEPSEEK.key)
    val savedBaseUrl by settingsRepository.aiBaseUrl.collectAsState(initial = "")
    val savedModel by settingsRepository.aiModel.collectAsState(initial = "")

    var baseUrlInput by remember(savedBaseUrl) { mutableStateOf(savedBaseUrl) }
    var modelInput by remember(savedModel) { mutableStateOf(savedModel) }
    var apiKeyInput by remember { mutableStateOf("") }
    var hasStoredKey by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val preset = AiProviderPreset.fromKey(providerKey)

    LaunchedEffect(Unit) {
        hasStoredKey = readStoredKey(credentialsRepository).isNotEmpty()
    }

    suspend fun persist() {
        settingsRepository.setAiProviderKey(providerKey)
        settingsRepository.setAiBaseUrl(baseUrlInput.trim())
        settingsRepository.setAiModel(modelInput.trim())
        val typed = apiKeyInput.trim()
        if (typed.isNotEmpty()) {
            credentialsRepository.savePassword(AI_API_KEY_SOURCE_ID, typed)
            apiKeyInput = ""
            hasStoredKey = true
        }
    }

    SettingsBlockTitle("AI 推荐")
    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
        SwitchPreference(
            title = "启用 AI 推荐",
            summary = "首页「猜你喜欢」由 AI 读曲库偏好推荐：只发送歌手/专辑统计与抽样曲目，不含文件路径与凭据",
            checked = enabled,
            startAction = { SettingsIcon(TablerIcons.Sparkles) },
            onCheckedChange = { value ->
                scope.launch { settingsRepository.setAiRecommendEnabled(value) }
            },
        )

        // ── 服务商 ──
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = "服务商",
                fontSize = 14.sp,
                color = scheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "全部走 OpenAI 兼容协议；切换后地址/模型留空即用该服务商默认值",
                fontSize = 11.sp,
                color = scheme.onSurfaceVariantSummary,
            )
            Spacer(Modifier.padding(top = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AiProviderPreset.entries.forEach { item ->
                    ProviderPill(
                        label = item.label.substringBefore("（"),
                        selected = item.key == providerKey,
                        onClick = {
                            scope.launch { settingsRepository.setAiProviderKey(item.key) }
                        },
                    )
                }
            }
        }

        AiInputRow(
            label = "服务地址",
            value = baseUrlInput,
            onValueChange = { baseUrlInput = it },
            placeholder = preset.baseUrl.ifBlank { "https://your-host/v1" },
            hint = if (preset.key == AiProviderPreset.CUSTOM.key) {
                "自定义服务商需填完整 baseUrl（到版本段为止，如 https://host/v1）"
            } else {
                "留空使用默认：${preset.baseUrl}"
            },
        )
        AiInputRow(
            label = "模型",
            value = modelInput,
            onValueChange = { modelInput = it },
            placeholder = preset.defaultModel.ifBlank { "model-name" },
            hint = if (preset.models.isEmpty()) {
                "填服务商支持的模型名"
            } else {
                "留空使用默认：${preset.defaultModel}（可选：${preset.models.joinToString("/")}）"
            },
        )
        AiInputRow(
            label = "API Key",
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            placeholder = if (hasStoredKey) "已保存（留空不修改）" else "sk-…",
            hint = "加密保存在本机；不会上传，也不回显",
            password = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MusesButton(
                onClick = {
                    scope.launch {
                        busy = true
                        persist()
                        statusText = "已保存"
                        busy = false
                    }
                },
                enabled = !busy,
            ) { Text("保存") }
            Spacer(Modifier.width(8.dp))
            MusesButton(
                onClick = {
                    scope.launch {
                        busy = true
                        statusText = null
                        persist()
                        val config = AiRecommendConfig(
                            providerKey = providerKey,
                            baseUrl = baseUrlInput.trim(),
                            model = modelInput.trim(),
                            apiKey = readStoredKey(credentialsRepository),
                        )
                        statusText = if (!config.isUsable) {
                            "配置不完整：请填写服务地址、模型与 API Key"
                        } else {
                            runCatching {
                                chatClient.complete(
                                    config = config,
                                    systemPrompt = "你是连通性测试助手。",
                                    userPrompt = "只回复两个字：正常",
                                )
                            }.fold(
                                onSuccess = { "连接成功：${it.trim().take(30)}" },
                                onFailure = { "连接失败：${it.message ?: "未知错误"}" },
                            )
                        }
                        busy = false
                    }
                },
                enabled = !busy,
            ) { Text(if (busy) "请稍候" else "测试连接") }
        }

        statusText?.let { text ->
            Text(
                text = text,
                fontSize = 12.sp,
                color = when {
                    text.startsWith("连接成功") || text == "已保存" -> scheme.primary
                    text.startsWith("连接失败") || text.startsWith("配置不完整") -> scheme.error
                    else -> scheme.onSurfaceVariantSummary
                },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

/** 输入行：标签 + 输入框 + 说明（设置页卡片内统一形态） */
@Composable
private fun AiInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    hint: String,
    password: Boolean = false,
) {
    val scheme = MiuixTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
        Text(text = label, fontSize = 14.sp, color = scheme.onSurface, fontWeight = FontWeight.Medium)
        Spacer(Modifier.padding(top = 4.dp))
        MusesTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = placeholder,
            singleLine = true,
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        )
        Text(
            text = hint,
            fontSize = 11.sp,
            color = scheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** 服务商胶囊（与首页筛选胶囊同视觉） */
@Composable
private fun ProviderPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) scheme.primary else scheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (selected) scheme.onPrimary else scheme.onSurface,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

/** 读已存 Key（只用于存在性判断与发起请求，不回显到 UI） */
private suspend fun readStoredKey(repository: CredentialsRepository): String =
    runCatching { repository.getPassword(AI_API_KEY_SOURCE_ID) }.getOrNull().orEmpty()
