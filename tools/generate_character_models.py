"""Generate original UV-safe GeckoLib recruit and equipped-armor assets.

The geometry and every painted box face come from the same deterministic source so a model
change cannot silently leave a texture pointing at an unrelated UV layout.
"""

from __future__ import annotations

import hashlib
import json
import math
import shutil
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/galacticwars"
ENTITY_MODELS = ASSETS / "geckolib/models/entity"
ENTITY_ANIMATIONS = ASSETS / "geckolib/animations/entity"
ENTITY_TEXTURES = ASSETS / "textures/entity"
ARMOR_MODELS = ASSETS / "geckolib/models/armor"
ARMOR_ANIMATIONS = ASSETS / "geckolib/animations/armor"
ARMOR_TEXTURES = ASSETS / "textures/armor"
ITEM_DEFINITIONS = ASSETS / "items"
ITEM_MODELS = ASSETS / "models/item"
ITEM_TEXTURES = ASSETS / "textures/item"
ATLAS_SIZE = 128
ARMOR_ATLAS_SIZE = 1024
ARMOR_TEXEL_DENSITY = 6


Color = tuple[int, int, int]


@dataclass(frozen=True)
class Palette:
    skin: Color
    shadow: Color
    dark: Color
    base: Color
    light: Color
    accent: Color
    cloth: Color


@dataclass(frozen=True)
class RecruitDesign:
    id: str
    style: str
    variant: str
    palette: Palette


RECRUITS = (
    RecruitDesign("clone_trooper", "plate", "clone", Palette(
        (187, 126, 87), (30, 35, 43), (18, 22, 29), (190, 198, 201),
        (239, 241, 237), (42, 91, 170), (44, 50, 58))),
    RecruitDesign("arc_trooper", "plate", "arc", Palette(
        (177, 113, 77), (27, 32, 40), (15, 21, 29), (194, 201, 204),
        (241, 242, 236), (29, 102, 202), (38, 44, 53))),
    RecruitDesign("jedi_knight", "robe", "jedi", Palette(
        (185, 124, 79), (78, 47, 29), (48, 31, 23), (160, 118, 68),
        (222, 198, 142), (87, 220, 110), (91, 61, 39))),
    RecruitDesign("b1_battle_droid", "droid_slim", "b1", Palette(
        (196, 163, 105), (87, 69, 45), (39, 37, 32), (151, 123, 78),
        (210, 180, 120), (232, 157, 48), (72, 65, 53))),
    RecruitDesign("b2_super_battle_droid", "droid_heavy", "b2", Palette(
        (91, 98, 108), (42, 47, 54), (20, 23, 28), (75, 83, 94),
        (133, 144, 153), (244, 146, 32), (45, 49, 56))),
    RecruitDesign("commando_droid", "droid_slim", "commando", Palette(
        (59, 65, 72), (29, 33, 39), (13, 16, 20), (45, 51, 58),
        (103, 113, 121), (212, 47, 52), (31, 35, 40))),
    RecruitDesign("mandalorian_warrior", "plate", "warrior", Palette(
        (180, 119, 76), (43, 53, 56), (24, 31, 34), (84, 112, 113),
        (170, 192, 190), (25, 158, 156), (48, 54, 56))),
    RecruitDesign("mandalorian_marksman", "plate", "marksman", Palette(
        (177, 116, 73), (66, 61, 51), (31, 30, 28), (143, 131, 101),
        (199, 187, 150), (216, 98, 30), (56, 52, 47))),
    RecruitDesign("mandalorian_heavy", "plate", "heavy", Palette(
        (177, 116, 73), (38, 53, 67), (20, 27, 34), (73, 99, 129),
        (135, 163, 188), (83, 207, 229), (38, 45, 54))),
    RecruitDesign("hutt_enforcer", "brute", "enforcer", Palette(
        (112, 123, 62), (62, 69, 38), (36, 34, 29), (91, 101, 52),
        (145, 155, 84), (180, 116, 49), (55, 48, 39))),
    RecruitDesign("bounty_hunter", "plate", "hunter", Palette(
        (169, 107, 67), (55, 50, 43), (28, 27, 25), (93, 91, 81),
        (157, 154, 137), (225, 145, 31), (65, 50, 36))),
    RecruitDesign("smuggler", "civilian", "smuggler", Palette(
        (194, 132, 83), (92, 59, 36), (48, 35, 27), (151, 117, 77),
        (210, 191, 145), (67, 142, 198), (134, 60, 31))),
    RecruitDesign("nightsister_acolyte", "robe", "acolyte", Palette(
        (199, 191, 183), (68, 32, 40), (20, 19, 23), (111, 35, 45),
        (160, 58, 72), (104, 225, 132), (48, 25, 34))),
    RecruitDesign("nightsister_archer", "robe", "archer", Palette(
        (199, 191, 183), (61, 29, 38), (18, 19, 22), (92, 33, 43),
        (143, 53, 67), (110, 227, 151), (43, 26, 34))),
    RecruitDesign("nightbrother_brute", "brute", "nightbrother", Palette(
        (165, 40, 36), (92, 29, 28), (29, 23, 22), (96, 48, 40),
        (164, 82, 59), (228, 91, 42), (51, 33, 30))),
    RecruitDesign("republic_civilian", "civilian", "republic", Palette(
        (184, 123, 81), (65, 72, 82), (30, 34, 42), (99, 116, 139),
        (173, 188, 201), (52, 105, 186), (72, 80, 94))),
    RecruitDesign("separatist_technician", "droid_slim", "technician", Palette(
        (170, 139, 88), (76, 69, 55), (34, 34, 31), (115, 106, 88),
        (179, 165, 132), (225, 151, 44), (62, 59, 51))),
    RecruitDesign("mandalorian_clansperson", "civilian", "clansperson", Palette(
        (180, 119, 76), (48, 58, 58), (28, 33, 34), (84, 108, 108),
        (156, 178, 176), (31, 151, 150), (68, 65, 58))),
    RecruitDesign("hutt_civilian", "civilian", "hutt", Palette(
        (167, 119, 77), (74, 54, 39), (39, 31, 25), (132, 93, 61),
        (190, 149, 99), (205, 126, 45), (92, 68, 51))),
    RecruitDesign("nightsister_civilian", "civilian", "nightsister", Palette(
        (194, 184, 177), (61, 32, 40), (23, 20, 25), (89, 42, 55),
        (149, 75, 91), (183, 48, 73), (50, 27, 38))),
)


ARMOR_PALETTES = {
    "republic_plastoid": Palette((0, 0, 0), (47, 54, 64), (18, 23, 31),
                                  (185, 195, 200), (240, 243, 239), (39, 88, 171), (45, 52, 62)),
    "separatist_alloy": Palette((0, 0, 0), (70, 61, 49), (31, 29, 26),
                                 (63, 57, 48), (117, 103, 82), (211, 126, 31), (48, 45, 40)),
    "mandalorian_alloy": Palette((0, 0, 0), (40, 57, 59), (21, 29, 32),
                                  (78, 111, 112), (166, 191, 188), (24, 158, 156), (45, 52, 54)),
    "nightsister_weave": Palette((0, 0, 0), (41, 18, 27), (12, 12, 16),
                                  (102, 27, 39), (151, 43, 52), (218, 34, 58), (24, 18, 24)),
    "beskar": Palette((0, 0, 0), (51, 62, 72), (24, 31, 38),
                       (124, 144, 155), (213, 225, 228), (63, 103, 132), (47, 55, 64)),
}


def clamp_color(color: Color, amount: int) -> Color:
    return tuple(max(0, min(255, channel + amount)) for channel in color)


class Atlas:
    def __init__(
            self,
            palette: Palette,
            salt: str,
            *,
            size: int = ATLAS_SIZE,
            texel_density: int = 1,
    ):
        self.palette = palette
        self.salt = salt
        self.size = size
        self.texel_density = texel_density
        self.image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        self.draw = ImageDraw.Draw(self.image)
        self.x = 1
        self.y = 1
        self.row_height = 0

    def allocate(
            self,
            size: tuple[float, float, float],
            label: str,
            material: str,
    ) -> list[int] | dict[str, dict[str, list[int]]]:
        width, height, depth = (max(1, math.ceil(value)) for value in size)
        if self.texel_density > 1:
            return self.allocate_faces((width, height, depth), label, material)
        footprint_width = depth * 2 + width * 2
        footprint_height = depth + height
        if self.x + footprint_width + 1 > self.size:
            self.x = 1
            self.y += self.row_height + 2
            self.row_height = 0
        if self.y + footprint_height + 1 > self.size:
            raise ValueError(f"{self.salt} atlas overflow while packing {label}")
        uv = [self.x, self.y]
        self.paint_box(uv, (width, height, depth), label, material)
        self.x += footprint_width + 2
        self.row_height = max(self.row_height, footprint_height)
        return uv

    def allocate_faces(
            self,
            size: tuple[int, int, int],
            label: str,
            material: str,
    ) -> dict[str, dict[str, list[int]]]:
        width, height, depth = size
        density = self.texel_density
        dimensions = {
            "north": (width * density, height * density),
            "south": (width * density, height * density),
            "east": (depth * density, height * density),
            "west": (depth * density, height * density),
            "up": (width * density, depth * density),
            "down": (width * density, depth * density),
        }
        mapping: dict[str, dict[str, list[int]]] = {}
        for face_name, (face_width, face_height) in dimensions.items():
            left, top = self.allocate_region(face_width, face_height, label)
            self.paint_high_density_face(
                (left, top, face_width, face_height), label, material, face_name)
            mapping[face_name] = {
                "uv": [left, top],
                "uv_size": [face_width, face_height],
            }
        return mapping

    def allocate_region(self, width: int, height: int, label: str) -> tuple[int, int]:
        padding = max(2, self.texel_density)
        if self.x + width + padding > self.size:
            self.x = padding
            self.y += self.row_height + padding
            self.row_height = 0
        if self.y + height + padding > self.size:
            raise ValueError(f"{self.salt} atlas overflow while packing {label}")
        position = self.x, self.y
        self.x += width + padding
        self.row_height = max(self.row_height, height)
        return position

    def paint_box(
            self,
            uv: list[int],
            size: tuple[int, int, int],
            label: str,
            material: str,
    ) -> None:
        u, v = uv
        width, height, depth = size
        faces = (
            ("top", (u + depth, v, width, depth)),
            ("bottom", (u + depth + width, v, width, depth)),
            ("left", (u, v + depth, depth, height)),
            ("front", (u + depth, v + depth, width, height)),
            ("right", (u + depth + width, v + depth, depth, height)),
            ("back", (u + depth * 2 + width, v + depth, width, height)),
        )
        base = getattr(self.palette, material)
        for face_name, (left, top, face_width, face_height) in faces:
            seed = int(hashlib.sha256(
                f"{self.salt}:{label}:{face_name}".encode("utf-8")).hexdigest()[:8], 16)
            for dy in range(face_height):
                for dx in range(face_width):
                    edge = dx == 0 or dy == 0 or dx == face_width - 1 or dy == face_height - 1
                    amount = -25 if edge else ((dx * 13 + dy * 7 + seed) % 13) - 6
                    if face_name == "top":
                        amount += 17
                    elif face_name in ("left", "bottom"):
                        amount -= 12
                    color = clamp_color(base, amount)
                    self.image.putpixel((left + dx, top + dy), (*color, 255))
            if face_width >= 4 and face_height >= 3:
                accent_period = 7 + seed % 5
                accent_y = top + 1 + seed % max(1, face_height - 2)
                for dx in range(1, face_width - 1):
                    if (dx + seed) % accent_period == 0 or material == "accent":
                        self.image.putpixel((left + dx, accent_y), (*clamp_color(base, 28), 255))

    def paint_high_density_face(
            self,
            rect: tuple[int, int, int, int],
            label: str,
            material: str,
            face_name: str,
    ) -> None:
        left, top, width, height = rect
        right = left + width - 1
        bottom = top + height - 1
        base = getattr(self.palette, material)
        seed = int(hashlib.sha256(
            f"{self.salt}:{label}:{face_name}:hd".encode("utf-8")).hexdigest()[:8], 16)
        family = self.salt.removeprefix("armor.")
        cloth_surface = material == "cloth" or family == "nightsister_weave" and (
            "hood" in label or "skirt" in label or "shell" in label)
        directional_light = {
            "up": 22,
            "down": -22,
            "north": 7,
            "south": -8,
            "east": -3,
            "west": 2,
        }[face_name]

        for dy in range(height):
            for dx in range(width):
                edge_distance = min(dx, dy, width - 1 - dx, height - 1 - dy)
                grain = ((seed + dx * 73 + dy * 151 + dx * dy * 11) % 17) - 8
                vertical = round(12 * (1 - dy / max(1, height - 1)))
                amount = directional_light + vertical + grain // (2 if cloth_surface else 4)
                if cloth_surface:
                    amount += 5 if (dx + dy + seed) % 7 == 0 else -3 if (dx - dy + seed) % 9 == 0 else 0
                elif edge_distance == 0:
                    amount -= 28
                elif edge_distance == 1:
                    amount += 14
                elif edge_distance == 2:
                    amount += 5
                self.image.putpixel((left + dx, top + dy), (*clamp_color(base, amount), 255))

        scale = self.texel_density
        dark = (*clamp_color(base, -52), 255)
        shadow = (*clamp_color(base, -27), 255)
        highlight = (*clamp_color(base, 27), 255)
        accent = (*self.palette.accent, 255)
        accent_light = (*clamp_color(self.palette.accent, 45), 255)
        plate_surface = not cloth_surface and material not in ("dark", "shadow")

        if cloth_surface:
            self.paint_weave(rect, seed)
        elif plate_surface and width >= 4 * scale and height >= 3 * scale:
            inset = max(2, scale)
            self.draw.rectangle(
                (left + inset, top + inset, right - inset, bottom - inset),
                outline=shadow,
                width=max(1, scale // 2),
            )
            self.draw.line(
                (left + inset + 1, top + inset + 1, right - inset - 1, top + inset + 1),
                fill=highlight,
                width=max(1, scale // 2),
            )
            hardware_labels = (
                "helmet_shell", "breastplate", "backplate", "pauldron",
                "gauntlet", "thigh", "shin", "heavy_collar", "power_cell",
            )
            if width >= 20 and height >= 16 and any(token in label for token in hardware_labels):
                fastener = max(1, scale // 3)
                for fx, fy in (
                        (left + inset + scale, top + inset + scale),
                        (right - inset - scale, bottom - inset - scale)):
                    self.draw.ellipse(
                        (fx - fastener, fy - fastener, fx + fastener, fy + fastener),
                        fill=dark,
                    )
                    self.draw.point((fx, fy), fill=highlight)

        if face_name == "north":
            self.paint_front_motif(rect, label, family, dark, shadow, highlight, accent, accent_light)
        elif face_name in ("east", "west") and height >= 5 * scale:
            seam_x = left + width // 2
            self.draw.line((seam_x, top + scale, seam_x, bottom - scale), fill=shadow, width=max(1, scale // 2))
        elif face_name == "south" and any(token in label for token in ("backplate", "compact_pack", "shell")):
            self.paint_back_motif(rect, dark, highlight, accent)

        if plate_surface:
            self.paint_wear(rect, seed, family, highlight, shadow)

    def paint_weave(self, rect: tuple[int, int, int, int], seed: int) -> None:
        left, top, width, height = rect
        right = left + width - 1
        bottom = top + height - 1
        thread_dark = (*clamp_color(self.palette.cloth, -18), 255)
        thread_light = (*clamp_color(self.palette.cloth, 20), 255)
        magic = (*clamp_color(self.palette.accent, 8), 255)
        step = max(3, self.texel_density + 1)
        for offset in range(-height, width, step):
            x0 = left + max(0, offset)
            y0 = top + max(0, -offset)
            length = min(right - x0, bottom - y0)
            if length > 0:
                self.draw.line((x0, y0, x0 + length, y0 + length), fill=thread_light)
        for offset in range(0, width + height, step):
            x0 = left + max(0, offset - height)
            y0 = bottom - min(height - 1, offset)
            length = min(right - x0, y0 - top)
            if length > 0:
                self.draw.line((x0, y0, x0 + length, y0 - length), fill=thread_dark)
        if width >= 12 and height >= 12:
            phase = seed % max(step * 2, 1)
            for y in range(top + step + phase // 2, bottom - step, step * 3):
                self.draw.line((left + step, y, right - step, y), fill=magic, width=1)

    def paint_front_motif(
            self,
            rect: tuple[int, int, int, int],
            label: str,
            family: str,
            dark: tuple[int, int, int, int],
            shadow: tuple[int, int, int, int],
            highlight: tuple[int, int, int, int],
            accent: tuple[int, int, int, int],
            accent_light: tuple[int, int, int, int],
    ) -> None:
        left, top, width, height = rect
        right = left + width - 1
        bottom = top + height - 1
        cx = left + width // 2
        scale = self.texel_density
        line = max(1, scale // 2)

        if "visor" in label:
            self.draw.rectangle((left + 1, top + 1, right - 1, bottom - 1), fill=dark)
            self.draw.line((left + scale, top + scale, right - scale, top + scale), fill=accent_light, width=line)
            self.draw.line((left + scale, bottom - scale, right - scale, bottom - scale), fill=accent, width=line)
            return
        if "face_guard" in label:
            for x in range(left + scale, right - scale, max(2, scale)):
                self.draw.line((x, top + scale, x, bottom - scale), fill=shadow, width=1)
            return
        if "helmet_shell" in label or "woven_hood" in label:
            self.draw.line((cx, top + scale, cx, bottom - scale), fill=shadow, width=line)
            self.draw.line((cx + line, top + scale, cx + line, bottom - scale), fill=highlight, width=1)
            vent_y = top + round(height * 0.72)
            for side in (-1, 1):
                start = cx + side * round(width * 0.18)
                end = cx + side * round(width * 0.38)
                self.draw.line((start, vent_y, end, vent_y), fill=dark, width=line)
                self.draw.line((start, vent_y + scale, end, vent_y + scale), fill=dark, width=1)
            if family == "republic_plastoid":
                stripe = max(scale, width // 8)
                self.draw.rectangle((cx - stripe // 2, top + scale, cx + stripe // 2, top + height // 3), fill=accent)
            elif family == "mandalorian_alloy":
                self.draw.line((left + scale, bottom - scale, right - scale, top + scale), fill=accent, width=line)
            elif family == "nightsister_weave":
                self.draw.line((left + scale, top + scale, cx, bottom - scale), fill=accent, width=line)
                self.draw.line((right - scale, top + scale, cx, bottom - scale), fill=accent_light, width=line)
            return
        if "breastplate" in label:
            shoulder_y = top + scale * 2
            sternum_y = top + round(height * 0.68)
            self.draw.line((left + scale, shoulder_y, cx, sternum_y, right - scale, shoulder_y), fill=shadow, width=line)
            self.draw.line((cx, top + scale, cx, bottom - scale), fill=dark, width=line)
            self.draw.line((left + scale, shoulder_y - 1, cx, sternum_y - 1), fill=highlight, width=1)
            self.draw.line((right - scale, shoulder_y - 1, cx, sternum_y - 1), fill=highlight, width=1)
            if family == "republic_plastoid":
                self.draw.rectangle((cx - scale, top + scale, cx + scale, top + scale * 2), fill=accent)
            elif family == "separatist_alloy":
                self.draw.rectangle((cx - scale, sternum_y, cx + scale, bottom - scale), fill=accent)
            elif family == "mandalorian_alloy":
                self.draw.line((left + scale, bottom - scale, cx, sternum_y), fill=accent, width=line)
            elif family == "nightsister_weave":
                self.draw.line((left + scale, top + scale, right - scale, bottom - scale), fill=accent, width=line)
                self.draw.line((right - scale, top + scale, left + scale, bottom - scale), fill=accent, width=line)
            return
        if "abdomen" in label or "power_cell" in label:
            for y in range(top + scale, bottom - scale, max(scale * 2, 3)):
                self.draw.line((left + scale, y, right - scale, y), fill=shadow, width=line)
                self.draw.line((left + scale, y - 1, right - scale, y - 1), fill=highlight, width=1)
            if "power_cell" in label:
                self.draw.rectangle((cx - scale, top + scale, cx + scale, bottom - scale), fill=accent)
                self.draw.line((cx, top + scale, cx, bottom - scale), fill=accent_light, width=line)
            return
        if "pauldron" in label or "collar" in label:
            self.draw.line((left + scale, bottom - scale, right - scale, top + scale), fill=accent, width=max(line, scale))
            self.draw.line((left + scale, bottom - scale - line, right - scale, top + scale - line), fill=accent_light, width=1)
            return
        if "gauntlet" in label:
            for y in range(top + scale * 2, bottom - scale, max(scale * 2, 3)):
                self.draw.line((left + scale, y, right - scale, y), fill=shadow, width=line)
            self.draw.rectangle((left + scale, top + scale, left + scale * 2, top + scale * 2), fill=accent)
            return
        if "thigh" in label or "shin" in label:
            panel = max(scale, width // 5)
            self.draw.rectangle((cx - panel, top + scale, cx + panel, bottom - scale), outline=shadow, width=line)
            if family in ("republic_plastoid", "mandalorian_alloy"):
                self.draw.line((cx, top + scale * 2, cx, bottom - scale * 2), fill=accent, width=line)
            return
        if "knee" in label:
            self.draw.polygon(
                ((cx, top + scale), (right - scale, top + height // 2),
                 (cx, bottom - scale), (left + scale, top + height // 2)),
                outline=highlight,
            )
            return
        if "boot" in label:
            toe_y = top + round(height * 0.62)
            self.draw.line((left + scale, toe_y, right - scale, toe_y), fill=shadow, width=line)
            self.draw.line((left + scale, toe_y - 1, right - scale, toe_y - 1), fill=highlight, width=1)
            return
        if "belt" in label:
            segment = max(scale * 2, width // 6)
            inset_y = max(1, min(scale, (height - 1) // 3))
            for x in range(left + scale, right - scale, segment):
                x1 = min(right - scale, x + segment - scale)
                if x1 >= x and bottom - inset_y >= top + inset_y:
                    self.draw.rectangle(
                        (x, top + inset_y, x1, bottom - inset_y),
                        outline=highlight,
                    )

    def paint_back_motif(
            self,
            rect: tuple[int, int, int, int],
            dark: tuple[int, int, int, int],
            highlight: tuple[int, int, int, int],
            accent: tuple[int, int, int, int],
    ) -> None:
        left, top, width, height = rect
        right = left + width - 1
        bottom = top + height - 1
        scale = self.texel_density
        cx = left + width // 2
        self.draw.line((cx, top + scale, cx, bottom - scale), fill=dark, width=max(1, scale // 2))
        for y in range(top + scale * 2, bottom - scale, max(3, scale * 2)):
            self.draw.line((left + scale, y, right - scale, y), fill=highlight)
        self.draw.rectangle((cx - scale, top + scale, cx + scale, top + scale * 2), fill=accent)

    def paint_wear(
            self,
            rect: tuple[int, int, int, int],
            seed: int,
            family: str,
            highlight: tuple[int, int, int, int],
            shadow: tuple[int, int, int, int],
    ) -> None:
        left, top, width, height = rect
        count = width * height // 800
        for index in range(min(6, count)):
            x = left + 2 + (seed // (index + 1) + index * 17) % max(1, width - 4)
            y = top + 2 + (seed // (index + 3) + index * 29) % max(1, height - 4)
            length = 1 + (seed + index * 7) % max(2, min(6, width // 4 + 1))
            color = highlight if (index + seed) % 3 else shadow
            self.draw.line((x, y, min(left + width - 2, x + length), max(top + 1, y - 1)), fill=color)
        if family == "beskar" and width >= 16 and height >= 12:
            for y in range(top + 4, top + height - 3, max(6, self.texel_density * 3)):
                tone = highlight if (y + seed) % 2 else shadow
                x0 = left + 3 + (seed + y) % max(1, width // 4)
                x1 = min(left + width - 4, x0 + max(4, width // 3))
                self.draw.line((x0, y, x1, y), fill=tone)


class ModelBuilder:
    def __init__(
            self,
            identifier: str,
            palette: Palette,
            *,
            atlas_size: int = ATLAS_SIZE,
            texel_density: int = 1,
    ):
        self.identifier = identifier
        self.atlas = Atlas(
            palette,
            identifier,
            size=atlas_size,
            texel_density=texel_density,
        )
        self.bones: dict[str, dict] = {}

    def bone(self, name: str, pivot: list[float], parent: str | None = None) -> None:
        bone = {"name": name, "pivot": pivot, "cubes": []}
        if parent is not None:
            bone["parent"] = parent
        self.bones[name] = bone

    def cube(
            self,
            bone: str,
            label: str,
            origin: list[float],
            size: list[float],
            material: str,
            *,
            inflate: float | None = None,
            rotation: list[float] | None = None,
            pivot: list[float] | None = None,
            mirror: bool = False,
    ) -> None:
        cube = {"origin": origin, "size": size, "uv": self.atlas.allocate(tuple(size), label, material)}
        if inflate is not None:
            cube["inflate"] = inflate
        if rotation is not None:
            cube["rotation"] = rotation
        if pivot is not None:
            cube["pivot"] = pivot
        if mirror:
            cube["mirror"] = True
        self.bones[bone]["cubes"].append(cube)

    def write(self, model_path: Path, texture_path: Path) -> None:
        model_path.parent.mkdir(parents=True, exist_ok=True)
        texture_path.parent.mkdir(parents=True, exist_ok=True)
        document = {
            "format_version": "1.12.0",
            "minecraft:geometry": [{
                "description": {
                    "identifier": f"geometry.galacticwars.{self.identifier}",
                    "texture_width": self.atlas.size,
                    "texture_height": self.atlas.size,
                    "visible_bounds_width": 3,
                    "visible_bounds_height": 4,
                    "visible_bounds_offset": [0, 1.5, 0],
                },
                "bones": list(self.bones.values()),
            }],
        }
        model_path.write_text(json.dumps(document, indent=2) + "\n", encoding="utf-8")
        self.atlas.image.save(texture_path)


def humanoid_bones(builder: ModelBuilder) -> None:
    builder.bone("head", [0, 24, 0])
    builder.bone("body", [0, 24, 0])
    builder.bone("right_arm", [-5, 22, 0])
    builder.bone("left_arm", [5, 22, 0])
    builder.bone("right_leg", [-1.9, 12, 0])
    builder.bone("left_leg", [1.9, 12, 0])
    builder.bone("RightHandItem", [-6, 12, 0], "right_arm")
    builder.bone("LeftHandItem", [6, 12, 0], "left_arm")


def basic_humanoid(builder: ModelBuilder, head_material: str = "skin", body_material: str = "cloth") -> None:
    builder.cube("head", "face", [-4, 24, -4], [8, 8, 8], head_material)
    builder.cube("body", "tunic", [-4, 12, -2], [8, 12, 4], body_material)
    builder.cube("right_arm", "right_sleeve", [-8, 12, -2], [4, 12, 4], body_material)
    builder.cube("left_arm", "left_sleeve", [4, 12, -2], [4, 12, 4], body_material, mirror=True)
    builder.cube("right_leg", "right_trouser", [-4, 0, -2], [4, 12, 4], "dark")
    builder.cube("left_leg", "left_trouser", [0, 0, -2], [4, 12, 4], "dark", mirror=True)


def add_human_face(builder: ModelBuilder) -> None:
    """Add readable facial features without relying on a flat skin template."""
    builder.cube("head", "right_eye", [-2.8, 28.0, -4.24], [1.35, 0.8, 0.28], "dark")
    builder.cube("head", "left_eye", [1.45, 28.0, -4.24], [1.35, 0.8, 0.28], "dark")
    builder.cube("head", "nose", [-0.55, 26.55, -4.34], [1.1, 1.45, 0.38], "skin")
    builder.cube("head", "mouth", [-1.35, 25.35, -4.25], [2.7, 0.55, 0.26], "shadow")


def add_hair_cap(builder: ModelBuilder, prefix: str, material: str = "shadow") -> None:
    """Build hair around the face instead of covering it with an enclosing cube."""
    builder.cube("head", f"{prefix}_top", [-4.15, 30.55, -4.15], [8.3, 1.6, 8.3], material)
    builder.cube("head", f"{prefix}_back", [-4.15, 24.0, 3.45], [8.3, 6.8, 0.7], material)
    builder.cube("head", f"{prefix}_right", [-4.15, 24.0, -4.15], [0.7, 6.8, 8.3], material)
    builder.cube("head", f"{prefix}_left", [3.45, 24.0, -4.15], [0.7, 6.8, 8.3], material)
    builder.cube("head", f"{prefix}_fringe", [-4.0, 29.55, -4.28], [8.0, 1.15, 0.45], material)


def add_open_hood(builder: ModelBuilder, prefix: str, material: str = "cloth") -> None:
    """Create a deep hood with an open face and a visible inner shadow."""
    builder.cube("head", f"{prefix}_crown", [-4.55, 30.45, -4.55], [9.1, 2.3, 9.1], material)
    builder.cube("head", f"{prefix}_back", [-4.55, 23.8, 3.45], [9.1, 7.0, 1.1], material)
    builder.cube("head", f"{prefix}_right", [-4.55, 23.8, -4.55], [1.25, 7.0, 8.4], material)
    builder.cube("head", f"{prefix}_left", [3.3, 23.8, -4.55], [1.25, 7.0, 8.4], material)
    builder.cube("head", f"{prefix}_brow", [-3.35, 29.75, -4.65], [6.7, 0.85, 0.65], "shadow")


def add_plate_details(builder: ModelBuilder, variant: str) -> None:
    builder.cube("head", "helmet_shell", [-4, 24, -4], [8, 8, 8], "light", inflate=0.55)
    builder.cube("head", "visor", [-3.6, 27, -4.7], [7.2, 1.4, 0.8], "accent")
    builder.cube("head", "jaw_guard", [-3.2, 24.2, -4.65], [6.4, 2.1, 0.75], "base")
    builder.cube("body", "breastplate", [-4.25, 17.5, -2.55], [8.5, 5.7, 1.2], "light")
    builder.cube("body", "abdomen", [-3.7, 13.2, -2.45], [7.4, 4.0, 0.9], "shadow")
    builder.cube("body", "backplate", [-4.2, 15.0, 1.75], [8.4, 7.7, 1.1], "base")
    builder.cube("body", "belt", [-4.5, 11.7, -2.6], [9, 1.7, 5.2], "dark")
    for side, bone, x in (("right", "right_arm", -8.65), ("left", "left_arm", 3.65)):
        builder.cube(bone, f"{side}_pauldron", [x, 18.4, -2.6], [5, 4.6, 5.2], "base", inflate=0.18)
        builder.cube(bone, f"{side}_gauntlet", [x + 0.45, 12.1, -2.35], [4.1, 4.8, 4.7], "shadow")
    for side, bone, x in (("right", "right_leg", -4.15), ("left", "left_leg", 0.15)):
        builder.cube(bone, f"{side}_thigh", [x, 6.3, -2.35], [4, 5.2, 4.7], "base")
        builder.cube(bone, f"{side}_knee", [x - 0.1, 4.4, -2.8], [4.2, 2.2, 1.2], "accent")
        builder.cube(bone, f"{side}_shin", [x, 0.3, -2.45], [4, 4.1, 4.9], "light")
    if variant in ("arc", "marksman", "hunter"):
        builder.cube("head", "rangefinder_stem", [4.35, 27.0, -0.7], [0.8, 5.0, 1.2], "accent")
        builder.cube("head", "rangefinder_sensor", [3.8, 31.0, -3.8], [1.8, 1.2, 3.4], "dark")
    if variant == "arc":
        builder.cube("body", "command_pauldron", [-6.7, 18.0, -2.8], [5.2, 5.2, 5.6], "accent")
        builder.cube("body", "right_kama", [-4.2, 7.5, -2.4], [3.6, 4.5, 4.8], "cloth")
        builder.cube("body", "left_kama", [0.6, 7.5, -2.4], [3.6, 4.5, 4.8], "cloth")
    if variant in ("warrior", "marksman", "heavy", "hunter"):
        builder.cube("head", "vertical_visor", [-0.75, 24.3, -4.75], [1.5, 4.2, 0.85], "accent")
        builder.cube("body", "jetpack", [-2.8, 15.0, 2.0], [5.6, 7.4, 2.2], "dark")
    if variant == "heavy":
        builder.cube("body", "heavy_collar", [-5.0, 20.7, -2.8], [10, 2.7, 5.6], "light")
        builder.cube("right_arm", "right_heavy_shoulder", [-9.7, 17.8, -3.2], [6.4, 5.8, 6.4], "light")
        builder.cube("left_arm", "left_heavy_shoulder", [3.3, 17.8, -3.2], [6.4, 5.8, 6.4], "light")


def add_robe_details(builder: ModelBuilder, variant: str) -> None:
    if variant in ("acolyte", "archer"):
        add_open_hood(builder, "ritual_hood")
    else:
        add_hair_cap(builder, "hair", "dark")
    builder.cube("body", "cross_tunic", [-4.25, 15.5, -2.45], [8.5, 7.8, 4.9], "light")
    builder.cube("body", "sash", [-4.45, 12.1, -2.55], [8.9, 2.0, 5.1], "accent")
    builder.cube("body", "robe_skirt", [-4.3, 7.4, -2.4], [8.6, 4.8, 4.8], "base")
    builder.cube("right_leg", "right_boot", [-4.15, 0, -2.2], [4.1, 5.5, 4.4], "shadow")
    builder.cube("left_leg", "left_boot", [0.05, 0, -2.2], [4.1, 5.5, 4.4], "shadow", mirror=True)
    if variant in ("acolyte", "archer"):
        builder.cube("body", "right_talisman", [-3.2, 13.5, -2.7], [1.2, 7.5, 0.8], "accent")
        builder.cube("body", "left_talisman", [2.0, 13.5, -2.7], [1.2, 7.5, 0.8], "accent")
    if variant == "archer":
        builder.cube("body", "quiver", [2.5, 15.0, 1.8], [2.6, 8.5, 2.6], "shadow", rotation=[0, 0, -12], pivot=[3.5, 18, 2])


def add_civilian_details(builder: ModelBuilder, variant: str) -> None:
    add_hair_cap(builder, "hair_or_cap")
    builder.cube("body", "jacket", [-4.2, 14.0, -2.35], [8.4, 9.8, 4.7], "base", inflate=0.12)
    builder.cube("body", "utility_belt", [-4.4, 11.8, -2.45], [8.8, 1.8, 4.9], "dark")
    builder.cube("body", "belt_pouch", [2.6, 11.4, -2.85], [2.0, 2.3, 1.1], "accent")
    builder.cube("right_leg", "right_boot", [-4.1, 0, -2.2], [4, 4.5, 4.4], "shadow")
    builder.cube("left_leg", "left_boot", [0.1, 0, -2.2], [4, 4.5, 4.4], "shadow", mirror=True)
    if variant == "republic":
        builder.cube("body", "republic_vest", [-3.5, 15.5, -2.7], [7, 7.2, 1.0], "light")
    elif variant == "clansperson":
        builder.cube("body", "clan_shoulder", [-6.1, 18.2, -2.6], [3.8, 4.6, 5.2], "light")
        builder.cube("right_arm", "work_bracer", [-8.25, 12.2, -2.3], [4.5, 4.8, 4.6], "accent")
    elif variant == "hutt":
        builder.cube("body", "merchant_sash", [-4.5, 14.0, -2.75], [9, 2.0, 5.5], "accent", rotation=[0, 0, -12], pivot=[0, 15, 0])
    elif variant == "nightsister":
        add_open_hood(builder, "civilian_hood")
        builder.cube("body", "woven_apron", [-3, 8, -2.65], [6, 6, 1.0], "accent")


def add_droid(builder: ModelBuilder, style: str, variant: str) -> None:
    if style == "droid_heavy":
        builder.cube("head", "sensor_head", [-4, 25, -4], [8, 7, 8], "base")
        builder.cube("head", "sensor_bar", [-3.5, 28.0, -4.6], [7, 1.2, 0.8], "accent")
        builder.cube("body", "heavy_torso", [-5.3, 12.5, -3], [10.6, 11.2, 6], "base")
        builder.cube("body", "reactor", [-2.8, 15.5, -3.6], [5.6, 5.5, 1.0], "accent")
        for side, bone, x in (("right", "right_arm", -9.2), ("left", "left_arm", 3.2)):
            builder.cube(bone, f"{side}_heavy_arm", [x, 11.5, -3], [6, 12.5, 6], "base")
            builder.cube(bone, f"{side}_wrist", [x + 0.7, 10.5, -2.5], [4.6, 3.2, 5], "shadow")
        for side, bone, x in (("right", "right_leg", -4.4), ("left", "left_leg", 0.4)):
            builder.cube(bone, f"{side}_heavy_leg", [x, 0, -2.5], [4.4, 12.5, 5], "base")
            builder.cube(bone, f"{side}_foot", [x - 0.2, -0.2, -3.4], [4.8, 2.5, 6.8], "dark")
        return
    builder.cube("head", "droid_head", [-3.8, 26, -4.4], [7.6, 5.5, 8.8], "base")
    builder.cube("head", "optic_bar", [-3.2, 28.2, -5.0], [6.4, 1.0, 0.8], "accent")
    builder.cube("head", "neck", [-1.2, 23.2, -1.2], [2.4, 3.0, 2.4], "dark")
    builder.cube("body", "droid_chest", [-3.5, 14, -2.2], [7, 9.5, 4.4], "base")
    builder.cube("body", "droid_core", [-2, 16, -2.8], [4, 4.5, 1.0], "accent")
    builder.cube("body", "droid_waist", [-2, 11.5, -1.6], [4, 2.5, 3.2], "dark")
    for side, bone, x in (("right", "right_arm", -7.1), ("left", "left_arm", 4.1)):
        builder.cube(bone, f"{side}_upper_arm", [x, 17.3, -1.5], [3, 6.2, 3], "base")
        builder.cube(bone, f"{side}_forearm", [x + 0.35, 11.5, -1.35], [2.3, 5.8, 2.7], "shadow")
    for side, bone, x in (("right", "right_leg", -3.4), ("left", "left_leg", 0.9)):
        builder.cube(bone, f"{side}_thigh_rod", [x, 6.0, -1.4], [2.5, 6, 2.8], "base")
        builder.cube(bone, f"{side}_shin_rod", [x + 0.2, 0, -1.2], [2.1, 6, 2.4], "shadow")
        builder.cube(bone, f"{side}_droid_foot", [x - 0.2, -0.2, -2.7], [3.0, 1.8, 5.4], "dark")
    if variant == "commando":
        builder.cube("head", "commando_crest", [-1.0, 31.0, -2.5], [2.0, 1.5, 5], "accent")
        builder.cube("body", "commando_plate", [-3.8, 17, -2.7], [7.6, 5.5, 1.0], "light")
    elif variant == "technician":
        builder.cube("body", "tool_pack", [-3.0, 14.0, 2.0], [6, 7, 2.6], "accent")
        builder.cube("head", "technician_antenna", [3.4, 29, -0.5], [0.8, 4.0, 1.0], "accent")


def add_brute(builder: ModelBuilder, variant: str) -> None:
    builder.cube("head", "brute_head", [-4.5, 23.7, -4.2], [9, 8.5, 8.4], "skin")
    add_human_face(builder)
    builder.cube("head", "brow", [-4.0, 28.0, -4.8], [8, 1.5, 0.9], "shadow")
    builder.cube("body", "brute_torso", [-5, 11.5, -2.8], [10, 12.5, 5.6], "base")
    for side, bone, x in (("right", "right_arm", -9.4), ("left", "left_arm", 3.4)):
        builder.cube(bone, f"{side}_brute_arm", [x, 11, -2.8], [6, 13, 5.6], "skin")
        builder.cube(bone, f"{side}_bracer", [x + 0.4, 11.3, -3.0], [5.2, 4.0, 6], "shadow")
    for side, bone, x in (("right", "right_leg", -4.5), ("left", "left_leg", 0.2)):
        builder.cube(bone, f"{side}_brute_leg", [x, 0, -2.5], [4.5, 12, 5], "cloth")
        builder.cube(bone, f"{side}_boot", [x - 0.2, -0.2, -3.0], [4.9, 4.2, 6], "dark")
    builder.cube("body", "chest_harness", [-5.2, 17.3, -3.2], [10.4, 2.0, 6.4], "accent")
    if variant == "nightbrother":
        builder.cube("head", "horn_right", [-3.5, 31.2, -1.5], [1.5, 2.2, 2.0], "light")
        builder.cube("head", "horn_left", [2.0, 31.2, -1.5], [1.5, 2.2, 2.0], "light")


def build_recruit(design: RecruitDesign) -> None:
    builder = ModelBuilder(design.id, design.palette)
    humanoid_bones(builder)
    if design.style == "plate":
        basic_humanoid(builder, "dark", "dark")
        add_plate_details(builder, design.variant)
    elif design.style == "robe":
        basic_humanoid(builder)
        add_human_face(builder)
        add_robe_details(builder, design.variant)
    elif design.style == "civilian":
        basic_humanoid(builder)
        add_human_face(builder)
        add_civilian_details(builder, design.variant)
    elif design.style.startswith("droid"):
        add_droid(builder, design.style, design.variant)
    elif design.style == "brute":
        add_brute(builder, design.variant)
    else:
        raise ValueError(f"Unknown recruit style: {design.style}")
    builder.write(
        ENTITY_MODELS / f"{design.id}.geo.json",
        ENTITY_TEXTURES / f"{design.id}.png",
    )
    write_spawn_egg(design)


def armor_bones(builder: ModelBuilder) -> None:
    # GeckoLib's armor template uses non-rendering biped anchors. The armor
    # bones must be children of these anchors so Bedrock-space cube origins
    # resolve around the vanilla player part instead of around the model root.
    builder.bone("bipedHead", [0, 24, 0])
    builder.bone("armorHead", [0, 24, 0], "bipedHead")
    builder.bone("bipedBody", [0, 24, 0])
    builder.bone("armorBody", [0, 24, 0], "bipedBody")
    builder.bone("bipedRightArm", [-5, 22, 0])
    builder.bone("armorRightArm", [-5, 22, 0], "bipedRightArm")
    builder.bone("bipedLeftArm", [5, 22, 0])
    builder.bone("armorLeftArm", [5, 22, 0], "bipedLeftArm")
    builder.bone("bipedRightLeg", [-1.9, 12, 0])
    builder.bone("armorRightLeg", [-1.9, 12, 0], "bipedRightLeg")
    builder.bone("armorRightBoot", [-1.9, 12, 0], "bipedRightLeg")
    builder.bone("bipedLeftLeg", [1.9, 12, 0])
    builder.bone("armorLeftLeg", [1.9, 12, 0], "bipedLeftLeg")
    builder.bone("armorLeftBoot", [1.9, 12, 0], "bipedLeftLeg")


def add_armor_layering(
        builder: ModelBuilder,
        primary_plate: str,
        secondary_plate: str,
) -> None:
    """Break the vanilla cuboid silhouette into readable overlapping armor plates."""
    builder.cube(
        "armorHead", "helmet_brow", [-3.8, 28.65, -5.05], [7.6, 0.75, 0.65],
        primary_plate)
    builder.cube(
        "armorHead", "right_cheek_plate", [-3.55, 24.55, -4.98], [2.5, 2.75, 0.62],
        primary_plate, rotation=[0, 0, 7], pivot=[-2.2, 25.8, -4.7])
    builder.cube(
        "armorHead", "left_cheek_plate", [1.05, 24.55, -4.98], [2.5, 2.75, 0.62],
        primary_plate, rotation=[0, 0, -7], pivot=[2.2, 25.8, -4.7])
    builder.cube(
        "armorBody", "right_pectoral", [-4.15, 18.0, -3.08], [3.85, 4.25, 0.58],
        primary_plate, rotation=[0, 0, -6], pivot=[-2.2, 20.2, -2.8])
    builder.cube(
        "armorBody", "left_pectoral", [0.3, 18.0, -3.08], [3.85, 4.25, 0.58],
        primary_plate, rotation=[0, 0, 6], pivot=[2.2, 20.2, -2.8])
    builder.cube(
        "armorBody", "sternum_insert", [-0.7, 17.1, -3.2], [1.4, 4.9, 0.72],
        "accent")
    builder.cube(
        "armorBody", "codpiece", [-1.8, 9.45, -2.75], [3.6, 2.55, 0.85],
        secondary_plate)
    builder.cube(
        "armorRightArm", "right_elbow_guard", [-8.35, 15.4, -3.0], [4.7, 2.0, 1.05],
        primary_plate)
    builder.cube(
        "armorLeftArm", "left_elbow_guard", [3.65, 15.4, -3.0], [4.7, 2.0, 1.05],
        primary_plate)


def build_armor(family: str, palette: Palette) -> None:
    builder = ModelBuilder(
        f"armor.{family}",
        palette,
        atlas_size=ARMOR_ATLAS_SIZE,
        texel_density=ARMOR_TEXEL_DENSITY,
    )
    armor_bones(builder)
    primary_plate = {
        "separatist_alloy": "base",
        "mandalorian_alloy": "base",
        "nightsister_weave": "base",
    }.get(family, "light")
    secondary_plate = "dark" if family == "nightsister_weave" else "base"
    builder.cube("armorHead", "helmet_shell", [-4, 24, -4], [8, 8, 8], "base", inflate=0.72)
    builder.cube("armorHead", "helmet_crown", [-3.5, 31.6, -3.5], [7, 1.2, 7], "light")
    builder.cube("armorHead", "visor", [-3.55, 27, -4.85], [7.1, 1.45, 0.85], "accent")
    builder.cube("armorHead", "face_guard", [-3.0, 24.2, -4.75], [6, 2.6, 0.8], "shadow")
    builder.cube("armorBody", "chest_shell", [-4, 12, -2], [8, 12, 4], "dark", inflate=0.36)
    builder.cube("armorBody", "breastplate", [-4.35, 17.3, -2.65], [8.7, 6.0, 1.3], primary_plate)
    builder.cube("armorBody", "abdomen_plate", [-3.75, 13.1, -2.55], [7.5, 4.0, 1.0], secondary_plate)
    builder.cube("armorBody", "backplate", [-4.3, 14.5, 1.8], [8.6, 8.4, 1.1], primary_plate)
    builder.cube("armorBody", "belt", [-4.6, 11.5, -2.6], [9.2, 2.0, 5.2], "shadow")
    for side, bone, x in (("right", "armorRightArm", -8.0), ("left", "armorLeftArm", 4.0)):
        builder.cube(bone, f"{side}_arm_shell", [x, 12, -2], [4, 12, 4], "dark", inflate=0.38)
        builder.cube(bone, f"{side}_pauldron", [x - 0.55, 18.4, -2.65], [5.1, 4.8, 5.3], primary_plate)
        builder.cube(bone, f"{side}_gauntlet", [x - 0.2, 12.1, -2.45], [4.4, 4.8, 4.9], "base")
    for side, leg_bone, boot_bone, x in (
            ("right", "armorRightLeg", "armorRightBoot", -4.0),
            ("left", "armorLeftLeg", "armorLeftBoot", 0.0)):
        builder.cube(leg_bone, f"{side}_leg_shell", [x, 0, -2], [4, 12, 4], "dark", inflate=0.34)
        builder.cube(leg_bone, f"{side}_thigh", [x - 0.15, 6.2, -2.45], [4.3, 5.4, 4.9], "base")
        builder.cube(leg_bone, f"{side}_knee", [x - 0.25, 4.1, -2.95], [4.5, 2.4, 1.3], "accent")
        builder.cube(boot_bone, f"{side}_shin", [x - 0.15, 0.1, -2.5], [4.3, 4.2, 5], primary_plate)
        builder.cube(boot_bone, f"{side}_boot", [x - 0.35, -0.25, -3.0], [4.7, 2.2, 6], "shadow")
    add_armor_layering(builder, primary_plate, secondary_plate)
    if family == "republic_plastoid":
        builder.cube("armorHead", "helmet_crest", [-0.75, 29.0, -4.95], [1.5, 3.8, 0.9], "accent")
        builder.cube("armorBody", "chest_rank", [-1.5, 18.5, -2.85], [3, 1.0, 0.6], "accent")
    elif family == "separatist_alloy":
        builder.cube("armorBody", "power_cell", [-2.5, 16.0, -3.0], [5, 3.2, 0.8], "accent")
        builder.cube("armorHead", "sensor_block", [2.6, 29.0, -4.9], [1.5, 1.5, 0.9], "accent")
    elif family == "mandalorian_alloy":
        builder.cube("armorHead", "vertical_visor", [-0.7, 24.5, -4.95], [1.4, 4.0, 0.9], "accent")
        builder.cube("armorHead", "rangefinder", [4.4, 27.0, -0.7], [0.8, 5.0, 1.2], "accent")
        builder.cube("armorBody", "compact_pack", [-2.8, 15.0, 2.1], [5.6, 7.2, 2.3], "shadow")
    elif family == "nightsister_weave":
        builder.cube("armorHead", "woven_hood_crown", [-4.55, 30.3, -4.55], [9.1, 2.55, 9.1], "cloth")
        builder.cube("armorHead", "woven_hood_back", [-4.55, 23.8, 3.45], [9.1, 6.8, 1.1], "cloth")
        builder.cube("armorHead", "woven_hood_right", [-4.55, 23.8, -4.55], [1.25, 6.8, 8.4], "cloth")
        builder.cube("armorHead", "woven_hood_left", [3.3, 23.8, -4.55], [1.25, 6.8, 8.4], "cloth")
        builder.cube("armorHead", "woven_hood_brow", [-3.35, 29.55, -4.7], [6.7, 0.9, 0.7], "cloth")
        builder.cube(
            "armorBody", "ritual_sash", [-4.45, 14.6, -3.0], [8.9, 1.35, 5.7],
            "cloth", rotation=[0, 0, -11], pivot=[0, 15.2, 0])
        builder.cube("armorBody", "right_skirt", [-4.2, 7.2, -2.3], [3.7, 4.5, 4.6], "cloth")
        builder.cube("armorBody", "left_skirt", [0.5, 7.2, -2.3], [3.7, 4.5, 4.6], "cloth")
    elif family == "beskar":
        builder.cube("armorBody", "heavy_collar", [-5, 20.5, -2.8], [10, 2.8, 5.6], "light")
        builder.cube("armorRightArm", "right_heavy_pauldron", [-9.4, 18.0, -3.1], [6, 5.8, 6.2], "light")
        builder.cube("armorLeftArm", "left_heavy_pauldron", [3.4, 18.0, -3.1], [6, 5.8, 6.2], "light")
    builder.write(ARMOR_MODELS / f"{family}.geo.json", ARMOR_TEXTURES / f"{family}.png")
    # GeckoLib parses the declared animation resource even though armor pose
    # comes from the wearer. Keep one valid no-op clip instead of an empty
    # animation table, which GeckoLib rejects during resource loading.
    animation = {
        "format_version": "1.8.0",
        "animations": {
            "armor.pose": {
                "loop": True,
                "animation_length": 1,
                "bones": {},
            }
        },
    }
    (ARMOR_ANIMATIONS / f"{family}.animation.json").write_text(
        json.dumps(animation, indent=2) + "\n", encoding="utf-8")


def write_spawn_egg(design: RecruitDesign) -> None:
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    outline = (*design.palette.dark, 255)
    base = (*design.palette.base, 255)
    light = (*design.palette.light, 255)
    accent = (*design.palette.accent, 255)
    silhouette = (
        (6, 1, 9, 1), (4, 2, 11, 3), (3, 4, 12, 6),
        (2, 7, 13, 10), (3, 11, 12, 12), (5, 13, 10, 14),
    )
    for bounds in silhouette:
        draw.rectangle(bounds, fill=outline)
    draw.ellipse((3, 2, 12, 13), fill=base, outline=outline)
    draw.arc((4, 3, 11, 10), 190, 330, fill=light, width=2)
    seed = int(hashlib.sha256(design.id.encode("utf-8")).hexdigest()[:8], 16)
    for index in range(7):
        x = 4 + (seed + index * 5) % 8
        y = 4 + (seed // 7 + index * 3) % 7
        draw.rectangle((x, y, min(12, x + index % 2), min(12, y + index % 2)), fill=accent)
    ITEM_TEXTURES.mkdir(parents=True, exist_ok=True)
    image.save(ITEM_TEXTURES / f"{design.id}_spawn_egg.png")


def write_civilian_item_assets() -> None:
    civilians = (
        "republic_civilian", "separatist_technician", "mandalorian_clansperson",
        "hutt_civilian", "nightsister_civilian",
    )
    for civilian in civilians:
        item_id = f"{civilian}_spawn_egg"
        ITEM_DEFINITIONS.mkdir(parents=True, exist_ok=True)
        ITEM_MODELS.mkdir(parents=True, exist_ok=True)
        (ITEM_DEFINITIONS / f"{item_id}.json").write_text(json.dumps({
            "model": {"type": "minecraft:model", "model": f"galacticwars:item/{item_id}"},
        }, indent=2) + "\n", encoding="utf-8")
        (ITEM_MODELS / f"{item_id}.json").write_text(json.dumps({
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"galacticwars:item/{item_id}"},
        }, indent=2) + "\n", encoding="utf-8")


def ensure_civilian_animations() -> None:
    source = ENTITY_ANIMATIONS / "smuggler.animation.json"
    for civilian in (
            "republic_civilian", "separatist_technician", "mandalorian_clansperson",
            "hutt_civilian", "nightsister_civilian"):
        shutil.copyfile(source, ENTITY_ANIMATIONS / f"{civilian}.animation.json")


def main() -> None:
    for directory in (
            ENTITY_MODELS, ENTITY_ANIMATIONS, ENTITY_TEXTURES,
            ARMOR_MODELS, ARMOR_ANIMATIONS, ARMOR_TEXTURES,
            ITEM_DEFINITIONS, ITEM_MODELS, ITEM_TEXTURES):
        directory.mkdir(parents=True, exist_ok=True)
    for design in RECRUITS:
        build_recruit(design)
    for family, palette in ARMOR_PALETTES.items():
        build_armor(family, palette)
    write_civilian_item_assets()
    ensure_civilian_animations()
    print(f"Generated {len(RECRUITS)} recruit sets and {len(ARMOR_PALETTES)} armor sets")


if __name__ == "__main__":
    main()
