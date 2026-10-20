param(
    [Parameter(Mandatory)][string]$InputPath,
    [Parameter(Mandatory)][string]$JobId,
    [Parameter(Mandatory)][string[]]$Names,
    [Parameter(Mandatory)][int]$Width,
    [Parameter(Mandatory)][int]$Height,
    [Parameter(Mandatory)][string[]]$Destinations,
    [int]$Columns = 0,
    [int]$Rows = 0,
    [int]$Corner = 0,
    [int]$SourceCorner = 0,
    [int[][]]$Rectangles
)
$ErrorActionPreference = 'Stop'
$aseprite = 'C:/Users/user/.codex/tools/aseprite/build-1.3.18.6/bin/aseprite.exe'
$sourceDirectory = Join-Path $PSScriptRoot "sources/$JobId"
New-Item -ItemType Directory -Force -Path $sourceDirectory | Out-Null
foreach ($destination in $Destinations) { New-Item -ItemType Directory -Force -Path $destination | Out-Null }
$original = Join-Path $PSScriptRoot "generated/$JobId.png"
Copy-Item -LiteralPath $InputPath -Destination $original -Force
$contract = [ordered]@{
    job = $JobId; original = $original; originalSha256 = (Get-FileHash -LiteralPath $original -Algorithm SHA256).Hash.ToLowerInvariant()
    names = $Names; width = $Width; height = $Height; columns = $Columns; rows = $Rows; corner = $Corner; sourceCorner = $SourceCorner; rectangles = $Rectangles
    sourceDir = $sourceDirectory; destinations = $Destinations
    method = 'Aseprite nearest-neighbor; explicit rectangles when supplied, otherwise floor-divided grid cells for atlases or centered aspect-preserving crop for standalone art; sourceCorner remaps fixed material geometry'
    sourceState = 'Imported generated artwork with separate hidden protected geometry guide; not a claim of hand-painted pixel construction or runtime acceptance'
}
$contract | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $PSScriptRoot "manifests/$JobId.json") -Encoding utf8
function LuaString([string]$value) { '[=[' + $value.Replace('\','/') + ']=]' }
$lines = @('return {', 'input=' + (LuaString $original) + ',', 'sourceDir=' + (LuaString $sourceDirectory) + ',', 'report=' + (LuaString (Join-Path $PSScriptRoot "manifests/$JobId.csv")) + ',', "width=$Width,height=$Height,corner=$Corner,")
if ($Columns -gt 0) { $lines += "columns=$Columns,rows=$Rows," }
if ($SourceCorner -gt 0) { $lines += "sourceCorner=$SourceCorner," }
if ($Rectangles) { $lines += 'rectangles={' + (($Rectangles | ForEach-Object { '{'+($_ -join ',')+'}' }) -join ',') + '},' }
$lines += 'names={' + (($Names | ForEach-Object { LuaString $_ }) -join ',') + '},'
$lines += 'destinations={' + (($Destinations | ForEach-Object { LuaString $_ }) -join ',') + '},'
$lines += '}'
$jobFile = Join-Path $PSScriptRoot "manifests/$JobId.lua"
$lines | Set-Content -LiteralPath $jobFile -Encoding utf8NoBOM
& $aseprite --batch --script-param "job=$jobFile" --script (Join-Path $PSScriptRoot 'export-assets.lua') 2>&1 | Out-String | Write-Output
if ($LASTEXITCODE -ne 0) { throw "Aseprite export failed: $JobId" }
$outputs = foreach ($destination in $Destinations) {
    foreach ($name in $Names) {
        $path = Join-Path $destination "$name.png"
        if (-not (Test-Path -LiteralPath $path)) { throw "Missing exported asset: $path" }
        [ordered]@{ path=$path; sha256=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant() }
    }
}
$outputs | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $PSScriptRoot "manifests/$JobId-exports.json") -Encoding utf8
