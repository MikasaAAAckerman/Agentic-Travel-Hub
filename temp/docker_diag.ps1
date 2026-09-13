Write-Host "=== 1. Windows Version ==="
(Get-ComputerInfo).WindowsVersion
(Get-ComputerInfo).OsArchitecture

Write-Host ""
Write-Host "=== 2. Hypervisor Status ==="
try {
    $bcd = bcdedit /enum
    $bcd | Select-String "hypervisor"
} catch {
    Write-Host "bcdedit failed"
}

Write-Host ""
Write-Host "=== 3. WSL Status ==="
try {
    wsl --version
    wsl --list --verbose
} catch {
    Write-Host "WSL not found or error"
}

Write-Host ""
Write-Host "=== 4. Windows Features ==="
try {
    Get-WindowsOptionalFeature -Online -FeatureName "Microsoft-Windows-Subsystem-Linux" | Select FeatureName, State
    Get-WindowsOptionalFeature -Online -FeatureName "VirtualMachinePlatform" | Select FeatureName, State
} catch {
    Write-Host "Cannot query features (need admin?)"
}

Write-Host ""
Write-Host "=== 5. Memory Integrity (Core Isolation) ==="
try {
    Get-ItemProperty -Path "HKLM:\SYSTEM\CurrentControlSet\Control\DeviceGuard\Scenarios\HypervisorEnforcedCodeIntegrity" -ErrorAction Stop | Select Enabled
} catch {
    Write-Host "Registry key not found"
}

Write-Host ""
Write-Host "=== 6. Docker Service ==="
try {
    Get-Service "*docker*" -ErrorAction Stop | Select Name, Status, StartType
} catch {
    Write-Host "Docker service not found"
}

Write-Host ""
Write-Host "=== Done ==="
