"""Package the actual domain screens; replace their transports, never their artwork.

Generated classes are relocated to avoid collisions with installed domain mods.
Only explicitly listed presentation sources and immutable view records are admitted.
No server subscriptions, gameplay services, registries or packet channels are copied.
"""
from pathlib import Path
import hashlib
import json
import re
import shutil

MODULE = Path(__file__).resolve().parent
ROOT = MODULE.parent.parent
SOURCES = {
    "JEMServer": {
        "client/smp": "SmpScreen SmpHit SmpSidebar SmpIcons SmpGuiAssets BackNavigation SmpRoute EntityPreview PlayerPreviewWidget PreviewPlayerEntity ItemRewardStrip SmpActionButton EventRulesPanel EventEntryScreen ResultScreen",
        "client/ui": "JemPalette SmpRenderState JemMenuScreen",
        "smp": "SmpQuery SmpUpdate SmpActionFailure ViewDelta",
        "smp/events": "EventPrompt",
    },
    "JEMSettlements": {
        "client": "SettlementScreen SettlementMap TerritoryDragState SettlementDashboardArt SettlementMaterials SettlementPageArt SettlementPriceRenderer TownFoundingScreen CityInspectorScreen HousingInspector",
        "compat/economy": "CoinPresentationAccess CoinPricePresentation",
    },
    "JEMEconomy": {
        "client": "MarketScreen CoinPriceRenderer CoinAmounts",
        "api": "MarketCatalogSnapshot MarketItemSnapshot",
        "physical": "CoinDenomination CoinFormatter",
    },
    "JEMRoles": {
        "client/races": "RaceExperienceScreen RaceStageRenderer RacePanelMaterials",
        "workbench": "RacePowerStages BloomMechanics",
    },
}
PREFIX = "com.siirio.jemguisandbox.mirror."
ASSETS = {"jem_server": "jem_gui_demo_server", "jem_settlements": "jem_gui_demo_settlements", "jem_economy": "jem_gui_demo_economy",
          "jem_visual_sandbox": "jem_gui_demo_races", "jem_roles": "jem_gui_demo_roles"}


def relocate(text):
    text = re.sub(r"com\.siirio\.(jemserver|jemsettlements|jemeconomy|jemcurrency|jemvisualsandbox)(?=[.\"])",
                  lambda m: PREFIX + m[1], text)
    for old, new in ASSETS.items():
        text = text.replace('"' + old + '"', '"' + new + '"').replace('"' + old + ':', '"' + new + ':')
    return text


def replace_method(text, signature, body):
    start = text.index(signature)
    opening = text.index("{", start)
    depth, end = 1, opening + 1
    while depth:
        depth += (text[end] == "{") - (text[end] == "}")
        end += 1
    return text[:opening + 1] + "\n" + body + "\n    " + text[end - 1:]


def main():
    target = MODULE / "build/screen-sources"
    resources = MODULE / "build/screen-resources"
    # Both directories are build-owned and resolved inside this module.
    for folder in (target, resources):
        assert folder.resolve().is_relative_to(MODULE / "build")
        if folder.exists():
            shutil.rmtree(folder)
        folder.mkdir(parents=True)
    provenance = []
    for module, groups in SOURCES.items():
        namespace = "jemvisualsandbox" if module == "JEMRoles" else module.lower()
        for folder, names in groups.items():
            for name in names.split():
                source = ROOT / f"Mods/{module}/src/main/java/com/siirio/{namespace}/{folder}/{name}.java"
                original = source.read_text(encoding="utf-8")
                text = original
                if name == "SmpSidebar":
                    text = replace_method(text, "public static boolean openExternal(",
                        '        return com.siirio.jemguisandbox.Demo.openExternal(route);')
                if name == "JemMenuScreen":
                    text = replace_method(text, "private void activate(",
                        '''        if (slot == MAP_ACTION && mapLocation != null) {
            var dimension = net.minecraft.resources.ResourceLocation.tryParse(mapLocation.getString("jem_ui_dimension"));
            if (dimension != null) com.siirio.jemguisandbox.Demo.mapPreview(dimension, net.minecraft.core.BlockPos.of(mapLocation.getLong("jem_ui_position")));
            return;
        }
        com.siirio.jemguisandbox.Demo.menuAction(menu, slot);''')
                    text = text.replace('if (minecraft.getConnection() != null) minecraft.getConnection().send(new ServerboundRenameItemPacket(value));',
                        'com.siirio.jemguisandbox.Demo.notice("Demo text: " + value);')
                    text = replace_method(text, "public void onClose()", '        minecraft.setScreen(null);')
                    # AbstractContainerScreen.removed closes the real player's menu.
                    text = replace_method(text, "public void removed()", '        SmpRenderState.restoreAfterScreen();')
                if name == "MarketScreen":
                    text = text.replace("    MarketScreen(Screen parent,", "    public MarketScreen(Screen parent,")
                    text = text.replace("    void accept(MarketCatalogSnapshot next)", "    public void accept(MarketCatalogSnapshot next)")
                if name == "RaceExperienceScreen":
                    text = text.replace("    RaceExperienceScreen(", "    public RaceExperienceScreen(")
                text = relocate(text)
                dest = target / Path("com/siirio/jemguisandbox/mirror") / namespace / folder / (name + ".java")
                dest.parent.mkdir(parents=True, exist_ok=True)
                dest.write_text(text, encoding="utf-8")
                provenance.append({"source": source.relative_to(ROOT).as_posix(), "sha256": hashlib.sha256(source.read_bytes()).hexdigest()})
        # Keep localization keys/resource namespaces intact: these are native surfaces.
        asset_root = ROOT / f"Mods/{module}/src/main/resources/assets"
        if asset_root.exists():
            for asset in asset_root.rglob("*"):
                if not asset.is_file():
                    continue
                relative = asset.relative_to(asset_root)
                path = relative.as_posix()
                # Admit presentation resources only. Workbench provenance, VFX,
                # unrelated source experiments and world data are not gallery inputs.
                if not any(part in path for part in ("/lang/", "/textures/gui/", "/font/", "/textures/font/", "/textures/item/", "/models/item/")):
                    continue
                namespace = ASSETS.get(relative.parts[0], relative.parts[0])
                dest = resources / "assets" / namespace / Path(*relative.parts[1:])
                dest.parent.mkdir(parents=True, exist_ok=True)
                if asset.suffix in (".json", ".mcmeta"):
                    value = asset.read_text(encoding="utf-8")
                    for old, new in ASSETS.items():
                        value = value.replace(old + ":", new + ":")
                    dest.write_text(value, encoding="utf-8")
                else:
                    shutil.copyfile(asset, dest)
    (resources / "jem-gui-sandbox-sources.json").write_text(json.dumps(provenance, indent=2) + "\n")
    coin_models = resources / "assets/jem_gui_sandbox/models/item"
    coin_models.mkdir(parents=True, exist_ok=True)
    for coin in ("copper", "silver", "gold", "aether", "arcane"):
        source = resources / f"assets/jem_gui_demo_economy/models/item/{coin}_coin.json"
        if not source.exists():
            raise FileNotFoundError(source)
        shutil.copyfile(source, coin_models / source.name)
    print(f"Prepared {len(provenance)} relocated native presentation sources")


if __name__ == "__main__":
    main()
