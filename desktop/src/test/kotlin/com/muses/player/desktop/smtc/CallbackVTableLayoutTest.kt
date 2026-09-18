package com.muses.player.desktop.smtc

import com.sun.jna.Native
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 回归：COM 回调 vtable 必须对 JNA 反射可访问。
 *
 * JNA 的 `Structure.deriveLayout` 经 `Field.get` 跨包读取字段值，JVM 访问检查要求
 * 「字段 + 声明类」同时为 public。该类历史上是 Kotlin `private`（JVM 编译为包级私有），
 * 一旦构造即抛 `IllegalAccessException`（线上表现为「订阅 SMTC 按键事件失败」）。
 * 本测试只做构造：`Structure.<init>` → `deriveLayout` 正是当初的报错路径。
 */
class CallbackVTableLayoutTest {

    @Test
    fun constructsAndLaysOutSevenFunctionPointers() {
        // IUnknown 3 槽 + IInspectable 3 槽 + Invoke 1 槽 = 7 个函数指针
        val vtable = CallbackVTable()
        assertEquals(7 * Native.POINTER_SIZE, vtable.size())
    }
}
