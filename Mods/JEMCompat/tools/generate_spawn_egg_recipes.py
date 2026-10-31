#!/usr/bin/env python3
"""Generate the reviewed Update 4 egg recipes, fallback eggs, and spawner policy."""

from __future__ import annotations

import itertools
import json
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
CANDIDATES = ROOT / "Catalog/changes/major_update_4_spawn_egg_candidates.json"
REVIEW = ROOT / "Catalog/changes/major_update_4_spawn_egg_review.json"
REGISTRIES = ROOT / "Catalog/registries"
RESOURCES = ROOT / "Mods/JEMCompat/src/generated/resources"
RECIPES = RESOURCES / "data/jemcompat/recipes/spawn_eggs"
POLICY = RESOURCES / "data/jemcompat/spawn_egg_policy.json"
FALLBACKS = RESOURCES / "data/jemcompat/fallback_spawn_eggs.json"

# These item ids are advertised by static registry metadata but are absent from the
# live Forge item registry in the installed pack. Runtime recipe loading is the
# authority for whether an egg item can be used as a crafting result.
RUNTIME_MISSING_EGGS = {
    "cataclysm:onyx_spawn_egg",
    "cnc:greywolf_spawn_egg",
    "cnc:largepolarbear_spawn_egg",
    "deep_dark_extended:spreader_spawn_egg",
    "netherexp:warphopper_spawn_egg",
    "primeval_adventures:brygmophyseter_spawn_egg",
    "primeval_adventures:dinornis_spawn_egg",
    "primeval_adventures:dodo_spawn_egg",
    "primeval_adventures:glyptodon_spawn_egg",
    "primeval_adventures:odobenocetops_spawn_egg",
    "primeval_adventures:thylacine_spawn_egg",
}
MODELS = RESOURCES / "assets/jemcompat/models/item"
GENERATED_JAVA = ROOT / "Mods/JEMCompat/src/generated/java/com/siirio/jemcompat/feature/spawner/GeneratedSpawnEggContent.java"

HORSE_BREEDS = (
    "icelandic", "friesian", "haflinger", "appaloosa", "thoroughbred",
    "american_paint", "andalusian", "mustang", "quarter", "arabian",
    "morgan", "percheron", "shire", "belgian", "clydesdale",
)
HORSE_OUTER = (
    "minecraft:leather", "minecraft:hay_block", "minecraft:golden_carrot", "minecraft:saddle",
    "minecraft:iron_ingot", "minecraft:wheat", "minecraft:apple", "minecraft:diamond",
)

# Every class-review candidate is resolved explicitly. Exclusions remain named
# in the generated review record rather than disappearing from coverage.
CLASS_REVIEW_EXCLUSIONS = {
    "alexscaves:magnetron": "MINIBOSS",
    "aquamirae:maw": "MINIBOSS",
    "aquamirae:shipbreaker": "EVENT_BOSS_TECHNICAL_ENTITY",
    "deep_dark_regrowth:abomination": "BOSSLIKE_UNIQUE_MONSTER",
    "deep_dark_regrowth:annihilator": "BOSSLIKE_UNIQUE_MONSTER",
    "irons_spellbooks:archevoker": "MINIBOSS",
    "irons_spellbooks:wisp": "SUMMON_ONLY_COMPANION",
    "skarrier_mobs:freeze": "TECHNICAL_EFFECT_ENTITY",
    "unearthed_journey:basilosaurus": "AQUATIC_FISHLIKE_CREATURE",
    "unearthed_journey:livyatan": "BOSS",
    "unearthed_journey:purussaurus_brasiliensis": "BOSS",
}
CLASS_REVIEW_EXCLUSIONS.update({
    f"tide:{path}": "FISH_OR_AQUATIC_COLLECTION_CREATURE"
    for path in (
        "bedrock_bug", "cave_crawler", "crystal_shrimp", "incandescent_larva",
        "luminescent_jellyfish", "pentapus", "pluto_snail", "saturn_cuttlefish",
        "shooting_starfish", "snatcher_squid", "spore_stalker",
    )
})

# These generic incubation catalysts are used only when two supplied recipes
# have the same ingredient multiset. Seven original ingredients and the center
# remain intact, while the pair makes shapeless equivalence impossible.
CATALYSTS = (
    "minecraft:amethyst_shard", "minecraft:blaze_powder", "minecraft:bone_meal",
    "minecraft:clay_ball", "minecraft:coal", "minecraft:copper_ingot",
    "minecraft:echo_shard", "minecraft:ender_pearl", "minecraft:feather",
    "minecraft:flint", "minecraft:glow_ink_sac", "minecraft:glowstone_dust",
    "minecraft:gold_ingot", "minecraft:gunpowder", "minecraft:honeycomb",
    "minecraft:ink_sac", "minecraft:iron_ingot", "minecraft:lapis_lazuli",
    "minecraft:leather", "minecraft:magma_cream", "minecraft:nautilus_shell",
    "minecraft:nether_brick", "minecraft:phantom_membrane", "minecraft:prismarine_crystals",
    "minecraft:prismarine_shard", "minecraft:quartz", "minecraft:rabbit_hide",
    "minecraft:redstone", "minecraft:scute", "minecraft:slime_ball",
    "minecraft:snowball", "minecraft:string", "minecraft:charcoal",
    "minecraft:brick", "minecraft:fire_charge", "minecraft:fermented_spider_eye",
    "minecraft:ghast_tear", "minecraft:gold_nugget", "minecraft:iron_nugget",
    "minecraft:spider_eye",
)


def registry_ids(kind: str) -> set[str]:
    result: set[str] = set()
    for path in sorted((REGISTRIES / kind).glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        result.update(row["registry_id"] for row in data.get("records", []))
    return result


def horse_rows() -> list[dict]:
    rows = []
    permutations = itertools.islice(itertools.permutations(HORSE_OUTER), len(HORSE_BREEDS))
    for breed, outer in zip(HORSE_BREEDS, permutations):
        grid = outer[:4] + ("minecraft:horse_spawn_egg",) + outer[4:]
        rows.append({
            "entity_id": f"icys-better-horses:{breed}_horse",
            "output_item": f"icys-better-horses:{breed}_horse_spawn_egg",
            **{f"slot_{index + 1}": value for index, value in enumerate(grid)},
            "spawner_allowed": "SLOW_24000_TICKS",
            "source_status": "BETTER_HORSES_HAND_REVIEW",
        })
    return rows


def stable_name(value: str) -> str:
    return re.sub(r"[^a-z0-9_.-]+", "_", value)


def fallback_id(entity_id: str) -> str:
    return "jemcompat:spawn_egg_" + stable_name(entity_id.replace(":", "_"))


def unique_grid(ingredients: list[str], seen: set[tuple[str, ...]]) -> list[str]:
    key = tuple(sorted(ingredients))
    if key not in seen:
        seen.add(key)
        return ingredients
    for left, right in itertools.combinations_with_replacement(CATALYSTS, 2):
        candidate = ingredients.copy()
        candidate[0], candidate[8] = left, right
        key = tuple(sorted(candidate))
        if key not in seen:
            seen.add(key)
            return candidate
    raise RuntimeError("Catalyst space exhausted while disambiguating egg recipes")


def write_fallback_java(entries: list[dict]) -> None:
    registrations = "\n".join(
        f'        register("{entry["item"].split(":", 1)[1]}", "{entry["entity"]}");'
        for entry in entries
    )
    source = f'''package com.siirio.jemcompat.feature.spawner;

import com.siirio.jemcompat.JEMCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Generated registrations for eligible creatures whose owning mod has no usable egg item. */
public final class GeneratedSpawnEggContent {{
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, JEMCompat.MOD_ID);

    static {{
{registrations}
    }}

    private GeneratedSpawnEggContent() {{ }}

    public static void register(IEventBus bus) {{ ITEMS.register(bus); }}

    @SuppressWarnings("unchecked")
    private static void register(String path, String entityId) {{
        var id = new ResourceLocation(entityId);
        ITEMS.register(path, () -> new ForgeSpawnEggItem(
                () -> (EntityType<? extends Mob>) ForgeRegistries.ENTITY_TYPES.getValue(id),
                0x6b5a43, 0xd9c27a, new Item.Properties()));
    }}
}}
'''
    GENERATED_JAVA.parent.mkdir(parents=True, exist_ok=True)
    GENERATED_JAVA.write_text(source, encoding="utf-8")


def main() -> None:
    item_ids = registry_ids("items")
    entity_ids = registry_ids("entities")
    candidates = json.loads(CANDIDATES.read_text(encoding="utf-8"))
    accepted, audit = [], []
    for row in candidates:
        status = row.get("approval_status")
        if status == "REQUIRES_CLASS_REVIEW" and row["entity_id"] in CLASS_REVIEW_EXCLUSIONS:
            audit.append({"entity": row["entity_id"], "decision": "EXCLUDED", "reason": CLASS_REVIEW_EXCLUSIONS[row["entity_id"]]})
            continue
        if status not in {"CONDITIONALLY_APPROVED", "REQUIRES_CLASS_REVIEW"}:
            audit.append({"entity": row["entity_id"], "decision": "EXCLUDED", "reason": f"UNSUPPORTED_STATUS_{status}"})
            continue
        if row["entity_id"] not in entity_ids:
            audit.append({"entity": row["entity_id"], "decision": "UNRESOLVED", "reason": "ENTITY_NOT_IN_INSTALLED_REGISTRY"})
            continue
        ingredients = [row[f"slot_{index}"] for index in range(1, 10)]
        missing = [item for item in ingredients if item not in item_ids and not item.startswith("minecraft:")]
        if missing:
            audit.append({"entity": row["entity_id"], "decision": "UNRESOLVED", "reason": "INGREDIENT_NOT_IN_INSTALLED_REGISTRY", "items": missing})
            continue
        row = dict(row)
        row["source_status"] = status
        accepted.append(row)
        audit.append({"entity": row["entity_id"], "decision": "RECIPE", "reason": "ELIGIBLE_LIVING_CREATURE"})
    accepted.extend(horse_rows())

    if RECIPES.exists():
        shutil.rmtree(RECIPES)
    RECIPES.mkdir(parents=True)
    if MODELS.exists():
        for model in MODELS.glob("spawn_egg_*.json"):
            model.unlink()
    MODELS.mkdir(parents=True, exist_ok=True)

    fallback_entries, policy_rows, seen = [], [], set()
    for row in accepted:
        ingredients = unique_grid([row[f"slot_{index}"] for index in range(1, 10)], seen)
        output = row["output_item"]
        if output not in item_ids or output in RUNTIME_MISSING_EGGS or output.startswith("jem_spawn_eggs:"):
            output = fallback_id(row["entity_id"])
            fallback_entries.append({"entity": row["entity_id"], "item": output})
            model_path = MODELS / f"{output.split(':', 1)[1]}.json"
            model_path.write_text(json.dumps({"parent": "minecraft:item/template_spawn_egg"}, indent=2) + "\n", encoding="utf-8")
        key = {letter: {"item": item} for letter, item in zip("ABCDEFGHI", ingredients)}
        recipe = {
            "type": "minecraft:crafting_shaped", "category": "misc",
            "pattern": ["ABC", "DEF", "GHI"], "key": key,
            "result": {"item": output, "count": 1}, "show_notification": True,
        }
        (RECIPES / f"{stable_name(row['entity_id'])}.json").write_text(json.dumps(recipe, indent=2) + "\n", encoding="utf-8")
        policy_rows.append({"entity": row["entity_id"], "egg": output, "spawner": row["spawner_allowed"]})

    POLICY.parent.mkdir(parents=True, exist_ok=True)
    POLICY.write_text(json.dumps({"schema": 1, "entries": policy_rows}, indent=2) + "\n", encoding="utf-8")
    FALLBACKS.write_text(json.dumps({"schema": 1, "entries": fallback_entries}, indent=2) + "\n", encoding="utf-8")
    REVIEW.write_text(json.dumps({
        "schema": 1,
        "eligible_recipes": len(accepted),
        "fallback_eggs": len(fallback_entries),
        "unresolved": [row for row in audit if row["decision"] == "UNRESOLVED"],
        "class_review": audit,
    }, indent=2) + "\n", encoding="utf-8")
    write_fallback_java(fallback_entries)
    unresolved = sum(1 for row in audit if row["decision"] == "UNRESOLVED")
    print(f"Generated {len(accepted)} full-grid recipes, {len(fallback_entries)} fallback eggs, {unresolved} unresolved")


if __name__ == "__main__":
    main()
