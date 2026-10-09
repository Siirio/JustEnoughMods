import argparse
import colorsys
import io
import json
import random
import shutil
import zipfile
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


PACK_FORMAT = 15
PACK_ICON_NATIVE_SIZE = 64
PACK_ICON_OUTPUT_SIZE = 256
GUI_ATLAS_SIZE = 256
BUTTON_WIDTH = 200
BUTTON_HEIGHT = 20
BUTTON_ROWS = {"disabled": 46, "normal": 66, "hover": 86}
CHECKBOX_ATLAS_SIZE = 64
CHECKBOX_CELL_SIZE = 20
BACKGROUND_TILE_SIZE = 32
CONTACT_SHEET_WIDTH = 1600
CONTACT_SHEET_HEIGHT = 980
CONTACT_MARGIN = 32
CONTACT_COLUMN_WIDTH = 768
CONTACT_TEXTURE_SCALE = 2
NEUTRAL_SATURATION_LIMIT = 0.20
NEUTRAL_RANGE_LIMIT = 24
MINECRAFT_GUI_PREFIX = "assets/minecraft/textures/gui/"
REALMS_GUI_PREFIX = "assets/realms/textures/gui/realms/"
EXCLUDED_PATH_SEGMENT = "/title/"
ZIP_COMPRESSION_LEVEL = 9


@dataclass(frozen=True)
class Theme:
    key: str
    title: str
    description: str
    accent: tuple[int, int, int]
    accent_bright: tuple[int, int, int]
    button_fill: tuple[int, int, int]
    button_hover: tuple[int, int, int]
    button_disabled: tuple[int, int, int]
    button_edge: tuple[int, int, int]
    button_shadow: tuple[int, int, int]
    background: tuple[int, int, int]
    background_alt: tuple[int, int, int]
    background_line: tuple[int, int, int]
    general_stops: tuple[tuple[float, tuple[int, int, int]], ...]
    container_stops: tuple[tuple[float, tuple[int, int, int]], ...]


LIGHT = Theme(
    key="light",
    title="JEM GUI — Sunlit Birch",
    description="§6JEM GUI §f• §eSunlit Birch §7• Java 1.20.1",
    accent=(72, 128, 101),
    accent_bright=(150, 199, 132),
    button_fill=(112, 69, 46),
    button_hover=(151, 88, 47),
    button_disabled=(91, 82, 74),
    button_edge=(201, 143, 81),
    button_shadow=(42, 28, 24),
    background=(222, 207, 166),
    background_alt=(205, 184, 139),
    background_line=(91, 64, 45),
    general_stops=(
        (0.00, (25, 18, 17)),
        (0.18, (62, 40, 31)),
        (0.42, (121, 78, 50)),
        (0.68, (198, 170, 116)),
        (0.84, (228, 213, 174)),
        (1.00, (255, 247, 218)),
    ),
    container_stops=(
        (0.00, (28, 20, 18)),
        (0.18, (70, 46, 34)),
        (0.42, (133, 91, 57)),
        (0.68, (213, 191, 147)),
        (0.84, (235, 222, 188)),
        (1.00, (255, 250, 226)),
    ),
)


DARK = Theme(
    key="dark",
    title="JEM GUI — Moonlit Slate",
    description="§3JEM GUI §f• §bMoonlit Slate §7• Java 1.20.1",
    accent=(65, 139, 150),
    accent_bright=(132, 211, 210),
    button_fill=(37, 46, 53),
    button_hover=(54, 78, 83),
    button_disabled=(47, 49, 52),
    button_edge=(166, 125, 70),
    button_shadow=(8, 11, 14),
    background=(22, 28, 34),
    background_alt=(31, 39, 46),
    background_line=(78, 91, 98),
    general_stops=(
        (0.00, (7, 10, 12)),
        (0.18, (18, 24, 29)),
        (0.42, (39, 49, 57)),
        (0.68, (72, 86, 94)),
        (0.84, (111, 128, 133)),
        (1.00, (202, 216, 214)),
    ),
    container_stops=(
        (0.00, (8, 11, 14)),
        (0.18, (24, 31, 36)),
        (0.42, (57, 68, 74)),
        (0.68, (135, 146, 150)),
        (0.84, (179, 188, 189)),
        (1.00, (232, 237, 232)),
    ),
)


THEMES = (LIGHT, DARK)


def interpolate_channel(start: int, end: int, amount: float) -> int:
    return round(start + (end - start) * amount)


def interpolate_color(start: tuple[int, int, int], end: tuple[int, int, int], amount: float) -> tuple[int, int, int]:
    return tuple(interpolate_channel(start[index], end[index], amount) for index in range(3))


def sample_stops(value: float, stops: tuple[tuple[float, tuple[int, int, int]], ...]) -> tuple[int, int, int]:
    for index in range(1, len(stops)):
        previous_position, previous_color = stops[index - 1]
        next_position, next_color = stops[index]
        if value <= next_position:
            distance = next_position - previous_position
            amount = 0.0 if distance == 0.0 else (value - previous_position) / distance
            return interpolate_color(previous_color, next_color, max(0.0, min(1.0, amount)))
    return stops[-1][1]


def is_container_path(resource_path: str) -> bool:
    return "/container/" in resource_path or "/advancements/" in resource_path or resource_path.endswith("book.png")


def recolor_image(source: Image.Image, theme: Theme, resource_path: str) -> Image.Image:
    source = source.convert("RGBA")
    result = Image.new("RGBA", source.size)
    source_pixels = source.load()
    result_pixels = result.load()
    stops = theme.container_stops if is_container_path(resource_path) else theme.general_stops
    for y in range(source.height):
        for x in range(source.width):
            red, green, blue, alpha = source_pixels[x, y]
            if alpha == 0:
                result_pixels[x, y] = (red, green, blue, alpha)
                continue
            maximum = max(red, green, blue)
            minimum = min(red, green, blue)
            _, saturation, value = colorsys.rgb_to_hsv(red / 255.0, green / 255.0, blue / 255.0)
            if saturation > NEUTRAL_SATURATION_LIMIT and maximum - minimum > NEUTRAL_RANGE_LIMIT:
                result_pixels[x, y] = (red, green, blue, alpha)
                continue
            luminance = (red * 0.2126 + green * 0.7152 + blue * 0.0722) / 255.0
            mapped = sample_stops((luminance + value) / 2.0, stops)
            result_pixels[x, y] = (*mapped, alpha)
    return result


def create_button(theme: Theme, state: str) -> Image.Image:
    fill = theme.button_fill
    edge = theme.button_edge
    if state == "hover":
        fill = theme.button_hover
        edge = theme.accent_bright
    elif state == "disabled":
        fill = theme.button_disabled
        edge = interpolate_color(theme.button_disabled, theme.button_shadow, 0.45)
    image = Image.new("RGBA", (BUTTON_WIDTH, BUTTON_HEIGHT))
    draw = ImageDraw.Draw(image)
    draw.rectangle((1, 0, BUTTON_WIDTH - 2, BUTTON_HEIGHT - 1), fill=theme.button_shadow)
    draw.rectangle((0, 1, BUTTON_WIDTH - 1, BUTTON_HEIGHT - 2), fill=theme.button_shadow)
    draw.rectangle((2, 1, BUTTON_WIDTH - 3, BUTTON_HEIGHT - 3), fill=edge)
    draw.rectangle((2, 3, BUTTON_WIDTH - 3, BUTTON_HEIGHT - 2), fill=interpolate_color(edge, theme.button_shadow, 0.65))
    draw.rectangle((3, 3, BUTTON_WIDTH - 4, BUTTON_HEIGHT - 4), fill=fill)
    draw.line((4, 3, BUTTON_WIDTH - 5, 3), fill=interpolate_color(fill, (255, 255, 255), 0.22))
    draw.line((4, BUTTON_HEIGHT - 4, BUTTON_WIDTH - 5, BUTTON_HEIGHT - 4), fill=interpolate_color(fill, theme.button_shadow, 0.55))
    for x in range(10, BUTTON_WIDTH - 8, 19):
        shade = interpolate_color(fill, theme.button_shadow, 0.11 if state != "disabled" else 0.06)
        draw.point((x, 7 + x % 5), fill=shade)
    return image


def replace_widgets(image: Image.Image, theme: Theme) -> Image.Image:
    result = image.convert("RGBA")
    for state, top in BUTTON_ROWS.items():
        result.alpha_composite(create_button(theme, state), (0, top))
    return result


def create_checkbox_cell(theme: Theme, checked: bool, hovered: bool) -> Image.Image:
    image = Image.new("RGBA", (CHECKBOX_CELL_SIZE, CHECKBOX_CELL_SIZE))
    draw = ImageDraw.Draw(image)
    edge = theme.accent_bright if hovered else theme.button_edge
    fill = theme.button_hover if hovered else theme.button_fill
    draw.rectangle((2, 2, CHECKBOX_CELL_SIZE - 3, CHECKBOX_CELL_SIZE - 3), fill=theme.button_shadow)
    draw.rectangle((3, 3, CHECKBOX_CELL_SIZE - 4, CHECKBOX_CELL_SIZE - 4), fill=edge)
    draw.rectangle((5, 5, CHECKBOX_CELL_SIZE - 6, CHECKBOX_CELL_SIZE - 6), fill=fill)
    if checked:
        check_color = (239, 247, 208) if theme.key == LIGHT.key else theme.accent_bright
        points = ((5, 10), (8, 13), (14, 6), (16, 7), (8, 16), (3, 11))
        draw.line(points, fill=theme.button_shadow, width=3, joint="curve")
        draw.line(points, fill=check_color, width=2, joint="curve")
    return image


def create_checkbox_atlas(theme: Theme) -> Image.Image:
    atlas = Image.new("RGBA", (CHECKBOX_ATLAS_SIZE, CHECKBOX_ATLAS_SIZE))
    atlas.alpha_composite(create_checkbox_cell(theme, False, False), (0, 0))
    atlas.alpha_composite(create_checkbox_cell(theme, False, True), (CHECKBOX_CELL_SIZE, 0))
    atlas.alpha_composite(create_checkbox_cell(theme, True, False), (0, CHECKBOX_CELL_SIZE))
    atlas.alpha_composite(create_checkbox_cell(theme, True, True), (CHECKBOX_CELL_SIZE, CHECKBOX_CELL_SIZE))
    return atlas


def create_background_tile(theme: Theme) -> Image.Image:
    image = Image.new("RGBA", (BACKGROUND_TILE_SIZE, BACKGROUND_TILE_SIZE), (*theme.background, 255))
    draw = ImageDraw.Draw(image)
    if theme.key == LIGHT.key:
        plank_height = 8
        for y in range(0, BACKGROUND_TILE_SIZE, plank_height):
            draw.line((0, y, BACKGROUND_TILE_SIZE - 1, y), fill=(*theme.background_line, 255))
            draw.line((0, y + 1, BACKGROUND_TILE_SIZE - 1, y + 1), fill=(*interpolate_color(theme.background, (255, 255, 255), 0.18), 255))
        draw.line((15, 0, 15, 7), fill=(*theme.background_line, 255))
        draw.line((7, 8, 7, 15), fill=(*theme.background_line, 255))
        draw.line((23, 16, 23, 23), fill=(*theme.background_line, 255))
        draw.line((11, 24, 11, 31), fill=(*theme.background_line, 255))
        for x, y in ((4, 4), (20, 12), (29, 20), (16, 28)):
            draw.point((x, y), fill=(*theme.background_alt, 255))
            draw.point(((x + 1) % BACKGROUND_TILE_SIZE, y), fill=(*theme.background_line, 255))
    else:
        brick_height = 8
        for y in range(0, BACKGROUND_TILE_SIZE, brick_height):
            draw.line((0, y, BACKGROUND_TILE_SIZE - 1, y), fill=(*theme.background_line, 255))
            draw.line((0, y + 1, BACKGROUND_TILE_SIZE - 1, y + 1), fill=(*interpolate_color(theme.background, (255, 255, 255), 0.07), 255))
            offset = 8 if (y // brick_height) % 2 else 16
            draw.line((offset, y + 2, offset, min(y + brick_height - 1, BACKGROUND_TILE_SIZE - 1)), fill=(*theme.background_line, 255))
        for x, y in ((5, 5), (24, 13), (12, 21), (28, 29)):
            draw.point((x, y), fill=(*theme.accent, 255))
    return image


def create_pack_icon(theme: Theme) -> Image.Image:
    image = Image.new("RGBA", (PACK_ICON_NATIVE_SIZE, PACK_ICON_NATIVE_SIZE), (*theme.background, 255))
    draw = ImageDraw.Draw(image)
    draw.rectangle((4, 4, 59, 59), fill=theme.button_shadow)
    draw.rectangle((6, 6, 57, 57), fill=theme.button_edge)
    draw.rectangle((9, 9, 54, 54), fill=theme.button_fill)
    draw.rectangle((11, 11, 52, 52), fill=theme.background)
    if theme.key == LIGHT.key:
        draw.ellipse((19, 19, 44, 44), fill=theme.accent_bright)
        draw.ellipse((24, 24, 39, 39), fill=(250, 226, 112))
        for start, end in (((31, 14), (31, 18)), ((31, 45), (31, 49)), ((14, 31), (18, 31)), ((45, 31), (49, 31))):
            draw.line((*start, *end), fill=(250, 226, 112), width=2)
    else:
        draw.ellipse((18, 17, 45, 45), fill=theme.accent_bright)
        draw.ellipse((27, 13, 49, 40), fill=theme.background)
        draw.point((19, 20), fill=theme.button_edge)
        draw.point((42, 47), fill=theme.button_edge)
        draw.point((17, 43), fill=theme.accent_bright)
    return image.resize((PACK_ICON_OUTPUT_SIZE, PACK_ICON_OUTPUT_SIZE), Image.Resampling.NEAREST)


def should_include(entry_name: str) -> bool:
    is_gui = entry_name.startswith(MINECRAFT_GUI_PREFIX) or entry_name.startswith(REALMS_GUI_PREFIX)
    return is_gui and entry_name.endswith(".png") and EXCLUDED_PATH_SEGMENT not in entry_name


def prepare_output_directory(path: Path, parent: Path) -> None:
    resolved_path = path.resolve()
    resolved_parent = parent.resolve()
    if resolved_path.parent != resolved_parent:
        raise ValueError(f"Refusing to replace output outside {resolved_parent}: {resolved_path}")
    if resolved_path.exists():
        shutil.rmtree(resolved_path)
    resolved_path.mkdir(parents=True)


def write_pack_metadata(pack_directory: Path, theme: Theme) -> None:
    metadata = {"pack": {"pack_format": PACK_FORMAT, "description": theme.description}}
    (pack_directory / "pack.mcmeta").write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    create_pack_icon(theme).save(pack_directory / "pack.png")


def build_theme(game_archive: zipfile.ZipFile, pack_directory: Path, theme: Theme) -> None:
    for entry_name in sorted(name for name in game_archive.namelist() if should_include(name)):
        with game_archive.open(entry_name) as source_file:
            source = Image.open(io.BytesIO(source_file.read())).convert("RGBA")
        transformed = recolor_image(source, theme, entry_name)
        if entry_name.endswith("/widgets.png"):
            transformed = replace_widgets(transformed, theme)
        if entry_name.endswith("/checkbox.png"):
            transformed = create_checkbox_atlas(theme)
        if entry_name.endswith("/options_background.png") or entry_name.endswith("/light_dirt_background.png"):
            transformed = create_background_tile(theme)
        destination = pack_directory / entry_name
        destination.parent.mkdir(parents=True, exist_ok=True)
        transformed.save(destination, optimize=True)
    write_pack_metadata(pack_directory, theme)


def archive_pack(pack_directory: Path, destination: Path) -> None:
    if destination.exists():
        destination.unlink()
    with zipfile.ZipFile(destination, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=ZIP_COMPRESSION_LEVEL) as archive:
        for path in sorted(pack_directory.rglob("*")):
            if path.is_file():
                archive.write(path, path.relative_to(pack_directory).as_posix())


def tile_background(tile: Image.Image, size: tuple[int, int]) -> Image.Image:
    result = Image.new("RGBA", size)
    for y in range(0, size[1], tile.height):
        for x in range(0, size[0], tile.width):
            result.alpha_composite(tile, (x, y))
    return result


def fit_preview(image: Image.Image, maximum_size: tuple[int, int]) -> Image.Image:
    copy = image.copy()
    copy.thumbnail(maximum_size, Image.Resampling.NEAREST)
    return copy


def create_contact_column(pack_directory: Path, theme: Theme) -> Image.Image:
    column = Image.new("RGBA", (CONTACT_COLUMN_WIDTH, CONTACT_SHEET_HEIGHT - CONTACT_MARGIN * 2), (*theme.background, 255))
    draw = ImageDraw.Draw(column)
    font = ImageFont.load_default()
    draw.text((20, 18), theme.title, fill=(245, 245, 240), font=font, stroke_width=2, stroke_fill=theme.button_shadow)
    icon = Image.open(pack_directory / "pack.png").convert("RGBA").resize((112, 112), Image.Resampling.NEAREST)
    column.alpha_composite(icon, (20, 50))
    background_path = pack_directory / MINECRAFT_GUI_PREFIX / "options_background.png"
    background_tile = Image.open(background_path).convert("RGBA")
    background_preview = tile_background(background_tile, (580, 112))
    column.alpha_composite(background_preview, (156, 50))
    widgets = Image.open(pack_directory / MINECRAFT_GUI_PREFIX / "widgets.png").convert("RGBA")
    for index, state in enumerate(("normal", "hover", "disabled")):
        top = BUTTON_ROWS[state]
        button = widgets.crop((0, top, BUTTON_WIDTH, top + BUTTON_HEIGHT)).resize((BUTTON_WIDTH * 2, BUTTON_HEIGHT * 2), Image.Resampling.NEAREST)
        column.alpha_composite(button, (20, 184 + index * 48))
    checkbox = Image.open(pack_directory / MINECRAFT_GUI_PREFIX / "checkbox.png").convert("RGBA")
    checkbox = checkbox.crop((0, 0, CHECKBOX_CELL_SIZE * 2, CHECKBOX_CELL_SIZE * 2)).resize((160, 160), Image.Resampling.NEAREST)
    column.alpha_composite(checkbox, (454, 174))
    preview_paths = (
        "container/inventory.png",
        "container/generic_54.png",
        "container/crafting_table.png",
        "container/furnace.png",
        "container/villager2.png",
        "advancements/window.png",
    )
    positions = ((20, 350), (390, 350), (20, 610), (270, 610), (520, 610), (390, 470))
    sizes = ((340, 240), (340, 240), (220, 220), (220, 220), (220, 220), (340, 150))
    for relative_path, position, size in zip(preview_paths, positions, sizes):
        image_path = pack_directory / MINECRAFT_GUI_PREFIX / relative_path
        if image_path.exists():
            preview = fit_preview(Image.open(image_path).convert("RGBA"), size)
            column.alpha_composite(preview, position)
    return column


def create_contact_sheet(output_root: Path) -> Path:
    sheet = Image.new("RGBA", (CONTACT_SHEET_WIDTH, CONTACT_SHEET_HEIGHT), (12, 15, 18, 255))
    for index, theme in enumerate(THEMES):
        column = create_contact_column(output_root / theme.key, theme)
        x = CONTACT_MARGIN + index * (CONTACT_COLUMN_WIDTH + CONTACT_MARGIN)
        sheet.alpha_composite(column, (x, CONTACT_MARGIN))
    destination = output_root / "JEM_Light_Dark_GUI_contact_sheet.png"
    sheet.save(destination, optimize=True)
    return destination


def build(game_jar: Path, output_root: Path, resourcepack_directory: Path) -> Path:
    output_root = output_root.resolve()
    output_root.mkdir(parents=True, exist_ok=True)
    resourcepack_directory = resourcepack_directory.resolve()
    resourcepack_directory.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(game_jar.resolve()) as game_archive:
        for theme in THEMES:
            pack_directory = output_root / theme.key
            prepare_output_directory(pack_directory, output_root)
            build_theme(game_archive, pack_directory, theme)
            archive_pack(pack_directory, resourcepack_directory / f"JEM_GUI_{theme.title.split('—')[-1].strip().replace(' ', '_')}_1.20.1.zip")
    return create_contact_sheet(output_root)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("game_jar", type=Path)
    parser.add_argument("output_root", type=Path)
    parser.add_argument("resourcepack_directory", type=Path)
    arguments = parser.parse_args()
    print(build(arguments.game_jar, arguments.output_root, arguments.resourcepack_directory))
