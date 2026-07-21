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
        raw_width, raw_height = (round(value) for value in face.get("uv_size", (width, height)))
        right = left + raw_width
        bottom = top + raw_height
        crop = texture.crop((min(left, right), min(top, bottom), max(left, right), max(top, bottom)))
        if raw_width < 0:
            crop = crop.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
        if raw_height < 0:
            crop = crop.transpose(Image.Transpose.FLIP_TOP_BOTTOM)
        return crop
    else:
        left = uv[0] + depth
        top = uv[1] + depth
        face_width, face_height = width, height
    return texture.crop((left, top, left + face_width, top + face_height))


def render_model(model_path: Path, texture_path: Path, label: str) -> Image.Image:
    model = json.loads(model_path.read_text(encoding="utf-8"))
    with Image.open(texture_path) as source:
        texture = source.convert("RGBA")
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


def texture_sheet(entries: list[tuple[Path, str]], columns: int, output: Path) -> None:
    """Render small game textures at nearest-neighbour scale for visual QA."""
    cell_width = 164
    cell_height = 142
    previews = []
    for path, label in entries:
        with Image.open(path) as source:
            texture = source.convert("RGBA")
        scale = min(112 / texture.width, 104 / texture.height)
        rendered = texture.resize(
            (max(1, round(texture.width * scale)), max(1, round(texture.height * scale))),
            Image.Resampling.NEAREST,
        )
        preview = Image.new("RGBA", (cell_width, cell_height), (20, 23, 28, 255))
        preview.alpha_composite(rendered, ((cell_width - rendered.width) // 2, 5))
        draw = ImageDraw.Draw(preview)
        text_box = draw.textbbox((0, 0), label, font=ImageFont.load_default())
        text_width = text_box[2] - text_box[0]
        draw.text(((cell_width - text_width) // 2, cell_height - 17), label,
                  fill=(232, 235, 240, 255), font=ImageFont.load_default())
        previews.append(preview)
    rows = math.ceil(len(previews) / columns)
    sheet = Image.new("RGBA", (columns * cell_width, rows * cell_height), (12, 14, 18, 255))
    for index, preview in enumerate(previews):
        sheet.alpha_composite(preview, ((index % columns) * cell_width,
                                        (index // columns) * cell_height))
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output)


def main() -> None:
    from render_asset_turnarounds import main as render_turnarounds
    render_turnarounds()
    entity_models = ASSETS / "geckolib/models/entity"
    entity_textures = ASSETS / "textures/entity"
    recruits = [
        "phase_i_clone_trooper", "phase_i_arc_trooper",
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
    armor = ["phase_i_clone", "republic_plastoid", "separatist_alloy",
             "mandalorian_alloy", "nightsister_weave", "beskar"]
    contact_sheet([
        (armor_models / f"{family}.geo.json", armor_textures / f"{family}.png", family)
        for family in armor
    ], 5, OUTPUT / "armor_models.png")
    block_textures = ASSETS / "textures/block"
    texture_sheet([
        (path, path.stem) for path in sorted(block_textures.glob("*.png"))
    ], 5, OUTPUT / "block_textures.png")
    item_textures = ASSETS / "textures/item"
    texture_sheet([
        (item_textures / f"{color}_lightsaber.png", f"{color}_lightsaber")
        for color in ("blue", "green", "red", "purple", "yellow", "white")
    ], 3, OUTPUT / "lightsaber_items.png")
    lightsaber_materials = item_textures / "lightsaber"
    lightsaber_entries = [
        (lightsaber_materials / f"{color}_{part}.png", f"{color}_{part}")
        for color in ("blue", "green", "red", "purple", "yellow", "white")
        for part in ("hilt", "blade")
        if (lightsaber_materials / f"{color}_{part}.png").is_file()
    ]
    if lightsaber_entries:
        texture_sheet(lightsaber_entries, 6, OUTPUT / "lightsaber_materials.png")
    print(f"Rendered previews to {OUTPUT}")


if __name__ == "__main__":
    main()
