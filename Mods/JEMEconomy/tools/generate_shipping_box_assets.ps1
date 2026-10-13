Add-Type -AssemblyName System.Drawing
$target = Join-Path $PSScriptRoot '..\src\main\resources\assets\jem_economy\textures\block'
New-Item -ItemType Directory -Force -Path $target | Out-Null

function New-Texture([string]$name, [scriptblock]$paint) {
    $bitmap = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    & $paint $bitmap
    $bitmap.Save((Join-Path $target $name), [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

$dark = [System.Drawing.Color]::FromArgb(255, 67, 39, 23)
$edge = [System.Drawing.Color]::FromArgb(255, 91, 53, 29)
$wood = [System.Drawing.Color]::FromArgb(255, 151, 94, 48)
$light = [System.Drawing.Color]::FromArgb(255, 185, 124, 67)
$mark = [System.Drawing.Color]::FromArgb(255, 225, 197, 132)
$iron = [System.Drawing.Color]::FromArgb(255, 84, 78, 70)

New-Texture 'shipping_box_side.png' {
    param($b)
    for ($y=0; $y -lt 16; $y++) { for ($x=0; $x -lt 16; $x++) { $b.SetPixel($x,$y,$wood) } }
    for ($x=0; $x -lt 16; $x++) { $b.SetPixel($x,0,$dark); $b.SetPixel($x,15,$dark) }
    for ($y=0; $y -lt 16; $y++) { $b.SetPixel(0,$y,$dark); $b.SetPixel(15,$y,$dark) }
    foreach ($y in 4,9,13) { for ($x=1; $x -lt 15; $x++) { $b.SetPixel($x,$y,$edge) } }
    foreach ($y in 2,6,11) { for ($x=2; $x -lt 14; $x+=5) { $b.SetPixel($x,$y,$light) } }
}
New-Texture 'shipping_box_front.png' {
    param($b)
    for ($y=0; $y -lt 16; $y++) { for ($x=0; $x -lt 16; $x++) { $b.SetPixel($x,$y,$wood) } }
    for ($x=0; $x -lt 16; $x++) { $b.SetPixel($x,0,$dark); $b.SetPixel($x,15,$dark) }
    for ($y=0; $y -lt 16; $y++) { $b.SetPixel(0,$y,$dark); $b.SetPixel(15,$y,$dark) }
    foreach ($y in 4,10) { for ($x=1; $x -lt 15; $x++) { $b.SetPixel($x,$y,$edge) } }
    for ($x=4; $x -le 11; $x++) { for ($y=6; $y -le 9; $y++) { $b.SetPixel($x,$y,$mark) } }
    for ($x=6; $x -le 9; $x++) { $b.SetPixel($x,5,$mark); $b.SetPixel($x,10,$mark) }
    $b.SetPixel(7,7,$dark); $b.SetPixel(8,7,$dark); $b.SetPixel(7,8,$dark); $b.SetPixel(8,8,$dark)
}
New-Texture 'shipping_box_top.png' {
    param($b)
    for ($y=0; $y -lt 16; $y++) { for ($x=0; $x -lt 16; $x++) { $b.SetPixel($x,$y,$wood) } }
    for ($x=0; $x -lt 16; $x++) { $b.SetPixel($x,0,$dark); $b.SetPixel($x,15,$dark) }
    for ($y=0; $y -lt 16; $y++) { $b.SetPixel(0,$y,$dark); $b.SetPixel(15,$y,$dark) }
    for ($x=3; $x -le 12; $x++) { $b.SetPixel($x,3,$edge); $b.SetPixel($x,12,$dark) }
    for ($y=3; $y -le 12; $y++) { $b.SetPixel(3,$y,$edge); $b.SetPixel(12,$y,$dark) }
    for ($x=6; $x -le 9; $x++) { $b.SetPixel($x,2,$iron); $b.SetPixel($x,3,$iron) }
    for ($x=5; $x -le 10; $x++) { $b.SetPixel($x,6,$light); $b.SetPixel($x,9,$edge) }
}
