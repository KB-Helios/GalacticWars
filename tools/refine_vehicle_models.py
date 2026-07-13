"""Refine the five launch vehicle GeckoLib geometries without breaking their UV/animation contracts.

The authored 256x256 atlases already contain high-detail material panels in each original cube's
UV footprint. Added detail cubes deliberately reuse a smaller portion of the matching bone's
existing footprint, so geometry can gain silhouette detail without inventing or misaligning UVs.
"""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MODEL_ROOT = ROOT / "src/main/resources/assets/galacticwars/geckolib/models/entity/vehicle"


def cube(origin, size, *, rotation=None, pivot=None, inflate=None):
    value = {"origin": origin, "size": size}
    if rotation is not None:
        value["rotation"] = rotation
    if pivot is not None:
        value["pivot"] = pivot
    if inflate is not None:
        value["inflate"] = inflate
    return value


DETAILS = {
    "barc_speeder": {
        "root": [
            cube([-6, 8, -14], [12, 2, 22]),
            cube([-4.5, 8.5, -24], [9, 2, 10], rotation=[-4, 0, 0], pivot=[0, 8, -17]),
            cube([-3, 9.5, -28], [6, 1.5, 6], rotation=[-7, 0, 0], pivot=[0, 9, -24]),
            cube([-7, 7, 12], [14, 3, 9]),
            cube([-5, 6, 17], [10, 2, 6]),
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
    bones = {bone["name"]: bone for bone in model["minecraft:geometry"][0]["bones"]}
    added = 0
    for bone_name, detail_cubes in additions.items():
        if bone_name not in bones:
            raise ValueError(f"{vehicle_id} is missing required animation bone {bone_name}")
        bone = bones[bone_name]
        if not bone.get("cubes"):
            raise ValueError(f"{vehicle_id}/{bone_name} has no source UV footprint")
        source_uv = bone["cubes"][0]["uv"]
        existing = {(tuple(cube["origin"]), tuple(cube["size"])) for cube in bone["cubes"]}
        for detail in detail_cubes:
            key = (tuple(detail["origin"]), tuple(detail["size"]))
            if key in existing:
                continue
            detail["uv"] = source_uv
            bone["cubes"].append(detail)
            existing.add(key)
            added += 1
    path.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
    return added


def main() -> None:
    total = 0
    for vehicle_id, additions in DETAILS.items():
        total += refine_model(vehicle_id, additions)
    print(f"Added {total} UV-safe detail cubes across {len(DETAILS)} GeckoLib vehicle models")


if __name__ == "__main__":
    main()
