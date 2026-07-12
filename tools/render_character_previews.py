"""Render deterministic front-view contact sheets from generated GeckoLib box geometry."""

from __future__ import annotations

import json
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/galacticwars"
OUTPUT = ROOT / "build/previews"
SCALE = 6
VIEW_WIDTH = 25
VIEW_HEIGHT = 37


def front_face(texture: Image.Image, uv: list[int] | dict, size: list[float]) -> Image.Image:
    width, height, depth = (max(1, math.ceil(value)) for value in size)
    if isinstance(uv, dict):
        face = uv.get("north") or next(iter(uv.values()))
        left, top = (round(value) for value in face["uv"])
        face_width, face_height = (max(1, round(value)) for value in face.get("uv_size", (width, height)))
    else:
        left = uv[0] + depth
        top = uv[1] + depth
        face_width, face_height = width, height
    return texture.crop((left, top, left + face_width, top + face_height))


def render_model(model_path: Path, texture_path: Path, label: str) -> Image.Image:
    model = json.loads(model_path.read_text(encoding="utf-8"))
    texture = Image.open(texture_path).convert("RGBA")
    cubes = []
    for bone in model["minecraft:geometry"][0]["bones"]:
        for cube in bone.get("cubes", []):
            cubes.append((cube["origin"][2], bone["name"], cube))
    cubes.sort(key=lambda entry: entry[0], reverse=True)
    canvas = Image.new("RGBA", (VIEW_WIDTH * SCALE, VIEW_HEIGHT * SCALE + 24), (20, 23, 28, 255))
    draw = ImageDraw.Draw(canvas)
    center_x = VIEW_WIDTH * SCALE // 2
    baseline = (VIEW_HEIGHT - 2) * SCALE
    for _, bone, cube in cubes:
        x, y, _ = cube["origin"]
        width, height, _ = cube["size"]
        inflate = cube.get("inflate", 0)
        left = round(center_x + (x - inflate) * SCALE)
        right = round(center_x + (x + width + inflate) * SCALE)
        top = round(baseline - (y + height + inflate) * SCALE)
        bottom = round(baseline - (y - inflate) * SCALE)
        face = front_face(texture, cube["uv"], cube["size"])
        face = face.resize((max(1, right - left + 1), max(1, bottom - top + 1)), Image.Resampling.NEAREST)
        canvas.alpha_composite(face, (left, top))
        draw.rectangle((left, top, right, bottom), outline=(20, 23, 28, 145), width=1)
    text_box = draw.textbbox((0, 0), label, font=ImageFont.load_default())
    text_width = text_box[2] - text_box[0]
    draw.text(((canvas.width - text_width) // 2, canvas.height - 17), label,
              fill=(232, 235, 240, 255), font=ImageFont.load_default())
    return canvas


def contact_sheet(entries: list[tuple[Path, Path, str]], columns: int, output: Path) -> None:
    previews = [render_model(*entry) for entry in entries]
    rows = math.ceil(len(previews) / columns)
    sheet = Image.new(
        "RGBA",
        (columns * previews[0].width, rows * previews[0].height),
        (12, 14, 18, 255),
    )
    for index, preview in enumerate(previews):
        sheet.alpha_composite(preview, ((index % columns) * preview.width,
                                        (index // columns) * preview.height))
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output)


def main() -> None:
    entity_models = ASSETS / "geckolib/models/entity"
    entity_textures = ASSETS / "textures/entity"
    recruits = [
        "clone_trooper", "arc_trooper", "jedi_knight", "b1_battle_droid",
        "b2_super_battle_droid", "commando_droid", "mandalorian_warrior",
        "mandalorian_marksman", "mandalorian_heavy", "hutt_enforcer",
        "bounty_hunter", "smuggler", "nightsister_acolyte", "nightsister_archer",
        "nightbrother_brute", "republic_civilian", "separatist_technician",
        "mandalorian_clansperson", "hutt_civilian", "nightsister_civilian",
    ]
    contact_sheet([
        (entity_models / f"{recruit}.geo.json", entity_textures / f"{recruit}.png", recruit)
        for recruit in recruits
    ], 5, OUTPUT / "recruit_models.png")
    armor_models = ASSETS / "geckolib/models/armor"
    armor_textures = ASSETS / "textures/armor"
    armor = ["republic_plastoid", "separatist_alloy", "mandalorian_alloy",
             "nightsister_weave", "beskar"]
    contact_sheet([
        (armor_models / f"{family}.geo.json", armor_textures / f"{family}.png", family)
        for family in armor
    ], 5, OUTPUT / "armor_models.png")
    print(f"Rendered previews to {OUTPUT}")


if __name__ == "__main__":
    main()
