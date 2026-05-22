Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "SSH Key Setup Helper" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

$SERVER_IP = Read-Host "Enter server IP (default: 10.10.10.203)"
if ([string]::IsNullOrWhiteSpace($SERVER_IP)) { $SERVER_IP = "10.10.10.203" }

$SERVER_USER = Read-Host "Enter SSH username (default: root)"
if ([string]::IsNullOrWhiteSpace($SERVER_USER)) { $SERVER_USER = "root" }

Write-Host ""
Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  Server IP: $SERVER_IP" -ForegroundColor White
Write-Host "  Username: $SERVER_USER" -ForegroundColor White
Write-Host ""

$confirm = Read-Host "Continue? (y/n)"
if ($confirm -ne "y" -and $confirm -ne "Y") { exit 0 }

Write-Host ""
Write-Host "[Step 1] Checking SSH key..." -ForegroundColor Green
$sshKeyPath = "$env:USERPROFILE\.ssh\id_rsa.pub"

if (Test-Path $sshKeyPath) {
    Write-Host "SSH key exists" -ForegroundColor Green
    $useExisting = Read-Host "Use existing key? (y/n)"
    if ($useExisting -ne "y" -and $useExisting -ne "Y") {
        Remove-Item "$env:USERPROFILE\.ssh\id_rsa" -ErrorAction SilentlyContinue
        Remove-Item $sshKeyPath -ErrorAction SilentlyContinue
        ssh-keygen -t rsa -b 4096 -f "$env:USERPROFILE\.ssh\id_rsa" -N '""'
    }
} else {
    Write-Host "Generating new SSH key..." -ForegroundColor Yellow
    ssh-keygen -t rsa -b 4096 -f "$env:USERPROFILE\.ssh\id_rsa" -N '""'
}

Write-Host ""
Write-Host "[Step 2] Public key:" -ForegroundColor Green
Get-Content $sshKeyPath
Write-Host ""

Write-Host "[Step 3] Copying to server..." -ForegroundColor Green
Write-Host "Note: Enter password one last time" -ForegroundColor Yellow
Write-Host ""

if (Get-Command ssh-copy-id -ErrorAction SilentlyContinue) {
    ssh-copy-id "${SERVER_USER}@${SERVER_IP}"
} else {
    Write-Host "Using manual method..." -ForegroundColor Yellow
    $publicKey = Get-Content $sshKeyPath
    
    Write-Host "  Creating .ssh directory..." -ForegroundColor Gray
    $cmd1 = "mkdir -p ~/.ssh"
    $cmd2 = "chmod 700 ~/.ssh"
    ssh ${SERVER_USER}@${SERVER_IP} "$cmd1 ; $cmd2"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Cannot connect" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "  Uploading key..." -ForegroundColor Gray
    $tempFile = "$env:TEMP\ssh_key_temp.txt"
    $publicKey | Out-File -FilePath $tempFile -Encoding ascii -NoNewline
    scp $tempFile "${SERVER_USER}@${SERVER_IP}:~/.ssh/authorized_keys.tmp"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Upload failed" -ForegroundColor Red
        Remove-Item $tempFile -ErrorAction SilentlyContinue
        exit 1
    }
    
    Write-Host "  Configuring..." -ForegroundColor Gray
    $cmd3 = "cat ~/.ssh/authorized_keys.tmp >> ~/.ssh/authorized_keys"
    $cmd4 = "chmod 600 ~/.ssh/authorized_keys"
    $cmd5 = "rm -f ~/.ssh/authorized_keys.tmp"
    ssh ${SERVER_USER}@${SERVER_IP} "$cmd3 ; $cmd4 ; $cmd5"
    
    Remove-Item $tempFile -ErrorAction SilentlyContinue
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to copy key" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "SUCCESS: Key copied!" -ForegroundColor Green
Write-Host ""

Write-Host "[Step 4] Testing..." -ForegroundColor Green
$testOutput = ssh ${SERVER_USER}@${SERVER_IP} "echo 'SSH connection successful'" 2>&1
$testSuccess = $LASTEXITCODE -eq 0

if ($testSuccess) {
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "SUCCESS! Setup Complete!" -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "You can now deploy without password:" -ForegroundColor Green
    Write-Host "  .\simple-deploy.ps1" -ForegroundColor White
    Write-Host ""
    Write-Host "Key location:" -ForegroundColor Yellow
    Write-Host "  $env:USERPROFILE\.ssh\id_rsa" -ForegroundColor Gray
} else {
    Write-Host ""
    Write-Host "WARNING: Test failed" -ForegroundColor Yellow
    Write-Host "You may still need password" -ForegroundColor Yellow
}

Write-Host ""
