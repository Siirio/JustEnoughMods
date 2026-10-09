import argparse
import shutil
from pathlib import Path

from PIL import Image, ImageEnhance, ImageFilter


SOURCE_NAMES = {
    "checkbox_unchecked.png": "ChatGPT Image 31 авг. 2026 г., 00_34_29 (1).png",
    "checkbox_checked.png": "ChatGPT Image 31 авг. 2026 г., 00_34_29 (2).png",
    "slider_track_source.png": "ChatGPT Image 31 авг. 2026 г., 00_34_30 (3).png",
    "slider_handle_source.png": "ChatGPT Image 31 авг. 2026 г., 00_34_30 (4).png",
    "default_background.png": "ChatGPT Image 31 авг. 2026 г., 00_34_31 (5).png",
    "input_normal.png": "ChatGPT Image 31 авг. 2026 г., 00_34_32 (6).png",
    "input_hover.png": "ChatGPT Image 31 авг. 2026 г., 00_34_32 (7).png",
    "input_focused.png": "ChatGPT Image 31 авг. 2026 г., 00_34_33 (8).png",
    "large_panel.png": "ChatGPT Image 31 авг. 2026 г., 00_34_33 (9).png",
}
CHECKBOX_ATLAS_SIZE = (64, 64)
CHECKBOX_SIZE = (20, 20)
SLIDER_TRACK_SIZE = (200, 20)
SLIDER_HANDLE_SIZE = (10, 20)
BACKGROUND_TILE_SIZE = (32, 32)
PACK_ICON_SIZE = (256, 256)


def prepare_fancymenu_assets(source: Path, destination: Path) -> None:
    destination.mkdir(parents=True, exist_ok=True)
    for target_name, source_name in SOURCE_NAMES.items():
        shutil.copy2(source / source_name, destination / target_name)

    track = Image.open(destination / "slider_track_source.png").convert("RGBA")
    track = track.transpose(Image.Transpose.ROTATE_90).resize(SLIDER_TRACK_SIZE, Image.Resampling.LANCZOS)
    track.save(destination / "slider_track.png")

    handle = Image.open(destination / "slider_handle_source.png").convert("RGBA")
    handle = handle.resize(SLIDER_HANDLE_SIZE, Image.Resampling.LANCZOS)
    handle.save(destination / "slider_handle.png")


def build_checkbox(source: Path, output: Path) -> None:
    unchecked = Image.open(source / SOURCE_NAMES["checkbox_unchecked.png"]).convert("RGBA")
    checked = Image.open(source / SOURCE_NAMES["checkbox_checked.png"]).convert("RGBA")
    unchecked = unchecked.resize(CHECKBOX_SIZE, Image.Resampling.LANCZOS)
    checked = checked.resize(CHECKBOX_SIZE, Image.Resampling.LANCZOS)
    unchecked_hover = ImageEnhance.Brightness(unchecked).enhance(1.16)
    checked_hover = ImageEnhance.Brightness(checked).enhance(1.16)
    atlas = Image.new("RGBA", CHECKBOX_ATLAS_SIZE)
    atlas.alpha_composite(unchecked, (0, 0))
    atlas.alpha_composite(unchecked_hover, (20, 0))
    atlas.alpha_composite(checked, (0, 20))
    atlas.alpha_composite(checked_hover, (20, 20))
    atlas.save(output / "checkbox.png")


def build_background(source: Path, output: Path) -> Image.Image:
    background = Image.open(source / SOURCE_NAMES["default_background.png"]).convert("RGB")
    repeated_section_size = min(background.width, background.height) // 3
    left = (background.width - repeated_section_size) // 2
    top = (background.height - repeated_section_size) // 2
    tile = background.crop((left, top, left + repeated_section_size, top + repeated_section_size))
    tile = tile.resize(BACKGROUND_TILE_SIZE, Image.Resampling.LANCZOS).convert("RGBA")
    tile.save(output / "options_background.png")
    tile.save(output / "light_dirt_background.png")
    return background


def build_pack_icon(background: Image.Image, source: Path, project: Path) -> None:
    icon = background.resize(PACK_ICON_SIZE, Image.Resampling.LANCZOS).convert("RGBA")
    checkbox = Image.open(source / SOURCE_NAMES["checkbox_checked.png"]).convert("RGBA")
    checkbox.thumbnail((150, 150), Image.Resampling.LANCZOS)
    shadow = Image.new("RGBA", PACK_ICON_SIZE)
    shadow.alpha_composite(checkbox, ((PACK_ICON_SIZE[0] - checkbox.width) // 2 + 4, 57))
    shadow = shadow.filter(ImageFilter.GaussianBlur(5))
    shadow.putalpha(shadow.getchannel("A").point(lambda value: value // 2))
    icon.alpha_composite(shadow)
    icon.alpha_composite(checkbox, ((PACK_ICON_SIZE[0] - checkbox.width) // 2, 53))
    icon.save(project / "pack.png")


def build(source: Path, fancy_destination: Path, project: Path) -> None:
    gui_output = project / "assets" / "minecraft" / "textures" / "gui"
    gui_output.mkdir(parents=True, exist_ok=True)
    prepare_fancymenu_assets(source, fancy_destination)
    build_checkbox(source, gui_output)
    background = build_background(source, gui_output)
    build_pack_icon(background, source, project)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("fancy_destination", type=Path)
    parser.add_argument("project", type=Path)
    arguments = parser.parse_args()
    build(arguments.source, arguments.fancy_destination, arguments.project)
