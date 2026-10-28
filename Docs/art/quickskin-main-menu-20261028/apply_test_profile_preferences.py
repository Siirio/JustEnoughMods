from pathlib import Path


root = Path(__file__).resolve().parents[3]
profile = Path("C:/Users/user/AppData/Roaming/.minecraft/versions/test jem")
stable_profile = Path("C:/Users/user/AppData/Roaming/.minecraft/versions/JustEnoughMods")
options_path = profile / "options.txt"
keybind_source = root / "Docs/releases/2026-10-26-original-controls-development-inventory/original-keybinds.txt"

excluded_prefixes = (
    "key_key.trashslot.",
    "key_key.backpack:",
    "key_key.backpacked.",
)
keybinds = [
    line
    for line in keybind_source.read_text(encoding="utf-8").splitlines()
    if line.startswith("key_") and not line.startswith(excluded_prefixes)
]
if len(keybinds) != 244:
    raise RuntimeError(f"Expected 244 curated keybinds, found {len(keybinds)}")

lines = options_path.read_text(encoding="utf-8").splitlines()
first_key = next((index for index, line in enumerate(lines) if line.startswith("key_")), None)
if first_key is None:
    raise RuntimeError("options.txt has no keybind block")

without_keys = [line for line in lines if not line.startswith("key_")]
non_keys_before = sum(1 for line in lines[:first_key] if not line.startswith("key_"))
without_keys[non_keys_before:non_keys_before] = keybinds

stable_resource_packs = next(
    line for line in (stable_profile / "options.txt").read_text(encoding="utf-8").splitlines()
    if line.startswith("resourcePacks:")
)
resource_packs = stable_resource_packs.replace(',"file/CoffeeGUI_1.20.1_release_v2.2.1.zip"', '')
resource_packs = resource_packs[:-1] + ',"file/JEM Release v0.3.zip"]'
found_resource_packs = False
found_entity_shadows = False
for index, line in enumerate(without_keys):
    if line.startswith("resourcePacks:"):
        without_keys[index] = resource_packs
        found_resource_packs = True
    elif line.startswith("entityShadows:"):
        without_keys[index] = "entityShadows:true"
        found_entity_shadows = True
if not found_resource_packs:
    raise RuntimeError("options.txt has no resourcePacks row")
if not found_entity_shadows:
    raise RuntimeError("options.txt has no entityShadows row")

options_path.write_text("\n".join(without_keys) + "\n", encoding="utf-8")

oculus_path = profile / "config/oculus.properties"
oculus = oculus_path.read_text(encoding="utf-8").splitlines()
for index, line in enumerate(oculus):
    if line.startswith("maxShadowRenderDistance="):
        oculus[index] = "maxShadowRenderDistance=9"
    elif line.startswith("shaderPack=Bloop-"):
        oculus[index] = "shaderPack=[\\u041A\\u043E\\u043C\\u0444\\u043E\\u0440\\u0442\\u043D\\u044B\\u0439]ComplementaryReimagined_r5.5.1 + EuphoriaPatches_1.6.5"
oculus_path.write_text("\n".join(oculus) + "\n", encoding="utf-8")

written = options_path.read_text(encoding="utf-8").splitlines()
written_keys = [line for line in written if line.startswith("key_")]
if written_keys != keybinds:
    raise RuntimeError("Written keybind order or values differ from the GitHub source")
if any("trashslot" in line or line.startswith("key_key.backpack:") or "key_key.backpacked." in line for line in written_keys):
    raise RuntimeError("Retired Backpacked or TrashSlot keybind remains")
if resource_packs not in written or "CoffeeGUI" in resource_packs or "JEM Legacy" in resource_packs:
    raise RuntimeError("Resource-pack selection is not original minus CoffeeGUI plus active JEM Release")
if "entityShadows:true" not in written or "maxShadowRenderDistance=9" not in oculus_path.read_text(encoding="utf-8"):
    raise RuntimeError("Entity or Oculus shadows are not enabled")

print(f"Applied {len(written_keys)} GitHub keybinds, original pack order plus JEM Release, inactive Legacy and restored shadows")
