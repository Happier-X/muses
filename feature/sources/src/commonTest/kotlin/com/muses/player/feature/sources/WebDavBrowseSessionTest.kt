package com.muses.player.feature.sources

import androidx.lifecycle.viewModelScope
import com.muses.player.core.webdav.WebDavClient
import com.muses.player.core.webdav.WebDavItem
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class WebDavBrowseSessionTest {
    private class Client : WebDavClient {
        var read: suspend (String) -> List<WebDavItem> = { emptyList() }
        override fun authenticate(username: String, password: String) = Unit
        override suspend fun list(url: String) = read(url)
        override suspend fun probe(baseUrl: String) = true
        override suspend fun get(url: String, dest: File): File = error("本测试不下载")
        override suspend fun put(url: String, source: File): Unit = error("本测试不写入")
        override suspend fun delete(url: String): Unit = error("本测试不删除")
        override suspend fun move(source: String, dest: String): Unit = error("本测试不移动")
        override suspend fun getString(url: String): String? = null
    }

    private fun workflow(block: suspend TestScope.(WebDavBrowseViewModel, Client) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val client = Client()
        val vm = WebDavBrowseViewModel(client)
        try { block(vm, client) } finally {
            vm.endSession()
            vm.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `取消后重开只预选表单目录不保留临时勾选`() = workflow { vm, _ ->
        vm.init("multiple", "/", "https://example.com", "user", "fake", listOf("/saved"))
        runCurrent()
        vm.toggleSelection("/draft")
        vm.endSession()
        vm.init("multiple", "/", "https://example.com", "user", "fake", listOf("/saved"))
        runCurrent()
        assertEquals(setOf("/saved"), vm.browseState.value.selectedPaths)
    }

    @Test
    fun `旧会话迟到的读取结果不能覆盖新面板`() = workflow { vm, client ->
        val old = CompletableDeferred<List<WebDavItem>>()
        var reads = 0
        client.read = {
            if (reads++ == 0) withContext(NonCancellable) { old.await() }
            else listOf(WebDavItem("新目录", "https://example.com/new", true))
        }
        vm.init("multiple", "/", "https://example.com", "user", "fake")
        runCurrent()
        vm.endSession()
        vm.init("multiple", "/", "https://example.com", "user", "changed")
        runCurrent()
        old.complete(listOf(WebDavItem("旧目录", "https://example.com/old", true)))
        runCurrent()
        assertEquals(listOf("新目录"), vm.browseState.value.directories.map { it.basename })
        assertNull(vm.browseState.value.errorMessage)
    }

    @Test
    fun `关闭面板会取消加载并清空会话`() = workflow { vm, client ->
        val pending = CompletableDeferred<List<WebDavItem>>()
        client.read = { pending.await() }
        vm.init("edit-multiple", "/", "https://example.com", "user", "fake", listOf("/saved"))
        runCurrent()
        vm.endSession()
        runCurrent()
        assertFalse(vm.browseState.value.isLoading)
        assertEquals(emptySet(), vm.browseState.value.selectedPaths)
        assertNull(vm.browseState.value.errorMessage)
    }
}
