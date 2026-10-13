param([Parameter(Mandatory=$true)][string]$SourceDirectory)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent $PSScriptRoot
$output = Join-Path $root 'docs/store-assets'
$names = @('01-browse','02-tabs','03-history','04-downloads','05-guide','06-settings')
foreach ($kind in @('phone','desktop')) {
    $destination = Join-Path $output $kind
    [void][System.IO.Directory]::CreateDirectory($destination)
    foreach ($name in $names) {
        $matches = @(Get-ChildItem -LiteralPath $SourceDirectory -Recurse -File | Where-Object { $_.Name -eq "$kind-$name.png" })
        if ($matches.Count -ne 1) { throw "Expected exactly one $kind-$name.png, found $($matches.Count)" }
        $header = [System.IO.File]::ReadAllBytes($matches[0].FullName)
        if ($header[24] -ne 8 -or $header[25] -ne 2) { throw "Screenshot must be 24-bit RGB PNG without alpha: $kind-$name.png" }
        $image = [System.Drawing.Image]::FromFile($matches[0].FullName)
        try {
            $width = if ($kind -eq 'phone') { 1080 } else { 1920 }
            $height = if ($kind -eq 'phone') { 1920 } else { 1080 }
            if ($image.Width -ne $width -or $image.Height -ne $height) { throw "Wrong dimensions for $kind-$name.png: $($image.Width) x $($image.Height)" }
            if ($matches[0].Length -gt 8MB) { throw "Screenshot exceeds 8 MB: $kind-$name.png" }
        } finally { $image.Dispose() }
        Copy-Item -LiteralPath $matches[0].FullName -Destination (Join-Path $destination "$name.png") -Force
    }
}
$short = (Get-Content -LiteralPath (Join-Path $output 'short-description-en.txt') -Raw).TrimEnd()
$full = (Get-Content -LiteralPath (Join-Path $output 'full-description-en.txt') -Raw).TrimEnd()
if ($short.Length -gt 80 -or $full.Length -gt 4000) { throw 'Play Store description exceeds its character limit' }
$featureFile = Get-Item -LiteralPath (Join-Path $output 'feature-graphic-1024x500.png')
$feature = [System.Drawing.Image]::FromFile($featureFile.FullName)
try { if ($feature.Width -ne 1024 -or $feature.Height -ne 500 -or $featureFile.Length -gt 15MB) { throw 'Invalid feature graphic' } }
finally { $feature.Dispose() }
$featureHeader = [System.IO.File]::ReadAllBytes($featureFile.FullName)
if ($featureHeader[24] -ne 8 -or $featureHeader[25] -ne 2) { throw 'Feature graphic must be 24-bit RGB PNG without alpha' }
$iconFile = Get-Item -LiteralPath (Join-Path $output 'play-icon-512.png')
$iconHeader = [System.IO.File]::ReadAllBytes($iconFile.FullName)
$icon = [System.Drawing.Image]::FromFile($iconFile.FullName)
try { if ($icon.Width -ne 512 -or $icon.Height -ne 512 -or $iconFile.Length -gt 1MB -or $iconHeader[24] -ne 8 -or $iconHeader[25] -ne 6) { throw 'App icon must be 512x512 32-bit RGBA PNG under 1 MB' } }
finally { $icon.Dispose() }
$manifest = Get-ChildItem -LiteralPath $output -Recurse -File | Where-Object { $_.Extension -eq '.png' -and $_.Name -ne 'logo-source.png' } | ForEach-Object {
    $image = [System.Drawing.Image]::FromFile($_.FullName)
    try {
        $header = [System.IO.File]::ReadAllBytes($_.FullName)
        $channels = if ($header[25] -eq 6) { 4 } else { 3 }
        [pscustomobject]@{ File=$_.FullName.Substring($output.Length+1).Replace('\','/'); Width=$image.Width; Height=$image.Height; BitsPerPixel=$header[24]*$channels; Bytes=$_.Length; SHA256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash }
    }
    finally { $image.Dispose() }
}
$manifest | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $output 'asset-manifest.json') -Encoding utf8
Write-Host "Validated 6 phone and 6 desktop screenshots. Description lengths: $($short.Length)/80, $($full.Length)/4000."
