$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
New-Item -ItemType Directory -Path (Join-Path $taskRoot 'dist') -Force | Out-Null
$output = Join-Path $taskRoot 'dist\salt-lyric-compat-source.zip'
# 使用明确的文件清单，避免把本地曲库、构建产物或私人资料打包进去。
$allowFiles = @('README.md', 'LICENSE', '.gitignore')
Add-Type -AssemblyName System.IO.Compression
$stream = [System.IO.File]::Open($output, [System.IO.FileMode]::Create)
$zip = [System.IO.Compression.ZipArchive]::new($stream, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    $files = @($allowFiles | ForEach-Object { Join-Path $taskRoot $_ })
    foreach ($directory in @('src', 'docs', 'scripts')) {
        $files += @(Get-ChildItem -LiteralPath (Join-Path $taskRoot $directory) -Recurse -File | ForEach-Object FullName)
    }
    foreach ($file in $files) {
        $relative = [System.IO.Path]::GetRelativePath($taskRoot, $file).Replace('\', '/')
        $entry = $zip.CreateEntry($relative, [System.IO.Compression.CompressionLevel]::Optimal)
        $inputStream = [System.IO.File]::OpenRead($file)
        $entryStream = $entry.Open()
        try { $inputStream.CopyTo($entryStream) }
        finally { $entryStream.Dispose(); $inputStream.Dispose() }
    }
} finally { $zip.Dispose(); $stream.Dispose() }
$assets = @('plugin-local.salt.lyriccompat-0.1.0.zip', 'plugin-local.salt.lyriccompat-0.1.0.spmod',
    'salt-lyric-compat-source.zip')
$lines = foreach ($asset in $assets) {
    $hash = (Get-FileHash -LiteralPath (Join-Path $taskRoot "dist\$asset") -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  $asset"
}
Set-Content -LiteralPath (Join-Path $taskRoot 'dist\SHA256SUMS.txt') -Value $lines -Encoding utf8NoBOM
Write-Output $output
