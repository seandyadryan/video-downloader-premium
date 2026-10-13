# Maintainer tool only; the application never calls a translation service.
param([int]$Throttle = 6)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$res = Join-Path $root 'app/src/main/res'
[xml]$source = Get-Content -LiteralPath (Join-Path $res 'values/strings.xml') -Raw -Encoding utf8
$strings = @($source.resources.string | Where-Object { $_.translatable -ne 'false' } | ForEach-Object {
    @{ Key = $_.name; Value = $_.InnerText }
})
# All languages in this release have complete UI resources. More can be added here.
$tags = 'af am ar as az be bg bn bs ca ceb co cs cy da de el eo es et eu fa fi fil fr fy ga gd gl gu ha haw he hi hmn hr ht hu hy id ig is it ja jv ka kk km kn ko ku ky la lb lo lt lv mg mi mk ml mn mr ms mt my ne nl no ny or pa pl ps pt pt-BR ro ru sd si sk sl sm sn so sq sr st su sv sw ta te tg th tk tr tt ug uk ur uz vi xh yi yo zh-CN zh-TW zu'.Split(' ')
$cache = Join-Path $PSScriptRoot 'translation-cache'
$overridesPath = Join-Path $PSScriptRoot 'indonesian-overrides.json'
[void][System.IO.Directory]::CreateDirectory($cache)
$results = $tags | ForEach-Object -Parallel {
    $tag = $_; $items = $using:strings; $resDir = $using:res; $cacheDir = $using:cache; $correctionsFile = $using:overridesPath
    $ErrorActionPreference = 'Stop'
    $cacheFile = Join-Path $cacheDir "$tag.json"
    $translated = @{}
    if (Test-Path -LiteralPath $cacheFile) {
        $old = Get-Content -LiteralPath $cacheFile -Raw -Encoding utf8 | ConvertFrom-Json -AsHashtable
        $translated = $old
    }
    function Translate([string]$value) {
        $language = switch ($tag) { 'fil' { 'tl' } 'pt-BR' { 'pt' } default { $tag } }
        $endpoint = 'https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=' + $language + '&dt=t&q=' + [Uri]::EscapeDataString($value)
        for ($attempt = 0; $attempt -lt 5; $attempt++) {
            try {
                $reply = Invoke-RestMethod -Uri $endpoint -TimeoutSec 35
                return (@($reply[0] | ForEach-Object { $_[0] }) -join '')
            } catch { if ($attempt -eq 4) { throw }; Start-Sleep -Seconds (2 + $attempt * 3) }
        }
    }
    try {
        $missingItems = @($items | Where-Object { [string]::IsNullOrWhiteSpace($translated[$_.Key]) })
        if ($missingItems.Count -gt 0) {
            # Newlines are a boundary; retry individual strings if a service merges lines.
            for ($start = 0; $start -lt $missingItems.Count; $start += 16) {
                $batch = @($missingItems[$start..([Math]::Min($start + 15, $missingItems.Count - 1))])
                $joined = ($batch | ForEach-Object { $_.Value }) -join "`n"
                $output = @( (Translate $joined) -split "`r?`n" | Where-Object { $_.Trim().Length -gt 0 } )
                if ($output.Count -eq $batch.Count) {
                    for ($i = 0; $i -lt $batch.Count; $i++) { $translated[$batch[$i].Key] = $output[$i].Trim() }
                } else {
                    foreach ($item in $batch) { $translated[$item.Key] = Translate $item.Value }
                }
                Start-Sleep -Milliseconds 150
            }
            $translated | ConvertTo-Json | Set-Content -LiteralPath $cacheFile -Encoding utf8
        }
        $qualifier = switch ($tag) {
            'zh-CN' { 'b+zh+Hans' } 'zh-TW' { 'b+zh+Hant' } 'pt-BR' { 'pt-rBR' }
            'he' { 'iw' } 'id' { 'in' } 'yi' { 'ji' } default { if ($tag.Length -gt 2) { 'b+' + $tag } else { $tag } }
        }
        $directory = Join-Path $resDir "values-$qualifier"
        [void][System.IO.Directory]::CreateDirectory($directory)
        $lines = @('<?xml version="1.0" encoding="utf-8"?>', '<resources>')
        if ($tag -eq 'id') {
            $corrections = Get-Content -LiteralPath $correctionsFile -Raw -Encoding utf8 | ConvertFrom-Json -AsHashtable
            foreach ($key in $corrections.Keys) { $translated[$key] = $corrections[$key] }
        }
        foreach ($item in $items) {
            $value = $translated[$item.Key]
            if ([string]::IsNullOrWhiteSpace($value)) { throw "Missing $($item.Key)" }
            $escaped = [System.Security.SecurityElement]::Escape($value.Replace('\', '\\').Replace('"', '\"').Replace("'", "\'")).Replace('`', '')
            $lines += "    <string name=`"$($item.Key)`" formatted=`"false`">$escaped</string>"
        }
        $lines += '</resources>'
        [System.IO.File]::WriteAllLines((Join-Path $directory 'strings.xml'), $lines, [System.Text.UTF8Encoding]::new($false))
        @{ Tag = $tag; Success = $true }
        Write-Host "Translated $tag"
    } catch { Write-Warning "Translation failed for ${tag}: $($_.Exception.Message)"; @{ Tag = $tag; Success = $false } }
} -ThrottleLimit $Throttle
$complete = @($results | Where-Object { $_.Success } | ForEach-Object {
    $languageTag = $_.Tag
    switch ($languageTag) { 'zh-CN' { 'zh-Hans' } 'zh-TW' { 'zh-Hant' } default { $languageTag } }
})
$assetDirectory = Join-Path $root 'app/src/main/assets'
[void][System.IO.Directory]::CreateDirectory($assetDirectory)
[System.IO.File]::WriteAllLines((Join-Path $assetDirectory 'languages.txt'), @('en') + @($complete | Sort-Object), [System.Text.UTF8Encoding]::new($false))
Write-Host "Complete language packs: $($complete.Count + 1)"
if (@($results | Where-Object { -not $_.Success }).Count -gt 0) { exit 1 }
