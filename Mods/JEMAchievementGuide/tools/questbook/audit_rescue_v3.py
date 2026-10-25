"""Requested before/after content, resource and geometry receipt. No game launch."""
from __future__ import annotations

import hashlib
import json
import math
from pathlib import Path
import zipfile

from audit_book import contract, inventory, read_snbt
from rescue_v3 import MAINS, REPORT, ROOT, SOURCE


def write(name, value):
    (REPORT/name).write_text(json.dumps(value,ensure_ascii=False,indent=2)+"\n",encoding="utf8")


def main():
    before=json.loads((REPORT/"before.json").read_text("utf8"))
    after=inventory()
    write("after.json",after)
    preservation={}
    for kind in ("cards","quests","tasks","rewards","reward_tables"):
        old,new=before[kind],after[kind]
        preservation[kind]={"before":len(old),"after":len(new),"missing":sorted(old.keys()-new.keys()),
                            "added":sorted(new.keys()-old.keys()),
                            "changed_contracts":[key for key in old.keys()&new.keys() if contract(old[key],kind)!=contract(new[key],kind)]}
    preservation["dependencies_unchanged"]=before["dependency_edges"]==after["dependency_edges"]
    preservation["duplicates"]=after["duplicates"]
    preservation["legacy_bytes_unchanged"]=before["legacy_source_sha256"]==after["legacy_source_sha256"]
    write("preservation.json",preservation)
    assert all(not row[key] for kind,row in preservation.items() if isinstance(row,dict) for key in ("missing","added","changed_contracts"))
    assert preservation["dependencies_unchanged"] and not preservation["duplicates"] and preservation["legacy_bytes_unchanged"]

    book=json.loads(SOURCE.read_text("utf8"))
    assert [c["id"] for c in book["chapters"]]==MAINS and not book["navigation_groups"]
    visible=[c for c in after["chapters"].values() if not c.get("always_invisible",False)]
    assert len(visible)==7
    textures=set()
    item_models=set()
    for jar in (ROOT/"Client/ManagedPack/mods").glob("*.jar"):
        with zipfile.ZipFile(jar) as z:
            textures.update(n for n in z.namelist() if n.startswith("assets/") and n.endswith((".png",".jpg")))
            item_models.update(n for n in z.namelist() if "/models/item/" in n)
    # Source resources override the previous installed Guide artifact.
    for p in (ROOT/"Mods/JEMAchievementGuide/src/main/resources/assets").rglob("*"):
        if p.is_file(): textures.add(p.relative_to(ROOT/"Mods/JEMAchievementGuide/src/main/resources").as_posix())
    # The actual Java client supplies vanilla item models and numbered compass frames.
    client_dir=Path.home()/"AppData/Roaming/.minecraft/versions"
    candidates=list(client_dir.glob("test jem/test jem.jar"))+list(client_dir.glob("1.20.1/1.20.1.jar"))
    for jar in candidates:
        with zipfile.ZipFile(jar) as z:
            textures.update(n for n in z.namelist() if n.startswith("assets/"))
            item_models.update(n for n in z.namelist() if "/models/item/" in n)
    # JEM item models may be build output from the same in-progress production build.
    for p in (ROOT/"Mods").glob("*/src/main/resources/assets/*/models/item/*.json"):
        item_models.add("assets/"+p.relative_to(p.parents[3]).as_posix())
    missing=[]
    for c in visible:
        for image in c.get("images",[]):
            location=image["image"]
            if location=="none": continue
            ns,path=location.split(":",1)
            if f"assets/{ns}/{path}" not in textures: missing.append(location)
    unresolved_icons=[]
    for q in after["quests"].values():
        icon=q.get("icon")
        if not isinstance(icon,str) or not icon or q.get("source_path","").split("/")[-1].startswith("_archive_"): continue
        ns,path=icon.split(":",1)
        if f"assets/{ns}/models/item/{path}.json" not in item_models: unresolved_icons.append({"quest":q["id"],"icon":icon})
    write("resources.json",{"missing_images":sorted(set(missing)),"unresolved_item_models":unresolved_icons,
                            "path_comparison":"case-sensitive","new_images_generated":False,
                            "title_alpha":"Original six supplied JPG emblems are opaque. No destructive alpha extraction performed.",
                            "boss_hero":"Native eye artwork and large localized heading. Dedicated Boss Busters title illustration is not available.",
                            "runtime":"NOT_VISUALLY_VERIFIED"})
    assert not missing,("Missing image resources",missing)
    assert not unresolved_icons,("Unresolved actual item models",unresolved_icons)

    geometry=[]
    for c in book["chapters"]:
        actual_hero=c["hero"] if c["theme"]!="bosses" else {"x":0,"y":-1,"width":3.4,"height":3.4,"kind":"native eye icon, dedicated banner unavailable"}
        points=[t["position"] for t in c["cards"]]
        bounds=[min(p[0] for p in points)-.625,min(p[1] for p in points)-.625,
                max(p[0] for p in points)+.625,max(p[1] for p in points)+.625]
        collisions=[]
        for i,t in enumerate(c["cards"]):
            for other in c["cards"][i+1:]:
                if math.dist(t["position"],other["position"])<1.5:collisions.append([t["id"],other["id"]])
        entry=c["entry_bounds"]
        core=[p for p in points if entry[0]<=p[0]<=entry[2] and entry[1]<=p[1]<=entry[3]]
        projections=[]
        for sidebar in (False,True):
            # Exact GUI scale and expanded sidebar width are measured only in game.
            panel_w,panel_h=1920/3-40,1080/3-2
            sb=170 if sidebar else 0
            available=max(160,panel_w-sb-12)
            zoom=max(4,min(16,round(min(available*.84/34,panel_h*.84/24)/1.5)))
            unit=1.5*zoom
            projections.append({"sidebar_expanded":sidebar,"assumed_gui_scale":3,"zoom":zoom,
                                "normal_button_px":unit*3,"milestone_button_px":unit*3*1.25,
                                "core_width_ratio":(max(p[0] for p in core)-min(p[0] for p in core)+1)*unit/available,
                                "core_height_ratio":(max(p[1] for p in core)-min(p[1] for p in core)+1)*unit/panel_h,
                                "hero_width_ratio":actual_hero["width"]*unit/available})
        geometry.append({"chapter":c["id"],"cards":len(points),"initial_cards":len(core),"quest_bounds":bounds,
                         "hero_bounds":actual_hero,"entry_bounds":entry,"collisions":collisions,
                         "projection_only":projections,"in_game_measurement":"PENDING_USER_CHECK"})
        assert not collisions,(c["id"],collisions)
    write("geometry.json",geometry)
    print(json.dumps({"visible_chapters":len(visible),"counts":after["counts"],"preservation":"unchanged",
                      "image_paths":"resolved","icon_models":"resolved","collisions":0,
                      "visual_acceptance":"NOT_VISUALLY_VERIFIED"},indent=2))


if __name__=="__main__": main()
