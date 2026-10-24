"""Package native Blockbench Java codec exports without authoring geometry or pixels.

Each source element is partitioned, never copied into every multiblock cell.
Unrotated elements are clipped at cell boundaries with interpolated native UVs.
Rotated elements are partitioned only along their unchanged rotation axis.
"""
import argparse
import copy
import itertools
import json
import math
from pathlib import Path

AXES = "xyz"
FACES = {
    "down": (0, 1, 2, -1), "up": (0, 1, 2, 1),
    "north": (0, -1, 1, -1), "south": (0, 1, 1, -1),
    "west": (2, 1, 1, -1), "east": (2, -1, 1, -1),
}


def cuts(lo, hi):
    return [lo] + [v for v in range(16, math.ceil(hi), 16) if lo < v < hi] + [hi]


def crop_face(face, direction, original, clipped):
    result = copy.deepcopy(face)
    if "uv" not in face:
        raise ValueError("Native codec export must contain explicit face UVs")
    u_axis, u_sign, v_axis, v_sign = FACES[direction]
    intervals = []
    for axis, sign in ((u_axis, u_sign), (v_axis, v_sign)):
        extent = original[1][axis] - original[0][axis]
        low = (clipped[0][axis] - original[0][axis]) / extent if extent else 0
        high = (clipped[1][axis] - original[0][axis]) / extent if extent else 1
        intervals.append((low, high) if sign > 0 else (1 - high, 1 - low))
    (s0, s1), (t0, t1) = intervals
    rotation = face.get("rotation", 0)
    if rotation == 0:
        u0, u1, v0, v1 = s0, s1, t0, t1
    elif rotation == 90:
        u0, u1, v0, v1 = t0, t1, 1 - s1, 1 - s0
    elif rotation == 180:
        u0, u1, v0, v1 = 1 - s1, 1 - s0, 1 - t1, 1 - t0
    elif rotation == 270:
        u0, u1, v0, v1 = 1 - t1, 1 - t0, s0, s1
    else:
        raise ValueError(f"Unsupported native face rotation {rotation}")
    a, b, c, d = face["uv"]
    result["uv"] = [a + u0 * (c - a), b + v0 * (d - b),
                    a + u1 * (c - a), b + v1 * (d - b)]
    result.pop("cullface", None)
    return result


def pieces(element):
    original = (element["from"], element["to"])
    rotation = element.get("rotation")
    axis = AXES.index(rotation["axis"]) if rotation else None
    if rotation and rotation["angle"] not in (-45, -22.5, 0, 22.5, 45):
        raise ValueError(f"Illegal Java element rotation: {rotation}")
    intervals = []
    for dimension in range(3):
        values = cuts(original[0][dimension], original[1][dimension]) if axis in (None, dimension) else [original[0][dimension], original[1][dimension]]
        intervals.append(list(zip(values, values[1:])))
    for ranges in itertools.product(*intervals):
        clipped = ([r[0] for r in ranges], [r[1] for r in ranges])
        part = copy.deepcopy(element)
        part["from"], part["to"] = clipped
        part["faces"] = {}
        for direction, face in element["faces"].items():
            normal_axis = {"west": 0, "east": 0, "down": 1, "up": 1, "north": 2, "south": 2}[direction]
            edge = 0 if direction in ("west", "down", "north") else 1
            if clipped[edge][normal_axis] == original[edge][normal_axis]:
                part["faces"][direction] = crop_face(face, direction, original, clipped)
        if part["faces"]:
            yield part


def clip_polygon(polygon, axis, boundary, keep_greater):
    result = []
    for start, end in zip(polygon, polygon[1:] + polygon[:1]):
        inside_start = (start[axis] >= boundary) if keep_greater else (start[axis] <= boundary)
        inside_end = (end[axis] >= boundary) if keep_greater else (end[axis] <= boundary)
        if inside_start:
            result.append(start)
        if inside_start != inside_end:
            fraction = (boundary - start[axis]) / (end[axis] - start[axis])
            result.append([start[i] + fraction * (end[i] - start[i]) for i in range(2)])
    return result


def collision_boxes(element):
    if element.get("name", "").startswith("nocollision:"):
        return []
    rotation = element.get("rotation")
    if not rotation or rotation["angle"] == 0:
        return [element["from"] + element["to"]]
    fixed = AXES.index(rotation["axis"])
    # Coordinate order follows the native right-handed positive rotation.
    a, b = ((1, 2), (2, 0), (0, 1))[fixed]
    angle = math.radians(rotation["angle"])
    cosine, sine = math.cos(angle), math.sin(angle)
    origin = rotation["origin"]
    scale = 1 / cosine if rotation.get("rescale", False) else 1
    polygon = []
    for low_a, low_b in ((True, True), (False, True), (False, False), (True, False)):
        x = element["from" if low_a else "to"][a] - origin[a]
        y = element["from" if low_b else "to"][b] - origin[b]
        polygon.append([origin[a] + scale * (x * cosine - y * sine),
                        origin[b] + scale * (x * sine + y * cosine)])
    result = []
    for start in range(math.floor(min(p[0] for p in polygon)), math.ceil(max(p[0] for p in polygon)), 2):
        strip = clip_polygon(clip_polygon(polygon, 0, start, True), 0, start + 2, False)
        if not strip:
            continue
        lower, upper = list(element["from"]), list(element["to"])
        lower[a], upper[a] = min(p[0] for p in strip), max(p[0] for p in strip)
        lower[b], upper[b] = min(p[1] for p in strip), max(p[1] for p in strip)
        if upper[a] > lower[a] and upper[b] > lower[b]:
            result.append(lower + upper)
    return result


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def gable_boxes(elements):
    result = []
    for side in ("front", "back"):
        panels = [element for element in elements if element.get("name", "").startswith("nocollision:" + side)
                  and element.get("name", "").endswith("_gable")]
        if len(panels) != 2:
            raise ValueError(f"Expected paired exterior/interior {side} gable panels")
        outer = panels[0]
        left, base = outer["from"][:2]
        right, peak = outer["to"][:2]
        centre = (left + right) / 2
        z0, z1 = sorted(panel["from"][2] for panel in panels)
        for y0 in range(math.floor(base), math.ceil(peak), 2):
            lower = max(base, y0)
            upper = min(peak, y0 + 2)
            half_width = (right - left) / 2 * (peak - lower) / (peak - base)
            result.append([centre - half_width, lower, z0, centre + half_width, upper, z1])
    return result


def package(source, resources, size, name, texture):
    model = json.loads(source.read_text(encoding="utf-8-sig"))
    models = [[] for _ in range(size ** 3)]
    collisions = gable_boxes(model["elements"])
    for element in model["elements"]:
        collisions.extend(collision_boxes(element))
        for part in pieces(element):
            candidates = []
            for y, z, x in itertools.product(range(size), repeat=3):
                offset = [16 * x, 16 * y, 16 * z]
                if all(-16 <= part[edge][i] - offset[i] <= 32 for edge in ("from", "to") for i in range(3)):
                    distance = sum(((part["from"][i] + part["to"][i]) / 2 - offset[i] - 8) ** 2 for i in range(3))
                    candidates.append((distance, x + size * z + size * size * y, offset))
            if not candidates:
                raise ValueError(f"Element cannot fit a legal occupied cell: {element.get('name', element)}")
            _, cell, offset = min(candidates)
            for key in ("from", "to"):
                part[key] = [part[key][i] - offset[i] for i in range(3)]
            if "rotation" in part:
                part["rotation"]["origin"] = [part["rotation"]["origin"][i] - offset[i] for i in range(3)]
            part.pop("name", None)
            for face in part["faces"].values():
                face["texture"] = "#material"
            models[cell].append(part)
    assets = resources / "assets" / "mobhome"
    shared = {"ambientocclusion": False, "render_type": "minecraft:cutout", "textures": {"material": texture, "particle": texture}}
    for cell, elements in enumerate(models):
        write_json(assets / "models" / "block" / f"{name}_{cell}.json", {**shared, "elements": elements})
    if size ** 3 < 27:
        write_json(assets / "models" / "block" / f"{name}_empty.json", {**shared, "elements": []})
    variants = {}
    for facing, rotation in (("south", 0), ("west", 90), ("north", 180), ("east", 270)):
        for cell in range(27):
            suffix = str(cell) if cell < size ** 3 else "empty"
            variants[f"cell={cell},facing={facing}"] = {"model": f"mobhome:block/{name}_{suffix}", "y": rotation}
    write_json(assets / "blockstates" / f"{name}.json", {"variants": variants})
    item = {**shared, "elements": model["elements"], "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, -2, 0], "scale": [0.25, 0.25, 0.25]},
        "ground": {"translation": [0, 2, 0], "scale": [0.15, 0.15, 0.15]},
        "fixed": {"rotation": [0, 180, 0], "scale": [0.25, 0.25, 0.25]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "scale": [0.15, 0.15, 0.15]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.2, 0.2, 0.2]}}}
    # Normalize item geometry uniformly, including pivots, without changing world models or UVs.
    # A rotated roof's unrotated authoring coordinates can exceed the rendered house bounds.
    centre = size * 8
    scale = 2 / size
    for element in item["elements"]:
        for key in ("from", "to"):
            element[key] = [8 + (value - centre) * scale for value in element[key]]
        if "rotation" in element:
            element["rotation"]["origin"] = [8 + (value - centre) * scale for value in element["rotation"]["origin"]]
        for face in element["faces"].values():
            face["texture"] = "#material"
            face.pop("cullface", None)
        if any(not -16 <= element[key][i] <= 32 for key in ("from", "to") for i in range(3)):
            raise ValueError("Centered whole-house item exceeds Java element bounds")
    write_json(assets / "models" / "item" / f"{name}.json", item)
    return collisions


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--small", required=True, type=Path)
    parser.add_argument("--large", required=True, type=Path)
    parser.add_argument("--resources", type=Path, default=Path(__file__).resolve().parents[1] / "src/main/resources")
    args = parser.parse_args()
    geometry = {
        "2": package(args.small, args.resources, 2, "small_animal_home", "mobhome:block/small_materials"),
        "3": package(args.large, args.resources, 3, "large_animal_home", "mobhome:block/large_materials"),
    }
    write_json(args.resources / "data/mobhome/shelter_geometry.json", geometry)


if __name__ == "__main__":
    main()
