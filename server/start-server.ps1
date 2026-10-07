$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root '.env'
if (-not (Test-Path -LiteralPath $envFile)) {
  $bytes = New-Object byte[] 32
  [Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
  $token = [Convert]::ToHexString($bytes).ToLowerInvariant()
  @("QIUQIU_ADMIN_TOKEN=$token", 'QIUQIU_PORT=8080', 'QIUQIU_INVITE_TTL=900', 'QIUQIU_MAX_BODY=12582912') | Set-Content -LiteralPath $envFile -Encoding utf8
  Write-Output '已生成 .env 和随机管理员令牌。'
}
docker compose --project-directory $root up -d --build
if ($LASTEXITCODE -ne 0) { throw 'Docker 服务启动失败。请确认 Docker Desktop 已运行。' }
Write-Output '球球服务端已启动：http://localhost:8080/admin'
