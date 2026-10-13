param(
    [string]$OutputRoot = (Join-Path $PSScriptRoot '..\src\main\resources\assets\jem_economy\textures\item')
)

Add-Type -AssemblyName System.Drawing

$palettes = @{
    copper = @('#4B1F15','#7A321C','#A94B27','#D96D39','#FFB36A','#FFF0BD','#612617','#F08A50')
    silver = @('#202331','#3B4052','#62687A','#9DA4B4','#D6DAE3','#FFFFFF','#303442','#BBC1CF')
    gold   = @('#5B2B05','#8E4704','#C66B02','#F2A900','#FFD84A','#FFF3A6','#713504','#FFBE20')
    aether = @('#06343A','#07545A','#087D7E','#12AAA1','#5ED8C6','#D7FFF1','#6F5736','#28C8B6')
    arcane = @('#231044','#3C126A','#5D1792','#8725C0','#C86AF0','#FFE39B','#8B4C21','#D69B45')
}

function C([string]$hex) {
    return [System.Drawing.ColorTranslator]::FromHtml($hex)
}

function New-Canvas {
    return [System.Drawing.Bitmap]::new(32, 32, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
}

function In-Diamond([int]$x,[int]$y,[int]$cx,[int]$cy,[int]$radius) {
    return ([math]::Abs($x-$cx) + [math]::Abs($y-$cy)) -le $radius
}

function Paint-Base([System.Drawing.Bitmap]$bmp,[object[]]$p) {
    for($y=0;$y -lt 32;$y++) {
        for($x=0;$x -lt 32;$x++) {
            $dx=$x-15.5; $dy=$y-15.5; $r=[math]::Sqrt($dx*$dx+$dy*$dy)
            if($r -gt 14.25) { continue }
            $shade = if($r -gt 12.35){2}elseif($r -gt 10.5){1}else{0}
            if($shade -eq 2) { $color=C $p[2] }
            elseif($shade -eq 1) { $color=C $p[1] }
            else { $color=C $p[2] }
            if(($x+$y) -lt 23 -and $r -gt 8.5) { $color=C $p[4] }
            elseif(($x+$y) -gt 40 -and $r -gt 7.5) { $color=C $p[0] }
            elseif($shade -eq 0 -and (($x+$y)%7 -eq 0)) { $color=C $p[3] }
            $bmp.SetPixel($x,$y,$color)
        }
    }
    for($y=0;$y -lt 32;$y++) { for($x=0;$x -lt 32;$x++) {
        $dx=$x-15.5; $dy=$y-15.5; $r=[math]::Sqrt($dx*$dx+$dy*$dy)
        if($r -ge 9.6 -and $r -le 10.5) { $bmp.SetPixel($x,$y,(C $p[0])) }
        if((In-Diamond $x $y 16 3 1) -or (In-Diamond $x $y 16 28 1) -or (In-Diamond $x $y 3 16 1) -or (In-Diamond $x $y 28 16 1)) { $bmp.SetPixel($x,$y,(C $p[5])) }
    }}
}

function Paint-Sprout([System.Drawing.Bitmap]$bmp,[object[]]$p) {
    for($y=13;$y -le 24;$y++){ $bmp.SetPixel(15,$y,(C $p[0])); $bmp.SetPixel(16,$y,(C $p[5])) }
    for($y=10;$y -le 18;$y++){ for($x=7;$x -le 15;$x++){
        if(($x+$y -ge 21) -and ($x+$y -le 28) -and ($y-$x -ge 1) -and ($y-$x -le 8)) { $bmp.SetPixel($x,$y,(C $p[5])) }
    }}
    for($y=8;$y -le 18;$y++){ for($x=16;$x -le 25;$x++){
        if(($x+$y -ge 29) -and ($x+$y -le 39) -and ($x-$y -ge 3) -and ($x-$y -le 11)) { $bmp.SetPixel($x,$y,(C $p[5])) }
    }}
    for($i=0;$i -lt 7;$i++){ $bmp.SetPixel(14-$i,16-$i,(C $p[0])); $bmp.SetPixel(17+$i,15-$i,(C $p[0])) }
}

function Paint-Compass([System.Drawing.Bitmap]$bmp,[object[]]$p) {
    for($y=7;$y -le 24;$y++){ for($x=7;$x -le 24;$x++){
        $dx=$x-15.5; $dy=$y-15.5
        $axis=[math]::Min([math]::Abs($dx),[math]::Abs($dy)); $diag=[math]::Abs([math]::Abs($dx)-[math]::Abs($dy))
        $long=[math]::Max([math]::Abs($dx),[math]::Abs($dy))
        if(($axis -le 1.0 -and $long -le 8.5) -or ($diag -le 1.0 -and $long -le 5.5)) { $bmp.SetPixel($x,$y,(C $p[5])) }
    }}
    for($y=13;$y -le 18;$y++){ for($x=13;$x -le 18;$x++){ if(In-Diamond $x $y 16 16 3){$bmp.SetPixel($x,$y,(C $p[7]))} } }
}

function Paint-Sun([System.Drawing.Bitmap]$bmp,[object[]]$p) {
    for($y=12;$y -le 19;$y++){ for($x=12;$x -le 19;$x++){ $dx=$x-15.5;$dy=$y-15.5;if(($dx*$dx+$dy*$dy)-le 14){$bmp.SetPixel($x,$y,(C $p[5]))} } }
    foreach($pt in @(@(16,7),@(16,24),@(7,16),@(24,16),@(10,10),@(22,10),@(10,22),@(22,22))){
        $cx=$pt[0];$cy=$pt[1];for($y=$cy-2;$y -le $cy+2;$y++){for($x=$cx-2;$x -le $cx+2;$x++){if(In-Diamond $x $y $cx $cy 2){$bmp.SetPixel($x,$y,(C $p[5]))}}}
    }
}

function Paint-Spiral([System.Drawing.Bitmap]$bmp,[object[]]$p) {
    for($i=0;$i -le 130;$i++){
        $t=$i/130.0*4.7*[math]::PI; $radius=1.0+($i/130.0)*8.2
        $x=[int][math]::Round(15.5+[math]::Cos($t)*$radius); $y=[int][math]::Round(15.5+[math]::Sin($t)*$radius)
        foreach($o in @(@(0,0),@(1,0),@(0,1))){$px=$x+$o[0];$py=$y+$o[1];if($px -ge 0 -and $px -lt 32 -and $py -ge 0 -and $py -lt 32){$bmp.SetPixel($px,$py,(C $p[5]))}}
    }
}

function Paint-Crescent([System.Drawing.Bitmap]$bmp,[object[]]$p) {
    for($y=7;$y -le 24;$y++){for($x=7;$x -le 23;$x++){
        $outer=(($x-14)*($x-14)+($y-16)*($y-16)) -le 72
        $cut=(($x-18)*($x-18)+($y-13)*($y-13)) -le 60
        if($outer -and -not $cut){$bmp.SetPixel($x,$y,(C $p[5]))}
    }}
    for($y=12;$y -le 18;$y++){for($x=20;$x -le 26;$x++){if(In-Diamond $x $y 23 15 3){$bmp.SetPixel($x,$y,(C $p[7]))}}}
}

function Paint-Back([System.Drawing.Bitmap]$bmp,[object[]]$p,[int]$tier) {
    for($y=0;$y -lt 32;$y++){for($x=0;$x -lt 32;$x++){
        $dx=$x-15.5;$dy=$y-15.5;$r=[math]::Sqrt($dx*$dx+$dy*$dy)
        if(($r -ge 6.5 -and $r -le 7.4) -or ($r -ge 9.2 -and $r -le 10.0)){$bmp.SetPixel($x,$y,(C $p[0]))}
    }}
    for($y=12;$y -le 19;$y++){for($x=12;$x -le 19;$x++){if(In-Diamond $x $y 16 16 4){$bmp.SetPixel($x,$y,(C $p[5]))}}}
    for($i=0;$i -lt $tier;$i++){
        $angle=(-[math]::PI/2)+(2*[math]::PI*$i/$tier);$cx=[int][math]::Round(15.5+[math]::Cos($angle)*8.4);$cy=[int][math]::Round(15.5+[math]::Sin($angle)*8.4)
        for($y=$cy-1;$y -le $cy+1;$y++){for($x=$cx-1;$x -le $cx+1;$x++){if(In-Diamond $x $y $cx $cy 1){$bmp.SetPixel($x,$y,(C $p[5]))}}}
    }
}

function Paint-Rim([System.Drawing.Bitmap]$bmp,[object[]]$p) {
    for($y=0;$y -lt 32;$y++){for($x=0;$x -lt 32;$x++){
        $stripe=[math]::Floor($x/4)%4
        $idx=if($stripe -eq 0){0}elseif($stripe -eq 1){2}elseif($stripe -eq 2){4}else{1}
        if($y -lt 5){$idx=5}elseif($y -gt 25){$idx=0}
        $bmp.SetPixel($x,$y,(C $p[$idx]))
    }}
}

New-Item -ItemType Directory -Force -Path $OutputRoot | Out-Null
$coins=@(
    @('copper',$palettes.copper,'sprout',1),
    @('silver',$palettes.silver,'compass',2),
    @('gold',$palettes.gold,'sun',3),
    @('aether',$palettes.aether,'spiral',4),
    @('arcane',$palettes.arcane,'crescent',5)
)

foreach($coin in $coins){
    $name=$coin[0];$palette=$coin[1];$motif=$coin[2];$tier=$coin[3]
    $front=New-Canvas; Paint-Base $front $palette
    switch($motif){'sprout'{Paint-Sprout $front $palette};'compass'{Paint-Compass $front $palette};'sun'{Paint-Sun $front $palette};'spiral'{Paint-Spiral $front $palette};'crescent'{Paint-Crescent $front $palette}}
    $front.Save((Join-Path $OutputRoot "${name}_coin_front.png"),[System.Drawing.Imaging.ImageFormat]::Png);$front.Dispose()
    $back=New-Canvas;Paint-Base $back $palette;Paint-Back $back $palette $tier
    $back.Save((Join-Path $OutputRoot "${name}_coin_back.png"),[System.Drawing.Imaging.ImageFormat]::Png);$back.Dispose()
    $rim=New-Canvas;Paint-Rim $rim $palette
    $rim.Save((Join-Path $OutputRoot "${name}_coin_rim.png"),[System.Drawing.Imaging.ImageFormat]::Png);$rim.Dispose()
}
