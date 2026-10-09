import argparse
import json
import zipfile
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--mods-dir", type=Path, required=True)
    parser.add_argument("--jar", type=Path)
    args = parser.parse_args()
    resources = args.root / "src/main/resources"
    document = json.loads((args.root / "src/main/advancements/campaign-guide.json").read_text(encoding="utf-8"))
    assets = set()
    displayed = set()
    for jar in args.mods_dir.glob("*.jar"):
        with zipfile.ZipFile(jar) as archive:
            assets.update(archive.namelist())
            for name in archive.namelist():
                prefix = "data/jemcompat/advancements/"
                if name.startswith(prefix) and name.endswith(".json"):
                    if "display" in json.loads(archive.read(name)):
                        displayed.add("jemcompat:" + name[len(prefix):-len(".json")])
    ids = [node["id"] for node in document["entries"]]
    assert len(ids) == len(set(ids)), "Duplicate campaign guide IDs"
    assert set(ids) == displayed, f"Coverage mismatch: missing={displayed - set(ids)}, extra={set(ids) - displayed}"
    generated = []
    for node in document["entries"]:
        assert set(node["text"]) == {"en_us", "ru_ru"}, node["id"]
        namespace, item = node["item"].split(":", 1)
        if namespace != "minecraft":
            assert f"assets/{namespace}/models/item/{item}.json" in assets, node["item"]
        category_namespace, category = node["category"].split(":", 1)
        assert (resources / f"assets/{category_namespace}/patchouli_books/just_enough_guide/en_us/categories/{category}.json").is_file(), node["category"]
        namespace, path = node["id"].split(":", 1)
        for language, text in node["text"].items():
            assert all(text.values()) and text["hint"] != text["reference"], node["id"]
            relative = f"assets/jem_advancements/patchouli_books/just_enough_guide/{language}/entries/native/{namespace}/{path}.json"
            entry = json.loads((resources / relative).read_text(encoding="utf-8"))
            assert entry["category"] == node["category"] and entry["icon"] == node["item"], relative
            assert entry["name"] == text["name"], relative
            assert entry["pages"] == [
                {"type": "patchouli:text", "title": text["name"], "text": text["hint"]},
                {"type": "patchouli:spotlight", "item": node["item"], "text": text["reference"]},
            ], relative
            generated.append(relative)
    if args.jar:
        with zipfile.ZipFile(args.jar) as archive:
            for relative in generated:
                assert archive.read(relative) == (resources / relative).read_bytes(), relative
    print(f"Validated {len(ids)} displayed advancements, {len(generated)} localized entries, categories, item model assets and generated content" + (", including packaged bytes." if args.jar else "."))


if __name__ == "__main__":
    main()
