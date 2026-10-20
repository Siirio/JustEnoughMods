"""Execute unchanged Java scene methods with a minimal player/server boundary.

This verifies packet replay/state transitions offline; it does not simulate Forge
events, networking or Minecraft player acceptance.
"""
from pathlib import Path
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[5]
SOURCE = ROOT / "Mods/JEMVisualSandboxR3/src/main/java/com/siirio/jemvisualsandbox/workbench/RaceExperience.java"


def method(text, signature):
    start = text.index(signature)
    opening = text.index("{", start)
    depth = 1
    end = opening + 1
    while depth:
        depth += (text[end] == "{") - (text[end] == "}")
        end += 1
    return text[start:end]


class RaceSelectionProtectionTest(unittest.TestCase):
    def test_actual_scene_methods_reject_reentry_and_expiry_extension(self):
        source = SOURCE.read_text(encoding="utf-8")
        methods = "\n".join(method(source, signature) for signature in (
            "public static void cinematic(",
            "private static void completeSelectionScene(",
            "private static boolean locked(",
            "private static boolean selectionPending(",
        ))
        harness = """
import java.util.*;
public final class RaceSceneContract {
  record Scene(Object dimension,Object anchor,long expires,boolean completing) {}
  static final Map<UUID,Scene> SCENES=new HashMap<>();
  static final class Clock { long now; Clock overworld(){return this;} long getGameTime(){return now;} }
  static final class Level { Object dimension(){return "overworld";} }
  static final class ServerPlayer {
    boolean chosen,authorized=true; final UUID id=UUID.randomUUID(); final Clock server=new Clock();
    UUID getUUID(){return id;} Object position(){return "anchor";} Level serverLevel(){return new Level();}
  }
  static boolean allowed(ServerPlayer p){return p.authorized;}
  static boolean selected(ServerPlayer p){return p.chosen;}
  static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
  public static void main(String[] args){
    var p=new ServerPlayer();
    cinematic(p,true); check(locked(p),"first choice remains protected");
    p.server.now=300;cinematic(p,true);
    check(SCENES.get(p.id).expires==600,"input cannot extend original first-choice scene");
    p.chosen=true;completeSelectionScene(p);
    check(SCENES.get(p.id).expires==400,"confirmation preserves exit animation window");
    for(int tick=301;tick<400;tick++){p.server.now=tick;completeSelectionScene(p);cinematic(p,true);}
    check(SCENES.get(p.id).expires==400,"same-race selection replay never extends exit immunity");
    p.server.now=400;check(!locked(p),"selected protection expires");
    cinematic(p,false);check(!SCENES.containsKey(p.id),"close clears scene");
    for(int tick=400;tick<1600;tick++){p.server.now=tick;cinematic(p,false);cinematic(p,true);}
    check(!locked(p)&&!SCENES.containsKey(p.id),"selected reentry cannot create immunity");
    p.chosen=false;cinematic(p,true);p.server.now=3000;
    check(locked(p),"unselected mandatory choice remains protected after initial timeout");
    p.chosen=true;completeSelectionScene(p);
    check(SCENES.get(p.id).expires==3100,"late first selection still protects its exit animation");
    p.server.now=3050;completeSelectionScene(p);
    check(SCENES.get(p.id).expires==3100,"late confirmation replay cannot renew exit protection");
    cinematic(p,false);p.authorized=false;cinematic(p,true);
    check(!SCENES.containsKey(p.id),"unauthenticated input creates no scene");
    System.out.println("Race scene packet replay and first-choice contracts passed");
  }
""" + methods + "\n}"
        with tempfile.TemporaryDirectory(prefix="jem-race-scene-") as directory:
            java = Path(directory) / "RaceSceneContract.java"
            java.write_text(harness, encoding="utf-8")
            subprocess.run(["javac", "-encoding", "UTF-8", str(java)], check=True, capture_output=True)
            result = subprocess.run(["java", "-cp", directory, "RaceSceneContract"], check=True, capture_output=True, text=True)
            self.assertIn("contracts passed", result.stdout)


if __name__ == "__main__":
    unittest.main()
