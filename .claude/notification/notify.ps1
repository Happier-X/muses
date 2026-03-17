# Claude Code Notification Script
# Author: Dylan
# Date: 2026-03-09
# Purpose: Send system notification when Claude Code needs user confirmation

param(
    [string]$Title = "Claude Code",
    [string]$Message = "Confirmation Required",
    [int]$Duration = 5
)

try {
    # Add Windows Forms assembly
    Add-Type -AssemblyName System.Windows.Forms

    # Create notification icon with explicit icon
    $notification = New-Object System.Windows.Forms.NotifyIcon
    $notification.Icon = [System.Drawing.SystemIcons]::Information
    $notification.BalloonTipIcon = [System.Windows.Forms.ToolTipIcon]::Info
    $notification.BalloonTipTitle = $Title
    $notification.BalloonTipText = $Message
    $notification.Visible = $true

    # Force the notification to appear on screen
    $notification.ShowBalloonTip($Duration * 1000)

    # Also show a message box as backup
    $result = [System.Windows.Forms.MessageBox]::Show(
        "$Message`n`n(System notification also sent to tray)",
        $Title,
        [System.Windows.Forms.MessageBoxButtons]::OK,
        [System.Windows.Forms.MessageBoxIcon]::Information
    )

    # Wait for notification display
    Start-Sleep -Seconds $Duration

    # Clean up resources
    $notification.Dispose()
} catch {
    # If error occurs, write to console and continue
    Write-Host "Notification error: $($_.Exception.Message)"
}
