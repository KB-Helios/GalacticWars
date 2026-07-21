"""Refine the five launch vehicle GeckoLib geometries without breaking their animation contracts.

The authored 256x256 atlases already contain high-detail material panels. Added detail cubes are
packed into dedicated, non-overlapping box-UV rectangles sized from their own dimensions so a large
detail never spills past a smaller source cube's footprint.
"""

from __future__ import annotations

import json
import math
import hashlib
import io
import os
import tempfile
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
MODEL_ROOT = ROOT / "src/main/resources/assets/galacticwars/geckolib/models/entity/vehicle"
TEXTURE_ROOT = ROOT / "src/main/resources/assets/galacticwars/textures/entity/vehicle"


def save_png(image: Image.Image, destination: Path) -> None:
    buffer = io.BytesIO()
    image.save(buffer, format="PNG", optimize=False)
    descriptor, temporary_name = tempfile.mkstemp(dir=destination.parent, suffix=".png")
    try:
        with os.fdopen(descriptor, "wb") as temporary:
            temporary.write(buffer.getvalue())
        os.replace(temporary_name, destination)
    finally:
        Path(temporary_name).unlink(missing_ok=True)


def cube(origin, size, *, rotation=None, pivot=None, inflate=None):
    value = {"origin": origin, "size": size}
    if rotation is not None:
        value["rotation"] = rotation
    if pivot is not None:
        value["pivot"] = pivot
    if inflate is not None:
        value["inflate"] = inflate
    return value


def box_uv_footprint(size: list[float]) -> tuple[int, int]:
    width, height, depth = (math.ceil(dimension) for dimension in size)
    return 2 * (width + depth), height + depth


def rectangles_overlap(
        left: tuple[int, int, int, int],
        right: tuple[int, int, int, int],
) -> bool:
    left_x, left_y, left_width, left_height = left
    right_x, right_y, right_width, right_height = right
    return not (
        left_x + left_width <= right_x
        or right_x + right_width <= left_x
        or left_y + left_height <= right_y
        or right_y + right_height <= left_y
    )


def allocate_uv(
        footprint: tuple[int, int],
        occupied: list[tuple[int, int, int, int]],
        atlas_width: int,
        atlas_height: int,
        label: str,
) -> list[int]:
    width, height = footprint
    if width > atlas_width or height > atlas_height:
        raise ValueError(
            f"{label} needs a {width}x{height} box-UV footprint in a "
            f"{atlas_width}x{atlas_height} atlas"
        )
    for y in range(atlas_height - height + 1):
        for x in range(atlas_width - width + 1):
            candidate = x, y, width, height
            if any(rectangles_overlap(candidate, existing) for existing in occupied):
                continue
            occupied.append(candidate)
            return [x, y]
    raise ValueError(
        f"{label} cannot fit a non-overlapping {width}x{height} box-UV footprint in the atlas"
    )


DETAILS = {
    "barc_speeder": {
        "root": [
            cube([-6, 8, -14], [12, 2, 22]),
            cube([-4.5, 8.5, -24], [9, 2, 10], rotation=[-4, 0, 0], pivot=[0, 8, -17]),
            cube([-3, 9.5, -28], [6, 1.5, 6], rotation=[-7, 0, 0], pivot=[0, 9, -24]),
            cube([-7, 7, 12], [14, 3, 9]),
            cube([-5, 6, 17], [10, 2, 6]),
            cube([-6.5, 8.4, -35], [4.6, 1.8, 14], rotation=[-5, 0, -2], pivot=[-3.5, 9, -22]),
            cube([1.9, 8.4, -35], [4.6, 1.8, 14], rotation=[-5, 0, 2], pivot=[3.5, 9, -22]),
            cube([-1.8, 8.7, -25], [3.6, 1.2, 6]),
        ],
        "engine_left": [
            cube([-12, 4, 7], [6, 5, 6]),
            cube([-12.5, 3.5, -10], [2, 7, 18], rotation=[0, 0, -5], pivot=[-9, 6, 0]),
        ],
        "engine_right": [
            cube([6, 4, 7], [6, 5, 6]),
            cube([10.5, 3.5, -10], [2, 7, 18], rotation=[0, 0, 5], pivot=[9, 6, 0]),
        ],
        "seat": [
            cube([-4, 12, 0], [8, 2, 7]),
            cube([-5, 9, 6], [10, 5, 2], rotation=[-8, 0, 0], pivot=[0, 10, 6]),
        ],
        "handlebars": [
            cube([-1, 8, -11], [2, 7, 2], rotation=[-12, 0, 0], pivot=[0, 14, -9]),
            cube([-11, 13.5, -10.5], [4, 3, 3]),
            cube([7, 13.5, -10.5], [4, 3, 3]),
        ],
    },
    "at_rt": {
        "root": [
            cube([-8.5, 16, -7], [3, 7, 14], rotation=[0, 0, -7], pivot=[-6, 18, 0]),
            cube([5.5, 16, -7], [3, 7, 14], rotation=[0, 0, 7], pivot=[6, 18, 0]),
            cube([-5, 16, -11], [10, 7, 4], rotation=[8, 0, 0], pivot=[0, 18, -7]),
            cube([-5, 18, 7], [10, 6, 4]),
            cube([-4, 34, -4], [8, 2, 8]),
            cube([-2, 24, -8.8], [4, 4, 1.2]),
            cube([-7.2, 21.0, -8.0], [1.2, 13.0, 1.2], rotation=[0, 0, -5], pivot=[-6, 21, 0]),
            cube([6.0, 21.0, -8.0], [1.2, 13.0, 1.2], rotation=[0, 0, 5], pivot=[6, 21, 0]),
            cube([-7.0, 32.5, -8.0], [14, 1.2, 1.2]),
            cube([-5.2, 20.5, 4.5], [10.4, 1.6, 5.0], rotation=[-8, 0, 0], pivot=[0, 21, 4]),
        ],
        "left_upper_leg": [
            cube([-8.5, 12, 0.5], [7, 7, 7]),
            cube([-7.8, 1, 1.2], [5.5, 12, 5.5]),
        ],
        "right_upper_leg": [
            cube([1.5, 12, 0.5], [7, 7, 7]),
            cube([2.3, 1, 1.2], [5.5, 12, 5.5]),
        ],
        "left_lower_leg": [
            cube([-8.3, -6, 0.8], [6.6, 7, 6.6]),
            cube([-7.8, -19, 1.2], [5.6, 13, 5.6]),
        ],
        "right_lower_leg": [
            cube([1.7, -6, 0.8], [6.6, 7, 6.6]),
            cube([2.2, -19, 1.2], [5.6, 13, 5.6]),
        ],
        "left_foot": [cube([-10, -24.5, -8], [10, 4, 11])],
        "right_foot": [cube([0, -24.5, -8], [10, 4, 11])],
        "chin_gun": [
            cube([-4, 13, -27], [2, 2, 20]),
            cube([2, 13, -27], [2, 2, 20]),
        ],
    },
    "stap": {
        "root": [
            cube([-5, 7, -15], [10, 2, 22]),
            cube([-4, 8, -23], [8, 2, 10], rotation=[-5, 0, 0], pivot=[0, 8, -16]),
            cube([-7, 5, 8], [14, 2, 8]),
            cube([-8, 4, 12], [16, 1.5, 7]),
            cube([-2.8, 7.8, -29], [5.6, 1.5, 17], rotation=[-4, 0, 0], pivot=[0, 8, -15]),
            cube([-1.8, 8.5, -32], [3.6, 1.0, 5]),
        ],
        "engine": [
            cube([-5, 5, 8], [10, 7, 6]),
            cube([-6, 6, 12], [4, 5, 7]),
            cube([2, 6, 12], [4, 5, 7]),
        ],
        "steering_column": [
            cube([-2.5, 17, -10.5], [5, 6, 5]),
            cube([-3.5, 21, -11.5], [7, 5, 2]),
            cube([-2, 8, -10], [4, 10, 4]),
        ],
        "controlbar": [
            cube([-11, 24, -10], [4, 4, 4]),
            cube([7, 24, -10], [4, 4, 4]),
        ],
        "footrest": [
            cube([-9, 6.5, -2], [7, 2.5, 8]),
            cube([2, 6.5, -2], [7, 2.5, 8]),
        ],
    },
    "aat": {
        "root": [
            cube([-15, 15, -17], [30, 5, 34], rotation=[0, 0, 0]),
            cube([-12, 19, -13], [24, 4, 26]),
            cube([-16, 3, -19], [6, 8, 38], rotation=[0, 0, -5], pivot=[-12, 7, 0]),
            cube([10, 3, -19], [6, 8, 38], rotation=[0, 0, 5], pivot=[12, 7, 0]),
            cube([-11, 1, -25], [22, 4, 8]),
            cube([-10, 1, 18], [20, 5, 7]),
            cube([-20, 10, -23], [8, 4, 13], rotation=[0, 8, -4], pivot=[-12, 10, -10]),
            cube([12, 10, -23], [8, 4, 13], rotation=[0, -8, 4], pivot=[12, 10, -10]),
            cube([-12, 11.5, -28], [24, 3.5, 10]),
        ],
        "turret": [
            cube([-8, 27, -10], [16, 4, 16]),
            cube([-6, 31, -8], [12, 3, 12]),
            cube([-3, 34, -5], [6, 2, 6]),
            cube([-11, 19, -9], [22, 5, 14]),
        ],
        "main_cannon": [
            cube([-4, 21, -17], [8, 8, 8]),
            cube([-2.2, 21.5, -45], [2, 3, 34]),
            cube([0.2, 21.5, -45], [2, 3, 34]),
        ],
        "left_pod": [
            cube([-22, 9, -14], [4, 10, 20]),
            cube([-21, 11, -18], [5, 4, 7]),
        ],
        "right_pod": [
            cube([18, 9, -14], [4, 10, 20]),
            cube([16, 11, -18], [5, 4, 7]),
        ],
    },
    "laat_gunship": {
        "root": [
            cube([-14, 21, -22], [28, 5, 42]),
            cube([-10, 22, -41], [20, 8, 18], rotation=[5, 0, 0], pivot=[0, 22, -25]),
            cube([-9, 7, -48], [18, 7, 9]),
            cube([-15, 1, -20], [30, 5, 39]),
            cube([-17, 7, -17], [3, 12, 30]),
            cube([14, 7, -17], [3, 12, 30]),
            cube([-10, 14, 19], [20, 6, 9]),
            cube([-12, 25.5, -39], [24, 6, 17], rotation=[8, 0, 0], pivot=[0, 25, -22]),
            cube([-14.5, 6.5, 27], [29, 14, 2.0]),
            cube([-13.5, 8.0, -19], [2.0, 11.0, 22.0]),
            cube([11.5, 8.0, -19], [2.0, 11.0, 22.0]),
        ],
        "left_wing": [
            cube([-52, 20, -3], [36, 3, 14]),
            cube([-48, 22, 7], [28, 2, 10], rotation=[0, 0, -4], pivot=[-14, 18, 4]),
            cube([-55, 18, -10], [8, 3, 12]),
            cube([-29, 19, 11], [8, 6, 5]),
        ],
        "right_wing": [
            cube([16, 20, -3], [36, 3, 14]),
            cube([20, 22, 7], [28, 2, 10], rotation=[0, 0, 4], pivot=[14, 18, 4]),
            cube([47, 18, -10], [8, 3, 12]),
            cube([21, 19, 11], [8, 6, 5]),
        ],
        "engine_left": [
            cube([-35, 20, 13], [14, 8, 7]),
            cube([-35, 20, -13], [14, 8, 6]),
        ],
        "engine_right": [
            cube([21, 20, 13], [14, 8, 7]),
            cube([21, 20, -13], [14, 8, 6]),
        ],
        "side_door": [cube([16.5, 6, -5], [2.5, 14, 18])],
        "tail": [
            cube([-6, 14, 39], [12, 7, 16]),
            cube([-2, 20, 31], [4, 15, 20]),
            cube([-9, 13, 52], [18, 4, 8]),
        ],
        "left_cannon": [
            cube([-20, 13, -32], [3, 3, 20]),
            cube([-23, 9, -18], [10, 10, 8]),
        ],
        "right_cannon": [
            cube([17, 13, -32], [3, 3, 20]),
            cube([13, 9, -18], [10, 10, 8]),
        ],
        "nose_guns": [
            cube([-5, 7, -67], [3, 3, 24]),
            cube([2, 7, -67], [3, 3, 24]),
        ],
    },
}


def refine_model(vehicle_id: str, additions: dict[str, list[dict]]) -> int:
    path = MODEL_ROOT / f"{vehicle_id}.geo.json"
    model = json.loads(path.read_text(encoding="utf-8"))
    geometry = model["minecraft:geometry"][0]
    description = geometry["description"]
    atlas_width = int(description["texture_width"])
    atlas_height = int(description["texture_height"])
    bones = {bone["name"]: bone for bone in geometry["bones"]}
    detail_keys = {
        bone_name: {
            (tuple(detail["origin"]), tuple(detail["size"]))
            for detail in detail_cubes
        }
        for bone_name, detail_cubes in additions.items()
    }
    occupied: list[tuple[int, int, int, int]] = []
    for bone_name, bone in bones.items():
        for existing_cube in bone.get("cubes", []):
            key = (tuple(existing_cube["origin"]), tuple(existing_cube["size"]))
            if key in detail_keys.get(bone_name, set()):
                continue
            width, height = box_uv_footprint(existing_cube["size"])
            u, v = existing_cube["uv"]
            if u < 0 or v < 0 or u + width > atlas_width or v + height > atlas_height:
                raise ValueError(
                    f"{vehicle_id}/{bone_name} source cube extends outside its "
                    f"{atlas_width}x{atlas_height} atlas"
                )
            # The authored cubes intentionally share broad material panels in the existing atlas.
            # Reserve their origins so generated details never masquerade as a copied source UV;
            # detail rectangles themselves remain fully non-overlapping and bounds-checked.
            occupied.append((u, v, 1, 1))
    pending: list[tuple[str, dict, tuple[int, int]]] = []
    added = 0
    for bone_name, detail_cubes in additions.items():
        if bone_name not in bones:
            raise ValueError(f"{vehicle_id} is missing required animation bone {bone_name}")
        bone = bones[bone_name]
        if not bone.get("cubes"):
            raise ValueError(f"{vehicle_id}/{bone_name} has no source UV footprint")
        existing = {
            (tuple(existing_cube["origin"]), tuple(existing_cube["size"])): existing_cube
            for existing_cube in bone["cubes"]
        }
        for detail in detail_cubes:
            key = (tuple(detail["origin"]), tuple(detail["size"]))
            target = existing.get(key)
            if target is None:
                target = dict(detail)
                bone["cubes"].append(target)
                existing[key] = target
                added += 1
            pending.append((bone_name, target, box_uv_footprint(detail["size"])))

    pending.sort(key=lambda entry: (-entry[2][1], -entry[2][0], entry[0]))
    for bone_name, detail, footprint in pending:
        detail["uv"] = allocate_uv(
            footprint,
            occupied,
            atlas_width,
            atlas_height,
            f"{vehicle_id}/{bone_name} detail at {detail['origin']}",
        )
    texture_path = TEXTURE_ROOT / f"{vehicle_id}.png"
    with Image.open(texture_path) as source:
        texture = source.convert("RGBA")
    visible_colors = [
        texture.getpixel((x, y))[:3]
        for y in range(texture.height)
        for x in range(texture.width)
        if texture.getpixel((x, y))[3]
        and max(texture.getpixel((x, y))[:3]) > 22
    ]
    if not visible_colors:
        raise ValueError(f"{vehicle_id} has no visible material colors")
    base = tuple(sorted(channel)[len(channel) // 2] for channel in zip(*visible_colors))
    for bone_name, detail, footprint in pending:
        u, v = detail["uv"]
        width, height = footprint
        salt = int(hashlib.sha256(f"{vehicle_id}:{bone_name}:{detail['origin']}".encode()).hexdigest()[:8], 16)
        for y in range(v, v + height):
            for x in range(u, u + width):
                amount = 24 if y == v else -20 if x in {u, u + width - 1} else ((salt + x * 7 + y * 11) % 9) - 4
                texture.putpixel((x, y), tuple(max(0, min(255, channel + amount)) for channel in base) + (255,))
    save_png(texture, texture_path)
    path.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
    return added


def main() -> None:
    total = 0
    for vehicle_id, additions in DETAILS.items():
        total += refine_model(vehicle_id, additions)
    print(f"Added {total} UV-safe detail cubes across {len(DETAILS)} GeckoLib vehicle models")


if __name__ == "__main__":
    main()
