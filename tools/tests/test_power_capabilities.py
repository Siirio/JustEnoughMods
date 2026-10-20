"""Installed-version capability contracts. No Minecraft launch or observed-DPS claim."""
import json
import pathlib
import tomllib
import unittest
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
IDS = {"the_incinerator", "the_annihilator", "soul_render", "cursed_bow", "wrath_of_the_desert"}
PREFIX = "data/cataclysm/capabilities/weapons/"


def installed(pattern):
    matches = list((ROOT / "Client/ManagedPack/mods").glob(pattern))
    if len(matches) != 1:
        raise AssertionError(f"Expected one installed {pattern}: {matches}")
    return zipfile.ZipFile(matches[0])


class NativeCapabilityContracts(unittest.TestCase):
    def test_all_native_conflicts_are_owned(self):
        with installed("p1nero_ec-*.jar") as dedicated, installed("EpicFight_X_L_Enders_Cataclysm-*.jar") as generic:
            overlapping = {n for n in set(dedicated.namelist()) & set(generic.namelist())
                           if n.startswith(PREFIX) and n.endswith(".json")
                           and json.loads(dedicated.read(n)) != json.loads(generic.read(n))}
            self.assertEqual(overlapping, {f"{PREFIX}{name}.json" for name in IDS})

    def test_native_attributes_and_distinct_types_survive(self):
        with installed("p1nero_ec-*.jar") as dedicated:
            types = set()
            for name in IDS:
                resource = f"{PREFIX}{name}.json"
                authored = json.loads((ROOT / "Mods/JEMPackCore/src/main/resources" / resource).read_text())
                self.assertEqual(authored, json.loads(dedicated.read(resource)), name)
                self.assertTrue(authored["type"].startswith("p1nero_ec:"))
                types.add(authored["type"])
            self.assertEqual(len(types), 5)

    def test_pack_order_declares_both_providers(self):
        data = tomllib.loads((ROOT / "Mods/JEMPackCore/src/main/resources/META-INF/mods.toml").read_text())
        dependencies = {d["modId"]: d for d in data["dependencies"]["jem_pack_core"]}
        for mod in ("p1nero_ec", "epicfight_cataclysm"):
            self.assertEqual(dependencies[mod]["ordering"], "AFTER")
            self.assertFalse(dependencies[mod]["mandatory"])
            self.assertEqual(dependencies[mod]["side"], "BOTH")


if __name__ == "__main__":
    unittest.main()
