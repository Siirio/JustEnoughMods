"""Deterministic presentation migration for the existing native FTB book.

Dry run is the default. No objectives, rewards, identities or unlock rules are
invented here. The receipt keeps the complete original native definitions.
"""
from __future__ import annotations

import argparse
import copy
import json
import math
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
SOURCE = Path(__file__).with_name("book.json")
REPORT = ROOT / "Docs/quests/rescue-v3"
MAINS = ["tab-16", "tab-17", "tab-07", "tab-03", "tab-12", "tab-26", "tab-25"]
THEMES = ["biomes", "companions", "home", "food", "engineering", "magic", "bosses"]
DESTINATIONS = {
    "tab-01": 0, "tab-02": 6, "tab-03": 3, "tab-04": 3, "tab-05": 3,
    "tab-06": 3, "tab-07": 2, "tab-08": 2, "tab-09": 2, "tab-10": 2,
    "tab-11": 1, "tab-12": 4, "tab-13": 4, "tab-14": 4, "tab-15": 4,
    "tab-16": 0, "tab-17": 1, "tab-18": 1, "tab-19": 2, "tab-20": 0,
    "tab-21": 0, "tab-22": 0, "tab-23": 6, "tab-24": 0, "tab-25": 6,
    "tab-26": 5, "tab-27": 6, "tab-28": 6, "tab-29": 6, "tab-30": 6,
    "tab-31": 6, "tab-32": 2, "nightfall-gear": 6, "enchanted-tools": 5,
    "campaign-gates": 6, "sea-friends": 1, "sky-roads": 0, "end-wilds": 0,
}
TITLES = [
    ("Very Many Biomes", "Очень много биомов"), ("Acompany", "В хорошей компании"),
    ("Cozy Home", "Уютный дом"), ("Farm 'n Food", "С грядки на стол"),
    ("Cogs & Trains", "Шестерни и поезда"), ("Secrets of Magic", "Тайны магии"),
    ("Boss Busters", "Охотники на боссов"),
]


def destination(chapter, card):
    key = card["id"]
    # Existing foundation/tool cards belong to the home entry, travel stays here.
    if key in {"start_01", "start_02", "start_03", "start_06", "field_08", "field_09", "field_10"}:
        return 2
    if chapter == "tab-19" and (key in {"vanilla_07", "vanilla_08", "vanilla_13", "vanilla_14", "vanilla_15"}):
        return 1
    return DESTINATIONS[chapter]


def label(chapter, key, en, ru, x, y, width=8):
    chapter.setdefault("section_labels", []).append({
        "id": key, "en_us": en, "ru_ru": ru, "x": x, "y": y,
        "width": width, "height": 1.1,
    })


def lane(cards, x, y, columns=5, dx=2.35, dy=2.5, bend=True, ripple=0):
    """Short folded rows, with stable local order and optional tributary bends."""
    for i, card in enumerate(cards):
        row, col = divmod(i, columns)
        if bend and row % 2:
            col = columns - col - 1
        card["position"] = [round(x + col * dx + ripple * math.sin(row * 1.3), 3),
                            round(y + row * dy + ripple * math.sin(col * 1.7), 3)]


def split_sources(cards):
    result = defaultdict(list)
    for card in cards:
        result[card["layout_source_chapter"]].append(card)
    return result


def settle(chapter):
    """Resolve genuine button/caption collisions without changing any graph edge."""
    hx, hy = chapter["hero"]["x"], chapter["hero"]["y"]
    half_h = chapter["hero"]["height"] / 2
    obstacles = [(hx-7.7,hy-half_h-.9,hx+7.7,hy+half_h+2)]
    if chapter["theme"] == "bosses": obstacles = [(-6,-3.5,6,3.4)]
    obstacles += [(s["x"]-s["width"]/2-.5,s["y"]-.9,s["x"]+s["width"]/2+.5,s["y"]+.9)
                  for s in chapter["section_labels"]]
    placed=[]
    offsets=sorted(((i*.5,j*.5) for i in range(-16,17) for j in range(-16,17)),key=lambda p:(p[0]**2+p[1]**2,p[1],p[0]))
    for card in chapter["cards"]:
        x,y=card["position"]
        for dx,dy in offsets:
            xx,yy=x+dx,y+dy
            if abs(xx)>19: continue
            if any(a<xx<b and c<yy<d for a,c,b,d in obstacles): continue
            if any(math.hypot(xx-ox,yy-oy)<1.75 for ox,oy in placed): continue
            card["position"]=[round(xx,3),round(yy,3)]
            placed.append((xx,yy))
            break
        else: raise ValueError(f"No local space for {card['id']}")


def layout(chapter, index):
    cards = chapter["cards"]
    sources = split_sources(cards)
    chapter["images"] = []
    chapter["section_labels"] = []
    chapter.pop("portal_image", None)
    # These bounds are presentation obstacles, never inputs to quest spacing.
    hero_y = -0.5
    if index == 0:
        groups = [
            ("field", ["tab-01", "tab-16", "sky-roads"], -17.0, -10.0, 5, "First horizons", "За горизонт"),
            ("sea", ["tab-20", "tab-21"], 8.3, -9.5, 5, "Sea and ancient ice", "Море и древний лёд"),
            ("caves", ["tab-22"], -16.0, 4.5, 8, "Worlds below", "Миры под землёй"),
            ("otherworlds", ["tab-24", "end-wilds"], 5.5, 4.5, 6, "Beyond the familiar", "По ту сторону"),
        ]
        for key, origins, x, y, cols, en, ru in groups:
            branch = [c for origin in origins for c in sources[origin]]
            # Regional rows have unequal elbows and occasional isolated discoveries.
            lane(branch, x, y, cols, dx=2.25, dy=2.3, ripple=.32)
            label(chapter, key, en, ru, x + (cols - 1)*1.125, y-1.5, cols*2.25)
        hero_y = -.2
    elif index == 1:
        pasture=[]
        wild=[]
        for card in sources["tab-17"]:
            ids={identity for binding in card["bindings"] for target in binding["targets"] for identity in target["ids"]}
            (pasture if any(identity.startswith("barnyardbuddies:") or identity in {
                "minecraft:cow","minecraft:sheep","minecraft:pig","minecraft:chicken","minecraft:horse",
                "minecraft:donkey","minecraft:mule","minecraft:rabbit","minecraft:goat"} for identity in ids) else wild).append(card)
        groups = [
            ("bond", sources["tab-18"], -15.5, -9, 5, "A bond and a home", "Друг и его дом"),
            ("pasture", pasture + sources["tab-11"] + sources["tab-19"], 7, -8.8, 5, "Hooves and wheels", "Копыта и колёса"),
            ("wild", wild, -15.5, 4.8, 8, "Wild neighbours", "Дикие соседи"),
            ("water", sources["sea-friends"], 7, 4.8, 5, "Under the surface", "Под водой"),
        ]
        for key, group, x, y, cols, en, ru in groups:
            lane(group, x, y, cols, dx=2.3, dy=2.4, ripple=.4)
            label(chapter, key, en, ru, x+(cols-1)*1.15, y-1.7, cols*2.3)
    elif index == 2:
        decor = sources["tab-07"]
        # The thirteen actual decor goals use the approved asymmetrical composition.
        centers = [(-9,-8),(-15,-3),(-10,4),(-3,6),(1,6),(5,6),(15,-3),(12,-8),(7,-8),(-4,-3),(15,4),(0,-3),(4,-3)]
        for i, card in enumerate(decor):
            card["position"] = list(centers[i])
        # Small related objects join boxes, never another navigation chapter.
        extras = sources["tab-19"]
        for i, card in enumerate(extras):
            cx, cy = centers[i % len(centers)]
            card["position"] = [cx + (2.1 if i % 2 else -2.1), cy]
        label(chapter, "gallery", "Make it yours", "Всё по-своему", 0, -10.6, 22)
        lane(sources["tab-08"], -15, 7.8, 6)
        label(chapter, "memories", "Little stories", "Маленькие истории", -9.2, 6.2, 14)
        lane(sources["tab-09"], 8, -9, 4)
        label(chapter, "storage", "Room for everything", "Место для всего", 12.2, -10.5, 13)
        lane(sources["tab-01"], -15, -10, 4)
        label(chapter, "tools", "A first workshop", "Первая мастерская", -11, -11.5, 16)
        lane(sources["tab-10"], 3, 8, 6)
        lane(sources["tab-32"], -16,-3,4)
        label(chapter, "neighbours", "A place to stay", "Место для жизни", 8.5, 6.5, 14)
        # Keep the large home title in the central opening, above the utility wing.
        hero_y = 1
    elif index == 3:
        for key, origins, x, y, cols, en, ru in [
            ("garden", ["tab-03"], -14,-10,6,"Garden and seasons","Грядки и времена года"),
            ("kitchen", ["tab-04"], 6.8,-10,5,"The warm kitchen","Тёплая кухня"),
            ("drinks", ["tab-05"], -14,6.5,7,"A little tavern","Маленькая таверна"),
            ("fishing", ["tab-06"], 6.8,6.5,5,"A good catch","Хороший улов"),
        ]:
            lane([c for origin in origins for c in sources[origin]],x,y,cols)
            label(chapter,key,en,ru,x+(cols-1)*1.175,y-1.6,cols*2.35)
    elif index == 4:
        for key, origin, x, y, cols, en, ru in [
            ("motion","tab-12",-14,-10,5,"Make it move","Привести в движение"),
            ("factory","tab-13",6.8,-10,5,"The busy workshop","Шумная мастерская"),
            ("cargo","tab-14",-14,6.8,5,"Nothing stands still","Всё в движении"),
            ("rail","tab-15",6.8,6.8,5,"All aboard!","По вагонам!"),
        ]:
            lane(sources[origin],x,y,cols,dy=2.6)
            label(chapter,key,en,ru,x+(cols-1)*1.175,y-1.6,cols*2.35)
    elif index == 5:
        spells = sources["tab-26"]
        lane(spells[:9],-15,-10,4,dx=2.6,dy=2.8,ripple=.2)
        lane(spells[9:],8,-10,4,dx=2.6,dy=2.8,ripple=.25)
        lane(sources["enchanted-tools"],-10,10.5,6,dx=4)
        label(chapter,"craft","Wonders in your hands","Чудеса в руках",-11,-11.6,13)
        label(chapter,"spell","Words of power","Слова силы",12,-11.6,12)
        label(chapter,"enchant","An enchanted edge","Зачарованная грань",0,8.9,18)
    else:
        # Twelve authentic MAIN paths converge visually. Rails are artwork only.
        eyes = [c for c in sources["tab-28"] if c["id"] not in {"eyes_01","eyes_02","eyes_15"}]
        assert len(eyes) == 12
        for i, card in enumerate(eyes):
            side = -1 if i < 6 else 1
            yy = -10 + (i % 6)*4
            card["position"] = [side*16, yy]
            chapter["images"].append({"image":"ftbquests:textures/shapes/square/background.png", "x":side*12.6,"y":yy,"width":4.7,"height":.06,"color":0xBBA477,"order":-15})
            chapter["images"].append({"image":"ftbquests:textures/shapes/square/background.png", "x":side*9.9,"y":yy/2,"width":.06,"height":abs(yy),"color":0xBBA477,"order":-15})
        for c in sources["tab-28"]:
            if c["id"] == "eyes_15": c["position"] = [0,4.6]
            elif c["id"] == "eyes_01": c["position"] = [-4,-5.8]
            elif c["id"] == "eyes_02": c["position"] = [4,-5.8]
        label(chapter,"eyes","The Twelve Eyes","Двенадцать глаз",0,-8,18)
        # Show real preparation, native weapon acquisition and power beside the
        # twelve paths in the first viewport. Their continuations stay nearby.
        prep=sources["tab-25"][:5]
        night=[c for c in sources["nightfall-gear"] if c["id"].startswith("nightfall_acquire_")]
        lane(prep,-7.5,-11,3,dx=2.4)
        lane(night,3,-11,3,dx=2.4)
        lane(sources["tab-02"],-6.5,7,5,dx=3.1)
        label(chapter,"first_arms","Ready for battle","Перед боем",-5.1,-12.5,9)
        label(chapter,"night_blades","Blades of dusk","Сумрачные клинки",5.4,-12.5,10)
        label(chapter,"first_power","Strength and survival","Сила и выживание",0,5.3,18)
        # Preparation is a compact outer wing, not false bracket prerequisites.
        for key, origins, x, y, cols, en, ru in [
            ("arms",["tab-25","nightfall-gear"],-16,-20,10,"Ready for the fight","Готов к бою"),
            ("hunt",["tab-27","tab-29"],-16,15,8,"Tracks and trophies","Следы и трофеи"),
            ("depth",["tab-23"],-16,27,10,"Keep your footing","Не потерять опору"),
            ("gates",["campaign-gates","tab-30"],-16,38,10,"Beyond the main road","В стороне от пути"),
            ("events",["tab-31"],-16,49,7,"Adventures together","Общие приключения"),
        ]:
            lane([c for origin in origins for c in sources[origin] if c not in prep and c not in night],x,y,cols)
            label(chapter,key,en,ru,x+(cols-1)*1.175,y-1.6,cols*2.35)
    hero_h=13.8*(1792/2398 if index==1 else 1536/2752)
    chapter["title_caption"] = {"x":0,"y":hero_y+hero_h/2+1,"width":18,"height":1.3}
    chapter["hero"] = {"x":0,"y":hero_y,"width":13.8,"height":hero_h}
    chapter["entry_bounds"] = [-18,-12,19,12]
    if index == 2: chapter["entry_bounds"] = [-18,-12,19,12]
    if index == 6: chapter["entry_bounds"] = [-18,-12,18,12]
    settle(chapter)
    # Fit the same composition with the native sidebar open or closed. This
    # adjusts saved presentation geometry only, never a render-only transform.
    horizontal = [.90, .93, .96, 1.03, 1.03, 1.0, .98][index]
    for card in cards: card["position"][0] = round(card["position"][0]*horizontal,3)
    for image in chapter["images"] + chapter["section_labels"]:
        image["x"] *= horizontal
        image["width"] *= horizontal
    chapter["hero"]["width"] *= horizontal
    chapter["hero"]["height"] *= horizontal
    chapter["autofocus_id"] = min(cards,key=lambda c: c["position"][0]**2+c["position"][1]**2)["ftb_id"]


def migrate(book):
    if book.get("revision") == "questbook_rescue_v3":
        for i,c in enumerate(book["chapters"]): layout(c,i)
        return book
    old = {c["id"]: c for c in book["chapters"]}
    chapters = []
    for i, key in enumerate(MAINS):
        c = copy.deepcopy(old[key])
        c.update(cards=[], group=None, order=i, routes=[], quest_routes=[], theme=THEMES[i], section_labels=[])
        c.pop("back_position",None)
        c.pop("background",None)
        c.pop("regional_arms",None)
        c["localization"] = {lang:{"title":title,"intro":old[key]["localization"][lang]["intro"]} for lang,title in zip(("en_us","ru_ru"),TITLES[i])}
        chapters.append(c)
    for original in book["chapters"]:
        for card in original["cards"]:
            card = copy.deepcopy(card)
            card["layout_source_chapter"] = original["id"]
            chapters[destination(original["id"],card)]["cards"].append(card)
    for i,c in enumerate(chapters):
        layout(c,i)
        c["layout_pattern"] = ["regional_constellation","river_delta","decor_gallery","productive_patchwork","kinetic_rail_network","arcane_web","twelve_eye_bracket"][i]
        c["visual_status"] = "NOT_VISUALLY_VERIFIED"
    book.update(chapters=chapters,navigation_groups=[],revision="questbook_rescue_v3")
    book.pop("hub",None)
    return book


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply",action="store_true")
    args=parser.parse_args()
    original=json.loads(SOURCE.read_text("utf8"))
    updated=migrate(copy.deepcopy(original))
    old={c["id"]:c for ch in original["chapters"] for c in ch["cards"]}
    new={c["id"]:c for ch in updated["chapters"] for c in ch["cards"]}
    assert old.keys()==new.keys()
    for key in old:
        for field,value in old[key].items():
            if field not in {"position","layout_source_chapter"}: assert value == new[key][field],(key,field)
    summary={"apply":args.apply,"chapters":len(updated["chapters"]),"cards":len(new),"progress_contracts_changed":0,
             "counts":{c["id"]:len(c["cards"]) for c in updated["chapters"]}}
    print(json.dumps(summary,indent=2))
    if args.apply: SOURCE.write_text(json.dumps(updated,ensure_ascii=False,indent=2)+"\n",encoding="utf8")


if __name__=="__main__": main()
