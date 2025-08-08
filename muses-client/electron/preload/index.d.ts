import { ElectronAPI } from '@electron-toolkit/preload'

declare global {
  interface Window {
    electron: ElectronAPI
    api: {
      closeWindow: () => void
      minimizeWindow: () => void
      toggleWindowSize: () => void
      isMaximized: () => Promise<boolean>
    }
  }
}
