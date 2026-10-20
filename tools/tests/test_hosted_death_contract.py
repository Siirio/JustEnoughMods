"""Source safety contracts for actual hosted death; these do not simulate Minecraft."""
import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
SOURCE = ROOT / "Mods/JEMWorldBossTiers/src/main/java/com/siirio/jemworldbosstiers/encounter/HostedEncounters.java"


def method(source, signature):
    start = source.index(signature)
    brace = source.index("{", start)
    depth = 1
    cursor = brace + 1
    while depth:
        depth += (source[cursor] == "{") - (source[cursor] == "}")
        cursor += 1
    return source[brace + 1:cursor - 1]


class HostedDeathContracts(unittest.TestCase):
    def test_actual_death_cannot_grant_extra_life(self):
        body = method(SOURCE.read_text(), "public static void death(LivingDeathEvent event)")
        for forbidden in ("setCanceled", "setHealth", "teleport", 'putString("State", DOWNED)',
                          'putLong("ImmuneUntil"', 'putBoolean("DownedUsed"'):
            self.assertNotIn(forbidden, body)
        self.assertIn("eliminate(player, entry);", body)

    def test_existing_canceled_death_abilities_keep_ownership(self):
        text = SOURCE.read_text()
        annotation = text[:text.index("public static void death(LivingDeathEvent event)")].rsplit("@SubscribeEvent", 1)[1]
        self.assertNotIn("receiveCanceled = true", annotation)
        self.assertIn("EventPriority.LOWEST", annotation)

    def test_elimination_removes_active_target_eligibility(self):
        text = SOURCE.read_text()
        eliminate = method(text, "private static void eliminate(ServerPlayer player, CompoundTag entry)")
        self.assertRegex(eliminate, r'entry\.putString\("State",\s*ELIMINATED\)')
        self.assertIn("remove(DOWNED_MARKER)", eliminate)
        target = method(text, "private static boolean validTarget(LivingEntity boss, ServerPlayer player)")
        self.assertIn('ALIVE.equals(participant(boss, player.getUUID()).getString("State"))', target)
        self.assertIn("player.isAlive()", target)
        self.assertIn("!player.isSpectator()", target)


if __name__ == "__main__":
    unittest.main()
