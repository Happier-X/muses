package com.muses.player

import android.media.AudioDeviceInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 音频输出设备移除 → 暂停的判定逻辑单测（08-18-bt-car-disconnect-pause）。
 * 纯函数 isDisruptiveDeviceRemoved 与 Android 运行时解耦，JVM 可直接测试。
 */
class AudioDeviceRemovalPolicyTest {

    private fun removed(type: Int, isSink: Boolean = true) =
        AudioPlayerPlugin.RemovedOutputDevice(type = type, isSink = isSink)

    @Test
    fun `bt_a2dp_remove_triggers_pause`() {
        assertTrue(
            AudioPlayerPlugin.isDisruptiveDeviceRemoved(
                listOf(removed(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)),
            ),
        )
    }

    @Test
    fun `bt_sco_remove_triggers_pause`() {
        assertTrue(
            AudioPlayerPlugin.isDisruptiveDeviceRemoved(
                listOf(removed(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)),
            ),
        )
    }

    @Test
    fun `wired_earphones_remove_triggers_pause`() {
        assertTrue(
            AudioPlayerPlugin.isDisruptiveDeviceRemoved(
                listOf(
                    removed(AudioDeviceInfo.TYPE_WIRED_HEADSET),
                    removed(AudioDeviceInfo.TYPE_WIRED_HEADPHONES),
                ),
            ),
        )
    }

    @Test
    fun `usb_audio_remove_triggers_pause`() {
        assertTrue(
            AudioPlayerPlugin.isDisruptiveDeviceRemoved(
                listOf(
                    removed(AudioDeviceInfo.TYPE_USB_DEVICE),
                    removed(AudioDeviceInfo.TYPE_USB_HEADSET),
                    removed(AudioDeviceInfo.TYPE_USB_ACCESSORY),
                ),
            ),
        )
    }

    @Test
    fun `dock_remove_triggers_pause`() {
        assertTrue(
            AudioPlayerPlugin.isDisruptiveDeviceRemoved(
                listOf(removed(AudioDeviceInfo.TYPE_DOCK)),
            ),
        )
    }

    @Test
    fun `non_sink_source_removal_does_not_trigger`() {
        assertFalse(
            AudioPlayerPlugin.isDisruptiveDeviceRemoved(
                listOf(removed(AudioDeviceInfo.TYPE_BUILTIN_MIC, isSink = false)),
            ),
        )
    }

    @Test
    fun `speaker_and_hdmi_removal_does_not_trigger`() {
        assertFalse(
            AudioPlayerPlugin.isDisruptiveDeviceRemoved(
                listOf(
                    removed(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER),
                    removed(AudioDeviceInfo.TYPE_HDMI),
                ),
            ),
        )
    }

    @Test
    fun `empty_removal_does_not_trigger`() {
        assertFalse(AudioPlayerPlugin.isDisruptiveDeviceRemoved(emptyList()))
    }

    @Test
    fun `mixed_list_with_disruptive_type_triggers`() {
        assertTrue(
            AudioPlayerPlugin.isDisruptiveDeviceRemoved(
                listOf(
                    removed(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER),
                    removed(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP),
                ),
            ),
        )
    }
}