"""Generate the shared GeckoLib lightsaber model and its UV-safe material variants.

The retained image-generation reference establishes silhouette and material direction. This
script is the distributable source of truth: geometry, UV allocation, animated textures,
glowmasks, item definitions, and display transforms are regenerated together.
"""

from __future__ import annotations

import hashlib
import json
import math
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/galacticwars"
MODEL_PATH = ASSETS / "geckolib/models/item/lightsaber.geo.json"
ANIMATION_PATH = ASSETS / "geckolib/animations/item/lightsaber.animation.json"
TEXTURE_ROOT = ASSETS / "textures/item/lightsaber"
ITEM_TEXTURE_ROOT = ASSETS / "textures/item"
ITEM_MODEL_ROOT = ASSETS / "models/item"
ITEM_DEFINITION_ROOT = ASSETS / "items"
ATLAS_SIZE = 256
TEXEL_DENSITY = 2
FRAME_COUNT = 4


Color = tuple[int, int, int]


@dataclass(frozen=True)
class Palette:
    shadow: Color
    metal: Color
    light: Color
    grip: Color
    grip_light: Color
    accent: Color
    blade: Color
    glow: Color


PALETTES = {
    "blue": Palette((25, 31, 39), (116, 132, 145), (220, 231, 235),
                    (19, 23, 29), (52, 61, 70), (35, 145, 255),
                    (42, 139, 255), (126, 205, 255)),
    "green": Palette((31, 35, 31), (111, 124, 112), (208, 221, 211),
                     (20, 27, 24), (50, 62, 55), (45, 220, 105),
                     (34, 205, 91), (137, 255, 178)),
    "red": Palette((38, 25, 27), (105, 99, 102), (205, 200, 202),
                   (24, 17, 19), (61, 43, 46), (245, 55, 55),
                   (235, 42, 47), (255, 139, 132)),
    "purple": Palette((28, 25, 38), (104, 98, 122), (208, 199, 225),
                      (19, 18, 26), (50, 45, 65), (165, 75, 255),
                      (151, 62, 244), (221, 156, 255)),
    "yellow": Palette((47, 37, 27), (119, 108, 88), (219, 207, 177),
                      (29, 24, 18), (66, 55, 40), (255, 205, 45),
                      (241, 185, 34), (255, 239, 145)),
    "white": Palette((32, 37, 44), (139, 154, 165), (231, 240, 244),
                     (22, 26, 32), (57, 66, 75), (205, 235, 255),
                     (183, 226, 255), (239, 251, 255)),
}


@dataclass(frozen=True)
class CubeSpec:
    label: str
    bone: str
    origin: tuple[float, float, float]
    size: tuple[float, float, float]
    material: str


def cubes() -> tuple[CubeSpec, ...]:
    specs = [
        CubeSpec("pommel_tip", "hilt", (-1.25, 0.0, -1.25), (2.5, 0.45, 2.5), "shadow"),
        CubeSpec("pommel_cap", "hilt", (-1.65, 0.4, -1.65), (3.3, 0.7, 3.3), "metal"),
        CubeSpec("pommel_ring", "hilt", (-1.9, 1.05, -1.9), (3.8, 0.45, 3.8), "light"),
        CubeSpec("grip_core", "hilt", (-1.38, 1.45, -1.38), (2.76, 4.5, 2.76), "grip"),
    ]
    for index, y in enumerate((1.65, 2.35, 3.05, 3.75, 4.45, 5.15), start=1):
        specs.append(CubeSpec(f"grip_rib_{index}", "hilt", (-1.62, y, -1.62),
                              (3.24, 0.28, 3.24), "grip_light"))
    specs.extend((
        CubeSpec("lower_collar", "hilt", (-1.75, 5.8, -1.75), (3.5, 0.65, 3.5), "metal"),
        CubeSpec("control_barrel", "hilt", (-1.48, 6.4, -1.48), (2.96, 1.7, 2.96), "metal"),
        CubeSpec("activation_housing", "hilt", (1.35, 6.65, -0.9), (0.55, 1.15, 1.8), "shadow"),
        CubeSpec("activation_switch", "hilt", (1.82, 6.95, -0.52), (0.3, 0.55, 1.04), "accent"),
        CubeSpec("upper_collar", "hilt", (-1.88, 8.05, -1.88), (3.76, 0.7, 3.76), "light"),
        CubeSpec("emitter_body", "hilt", (-1.55, 8.72, -1.55), (3.1, 1.05, 3.1), "metal"),
        CubeSpec("emitter_front", "hilt", (-1.7, 9.55, -1.7), (3.4, 0.52, 3.4), "light"),
        CubeSpec("emitter_prong_north", "hilt", (-1.5, 9.7, -2.0), (3.0, 1.15, 0.48), "metal"),
        CubeSpec("emitter_prong_south", "hilt", (-1.5, 9.7, 1.52), (3.0, 1.15, 0.48), "metal"),
        CubeSpec("emitter_prong_west", "hilt", (-2.0, 9.7, -1.5), (0.48, 1.15, 3.0), "metal"),
        CubeSpec("emitter_prong_east", "hilt", (1.52, 9.7, -1.5), (0.48, 1.15, 3.0), "metal"),
        CubeSpec("energy_blade", "blade", (-1.1, 10.0, -1.1), (2.2, 36.0, 2.2), "blade"),
        CubeSpec("blade_core", "blade", (-0.46, 10.04, -0.46), (0.92, 36.1, 0.92), "core"),
        CubeSpec("blade_tip", "blade", (-0.86, 45.9, -0.86), (1.72, 0.75, 1.72), "blade"),
    ))
    return tuple(specs)


class Layout:
    def __init__(self) -> None:
        self.x = 2
        self.y = 2
        self.row_height = 0
        self.mapping: dict[str, dict[str, dict[str, list[int]]]] = {}
        self.regions: dict[tuple[str, str], tuple[int, int, int, int]] = {}

    def allocate(self, spec: CubeSpec) -> dict[str, dict[str, list[int]]]:
        width, height, depth = (max(1, math.ceil(value)) for value in spec.size)
        dimensions = {
            "north": (width * TEXEL_DENSITY, height * TEXEL_DENSITY),
            "south": (width * TEXEL_DENSITY, height * TEXEL_DENSITY),
            "east": (depth * TEXEL_DENSITY, height * TEXEL_DENSITY),
            "west": (depth * TEXEL_DENSITY, height * TEXEL_DENSITY),
            "up": (width * TEXEL_DENSITY, depth * TEXEL_DENSITY),
            "down": (width * TEXEL_DENSITY, depth * TEXEL_DENSITY),
        }
        faces = {}
        for face, (face_width, face_height) in dimensions.items():
            left, top = self.allocate_region(face_width, face_height, spec.label)
            faces[face] = {"uv": [left, top], "uv_size": [face_width, face_height]}
            self.regions[(spec.label, face)] = (left, top, face_width, face_height)
        self.mapping[spec.label] = faces
        return faces

    def allocate_region(self, width: int, height: int, label: str) -> tuple[int, int]:
        if self.x + width + 2 > ATLAS_SIZE:
            self.x = 2
            self.y += self.row_height + 3
            self.row_height = 0
        if self.y + height + 2 > ATLAS_SIZE:
            raise ValueError(f"Lightsaber atlas overflow while packing {label}")
        left, top = self.x, self.y
        self.x += width + 3
        self.row_height = max(self.row_height, height)
        return left, top


def clamp(color: Color, amount: int) -> Color:
    return tuple(max(0, min(255, channel + amount)) for channel in color)


def mix(left: Color, right: Color, amount: float) -> Color:
    return tuple(round(a + (b - a) * amount) for a, b in zip(left, right))


def material_color(palette: Palette, material: str, frame: int) -> Color:
    if material == "core":
        return mix((255, 255, 255), palette.glow, frame * 0.025)
    if material == "blade":
        return mix(palette.blade, palette.glow, (0.12, 0.28, 0.4, 0.2)[frame])
    return getattr(palette, material)


def paint_region(
        image: Image.Image,
        bounds: tuple[int, int, int, int],
        spec: CubeSpec,
        face: str,
        palette: Palette,
        frame: int,
        glowmask: bool,
) -> None:
    left, top, width, height = bounds
    draw = ImageDraw.Draw(image)
    luminous = spec.material in {"blade", "core", "accent"}
    if glowmask and not luminous:
        return
    base = material_color(palette, spec.material, frame)
    if glowmask:
        base = clamp(base, 22 if spec.material != "core" else 0)
    seed = int(hashlib.sha256(f"{spec.label}:{face}".encode()).hexdigest()[:8], 16)
    for y in range(height):
        for x in range(width):
            edge = x in (0, width - 1) or y in (0, height - 1)
            grain = ((seed + x * 13 + y * 17) % 11) - 5
            amount = grain
            if edge and spec.material not in {"blade", "core", "accent"}:
                amount -= 28
            elif y == 1 and spec.material not in {"grip", "shadow"}:
                amount += 18
            if spec.material == "grip" and (x + y) % 4 == 0:
                amount -= 10
            color = clamp(base, amount)
            draw.point((left + x, top + y), fill=(*color, 255))
    if width > 4 and height > 4 and spec.material in {"metal", "light"}:
        draw.line((left + 2, top + 2, left + width - 3, top + 2), fill=(*clamp(base, 34), 255))
        draw.line((left + 2, top + height - 2, left + width - 3, top + height - 2),
                  fill=(*clamp(base, -30), 255))


def build_geometry(specs: tuple[CubeSpec, ...], layout: Layout) -> dict:
    bones = {
        "root": {"name": "root", "pivot": [0, 0, 0], "cubes": []},
        "hilt": {"name": "hilt", "parent": "root", "pivot": [0, 0, 0], "cubes": []},
        "blade": {"name": "blade", "parent": "root", "pivot": [0, 5, 0], "cubes": []},
    }
    for spec in specs:
        centered_origin = list(spec.origin)
        centered_origin[1] -= 5.0
        bones[spec.bone]["cubes"].append({
            "origin": centered_origin,
            "size": list(spec.size),
            "uv": layout.allocate(spec),
        })
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.galacticwars.item.lightsaber",
                "texture_width": ATLAS_SIZE,
                "texture_height": ATLAS_SIZE,
                "visible_bounds_width": 4,
                "visible_bounds_height": 7,
                "visible_bounds_offset": [0, 2.5, 0],
            },
            "bones": list(bones.values()),
        }],
    }


def write_variant(name: str, palette: Palette, specs: tuple[CubeSpec, ...], layout: Layout) -> None:
    texture = Image.new("RGBA", (ATLAS_SIZE, ATLAS_SIZE * FRAME_COUNT), (0, 0, 0, 0))
    glowmask = Image.new("RGBA", texture.size, (0, 0, 0, 0))
    for frame in range(FRAME_COUNT):
        frame_image = Image.new("RGBA", (ATLAS_SIZE, ATLAS_SIZE), (0, 0, 0, 0))
        frame_glow = Image.new("RGBA", frame_image.size, (0, 0, 0, 0))
        for spec in specs:
            for face in ("north", "south", "east", "west", "up", "down"):
                bounds = layout.regions[(spec.label, face)]
                paint_region(frame_image, bounds, spec, face, palette, frame, False)
                paint_region(frame_glow, bounds, spec, face, palette, frame, True)
        texture.alpha_composite(frame_image, (0, frame * ATLAS_SIZE))
        glowmask.alpha_composite(frame_glow, (0, frame * ATLAS_SIZE))

    texture_path = TEXTURE_ROOT / f"{name}.png"
    glow_path = TEXTURE_ROOT / f"{name}_glowmask.png"
    texture.save(texture_path)
    glowmask.save(glow_path)
    metadata = {"animation": {"frametime": 2, "interpolate": True}}
    for path in (texture_path, glow_path):
        path.with_suffix(".png.mcmeta").write_text(
            json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    write_icon(name, palette)


def write_icon(name: str, palette: Palette) -> None:
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(icon)
    # Long diagonal blade with a compact, readable segmented hilt.
    for x, y in ((2, 14), (3, 13), (4, 12), (5, 11)):
        draw.point((x, y), fill=(*palette.shadow, 255))
        if x + 1 < 16:
            draw.point((x + 1, y), fill=(*palette.metal, 255))
        if y - 1 >= 0:
            draw.point((x, y - 1), fill=(*palette.light, 255))
    draw.point((4, 13), fill=(*palette.grip, 255))
    draw.point((5, 12), fill=(*palette.accent, 255))
    for x, y in zip(range(6, 15), range(10, 1, -1)):
        draw.point((x, y), fill=(251, 254, 255, 255))
        if x - 1 >= 0:
            draw.point((x - 1, y), fill=(*palette.blade, 238))
        if y - 1 >= 0:
            draw.point((x, y - 1), fill=(*palette.glow, 210))
    icon.save(ITEM_TEXTURE_ROOT / f"{name}_lightsaber.png")


def write_item_files() -> None:
    display = {
        "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.55, 0.55, 0.55]},
        "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.55, 0.55, 0.55]},
        "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.46, 0.46, 0.46]},
        "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [-1.13, 3.2, 1.13], "scale": [0.46, 0.46, 0.46]},
        "gui": {"rotation": [18, 225, 0], "translation": [0, -5.4, 0], "scale": [0.31, 0.31, 0.31]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2.0, 0], "scale": [0.22, 0.22, 0.22]},
        "fixed": {"rotation": [0, 180, 38], "translation": [0, -4.0, 0], "scale": [0.36, 0.36, 0.36]},
    }
    (ITEM_MODEL_ROOT / "lightsaber_base.json").write_text(json.dumps({
        "parent": "builtin/entity",
        "ambientocclusion": False,
        "gui_light": "front",
        "display": display,
    }, indent=2) + "\n", encoding="utf-8")
    for name in PALETTES:
        item_id = f"{name}_lightsaber"
        (ITEM_MODEL_ROOT / f"{item_id}.json").write_text(json.dumps({
            "parent": "galacticwars:item/lightsaber_base",
        }, indent=2) + "\n", encoding="utf-8")
        (ITEM_DEFINITION_ROOT / f"{item_id}.json").write_text(json.dumps({
            "model": {
                "type": "minecraft:special",
                "base": f"galacticwars:item/{item_id}",
                "model": {"type": "geckolib:geckolib"},
            }
        }, indent=2) + "\n", encoding="utf-8")


def generate_all() -> None:
    for directory in (
            MODEL_PATH.parent, ANIMATION_PATH.parent, TEXTURE_ROOT,
            ITEM_TEXTURE_ROOT, ITEM_MODEL_ROOT, ITEM_DEFINITION_ROOT):
        directory.mkdir(parents=True, exist_ok=True)
    specs = cubes()
    layout = Layout()
    geometry = build_geometry(specs, layout)
    MODEL_PATH.write_text(json.dumps(geometry, indent=2) + "\n", encoding="utf-8")
    ANIMATION_PATH.write_text(json.dumps({
        "format_version": "1.8.0",
        "animations": {
            "animation.lightsaber.idle": {
                "loop": True,
                "animation_length": 1.0,
                "bones": {
                    "blade": {
                        "scale": {
                            "0.0": [1.0, 1.0, 1.0],
                            "0.5": [1.012, 1.0, 1.012],
                            "1.0": [1.0, 1.0, 1.0],
                        }
                    }
                },
            }
        },
    }, indent=2) + "\n", encoding="utf-8")
    for name, palette in PALETTES.items():
        write_variant(name, palette, specs, layout)
    write_item_files()
    print(f"Generated one GeckoLib lightsaber model and {len(PALETTES)} animated material sets")


if __name__ == "__main__":
    generate_all()
