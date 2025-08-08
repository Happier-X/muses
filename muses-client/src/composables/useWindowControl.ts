import { ref, onMounted, onBeforeUnmount } from 'vue'

export function useWindowControl() {
    // 是否最大化
    const isMaximized = ref(false)
    /**
     * 获取窗口是否最大化
     */
    const getIsMaximized = async()=>{
        isMaximized.value = await window.api.isMaximized()
    }
    onMounted(async () => {
        getIsMaximized()
        window.addEventListener('resize',getIsMaximized)
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
    }
    /**
     * 关闭
     */
    const handleClose =  () => {
        window.api.closeWindow()
    }
    onBeforeUnmount(()=>{
        window.removeEventListener('resize',getIsMaximized)
    })

    return { isMaximized, handleMinimize, handleToggleWindowSize, handleClose }
}
