# Claude Code 通知系统配置指南

**Author**: Dylan
**创建时间**: 2026-03-09
**版本**: v1.0

## 概述

本指南说明如何在 Windows 11 上为 Claude Code 配置系统通知功能，当 Claude Code 需要用户确认时，会自动发送系统通知和提示音，避免因切换窗口而错过确认。

## 文件说明

### 1. notify.ps1
- **位置**: `.claude/notification/notify.ps1`
- **用途**: PowerShell 通知脚本
- **功能**:
  - 发送 Windows 系统通知
  - 播放提示音（800Hz，持续 500ms）
  - 可自定义标题、消息内容和显示时长

### 2. settings.local.json
- **位置**: `.claude/settings.local.json`
- **配置项**: `hooks.PermissionRequest`
- **触发条件**: 当 Claude Code 需要权限确认时

## 配置原理

配置使用了 Claude Code 的 `PermissionRequest` hook，该 hook 在需要权限确认时触发：

```json
{
  "hooks": {
    "PermissionRequest": [
      {
        "matcher": ".*",
        "hooks": [
          {
            "type": "command",
            "command": "powershell.exe -ExecutionPolicy Bypass -File \"D:\\company_project\\ceam\\backend\\.claude\\notification\\notify.ps1\" -Title \"Claude Code 需要确认\" -Message \"请在确认对话框中做出选择\" -Duration 5",
            "async": true
          }
        ]
      }
    ]
  }
}
```

- **matcher**: ".*" 匹配所有权限请求
- **type**: "command" 表示执行 shell 命令
- **command**: PowerShell 命令调用通知脚本
- **async**: true 表示异步执行，不会阻塞 Claude Code

## 自定义配置

### 修改通知参数

您可以修改 PowerShell 脚本的参数来定制通知：

```powershell
# 示例：修改通知参数
powershell.exe -ExecutionPolicy Bypass -File "notify.ps1" `
    -Title "自定义标题" `
    -Message "自定义消息内容" `
    -Duration 10
```

### 修改提示音

在 `notify.ps1` 脚本中修改频率和时长：

```powershell
# 频率：800Hz（高音）到 200Hz（低音）
# 时长：500ms（0.5秒）
[console]::beep(800, 500)
```

### 添加其他触发条件

您可以为不同的工具添加不同的通知：

```json
{
  "hooks": {
    "PermissionRequest": [
      {
        "matcher": "Bash.*",
        "hooks": [
          {
            "type": "command",
            "command": "powershell.exe -ExecutionPolicy Bypass -File \"notify.ps1\" -Title \"Bash 命令需要确认\" -Message \"请确认是否执行此命令\"",
            "async": true
          }
        ]
      },
      {
        "matcher": "Write.*",
        "hooks": [
          {
            "type": "command",
            "command": "powershell.exe -ExecutionPolicy Bypass -File \"notify.ps1\" -Title \"文件写入需要确认\" -Message \"请确认是否写入文件\"",
            "async": true
          }
        ]
      }
    ]
  }
}
```

## 测试通知功能

### 方法 1：手动测试脚本

直接运行 PowerShell 脚本测试通知：

```powershell
powershell.exe -ExecutionPolicy Bypass -File "D:\company_project\ceam\backend\.claude\notification\notify.ps1"
```

### 方法 2：触发权限确认

在 Claude Code 中执行一个需要确认的命令：

```bash
# 示例：执行一个未被允许的命令
rm -rf /
```

此时应该会收到系统通知和提示音。

## 常见问题

### Q1: 通知没有显示

**解决方案**:
1. 检查 Windows 通知设置是否开启
2. 检查 PowerShell 执行策略是否允许脚本运行
3. 尝试手动运行 `notify.ps1` 脚本

### Q2: 提示音没有播放

**解决方案**:
1. 检查系统音量是否开启
2. 确认系统没有静音
3. 检查音频设备是否正常工作

### Q3: 通知频繁弹出

**解决方案**:
1. 可以通过设置 `Duration` 参数减少通知显示时间
2. 或者考虑只在特定情况下显示通知（修改 matcher）

### Q4: 配置不生效

**解决方案**:
1. 重启 Claude Code
2. 检查 JSON 配置格式是否正确
3. 确认 PowerShell 脚本路径是否正确

## 技术细节

### Windows 通知系统

使用 `System.Windows.Forms.NotifyIcon` 类发送通知：
- `BalloonTipTitle`: 通知标题
- `BalloonTipText`: 通知内容
- `BalloonTipIcon`: 通知图标类型
- `ShowBalloonTip`: 显示通知方法

### PowerShell 执行策略

使用 `-ExecutionPolicy Bypass` 参数绕过执行策略限制：
- `-ExecutionPolicy Bypass`: 不加载配置文件，不阻止运行脚本
- 适用于临时脚本执行

### 异步执行

设置 `async: true` 让通知在后台运行：
- 不阻塞 Claude Code 的正常执行
- 避免等待通知完成

## 高级配置

### 添加声音文件

如果您想使用自定义声音文件：

```powershell
# 修改 notify.ps1
$player = New-Object System.Media.SoundPlayer
$player.SoundLocation = "path\to\your\sound.wav"
$player.Play()
```

### 添加更多视觉效果

可以在通知中添加更多视觉效果：

```powershell
# 修改 notify.ps1
$notification.Visible = $true
$notification.ShowBalloonTip(5000, "Claude Code", "需要确认", [System.Windows.Forms.ToolTipIcon]::Warning)
```

## 卸载通知功能

如果不再需要通知功能，可以：

1. 删除 `settings.local.json` 中的 `hooks` 配置
2. 或删除 `notify.ps1` 脚本文件
3. 重启 Claude Code

## 版本历史

- **v1.0** (2026-03-09): 初始版本，实现基础通知功能

## 维护人

Dylan

---

**注意**: 本功能仅适用于 Windows 系统，其他系统需要使用不同的通知机制。
