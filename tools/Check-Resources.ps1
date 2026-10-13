$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$res = Join-Path $root 'app/src/main/res'
[xml]$base = Get-Content -LiteralPath (Join-Path $res 'values/strings.xml') -Raw -Encoding utf8
$required = @($base.resources.string | Where-Object { $_.translatable -ne 'false' } | ForEach-Object { $_.name })
$packs = @(Get-ChildItem -LiteralPath $res -Directory | Where-Object { $_.Name -like 'values-*' -and (Test-Path -LiteralPath (Join-Path $_.FullName 'strings.xml')) })
foreach ($pack in $packs) {
    [xml]$xml = Get-Content -LiteralPath (Join-Path $pack.FullName 'strings.xml') -Raw -Encoding utf8
    $keys = @($xml.resources.string | ForEach-Object { $_.name })
    $missing = @($required | Where-Object { $_ -notin $keys })
    if ($missing.Count -gt 0) { throw "$($pack.Name) missing: $($missing -join ', ')" }
    if (@($keys | Select-Object -Unique).Count -ne $keys.Count) { throw "$($pack.Name) duplicate keys" }
    foreach ($string in $xml.resources.string) {
        if ([string]::IsNullOrWhiteSpace($string.InnerText)) { throw "$($pack.Name) empty: $($string.name)" }
    }
}
$languages = @(Get-Content -LiteralPath (Join-Path $root 'app/src/main/assets/languages.txt') | Where-Object { $_ })
if ($languages.Count -ne $packs.Count + 1) { throw 'Language picker and resource packs disagree' }
Write-Host "Validated $($languages.Count) languages, $($required.Count) translated strings per pack."
