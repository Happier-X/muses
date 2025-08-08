import { ipcMain, BrowserWindow } from 'electron'

export function registerIpc(mainWindow: BrowserWindow,app:Electron.App) {
    // 关闭窗口
    ipcMain.on('close-window', () => {
        mainWindow.close()
    })
    // 最小化窗口
    ipcMain.on('minimize-window', () => {
        mainWindow.minimize()
    })
    // 切换窗口大小
    ipcMain.on('toggle-window-size', () => {
        if (mainWindow.isMaximized()) {
            mainWindow.unmaximize()
        } else {
            mainWindow.maximize()
        }
    })
    // 窗口是否最大化
    ipcMain.handle('is-maximized', () => {
        return mainWindow.isMaximized()    
    })
}