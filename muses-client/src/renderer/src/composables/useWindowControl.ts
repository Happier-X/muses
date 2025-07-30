import { ref, onMounted } from 'vue'

export function useWindowControl() {
    // 是否最大化
    const isMaximized = ref(false)
    onMounted(async () => {
        isMaximized.value = await window.api.isMaximized()
    })
    /**
     * 最小化
     */
    const handleMinimize = () => {
        window.api.minimizeWindow()
    }
    /**
     * 切换窗口大小
     */
    const handleToggleWindowSize = () => {
        window.api.toggleWindowSize()
        isMaximized.value = !isMaximized.value
    }
    /**
     * 关闭
     */
    const handleClose =  () => {
        window.api.closeWindow()
    }

    return { isMaximized, handleMinimize, handleToggleWindowSize, handleClose }
}
