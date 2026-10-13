param(
    [string]$RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
)

Add-Type -AssemblyName System.IO.Compression.FileSystem
$policyPath = Join-Path $RepositoryRoot 'Mods\JEMEconomy\src\main\resources\data\jem_economy\economy\item_policies.json'
$outputDirectory = Join-Path $RepositoryRoot 'Docs\economy'
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
$policies = @{}
foreach ($row in (Get-Content $policyPath -Raw | ConvertFrom-Json)) { $policies[$row.item] = $row }

$ids = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$authoritativeIds = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$sources = @{}
$recipes = @{}
$recipeDocuments = [Collections.Generic.List[object]]::new()
function Add-Identifier([string]$id, [string]$source, [bool]$authoritative = $false) {
    if ($id -notmatch '^[a-z0-9_.-]+:[a-z0-9_./-]+$') { return }
    [void]$ids.Add($id)
    if ($authoritative) { [void]$authoritativeIds.Add($id) }
    if (!$sources.ContainsKey($id)) { $sources[$id] = $source }
}
function Inspect-Archive([string]$path, [string]$source) {
    $archive = [IO.Compression.ZipFile]::OpenRead($path)
    try {
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName -match '^assets/([^/]+)/models/item/([^/]+)\.json$') { Add-Identifier "$($Matches[1]):$([IO.Path]::GetFileNameWithoutExtension($entry.Name))" $source $false; continue }
            if ($entry.FullName -match '^assets/([^/]+)/lang/en_us\.json$') {
                try { $reader = [IO.StreamReader]::new($entry.Open()); $json = $reader.ReadToEnd() | ConvertFrom-Json; $reader.Dispose(); foreach ($key in $json.PSObject.Properties.Name) { if ($key -match '^(?:item|block)\.([a-z0-9_.-]+)\.(.+)$') { Add-Identifier "$($Matches[1]):$($Matches[2])" $source $true } } } catch { }
                continue
            }
            if ($entry.FullName -match '^data/[^/]+/recipes/.+\.json$') {
                try { $reader = [IO.StreamReader]::new($entry.Open()); $text = $reader.ReadToEnd(); $reader.Dispose(); $reference = "$source::$($entry.FullName)"; $recipeDocuments.Add([pscustomobject]@{ Reference=$reference; Text=$text }); $json = $text | ConvertFrom-Json; foreach ($candidate in @($json.result,$json.output,$json.results)) { if ($candidate -is [string]) { Add-Identifier $candidate $source $true } elseif ($null -ne $candidate) { if ($candidate.item) { Add-Identifier ([string]$candidate.item) $source $true }; if ($candidate.id) { Add-Identifier ([string]$candidate.id) $source $true } } }; foreach ($match in [regex]::Matches($text, '"([a-z0-9_.-]+:[a-z0-9_./-]+)"')) { $id = $match.Groups[1].Value; if (!$recipes.ContainsKey($id)) { $recipes[$id] = [Collections.Generic.List[string]]::new() }; if ($recipes[$id].Count -lt 8) { $recipes[$id].Add($reference) } } } catch { }
            }
        }
    } finally { $archive.Dispose() }
}

$vanilla = Get-ChildItem "$env:USERPROFILE\.gradle\caches\forge_gradle\minecraft_repo\versions\1.20.1" -Filter 'client-extra.jar' -File | Select-Object -First 1
if ($vanilla) { Inspect-Archive $vanilla.FullName 'vanilla:minecraft-1.20.1-client-extra.jar' }
$serverJars = Get-ChildItem (Join-Path $RepositoryRoot 'Server\mods') -Filter '*.jar' -File | Sort-Object Name
$serverNames = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
foreach ($jar in $serverJars) { [void]$serverNames.Add($jar.Name); Inspect-Archive $jar.FullName "server:$($jar.Name)" }
foreach ($jar in Get-ChildItem (Join-Path $RepositoryRoot 'Client\ManagedPack\mods') -Filter '*.jar' -File | Sort-Object Name) {
    if (!$serverNames.Contains($jar.Name)) { Inspect-Archive $jar.FullName "managed-client-only:$($jar.Name)" }
}
foreach ($row in $policies.Values) { Add-Identifier $row.item 'jem-economy-policy-v1' $true }

$mandatory = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
@('minecraft:elytra','minecraft:dragon_egg','minecraft:nether_star','minecraft:beacon','minecraft:netherite_upgrade_smithing_template','minecraft:netherite_scrap','minecraft:netherite_ingot','minecraft:netherite_sword','minecraft:netherite_pickaxe','minecraft:netherite_axe','minecraft:netherite_shovel','minecraft:netherite_hoe','minecraft:netherite_helmet','minecraft:netherite_chestplate','minecraft:netherite_leggings','minecraft:netherite_boots','minecraft:trident','minecraft:totem_of_undying','minecraft:enchanted_golden_apple','minecraft:heart_of_the_sea','minecraft:conduit','minecraft:recovery_compass','minecraft:spawner','minecraft:command_block','minecraft:structure_block','minecraft:barrier','minecraft:bedrock','minecraft:knowledge_book','minecraft:debug_stick','jem_economy:copper_coin','jem_economy:silver_coin','jem_economy:gold_coin','jem_economy:aether_coin','jem_economy:arcane_coin') | ForEach-Object { [void]$mandatory.Add($_) }

$rows = foreach ($id in ($ids | Where-Object { $authoritativeIds.Contains($_) } | Sort-Object)) {
    $policy = $policies[$id]
    $outcome = if ($policy) { $policy.outcome } else { 'NONE' }
    $reason = if ($outcome -eq 'BUY_AND_SELL') { '' } elseif ($mandatory.Contains($id)) { 'mandatory_protected' } elseif ($id -match '(^endrem:|eye|boss|summon|spawn_egg|trophy|creative|command|structure|barrier|bedrock|netherite|totem|elytra|trident|upgrade_smithing_template|spell_book|artifact|key$)') { 'progression_or_unsafe_exact_id_reviewed_none' } else { 'not_explicitly_approved; resolved_none' }
    [pscustomobject]@{
        schema = 1
        item = $id
        source_mod = ($id -split ':')[0]
        source_artifact = $sources[$id]
        outcome = $outcome
        buy_copper = if ($policy) { $policy.buy_copper } else { $null }
        sell_copper = if ($policy) { $policy.sell_copper } else { $null }
        daily_player_units = if ($policy) { $policy.daily_player_units } else { 0 }
        daily_global_units = if ($policy) { $policy.daily_global_units } else { 0 }
        demand_group = if ($policy) { $policy.demand_group } else { '' }
        protect_reason = $reason
        recipe_reference = if ($recipes.ContainsKey($id)) { ($recipes[$id] -join '|') } else { '' }
        confidence = if ($policy) { 'hand_reviewed_exact_id_and_internal_jar_assets' } else { 'resolved_none_from_internal_jar_assets' }
        verification = 'server_and_managed_client_jar_internal_assets_or_vanilla_client_extra; runtime_unknowns_fail_none'
    }
}
$rows | Export-Csv (Join-Path $outputDirectory 'item-policy-audit.csv') -NoTypeInformation -Encoding utf8

$protectedSet = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
foreach ($row in $rows | Where-Object { $_.protect_reason -in @('mandatory_protected','progression_or_unsafe_exact_id_reviewed_none') }) { [void]$protectedSet.Add($row.item) }
$generic = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
@('minecraft:stick','minecraft:iron_ingot','minecraft:gold_ingot','minecraft:diamond','minecraft:paper','minecraft:string','minecraft:leather','minecraft:plank','minecraft:glass','minecraft:redstone','minecraft:copper_ingot') | ForEach-Object { [void]$generic.Add($_) }
$ingredientRows = foreach ($document in $recipeDocuments) {
    try { $json = $document.Text | ConvertFrom-Json } catch { continue }
    $outputs = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($candidate in @($json.result, $json.output)) {
        if ($candidate -is [string]) { [void]$outputs.Add($candidate) }
        elseif ($null -ne $candidate) { if ($candidate.item) { [void]$outputs.Add([string]$candidate.item) }; if ($candidate.id) { [void]$outputs.Add([string]$candidate.id) } }
    }
    foreach ($candidate in @($json.results)) { if ($candidate.item) { [void]$outputs.Add([string]$candidate.item) }; if ($candidate.id) { [void]$outputs.Add([string]$candidate.id) } }
    foreach ($output in $outputs) {
        if (!$protectedSet.Contains($output)) { continue }
        $seen = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
        foreach ($match in [regex]::Matches($document.Text, '"([a-z0-9_.-]+:[a-z0-9_./-]+)"')) {
            $ingredient = $match.Groups[1].Value
            if ($ingredient -eq $output -or $ingredient -eq [string]$json.type -or !$seen.Add($ingredient)) { continue }
            $policy = $policies[$ingredient]
            $decision = if ($generic.Contains($ingredient) -or ($policy -and $policy.outcome -eq 'BUY_AND_SELL')) { 'generic_or_explicit_market_exemption' } else { 'NONE_one_edge_progression_precursor' }
            [pscustomobject]@{ protected_output=$output; direct_ingredient=$ingredient; ingredient_outcome=$(if($policy){$policy.outcome}else{'NONE'}); decision=$decision; recipe_reference=$document.Reference; policy_model='BUY_AND_SELL_or_NONE' }
        }
    }
}
$ingredientRows | Export-Csv (Join-Path $outputDirectory 'protected-one-craft-away-review.csv') -NoTypeInformation -Encoding utf8

Write-Host "Wrote $($rows.Count) resolved item rows and $($ingredientRows.Count) protected recipe references."
