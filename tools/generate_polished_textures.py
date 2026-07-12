import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance, ImageOps


ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src/main/resources/assets/galacticwars/textures"
SOURCE_ART = ROOT / "tools/source_art"

ARMOR_PALETTES = {
    "republic_plastoid": {
        "shadow": (45, 51, 59), "base": (188, 197, 202),
        "light": (241, 243, 239), "accent": (43, 83, 151),
    },
    "separatist_alloy": {
        "shadow": (43, 43, 40), "base": (115, 105, 86),
        "light": (181, 165, 130), "accent": (211, 142, 42),
    },
    "mandalorian_alloy": {
        "shadow": (35, 48, 51), "base": (91, 119, 120),
        "light": (174, 196, 193), "accent": (27, 156, 153),
    },
    "nightsister_weave": {
        "shadow": (29, 17, 29), "base": (82, 30, 48),
        "light": (139, 49, 72), "accent": (193, 46, 70),
    },
    "beskar": {
        "shadow": (44, 53, 61), "base": (125, 143, 153),
        "light": (213, 224, 227), "accent": (71, 105, 128),
    },
}


def cube(draw, uv, size, colors):
    u, v = uv
    width, height, depth = size
    top, bottom, left, front, right, back = colors
    faces = (
        ((u + depth, v, u + depth + width - 1, v + depth - 1), top),
        ((u + depth + width, v, u + depth + width * 2 - 1, v + depth - 1), bottom),
        ((u, v + depth, u + depth - 1, v + depth + height - 1), left),
        ((u + depth, v + depth, u + depth + width - 1, v + depth + height - 1), front),
        ((u + depth + width, v + depth, u + depth + width + depth - 1,
          v + depth + height - 1), right),
        ((u + depth + width + depth, v + depth, u + depth * 2 + width * 2 - 1,
          v + depth + height - 1), back),
    )
    for bounds, color in faces:
        draw.rectangle(bounds, fill=color)
        if bounds[2] - bounds[0] >= 2 and bounds[3] - bounds[1] >= 2:
            draw.line((bounds[0], bounds[1], bounds[2], bounds[1]), fill=lighten(color, 16))
            draw.line((bounds[0], bounds[3], bounds[2], bounds[3]), fill=darken(color, 18))


def lighten(color, amount):
    return tuple(min(255, channel + amount) for channel in color[:3]) + color[3:]


def darken(color, amount):
    return tuple(max(0, channel - amount) for channel in color[:3]) + color[3:]


def mix(left, right, amount):
    return tuple(round(a + (b - a) * amount) for a, b in zip(left, right))


def average_rgb(left, right):
    return tuple(round((a + b) / 2) for a, b in zip(left[:3], right[:3])) + (255,)


def generated_block_texture(name):
    """Reduce retained generated material art to a compact seamless block texture."""
    source = Image.open(SOURCE_ART / f"generated_{name}_source.png").convert("RGB")
    texture = ImageOps.fit(source, (16, 16), method=Image.Resampling.LANCZOS)
    if name == "coruscant_panel":
        texture = ImageEnhance.Brightness(texture).enhance(1.35)
        texture = ImageEnhance.Contrast(texture).enhance(1.15)
    texture = texture.quantize(
        colors=24,
        method=Image.Quantize.MEDIANCUT,
        dither=Image.Dither.NONE,
    ).convert("RGBA")
    pixels = texture.load()

    # Generated sources are prompted to tile; equalizing the final opposite edge
    # texels removes any last sub-pixel mismatch introduced by reduction.
    for y in range(16):
        edge = average_rgb(pixels[0, y], pixels[15, y])
        pixels[0, y] = edge
        pixels[15, y] = edge
    for x in range(16):
        edge = average_rgb(pixels[x, 0], pixels[x, 15])
        pixels[x, 0] = edge
        pixels[x, 15] = edge
    texture.save(TEXTURES / f"block/{name}.png")


def generated_lightsaber(name):
    """Fit a chroma-cleaned generated saber into a native transparent item canvas."""
    source = Image.open(SOURCE_ART / f"generated_{name}_lightsaber_cutout.png").convert("RGBA")
    visible = source.getchannel("A").point(lambda alpha: 255 if alpha >= 24 else 0)
    bounds = visible.getbbox()
    if bounds is None:
        raise ValueError(f"Generated lightsaber source is empty: {name}")
    subject = source.crop(bounds)
    subject.thumbnail((15, 15), Image.Resampling.LANCZOS)
    icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    icon.alpha_composite(subject, ((16 - subject.width) // 2, (16 - subject.height) // 2))
    icon.putalpha(icon.getchannel("A").point(lambda alpha: 0 if alpha < 48 else alpha))
    icon.save(TEXTURES / f"item/{name}_lightsaber.png")


def lightsaber_model_textures(name, blade, glow, hilt_dark, hilt_metal):
    """Create compact hilt materials and a four-frame energy shimmer for 3D models."""
    output = TEXTURES / "item/lightsaber"
    output.mkdir(parents=True, exist_ok=True)

    hilt = Image.new("RGBA", (16, 16), (*hilt_dark, 255))
    draw = ImageDraw.Draw(hilt)
    draw.rectangle((0, 0, 15, 2), fill=(*hilt_metal, 255))
    draw.rectangle((0, 13, 15, 15), fill=(*darken(hilt_metal, 30), 255))
    for x in (2, 6, 10, 14):
        draw.line((x, 3, x, 12), fill=(*darken(hilt_dark, 18), 255))
    draw.rectangle((5, 5, 10, 8), fill=(*darken(hilt_metal, 18), 255))
    draw.rectangle((6, 6, 9, 7), fill=(*glow[:3], 255))
    draw.line((1, 1, 14, 1), fill=(*lighten(hilt_metal, 35), 255))
    hilt.save(output / f"{name}_hilt.png")

    blade_sheet = Image.new("RGBA", (16, 64), (0, 0, 0, 0))
    for frame in range(4):
        frame_image = Image.new("RGBA", (16, 16), (*glow[:3], 255))
        frame_draw = ImageDraw.Draw(frame_image)
        edge = mix(glow[:3], blade[:3], 0.45 + frame * 0.08)
        core = mix((255, 255, 255), blade[:3], frame * 0.035)
        frame_draw.rectangle((0, 0, 2, 15), fill=(*edge, 255))
        frame_draw.rectangle((13, 0, 15, 15), fill=(*edge, 255))
        frame_draw.rectangle((3, 0, 12, 15), fill=(*core, 255))
        if frame in (1, 3):
            frame_draw.line((3, 2 + frame, 12, 2 + frame), fill=(255, 255, 255, 255))
        blade_sheet.alpha_composite(frame_image, (0, frame * 16))
    blade_path = output / f"{name}_blade.png"
    blade_sheet.save(blade_path)
    blade_path.with_suffix(".png.mcmeta").write_text(json.dumps({
        "animation": {"frametime": 2, "interpolate": True},
    }, indent=2) + "\n", encoding="utf-8")


def armor_texture(path, palette, family):
    """Repaint an existing armor UV without changing its dimensions or alpha mask."""
    image = Image.open(path).convert("RGBA")
    source = image.copy()
    pixels = image.load()
    original = source.load()
    width, height = image.size

    def opaque(x, y):
        return 0 <= x < width and 0 <= y < height and original[x, y][3] > 0

    for y in range(height):
        for x in range(width):
            *_, alpha = original[x, y]
            if alpha == 0:
                continue
            variation = ((x * 7 + y * 11) % 13) / 12
            color = mix(palette["base"], palette["light"], 0.08 + variation * 0.22)
            if (x // 4 + y // 4) % 4 == 0:
                color = mix(color, palette["shadow"], 0.18)

            # Dark seams define each UV island; a sparse highlight gives metal and plates depth.
            edge = any(not opaque(x + dx, y + dy) for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)))
            if edge:
                color = mix(color, palette["shadow"], 0.45)
            elif (x * 3 + y * 5) % 17 == 0:
                color = mix(color, palette["light"], 0.28)

            # Restrained faction markings repeat across UV islands without altering their shape.
            marked = (family == "republic_plastoid" and (x + 2 * y) % 23 in (0, 1))
            marked |= (family == "separatist_alloy" and (2 * x + y) % 29 == 0)
            marked |= (family == "mandalorian_alloy" and (x - y) % 19 == 0)
            marked |= (family == "nightsister_weave" and (x + y) % 13 == 0)
            marked |= (family == "beskar" and (3 * x - y) % 31 == 0)
            if marked and not edge:
                color = mix(color, palette["accent"], 0.78)
            pixels[x, y] = (*color, alpha)

    image.save(path)


def armor_families():
    equipment = TEXTURES / "entity/equipment"
    for family, palette in ARMOR_PALETTES.items():
        for layer in ("humanoid", "humanoid_baby", "humanoid_leggings"):
            armor_texture(equipment / layer / f"{family}.png", palette, family)


def armor_item_icons():
    """Extract the generated 5x4 source sheet into crisp transparent 16x16 item icons."""
    source = Image.open(ROOT / "tools/source_art/generated_armor_icons.png").convert("RGBA")
    families = tuple(ARMOR_PALETTES)
    pieces = ("helmet", "chestplate", "leggings", "boots")
    for row, family in enumerate(families):
        for column, piece in enumerate(pieces):
            left = round(column * source.width / len(pieces))
            top = round(row * source.height / len(families))
            right = round((column + 1) * source.width / len(pieces))
            bottom = round((row + 1) * source.height / len(families))
            cell = source.crop((left, top, right, bottom))

            # The generated source uses a flat green key. Remove it before finding the subject bounds.
            keyed = Image.new("RGBA", cell.size, (0, 0, 0, 0))
            keyed_pixels = keyed.load()
            for y in range(cell.height):
                for x in range(cell.width):
                    red, green, blue, _ = cell.getpixel((x, y))
                    if not (green > 150 and green > red * 1.35 and green > blue * 1.35):
                        keyed_pixels[x, y] = (red, green, blue, 255)
            bounds = keyed.getbbox()
            if bounds is None:
                raise ValueError(f"Generated armor cell is empty: {family}_{piece}")
            subject = keyed.crop(bounds)
            scale = min(14 / subject.width, 14 / subject.height)
            size = (max(1, round(subject.width * scale)), max(1, round(subject.height * scale)))
            subject = subject.resize(size, Image.Resampling.LANCZOS)

            icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
            icon.alpha_composite(subject, ((16 - size[0]) // 2, (16 - size[1]) // 2))
            # Remove faint resampling haze while retaining a one-pixel antialiased edge.
            icon.putalpha(icon.getchannel("A").point(lambda alpha: 0 if alpha < 48 else alpha))
            icon.save(TEXTURES / f"item/{family}_{piece}.png")


def clone_trooper():
    image = Image.new("RGBA", (64, 64), (31, 36, 43, 255))
    draw = ImageDraw.Draw(image)
    undersuit = ((50, 57, 66, 255), (22, 26, 31, 255), (29, 34, 41, 255),
                 (37, 43, 51, 255), (27, 32, 39, 255), (32, 37, 44, 255))
    armor = ((236, 238, 235, 255), (174, 182, 187, 255), (196, 202, 203, 255),
             (224, 227, 224, 255), (187, 194, 198, 255), (207, 212, 211, 255))
    cube(draw, (0, 0), (8, 8, 8), undersuit)
    cube(draw, (16, 16), (8, 12, 4), undersuit)
    cube(draw, (40, 16), (4, 12, 4), undersuit)
    cube(draw, (32, 48), (4, 12, 4), undersuit)
    cube(draw, (0, 16), (4, 12, 4), undersuit)
    cube(draw, (16, 48), (4, 12, 4), undersuit)
    cube(draw, (32, 0), (8, 8, 8), armor)
    cube(draw, (16, 32), (8, 12, 4), armor)
    cube(draw, (40, 32), (4, 12, 4), armor)
    cube(draw, (48, 48), (4, 12, 4), armor)
    cube(draw, (0, 32), (4, 12, 4), armor)
    cube(draw, (0, 48), (4, 12, 4), armor)

    # Phase-II-inspired helmet front on the inflated head layer.
    draw.rectangle((40, 9, 47, 10), fill=(26, 31, 37, 255))
    draw.rectangle((41, 11, 46, 12), fill=(13, 17, 22, 255))
    draw.point((40, 12), fill=(23, 45, 82, 255))
    draw.point((47, 12), fill=(23, 45, 82, 255))
    draw.line((43, 8, 44, 8), fill=(41, 77, 137, 255))
    draw.line((43, 13, 44, 15), fill=(41, 77, 137, 255))
    draw.point((41, 14), fill=(86, 95, 100, 255))
    draw.point((46, 14), fill=(86, 95, 100, 255))

    # Chest plates, abdominal seal, belt, shoulders, gauntlets and knees.
    draw.line((21, 37, 26, 37), fill=(247, 248, 244, 255))
    draw.line((20, 38, 23, 41), fill=(164, 173, 179, 255))
    draw.line((27, 38, 24, 41), fill=(164, 173, 179, 255))
    draw.rectangle((22, 42, 25, 44), fill=(49, 56, 65, 255))
    draw.line((20, 45, 27, 45), fill=(38, 45, 54, 255))
    draw.point((21, 46), fill=(41, 77, 137, 255))
    draw.point((26, 46), fill=(41, 77, 137, 255))
    for x in (44, 52):
        draw.line((x, 37, x + 3, 37), fill=(41, 77, 137, 255))
        draw.line((x, 44, x + 3, 44), fill=(87, 96, 101, 255))
    for x in (4, 20):
        draw.line((x, 38, x + 3, 38), fill=(41, 77, 137, 255))
        draw.line((x, 43, x + 3, 43), fill=(104, 112, 116, 255))
    image.putpixel((63, 0), (0, 0, 0, 0))
    image.save(TEXTURES / "entity/clone_trooper.png")


def battle_droid():
    image = Image.new("RGBA", (64, 64), (47, 43, 35, 255))
    draw = ImageDraw.Draw(image)
    joints = ((77, 69, 55, 255), (32, 30, 27, 255), (49, 46, 40, 255),
              (58, 54, 46, 255), (40, 38, 33, 255), (52, 48, 41, 255))
    plating = ((204, 174, 116, 255), (119, 94, 60, 255), (150, 124, 81, 255),
               (187, 155, 99, 255), (137, 110, 71, 255), (162, 133, 86, 255))
    cube(draw, (0, 0), (9, 8, 8), joints)
    cube(draw, (16, 16), (9, 12, 5), joints)
    cube(draw, (40, 16), (4, 12, 4), joints)
    cube(draw, (32, 48), (4, 12, 4), joints)
    cube(draw, (0, 16), (4, 12, 4), joints)
    cube(draw, (16, 48), (4, 12, 4), joints)
    cube(draw, (30, 0), (9, 8, 8), plating)
    cube(draw, (16, 32), (9, 12, 5), plating)
    cube(draw, (40, 32), (4, 12, 4), plating)
    cube(draw, (48, 48), (4, 12, 4), plating)
    cube(draw, (0, 32), (4, 12, 4), plating)
    cube(draw, (0, 48), (4, 12, 4), plating)

    # Narrow photoreceptors and long face plate.
    draw.rectangle((38, 10, 46, 11), fill=(43, 37, 26, 255))
    draw.point((40, 10), fill=(255, 188, 32, 255))
    draw.point((44, 10), fill=(255, 188, 32, 255))
    draw.line((41, 12, 43, 12), fill=(104, 81, 51, 255))
    draw.line((42, 13, 42, 15), fill=(91, 71, 46, 255))
    # Chest mechanics and segmented torso.
    draw.rectangle((22, 38, 28, 39), fill=(116, 91, 59, 255))
    draw.line((21, 40, 24, 44), fill=(102, 80, 54, 255))
    draw.line((29, 40, 26, 44), fill=(102, 80, 54, 255))
    draw.rectangle((24, 41, 26, 45), fill=(50, 49, 45, 255))
    draw.point((25, 42), fill=(116, 126, 124, 255))
    draw.line((21, 46, 29, 46), fill=(91, 70, 45, 255))
    for x in (44, 52, 4, 20):
        draw.line((x, 39, x + 3, 39), fill=(100, 78, 50, 255))
        draw.point((x + 1, 44), fill=(50, 49, 45, 255))
    image.putpixel((63, 0), (0, 0, 0, 0))
    image.save(TEXTURES / "entity/b1_battle_droid.png")


def lightsaber(name, blade, glow):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pixels = image.load()
    # Compact ridged hilt, diagonal lower-left to upper-right.
    hilt = ((2, 14, (35, 39, 44, 255)), (3, 13, (181, 190, 195, 255)),
            (4, 12, (49, 54, 60, 255)), (5, 11, (202, 208, 209, 255)),
            (6, 10, (45, 49, 55, 255)))
    for x, y, color in hilt:
        pixels[x, y] = color
        if x + 1 < 16:
            pixels[x + 1, y] = darken(color, 34)
        if y - 1 >= 0:
            pixels[x, y - 1] = lighten(color, 18)
    pixels[5, 12] = (18, 21, 25, 255)
    pixels[6, 11] = (224, 230, 232, 255)
    # White-hot blade with a restrained colored edge.
    for x, y in zip(range(7, 14), range(9, 2, -1)):
        pixels[x, y] = (248, 252, 255, 255)
        if x - 1 >= 0:
            pixels[x - 1, y] = glow
        if y - 1 >= 0:
            pixels[x, y - 1] = blade
    pixels[13, 2] = glow
    image.save(TEXTURES / f"item/{name}_lightsaber.png")


if __name__ == "__main__":
    for block in ("coruscant_panel", "kamino_panel", "geonosis_rock", "tatooine_sand"):
        generated_block_texture(block)
    # Recruit textures are generated alongside their exact 128x128 UV geometry
    # by generate_character_models.py. Do not rewrite them from this legacy pass.
    lightsaber("blue", (84, 188, 255, 255), (28, 111, 231, 255))
    lightsaber("green", (102, 244, 145, 255), (20, 166, 84, 255))
    lightsaber("red", (255, 104, 96, 255), (211, 31, 39, 255))
    for color in ("purple", "yellow", "white"):
        generated_lightsaber(color)
    for args in (
            ("blue", (84, 188, 255, 255), (28, 111, 231, 255), (31, 37, 45), (181, 190, 195)),
            ("green", (102, 244, 145, 255), (20, 166, 84, 255), (29, 38, 35), (171, 190, 182)),
            ("red", (255, 104, 96, 255), (211, 31, 39, 255), (43, 31, 32), (188, 176, 176)),
            ("purple", (221, 136, 255, 255), (132, 43, 218, 255), (30, 29, 40), (179, 170, 198)),
            ("yellow", (255, 232, 92, 255), (225, 160, 25, 255), (55, 39, 31), (177, 161, 138)),
            ("white", (244, 250, 255, 255), (151, 199, 235, 255), (34, 37, 45), (205, 215, 225)),
    ):
        lightsaber_model_textures(*args)
    armor_families()
    armor_item_icons()
