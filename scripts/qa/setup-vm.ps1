#Requires -RunAsAdministrator
<#
.SYNOPSIS
  RunicInk QA 用 Hyper-V VM を一括セットアップする。

.DESCRIPTION
  Windows 11 VM を作成し、JDK 21 / Python / pip 依存関係を自動インストール。
  別の開発者が同じ環境を再現できるよう、全工程を自動化。

  前提:
    - Windows 11 Pro (Hyper-V 対応)
    - 管理者 PowerShell で実行
    - Windows 11 ISO ファイル
      取得方法: Microsoft 公式サイトで "Windows 11 ディスク イメージ (ISO) をダウンロードする" を検索
      または: Windows の設定 → システム → バージョン情報 から "メディア作成ツール" を入手

.PARAMETER IsoPath
  Windows 11 ISO のフルパス (必須)

.PARAMETER VMName
  VM 名 (デフォルト: RunicInk-QA)

.PARAMETER MemoryGB
  VM に割り当てる RAM (GB, デフォルト: 6)

.PARAMETER CpuCount
  vCPU 数 (デフォルト: 4)

.PARAMETER DiskGB
  仮想ディスクサイズ (GB, デフォルト: 60)

.PARAMETER VMBasePath
  VM ファイルの保存先 (デフォルト: C:\VMs\RunicInk-QA)

.PARAMETER AdminUser
  VM 内ローカル管理者名 (デフォルト: qauser)

.PARAMETER AdminPassword
  VM 内ローカル管理者パスワード (デフォルト: RunicInkQA1!)

.EXAMPLE
  # 管理者 PowerShell で実行
  .\scripts\qa\setup-vm.ps1 -IsoPath "D:\ISOs\win11.iso"
#>
param(
    [Parameter(Mandatory)]
    [ValidateScript({ Test-Path $_ -PathType Leaf })]
    [string]$IsoPath,

    [string]$VMName       = "RunicInk-QA",
    [int]   $MemoryGB     = 6,
    [int]   $CpuCount     = 4,
    [int]   $DiskGB       = 60,
    [string]$VMBasePath   = "C:\VMs\RunicInk-QA",
    [string]$AdminUser    = "qauser",
    [string]$AdminPassword = "RunicInkQA1!"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$PSDefaultParameterValues['*:ErrorAction'] = 'Stop'

function Write-Step([string]$msg) { Write-Host "`n=== $msg ===" -ForegroundColor Cyan }
function Write-OK([string]$msg)   { Write-Host "  [OK] $msg"  -ForegroundColor Green }
function Write-Info([string]$msg) { Write-Host "  [--] $msg" }

# ─────────────────────────────────────────────────────────────────────────────
# Step 1: Hyper-V の確認 / 有効化
# ─────────────────────────────────────────────────────────────────────────────
Write-Step "Hyper-V の確認"

$hvFeature = Get-WindowsOptionalFeature -Online -FeatureName Microsoft-Hyper-V
if ($hvFeature.State -ne "Enabled") {
    Write-Info "Hyper-V を有効化しています..."
    Enable-WindowsOptionalFeature -Online -FeatureName Microsoft-Hyper-V -All -NoRestart | Out-Null
    Write-Host @"

  Hyper-V を有効化しました。再起動後にスクリプトを再実行してください:
    Restart-Computer -Force
    # 再起動後:
    .\scripts\qa\setup-vm.ps1 -IsoPath "$IsoPath"

"@ -ForegroundColor Yellow
    exit 0
}
Write-OK "Hyper-V は有効です"

Import-Module Hyper-V -ErrorAction Stop

# 既存 VM の確認
if (Get-VM -Name $VMName -ErrorAction SilentlyContinue) {
    Write-Host "  [!!] VM '$VMName' は既に存在します。削除して再作成しますか？ [y/N]: " -NoNewline -ForegroundColor Yellow
    $answer = Read-Host
    if ($answer -ne 'y') { Write-Host "中断しました。"; exit 0 }
    Stop-VM -Name $VMName -TurnOff -Force -ErrorAction SilentlyContinue
    # VHD パスを先に取得してから VM を削除
    $oldVhds = Get-VMHardDiskDrive -VMName $VMName | Select-Object -ExpandProperty Path
    Remove-VM -Name $VMName -Force
    $oldVhds | Where-Object { $_ } | ForEach-Object {
        if (Test-Path $_) { Remove-Item $_ -Force }
    }
    Write-OK "既存 VM を削除しました"
}

# ─────────────────────────────────────────────────────────────────────────────
# Step 2: VM と仮想ディスクの作成
# ─────────────────────────────────────────────────────────────────────────────
Write-Step "VM の作成"

New-Item -ItemType Directory -Path $VMBasePath -Force | Out-Null
$vhdPath = Join-Path $VMBasePath "$VMName.vhdx"

New-VHD -Path $vhdPath -SizeBytes ($DiskGB * 1GB) -Dynamic | Out-Null
Write-OK "仮想ディスク作成: $vhdPath ($DiskGB GB)"

$vm = New-VM -Name $VMName `
             -Generation 2 `
             -MemoryStartupBytes ($MemoryGB * 1GB) `
             -VHDPath $vhdPath `
             -SwitchName "Default Switch"   # NAT 付きスイッチ (Hyper-V 既定)

Set-VMProcessor      -VMName $VMName -Count $CpuCount
Set-VMMemory         -VMName $VMName -DynamicMemoryEnabled $false
Set-VM               -VMName $VMName -AutomaticCheckpointsEnabled $false `
                                     -CheckpointType Production

# Secure Boot: QA VM なので無効にする (有効にすると ISO によってはブートローダーが拒否される)
Set-VMFirmware -VMName $VMName -EnableSecureBoot Off

# Virtual TPM (Windows 11 に必須)
Set-VMKeyProtector -VMName $VMName -NewLocalKeyProtector
Enable-VMTPM -VMName $VMName

# Integration Services (Guest Service Interface はロケールによって名前が異なるため任意)
try {
    $gsi = Get-VMIntegrationService -VMName $VMName | Where-Object { $_.Name -match "Guest Service" }
    if ($gsi) { Enable-VMIntegrationService -VMName $VMName -Name $gsi.Name }
} catch { <# 省略可 #> }

Write-OK "VM 作成完了: $VMName  (RAM: ${MemoryGB}GB / vCPU: ${CpuCount} / Disk: ${DiskGB}GB)"

# ─────────────────────────────────────────────────────────────────────────────
# Step 3: autounattend.xml を含む回答ファイル ISO の作成
# ─────────────────────────────────────────────────────────────────────────────
Write-Step "回答ファイル ISO の作成"

# autounattend.xml テンプレートを読み込んでプレースホルダを置換
$templatePath = Join-Path $PSScriptRoot "autounattend.xml"
$xmlContent = (Get-Content $templatePath -Raw -Encoding UTF8) `
    -replace '{{ADMIN_USER}}',     $AdminUser `
    -replace '{{ADMIN_PASSWORD}}', $AdminPassword `
    -replace '{{COMPUTER_NAME}}',  $VMName.ToUpper()

# autounattend.xml を FAT32 VHD に格納する
# (IMAPI2 COM IStream の問題を回避。Windows Setup は FAT32 ディスクルートも自動スキャンする)
$answerVhd = Join-Path $VMBasePath "answerfile.vhdx"
if (Test-Path $answerVhd) { Remove-Item $answerVhd -Force }

$answerXml = Join-Path $env:TEMP "autounattend_qa.xml"
[System.IO.File]::WriteAllText($answerXml, $xmlContent, [System.Text.Encoding]::UTF8)

$null = New-VHD -Path $answerVhd -SizeBytes 64MB -Fixed
$mounted = Mount-VHD -Path $answerVhd -PassThru
try {
    Start-Sleep -Seconds 1
    $diskNum = (Get-VHD -Path $answerVhd).DiskNumber
    $null = Initialize-Disk  -Number $diskNum -PartitionStyle MBR -PassThru
    $part  = New-Partition    -DiskNumber $diskNum -UseMaximumSize -AssignDriveLetter
    $null  = Format-Volume    -DriveLetter $part.DriveLetter -FileSystem FAT32 `
                               -NewFileSystemLabel "AUTOUNATTEND" -Force -Confirm:$false
    Copy-Item $answerXml -Destination "$($part.DriveLetter):\autounattend.xml" -Force
    Write-OK "回答ファイル VHD: $answerVhd (ドライブ $($part.DriveLetter):)"
} finally {
    Dismount-VHD -Path $answerVhd
    Remove-Item  $answerXml -Force -ErrorAction SilentlyContinue
}

Add-VMHardDiskDrive -VMName $VMName -Path $answerVhd -ControllerType SCSI
Write-OK "回答ファイル VHD を VM にアタッチしました"

# ─────────────────────────────────────────────────────────────────────────────
# Step 4: ISO をマウントして VM を起動
# ─────────────────────────────────────────────────────────────────────────────
Write-Step "Windows インストール開始"

# Windows ISO を DVD ドライブにマウント
$dvd0 = Get-VMDvdDrive -VMName $VMName | Select-Object -First 1
if (-not $dvd0) {
    Add-VMDvdDrive -VMName $VMName -Path $IsoPath
} else {
    Set-VMDvdDrive -VMName $VMName -ControllerNumber $dvd0.ControllerNumber `
                   -ControllerLocation $dvd0.ControllerLocation -Path $IsoPath
}

# UEFI ブート順序: DVD を最優先
$firmware = Get-VMFirmware -VMName $VMName
$dvdDrive = $firmware.BootOrder | Where-Object { $_.BootType -eq "Drive" -and $_.Device -is [Microsoft.HyperV.PowerShell.DvdDrive] } | Select-Object -First 1
if ($dvdDrive) {
    Set-VMFirmware -VMName $VMName -FirstBootDevice $dvdDrive
}

Start-VM -Name $VMName
Write-OK "VM を起動しました。Windows のインストールを開始します..."
Write-Info "所要時間の目安: 10〜20 分"

# ─────────────────────────────────────────────────────────────────────────────
# Step 5: インストール完了待機 (PowerShell Direct でポーリング)
# ─────────────────────────────────────────────────────────────────────────────
Write-Step "インストール完了を待機中..."

$credential = New-Object PSCredential(
    $AdminUser,
    (ConvertTo-SecureString $AdminPassword -AsPlainText -Force)
)

$deadline = (Get-Date).AddMinutes(40)
$ready = $false

while ((Get-Date) -lt $deadline) {
    $state = (Get-VM -Name $VMName).State
    if ($state -eq "Off") {
        # インストール後の再起動が完了して停止した場合は起動し直す
        Write-Info "VM が停止しました。再起動します..."
        Start-VM -Name $VMName
        Start-Sleep -Seconds 30
        continue
    }

    # PowerShell Direct でテスト接続
    try {
        $result = Invoke-Command -VMName $VMName -Credential $credential `
                                 -ScriptBlock { $env:COMPUTERNAME } `
                                 -ErrorAction Stop
        $ready = $true
        Write-OK "PowerShell Direct 接続確認: $result"
        break
    } catch {
        Write-Host "." -NoNewline
        Start-Sleep -Seconds 15
    }
}

if (-not $ready) {
    Write-Host "`n[ERROR] 40 分以内にインストールが完了しませんでした。" -ForegroundColor Red
    Write-Host "  VM '$VMName' の状態を Hyper-V マネージャーで確認してください。" -ForegroundColor Yellow
    exit 1
}

Write-Host ""  # 改行

# ─────────────────────────────────────────────────────────────────────────────
# Step 6: VM 内プロビジョニング (JDK / Python / Git / pip deps)
# ─────────────────────────────────────────────────────────────────────────────
Write-Step "VM 内プロビジョニング"

$provisionScript = Join-Path $PSScriptRoot "vm-provision.ps1"
Write-Info "vm-provision.ps1 を VM 内で実行しています (5〜10 分)..."

Invoke-Command -VMName $VMName -Credential $credential -FilePath $provisionScript

Write-OK "プロビジョニング完了"

# ─────────────────────────────────────────────────────────────────────────────
# Step 7: Enhanced Session (GPU 経由の RDP) を有効化
# ─────────────────────────────────────────────────────────────────────────────
Write-Step "Enhanced Session の設定"

Set-VMHost -EnableEnhancedSessionMode $true

# VM 内で RDP と Enhanced Session を有効化
Invoke-Command -VMName $VMName -Credential $credential -ScriptBlock {
    # RDP 有効化
    Set-ItemProperty -Path "HKLM:\System\CurrentControlSet\Control\Terminal Server" `
                     -Name fDenyTSConnections -Value 0
    Enable-NetFirewallRule -DisplayGroup "Remote Desktop" -ErrorAction SilentlyContinue

    # Enhanced Session に必要なサービスを有効化
    Set-Service -Name TermService -StartupType Automatic
    Start-Service -Name TermService -ErrorAction SilentlyContinue
}

Write-OK "Enhanced Session 設定完了"

# ─────────────────────────────────────────────────────────────────────────────
# Step 8: インストール DVD を切り離し、チェックポイント作成
# ─────────────────────────────────────────────────────────────────────────────
Write-Step "後処理"

Get-VMDvdDrive -VMName $VMName | ForEach-Object {
    Set-VMDvdDrive -VMName $VMName -ControllerNumber $_.ControllerNumber `
                   -ControllerLocation $_.ControllerLocation -Path $null
}
Remove-Item $answerIso -Force -ErrorAction SilentlyContinue
Write-OK "DVD を切り離しました"

Checkpoint-VM -VMName $VMName -SnapshotName "baseline"
Write-OK "チェックポイント 'baseline' を作成しました"

# ─────────────────────────────────────────────────────────────────────────────
# 完了
# ─────────────────────────────────────────────────────────────────────────────
Write-Host @"

╔══════════════════════════════════════════════════════════════╗
║  セットアップ完了!                                           ║
╠══════════════════════════════════════════════════════════════╣
║  VM 名:       $VMName
║  ユーザー:    $AdminUser
║  接続方法:    Hyper-V マネージャー → $VMName → 接続          ║
║               (Enhanced Session = GPU 加速 RDP)              ║
║                                                              ║
║  QA 実行環境変数 (ホスト側で設定):                           ║
║    `$env:QA_WINDOW_TITLE = "$VMName"                         ║
║                                                              ║
║  VM をリセットするには:                                       ║
║    Restore-VMCheckpoint -VMName "$VMName" -Name "baseline"   ║
╚══════════════════════════════════════════════════════════════╝
"@ -ForegroundColor Green
