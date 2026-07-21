"""Generate four distinct volumetric GeckoLib blasters and their UV-safe atlases."""

from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path

from PIL import Image

from generate_character_models import ModelBuilder, Palette, save_png


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/galacticwars"
MODEL_ROOT = ASSETS / "geckolib/models/item/blaster"
ANIMATION_ROOT = ASSETS / "geckolib/animations/item/blaster"
TEXTURE_ROOT = ASSETS / "textures/item/blaster"
ITEM_MODEL_ROOT = ASSETS / "models/item"
ITEM_DEFINITION_ROOT = ASSETS / "items"


@dataclass(frozen=True)
class BlasterDesign:
    id: str
    palette: Palette


DESIGNS = (
    BlasterDesign("dc15_blaster", Palette(
        (0, 0, 0), (47, 57, 68), (17, 23, 29), (64, 75, 86),
        (148, 162, 169), (49, 158, 236), (78, 69, 57))),
    BlasterDesign("e5_blaster", Palette(
        (0, 0, 0), (62, 53, 42), (28, 27, 24), (116, 97, 66),
        (183, 153, 99), (57, 215, 203), (67, 52, 35))),
    BlasterDesign("westar_blaster", Palette(
        (0, 0, 0), (51, 52, 60), (19, 20, 25), (118, 123, 132),
        (211, 217, 220), (176, 75, 244), (58, 47, 42))),
    BlasterDesign("scatter_blaster", Palette(
        (0, 0, 0), (56, 58, 47), (20, 23, 22), (76, 82, 65),
        (145, 150, 122), (240, 126, 37), (69, 52, 38))),
)


def bones(builder: ModelBuilder) -> None:
    builder.bone("root", [0, 0, 0])
    builder.bone("receiver", [0, 0, 0], "root")
    builder.bone("stock", [0, 0, 5], "root")
    builder.bone("grip", [0, -1, 1], "root")
    builder.bone("barrel", [0, 0, -6], "root")
    builder.bone("sight", [0, 3, -2], "receiver")
    builder.bone("power_cell", [0, 0, 0], "receiver")
    builder.bone("muzzle", [0, 0, -16], "barrel")


def common_details(builder: ModelBuilder) -> None:
    builder.cube("receiver", "receiver_core", [-2.8, -1.8, -6], [5.6, 4.8, 11], "base")
    builder.cube("receiver", "receiver_top_rail", [-2.2, 2.8, -5.5], [4.4, 0.7, 9.5], "light")
    builder.cube("receiver", "receiver_lower", [-2.15, -2.6, -4.5], [4.3, 1.0, 7.4], "shadow")
    builder.cube("grip", "pistol_grip", [-1.65, -7.0, 0.2], [3.3, 5.8, 3.4], "cloth",
                 rotation=[18, 0, 0], pivot=[0, -1, 1.5])
    builder.cube("grip", "trigger_guard", [-1.8, -3.6, -2.2], [3.6, 0.65, 3.0], "dark")
    builder.cube("power_cell", "power_cell", [-2.95, -0.2, -1.7], [0.7, 2.2, 4.8], "accent")
    builder.cube("sight", "rear_sight", [-1.3, 3.4, 1.2], [2.6, 1.0, 1.2], "dark")


def dc15(builder: ModelBuilder) -> None:
    common_details(builder)
    builder.cube("stock", "shoulder_stock", [-3.0, -1.8, 4.5], [6.0, 5.0, 8.8], "dark")
    builder.cube("stock", "stock_cutout", [-2.25, -0.7, 10.8], [4.5, 2.8, 3.0], "shadow")
    builder.cube("barrel", "long_barrel", [-1.4, -0.5, -18.5], [2.8, 2.6, 13], "shadow")
    builder.cube("barrel", "barrel_shroud", [-2.2, -1.0, -12.0], [4.4, 3.6, 5.0], "base")
    builder.cube("barrel", "gas_tube", [-0.55, 2.1, -15.5], [1.1, 1.0, 9.0], "light")
    builder.cube("sight", "front_sight", [-0.7, 3.0, -15.0], [1.4, 1.8, 1.0], "light")
    builder.cube("muzzle", "muzzle_brake", [-2.0, -0.8, -20.0], [4.0, 3.2, 2.0], "dark")
    builder.cube("muzzle", "emitter", [-1.3, -0.25, -20.35], [2.6, 2.1, 0.55], "accent")


def e5(builder: ModelBuilder) -> None:
    common_details(builder)
    builder.cube("stock", "skeletal_stock_top", [-2.2, 1.0, 4.2], [4.4, 1.0, 9.5], "base")
    builder.cube("stock", "skeletal_stock_bottom", [-2.2, -2.2, 4.2], [4.4, 1.0, 9.5], "base")
    builder.cube("stock", "stock_end", [-2.7, -2.3, 12.5], [5.4, 4.5, 1.4], "dark")
    builder.cube("barrel", "thin_barrel", [-1.0, 0.0, -18.0], [2.0, 1.8, 12.5], "dark")
    for index, z in enumerate((-9.5, -12.5, -15.5)):
        builder.cube("barrel", f"barrel_ring_{index}", [-1.7, -0.7, z], [3.4, 3.2, 0.8], "light")
    builder.cube("barrel", "lower_support", [-0.55, -2.5, -15.8], [1.1, 1.0, 9.2], "base")
    builder.cube("muzzle", "emitter", [-1.45, -0.4, -19.0], [2.9, 2.6, 0.65], "accent")


def westar(builder: ModelBuilder) -> None:
    builder.cube("receiver", "pistol_receiver", [-2.45, -1.6, -5.0], [4.9, 4.3, 10.0], "light")
    builder.cube("receiver", "angular_upper", [-2.0, 2.5, -4.2], [4.0, 1.0, 7.2], "base")
    builder.cube("receiver", "side_plate", [-2.9, -0.5, -3.5], [0.65, 2.3, 6.0], "shadow")
    builder.cube("grip", "twin_grip_right", [-2.0, -7.0, 0.4], [1.55, 5.7, 3.2], "cloth",
                 rotation=[16, 0, 0], pivot=[-1.2, -1, 1.4])
    builder.cube("grip", "twin_grip_left", [0.45, -7.0, 0.4], [1.55, 5.7, 3.2], "cloth",
                 rotation=[16, 0, 0], pivot=[1.2, -1, 1.4])
    builder.cube("grip", "trigger_guard", [-1.7, -3.4, -2.0], [3.4, 0.6, 2.8], "dark")
    builder.cube("power_cell", "side_cell", [2.3, -0.3, -2.4], [0.7, 2.0, 4.2], "accent")
    builder.cube("barrel", "short_barrel", [-1.25, -0.3, -11.5], [2.5, 2.2, 7.0], "shadow")
    builder.cube("barrel", "barrel_collar", [-2.0, -0.9, -7.2], [4.0, 3.4, 2.0], "base")
    builder.cube("sight", "compact_sight", [-0.7, 3.2, -3.2], [1.4, 1.1, 2.3], "dark")
    builder.cube("muzzle", "fork_right", [-2.0, -0.5, -12.5], [1.2, 2.6, 1.7], "dark")
    builder.cube("muzzle", "fork_left", [0.8, -0.5, -12.5], [1.2, 2.6, 1.7], "dark")
    builder.cube("muzzle", "emitter", [-0.7, 0.0, -12.8], [1.4, 1.6, 0.55], "accent")


def scatter(builder: ModelBuilder) -> None:
    builder.cube("receiver", "broad_receiver", [-3.8, -2.3, -5.5], [7.6, 5.8, 11.5], "base")
    builder.cube("receiver", "vented_shroud", [-3.3, 3.25, -4.8], [6.6, 0.9, 8.0], "light")
    for index, z in enumerate((-3.5, -1.0, 1.5)):
        builder.cube("receiver", f"vent_{index}", [-3.95, 0.2, z], [0.55, 1.4, 1.1], "dark")
    builder.cube("stock", "heavy_stock", [-3.4, -2.0, 5.5], [6.8, 5.4, 8.5], "dark")
    builder.cube("grip", "heavy_grip", [-1.8, -7.2, 0.5], [3.6, 5.8, 3.8], "cloth",
                 rotation=[16, 0, 0], pivot=[0, -1, 1.5])
    builder.cube("grip", "forward_grip", [-1.5, -5.2, -7.5], [3.0, 4.2, 3.0], "cloth",
                 rotation=[-10, 0, 0], pivot=[0, -1, -7])
    builder.cube("power_cell", "heat_bank", [-3.95, -0.8, -1.8], [0.7, 2.8, 5.2], "accent")
    builder.cube("barrel", "right_scatter_barrel", [-2.6, -0.7, -15.5], [2.0, 2.4, 10.5], "shadow")
    builder.cube("barrel", "left_scatter_barrel", [0.6, -0.7, -15.5], [2.0, 2.4, 10.5], "shadow")
    builder.cube("barrel", "barrel_bridge", [-3.1, -1.2, -9.0], [6.2, 3.4, 2.0], "light")
    builder.cube("muzzle", "wide_emitter", [-2.8, -0.9, -16.0], [5.6, 2.8, 0.65], "accent")


BUILDERS = {
    "dc15_blaster": dc15,
    "e5_blaster": e5,
    "westar_blaster": westar,
    "scatter_blaster": scatter,
}


def write_glowmask(texture_path: Path, palette: Palette) -> None:
    with Image.open(texture_path) as source:
        texture = source.convert("RGBA")
    glow = Image.new("RGBA", texture.size, (0, 0, 0, 0))
    for y in range(texture.height):
        for x in range(texture.width):
            pixel = texture.getpixel((x, y))
            if pixel[3] and sum(abs(pixel[index] - palette.accent[index]) for index in range(3)) <= 100:
                glow.putpixel((x, y), pixel)
    save_png(glow, texture_path.with_name(texture_path.stem + "_glowmask.png"))


def display_model(weapon_id: str) -> dict:
    pistol = weapon_id == "westar_blaster"
    heavy = weapon_id == "scatter_blaster"
    scale = 0.58 if heavy else 0.66 if pistol else 0.62
    return {
        "parent": "builtin/entity",
        "ambientocclusion": False,
        "gui_light": "front",
        "display": {
            "thirdperson_righthand": {"rotation": [-8, -88, -8], "translation": [0.4, 2.6, -1.2], "scale": [scale] * 3},
            "thirdperson_lefthand": {"rotation": [-8, 88, 8], "translation": [-0.4, 2.6, -1.2], "scale": [scale] * 3},
            "firstperson_righthand": {"rotation": [0, -92, -4], "translation": [1.2, 2.8, 0.6], "scale": [scale - 0.08] * 3},
            "firstperson_lefthand": {"rotation": [0, 92, 4], "translation": [-1.2, 2.8, 0.6], "scale": [scale - 0.08] * 3},
            "gui": {"rotation": [24, 225, 0], "translation": [0, 0, 0], "scale": [0.55] * 3},
            "ground": {"rotation": [0, 90, 0], "translation": [0, 2, 0], "scale": [0.42] * 3},
            "fixed": {"rotation": [0, 90, 15], "translation": [0, 0, 0], "scale": [0.58] * 3},
        },
    }


def generate_all() -> None:
    for directory in (MODEL_ROOT, ANIMATION_ROOT, TEXTURE_ROOT, ITEM_MODEL_ROOT, ITEM_DEFINITION_ROOT):
        directory.mkdir(parents=True, exist_ok=True)
    animation = {
        "format_version": "1.8.0",
        "animations": {"animation.blaster.idle": {"loop": True, "animation_length": 1, "bones": {}}},
    }
    for design in DESIGNS:
        builder = ModelBuilder(
            f"item.blaster.{design.id}", design.palette, atlas_size=256, texel_density=2)
        bones(builder)
        BUILDERS[design.id](builder)
        model_path = MODEL_ROOT / f"{design.id}.geo.json"
        texture_path = TEXTURE_ROOT / f"{design.id}.png"
        builder.write(model_path, texture_path)
        model = json.loads(model_path.read_text(encoding="utf-8"))
        description = model["minecraft:geometry"][0]["description"]
        description.update({"visible_bounds_width": 5, "visible_bounds_height": 3, "visible_bounds_offset": [0, 0, -4]})
        model_path.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
        write_glowmask(texture_path, design.palette)
        (ANIMATION_ROOT / f"{design.id}.animation.json").write_text(
            json.dumps(animation, indent=2) + "\n", encoding="utf-8")
        (ITEM_MODEL_ROOT / f"{design.id}.json").write_text(
            json.dumps(display_model(design.id), indent=2) + "\n", encoding="utf-8")
        (ITEM_DEFINITION_ROOT / f"{design.id}.json").write_text(json.dumps({
            "model": {
                "type": "minecraft:special",
                "base": f"galacticwars:item/{design.id}",
                "model": {"type": "geckolib:geckolib"},
            }
        }, indent=2) + "\n", encoding="utf-8")
    print(f"Generated {len(DESIGNS)} volumetric GeckoLib blasters")


if __name__ == "__main__":
    generate_all()
