$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path $PSScriptRoot -Parent)
New-Item -ItemType Directory -Force checks/calendar-source | Out-Null
$calendarSource = (Get-Content -Raw app/src/main/java/com/qiuqiu/pet/Dialogue.java).Replace('android.icu','com.ibm.icu')
$calendarPath = Join-Path (Get-Location) 'checks/calendar-source/Dialogue.java'
[System.IO.File]::WriteAllText($calendarPath, $calendarSource, (New-Object System.Text.UTF8Encoding($false)))
javac -encoding UTF-8 -cp .tools/icu4j-76.1.jar -d checks/out checks/android/content/Context.java checks/android/content/SharedPreferences.java checks/org/json/JSONObject.java checks/com/qiuqiu/pet/SyncRepository.java app/src/main/java/com/qiuqiu/pet/PetState.java checks/MemoryCheck.java checks/calendar-source/Dialogue.java app/src/main/java/com/qiuqiu/pet/SolarTerms.java checks/CalendarCheck.java app/src/main/java/com/qiuqiu/pet/ActionTimeline.java app/src/main/java/com/qiuqiu/pet/IdleMotion.java checks/ActionCheck.java app/src/main/java/com/qiuqiu/pet/Companion.java checks/CompanionCheck.java checks/MotionCheck.java app/src/main/java/com/qiuqiu/pet/MotionCycle.java app/src/main/java/com/qiuqiu/pet/AiRules.java checks/AiRulesCheck.java
if ($LASTEXITCODE -ne 0) { throw '检查代码编译失败' }
java -cp 'checks/out;.tools/icu4j-76.1.jar' com.qiuqiu.pet.MemoryCheck
if ($LASTEXITCODE -ne 0) { throw '记忆检查失败' }
java -cp 'checks/out;.tools/icu4j-76.1.jar' com.qiuqiu.pet.CalendarCheck
if ($LASTEXITCODE -ne 0) { throw '日历检查失败' }
java -cp 'checks/out' com.qiuqiu.pet.ActionCheck
if ($LASTEXITCODE -ne 0) { throw '动作调度检查失败' }
node checks/preview-check.cjs
if ($LASTEXITCODE -ne 0) { throw '预览逻辑检查失败' }
node checks/interaction-check.cjs
if ($LASTEXITCODE -ne 0) { throw '预览交互冲突检查失败' }

java -cp checks/out com.qiuqiu.pet.CompanionCheck
if ($LASTEXITCODE -ne 0) { throw "陪伴记忆检查失败" }
java -cp checks/out com.qiuqiu.pet.MotionCheck
if ($LASTEXITCODE -ne 0) { throw "144帧循环检查失败" }
java -cp checks/out com.qiuqiu.pet.AiRulesCheck
if ($LASTEXITCODE -ne 0) { throw "AI 记忆规则检查失败" }
node checks/motion-check.cjs
if ($LASTEXITCODE -ne 0) { throw "动作补间与三周期待机检查失败" }
node checks/companion-check.cjs
if ($LASTEXITCODE -ne 0) { throw "预览陪伴检查失败" }
