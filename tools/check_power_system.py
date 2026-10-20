"""Explicit offline power-system regression checks; never launches a game/server."""
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]


def run(*args):
    subprocess.run([str(arg) for arg in args], cwd=ROOT, check=True)


def main():
    java_home = Path(os.environ["JAVA_HOME"]) / "bin" if os.environ.get("JAVA_HOME") else None
    javac = java_home / "javac.exe" if java_home else shutil.which("javac")
    java = java_home / "java.exe" if java_home else shutil.which("java")
    if not javac or not java:
        raise SystemExit("A JDK with javac and java is required")
    for script in (
        "tools/tests/test_power_capabilities.py",
        "tools/tests/test_hosted_death_contract.py",
        "Mods/JEMVisualSandboxR3/src/test/python/test_race_selection_protection.py",
        "Mods/JEMVisualSandboxR3/src/test/python/test_bloom_boss_targeting.py",
    ):
        run(sys.executable, ROOT / script)
    with tempfile.TemporaryDirectory(prefix="jem-power-contracts-") as temporary:
        module = ROOT / "Mods/JEMWorldBossTiers/src"
        classes = Path(temporary)
        sources = [
            module / "main/java/com/siirio/jemworldbosstiers/balance/GuardHitTracker.java",
            module / "test/java/com/siirio/jemworldbosstiers/balance/GuardHitTrackerTest.java",
            module / "main/java/com/siirio/jemworldbosstiers/diagnostic/CombatCaptureTotals.java",
            module / "test/java/com/siirio/jemworldbosstiers/diagnostic/CombatCaptureTotalsTest.java",
            module / "main/java/com/siirio/jemworldbosstiers/compat/irons/SiphonHealing.java",
            module / "test/java/com/siirio/jemworldbosstiers/compat/irons/SiphonHealingTest.java",
        ]
        run(javac, "--release", "17", "-proc:none", "-d", classes, *sources)
        run(java, "-ea", "-cp", classes, "com.siirio.jemworldbosstiers.balance.GuardHitTrackerTest")
        run(java, "-ea", "-cp", classes, "com.siirio.jemworldbosstiers.diagnostic.CombatCaptureTotalsTest")
        run(java, "-ea", "-cp", classes, "com.siirio.jemworldbosstiers.compat.irons.SiphonHealingTest")
        mixin_jars = sorted((ROOT / "Server/libraries/org/spongepowered").rglob("mixin-*.jar"))
        if not mixin_jars:
            raise SystemExit("Installed Forge Mixin dependency is required for the Frost contract")
        mixin = mixin_jars[-1]
        compat = ROOT / "Mods/JEMCompat/src"
        run(javac, "--release", "17", "-proc:none", "-cp", mixin, "-d", classes,
            compat / "main/java/com/siirio/jemcompat/mixin/worldtier/FrostbittenDifficultyMixin.java",
            compat / "test/java/com/siirio/jemcompat/mixin/worldtier/FrostbittenDifficultyContractTest.java")
        run(java, "-ea", "-cp", os.pathsep.join((str(classes), str(mixin))),
            "com.siirio.jemcompat.mixin.worldtier.FrostbittenDifficultyContractTest",
            ROOT / "Mods/JEMPackCore/src/main/resources/data/jem_world_boss_tiers/jem_world_boss_tiers/bosses/expanded_bosses.json")
    print("Offline owner contracts passed; this does not establish in-game DPS, Mixin application or player acceptance.")


if __name__ == "__main__":
    main()
