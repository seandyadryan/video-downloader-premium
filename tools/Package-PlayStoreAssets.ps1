$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$source = Join-Path $root 'docs/store-assets'
$manifest = Get-Content -LiteralPath (Join-Path $source 'asset-manifest.json') -Raw | ConvertFrom-Json
foreach ($item in $manifest) {
    $hash = (Get-FileHash -LiteralPath (Join-Path $source $item.File) -Algorithm SHA256).Hash
    if ($hash -ne $item.SHA256) { throw "Changed asset: $($item.File). Run Import-StoreScreenshots.ps1 again." }
}
$artifacts = Join-Path $root 'artifacts'
[void][System.IO.Directory]::CreateDirectory($artifacts)
$files = @('ASSETS.md','asset-manifest.json','short-description-en.txt','full-description-en.txt','play-icon-512.png','feature-graphic-1024x500.png','phone','desktop') | ForEach-Object { Join-Path $source $_ }
$zip = Join-Path $artifacts 'PlayStore-English-Assets.zip'
Compress-Archive -LiteralPath $files -DestinationPath $zip -Force
Write-Host "Packaged: $zip"
