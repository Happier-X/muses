package com.muses.player.core.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshableStateTest {
    @Test
    fun 手动刷新重新读取且继续接收实时变更() = runTest {
        var stored = "旧数据"
        var reads = 0
        val changes = MutableSharedFlow<String>()
        val source = flow {
            reads++
            emit(stored)
            changes.collect { emit(it) }
        }
        val holder = RefreshableState(source, backgroundScope, "初始值")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { holder.state.collect {} }
        runCurrent()
        assertEquals("旧数据", holder.state.value)

        stored = "重新查询的数据"
        holder.refresh()
        runCurrent()
        assertEquals(2, reads)
        assertEquals("重新查询的数据", holder.state.value)

        changes.emit("实时变更")
        runCurrent()
        assertEquals("实时变更", holder.state.value)
    }

    @Test
    fun 读取失败保留当前内容() = runTest {
        var fail = false
        val holder = RefreshableState(flow {
            if (fail) error("读取失败")
            emit("已有内容")
            awaitCancellation()
        }, backgroundScope, "初始值")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { holder.state.collect {} }
        runCurrent()
        fail = true
        assertFailsWith<IllegalStateException> { holder.refresh() }
        assertEquals("已有内容", holder.state.value)
    }
}
