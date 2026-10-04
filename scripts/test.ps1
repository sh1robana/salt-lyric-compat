param(
    [Parameter(Mandatory=$true)][string]$PlayerPath,
    [string]$JdkPath = $env:JAVA_HOME,
    [string]$CorpusList
)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot 'build.ps1') -PlayerPath $PlayerPath -JdkPath $JdkPath
$hostArchive = Join-Path $PlayerPath 'app\ffmpeg-x64.dll'
$classes = Join-Path $taskRoot 'build\classes'
$testClasses = Join-Path $taskRoot 'build\test-classes'
New-Item -ItemType Directory -Path $testClasses -Force | Out-Null
$testSources = @(Get-ChildItem -LiteralPath (Join-Path $taskRoot 'src\test\java') -Recurse -Filter '*.java' | ForEach-Object FullName)
& (Join-Path $JdkPath 'bin\javac.exe') -proc:none --release 21 -encoding UTF-8 -classpath "$classes;$hostArchive" -d $testClasses @testSources
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
$testArgs = @('-classpath', "$testClasses;$classes;$hostArchive", 'local.salt.lyriccompat.CompatTests')
if ($CorpusList) { $testArgs += $CorpusList }
& (Join-Path $JdkPath 'bin\java.exe') @testArgs
if ($LASTEXITCODE -ne 0) { throw 'Tests failed.' }
& (Join-Path $JdkPath 'bin\java.exe') -classpath "$testClasses;$hostArchive" 'local.salt.lyriccompat.PackagingTests' (Join-Path $taskRoot 'dist\plugin-local.salt.lyriccompat-0.1.0.zip')
if ($LASTEXITCODE -ne 0) { throw 'Packaging tests failed.' }
