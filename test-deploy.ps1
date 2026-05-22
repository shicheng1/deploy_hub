param(
    [string]$SourceFile,
    [string]$TargetPath = "C:\deploy-test"
)

Write-Host "=== 模拟远程部署测试 ===" -ForegroundColor Cyan

if (-not (Test-Path $SourceFile)) {
    Write-Host "错误：源文件不存在: $SourceFile" -ForegroundColor Red
    exit 1
}

New-Item -ItemType Directory -Path $TargetPath -Force | Out-Null

$fileName = [System.IO.Path]::GetFileName($SourceFile)
$targetFile = Join-Path $TargetPath $fileName

Copy-Item -Path $SourceFile -Destination $targetFile -Force

if (Test-Path $targetFile) {
    Write-Host "✅ 文件上传成功: $targetFile" -ForegroundColor Green
    
    Write-Host "`n=== 执行部署脚本 ===" -ForegroundColor Cyan
    Write-Host "模拟执行安装命令..." -ForegroundColor Yellow
    
    Start-Sleep -Seconds 2
    
    Write-Host "✅ 部署完成！" -ForegroundColor Green
    Write-Host "`n部署结果:" -ForegroundColor Cyan
    Write-Host "  - 源文件: $SourceFile"
    Write-Host "  - 目标路径: $targetFile"
    Write-Host "  - 文件大小: $((Get-Item $targetFile).Length / 1KB) KB"
}
else {
    Write-Host "❌ 文件上传失败" -ForegroundColor Red
    exit 1
}
