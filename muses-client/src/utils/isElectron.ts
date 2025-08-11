/**
 * 检测当前环境是否为Electron应用
 * @returns {boolean} 如果是Electron环境则返回true，否则返回false
 */
export function isElectron(): boolean {
  // 渲染进程环境检测
  if (typeof window !== 'undefined' && typeof window.process === 'object') {
    return window.process.type === 'renderer';
  }

  // 主进程环境检测
  if (typeof process !== 'undefined' && typeof process.versions === 'object' && !!process.versions.electron) {
    return true;
  }

  // 通过尝试导入electron模块进行检测
  try {
    // 动态导入electron模块
    const electron = require('electron');
    return typeof electron !== 'undefined';
  } catch (e) {
    return false;
  }
}