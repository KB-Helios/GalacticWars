from __future__ import annotations

import hashlib
import json
import math
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import fetch_authorized_character_sources as fetcher
from tools import import_authorized_character_assets as importer


SOURCE_ROOT = ROOT / "tools/source_art/authorized_upstream"
INVENTORY = SOURCE_ROOT / "source_inventory.json"
PROJECT_ASSETS = ROOT / "src/main/resources/assets/galacticwars"
CURATED_RECRUITS = {
    "senate_commando": (128, 128, 22),
    "republic_honor_guard": (128, 128, 21),
    "b1_security_droid": (128, 64, 23),
    "togruta_civilian": (256, 256, 16),
    "hutt_enforcer": (256, 256, 18),
    "smuggler": (256, 256, 16),
    "hutt_civilian": (256, 256, 17),
    "separatist_technician": (128, 64, 23),
}


def inventory_entries() -> list[dict]:
    inventory = json.loads(INVENTORY.read_text(encoding="utf-8"))
    return [entry for source in inventory["sources"] for entry in source["files"]]


def file_hash(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def tree_hashes(root: Path) -> dict[str, str]:
    return {
        path.relative_to(root).as_posix(): file_hash(path)
        for path in sorted(root.rglob("*"))
        if path.is_file()
    }


def geometry(asset_root: Path, asset_id: str) -> dict:
    model_path = asset_root / "geckolib/models/entity" / f"{asset_id}.geo.json"
    return json.loads(model_path.read_text(encoding="utf-8"))["minecraft:geometry"][0]


def face_rect(face: dict) -> tuple[int, int, int, int]:
    u, v = face["uv"]
    width, height = face["uv_size"]
    return (
        int(min(u, u + width)),
        int(min(v, v + height)),
        int(max(u, u + width)),
        int(max(v, v + height)),
    )


def box_face_rects(uv: list[int], size: list[float]) -> list[tuple[int, int, int, int]]:
    u, v = uv
    width, height, depth = (max(1, int(math.ceil(value))) for value in size)
    return [
        (u + depth, v, u + depth + width, v + depth),
        (u + depth + width, v, u + depth + 2 * width, v + depth),
        (u, v + depth, u + depth, v + depth + height),
        (u + depth, v + depth, u + depth + width, v + depth + height),
        (u + depth + width, v + depth, u + 2 * depth + width, v + depth + height),
        (u + 2 * depth + width, v + depth, u + 2 * depth + 2 * width, v + depth + height),
    ]


class AuthorizedCharacterAssetTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.temporary_directory = tempfile.TemporaryDirectory()
        cls.asset_root = Path(cls.temporary_directory.name) / "assets"
        shutil.copytree(
            PROJECT_ASSETS / "geckolib/animations/entity",
            cls.asset_root / "geckolib/animations/entity",
        )
        cls.source_hashes = tree_hashes(SOURCE_ROOT)
        importer.import_assets(cls.asset_root)

    @classmethod
    def tearDownClass(cls) -> None:
        cls.temporary_directory.cleanup()

    def test_inventory_hashes_and_privacy_sanitization(self) -> None:
        inventory = json.loads(INVENTORY.read_text(encoding="utf-8"))
        self.assertEqual(2, inventory["schema_version"])
        entries = inventory_entries()
        self.assertEqual(43, len(entries))
        self.assertEqual(len(entries), len({entry["destination"] for entry in entries}))

        for entry in entries:
            self.assertRegex(entry["upstream_sha256"], r"^[0-9a-f]{64}$")
            self.assertRegex(entry["vendored_sha256"], r"^[0-9a-f]{64}$")
            self.assertTrue(entry["usage"])
            vendored = SOURCE_ROOT / entry["destination"]
            self.assertEqual(entry["vendored_sha256"], file_hash(vendored))
            if entry.get("sanitizer") == "blockbench_texture_paths":
                self.assertNotEqual(entry["upstream_sha256"], entry["vendored_sha256"])
                model = json.loads(vendored.read_text(encoding="utf-8"))
                for texture in model.get("textures", []):
                    self.assertNotIn("path", texture)
                    self.assertNotIn("relative_path", texture)

    def test_fetch_fails_before_write_and_then_sanitizes(self) -> None:
        model_bytes = json.dumps({
            "textures": [{
                "name": "atlas",
                "path": "C:\\Users\\artist\\Desktop\\atlas.png",
                "relative_path": "../../atlas.png",
                "source": "data:image/png;base64,AA==",
            }],
            "elements": [],
        }).encode("utf-8")
        texture_bytes = b"texture"
        model_entry = {
            "source_path": "model.bbmodel",
            "destination": "repo/model.bbmodel",
            "upstream_sha256": fetcher.sha256(model_bytes),
            "vendored_sha256": fetcher.sha256(fetcher.sanitize_blockbench_payload(model_bytes)),
            "sanitizer": "blockbench_texture_paths",
            "usage": "test model",
        }
        texture_entry = {
            "source_path": "texture.png",
            "destination": "repo/texture.png",
            "upstream_sha256": "0" * 64,
            "vendored_sha256": fetcher.sha256(texture_bytes),
            "usage": "test texture",
        }
        inventory = {
            "schema_version": 2,
            "sources": [{
                "repository": "owner/repository",
                "revision": "revision",
                "files": [model_entry, texture_entry],
            }],
        }
        payloads = {"model.bbmodel": model_bytes, "texture.png": texture_bytes}

        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            inventory_path = root / "inventory.json"
            inventory_path.write_text(json.dumps(inventory), encoding="utf-8")

            def download(url: str) -> bytes:
                return payloads[url.rsplit("/", 1)[-1]]

            with self.assertRaises(ValueError):
                fetcher.fetch_sources(inventory_path, root / "sources", download)
            self.assertFalse((root / "sources").exists(), "a failed batch must not write any source")

            texture_entry["upstream_sha256"] = fetcher.sha256(texture_bytes)
            inventory_path.write_text(json.dumps(inventory), encoding="utf-8")
            self.assertEqual(2, fetcher.fetch_sources(inventory_path, root / "sources", download))
            sanitized = json.loads((root / "sources/repo/model.bbmodel").read_text(encoding="utf-8"))
            self.assertNotIn("path", sanitized["textures"][0])
            self.assertNotIn("relative_path", sanitized["textures"][0])
            self.assertEqual("atlas", sanitized["textures"][0]["name"])
            self.assertIn("source", sanitized["textures"][0])

    def test_bounded_uv_rectangles(self) -> None:
        image = Image.new("RGBA", (16, 8))
        self.assertEqual((0, 0, 16, 8), importer.bounded_face_rect(
            image, {"uv": [-5, -2, 20, 12]}))
        self.assertEqual((3, 2, 9, 7), importer.bounded_face_rect(
            image, {"uv": [9, 7, 3, 2]}))
        self.assertIsNone(importer.bounded_face_rect(image, {"uv": [20, 1, 24, 5]}))

    def test_import_is_deterministic_and_source_immutable(self) -> None:
        first = tree_hashes(self.asset_root)
        importer.import_assets(self.asset_root)
        self.assertEqual(first, tree_hashes(self.asset_root))
        self.assertEqual(self.source_hashes, tree_hashes(SOURCE_ROOT))

    def test_curated_geometry_contracts_and_rigs(self) -> None:
        for asset_id, (width, height, minimum_cubes) in CURATED_RECRUITS.items():
            model = geometry(self.asset_root, asset_id)
            self.assertEqual(width, model["description"]["texture_width"], asset_id)
            self.assertEqual(height, model["description"]["texture_height"], asset_id)
            cube_count = sum(len(bone.get("cubes", [])) for bone in model["bones"])
            self.assertGreaterEqual(cube_count, minimum_cubes, asset_id)

        for asset_id in ("republic_honor_guard", "togruta_civilian"):
            bones = {bone["name"]: bone for bone in geometry(self.asset_root, asset_id)["bones"]}
            self.assertEqual([0, 24, 0], bones["head"]["pivot"], asset_id)

        trandoshan = {bone["name"]: bone for bone in geometry(
            self.asset_root, "hutt_enforcer")["bones"]}
        for limb in ("right_arm", "left_arm", "right_leg", "left_leg"):
            self.assertIn(limb, trandoshan)
            self.assertTrue(trandoshan[limb].get("cubes"), limb)
        self.assertEqual("right_arm", trandoshan["rightsleeve"]["parent"])
        self.assertEqual("left_arm", trandoshan["leftsleeve"]["parent"])
        self.assertEqual("right_leg", trandoshan["rightpant"]["parent"])
        self.assertEqual("left_leg", trandoshan["leftpant"]["parent"])
        self.assertEqual("right_arm", trandoshan["RightHandItem"]["parent"])
        self.assertEqual("left_arm", trandoshan["LeftHandItem"]["parent"])

    def test_uv_bounds_partitions_and_required_face_opacity(self) -> None:
        composites = {"togruta_civilian", "smuggler", "hutt_civilian"}
        texture_models = {asset_id: asset_id for asset_id in CURATED_RECRUITS}
        texture_models.update({
            "clone_trooper_commander": "clone_trooper",
            "arc_trooper_commander": "arc_trooper",
            "b1_battle_droid_commander": "b1_battle_droid",
        })
        for asset_id, model_id in texture_models.items():
            model = geometry(self.asset_root, model_id)
            texture_path = self.asset_root / "textures/entity" / f"{asset_id}.png"
            with Image.open(texture_path) as source:
                texture = source.convert("RGBA")
            commander_variant = asset_id.endswith("_commander") and asset_id != "b1_battle_droid_commander"
            for bone in model["bones"]:
                for cube in bone.get("cubes", []):
                    uv = cube.get("uv")
                    if isinstance(uv, dict):
                        rectangles = [face_rect(face) for face in uv.values()]
                    else:
                        self.assertIsInstance(uv, list, f"{asset_id} {bone['name']} UV mapping")
                        rectangles = box_face_rects(uv, cube["size"])
                    for left, top, right, bottom in rectangles:
                        self.assertGreater(right, left, asset_id)
                        self.assertGreater(bottom, top, asset_id)
                        self.assertGreaterEqual(left, 0, asset_id)
                        self.assertGreaterEqual(top, 0, asset_id)
                        self.assertLessEqual(right, texture.width, asset_id)
                        self.assertLessEqual(bottom, texture.height, asset_id)
                        if asset_id in composites and isinstance(uv, dict):
                            self.assertTrue(
                                right <= 96 or left >= 106,
                                f"{asset_id} face overlaps reserved body/head atlas regions",
                            )
                        if not commander_variant:
                            for y in range(top, bottom):
                                for x in range(left, right):
                                    self.assertEqual(
                                        255,
                                        texture.getpixel((x, y))[3],
                                        f"{asset_id} transparent required face pixel {x},{y}",
                                    )
            if commander_variant:
                soldier_id = asset_id.removesuffix("_commander")
                with Image.open(self.asset_root / "textures/entity" / f"{soldier_id}.png") as source:
                    soldier = source.convert("RGBA")
                self.assertEqual(
                    soldier.getchannel("A").tobytes(),
                    texture.getchannel("A").tobytes(),
                    f"{asset_id} must preserve the exact soldier alpha mask",
                )


if __name__ == "__main__":
    unittest.main()
