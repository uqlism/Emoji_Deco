<#
.SYNOPSIS
  RunicInk QA VM 内で実行するプロビジョニングスクリプト。
  setup-vm.ps1 から PowerShell Direct で自動呼び出しされる。
  手動で再実行しても冪等 (何度実行しても安全)。
#>
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Step([string]$msg) { Write-Host "[provision] $msg" -ForegroundColor Cyan }
function installed([string]$cmd)  { $null -ne (Get-Command $cmd -ErrorAction SilentlyContinue) }

# ─── winget の準備 ────────────────────────────────────────────────────────────
Write-Step "winget を確認..."
# Windows 11 には winget が標準搭載だが、初回起動直後は更新が必要な場合がある
$null = & winget --version 2>&1
if ($LASTEXITCODE -ne 0) {
    # App Installer (winget) を Microsoft Store から取得
    Add-AppxPackage -RegisterByFamilyName -MainPackage Microsoft.DesktopAppInstaller_8wekyb3d8bbwe
}
Write-Host "  winget: $(winget --version)"

# ─── JDK 21 (Eclipse Adoptium Temurin) — Minecraft 1.21.1 / Forge 52 は Java 21 必須 ──
Write-Step "JDK 21 のインストール..."
$jdkInstalled = winget list --id EclipseAdoptium.Temurin.21.JDK 2>&1 | Select-String "Temurin"
if (-not $jdkInstalled) {
    winget install --id EclipseAdoptium.Temurin.21.JDK `
          --silent --accept-source-agreements --accept-package-agreements
} else {
    Write-Host "  JDK 21 は導入済み"
}

# ─── MS Store の Python/python3 スタブを削除 (winget 版より優先されてしまうため) ──
Write-Step "MS Store Python スタブを削除..."
$stubs = @(
    "$env:LOCALAPPDATA\Microsoft\WindowsApps\python.exe",
    "$env:LOCALAPPDATA\Microsoft\WindowsApps\python3.exe"
)
foreach ($s in $stubs) {
    if (Test-Path $s) { Remove-Item $s -Force; Write-Host "  削除: $s" }
}

# ─── Python 3 ────────────────────────────────────────────────────────────────
Write-Step "Python 3 のインストール..."
# winget でインストール済みかどうかをレジストリで確認 (スタブに惑わされないよう)
$pyInstalled = winget list --id Python.Python.3.13 2>&1 | Select-String "Python.Python"
if (-not $pyInstalled) {
    winget install --id Python.Python.3.13 `
          --silent --accept-source-agreements --accept-package-agreements
}

# ─── Git ─────────────────────────────────────────────────────────────────────
Write-Step "Git のインストール..."
if (-not (installed "git")) {
    winget install --id Git.Git `
          --silent --accept-source-agreements --accept-package-agreements
} else {
    Write-Host "  Git は導入済み: $(git --version)"
}

# ─── PATH を最新化 ────────────────────────────────────────────────────────────
Write-Step "PATH を更新..."
$env:PATH = [System.Environment]::GetEnvironmentVariable("PATH", "Machine") + ";" +
            [System.Environment]::GetEnvironmentVariable("PATH", "User")

# ─── python.exe の実パスを特定 ────────────────────────────────────────────────
$pythonExe = (Get-Command python -ErrorAction SilentlyContinue).Source
if (-not $pythonExe -or $pythonExe -like "*WindowsApps*") {
    $pythonExe = Get-ChildItem "$env:LOCALAPPDATA\Programs\Python\Python*\python.exe" `
                     -ErrorAction SilentlyContinue | Sort-Object -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $pythonExe) { throw "python.exe が見つかりません。手動でインストールしてください。" }
Write-Host "  python: $pythonExe"

# ─── Python 依存パッケージ ────────────────────────────────────────────────────
Write-Step "Python 依存パッケージのインストール..."
& $pythonExe -m pip install --upgrade pip --quiet
& $pythonExe -m pip install pyautogui pygetwindow pyperclip Pillow --quiet
Write-Host "  pyautogui: $(& $pythonExe -c 'import pyautogui; print(pyautogui.__version__)')"

# ─── JAVA_HOME を永続設定 ────────────────────────────────────────────────────
Write-Step "JAVA_HOME を設定..."
$javaExe = (Get-Command java -ErrorAction SilentlyContinue).Source
if ($javaExe) {
    $javaHome = Split-Path (Split-Path $javaExe)
    [System.Environment]::SetEnvironmentVariable("JAVA_HOME", $javaHome, "Machine")
    Write-Host "  JAVA_HOME = $javaHome"
}

# ─── Gradle キャッシュ用ディレクトリ ─────────────────────────────────────────
Write-Step "Gradle キャッシュディレクトリを準備..."
New-Item -ItemType Directory -Path "$env:USERPROFILE\.gradle\wrapper\dists" -Force | Out-Null
Write-Host "  $env:USERPROFILE\.gradle\wrapper\dists"

# ─── Windows Defender 除外 (Gradle ビルド高速化) ──────────────────────────────
Write-Step "Windows Defender の除外設定..."
$excludePaths = @(
    "$env:USERPROFILE\.gradle",
    "$env:USERPROFILE\.m2",
    "C:\VMs"
)
foreach ($p in $excludePaths) {
    Add-MpPreference -ExclusionPath $p -ErrorAction SilentlyContinue
}
Write-Host "  除外パス: $($excludePaths -join ', ')"

# ─── 自動更新を無効化 (テスト中の予期しない再起動を防ぐ) ─────────────────────
Write-Step "Windows Update を無効化..."
Set-Service wuauserv -StartupType Disabled -ErrorAction SilentlyContinue
Stop-Service wuauserv -Force -ErrorAction SilentlyContinue

Write-Host "`n[provision] 完了!" -ForegroundColor Green
