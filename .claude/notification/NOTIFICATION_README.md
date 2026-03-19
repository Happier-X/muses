# Claude Code Notification System

## Files Created

### 1. notify.ps1
- **Location**: `.claude/notification/notify.ps1`
- **Purpose**: PowerShell script that displays Windows system notifications
- **Features**:
  - Sends system notification to Windows notification center
  - Displays customizable title and message
  - No sound (silent notification)

### 2. settings.local.json (Modified)
- **Location**: `.claude/settings.local.json`
- **Change**: Added `PermissionRequest` hook configuration
- **Trigger**: Activates when Claude Code needs user permission confirmation

## How It Works

The system uses Claude Code's `PermissionRequest` hook to trigger notifications when confirmation is needed:

```json
{
  "hooks": {
    "PermissionRequest": [
      {
        "matcher": ".*",
        "hooks": [
          {
            "type": "command",
            "command": "powershell.exe -ExecutionPolicy Bypass -File \"D:\\company_project\\ceam\\backend\\.claude\\notification\\notify.ps1\" -Title \"Claude Code\" -Message \"Confirmation Required\" -Duration 5",
            "async": true
          }
        ]
      }
    ]
  }
}
```

## Configuration Parameters

### notify.ps1 Parameters
- `$Title`: Notification title (default: "Claude Code")
- `$Message`: Notification message (default: "Confirmation Required")
- `$Duration`: Display duration in seconds (default: 5)

### settings.local.json Parameters
- `matcher`: Pattern to match permission requests (".*" matches all)
- `type`: Hook type ("command" for shell commands)
- `async`: Run notification in background (true)

## Testing

### Test the notification script directly:
```powershell
powershell.exe -ExecutionPolicy Bypass -File "D:\company_project\ceam\backend\.claude\notification\notify.ps1" -Title "Test" -Message "This is a test" -Duration 3
```

### Test via Claude Code:
1. Ask Claude Code to perform an action that requires confirmation
2. Switch to another window
3. Wait for system notification to appear

## Customization

### Change notification text:
Edit the command in `settings.local.json`:
```json
"command": "powershell.exe -ExecutionPolicy Bypass -File \"D:\\company_project\\ceam\\backend\\.claude\\notification\\notify.ps1\" -Title \"Your Title\" -Message \"Your Message\" -Duration 5"
```

### Change display duration:
Modify the `-Duration` parameter (in seconds)

### Add different notifications for different tools:
```json
{
  "hooks": {
    "PermissionRequest": [
      {
        "matcher": "Bash.*",
        "hooks": [
          {
            "type": "command",
            "command": "powershell.exe -ExecutionPolicy Bypass -File \"notify.ps1\" -Title \"Bash Command\" -Message \"Confirm bash command\" -Duration 5",
            "async": true
          }
        ]
      },
      {
        "matcher": "Write.*",
        "hooks": [
          {
            "type": "command",
            "command": "powershell.exe -ExecutionPolicy Bypass -File \"notify.ps1\" -Title \"File Write\" -Message \"Confirm file write\" -Duration 5",
            "async": true
          }
        ]
      }
    ]
  }
}
```

## Troubleshooting

### Notification not appearing:
1. Check Windows notification settings
2. Ensure PowerShell execution policy allows scripts
3. Test the script manually

### Configuration not working:
1. Restart Claude Code
2. Verify JSON syntax is correct
3. Check file paths are accurate

## Removal

To remove the notification system:

**Option 1**: Remove hooks from `settings.local.json`:
```json
{
  "$schema": "https://json.schemastore.org/claude-code-settings.json",
  "permissions": {
    // ... existing permissions ...
  }
  // Remove the hooks section
}
```

**Option 2**: Delete the `notify.ps1` file and restart Claude Code

## Technical Details

- **Platform**: Windows 10/11 only
- **PowerShell Version**: Works with PowerShell 5.1+ (default on Windows)
- **Execution Policy**: Uses `-ExecutionPolicy Bypass` to avoid policy restrictions
- **Windows Forms**: Uses `System.Windows.Forms.NotifyIcon` for notifications

## Author

Dylan - 2026-03-09

## Notes

- This system is designed specifically for Windows
- Notifications are silent (no sound)
- Runs asynchronously to avoid blocking Claude Code
- Works for all permission requests by default
