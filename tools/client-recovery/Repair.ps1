$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
try {
    $bundleRoot = $PSScriptRoot
    $profileRoot = Join-Path $env:APPDATA '.minecraft\versions\JustEnoughMods'
    if (-not (Test-Path -LiteralPath (Join-Path $profileRoot 'JustEnoughMods.json'))) {
        $picker = New-Object System.Windows.Forms.FolderBrowserDialog
        $picker.Description = 'Выберите папку профиля JustEnoughMods внутри versions'
        if ($picker.ShowDialog() -ne 'OK') { exit 0 }
        $profileRoot = $picker.SelectedPath
    }
    $profileRoot = (Resolve-Path -LiteralPath $profileRoot).Path
    $metadataPath = Join-Path $profileRoot 'JustEnoughMods.json'
    if (-not (Test-Path -LiteralPath $metadataPath)) { throw 'Выбрана другая папка: нужен JustEnoughMods.json.' }
    $bootstrapPath = Join-Path $profileRoot 'mods\jem-bootstrap.jar'
    if ((Get-FileHash -LiteralPath $bootstrapPath -Algorithm SHA256).Hash.ToLowerInvariant() -ne 'a8c26d26b28ba4d51134731cf6c285cd6a92268777d82c28831e38a716a5c99b') {
        throw 'Другая версия jem-bootstrap.jar. Отправьте эту ошибку администратору; файлы не менялись.'
    }
    $helperPath = Join-Path $bundleRoot 'jem-client-recovery.jar'
    if ((Get-FileHash -LiteralPath $helperPath -Algorithm SHA256).Hash.ToLowerInvariant() -ne '__RECOVERY_SHA256__') {
        throw 'Файл восстановления поврежден. Распакуйте архив заново.'
    }
    $minecraftRoot = Split-Path (Split-Path $profileRoot -Parent) -Parent
    $metadata = Get-Content -LiteralPath $metadataPath -Raw -Encoding UTF8 | ConvertFrom-Json
    $classPath = New-Object 'System.Collections.Generic.List[string]'
    $classPath.Add($helperPath)
    $classPath.Add($bootstrapPath)
    foreach ($library in $metadata.libraries) {
        $parts = $library.name.Split(':')
        if ($parts.Count -lt 3) { continue }
        $relative = $parts[0].Replace('.', '/') + '/' + $parts[1] + '/' + $parts[2] + '/' + $parts[1] + '-' + $parts[2] + '.jar'
        $libraryPath = Join-Path (Join-Path $minecraftRoot 'libraries') $relative
        if (Test-Path -LiteralPath $libraryPath) { $classPath.Add($libraryPath) }
    }
    $javaCandidates = New-Object 'System.Collections.Generic.List[string]'
    $runtimeRoot = Join-Path $minecraftRoot 'runtime'
    if (Test-Path -LiteralPath $runtimeRoot) {
        Get-ChildItem -LiteralPath $runtimeRoot -Filter java.exe -Recurse -File | ForEach-Object { $javaCandidates.Add($_.FullName) }
    }
    if ($env:JAVA_HOME) { $javaCandidates.Add((Join-Path $env:JAVA_HOME 'bin\java.exe')) }
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($javaCommand) { $javaCandidates.Add($javaCommand.Source) }
    $javaPath = $null
    foreach ($candidate in $javaCandidates) {
        if (-not (Test-Path -LiteralPath $candidate)) { continue }
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = $candidate
        $psi.Arguments = '-version'
        $psi.UseShellExecute = $false
        $psi.CreateNoWindow = $true
        $psi.RedirectStandardError = $true
        $process = [System.Diagnostics.Process]::Start($psi)
        $version = $process.StandardError.ReadToEnd()
        $process.WaitForExit()
        if ($version -match 'version "(17|21)\.') { $javaPath = $candidate; break }
    }
    if (-not $javaPath) { throw 'Не найдена Java 17/21. Выберите ее в лаунчере и установите игровой runtime.' }
    Write-Host 'Не закрывайте это окно. Штатный установщик готовит план; затем появится подтверждение.'
    Push-Location -LiteralPath $profileRoot
    try {
        & $javaPath '-Dfile.encoding=UTF-8' '-cp' ($classPath -join ';') 'JemClientRecovery' $profileRoot $bundleRoot
        if ($LASTEXITCODE -ne 0) { throw 'Восстановление не завершено. Сфотографируйте текст выше для администратора.' }
    } finally { Pop-Location }
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    [System.Windows.Forms.MessageBox]::Show($_.Exception.Message, 'JEM: восстановление остановлено') | Out-Null
    exit 1
}
