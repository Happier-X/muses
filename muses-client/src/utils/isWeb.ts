/**
 * 检测当前环境是否为Web应用
 * @returns {boolean} 如果是Web环境则返回true，否则返回false
 */
export function isWeb(): boolean {
    let isWeb = true
    const userAgent = window.navigator.userAgent
    if(userAgent.includes('Electron')){
        isWeb = false
    }
    return isWeb
}