package com.muses.player.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ai.AI_API_KEY_SOURCE_ID
import com.muses.player.core.ai.AiChatClient
import com.muses.player.core.ai.AiRecommendConfig
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.ui.components.MusesButton
import com.muses.player.core.ui.components.MusesIconButton
import com.muses.player.core.ui.components.MusesIconButtonSize
import com.muses.player.core.ui.components.MusesTextField
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.components.SettingsBlockTitle
import com.muses.player.core.ui.theme.LocalBottomChromePadding
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup

/**
 * 设置页「AI 推荐」分组：一级只留总开关 + 「AI 服务」箭头入口，
 * 名称/地址/Key/模型 收进二级页 [AiSettingsScreen]（全部自填，无内置服务商）。
 *
 * 安全口径：
 * - **API Key 不回显**：已保存时只显示「已保存（留空不修改）」，输入框永远为空开始；
 * - Key 存进 `CredentialsRepository`（安卓 Keystore / 桌面 DPAPI 密文），不进 DataStore；
 * - 名称/服务地址/模型落在 DataStore（非敏感）。
 */
@Composable
fun AiRecommendSettingSection(
    onOpenAiSettings: () -> Unit,
) {
    val settingsRepository = koinInject<SettingsRepository>()
    val credentialsRepository = koinInject<CredentialsRepository>()
    val scope = rememberCoroutineScope()

    val enabled by settingsRepository.aiRecommendEnabled.collectAsState(initial = false)
    val serviceName by settingsRepository.aiServiceName.collectAsState(initial = "")
    var hasStoredKey by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasStoredKey = readStoredKey(credentialsRepository).isNotEmpty()
    }

    val summary = if (hasStoredKey) serviceName.ifBlank { "已配置" } else "未配置"

    SettingsBlockTitle("AI 推荐")
    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
        SwitchPreference(
            title = "AI 推荐",
            checked = enabled,
            onCheckedChange = { value ->
                scope.launch { settingsRepository.setAiRecommendEnabled(value) }
            },
        )
        ArrowPreference(
            title = "AI 服务",
            summary = summary,
            onClick = onOpenAiSettings,
        )
    }
}

/**
 * AI 服务配置二级页（设置页「AI 推荐」→「AI 服务」进入）。
 *
 * 顺序：名称 → 服务地址 → API Key → 获取模型列表 → 模型。
 * 模型经 `GET {服务地址}/models` 自动拉取，下拉选择后落盘；也支持手动填写。
 * 「保存」与「测试连接」都会先落盘再取值，避免测到上一轮配置。
 */
@Composable
fun AiSettingsScreen(
    onBack: () -> Unit,
) {
    val settingsRepository = koinInject<SettingsRepository>()
    val credentialsRepository = koinInject<CredentialsRepository>()
    val chatClient = koinInject<AiChatClient>()
    val scope = rememberCoroutineScope()
    val scheme = MiuixTheme.colorScheme

    val savedName by settingsRepository.aiServiceName.collectAsState(initial = "")
    val savedBaseUrl by settingsRepository.aiBaseUrl.collectAsState(initial = "")
    val savedModel by settingsRepository.aiModel.collectAsState(initial = "")

    var nameInput by remember(savedName) { mutableStateOf(savedName) }
    var baseUrlInput by remember(savedBaseUrl) { mutableStateOf(savedBaseUrl) }
    var modelInput by remember(savedModel) { mutableStateOf(savedModel) }
    var apiKeyInput by remember { mutableStateOf("") }
    var hasStoredKey by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var fetchingModels by remember { mutableStateOf(false) }
    var modelOptions by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(Unit) {
        // 已保存的密钥回显到输入框；默认仍由 PasswordVisualTransformation 隐藏，
        // 用户可通过官方 trailingIcon 小眼睛切换明文。
        val storedKey = readStoredKey(credentialsRepository)
        hasStoredKey = storedKey.isNotEmpty()
        apiKeyInput = storedKey
    }

    suspend fun persist() {
        settingsRepository.setAiServiceName(nameInput.trim())
        settingsRepository.setAiBaseUrl(baseUrlInput.trim())
        settingsRepository.setAiModel(modelInput.trim())
        val typed = apiKeyInput.trim()
        if (typed.isNotEmpty()) {
            credentialsRepository.savePassword(AI_API_KEY_SOURCE_ID, typed)
            hasStoredKey = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = scheme.surface,
        topBar = {
            MusesTopBar(
                title = "AI 服务",
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(top = 8.dp)
                .padding(bottom = LocalBottomChromePadding.current),
        ) {
            SettingsBlockTitle("服务配置")
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                AiInputRow(
                    label = "名称",
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = "请输入名称",
                )
                AiInputRow(
                    label = "服务地址",
                    value = baseUrlInput,
                    onValueChange = { baseUrlInput = it },
                    placeholder = "请输入服务地址",
                )
                AiInputRow(
                    label = "API Key",
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    placeholder = if (hasStoredKey) "已保存（留空不修改）" else "请输入 API Key",
                    password = true,
                )

                AiModelRow(
                    modelInput = modelInput,
                    onModelChange = { modelInput = it },
                    modelOptions = modelOptions,
                    fetchingModels = fetchingModels,
                    fetchEnabled = !busy && !fetchingModels,
                    onFetchModels = {
                        scope.launch {
                            fetchingModels = true
                            statusText = null
                            val baseUrl = baseUrlInput.trim()
                            val key = apiKeyInput.trim().ifNotEmpty() ?: readStoredKey(credentialsRepository)
                            if (baseUrl.isBlank() || key.isBlank()) {
                                statusText = "请先填写服务地址与 API Key"
                            } else {
                                runCatching { chatClient.listModels(baseUrl, key) }.fold(
                                    onSuccess = { models ->
                                        modelOptions = models
                                        statusText = if (models.isEmpty()) "该服务返回了空模型列表" else "已获取 ${models.size} 个模型"
                                        if (modelInput.trim().isBlank() && models.isNotEmpty()) {
                                            modelInput = models.first()
                                            scope.launch { settingsRepository.setAiModel(models.first()) }
                                        }
                                    },
                                    onFailure = { statusText = it.message ?: "获取模型列表失败" },
                                )
                            }
                            fetchingModels = false
                        }
                    },
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
                            text.startsWith("连接成功") || text == "已保存" || text.startsWith("已获取") -> scheme.primary
                            text.startsWith("连接失败") || text.startsWith("配置不完整") ||
                                text.startsWith("请先填写") || text.startsWith("获取模型") ||
                                text.startsWith("模型列表") || text == "该服务返回了空模型列表" -> scheme.error
                            else -> scheme.onSurfaceVariantSummary
                        },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

/** 模型行：输入框（支持手填）+ 获取模型列表按钮 + 拉到列表后的胶囊快选 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AiModelRow(
    modelInput: String,
    onModelChange: (String) -> Unit,
    modelOptions: List<String>,
    fetchingModels: Boolean,
    fetchEnabled: Boolean,
    onFetchModels: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
        Text(text = "模型", fontSize = 14.sp, color = scheme.onSurface, fontWeight = FontWeight.Medium)
        Spacer(Modifier.padding(top = 4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MusesTextField(
                value = modelInput,
                onValueChange = onModelChange,
                modifier = Modifier.weight(1f),
                label = "请输入模型",
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            MusesButton(
                onClick = onFetchModels,
                enabled = fetchEnabled,
            ) { Text(if (fetchingModels) "获取中" else "获取模型") }
        }
        if (modelOptions.isNotEmpty()) {
            // 列表可能很长：流式胶囊快速挑选，与之前服务商胶囊同视觉
            Spacer(Modifier.padding(top = 8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                modelOptions.forEach { name ->
                    val selected = name == modelInput
                    Text(
                        text = name,
                        fontSize = 13.sp,
                        color = if (selected) scheme.onPrimary else scheme.onSurface,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) scheme.primary else scheme.surfaceContainerHigh)
                            .clickable { onModelChange(name) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

/** 输入行：标签 + 输入框（设置页卡片内统一形态，无提示文案） */
@Composable
private fun AiInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    password: Boolean = false,
) {
    val scheme = MiuixTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
        Text(text = label, fontSize = 14.sp, color = scheme.onSurface, fontWeight = FontWeight.Medium)
        Spacer(Modifier.padding(top = 4.dp))
        if (password) {
            var visible by remember { mutableStateOf(false) }
            MusesTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = placeholder,
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    MusesIconButton(
                        onClick = { visible = !visible },
                        imageVector = if (visible) TablerIcons.EyeOff else TablerIcons.Eye,
                        contentDescription = if (visible) "隐藏 API Key" else "显示 API Key",
                        size = MusesIconButtonSize.SM,
                        tint = scheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                },
            )
        } else {
            MusesTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = placeholder,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
            )
        }
    }
}

private fun String.ifNotEmpty(): String? = this.trim().takeIf { it.isNotEmpty() }

/** 读已存 Key（只用于存在性判断与发起请求，不回显到 UI） */
private suspend fun readStoredKey(repository: CredentialsRepository): String =
    runCatching { repository.getPassword(AI_API_KEY_SOURCE_ID) }.getOrNull().orEmpty()
