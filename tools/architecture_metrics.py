import argparse
import collections
import hashlib
import json
import re
import struct
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
WINDOW = 12
JAVA_LEXEMES = re.compile(r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/')


def source_lines(source):
    if isinstance(source, Path): source = source.read_text(encoding="utf-8-sig")
    source = JAVA_LEXEMES.sub(
        lambda match: "\n" * match.group().count("\n")
        if match.group().startswith(("//", "/*")) else match.group(), source)
    return [line.strip() for line in source.splitlines() if line.strip()]


def source_scope(revision, profiles, historical_mode):
    if historical_mode:
        if not revision:
            raise ValueError("--historical-mode requires --revision")
        if profiles:
            raise ValueError("Historical mode has fixed legacy roots and accepts no --profile")
        return None
    if revision:
        result = subprocess.run(["git", "show", f"{revision}:tools/module-inventory.json"],
                                cwd=ROOT, capture_output=True, check=False)
        if result.returncode:
            raise ValueError("Revision has no module inventory; use --historical-mode for the explicitly legacy scope")
        inventory = json.loads(result.stdout)
    else:
        inventory = json.loads((ROOT / "tools/module-inventory.json").read_text(encoding="utf-8"))
    roots = []
    recognized = set()
    for module in inventory["modules"]:
        if not module["java"] or module["status"] not in ("production", "development"):
            continue
        directories = ["src/main/java", "src/main/resources", *module.get("additionalSourceRoots", [])]
        profile = module.get("sourceProfile")
        if profile:
            key = profile["property"]
            recognized.add(key)
            selected = profiles.get(key, profile["default"])
            if selected not in profile["choices"]:
                raise ValueError(f"Unsupported {key}={selected}; expected {profile['choices']}")
            directories.extend((f"src/{selected}/java", f"src/{selected}/resources"))
        roots.extend(f"Mods/{module['name']}/{directory}/" for directory in directories)
    unknown = profiles.keys() - recognized
    if unknown:
        raise ValueError(f"Unknown source profile properties: {sorted(unknown)}")
    return sorted(set(roots))


def content(revision, roots):
    if revision:
        paths = subprocess.check_output(["git", "ls-tree", "-r", "--name-only", revision, "--", "Mods"], cwd=ROOT).decode().splitlines()
        if roots is None:
            paths = [path for path in paths if ("/src/main/java/" in path and path.endswith(".java"))
                     or "/src/main/resources/" in path or "/src/generated/" in path]
        else:
            paths = [path for path in paths if any(path.startswith(root) for root in roots)]
        process = subprocess.run(["git", "cat-file", "--batch"], input=("".join(revision + ":" + path + "\n" for path in paths)).encode(), capture_output=True, check=True, cwd=ROOT)
        position = 0
        for path in paths:
            newline = process.stdout.index(b"\n", position)
            size = int(process.stdout[position:newline].split()[-1])
            position = newline + 1
            yield path, process.stdout[position:position + size]
            position += size + 1
    else:
        for directory in roots:
            for path in sorted((ROOT / directory).rglob("*")):
                if path.is_file():
                    yield path.relative_to(ROOT).as_posix(), path.read_bytes()


def ast_metrics(sources):
    payload = bytearray(struct.pack(">i", len(sources)))
    for path, source in sources.items():
        for value in (path, source):
            encoded = value.encode("utf-8")
            payload.extend(struct.pack(">i", len(encoded)))
            payload.extend(encoded)
    output = subprocess.run(["java", str(ROOT / "tools/JavaSourceMetrics.java")], input=payload, capture_output=True, check=True)
    counts = collections.Counter()
    methods = []
    for line in output.stdout.decode().splitlines():
        category, path, name, start, end = line.split("\t")
        counts[category] += 1
        if category == "method" and int(end) >= 0:
            snippet = sources[path].encode("utf-16-le")[int(start) * 2:int(end) * 2].decode("utf-16-le")
            methods.append({"path": path, "method": name, "lines": len(source_lines(snippet))})
    return {"authoredTypes": dict(counts), "largestMethods": sorted(methods, key=lambda value: value["lines"], reverse=True)[:20]}


def measure(revision=None, profiles=None, historical_mode=False):
    profiles = profiles or {}
    roots = source_scope(revision, profiles, historical_mode)
    modules = {}
    windows = collections.defaultdict(list)
    resources = collections.defaultdict(dict)
    classes = []
    packages = {}
    sources = {}
    for relative, data in content(revision, roots):
        name = Path(relative).parts[1]
        counts = modules.setdefault(name, {"javaFiles": 0, "handwrittenJavaLines": 0, "generatedJavaLines": 0, "resources": 0, "resourceBytes": 0})
        if relative.endswith(".java"):
            source = data.decode("utf-8-sig")
            lines = source_lines(source)
            if "/src/generated/" in relative:
                counts["generatedJavaLines"] += len(lines)
                continue
            counts["javaFiles"] += 1
            counts["handwrittenJavaLines"] += len(lines)
            classes.append({"path": relative, "lines": len(lines)})
            sources[relative] = source
            package = re.search(r"^package\s+([\w.]+);", source, re.M)
            if package: packages[package.group(1) + "." + Path(relative).stem] = name
            normalized = [re.sub(r"\s+", " ", line) for line in lines]
            for offset in range(max(0, len(normalized) - WINDOW + 1)):
                block = tuple(normalized[offset:offset + WINDOW])
                if sum(len(line) for line in block) >= 240: windows[block].append((relative, offset))
        else:
            counts["resources"] += 1
            pointer = re.fullmatch(rb"version https://git-lfs.github.com/spec/v1\noid sha256:([a-f0-9]+)\nsize ([0-9]+)\n?", data)
            counts["resourceBytes"] += int(pointer.group(2)) if pointer else len(data)
            path = relative.split("/resources/", 1)[-1]
            resources[path][name] = pointer.group(1).decode() if pointer else hashlib.sha256(data).hexdigest()
    coverage = collections.defaultdict(set)
    duplicated = 0
    for matches in windows.values():
        if len(matches) < 2:
            continue
        duplicated += 1
        for path, offset in matches:
            coverage[path].update(range(offset, offset + WINDOW))
    edges = collections.defaultdict(set)
    for path, source in sources.items():
        owner = Path(path).parts[1]
        references = re.findall(r"\b(?:com|net|org)\.[\w.]+", source)
        for reference in references:
            while reference and reference not in packages:
                reference = reference.rpartition(".")[0]
            dependency = packages.get(reference)
            if dependency and dependency != owner:
                edges[owner].add(dependency)
    return {
        "method": "Nonblank Java lines after lexical comment removal; exact 12-line whitespace-normalized windows of >=240 characters; duplicated coverage includes boilerplate and is not semantic duplication.",
        **ast_metrics(sources),
        "revision": revision or "working-tree",
        "sourceScope": "legacy-all-modules-main-and-generated" if historical_mode else "module-inventory-active-profiles",
        "sourceRoots": roots,
        "profileOverrides": profiles,
        "modules": modules,
        "handwrittenJavaLines": sum(item["handwrittenJavaLines"] for item in modules.values()),
        "generatedJavaLines": sum(item["generatedJavaLines"] for item in modules.values()),
        "duplicatedWindows": duplicated,
        "duplicatedLineCoverage": sum(len(lines) for lines in coverage.values()),
        "duplicateCoverageByModule": {name: sum(len(lines) for path, lines in coverage.items() if Path(path).parts[1] == name) for name in modules},
        "largestClasses": sorted(classes, key=lambda item: item["lines"], reverse=True)[:20],
        "sourceDependencies": {name: sorted(dependencies) for name, dependencies in edges.items()},
        "overlaps": {path: owners for path, owners in sorted(resources.items()) if len(owners) > 1},
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("output", type=Path)
    parser.add_argument("--revision")
    parser.add_argument("--profile", action="append", default=[], metavar="PROPERTY=VALUE",
                        help="Match an explicitly selected Gradle source profile; defaults come from the inventory")
    parser.add_argument("--historical-mode", action="store_true",
                        help="Use legacy all-module main/generated scope for pre-inventory revisions; clearly labeled")
    args = parser.parse_args()
    profiles = {}
    for choice in args.profile:
        key, separator, value = choice.partition("=")
        if not separator or not key or not value or key in profiles:
            parser.error("Each --profile must be a unique PROPERTY=VALUE")
        profiles[key] = value
    try:
        metrics = measure(args.revision, profiles, args.historical_mode)
    except ValueError as invalid:
        parser.error(str(invalid))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(metrics, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: metrics[key] for key in ("handwrittenJavaLines", "generatedJavaLines", "duplicatedWindows", "duplicatedLineCoverage", "sourceDependencies")}, indent=2))


if __name__ == "__main__":
    main()
