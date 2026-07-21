"""Render deterministic front/side/back QA sheets for every player-visible 3D asset."""

from __future__ import annotations

import json
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from generate_character_models import save_png


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/galacticwars"
OUTPUT = ROOT / "build/previews/turnarounds"
VIEWS = {
    "front": ((0.0, 0.0, -1.0), lambda p: (p[0], p[1])),
    "side": ((1.0, 0.0, 0.0), lambda p: (p[2], p[1])),
    "back": ((0.0, 0.0, 1.0), lambda p: (-p[0], p[1])),
}
FACES = {
    "north": ((0, 1, 2, 3), (0.0, 0.0, -1.0)),
    "south": ((5, 4, 7, 6), (0.0, 0.0, 1.0)),
    "west": ((4, 0, 3, 7), (-1.0, 0.0, 0.0)),
    "east": ((1, 5, 6, 2), (1.0, 0.0, 0.0)),
    "up": ((3, 2, 6, 7), (0.0, 1.0, 0.0)),
    "down": ((4, 5, 1, 0), (0.0, -1.0, 0.0)),
}


def rotate(point: tuple[float, float, float], pivot: list[float], angles: list[float]) -> tuple[float, float, float]:
    x, y, z = (point[index] - float(pivot[index]) for index in range(3))
    ax, ay, az = (math.radians(float(value)) for value in angles)
    y, z = y * math.cos(ax) - z * math.sin(ax), y * math.sin(ax) + z * math.cos(ax)
    x, z = x * math.cos(ay) + z * math.sin(ay), -x * math.sin(ay) + z * math.cos(ay)
    x, y = x * math.cos(az) - y * math.sin(az), x * math.sin(az) + y * math.cos(az)
    return x + pivot[0], y + pivot[1], z + pivot[2]


def transform(point, cube: dict, bone: dict, bones: dict[str, dict]):
    transformed = tuple(float(value) for value in point)
    if "rotation" in cube:
        transformed = rotate(transformed, cube.get("pivot", bone.get("pivot", [0, 0, 0])), cube["rotation"])
    current = bone
    while current is not None:
        if "rotation" in current:
            transformed = rotate(transformed, current.get("pivot", [0, 0, 0]), current["rotation"])
        current = bones.get(current.get("parent"))
    return transformed


def cube_vertices(cube: dict) -> list[tuple[float, float, float]]:
    x, y, z = (float(value) for value in cube["origin"])
    width, height, depth = (float(value) for value in cube["size"])
    inflate = float(cube.get("inflate", 0))
    x0, y0, z0 = x - inflate, y - inflate, z - inflate
    x1, y1, z1 = x + width + inflate, y + height + inflate, z + depth + inflate
    return [
        (x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0),
        (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1),
    ]


def box_uv_bounds(uv: list[int], size: list[float], face: str) -> tuple[int, int, int, int]:
    u, v = (round(value) for value in uv)
    width, height, depth = (max(1, math.ceil(float(value))) for value in size)
    return {
        "west": (u, v + depth, depth, height),
        "north": (u + depth, v + depth, width, height),
        "east": (u + depth + width, v + depth, depth, height),
        "south": (u + 2 * depth + width, v + depth, width, height),
        "up": (u + depth, v, width, depth),
        "down": (u + depth + width, v, width, depth),
    }[face]


def face_color(texture: Image.Image, cube: dict, face: str) -> tuple[int, int, int, int] | None:
    uv = cube.get("uv")
    if isinstance(uv, list):
        left, top, width, height = box_uv_bounds(uv, cube["size"], face)
    elif isinstance(uv, dict) and face in uv:
        face_uv = uv[face]
        left, top = (round(value) for value in face_uv["uv"])
        width, height = (round(abs(value)) for value in face_uv.get("uv_size", (1, 1)))
    else:
        return None
    pixels = []
    for y in range(max(0, top), min(texture.height, top + max(1, height))):
        for x in range(max(0, left), min(texture.width, left + max(1, width))):
            pixel = texture.getpixel((x, y))
            if pixel[3]:
                pixels.append(pixel)
    if not pixels:
        return None
    return tuple(sum(pixel[index] for pixel in pixels) // len(pixels) for index in range(4))


def scene(model_path: Path, texture_path: Path, view: str) -> tuple[list, tuple[float, float, float, float]]:
    model = json.loads(model_path.read_text(encoding="utf-8"))["minecraft:geometry"][0]
    bones = {bone["name"]: bone for bone in model["bones"]}
    with Image.open(texture_path) as source:
        texture = source.convert("RGBA")
    camera, project = VIEWS[view]
    polygons = []
    bounds = [float("inf"), float("inf"), float("-inf"), float("-inf")]
    for bone in model["bones"]:
        for cube in bone.get("cubes", []):
            vertices = [transform(point, cube, bone, bones) for point in cube_vertices(cube)]
            center = tuple(sum(point[index] for point in vertices) / 8 for index in range(3))
            for face, (indices, normal) in FACES.items():
                transformed_center = transform(center, cube, bone, bones)
                transformed_normal_tip = transform(tuple(center[index] + normal[index] for index in range(3)), cube, bone, bones)
                transformed_normal = tuple(transformed_normal_tip[index] - transformed_center[index] for index in range(3))
                if sum(transformed_normal[index] * camera[index] for index in range(3)) <= 0.01:
                    continue
                color = face_color(texture, cube, face)
                if color is None:
                    continue
                points = [project(vertices[index]) for index in indices]
                for x, y in points:
                    bounds[0] = min(bounds[0], x)
                    bounds[1] = min(bounds[1], y)
                    bounds[2] = max(bounds[2], x)
                    bounds[3] = max(bounds[3], y)
                depth = sum(center[index] * camera[index] for index in range(3))
                polygons.append((depth, points, color))
    if not polygons:
        raise ValueError(f"{model_path} has no visible {view} polygons")
    return sorted(polygons, key=lambda value: value[0]), tuple(bounds)


def render_turnaround(model_path: Path, texture_path: Path, label: str) -> Image.Image:
    width, height = 342, 180
    canvas = Image.new("RGBA", (width, height), (18, 21, 26, 255))
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.load_default()
    for view_index, view in enumerate(VIEWS):
        polygons, bounds = scene(model_path, texture_path, view)
        left, bottom, right, top = bounds
        cell_left = view_index * 114
        scale = min(100 / max(1, right - left), 138 / max(1, top - bottom))
        center_x = cell_left + 57
        baseline = 151
        for _, points, color in polygons:
            projected = [
                (round(center_x + (x - (left + right) / 2) * scale), round(baseline - (y - bottom) * scale))
                for x, y in points
            ]
            draw.polygon(projected, fill=color, outline=(10, 12, 16, 210))
        draw.text((cell_left + 5, 5), view, fill=(170, 178, 190, 255), font=font)
    text_width = draw.textbbox((0, 0), label, font=font)[2]
    draw.text(((width - text_width) // 2, 164), label, fill=(239, 241, 244, 255), font=font)
    return canvas


def contact_sheet(entries: list[tuple[Path, Path, str]], output: Path, columns: int = 2) -> None:
    previews = [render_turnaround(*entry) for entry in entries]
    rows = math.ceil(len(previews) / columns)
    sheet = Image.new("RGBA", (columns * 342, rows * 180), (10, 12, 16, 255))
    for index, preview in enumerate(previews):
        sheet.alpha_composite(preview, ((index % columns) * 342, (index // columns) * 180))
    save_png(sheet, output)


def main() -> None:
    entity_models = ASSETS / "geckolib/models/entity"
    entity_textures = ASSETS / "textures/entity"
    recruits = [
        "phase_i_clone_trooper", "phase_i_arc_trooper", "clone_trooper", "arc_trooper",
        "jedi_knight", "senate_commando", "republic_honor_guard", "b1_battle_droid",
        "b1_security_droid", "b2_super_battle_droid", "commando_droid", "mandalorian_warrior",
        "mandalorian_marksman", "mandalorian_heavy", "hutt_enforcer", "bounty_hunter",
        "smuggler", "nightsister_acolyte", "nightsister_archer", "nightbrother_brute",
        "republic_civilian", "togruta_civilian", "separatist_technician",
        "mandalorian_clansperson", "hutt_civilian", "nightsister_civilian",
    ]
    contact_sheet([(entity_models / f"{asset}.geo.json", entity_textures / f"{asset}.png", asset) for asset in recruits], OUTPUT / "all_26_recruits.png")
    commander = [
        (entity_models / "clone_trooper.geo.json", entity_textures / "clone_trooper_commander.png", "clone commander"),
        (entity_models / "arc_trooper.geo.json", entity_textures / "arc_trooper_commander.png", "ARC commander"),
        (entity_models / "b1_battle_droid.geo.json", entity_textures / "b1_battle_droid_commander.png", "B1 commander"),
    ]
    contact_sheet(commander, OUTPUT / "commander_variants.png", 1)
    armor = ["phase_i_clone", "republic_plastoid", "separatist_alloy", "mandalorian_alloy", "nightsister_weave", "beskar"]
    contact_sheet([(ASSETS / f"geckolib/models/armor/{asset}.geo.json", ASSETS / f"textures/armor/{asset}.png", asset) for asset in armor], OUTPUT / "armor_families.png")
    vehicles = ["barc_speeder", "at_rt", "stap", "aat", "laat_gunship"]
    contact_sheet([(entity_models / f"vehicle/{asset}.geo.json", entity_textures / f"vehicle/{asset}.png", asset) for asset in vehicles], OUTPUT / "vehicles.png", 1)
    sabers = ["blue", "green", "red", "purple", "yellow", "white"]
    contact_sheet([(ASSETS / f"geckolib/models/item/lightsaber/{asset}.geo.json", ASSETS / f"textures/item/lightsaber/{asset}.png", f"{asset} lightsaber") for asset in sabers], OUTPUT / "lightsabers.png")
    blasters = ["dc15_blaster", "e5_blaster", "westar_blaster", "scatter_blaster"]
    contact_sheet([(ASSETS / f"geckolib/models/item/blaster/{asset}.geo.json", ASSETS / f"textures/item/blaster/{asset}.png", asset) for asset in blasters], OUTPUT / "blasters.png")
    capsules = ["clone_trooper", "b1_battle_droid", "togruta_civilian", "nightsister_acolyte"]
    contact_sheet([(ASSETS / "geckolib/models/item/spawn_capsule.geo.json", ASSETS / f"textures/item/spawn_capsule/{asset}.png", f"{asset} capsule") for asset in capsules], OUTPUT / "representative_capsules.png")
    print(f"Rendered {len(recruits) + 3 + len(armor) + len(vehicles) + len(sabers) + len(blasters) + len(capsules)} front/side/back previews to {OUTPUT}")


if __name__ == "__main__":
    main()
