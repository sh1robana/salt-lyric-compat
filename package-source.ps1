$ErrorActionPreference = 'Stop'
$taskRoot = $PSScriptRoot
$output = Join-Path $taskRoot 'dist\salt-lyric-compat-0.1.0-source.zip'
$allowFiles = @('README.md', 'VALIDATION.md', 'CREDITS.md', 'LICENSE', 'RELEASE_NOTES.md',
    'build.ps1', 'test.ps1', 'package-source.ps1', '.gitignore')
Add-Type -AssemblyName System.IO.Compression
$stream = [System.IO.File]::Open($output, [System.IO.FileMode]::Create)
$zip = [System.IO.Compression.ZipArchive]::new($stream, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    $files = @($allowFiles | ForEach-Object { Join-Path $taskRoot $_ })
    $files += @(Get-ChildItem -LiteralPath (Join-Path $taskRoot 'src') -Recurse -File | ForEach-Object FullName)
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
    'salt-lyric-compat-0.1.0-source.zip')
$lines = foreach ($asset in $assets) {
    $hash = (Get-FileHash -LiteralPath (Join-Path $taskRoot "dist\$asset") -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  $asset"
}
Set-Content -LiteralPath (Join-Path $taskRoot 'dist\SHA256SUMS.txt') -Value $lines -Encoding utf8NoBOM
Write-Output $output
