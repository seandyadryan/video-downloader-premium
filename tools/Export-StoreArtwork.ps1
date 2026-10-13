# Deterministic export of the imagegen logo into Android and Play Store assets (Windows).
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent $PSScriptRoot
$output = Join-Path $root 'docs/store-assets'
$source = [System.Drawing.Image]::FromFile((Join-Path $output 'logo-source.png'))
$dark = [System.Drawing.ColorTranslator]::FromHtml('#101715')
$green = [System.Drawing.ColorTranslator]::FromHtml('#64E2BA')
$gold = [System.Drawing.ColorTranslator]::FromHtml('#E7CC85')
$white = [System.Drawing.ColorTranslator]::FromHtml('#EEF6EF')
$muted = [System.Drawing.ColorTranslator]::FromHtml('#A8BBB1')
function Canvas([int]$width, [int]$height, [bool]$alpha = $false) {
    $format = if ($alpha) { [System.Drawing.Imaging.PixelFormat]::Format32bppArgb } else { [System.Drawing.Imaging.PixelFormat]::Format24bppRgb }
    $bitmap = [System.Drawing.Bitmap]::new($width, $height, $format)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    return @($bitmap, $graphics)
}
function Mark($graphics, [float]$x, [float]$y, [float]$size) {
    $graphics.DrawImage($source, [System.Drawing.RectangleF]::new($x + $size*.11, $y + $size*.11, $size*.78, $size*.78))
}
function Save($bitmap, $graphics, $path) {
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose(); $bitmap.Dispose()
}
$foreground, $g = Canvas 1024 1024 $true
$g.Clear([System.Drawing.Color]::Transparent); Mark $g 0 0 1024
Save $foreground $g (Join-Path $root 'app/src/main/res/drawable-nodpi/brand_foreground.png')
$icon, $g = Canvas 512 512 $true
$g.Clear($dark); Mark $g 0 0 512
Save $icon $g (Join-Path $output 'play-icon-512.png')
$feature, $g = Canvas 1024 500
$g.Clear($dark)
$ambient = [System.Drawing.Drawing2D.LinearGradientBrush]::new([System.Drawing.Point]::new(600,0), [System.Drawing.Point]::new(1024,500), [System.Drawing.Color]::FromArgb(0,20,123,86), [System.Drawing.Color]::FromArgb(120,20,123,86))
$g.FillRectangle($ambient, 550, 0, 474, 500); $ambient.Dispose()
$whiteBrush = [System.Drawing.SolidBrush]::new($white)
$greenBrush = [System.Drawing.SolidBrush]::new($green)
$goldBrush = [System.Drawing.SolidBrush]::new($gold)
$mutedBrush = [System.Drawing.SolidBrush]::new($muted)
$brand = [System.Drawing.Font]::new('Segoe UI', 15, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$headline = [System.Drawing.Font]::new('Segoe UI', 56, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$copy = [System.Drawing.Font]::new('Segoe UI', 21, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$small = [System.Drawing.Font]::new('Segoe UI', 16, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$g.FillRectangle($goldBrush, 56, 54, 40, 3)
$g.DrawString('VIDEO DOWNLOADER PREMIUM', $brand, $goldBrush, 56, 75)
$g.DrawString("Browse. Save.`nKeep exploring.", $headline, $whiteBrush, [System.Drawing.RectangleF]::new(50, 136, 610, 158))
$g.DrawString('Tabs, history and your video collection.', $copy, $mutedBrush, 56, 327)
$g.DrawString('One purchase  /  No in-app ads', $small, $greenBrush, 56, 399)
Mark $g 548 14 492
Save $feature $g (Join-Path $output 'feature-graphic-1024x500.png')
foreach ($item in @($source,$brand,$headline,$copy,$small,$whiteBrush,$greenBrush,$goldBrush,$mutedBrush)) { $item.Dispose() }
Write-Host "Store artwork exported: $output"
