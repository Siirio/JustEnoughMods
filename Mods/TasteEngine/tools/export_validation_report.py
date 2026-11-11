#!/usr/bin/env python3
"""Create explicit NOT_RUN scene and hardware reports without inventing runtime evidence."""

from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--module", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--source-scenes", type=Path, required=True)
    args = parser.parse_args()
    output = args.module / "validation"
    output.mkdir(parents=True, exist_ok=True)
    with args.source_scenes.open("r", newline="", encoding="utf-8-sig") as source:
        rows = list(csv.DictReader(source))
    scene_path = output / "scene-results.csv"
    with scene_path.open("w", newline="", encoding="utf-8") as destination:
        writer = csv.DictWriter(destination, fieldnames=["scene_id", "status", "backend", "gpu", "evidence", "reason"])
        writer.writeheader()
        for index, row in enumerate(rows, start=1):
            scene_id = row.get("scene_id") or row.get("id") or f"S{index:02d}"
            writer.writerow({"scene_id": scene_id, "status": "NOT_RUN", "backend": "", "gpu": "",
                             "evidence": "", "reason": "Minecraft runtime launch was not authorized in this task"})
    hardware = {
        "schemaVersion": 1,
        "status": "NOT_RUN",
        "tested": [],
        "required": ["NVIDIA", "AMD", "Intel"],
        "reason": "No physical GPU gameplay matrix was run",
    }
    (output / "hardware-results.json").write_text(json.dumps(hardware, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {len(rows)} NOT_RUN scene rows and an explicit hardware matrix")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
