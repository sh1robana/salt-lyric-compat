param(
    [Parameter(Mandatory=$true)][string]$PlayerPath,
    [string]$JdkPath = $env:JAVA_HOME
)
$ErrorActionPreference = 'Stop'
if (-not $JdkPath) { throw 'Provide -JdkPath pointing to JDK 21 or newer.' }
$taskRoot = $PSScriptRoot
$compiler = Join-Path $JdkPath 'bin\javac.exe'
$archiveTool = Join-Path $JdkPath 'bin\jar.exe'
$hostArchive = Join-Path $PlayerPath 'app\ffmpeg-x64.dll'
if (-not (Test-Path -LiteralPath $hostArchive)) { throw 'Installed host API archive not found.' }
$classDir = Join-Path $taskRoot 'build\classes'
$distDir = Join-Path $taskRoot 'dist'
New-Item -ItemType Directory -Path $classDir,$distDir -Force | Out-Null
$sources = @(Get-ChildItem -LiteralPath (Join-Path $taskRoot 'src\main\java') -Recurse -Filter '*.java' | ForEach-Object FullName)
& $compiler -proc:none --release 21 -encoding UTF-8 -classpath $hostArchive -d $classDir @sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
Copy-Item -LiteralPath (Join-Path $taskRoot 'src\main\resources\META-INF') -Destination $classDir -Recurse -Force
$output = Join-Path $distDir 'plugin-local.salt.lyriccompat-0.1.0.spmod'
# Official distribution layout: archive root classes/ plus optional lib/.
& $archiveTool --create --file $output --no-manifest -C (Join-Path $taskRoot 'build') classes -C $taskRoot LICENSE -C $taskRoot CREDITS.md
if ($LASTEXITCODE -ne 0) { throw 'Packaging failed.' }
$legacyOutput = Join-Path $distDir 'plugin-local.salt.lyriccompat-0.1.0.zip'
Copy-Item -LiteralPath $output -Destination $legacyOutput -Force
Write-Output $output
Write-Output $legacyOutput
