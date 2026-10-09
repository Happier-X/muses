param(
    [Parameter(Mandatory = $true)]
    [string]$Directory
)

$ErrorActionPreference = 'Stop'
# 在打包产物内固定策略，用户双击 MSI 安装时也不会自动重启电脑。
# 只改安装包的 Property 表，不执行安装、不修改系统或用户数据。
$packages = @(Get-ChildItem -LiteralPath $Directory -Filter '*.msi' -Recurse -File)
if ($packages.Count -eq 0) { throw "未找到 MSI 产物：$Directory" }

$installer = New-Object -ComObject WindowsInstaller.Installer
try {
    foreach ($package in $packages) {
        $database = $installer.OpenDatabase($package.FullName, 1)
        try {
            $view = $database.OpenView("SELECT ``Value`` FROM ``Property`` WHERE ``Property`` = 'REBOOT'")
            try {
                $view.Execute()
                $record = $view.Fetch()
                $exists = $null -ne $record
                if ($exists) { [void][System.Runtime.InteropServices.Marshal]::FinalReleaseComObject($record) }
            } finally {
                $view.Close()
                [void][System.Runtime.InteropServices.Marshal]::FinalReleaseComObject($view)
            }

            $sql = if ($exists) {
                "UPDATE ``Property`` SET ``Value`` = 'ReallySuppress' WHERE ``Property`` = 'REBOOT'"
            } else {
                "INSERT INTO ``Property`` (``Property``, ``Value``) VALUES ('REBOOT', 'ReallySuppress')"
            }
            $view = $database.OpenView($sql)
            try { $view.Execute() } finally {
                $view.Close()
                [void][System.Runtime.InteropServices.Marshal]::FinalReleaseComObject($view)
            }
            $database.Commit()
        } finally {
            [void][System.Runtime.InteropServices.Marshal]::FinalReleaseComObject($database)
        }

        # 重新打开已提交的文件核验，策略缺失时阻止发布。
        $database = $installer.OpenDatabase($package.FullName, 0)
        try {
            $view = $database.OpenView("SELECT ``Value`` FROM ``Property`` WHERE ``Property`` = 'REBOOT'")
            try {
                $view.Execute()
                $record = $view.Fetch()
                if ($null -eq $record -or $record.StringData(1) -ne 'ReallySuppress') {
                    throw "MSI 禁止重启策略校验失败：$($package.FullName)"
                }
            } finally {
                if ($null -ne $record) { [void][System.Runtime.InteropServices.Marshal]::FinalReleaseComObject($record) }
                $view.Close()
                [void][System.Runtime.InteropServices.Marshal]::FinalReleaseComObject($view)
            }
        } finally {
            [void][System.Runtime.InteropServices.Marshal]::FinalReleaseComObject($database)
        }
        Write-Host "MSI 已禁止自动重启：$($package.FullName)"
    }
} finally {
    [void][System.Runtime.InteropServices.Marshal]::FinalReleaseComObject($installer)
}
