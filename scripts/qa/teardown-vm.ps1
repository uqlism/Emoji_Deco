#Requires -RunAsAdministrator
<#
.SYNOPSIS
  RunicInk QA VM を完全に削除する。

.PARAMETER VMName
  削除する VM 名 (デフォルト: RunicInk-QA)

.PARAMETER VMBasePath
  VM ファイルの保存先 (デフォルト: C:\VMs\RunicInk-QA)

.EXAMPLE
  .\scripts\qa\teardown-vm.ps1
#>
param(
    [string]$VMName     = "RunicInk-QA",
    [string]$VMBasePath = "C:\VMs\RunicInk-QA"
)

Import-Module Hyper-V -ErrorAction Stop

$vm = Get-VM -Name $VMName -ErrorAction SilentlyContinue
if (-not $vm) {
    Write-Host "VM '$VMName' は存在しません。" -ForegroundColor Yellow
    exit 0
}

Write-Host "VM '$VMName' を削除します。この操作は取り消せません。[y/N]: " -NoNewline -ForegroundColor Yellow
if ((Read-Host) -ne 'y') { Write-Host "中断しました。"; exit 0 }

if ($vm.State -ne "Off") {
    Stop-VM -Name $VMName -TurnOff -Force
    Write-Host "  VM を強制停止しました。"
}

Remove-VM -Name $VMName -Force
Write-Host "  VM を削除しました。"

if (Test-Path $VMBasePath) {
    Remove-Item $VMBasePath -Recurse -Force
    Write-Host "  ファイルを削除しました: $VMBasePath"
}

Write-Host "完了。" -ForegroundColor Green
