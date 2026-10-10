import argparse
import collections
import hashlib
import io
import json
import shutil
import tomllib
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SIDES = {"JEMClaims": "server-required", "JEMAdaptiveCulling": "client-only",
         "JEMVillagerTalking": "development-only", "JEMMenuSupport": "embedded-library"}


def metadata(archive):
    manifest = archive.read("META-INF/MANIFEST.MF").decode("utf-8").replace("\r\n ", "")
    attributes = dict(line.split(": ", 1) for line in manifest.splitlines() if ": " in line)
    if "META-INF/mods.toml" in archive.namelist():
        data = tomllib.loads(archive.read("META-INF/mods.toml").decode("utf-8"))
    elif attributes.get("FMLModType") in ("LIBRARY", "GAMELIBRARY"):
        data = {"mods": [], "libraryModule": attributes["Automatic-Module-Name"]}
    else:
        raise ValueError("Missing mod or library metadata")
    for config_name in filter(None, attributes.get("MixinConfigs", "").split(",")):
        config = json.loads(archive.read(config_name))
        refmap = config.get("refmap")
        if refmap and refmap not in archive.namelist():
            raise ValueError(f"Missing refmap {refmap}")
        for section in ("mixins", "client", "server"):
            for mixin in config.get(section, []):
                path = (config["package"] + "." + mixin).replace(".", "/") + ".class"
                if path not in archive.namelist():
                    raise ValueError(f"Missing mixin class {path}")
        plugin = config.get("plugin")
        if plugin and plugin.replace(".", "/") + ".class" not in archive.namelist():
            raise ValueError(f"Missing mixin plugin {plugin}")
    for path in archive.namelist():
        if path.endswith((".json", ".mcmeta")):
            json.loads(archive.read(path))
        if path.endswith((".java", ".log")) or path.startswith(("run/", "src/")):
            raise ValueError(f"Development content in archive: {path}")
    return data


def stage(destination):
    destination.mkdir(parents=True, exist_ok=True)
    classes = collections.defaultdict(list)
    paths = collections.defaultdict(dict)
    artifacts = []
    ids = set()
    libraries = {}
    for module in sorted((ROOT / "Mods").iterdir()):
        jars = [path for path in (module / "build/libs").glob("*.jar")
                if not path.name.endswith(("-sources.jar", "-javadoc.jar", "-slim.jar"))]
        if len(jars) != 1:
            raise ValueError(f"Expected one production artifact in {module.name}, got {len(jars)}")
        path = jars[0]
        with zipfile.ZipFile(path) as archive:
            data = metadata(archive)
            if "META-INF/jarjar/metadata.json" in archive.namelist():
                for library in json.loads(archive.read("META-INF/jarjar/metadata.json"))["jars"]:
                    embedded = archive.read(library["path"])
                    key = library["identifier"]["group"] + ":" + library["identifier"]["artifact"]
                    digest = hashlib.sha256(embedded).hexdigest()
                    if key in libraries and libraries[key]["sha256"] != digest:
                        raise ValueError(f"Incompatible embedded library bytes for {key}")
                    libraries.setdefault(key, {"sha256": digest, "consumers": [], "version": library["version"]})["consumers"].append(module.name)
                    with zipfile.ZipFile(io.BytesIO(embedded)) as nested:
                        metadata(nested)
            for mod in data["mods"]:
                if mod["modId"] in ids:
                    raise ValueError(f"Duplicate mod ID {mod['modId']}")
                ids.add(mod["modId"])
            for name in archive.namelist():
                if name.endswith("/"): continue
                if name.endswith(".class"):
                    classes[name].append(module.name)
                elif name.startswith(("assets/", "data/")):
                    paths[name][module.name] = hashlib.sha256(archive.read(name)).hexdigest()
        side = SIDES.get(module.name, "client-and-server-required")
        directory = destination / ("development" if side == "development-only" else "libraries" if side == "embedded-library" else "mods")
        directory.mkdir(exist_ok=True)
        shutil.copy2(path, directory / path.name)
        artifacts.append({"module": module.name, "file": (directory / path.name).relative_to(destination).as_posix(),
                          "sha256": hashlib.sha256(path.read_bytes()).hexdigest(), "size": path.stat().st_size,
                          "side": side, "mods": data["mods"], "dependencies": data.get("dependencies", {})})
    duplicates = {name: owners for name, owners in classes.items() if len(owners) > 1}
    if duplicates:
        raise ValueError(f"Duplicate implementation classes: {duplicates}")
    result = {"minecraft": "1.20.1", "forge": "47.4.10", "java": 17, "artifacts": artifacts,
              "installation": {"matchedClientServerRequired": ["jem_server:smp_v2 protocol 3"],
                               "replace": ["jemcompat-3.7.0.jar", "jem-server-2.0.0-dev.jar", "jem_twelve_eyes-1.7.0-dev.jar", "jem-world-boss-tiers-2.5.0-dev.jar", "jem-pack-core-2.0.0-dev.jar", "jem-claims-2.0.0-dev.jar"],
                               "add": ["jem_drill_lifecycle-1.0.0.jar"],
                               "removeObsolete": ["Any older jemcompat or jem_drill_lifecycle JAR alongside its replacement"],
                               "notes": "Claims belongs to the dedicated server; Adaptive Culling only to clients; Villager Talking remains development-only. These artifacts are staged, not installed."},
              "classDuplicates": duplicates,
              "embeddedLibraries": libraries,
              "resourceOverlaps": {name: owners for name, owners in sorted(paths.items()) if len(owners) > 1}}
    (destination / "manifest.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"artifacts": len(artifacts), "classDuplicates": len(duplicates), "resourceOverlaps": len(result["resourceOverlaps"])}))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    stage(args.destination)


if __name__ == "__main__":
    main()
