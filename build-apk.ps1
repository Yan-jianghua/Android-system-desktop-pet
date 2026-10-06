$ErrorActionPreference = 'Stop'
$taskRoot = $PSScriptRoot
$taskGradle = Join-Path $taskRoot '.tools/gradle-8.9/bin/gradle.bat'
if (-not (Test-Path -LiteralPath $taskGradle)) { throw '本地构建工具不存在，请使用 Android Studio 打开项目。' }
& $taskGradle --project-dir $taskRoot --gradle-user-home (Join-Path $taskRoot '.tools/gradle-cache') --no-daemon :app:assembleDebug
if ($LASTEXITCODE -ne 0) { throw 'APK 构建失败，请查看上方错误。' }
New-Item -ItemType Directory -Force (Join-Path $taskRoot 'dist') | Out-Null
Copy-Item -LiteralPath (Join-Path $taskRoot 'app/build/outputs/apk/debug/app-debug.apk') -Destination (Join-Path $taskRoot 'dist/球球桌面宠物-1.10-五秒144帧.apk')

