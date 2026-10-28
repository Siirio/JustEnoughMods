param(
    [Parameter(Mandatory = $true)]
    [string]$InputPath,

    [Parameter(Mandatory = $true)]
    [string]$OutputPath
)

$content = Get-Content -LiteralPath $InputPath -Raw
$content = $content.Replace(
    '[source:local]/config/fancymenu/assets/just_enough_mods/2djembg.png',
    '[source:local]/config/fancymenu/assets/just_enough_mods/main_menu_background.png'
)
$content = $content.Replace(
    '[source:local]/config/fancymenu/assets/just_enough_mods/2djem.png',
    '[source:local]/config/fancymenu/assets/just_enough_mods/main_menu_title.png'
)

function Update-Block {
    param(
        [string]$Text,
        [string]$BlockType,
        [string]$Identifier,
        [hashtable]$Properties
    )

    $pattern = '(?ms)^' + [regex]::Escape($BlockType) + ' \{\r?\n(?:(?!^\}).)*?^\}'
    return [regex]::Replace($Text, $pattern, {
        param($match)
        $block = $match.Value
        if ($block -notmatch '(?m)^\s*instance_identifier\s*=\s*' + [regex]::Escape($Identifier) + '\s*$') {
            return $block
        }
        foreach ($name in $Properties.Keys) {
            $replacement = '  ' + $name + ' = ' + $Properties[$name]
            $block = [regex]::Replace(
                $block,
                '(?m)^\s*' + [regex]::Escape($name) + '\s*=.*$',
                $replacement,
                1
            )
        }
        return $block
    })
}

$content = Update-Block -Text $content -BlockType 'element' -Identifier 'd7bb087a-c122-47d7-a653-bf52964bca89-1788108875058' -Properties @{
    x = '-147'
    y = '-169'
    width = '294'
    height = '101'
    should_be_affected_by_decoration_overlays = 'false'
}

$buttonLayout = [ordered]@{
    mc_titlescreen_singleplayer_button = '-72'
    mc_titlescreen_multiplayer_button = '-46'
    mc_titlescreen_realms_button = '-20'
    forge_titlescreen_mods_button = '6'
    mc_titlescreen_options_button = '32'
    mc_titlescreen_quit_button = '58'
    '628382' = '84'
}

$content = Update-Block -Text $content -BlockType 'vanilla_button' -Identifier '608292' -Properties @{
    transparent_background = 'true'
    x = '8'
    y = '258'
    width = '0'
    height = '0'
    stay_on_screen = 'false'
    should_be_affected_by_decoration_overlays = 'false'
    is_hidden = 'false'
}

foreach ($entry in $buttonLayout.GetEnumerator()) {
    $content = Update-Block -Text $content -BlockType 'vanilla_button' -Identifier $entry.Key -Properties @{
        underline_label_on_hover = 'true'
        transparent_background = 'true'
        x = '105'
        y = $entry.Value
        width = '190'
        height = '20'
        should_be_affected_by_decoration_overlays = 'false'
        label_shadow = 'true'
        is_hidden = 'false'
    }
}

$hiddenIconButtons = @(
    '706358',
    'minecraft_realms_notification_icons_widget',
    '6283821',
    '628358',
    '706292',
    '706314',
    'mc_titlescreen_language_button',
    '376322',
    'mc_titlescreen_accessibility_button',
    '706336'
)

foreach ($identifier in $hiddenIconButtons) {
    $content = Update-Block -Text $content -BlockType 'vanilla_button' -Identifier $identifier -Properties @{
        transparent_background = 'true'
        is_hidden = 'true'
    }
}

$content = [regex]::Replace(
    $content,
    '(?ms)(decoration_overlay \{\r?\n\s*overlay_type = leaves\r?\n(?:(?!^\}).)*?\s*show_decoration_overlay\s*=\s*)true',
    '${1}false'
)

$parent = Split-Path -Parent $OutputPath
if ($parent) {
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
}
Set-Content -LiteralPath $OutputPath -Value $content -NoNewline -Encoding utf8
