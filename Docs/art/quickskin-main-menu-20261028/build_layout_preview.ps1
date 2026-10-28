param(
    [Parameter(Mandatory = $true)]
    [string]$Background,

    [Parameter(Mandatory = $true)]
    [string]$Title,

    [Parameter(Mandatory = $true)]
    [string]$Output
)

Add-Type -AssemblyName System.Drawing

$canvas = [System.Drawing.Bitmap]::new(1280, 720)
$graphics = [System.Drawing.Graphics]::FromImage($canvas)
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$backgroundImage = [System.Drawing.Image]::FromFile($Background)
$titleImage = [System.Drawing.Image]::FromFile($Title)

$graphics.DrawImage($backgroundImage, [System.Drawing.Rectangle]::new(0, 0, 1280, 720))
$graphics.DrawImage($titleImage, [System.Drawing.Rectangle]::new(346, 22, 588, 202))

$shadowBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(190, 0, 0, 0))
$textBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
$font = [System.Drawing.Font]::new('Segoe UI Semibold', 22, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$labels = @('Singleplayer', 'Multiplayer', 'Realms', 'Mods', 'Options', 'Quit Game', 'Change Skin')
$buttonY = @(216, 268, 320, 372, 424, 476, 528)

for ($index = 0; $index -lt $labels.Count; $index++) {
    $format = [System.Drawing.StringFormat]::new()
    $format.Alignment = [System.Drawing.StringAlignment]::Center
    $format.LineAlignment = [System.Drawing.StringAlignment]::Center
    $rectangle = [System.Drawing.RectangleF]::new(850, $buttonY[$index], 380, 40)
    $shadowRectangle = [System.Drawing.RectangleF]::new(853, $buttonY[$index] + 3, 380, 40)
    $graphics.DrawString($labels[$index], $font, $shadowBrush, $shadowRectangle, $format)
    $graphics.DrawString($labels[$index], $font, $textBrush, $rectangle, $format)
    $format.Dispose()
}

$previewPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(180, 255, 255, 255), 3)
$previewPen.DashStyle = [System.Drawing.Drawing2D.DashStyle]::Dash
$graphics.DrawRectangle($previewPen, 44, 396, 234, 308)
$previewFont = [System.Drawing.Font]::new('Segoe UI Semibold', 18, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$previewFormat = [System.Drawing.StringFormat]::new()
$previewFormat.Alignment = [System.Drawing.StringAlignment]::Center
$previewFormat.LineAlignment = [System.Drawing.StringAlignment]::Center
$graphics.DrawString("QUICK SKIN`nPLAYER PREVIEW", $previewFont, $shadowBrush, [System.Drawing.RectangleF]::new(46, 398, 234, 308), $previewFormat)
$graphics.DrawString("QUICK SKIN`nPLAYER PREVIEW", $previewFont, $textBrush, [System.Drawing.RectangleF]::new(44, 396, 234, 308), $previewFormat)

$parent = Split-Path -Parent $Output
if ($parent) {
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
}
$canvas.Save($Output, [System.Drawing.Imaging.ImageFormat]::Png)

$previewFormat.Dispose()
$previewFont.Dispose()
$previewPen.Dispose()
$font.Dispose()
$textBrush.Dispose()
$shadowBrush.Dispose()
$titleImage.Dispose()
$backgroundImage.Dispose()
$graphics.Dispose()
$canvas.Dispose()
