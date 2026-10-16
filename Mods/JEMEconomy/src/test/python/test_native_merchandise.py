"""Production-data contract checks; does not claim Forge lifecycle/runtime validation."""
import collections
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[5]
DATA = ROOT / "Mods/JEMEconomy/src/main/resources/data/jem_economy/economy"

class NativeMerchandiseContracts(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.native = json.loads((DATA / "native_item_prices.json").read_text(encoding="utf-8"))
        cls.pools = json.loads((DATA / "profession_pools.json").read_text(encoding="utf-8"))
        cls.market = json.loads((DATA / "item_policies.json").read_text(encoding="utf-8"))

    def test_no_conflicting_item_prices_between_professions(self):
        prices = {}
        for row in self.native + self.pools:
            pair = row["buy_copper"], row["sell_copper"], row.get("sell_units", 1)
            self.assertEqual(prices.setdefault(row["item"], pair), pair, row["item"])
            self.assertGreater(pair[0] * pair[2], pair[1] * 2, row["item"])
            self.assertGreater(pair[2], 0)
            self.assertGreater(pair[1], 0)

    def test_every_pool_uses_exact_atlas_record_not_asset_pseudo_id(self):
        ids = set()
        for file in (ROOT / "Catalog/registries/items").glob("*.json"):
            ids.update(row["registry_id"] for row in json.loads(file.read_text(encoding="utf-8")).get("records", []))
        for row in self.pools:
            self.assertIn(row["item"], ids)
            self.assertNotIn("tooltip", row["item"])
            self.assertNotIn("/", row["item"])

    def test_native_identity_including_nbt_merchandise_is_covered(self):
        ids = {r["item"] for r in self.native}
        self.assertTrue({"minecraft:enchanted_book", "minecraft:diamond_chestplate", "minecraft:filled_map",
                         "minecraft:tipped_arrow", "minecraft:bread", "minecraft:rotten_flesh"} <= ids)
        self.assertFalse({"minecraft:emerald", "minecraft:nether_star", "minecraft:netherite_ingot",
                          "minecraft:elytra", "minecraft:dragon_egg"} & ids)

    def test_native_meals_do_not_change_global_market_admission(self):
        policies = {r["item"]: r for r in self.market}
        for item in ("minecraft:bread", "minecraft:cooked_beef", "minecraft:cooked_cod"):
            self.assertEqual(policies[item]["outcome"], "NONE")

    def test_gradual_unlock_weighted_alternatives_and_stock(self):
        groups = collections.defaultdict(list)
        for row in self.pools:
            self.assertIn(row["level"], (2, 3, 4, 5))
            self.assertGreater(row["weight"], 0)
            self.assertGreater(row["max_uses"], 0)
            self.assertLessEqual(row["max_uses"], 8)
            groups[row["profession"], row["level"]].append(row)
        self.assertEqual({level for _, level in groups}, {2, 3, 4, 5})
        self.assertTrue(any(len(rows) > 1 for rows in groups.values()))
        for rows in groups.values():
            self.assertEqual(len(rows), len({r["item"] for r in rows}))

    def test_recipe_batch_conversions_cannot_raise_native_buyback(self):
        p = {r["item"]: r for r in self.native}
        # Native crafts whose batch outputs historically undermine item-only pricing.
        recipes = [("minecraft:white_wool", 2, "minecraft:white_carpet", 3),
                   ("minecraft:glass", 6, "minecraft:glass_pane", 16),
                   ("minecraft:sugar_cane", 3, "minecraft:paper", 3),
                   ("minecraft:book", 3, "minecraft:bookshelf", 1)]
        market = {r["item"]: r for r in self.market if r["outcome"] == "BUY_AND_SELL"}
        for source, inputs, output, count in recipes:
            buy = market.get(source, p[source])["buy_copper"] * inputs
            row = market.get(output, p[output])
            maximum_payout = ((row["sell_copper"] * 150 // 100) * count if output in market else
                              max(1, (row["sell_copper"] * 150 * count + 100 * row.get("sell_units", 1) - 1) //
                                  (100 * row.get("sell_units", 1))))
            self.assertLessEqual(maximum_payout, buy, output)

if __name__ == "__main__":
    unittest.main()
