param(
    [Parameter(Mandatory=$true)][string]$PlayerPath,
    [string]$JdkPath = $env:JAVA_HOME
)
$ErrorActionPreference = 'Stop'
if (-not $JdkPath) { throw '请用 -JdkPath 指定 JDK 21 或更新版本的目录。' }
$taskRoot = Split-Path -Parent $PSScriptRoot
$compiler = Join-Path $JdkPath 'bin\javac.exe'
$archiveTool = Join-Path $JdkPath 'bin\jar.exe'
$hostArchive = Join-Path $PlayerPath 'app\ffmpeg-x64.dll'
if (-not (Test-Path -LiteralPath $hostArchive)) { throw '未找到播放器安装目录中的创意工坊接口文件。' }
$classDir = Join-Path $taskRoot 'build\classes'
$distDir = Join-Path $taskRoot 'dist'
New-Item -ItemType Directory -Path $classDir,$distDir -Force | Out-Null
$sources = @(Get-ChildItem -LiteralPath (Join-Path $taskRoot 'src\main\java') -Recurse -Filter '*.java' | ForEach-Object FullName)
& $compiler -proc:none --release 21 -encoding UTF-8 -classpath $hostArchive -d $classDir @sources
if ($LASTEXITCODE -ne 0) { throw '源码编译失败。' }
Copy-Item -LiteralPath (Join-Path $taskRoot 'src\main\resources\META-INF') -Destination $classDir -Recurse -Force
$output = Join-Path $distDir 'plugin-local.salt.lyriccompat-0.1.0.spmod'
# 官方安装包布局：根目录中的 classes/，以及可选的 lib/。
& $archiveTool --create --file $output --no-manifest -C (Join-Path $taskRoot 'build') classes -C $taskRoot LICENSE -C (Join-Path $taskRoot 'docs') CREDITS.md
if ($LASTEXITCODE -ne 0) { throw '模组打包失败。' }
$legacyOutput = Join-Path $distDir 'plugin-local.salt.lyriccompat-0.1.0.zip'
Copy-Item -LiteralPath $output -Destination $legacyOutput -Force
Write-Output $output
Write-Output $legacyOutput
