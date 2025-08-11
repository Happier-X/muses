import { ref, onMounted, onBeforeUnmount } from 'vue'
import { isElectron } from '@/utils/isElectron';

export function useWindowControl() {
    // 是否最大化
    const isMaximized = ref(false)
    /**
     * 获取窗口是否最大化
     */
    const getIsMaximized = async()=>{
        if (isElectron() && window.api) {
            isMaximized.value = await window.api.isMaximized();
        } else {
            isMaximized.value = false;
        }
    }
    onMounted(async () => {
        if (isElectron()) {
            getIsMaximized();
            window.addEventListener('resize', getIsMaximized);
        }
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