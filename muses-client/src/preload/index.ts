import { contextBridge, ipcRenderer } from 'electron'
import { electronAPI } from '@electron-toolkit/preload'

// Custom APIs for renderer
const api = {
  // 关闭窗口
  closeWindow:()=>{
    ipcRenderer.send('close-window')
  },
  // 最小化窗口
  minimizeWindow:()=>{
    ipcRenderer.send('minimize-window')
  },
  // 切换窗口大小
  toggleWindowSize:()=>{
    ipcRenderer.send('toggle-window-size')
  },
  // 判断窗口是否最大化
  isMaximized:()=>{
    return ipcRenderer.invoke('is-maximized')
  },
}

// Use `contextBridge` APIs to expose Electron APIs to
// renderer only if context isolation is enabled, otherwise
// just add to the DOM global.
if (process.contextIsolated) {
  try {
    contextBridge.exposeInMainWorld('electron', electronAPI)
    contextBridge.exposeInMainWorld('api', api)
  } catch (error) {
    console.error(error)
  }
} else {
  // @ts-ignore (define in dts)
  window.electron = electronAPI
  // @ts-ignore (define in dts)
  window.api = api
}
