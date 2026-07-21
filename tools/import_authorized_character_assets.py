"""Convert licensed upstream Blockbench character art into Galactic Wars GeckoLib assets.

The files under ``tools/source_art/authorized_upstream`` are immutable source inputs. This
module performs only format conversion, variant selection, and documented palette adaptations.
Running it repeatedly produces byte-identical JSON and PNG outputs.
"""

from __future__ import annotations

import json
import math
import re
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Iterable, Literal

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOT = ROOT / "tools/source_art/authorized_upstream"
FORGE_SOURCE = SOURCE_ROOT / "forge_star_wars_clone_wars"
GALAXIES_SOURCE = SOURCE_ROOT / "galaxies_pswg"
ASSETS = ROOT / "src/main/resources/assets/galacticwars"
ENTITY_MODELS = ASSETS / "geckolib/models/entity"
ENTITY_ANIMATIONS = ASSETS / "geckolib/animations/entity"
ENTITY_TEXTURES = ASSETS / "textures/entity"
ARMOR_MODELS = ASSETS / "geckolib/models/armor"
ARMOR_TEXTURES = ASSETS / "textures/armor"
ITEM_TEXTURES = ASSETS / "textures/item"

Policy = Literal["default", "include", "force", "exclude"]
GroupPolicy = Callable[[tuple[str, ...], dict], Policy]
CubePolicy = Callable[[tuple[str, ...], dict, int], bool]


@dataclass(frozen=True)
class ConvertedModel:
    geometry: dict
    selected_elements: tuple[tuple[tuple[str, ...], dict], ...]


def snake_case(value: str) -> str:
    value = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", value)
    value = re.sub(r"[^a-zA-Z0-9]+", "_", value).strip("_").lower()
    return value or "part"


def clean_number(value: float) -> int | float:
    rounded = round(float(value), 5)
    return int(rounded) if rounded.is_integer() else rounded


def clean_vector(values: Iterable[float]) -> list[int | float]:
    return [clean_number(value) for value in values]


def mirror_x_point(values: Iterable[float]) -> list[int | float]:
    x, y, z = (float(value) for value in values)
    return clean_vector((-x, y, z))


def mirror_x_box(start: Iterable[float], end: Iterable[float]) -> tuple[list, list]:
    start_x, start_y, start_z = (float(value) for value in start)
    end_x, end_y, end_z = (float(value) for value in end)
    origin = clean_vector((-end_x, start_y, start_z))
    size = clean_vector((end_x - start_x, end_y - start_y, end_z - start_z))
    return origin, size


def face_uv(face: dict) -> dict | None:
    coordinates = face.get("uv")
    if not isinstance(coordinates, list) or len(coordinates) != 4:
        return None
    u1, v1, u2, v2 = (float(value) for value in coordinates)
    return {
        "uv": clean_vector((u1, v1)),
        "uv_size": clean_vector((u2 - u1, v2 - v1)),
    }


def convert_cube(element: dict, mirror_x: bool) -> dict | None:
    start = element.get("from", (0, 0, 0))
    end = element.get("to", (0, 0, 0))
    if mirror_x:
        origin, size = mirror_x_box(start, end)
    else:
        origin = clean_vector(start)
        size = clean_vector(float(right) - float(left) for left, right in zip(start, end))
    if any(float(value) <= 0 for value in size):
        return None

    converted = {
        "name": snake_case(element.get("name", "cube")),
        "origin": origin,
        "size": size,
    }
    faces = {}
    for direction, source_face in element.get("faces", {}).items():
        converted_face = face_uv(source_face)
        if converted_face is not None:
            faces[direction] = converted_face
    if faces:
        converted["uv"] = faces
    elif "uv_offset" in element:
        converted["uv"] = clean_vector(element["uv_offset"])
    else:
        converted["uv"] = [0, 0]

    inflate = float(element.get("inflate", 0))
    if inflate:
        converted["inflate"] = clean_number(inflate)
    if element.get("mirror_uv"):
        converted["mirror"] = True
    rotation = element.get("rotation")
    if rotation and any(abs(float(value)) > 0.00001 for value in rotation):
        x_rotation, y_rotation, z_rotation = (float(value) for value in rotation)
        converted["rotation"] = clean_vector(
            (x_rotation, -y_rotation, -z_rotation) if mirror_x
            else (x_rotation, y_rotation, z_rotation)
        )
        converted["pivot"] = (
            mirror_x_point(element.get("origin", (0, 0, 0)))
            if mirror_x else clean_vector(element.get("origin", (0, 0, 0)))
        )
    return converted


def clone_group_policy(phase: int, arc: bool) -> GroupPolicy:
    def policy(path: tuple[str, ...], group: dict) -> Policy:
        name = snake_case(group.get("name", ""))
        if len(path) == 1:
            return "include"
        if name in {"phase1", "phase_1"}:
            return "force" if phase == 1 else "exclude"
        if name in {"phase2", "phase_2"}:
            return "force" if phase == 2 else "exclude"
        if name in {"left_arm_slim", "right_arm_slim"}:
            return "exclude"
        if name in {"left_arm_default", "right_arm_default"}:
            return "force"
        if name in {"rangefinder", "kama", "pauldron"}:
            return "force" if arc else "exclude"
        if name in {"macrobinoculars", "visor"}:
            return "exclude"
        return "default"

    return policy


MANDALORIAN_VARIANTS = {
    "warrior": {"helmet1", "j_t12"},
    "marksman": {"helmet2", "rangefinder"},
    "heavy": {"helmet3", "macrobinoculars", "z6"},
    "clansperson": {"helmet1"},
}


def mandalorian_group_policy(variant: str) -> GroupPolicy:
    selected = MANDALORIAN_VARIANTS[variant]
    optional = {
        "helmet1", "helmet2", "helmet3", "horns1", "horns2",
        "antennaright", "antennaleft", "macrobinoculars", "rangefinder",
        "z6", "j_t12", "jt12",
    }

    def policy(path: tuple[str, ...], group: dict) -> Policy:
        name = snake_case(group.get("name", ""))
        if len(path) == 1:
            return "include"
        if name in optional:
            normalized = "j_t12" if name == "jt12" else name
            return "force" if normalized in selected else "exclude"
        return "default"

    return policy


def default_group_policy(path: tuple[str, ...], group: dict) -> Policy:
    return "include" if len(path) == 1 else "default"


def b1_cube_policy(path: tuple[str, ...], element: dict, child_index: int) -> bool:
    # The upstream right forearm contains an integrated E-5. Galactic Wars renders its
    # registered held item instead, so retain the authored arm segments and omit only the
    # three weapon cuboids that would otherwise z-fight with the runtime blaster. The
    # small third cuboid is the authored wrist joint and remains part of the arm.
    return not (path[-1] == "right_arm" and child_index >= 3)


def converted_bone_name(source_name: str, armor: bool, parent: str | None) -> str:
    name = snake_case(source_name)
    root_names = {
        "head": "armorHead" if armor else "head",
        "body": "armorBody" if armor else "body",
        "torso": "armorBody" if armor else "body",
        "right_arm": "armorRightArm" if armor else "right_arm",
        "rightarm": "armorRightArm" if armor else "right_arm",
        "left_arm": "armorLeftArm" if armor else "left_arm",
        "leftarm": "armorLeftArm" if armor else "left_arm",
        "right_leg": "armorRightLeg" if armor else "right_leg",
        "rightleg": "armorRightLeg" if armor else "right_leg",
        "left_leg": "armorLeftLeg" if armor else "left_leg",
        "leftleg": "armorLeftLeg" if armor else "left_leg",
    }
    if parent is None and name in root_names:
        return root_names[name]
    if name in {"phase1", "phase_1", "phase2", "phase_2", "helmet1", "helmet2", "helmet3"}:
        return "helmet"
    if armor and name == "right_boot":
        return "armorRightBoot"
    if armor and name == "left_boot":
        return "armorLeftBoot"
    return name


def armor_parent_for(root_bone: str) -> str:
    return {
        "armorHead": "bipedHead",
        "armorBody": "bipedBody",
        "armorRightArm": "bipedRightArm",
        "armorLeftArm": "bipedLeftArm",
        "armorRightLeg": "bipedRightLeg",
        "armorLeftLeg": "bipedLeftLeg",
    }[root_bone]


def convert_bbmodel(
        source_path: Path,
        identifier: str,
        group_policy: GroupPolicy = default_group_policy,
        cube_policy: CubePolicy | None = None,
        armor: bool = False,
) -> ConvertedModel:
    source = json.loads(source_path.read_text(encoding="utf-8"))
    elements = {element["uuid"]: element for element in source["elements"]}
    mirror_x = bool(source.get("modded_entity_flip_y", True))
    bones: list[dict] = []
    selected_elements: list[tuple[tuple[str, ...], dict]] = []
    bone_names: set[str] = set()

    if armor:
        for name in (
                "bipedHead", "bipedBody", "bipedRightArm", "bipedLeftArm",
                "bipedRightLeg", "bipedLeftLeg"):
            bones.append({"name": name, "pivot": [0, 0, 0], "cubes": []})
            bone_names.add(name)

    def walk(group: dict, parent: str | None, path: tuple[str, ...], inherited_force: bool) -> None:
        source_name = snake_case(group.get("name", "part"))
        current_path = path + (source_name,)
        policy = group_policy(current_path, group)
        if policy == "exclude":
            return
        if policy == "default" and group.get("visibility") is False and not inherited_force:
            return
        force_children = inherited_force or policy == "force"
        name = converted_bone_name(group.get("name", "part"), armor, parent)
        base_name = name
        suffix = 2
        while name in bone_names:
            name = f"{base_name}_{suffix}"
            suffix += 1
        bone_names.add(name)

        pivot = group.get("origin", (0, 0, 0))
        bone = {
            "name": name,
            "pivot": mirror_x_point(pivot) if mirror_x else clean_vector(pivot),
            "cubes": [],
        }
        if parent is not None:
            bone["parent"] = parent
        elif armor:
            bone["parent"] = armor_parent_for(name)
        rotation = group.get("rotation")
        if rotation and any(abs(float(value)) > 0.00001 for value in rotation):
            x_rotation, y_rotation, z_rotation = (float(value) for value in rotation)
            bone["rotation"] = clean_vector(
                (x_rotation, -y_rotation, -z_rotation) if mirror_x
                else (x_rotation, y_rotation, z_rotation)
            )
        bones.append(bone)

        cube_index = 0
        nested_groups = []
        for child in group.get("children", []):
            if isinstance(child, str):
                element = elements.get(child)
                if element is None:
                    continue
                include_cube = force_children or element.get("visibility") is not False
                if include_cube and (cube_policy is None or cube_policy(current_path, element, cube_index)):
                    converted = convert_cube(element, mirror_x)
                    if converted is not None:
                        bone["cubes"].append(converted)
                        selected_elements.append((current_path, element))
                cube_index += 1
            elif isinstance(child, dict):
                nested_groups.append(child)
        for child_group in nested_groups:
            walk(child_group, name, current_path, force_children)

    for root_group in source["outliner"]:
        walk(root_group, None, (), False)

    if armor:
        # Boots are separate renderer targets in GeckoLib. The upstream clone model already
        # groups them separately; reparent those groups so boot items remain visible when the
        # leggings bones are hidden.
        for bone in bones:
            if bone["name"] == "armorRightBoot":
                bone["parent"] = "bipedRightLeg"
            elif bone["name"] == "armorLeftBoot":
                bone["parent"] = "bipedLeftLeg"
        existing = {bone["name"] for bone in bones}
        for name, parent in (
                ("armorRightBoot", "bipedRightLeg"),
                ("armorLeftBoot", "bipedLeftLeg")):
            if name not in existing:
                bones.append({"name": name, "pivot": [0, 0, 0], "cubes": [], "parent": parent})
    else:
        existing = {bone["name"] for bone in bones}
        for name, parent, pivot in (
                ("RightHandItem", "right_arm", (-6, 12, 0)),
                ("LeftHandItem", "left_arm", (6, 12, 0))):
            if name not in existing:
                bones.append({"name": name, "pivot": list(pivot), "cubes": [], "parent": parent})

    width = int(source["resolution"]["width"])
    height = int(source["resolution"]["height"])
    geometry = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": f"geometry.galacticwars.{identifier}",
                "texture_width": width,
                "texture_height": height,
                "visible_bounds_width": 3,
                "visible_bounds_height": 4,
                "visible_bounds_offset": [0, 1.5, 0],
            },
            "bones": bones,
        }],
    }
    return ConvertedModel(geometry, tuple(selected_elements))


def add_component_bones(converted: ConvertedModel, components: Iterable[tuple[str, str]]) -> None:
    bones = converted.geometry["minecraft:geometry"][0]["bones"]
    existing = {bone["name"] for bone in bones}
    for name, parent in components:
        if name not in existing:
            bones.append({"name": name, "pivot": [0, 0, 0], "cubes": [], "parent": parent})


def set_bone_pivot(converted: ConvertedModel, bone_name: str, pivot: Iterable[float]) -> None:
    bones = converted.geometry["minecraft:geometry"][0]["bones"]
    matches = [bone for bone in bones if bone["name"] == bone_name]
    if len(matches) != 1:
        raise ValueError(f"Expected one {bone_name} bone, found {len(matches)}")
    matches[0]["pivot"] = clean_vector(pivot)


def split_cube_y(cube: dict, split_y: float) -> tuple[dict, dict]:
    """Split one authored limb cuboid at a joint while preserving its face texture regions."""
    origin_y = float(cube["origin"][1])
    size_y = float(cube["size"][1])
    end_y = origin_y + size_y
    if not origin_y < split_y < end_y:
        raise ValueError(f"Split {split_y} is outside cube Y range {origin_y}..{end_y}")
    lower_height = split_y - origin_y
    upper_height = end_y - split_y
    upper_fraction = upper_height / size_y

    lower = json.loads(json.dumps(cube))
    upper = json.loads(json.dumps(cube))
    lower["size"][1] = clean_number(lower_height)
    upper["origin"][1] = clean_number(split_y)
    upper["size"][1] = clean_number(upper_height)

    for face_name in ("north", "south", "east", "west"):
        source_face = cube["uv"][face_name]
        u, v = (float(value) for value in source_face["uv"])
        uv_width, uv_height = (float(value) for value in source_face["uv_size"])
        upper_uv_height = uv_height * upper_fraction
        lower["uv"][face_name]["uv"] = clean_vector((u, v + upper_uv_height))
        lower["uv"][face_name]["uv_size"] = clean_vector(
            (uv_width, uv_height - upper_uv_height)
        )
        upper["uv"][face_name]["uv_size"] = clean_vector((uv_width, upper_uv_height))
    return upper, lower


def rig_b1_mechanical_joints(converted: ConvertedModel) -> None:
    """Turn the upstream single-cuboid limbs into articulated GeckoLib child joints."""
    bones = converted.geometry["minecraft:geometry"][0]["bones"]
    by_name = {bone["name"]: bone for bone in bones}

    body = by_name["body"]
    torso_cubes = [body["cubes"][index] for index in (0, 1, 6, 7)]
    body["cubes"] = [cube for index, cube in enumerate(body["cubes"])
                     if index not in {0, 1, 6, 7}]
    torso_core = {
        "name": "torso_core",
        "pivot": list(body["pivot"]),
        "cubes": torso_cubes,
        "parent": "body",
    }
    bones.append(torso_core)

    head = by_name["head"]
    neck_cube = head["cubes"].pop()
    neck = {
        "name": "neck",
        "pivot": [0, 24, 0],
        "cubes": [neck_cube],
        "parent": "torso_core",
    }
    head["parent"] = "neck"
    bones.append(neck)

    right_arm = by_name["right_arm"]
    right_forearm_cubes = right_arm["cubes"][1:]
    right_arm["cubes"] = right_arm["cubes"][:1]
    bones.append({
        "name": "right_forearm",
        "pivot": [-5, 19, 0],
        "cubes": right_forearm_cubes,
        "parent": "right_arm",
    })

    left_arm = by_name["left_arm"]
    left_upper, left_lower = split_cube_y(left_arm["cubes"].pop(), 18)
    left_arm["cubes"].append(left_upper)
    bones.append({
        "name": "left_forearm",
        "pivot": [5, 18, 0],
        "cubes": [left_lower],
        "parent": "left_arm",
    })

    for side, pivot_x in (("right", -2), ("left", 2)):
        leg = by_name[f"{side}_leg"]
        upper_leg, shin = split_cube_y(leg["cubes"].pop(), 6)
        leg["cubes"].append(upper_leg)
        bones.append({
            "name": f"{side}_shin",
            "pivot": [pivot_x, 6, 0],
            "cubes": [shin],
            "parent": f"{side}_leg",
        })

    for limb in ("right_arm", "left_arm"):
        by_name[limb]["parent"] = "torso_core"
    by_name["RightHandItem"]["parent"] = "right_forearm"
    by_name["RightHandItem"]["pivot"] = [-5, 19, -6]
    by_name["LeftHandItem"]["parent"] = "left_forearm"
    by_name["LeftHandItem"]["pivot"] = [5, 12, 0]


def write_model(path: Path, converted: ConvertedModel) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(converted.geometry, indent=2) + "\n", encoding="utf-8")


def adapt_animation_bones(asset_id: str, aliases: dict[str, str]) -> None:
    """Retarget generated detail-bone motion onto the source model's actual articulated bones."""
    path = ENTITY_ANIMATIONS / f"{asset_id}.animation.json"
    document = json.loads(path.read_text(encoding="utf-8"))
    for animation in document.get("animations", {}).values():
        bones = animation.get("bones", {})
        for source_name, target_name in aliases.items():
            source_channel = bones.pop(source_name, None)
            if source_channel is None:
                continue
            target_channel = bones.setdefault(target_name, {})
            for transform, keyframes in source_channel.items():
                target_channel.setdefault(transform, keyframes)
    path.write_text(json.dumps(document, indent=2) + "\n", encoding="utf-8")


def scale_model_uvs(converted: ConvertedModel, scale: int) -> None:
    geometry = converted.geometry["minecraft:geometry"][0]
    description = geometry["description"]
    description["texture_width"] *= scale
    description["texture_height"] *= scale
    for bone in geometry["bones"]:
        for cube in bone.get("cubes", []):
            uv = cube.get("uv")
            if isinstance(uv, list):
                cube["uv"] = [clean_number(float(value) * scale) for value in uv]
            elif isinstance(uv, dict):
                for face in uv.values():
                    face["uv"] = [clean_number(float(value) * scale) for value in face["uv"]]
                    face["uv_size"] = [
                        clean_number(float(value) * scale) for value in face["uv_size"]
                    ]


def copy_rgba(source: Path, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    with Image.open(source) as image:
        image.convert("RGBA").save(destination, optimize=False)


def paint_pixels(image: Image.Image, face: dict, predicate: Callable[[float, float], bool], color: tuple[int, int, int]) -> None:
    coordinates = face.get("uv")
    if not isinstance(coordinates, list) or len(coordinates) != 4:
        return
    u1, v1, u2, v2 = (int(round(float(value))) for value in coordinates)
    left, right = sorted((u1, u2))
    top, bottom = sorted((v1, v2))
    width = max(1, right - left)
    height = max(1, bottom - top)
    for y in range(top, bottom):
        for x in range(left, right):
            nx = (x - left + 0.5) / width
            ny = (y - top + 0.5) / height
            if not predicate(nx, ny):
                continue
            red, green, blue, alpha = image.getpixel((x, y))
            if alpha == 0 or max(red, green, blue) < 72:
                continue
            image.putpixel((x, y), (
                (red + color[0] * 3) // 4,
                (green + color[1] * 3) // 4,
                (blue + color[2] * 3) // 4,
                alpha,
            ))


def forge_501st_blue() -> tuple[int, int, int]:
    """Select the dominant authored 501st blue from the permissioned Forge texture."""
    source_path = FORGE_SOURCE / "clone_armor_armor_501st_layer_helmet.png"
    with Image.open(source_path) as source:
        colors = Counter(
            pixel[:3]
            for pixel in source.convert("RGBA").get_flattened_data()
            if pixel[3] and pixel[2] > pixel[0] * 1.15
        )
    if not colors:
        raise ValueError(f"No 501st blue pixels found in {source_path}")
    return colors.most_common(1)[0][0]


def write_501st_texture(converted: ConvertedModel, destination: Path) -> None:
    with Image.open(GALAXIES_SOURCE / "Clone Trooper.png") as source:
        image = source.convert("RGBA")
    blue = forge_501st_blue()
    for path, element in converted.selected_elements:
        name = snake_case(element.get("name", ""))
        faces = element.get("faces", {})
        if "phase2" in path or "phase_2" in path:
            for direction in ("north", "south"):
                if direction in faces:
                    paint_pixels(image, faces[direction], lambda x, y: 0.38 <= x <= 0.62, blue)
        elif name == "jacket":
            if "north" in faces:
                paint_pixels(image, faces["north"], lambda x, y: y < 0.58 and (x < 0.28 or x > 0.72), blue)
        elif ("arm_default" in "_".join(path)
              and float(element.get("inflate", 0)) >= 0.35):
            for direction in ("north", "south", "east", "west"):
                if direction in faces:
                    paint_pixels(image, faces[direction], lambda x, y: y < 0.34, blue)
        elif "pantleg" in name and float(element.get("inflate", 0)) >= 0.45:
            for direction in ("north", "south"):
                if direction in faces:
                    paint_pixels(image, faces[direction], lambda x, y: 0.32 < y < 0.62, blue)
    destination.parent.mkdir(parents=True, exist_ok=True)
    image.save(destination, optimize=False)


MANDALORIAN_PALETTES = {
    "warrior": ((25, 37, 44), (68, 94, 103), (153, 176, 178)),
    "marksman": ((45, 31, 25), (144, 82, 40), (205, 151, 88)),
    "heavy": ((20, 29, 41), (55, 75, 99), (129, 148, 164)),
    "clansperson": ((36, 40, 34), (91, 102, 75), (164, 157, 127)),
}


def fill_transparent_body_glove(
        image: Image.Image,
        selected_elements: Iterable[tuple[tuple[str, ...], dict]],
        color: tuple[int, int, int],
) -> None:
    base_elements = {"torso", "leftarm", "rightarm", "leftleg", "rightleg"}
    for _, element in selected_elements:
        if snake_case(element.get("name", "")) not in base_elements:
            continue
        for face in element.get("faces", {}).values():
            coordinates = face.get("uv")
            if not isinstance(coordinates, list) or len(coordinates) != 4:
                continue
            u1, v1, u2, v2 = (int(round(float(value))) for value in coordinates)
            left, right = sorted((u1, u2))
            top, bottom = sorted((v1, v2))
            for y in range(top, bottom):
                for x in range(left, right):
                    if image.getpixel((x, y))[3] == 0:
                        shade = 8 if (x + y) % 3 == 0 else 0
                        image.putpixel((x, y), tuple(min(255, channel + shade) for channel in color) + (255,))


def write_mandalorian_texture(
        variant: str,
        converted: ConvertedModel,
        destination: Path,
) -> None:
    dark, middle, light = MANDALORIAN_PALETTES[variant]
    with Image.open(GALAXIES_SOURCE / "PSWG_Mandalorian.png") as source:
        image = source.convert("RGBA")
    fill_transparent_body_glove(image, converted.selected_elements, dark)
    for y in range(image.height):
        for x in range(image.width):
            red, green, blue, alpha = image.getpixel((x, y))
            if alpha == 0:
                continue
            luminance = (red * 3 + green * 6 + blue) / 10
            if luminance < 28:
                target = dark
                factor = max(0.45, luminance / 28)
            elif luminance < 150:
                target = middle
                factor = 0.65 + luminance / 500
            else:
                target = light
                factor = 0.72 + luminance / 850
            image.putpixel((x, y), tuple(min(255, round(channel * factor)) for channel in target) + (alpha,))
    destination.parent.mkdir(parents=True, exist_ok=True)
    image.save(destination, optimize=False)


def add_sprite_marking(image: Image.Image, piece: str) -> None:
    blue = forge_501st_blue() + (255,)
    masks = {
        "helmet": ((7, 2, 8, 9),),
        "chestplate": ((5, 4, 6, 10), (9, 4, 10, 10)),
        "leggings": ((4, 7, 5, 11), (10, 7, 11, 11)),
        "boots": ((3, 9, 5, 12), (10, 9, 12, 12)),
    }
    for left, top, right, bottom in masks[piece]:
        for y in range(top, min(bottom + 1, image.height)):
            for x in range(left, min(right + 1, image.width)):
                red, green, old_blue, alpha = image.getpixel((x, y))
                if alpha and max(red, green, old_blue) > 70:
                    image.putpixel((x, y), blue)


def write_clone_item_icons() -> None:
    sources = {
        "helmet": "ct_p1_helmet.png",
        "chestplate": "clonetrooper_chestplate.png",
        "leggings": "clonetrooper_leggings.png",
        "boots": "clonetrooper_boots.png",
    }
    for piece, source_name in sources.items():
        with Image.open(GALAXIES_SOURCE / source_name) as source:
            phase_i = source.convert("RGBA")
        phase_i.save(ITEM_TEXTURES / f"phase_i_clone_{piece}.png", optimize=False)

        phase_ii_source = "ct_p2_helmet.png" if piece == "helmet" else source_name
        with Image.open(GALAXIES_SOURCE / phase_ii_source) as source:
            phase_ii = source.convert("RGBA")
        add_sprite_marking(phase_ii, piece)
        phase_ii.save(ITEM_TEXTURES / f"republic_plastoid_{piece}.png", optimize=False)


CLONE_COMPONENTS = (
    ("chest_armor", "body"),
    ("right_gauntlet", "right_arm"),
    ("left_gauntlet", "left_arm"),
    ("right_boot", "right_leg"),
    ("left_boot", "left_leg"),
)


def build_clone_assets() -> None:
    source_model = GALAXIES_SOURCE / "Clone Trooper.bbmodel"
    variants = {
        "phase_i_clone_trooper": (1, False),
        "phase_i_arc_trooper": (1, True),
        "clone_trooper": (2, False),
        "arc_trooper": (2, True),
    }
    converted_variants = {}
    for asset_id, (phase, arc) in variants.items():
        converted = convert_bbmodel(
            source_model,
            asset_id,
            group_policy=clone_group_policy(phase, arc),
        )
        add_component_bones(converted, CLONE_COMPONENTS)
        if arc:
            # The source kama group inherits a modeling-handle origin far to the side.
            # Rotate both authored panels around the wearer's hips instead.
            set_bone_pivot(converted, "kama", (0, 12, 0))
        write_model(ENTITY_MODELS / f"{asset_id}.geo.json", converted)
        if phase == 1:
            copy_rgba(GALAXIES_SOURCE / "Clone Trooper.png", ENTITY_TEXTURES / f"{asset_id}.png")
        else:
            write_501st_texture(converted, ENTITY_TEXTURES / f"{asset_id}.png")
        converted_variants[asset_id] = converted

    for family, phase in (("phase_i_clone", 1), ("republic_plastoid", 2)):
        converted = convert_bbmodel(
            source_model,
            family,
            group_policy=clone_group_policy(phase, False),
            armor=True,
        )
        write_model(ARMOR_MODELS / f"{family}.geo.json", converted)
        if phase == 1:
            copy_rgba(GALAXIES_SOURCE / "Clone Trooper.png", ARMOR_TEXTURES / f"{family}.png")
        else:
            write_501st_texture(converted, ARMOR_TEXTURES / f"{family}.png")
    write_clone_item_icons()


def build_b1_assets() -> None:
    converted = convert_bbmodel(
        FORGE_SOURCE / "Droid.bbmodel",
        "b1_battle_droid",
        cube_policy=b1_cube_policy,
    )
    rig_b1_mechanical_joints(converted)
    # Forge-StarWarsCloneWars ships a hand-upscaled 128x64 texture for the original
    # 64x32 Blockbench UV layout. Scaling UV coordinates keeps every authored pixel aligned.
    scale_model_uvs(converted, 2)
    write_model(ENTITY_MODELS / "b1_battle_droid.geo.json", converted)
    copy_rgba(FORGE_SOURCE / "droid.png", ENTITY_TEXTURES / "b1_battle_droid.png")


def build_mandalorian_assets() -> None:
    source_model = GALAXIES_SOURCE / "PSWG_Mandalorian.bbmodel"
    variants = {
        "mandalorian_warrior": "warrior",
        "mandalorian_marksman": "marksman",
        "mandalorian_heavy": "heavy",
        "mandalorian_clansperson": "clansperson",
    }
    for asset_id, variant in variants.items():
        converted = convert_bbmodel(
            source_model,
            asset_id,
            group_policy=mandalorian_group_policy(variant),
        )
        # The source helmet group uses a horn-editing handle near ground level.
        # GeckoLib needs the head center so helmet motion turns in place.
        set_bone_pivot(converted, "helmet", (0, 24, 0))
        write_model(ENTITY_MODELS / f"{asset_id}.geo.json", converted)
        write_mandalorian_texture(variant, converted, ENTITY_TEXTURES / f"{asset_id}.png")
        if variant == "clansperson":
            adapt_animation_bones(asset_id, {
                "hair": "head",
                "outerwear": "body",
                "utility_belt": "body",
            })
        else:
            adapt_animation_bones(asset_id, {
                "chest_armor": "body",
                "right_gauntlet": "right_arm",
                "left_gauntlet": "left_arm",
                "right_boot": "right_leg",
                "left_boot": "left_leg",
            })


def main() -> None:
    for directory in (ENTITY_MODELS, ENTITY_TEXTURES, ARMOR_MODELS, ARMOR_TEXTURES, ITEM_TEXTURES):
        directory.mkdir(parents=True, exist_ok=True)
    build_clone_assets()
    build_b1_assets()
    build_mandalorian_assets()
    print("Imported 9 NPC sets and 2 wearable armor sets from authorized upstream art")


if __name__ == "__main__":
    main()
