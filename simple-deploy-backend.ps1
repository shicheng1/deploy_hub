Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Backend Upload & Deploy" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

$PROJECT_ROOT = "D:\01-project\hg_yw"
$SERVER_IP = Read-Host "Enter server IP (default: 10.10.10.203)"
if ([string]::IsNullOrWhiteSpace($SERVER_IP)) { $SERVER_IP = "10.10.10.203" }

$SERVER_USER = Read-Host "Enter SSH username (default: root)"
if ([string]::IsNullOrWhiteSpace($SERVER_USER)) { $SERVER_USER = "root" }

$jarFile = Join-Path $PROJECT_ROOT "ims-admin\target\ims-admin.jar"
$configDir = "/opt/ims/config"
$deployDir = "/opt/ims"
$logDir = "/opt/ims/logs"

Write-Host ""
Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  Server: ${SERVER_USER}@${SERVER_IP}" -ForegroundColor White
Write-Host "  Deploy Dir: $deployDir" -ForegroundColor White
Write-Host ""

$buildNow = Read-Host "Build backend before deploy? (y/n)"
if ($buildNow -eq "y" -or $buildNow -eq "Y") {
    Write-Host ""
    Write-Host "[Step 0] Building backend..." -ForegroundColor Green
    Write-Host "Running: mvn clean package -DskipTests" -ForegroundColor Gray
    Write-Host ""
    
    Set-Location $PROJECT_ROOT
    mvn clean package "-DskipTests"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Build failed!" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "SUCCESS: Build complete" -ForegroundColor Green
}

Write-Host ""
Write-Host "[Step 1] Checking JAR file..." -ForegroundColor Green

if (-Not (Test-Path $jarFile)) {
    Write-Host "ERROR: JAR file not found!" -ForegroundColor Red
    Write-Host "Please build first: mvn clean package -Dmaven.test.skip=true" -ForegroundColor Yellow
    exit 1
}

$fileSize = [math]::Round((Get-Item $jarFile).Length / 1MB, 2)
Write-Host "SUCCESS: Found ims-admin.jar (${fileSize} MB)" -ForegroundColor Green

Write-Host ""
Write-Host "[Step 2] Uploading JAR..." -ForegroundColor Green
Write-Host "Note: Enter password if needed" -ForegroundColor Yellow
Write-Host ""

try {
    scp $jarFile "${SERVER_USER}@${SERVER_IP}:${deployDir}/ims-admin.jar"
    if ($LASTEXITCODE -ne 0) { throw "Upload failed" }
    Write-Host "SUCCESS: JAR uploaded" -ForegroundColor Green
} catch {
    Write-Host "ERROR: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[Step 3] Stopping old service..." -ForegroundColor Green

# Stop gracefully first
$stopCmd = 'ps -ef | grep ''ims-admin.jar'' | grep -v grep | awk ''{print $2}'' | xargs -r kill -15'
ssh ${SERVER_USER}@${SERVER_IP} $stopCmd
Start-Sleep -Seconds 3

# Check if still running
$processCheck = ssh ${SERVER_USER}@${SERVER_IP} "ps -ef | grep 'ims-admin.jar' | grep -v grep | wc -l"
if ($processCheck.Trim() -ne "0") {
    Write-Host "  Force killing..." -ForegroundColor Yellow
    $forceKillCmd = 'ps -ef | grep ''ims-admin.jar'' | grep -v grep | awk ''{print $2}'' | xargs -r kill -9'
    ssh ${SERVER_USER}@${SERVER_IP} $forceKillCmd
    Start-Sleep -Seconds 2
}

Write-Host "SUCCESS: Old service stopped" -ForegroundColor Green

Write-Host ""
Write-Host "[Step 4] Creating directories..." -ForegroundColor Green

ssh ${SERVER_USER}@${SERVER_IP} "mkdir -p $configDir"
ssh ${SERVER_USER}@${SERVER_IP} "mkdir -p $logDir"

Write-Host "SUCCESS: Directories created" -ForegroundColor Green

Write-Host ""
Write-Host "[Step 5] Detecting Java path..." -ForegroundColor Green

$javaPath = ssh ${SERVER_USER}@${SERVER_IP} "which java"
if (-not $javaPath -or $javaPath.Trim() -eq "") {
    Write-Host "  WARNING: java not found in PATH, trying common locations..." -ForegroundColor Yellow
    $javaPath = ssh ${SERVER_USER}@${SERVER_IP} "ls /usr/local/jdk/*/bin/java 2>/dev/null | head -1"
}
if (-not $javaPath -or $javaPath.Trim() -eq "") {
    $javaPath = "/usr/bin/java"
}
$javaPath = $javaPath.Trim()

Write-Host "  Using Java: $javaPath" -ForegroundColor Gray
Write-Host "SUCCESS: Java detected" -ForegroundColor Green

Write-Host ""
Write-Host "[Step 7] Creating systemd service..." -ForegroundColor Green

$serviceFile = @"
[Unit]
Description=IMS Admin Service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=$deployDir
ExecStart=$javaPath -Xms512m -Xmx2048m -jar $deployDir/ims-admin.jar --spring.profiles.active=prod
Restart=on-failure
RestartSec=10
StandardOutput=append:$logDir/app.log
StandardError=append:$logDir/error.log

[Install]
WantedBy=multi-user.target
"@

$tempServiceFile = "$env:TEMP\ims-admin.service"
$serviceFile | Out-File -FilePath $tempServiceFile -Encoding utf8

scp $tempServiceFile "${SERVER_USER}@${SERVER_IP}:/tmp/ims-admin.service"
Remove-Item $tempServiceFile -Force

# Install systemd service
ssh ${SERVER_USER}@${SERVER_IP} "mv /tmp/ims-admin.service /etc/systemd/system/ims-admin.service"
ssh ${SERVER_USER}@${SERVER_IP} "systemctl daemon-reload"
ssh ${SERVER_USER}@${SERVER_IP} "systemctl enable ims-admin.service"

Write-Host "SUCCESS: Systemd service created and enabled" -ForegroundColor Green

Write-Host ""
Write-Host "[Step 8] Starting service..." -ForegroundColor Green

ssh ${SERVER_USER}@${SERVER_IP} "systemctl start ims-admin.service"
Start-Sleep -Seconds 5

Write-Host "SUCCESS: Service started" -ForegroundColor Green

Write-Host ""
Write-Host "[Step 9] Verifying service..." -ForegroundColor Green

Start-Sleep -Seconds 5

# Check service status
$serviceStatus = ssh ${SERVER_USER}@${SERVER_IP} "systemctl is-active ims-admin.service"
if ($serviceStatus.Trim() -eq "active") {
    Write-Host "SUCCESS: Service is running (systemd)" -ForegroundColor Green
} else {
    Write-Host "WARNING: Service may not be running, checking logs..." -ForegroundColor Yellow
    ssh ${SERVER_USER}@${SERVER_IP} "journalctl -u ims-admin.service -n 50 --no-pager"
}

# Health check
$healthCheck = ssh ${SERVER_USER}@${SERVER_IP} "curl -s http://localhost:8011/login | head -c 100"
if ($healthCheck) {
    Write-Host "SUCCESS: Health check passed" -ForegroundColor Green
} else {
    Write-Host "WARNING: Health check failed" -ForegroundColor Yellow
    ssh ${SERVER_USER}@${SERVER_IP} "tail -50 $logDir/app.log"
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Deployment Complete!" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Service URL: http://${SERVER_IP}:8011" -ForegroundColor White
Write-Host "API Docs: http://${SERVER_IP}:8011/swagger-ui/index.html" -ForegroundColor White
Write-Host ""
Write-Host "Service Management Commands:" -ForegroundColor Yellow
Write-Host "  Start: ssh ${SERVER_USER}@${SERVER_IP} 'systemctl start ims-admin.service'" -ForegroundColor Gray
Write-Host "  Stop: ssh ${SERVER_USER}@${SERVER_IP} 'systemctl stop ims-admin.service'" -ForegroundColor Gray
Write-Host "  Restart: ssh ${SERVER_USER}@${SERVER_IP} 'systemctl restart ims-admin.service'" -ForegroundColor Gray
Write-Host "  Status: ssh ${SERVER_USER}@${SERVER_IP} 'systemctl status ims-admin.service'" -ForegroundColor Gray
Write-Host "  Logs: ssh ${SERVER_USER}@${SERVER_IP} 'journalctl -u ims-admin.service -f'" -ForegroundColor Gray
Write-Host "  App Logs: ssh ${SERVER_USER}@${SERVER_IP} 'tail -f $logDir/app.log'" -ForegroundColor Gray
Write-Host ""
Write-Host "Auto-start on boot: ENABLED" -ForegroundColor Green
Write-Host ""
