$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
docker compose --project-directory $root down
if ($LASTEXITCODE -ne 0) { throw 'Docker 服务停止失败。' }
