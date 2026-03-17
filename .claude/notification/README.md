# Claude Code 通知系统

**Author**: Dylan
**创建时间**: 2026-03-09
**版本**: v1.0

## 概述

这是一个专门用于 Claude Code 通知系统的目录，包含所有相关的脚本和配置文档。

## 文件结构

```
notification/
├── README.md                          # 本文件 - 系统概览和入口
├── notify.ps1                         # PowerShell 通知脚本
├── NOTIFICATION_GUIDE.md              # 中文配置指南
└── NOTIFICATION_README.md             # 英文说明文档
```

## 文件说明

### 1. notify.ps1
- **用途**: PowerShell 通知脚本
- **功能**: 发送 Windows 系统通知
- **特点**:
  - 可自定义标题、消息和显示时长
  - 支持消息框和系统托盘通知
  - 错误处理机制

### 2. NOTIFICATION_GUIDE.md
- **用途**: 中文配置指南
- **内容**: 详细的使用说明、配置方法、常见问题解答
- **适用人群**: 中文用户

### 3. NOTIFICATION_README.md
- **用途**: 英文说明文档
- **内容**: 完整的技术文档、配置示例、故障排除
- **适用人群**: 英文用户或需要详细技术信息的用户

## 快速开始

### 1. 配置通知系统

在 `.claude/settings.local.json` 中添加以下配置：

```json
{
  "hooks": {
    "PermissionRequest": [
      {
        "matcher": ".*",
        "hooks": [
          {
            "type": "command",
            "command": "powershell.exe -ExecutionPolicy Bypass -File 'D:\\\\company_project\\\\ceam\\\\backend\\\\.claude\\\\notification\\\\notify.ps1' -Title 'Claude Code' -Message 'Confirmation Required' -Duration 5",
            "async": false
          }
        ]
      }
    ]
  }
}
```

### 2. 测试通知

手动运行脚本测试：

```powershell
powershell.exe -ExecutionPolicy Bypass -File "D:\company_project\ceam\backend\.claude\notification\notify.ps1" -Title "测试" -Message "这是一个测试通知" -Duration 3
```

### 3. 重启 Claude Code

重启 Claude Code 使配置生效。

## 详细文档

- **中文用户**: 请阅读 [NOTIFICATION_GUIDE.md](NOTIFICATION_GUIDE.md)
- **英文用户**: 请阅读 [NOTIFICATION_README.md](NOTIFICATION_README.md)

## 自定义配置

您可以通过修改脚本的参数来自定义通知：

- `-Title`: 通知标题
- `-Message`: 通知消息内容
- `-Duration`: 通知显示时长（秒）

## 故障排除

### 通知没有显示
1. 检查 Windows 通知设置
2. 验证 PowerShell 执行策略
3. 手动运行脚本测试

### 配置不生效
1. 重启 Claude Code
2. 检查 JSON 配置格式
3. 确认脚本路径正确

## 系统要求

- **操作系统**: Windows 10/11
- **PowerShell**: 5.1 或更高版本
- **Claude Code**: 支持 hooks 配置的版本

## 技术细节

- **通知机制**: Windows Forms NotifyIcon
- **执行策略**: Bypass 模式
- **异步执行**: 可配置（async 参数）

## 版本历史

- **v1.0** (2026-03-09): 初始版本，创建通知系统目录结构

## 维护人

Dylan

---

**注意**: 本通知系统仅适用于 Windows 系统。如需在其他操作系统上使用，请参考相应系统的通知机制。