import { getCurrentWindow } from '@tauri-apps/api/window'
import { ref, onMounted } from 'vue'

export function useWindowControl() {
    // 窗口对象
    const appWindow = getCurrentWindow()
    // 是否最大化
    const isMaximized = ref(false)
    onMounted(async () => {
        isMaximized.value = await appWindow.isMaximized()
        appWindow.onResized(async () => {
            isMaximized.value = await appWindow.isMaximized()
        })
    })
    /**
     * 最小化
     */
    const handleMinimize = async () => {
        await appWindow.minimize()
    }
    /**
     * 切换屏幕大小
     */
    const handleToggleScreenSize = async () => {
        await appWindow.toggleMaximize()
    }
    /**
     * 关闭
     */
    const handleClose = async () => {
        await appWindow.close()
    }

    return { isMaximized, handleMinimize, handleToggleScreenSize, handleClose }
}
