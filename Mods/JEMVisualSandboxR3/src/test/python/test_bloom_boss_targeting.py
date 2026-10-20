"""Run actual target and optional bridge methods against minimal API boundaries."""
from pathlib import Path
import subprocess
import tempfile
import unittest

from test_race_selection_protection import ROOT, method


class BloomBossTargetingTest(unittest.TestCase):
    def test_canonical_profiles_preserve_boss_targeting(self):
        owner = ROOT / "Mods/JEMVisualSandboxR3/src/main/java/com/siirio/jemvisualsandbox/workbench"
        target = method((owner / "SandboxRaceTraits.java").read_text(encoding="utf-8"), "public static void target(")
        bridge = method((owner / "ProductionRaceBridge.java").read_text(encoding="utf-8"), "public static boolean registeredBoss(")
        call = method((owner / "ProductionRaceBridge.java").read_text(encoding="utf-8"), "private static Object call(")
        harness = """
import java.util.*;
import net.minecraft.world.entity.LivingEntity;
public final class BloomBossContract {
 static class ServerPlayer extends LivingEntity { int race=2; boolean green=true; Object serverLevel(){return this;} Object blockPosition(){return this;} }
 static class EnderMan extends LivingEntity {} static class EnderDragon extends LivingEntity {} static class AbstractPiglin extends LivingEntity {}
 static class RaceExperience { static boolean selected(ServerPlayer p){return true;} static int race(ServerPlayer p){return p.race;} }
 static class SandboxEnvironment { static boolean green(Object level,Object pos){return ((ServerPlayer)level).green;} }
 static boolean owner(ServerPlayer p){return true;}
 static class LivingChangeTargetEvent {
   LivingEntity actor; ServerPlayer player; boolean canceled;
   LivingChangeTargetEvent(LivingEntity a,ServerPlayer p){actor=a;player=p;}
   LivingEntity getEntity(){return actor;} LivingEntity getNewTarget(){return player;} void setCanceled(boolean value){canceled=value;}
 }
 static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
 static boolean canceled(LivingEntity actor,ServerPlayer p){var e=new LivingChangeTargetEvent(actor,p);target(e);return e.canceled;}
 public static void main(String[] args){
   var p=new ServerPlayer();var ordinary=new LivingEntity();var boss=new LivingEntity();boss.boss=true;
   check(canceled(ordinary,p),"ordinary mob passive retained");
   check(!canceled(boss,p),"registered boss can target Bloom on green");
   boss.boss=false;check(canceled(boss,p),"membership follows canonical registry rather than cached local list");
   boss.boss=true;p.green=false;check(!canceled(ordinary,p),"barren-ground behavior retained");p.green=true;
   int calls=com.siirio.jemworldbosstiers.api.WorldTierApi.calls;
   net.minecraftforge.fml.ModList.loaded=false;
   check(canceled(ordinary,p),"optional module absent preserves ordinary passive");
   check(calls==com.siirio.jemworldbosstiers.api.WorldTierApi.calls,"absent module API never called");
   p.race=1;ordinary.type="minecraft:endermite";check(canceled(ordinary,p),"Void native kin unchanged");
   p.race=4;check(canceled(new AbstractPiglin(),p),"Nether piglin passive unchanged");
   System.out.println("Bloom canonical boss exemption and ordinary passive contracts passed");
 }
 static class ProductionRaceBridge {
""" + call + "\n" + bridge + "\n}\n" + target + "\n}"
        files = {
            "BloomBossContract.java": harness,
            "net/minecraft/world/entity/LivingEntity.java": """
package net.minecraft.world.entity;
public class LivingEntity { public boolean boss; public String type="ordinary:mob"; public Object getType(){return type;} }
""",
            "net/minecraftforge/fml/ModList.java": """
package net.minecraftforge.fml;
public final class ModList { public static boolean loaded=true; public static ModList get(){return new ModList();} public boolean isLoaded(String id){if(!id.equals("jem_world_boss_tiers"))throw new AssertionError(id);return loaded;} }
""",
            "net/minecraft/core/registries/BuiltInRegistries.java": """
package net.minecraft.core.registries;
public final class BuiltInRegistries { public static final Registry ENTITY_TYPE=new Registry(); public static class Registry { public Object getKey(Object type){return type;} } }
""",
            "com/siirio/jemworldbosstiers/api/WorldTierApi.java": """
package com.siirio.jemworldbosstiers.api;
import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
public final class WorldTierApi { public static int calls; public static Optional<Object> profile(LivingEntity entity){calls++;return entity.boss?Optional.of(entity):Optional.empty();} }
""",
        }
        with tempfile.TemporaryDirectory(prefix="jem-bloom-boss-") as directory:
            paths = []
            for relative, text in files.items():
                path = Path(directory) / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(text, encoding="utf-8")
                paths.append(str(path))
            subprocess.run(["javac", "-encoding", "UTF-8", *paths], check=True, capture_output=True)
            result = subprocess.run(["java", "-cp", directory, "BloomBossContract"], check=True, capture_output=True, text=True)
            self.assertIn("contracts passed", result.stdout)


if __name__ == "__main__":
    unittest.main()
