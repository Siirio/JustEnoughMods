#!/usr/bin/env python3
"""Export read-only GUI reference attachments, never canonical Atlas gameplay facts.

The shared Atlas CLI has no GUI adapter. This receipt deliberately stays outside
its generated/atlas export and does not claim runtime screen/control coverage.
PNG bytes are copied unchanged; no pixels, crops or resource packs are authored.
"""
from __future__ import annotations

import argparse
import hashlib
import html
import io
import json
import re
import struct
import tomllib
import zipfile
from collections import defaultdict
from pathlib import Path, PurePosixPath
from urllib.parse import quote, unquote

ROOT = Path(__file__).resolve().parents[2]
GUI_PATH = re.compile(r"(?:^|/)(?:gui|guis|hud|huds|screen|screens|overlay|overlays|widget|widgets|icon|icons|font|fonts|book|books|advancement|advancements|interface|interfaces|jei|rei|emi|mob_effect|skill|skills)(?:/|$)", re.I)
UI_CLASS = re.compile(r"(?:gui|screen|hud|overlay|widget|tooltip|toast)", re.I)


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


def dump(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def class_header(raw):
    """Read declared superclass and UTF8 constants, without executing bytecode."""
    if raw[:4] != b"\xca\xfe\xba\xbe":
        raise ValueError("Invalid Java class magic")
    count = struct.unpack_from(">H", raw, 8)[0]
    pool, offset, index = {}, 10, 1
    sizes = {3: 4, 4: 4, 5: 8, 6: 8, 7: 2, 8: 2, 9: 4, 10: 4,
             11: 4, 12: 4, 15: 3, 16: 2, 17: 4, 18: 4, 19: 2, 20: 2}
    while index < count:
        tag = raw[offset]
        offset += 1
        if tag == 1:
            length = struct.unpack_from(">H", raw, offset)[0]
            offset += 2
            pool[index] = raw[offset:offset + length].decode("utf-8", errors="replace")
            offset += length
        elif tag == 7:
            pool[index] = struct.unpack_from(">H", raw, offset)[0]
            offset += 2
        else:
            offset += sizes[tag]
        index += 2 if tag in (5, 6) else 1
    _, this_id, super_id = struct.unpack_from(">HHH", raw, offset)
    name = pool[pool[this_id]]
    parent = pool[pool[super_id]] if super_id else None
    return name, parent, [value for value in pool.values() if isinstance(value, str)]


class ReferenceExport:
    def __init__(self, profile, output):
        self.profile, self.output = profile.resolve(), output.resolve()
        previous = self.output / "manifest.json"
        self.previous = json.loads(previous.read_text(encoding="utf-8")) if previous.is_file() else {}
        self.minecraft = self.profile.parent.parent
        state_path = self.profile / "automodpack/client/active-state.json"
        self.state = json.loads(state_path.read_text(encoding="utf-8")) if state_path.is_file() else None
        if self.state and self.state.get("status") != "ACTIVE":
            raise ValueError("Native AutoModpack installation is not ACTIVE")
        self.active = self.profile / "automodpack/client/active" if self.state else self.profile
        if not (self.active / "mods").is_dir():
            raise ValueError("Installed managed mods directory is missing")
        self.sources, self.images, self.classes, self.non_png, self.gaps = [], [], [], [], []
        self.groups = {}
        options = dict(line.split(":", 1) for line in (self.profile / "options.txt").read_text(encoding="utf-8").splitlines() if ":" in line)
        self.selected = json.loads(options.get("resourcePacks", "[]"))

    def label(self, path):
        if path.is_relative_to(self.profile):
            return "$PROFILE/" + path.relative_to(self.profile).as_posix()
        if path.is_relative_to(self.minecraft):
            return "$MINECRAFT/" + path.relative_to(self.minecraft).as_posix()
        return path.name

    def add_archive(self, path, kind, priority=None, raw=None, locator=None, depth=0):
        raw = path.read_bytes() if raw is None else raw
        locator = locator or self.label(path)
        source_hash = digest(raw)
        source = {"id": "source-" + digest(locator.encode("utf-8"))[:12], "path": locator,
                  "sha256": source_hash, "kind": kind, "priority": priority,
                  "mods": [], "licenses": [], "image_count": 0, "class_count": 0}
        self.sources.append(source)
        with zipfile.ZipFile(io.BytesIO(raw)) as archive:
            names = set(archive.namelist())
            if "META-INF/mods.toml" in names:
                metadata = tomllib.loads(archive.read("META-INF/mods.toml").decode("utf-8-sig"))
                source["licenses"] = [str(metadata.get("license", "not declared"))]
                source["mods"] = [{"id": row.get("modId"), "name": row.get("displayName", row.get("modId")),
                                   "version": str(row.get("version", "unresolved")),
                                   "url": row.get("displayURL", metadata.get("issueTrackerURL"))}
                                  for row in metadata.get("mods", [])]
            elif "fabric.mod.json" in names:
                metadata = json.loads(archive.read("fabric.mod.json"))
                source["mods"] = [{"id": metadata["id"], "name": metadata.get("name", metadata["id"]), "version": metadata.get("version")}]
                source["licenses"] = metadata.get("license", [])
            if kind.startswith("vanilla"):
                source["mods"] = [{"id": "minecraft", "name": "Minecraft", "version": "1.20.1"}]
            for mod in source["mods"]:
                self.groups.setdefault(mod["id"], {"id": mod["id"], "name": mod["name"], "versions": set()})["versions"].add(mod.get("version", "unresolved"))
            for name in sorted(names):
                if name.endswith("/"):
                    continue
                if name.endswith(".class") and kind not in ("resource_pack", "vanilla_assets"):
                    source["class_count"] += 1
                    try:
                        own, parent, strings = class_header(archive.read(name))
                        if UI_CLASS.search(name) or UI_CLASS.search(parent or "") or any("net/minecraft/client/gui/" in value for value in strings):
                            self.classes.append({"source": source["id"], "entry": name, "name": own,
                                                 "super": parent, "strings": strings})
                    except (ValueError, KeyError, IndexError, struct.error) as failure:
                        self.gaps.append({"type": "class_header", "source": source["id"], "entry": name, "reason": str(failure)})
                match = re.fullmatch(r"(?:resourcepacks/([^/]+)/)?assets/([^/]+)/(.+)", name)
                if match:
                    builtin, namespace, relative = match.groups()
                    if relative.endswith(".png"):
                        source["image_count"] += 1
                        self.images.append({"source": source["id"], "entry": name, "namespace": namespace,
                                            "resource": f"{namespace}:{relative}", "builtin": builtin,
                                            "path_match": bool(GUI_PATH.search(relative)), "raw": None,
                                            "archive": path if raw is not None and "!/" not in locator else None,
                                            "archive_bytes": raw if "!/" in locator else None})
                    elif GUI_PATH.search(relative) and relative.endswith((".json", ".mcmeta", ".ttf", ".otf", ".svg", ".gif", ".jpg", ".webp", ".fsh", ".vsh")):
                        self.non_png.append({"source": source["id"], "entry": name, "resource": f"{namespace}:{relative}"})
            nested = "META-INF/jarjar/metadata.json"
            if nested in names:
                if depth >= 8:
                    self.gaps.append({"type": "nested_depth", "source": source["id"]})
                else:
                    for item in json.loads(archive.read(nested)).get("jars", []):
                        child = item["path"]
                        self.add_archive(path, "nested_mod", raw=archive.read(child), locator=locator + "!/" + child, depth=depth + 1)

    def add_directory(self, path, kind, priority=None):
        if not path.is_dir():
            return
        files = sorted(file for file in path.rglob("*") if file.is_file())
        # Hash exact resource bytes, including metadata, without reading personal runtime files.
        fingerprint = hashlib.sha256()
        for file in files:
            fingerprint.update(file.relative_to(path).as_posix().encode("utf-8") + b"\0")
            fingerprint.update(hashlib.sha256(file.read_bytes()).digest())
        source = {"id": "source-" + digest(self.label(path).encode("utf-8"))[:12], "path": self.label(path), "sha256": fingerprint.hexdigest(),
                  "kind": kind, "priority": priority, "mods": [], "licenses": [], "image_count": 0, "class_count": 0}
        self.sources.append(source)
        for file in files:
            relative = file.relative_to(path).as_posix()
            match = re.fullmatch(r"assets/([^/]+)/(.+)", relative)
            if match and file.suffix.lower() == ".png":
                namespace, resource = match.groups()
                source["image_count"] += 1
                self.images.append({"source": source["id"], "entry": relative, "namespace": namespace,
                                    "resource": f"{namespace}:{resource}", "builtin": None,
                                    "path_match": bool(GUI_PATH.search(resource)), "raw": file.read_bytes()})
            elif kind == "fancymenu_assets" and file.suffix.lower() == ".png":
                source["image_count"] += 1
                self.images.append({"source": source["id"], "entry": relative, "namespace": "fancymenu",
                                    "resource": "fancymenu-local:" + relative, "builtin": None,
                                    "path_match": True, "raw": file.read_bytes()})
            elif (match and GUI_PATH.search(relative)) or kind == "fancymenu_assets":
                self.non_png.append({"source": source["id"], "entry": relative, "resource": None})

    def collect(self):
        vanilla = self.profile / (self.profile.name + ".jar")
        if not vanilla.is_file():
            raise ValueError("Profile vanilla client archive is missing")
        self.add_archive(vanilla, "vanilla_assets", 0)
        for folder in sorted({self.profile / "mods", self.active / "mods"}):
            for jar in sorted(folder.glob("*.jar")):
                self.add_archive(jar, "installed_mod", 1)
        # Forge's mapped client supplies readable vanilla GUI class names.
        mapped = self.minecraft / "libraries/net/minecraft/client/1.20.1-20230612.114412/client-1.20.1-20230612.114412-srg.jar"
        if mapped.is_file():
            self.add_archive(mapped, "vanilla_classes")
        else:
            self.gaps.append({"type": "vanilla_class_names", "reason": "Mapped 1.20.1 client archive unavailable"})
        self.add_directory(self.active / "kubejs", "kubejs_assets", 2)
        self.add_directory(self.profile / "config/fancymenu/assets", "fancymenu_assets")
        if self.active != self.profile:
            self.add_directory(self.active / "config/fancymenu/assets", "fancymenu_assets")
        for index, item in enumerate(self.selected):
            if not item.startswith("file/"):
                continue
            name = item[5:]
            candidates = [folder / part for folder in (self.active / "resourcepacks", self.profile / "resourcepacks")
                          for part in (name, unquote(name))]
            path = next((candidate for candidate in candidates if candidate.exists()), None)
            if path is None:
                self.gaps.append({"type": "missing_selected_pack", "pack": item})
            elif path.is_dir():
                self.add_directory(path, "resource_pack", index + 10)
            else:
                self.add_archive(path, "resource_pack", index + 10)
        for folder in (self.active / "config/paxi/resourcepacks",):
            if folder.is_dir():
                for path in sorted(folder.iterdir()):
                    if path.is_dir():
                        self.add_directory(path, "paxi_pack_order_unresolved")
                    elif zipfile.is_zipfile(path):
                        self.add_archive(path, "paxi_pack_order_unresolved")

    def emit(self):
        source_by_id = {source["id"]: source for source in self.sources}
        references = defaultdict(set)
        class_rows = []
        for cls in self.classes:
            source = source_by_id[cls["source"]]
            namespaces = {mod["id"] for mod in source["mods"]}
            pngs = [value for value in cls.pop("strings") if value.endswith(".png") and len(value) < 512 and "\n" not in value]
            for value in pngs:
                references[value].add(cls["name"])
                if ":" not in value:
                    for namespace in namespaces:
                        references[f"{namespace}:{value}"].add(cls["name"])
            cls["png_constants"] = sorted(set(pngs))
            cls["status"] = "static_ui_class_candidate"
            cls["group"] = source["mods"][0]["id"] if len(source["mods"]) == 1 else "shared-libraries"
            cls["group"] = "vanilla" if cls["group"] == "minecraft" else cls["group"]
            class_rows.append(cls)
        records = []
        archives = {}
        try:
            for image in self.images:
                users = sorted(references.get(image["resource"], set()))
                if not image["path_match"] and not users:
                    continue
                source = source_by_id[image["source"]]
                group = "vanilla" if image["namespace"] == "minecraft" else image["namespace"]
                # Preserve legacy resource namespaces; runtime module ownership is separate.
                raw = image["raw"]
                if raw is None:
                    if source["id"] not in archives:
                        archives[source["id"]] = zipfile.ZipFile(io.BytesIO(image["archive_bytes"]) if image["archive_bytes"] else image["archive"])
                    raw = archives[source["id"]].read(image["entry"])
                if raw[:8] != b"\x89PNG\r\n\x1a\n":
                    self.gaps.append({"type": "invalid_png", "source": source["id"], "entry": image["entry"]})
                    continue
                width, height = struct.unpack_from(">II", raw, 16)
                safe = PurePosixPath(image["entry"])
                if safe.is_absolute() or ".." in safe.parts or any(":" in part for part in safe.parts):
                    raise ValueError("Unsafe archive entry: " + image["entry"])
                target = Path(group) / "png" / source["id"] / image["entry"]
                destination = self.output / target
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_bytes(raw)
                record = {key: image[key] for key in ("source", "entry", "resource", "namespace", "builtin")}
                record.update({"id": f"png-{len(records):05d}", "group": group, "png": target.as_posix(),
                               "width": width, "height": height, "sha256": digest(raw), "bytes": len(raw),
                               "selection_evidence": "ui_class_png_constant" if users else "gui_resource_path",
                               "referencing_ui_classes": users, "role": "original_reference",
                               "resource_pack_target": "assets/" + image["namespace"] + "/" + image["resource"].split(":", 1)[1] if ":" in image["resource"] and not image["resource"].startswith("fancymenu-local:") else None})
                map_icon = re.search(r"entity/icon/(?:sprite/)?([^/]+)/", image["resource"])
                record["category"] = "map_entity_icons" if map_icon else "fonts" if "/font/" in image["resource"] or "/fonts/" in image["resource"] else "interface_assets"
                if record["category"] == "interface_assets" and not image["path_match"]:
                    record["category"] = "render_dependencies"
                if map_icon:
                    record["map_icon_target_namespace"] = map_icon.group(1)
                    record["map_icon_target_mod_metadata_observed"] = map_icon.group(1) in self.groups
                companion = image["entry"] + ".mcmeta"
                archive = archives.get(source["id"])
                if archive is not None and companion in archive.namelist():
                    metadata = archive.read(companion)
                    (self.output / (target.as_posix() + ".mcmeta")).write_bytes(metadata)
                    record["png_mcmeta"] = target.as_posix() + ".mcmeta"
                    record["png_mcmeta_sha256"] = digest(metadata)
                records.append(record)
        finally:
            for archive in archives.values():
                archive.close()
        variants = defaultdict(list)
        for record in records:
            variants[record["resource"]].append(record)
        for rows in variants.values():
            known = [row for row in rows if source_by_id[row["source"]]["priority"] is not None and not row["builtin"]]
            high = max((source_by_id[row["source"]]["priority"] for row in known), default=None)
            winners = [row for row in known if source_by_id[row["source"]]["priority"] == high]
            for row in rows:
                row["resolution"] = "selected_stack_candidate" if row in winners else "source_variant"
                if len(winners) > 1 or any(source_by_id[other["source"]]["priority"] is None or other["builtin"] for other in rows):
                    row["resolution"] = "precedence_unresolved"
        counts = {"sources": len(self.sources), "installed_top_level_mod_jars": sum(source["kind"] == "installed_mod" for source in self.sources),
                  "all_image_resources_considered": len(self.images), "gui_png_variants": len(records),
                  "unique_gui_resource_paths": len(variants), "ui_class_candidates": len(class_rows),
                  "non_png_gui_resources": len(self.non_png), "gaps": len(self.gaps)}
        manifest = {"schema_version": 1, "scope": "read_only_installed_gui_reference_attachments",
                    "profile": self.profile.name, "minecraft": "1.20.1", "native_installation": {key: self.state.get(key) for key in ("modpackId", "contentToken", "status")} if self.state else None,
                    "source_root": "$PROFILE/automodpack/client/active" if self.state else "$PROFILE",
                    "selected_resource_packs": self.selected, "counts": counts, "sources": self.sources,
                    "coverage": {"status": "STATIC_REFERENCE_INVENTORY_ONLY", "canonical_atlas_gui_adapter": "UNSUPPORTED",
                                 "runtime_screens": "UNOBSERVED", "individual_sheet_uv_regions": "UNRESOLVED",
                                 "class_selection": "GUI/screen/HUD/overlay/widget/tooltip/toast name, superclass or net/minecraft/client/gui constant-pool reference; includes helpers and mixins",
                                 "png_selection": "GUI resource-path tokens plus exact PNG constants in selected UI classes",
                                 "limitations": ["Names and constants do not prove reachable screens or active controls.",
                                                 "Procedural controls, dynamic images, rendered items/entities, font glyph composition and shader output have no standalone PNG proven here.",
                                                 "Native sheets are preserved whole; no guessed control crops or UV rectangles.",
                                                 "Builtin, generated, server-pack and Paxi precedence require native runtime evidence; selected-stack candidates are not asserted runtime winners.",
                                                 "Original references are local-only attachments; the export grants no third-party redistribution rights."]},
                    "gaps": self.gaps, "records": records}
        dump(self.output / "manifest.json", manifest)
        dump(self.output / "ui-classes.json", class_rows)
        dump(self.output / "non-png-resources.json", self.non_png)
        by_group = defaultdict(list)
        by_class = defaultdict(list)
        for row in records:
            by_group[row["group"]].append(row)
        for cls in class_rows:
            by_class[cls["group"]].append(cls)
        groups = sorted(set(by_group) | set(by_class) | {"vanilla" if key == "minecraft" else key for key in self.groups})
        lines = ["# Installed GUI reference catalog", "", "Requested layout: `GUI/mod-name (or vanilla) -> resource link + PNG`.", "",
                 f"Source: `{self.profile.name}`, native AutoModpack token `{manifest['native_installation']['contentToken'] if self.state else 'unmanaged'}`.", "",
                 f"{counts['gui_png_variants']:,} PNG variants / {counts['unique_gui_resource_paths']:,} resource paths / {len(groups)} groups / {len(class_rows):,} UI class candidates.", "",
                 "Open [the searchable gallery](index.html), or choose a group below. Each group contains an original-file link and inline PNG for every image. PNGs are extracted locally with original bytes and alpha, and excluded from Git. Regenerate attachments before using a fresh checkout.", "",
                 "This is a static reference inventory, not a complete runtime screen/control census. `ui-classes.json` retains code-drawn/candidate interfaces; `non-png-resources.json` retains other GUI resources. Sprite sheets remain whole because exact control UVs have not been established. No custom art or resource pack is installed.", "",
                 "The gallery distinguishes selected-stack candidates from unresolved precedence. CoffeeGUI and other selected packs are preserved alongside native assets. Class-name candidates can be helpers or mixins, and packaged assets may be unreachable or disabled. Active builtin/generated/server resource order remains unobserved.", "",
                 "Xaero's entity icon support pack includes icons for other packs' mods. These are cataloged as packaged references, not as installed mobs. The gallery initially shows interface assets; choose map entity icons or all categories to include the large icon library. Font sheets are separate too.", "",
                 "Recreate: `python Catalog/generator/export_gui_references.py --profile <test-jem-profile>`.", "",
                 "[Machine-readable manifest](manifest.json) | [UI class candidates](ui-classes.json) | [Other GUI resources](non-png-resources.json) | [Coverage and limits](COVERAGE.md)", "",
                 "| GUI owner / namespace | PNG variants | UI class candidates |", "| --- | ---: | ---: |"]
        for group in groups:
            rows = sorted(by_group[group], key=lambda row: (row["resource"], row["source"]))
            folder = self.output / group
            folder.mkdir(parents=True, exist_ok=True)
            group_lines = [f"# GUI/{group}", "", "[All GUI groups](../README.md) | [Gallery](../index.html)", "",
                           "Original reference assets. Control sheets retain their source dimensions and alpha. Resource namespace and containing mod are recorded separately; a path is not evidence of a live screen.", ""]
            if not rows:
                group_lines.extend(["No separate GUI PNG was selected for this group. This does not prove the mod has no interface: it may use shared controls, procedural rendering, dynamic resources or an unclassified asset path.", ""])
            for row in rows:
                source = source_by_id[row["source"]]
                png = quote(str(PurePosixPath(row["png"]).relative_to(group)), safe="/")
                group_lines.extend([f"## {row['resource']}", "", f"[PNG]({png}) - {row['width']} x {row['height']} - `{row['resolution']}`", "",
                                    f"Source: `{source['path']}!/{row['entry']}`", "",
                                    f"Override target: `{row['resource_pack_target'] or 'local FancyMenu asset; inspect owning layout'}`", "",
                                    f"![{row['resource']}]({png})", ""])
                if row.get("png_mcmeta"):
                    group_lines.extend([f"[Original animation/texture metadata]({quote(str(PurePosixPath(row['png_mcmeta']).relative_to(group)), safe='/')})", ""])
            if by_class[group]:
                group_lines.extend(["## UI class candidates", "", "Static names and declared superclasses only. Helpers/mixins can occur here. Shared vanilla controls, procedural drawing, item/entity renders and runtime state do not necessarily have their own PNG.", ""])
                for cls in by_class[group]:
                    group_lines.append(f"- `{cls['name']}` extends `{cls['super']}`; `{source_by_id[cls['source']]['path']}!/{cls['entry']}`")
            (folder / "README.md").write_text("\n".join(group_lines) + "\n", encoding="utf-8")
            lines.append(f"| [{group}]({quote(group, safe='')}/README.md) | {len(rows)} | {len(by_class[group])} |")
        (self.output / "README.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
        self.gallery(records, source_by_id, groups, counts)
        # Remove only attachments explicitly owned by the preceding receipt.
        # Never recursively clean user files or a caller-supplied output folder.
        keep = {row[key] for row in records for key in ("png", "png_mcmeta") if key in row}
        for row in self.previous.get("records", []):
            for key in ("png", "png_mcmeta"):
                relative = row.get(key)
                if relative and relative not in keep:
                    old = (self.output / relative).resolve()
                    if not old.is_relative_to(self.output):
                        raise ValueError("Prior receipt contains an unsafe attachment path")
                    old.unlink(missing_ok=True)
        return counts

    def gallery(self, records, sources, groups, counts):
        cards = []
        ordered = sorted(records, key=lambda row: (row["group"], row["resource"], row["source"]))
        for row in ordered:
            source = sources[row["source"]]
            png = quote(row["png"], safe="/")
            esc = html.escape
            search = esc((row["group"] + " " + row["resource"] + " " + source["path"]).lower(), quote=True)
            cards.append(f'<article data-search="{search}" data-group="{esc(row["group"], quote=True)}" data-category="{row["category"]}"><h2>{esc(row["group"])}</h2><code>{esc(row["resource"])}</code><p>{row["width"]} x {row["height"]} | {esc(row["resolution"])}</p><a href="{png}" target="_blank"><img loading="lazy" src="{png}" alt="{esc(row["resource"], quote=True)}"></a><p><a href="{png}" download>Original PNG</a> · <a href="{quote(row["group"])}/README.md">Group/source details</a></p><small>{esc(source["path"])}</small><p><label><input type="checkbox" data-id="{row["id"]}"> Select for later resource-pack planning</label></p></article>')
        data = [{**{key: row[key] for key in ("id", "resource", "png", "sha256", "source", "resource_pack_target", "category", "group")}, "source_path": sources[row["source"]]["path"]} for row in ordered]
        document = '''<!doctype html><html lang="en"><meta charset="utf-8"><title>test jem GUI reference catalog</title>
<style>body{margin:24px;font:15px system-ui;background:#f4f4f2;color:#222}header{position:sticky;top:0;background:#f4f4f2;padding:12px 0;border-bottom:1px solid #aaa;z-index:1}h1{margin:0 0 10px}input[type=search]{width:40%;padding:8px}select,button{padding:8px}main{display:grid;grid-template-columns:repeat(auto-fill,minmax(320px,1fr));gap:16px;margin-top:20px}article{background:white;padding:16px;border:1px solid #bbb;min-width:0}article[hidden]{display:none}h2{font-size:16px;margin:0 0 8px}code,small{overflow-wrap:anywhere}img{display:block;max-width:100%;max-height:360px;object-fit:contain;image-rendering:pixelated;background:repeating-conic-gradient(#dedede 0 25%,#f7f7f7 0 50%) 0/16px 16px;margin:12px 0}a{color:#164d80}small{color:#555}</style>
<header><h1>GUI / test jem</h1><p>COUNTS. Original references; no custom art installed. Whole sheets and original alpha retained.</p><p>Static asset/class inventory. Code-drawn controls and sheet regions remain unresolved; selected-stack candidates are not proven runtime winners.</p><input type="search" id="search" placeholder="Find a mod, control name or texture path"><select id="group"><option value="">All groups</option>OPTIONS</select><select id="category"><option value="interface_assets">Interfaces and controls</option><option value="map_entity_icons">Map entity icons (including other packs' support)</option><option value="fonts">Font sheets</option><option value="render_dependencies">Other textures referenced by UI classes (candidates)</option><option value="">All categories</option></select> <button id="download">Export selected references</button> <span id="count"></span></header><main></main><p><button id="more">Show next 60</button></p>
<script>const rows=DATA;const cards=CARDDATA;const selected=new Set();const q=document.querySelector('#search'),g=document.querySelector('#group'),category=document.querySelector('#category'),count=document.querySelector('#count'),main=document.querySelector('main'),more=document.querySelector('#more');let matches=[],shown=0;function status(){count.textContent=matches.length+' matches / '+shown+' shown / '+selected.size+' selected';more.hidden=shown>=matches.length}function append(){const next=matches.slice(shown,shown+60);main.insertAdjacentHTML('beforeend',next.map(i=>cards[i]).join(''));shown+=next.length;for(const input of main.querySelectorAll('input[data-id]'))input.checked=selected.has(input.dataset.id);status()}function filter(){matches=[];for(let i=0;i<rows.length;i++){const r=rows[i];if((!g.value||g.value===r.group)&&(!category.value||category.value===r.category)&&(r.group+' '+r.resource+' '+r.source_path).toLowerCase().includes(q.value.toLowerCase()))matches.push(i)}shown=0;main.replaceChildren();append()}more.onclick=append;q.oninput=filter;g.onchange=filter;category.onchange=filter;document.addEventListener('change',e=>{if(e.target.dataset.id){e.target.checked?selected.add(e.target.dataset.id):selected.delete(e.target.dataset.id);status()}});document.querySelector('#download').onclick=()=>{const blob=new Blob([JSON.stringify({purpose:'JEM resource-pack reference selection; no installation',records:rows.filter(r=>selected.has(r.id))},null,2)],{type:'application/json'});const a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download='jem-gui-selection.json';a.click();setTimeout(()=>URL.revokeObjectURL(a.href),1000)};filter();</script></html>'''
        document = document.replace("COUNTS", f"{len(records):,} PNG variants, {counts['unique_gui_resource_paths']:,} paths, {len(groups)} groups")
        document = document.replace("OPTIONS", "".join(f'<option>{html.escape(group)}</option>' for group in groups))
        document = document.replace("CARDDATA", json.dumps(cards, ensure_ascii=False).replace("<", "\\u003c"))
        document = document.replace("rows=DATA", "rows=" + json.dumps(data, ensure_ascii=False).replace("<", "\\u003c"))
        (self.output / "index.html").write_text(document, encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", required=True, type=Path)
    parser.add_argument("--output", type=Path, default=ROOT / "Catalog/GUI")
    args = parser.parse_args()
    export = ReferenceExport(args.profile, args.output)
    export.collect()
    print(json.dumps(export.emit(), indent=2))


if __name__ == "__main__":
    main()
