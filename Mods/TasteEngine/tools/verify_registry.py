#!/usr/bin/env python3
"""Validate the bundled Taste registry and export deterministic consumer ownership."""

from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path


PHASES = {
    "general": "frame", "lighting": "lighting", "shadows": "shadow", "gi": "gi", "ao": "ao",
    "sky": "sky", "clouds": "cloud", "cloudLayer1": "cloud", "cloudLayer2": "cloud",
    "cloudLayer3": "cloud", "fog": "volume", "weather": "weather", "wetness": "weather",
    "water": "water", "underwater": "water", "reflections": "reflection", "glass": "glass",
    "materials": "material", "emission": "material", "bloom": "bloom", "vegetation": "vegetation",
    "particles": "particle", "entities": "forward", "hand": "hand", "color": "color",
    "camera": "camera", "antialiasing": "temporal", "style": "style", "dimensions": "environment",
    "accessibility": "accessibility", "performance": "performance", "photo": "photo",
    "server": "environment", "lava": "lava", "portals": "portal", "interaction": "interaction",
}


def load(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--module", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--audit", type=Path)
    args = parser.parse_args()
    settings_root = args.module / "src/main/resources/assets/taste/settings"
    registry = load(settings_root / "registry.json")
    defaults = load(settings_root / "defaults.json")
    settings = registry["settings"]
    ids = [setting["id"] for setting in settings]
    categories = [category["id"] for category in registry["categories"]]
    if len(ids) != 387 or len(set(ids)) != 387:
        raise SystemExit(f"expected 387 unique settings, found {len(ids)} / {len(set(ids))}")
    if len(categories) != 36 or len(set(categories)) != 36:
        raise SystemExit(f"expected 36 unique categories, found {len(categories)} / {len(set(categories))}")
    default_values = defaults.get("values", defaults)
    if set(default_values) != set(ids):
        missing = sorted(set(ids) - set(default_values))
        extra = sorted(set(default_values) - set(ids))
        raise SystemExit(f"default coverage mismatch, missing={missing}, extra={extra}")
    unknown_categories = sorted({setting["category"] for setting in settings} - set(PHASES))
    if unknown_categories:
        raise SystemExit(f"consumer phases missing for {unknown_categories}")
    if args.audit:
        args.audit.parent.mkdir(parents=True, exist_ok=True)
        with args.audit.open("w", newline="", encoding="utf-8") as handle:
            columns = ["setting_id", "consumer_class", "consumer_method", "runtime_field_or_uniform", "stage",
                       "apply_actions", "effective_prerequisites", "implementation_status", "scene_ids"]
            writer = csv.DictWriter(handle, fieldnames=columns)
            writer.writeheader()
            for setting in settings:
                phase = PHASES[setting["category"]]
                writer.writerow({
                    "setting_id": setting["id"],
                    "consumer_class": "dev.taste.engine.render.PipelineParameters",
                    "consumer_method": "resolve",
                    "runtime_field_or_uniform": phase,
                    "stage": phase,
                    "apply_actions": "|".join(setting.get("apply", [])),
                    "effective_prerequisites": json.dumps(setting.get("requires", []), separators=(",", ":")),
                    "implementation_status": "CODE_AUTHORED_RUNTIME_NOT_RUN",
                    "scene_ids": "",
                })
    print("registry: 387 settings, 36 categories, complete defaults and consumer phases")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
