import argparse
import io
import zipfile
from pathlib import Path

from PIL import Image, ImageEnhance, ImageFilter


ATLAS_SIZE = (256, 256)
BUTTON_SIZE = (200, 20)
BUTTON_ROWS = {"button_disabled.png": 46, "button.png": 66, "button_highlighted.png": 86}
FACE_SIZE = 1024
LOGO_CANVAS_SIZE = (1024, 256)
LOGO_CONTENT_SIZE = (1024, 176)
EDITION_SIZE = (512, 64)
OVERLAY_SIZE = (16, 128)
PACK_ICON_SIZE = (256, 256)


def build(instance: Path, project: Path) -> None:
    fancy_assets = instance / "config" / "fancymenu" / "assets"
    background_path = fancy_assets / "just_enough_mods" / "2djembg.png"
    logo_path = fancy_assets / "just_enough_mods" / "2djem.png"
    buttons_path = fancy_assets / "buttons" / "wanderwood"
    gui_path = project / "assets" / "minecraft" / "textures" / "gui"
    title_path = gui_path / "title"
    panorama_path = title_path / "background"
    panorama_path.mkdir(parents=True, exist_ok=True)

    with zipfile.ZipFile(instance / "JustEnoughMods.jar") as game_jar:
        widgets_data = game_jar.read("assets/minecraft/textures/gui/widgets.png")

    widgets = Image.open(io.BytesIO(widgets_data)).convert("RGBA")
    if widgets.size != ATLAS_SIZE:
        raise ValueError(f"Unexpected widgets atlas size: {widgets.size}")
    for filename, top in BUTTON_ROWS.items():
        button = Image.open(buttons_path / filename).convert("RGBA")
        if button.size != BUTTON_SIZE:
            raise ValueError(f"Unexpected button size for {filename}: {button.size}")
        widgets.alpha_composite(button, (0, top))
    widgets.save(gui_path / "widgets.png")

    source_logo = Image.open(logo_path).convert("RGBA")
    logo = Image.new("RGBA", LOGO_CANVAS_SIZE)
    compressed_logo = source_logo.resize(LOGO_CONTENT_SIZE, Image.Resampling.LANCZOS)
    logo.alpha_composite(compressed_logo, (0, 0))
    logo.save(title_path / "minecraft.png")
    Image.new("RGBA", EDITION_SIZE).save(title_path / "edition.png")

    background = Image.open(background_path).convert("RGB")
    crop_height = background.width // 2
    crop_top = max((background.height - crop_height) // 2, 0)
    wide_background = background.crop((0, crop_top, background.width, crop_top + crop_height))
    wide_background = wide_background.resize((FACE_SIZE * 2, FACE_SIZE), Image.Resampling.LANCZOS)
    strip = Image.new("RGB", (FACE_SIZE * 4, FACE_SIZE))
    strip.paste(wide_background, (0, 0))
    strip.paste(wide_background.transpose(Image.Transpose.FLIP_LEFT_RIGHT), (FACE_SIZE * 2, 0))
    strip = Image.fromarray(__import__("numpy").roll(__import__("numpy").array(strip), -FACE_SIZE // 2, axis=1))
    for index in range(4):
        face = strip.crop((index * FACE_SIZE, 0, (index + 1) * FACE_SIZE, FACE_SIZE))
        face.save(panorama_path / f"panorama_{index}.png")

    sky = background.crop((0, 0, background.width, max(background.height // 2, 1)))
    sky = sky.resize((FACE_SIZE, FACE_SIZE), Image.Resampling.LANCZOS).filter(ImageFilter.GaussianBlur(18))
    sky = ImageEnhance.Brightness(sky).enhance(1.08)
    sky.save(panorama_path / "panorama_4.png")
    ground = background.crop((0, background.height // 2, background.width, background.height))
    ground = ground.resize((FACE_SIZE, FACE_SIZE), Image.Resampling.LANCZOS).filter(ImageFilter.GaussianBlur(18))
    ground = ImageEnhance.Brightness(ground).enhance(0.82)
    ground.save(panorama_path / "panorama_5.png")
    Image.new("RGBA", OVERLAY_SIZE).save(panorama_path / "panorama_overlay.png")

    pack_icon = background.crop(((background.width - background.height) // 2, 0, (background.width + background.height) // 2, background.height))
    pack_icon = pack_icon.resize(PACK_ICON_SIZE, Image.Resampling.LANCZOS).convert("RGBA")
    icon_logo = source_logo.copy()
    icon_logo.thumbnail((232, 82), Image.Resampling.LANCZOS)
    shadow = Image.new("RGBA", PACK_ICON_SIZE)
    shadow.alpha_composite(icon_logo, ((PACK_ICON_SIZE[0] - icon_logo.width) // 2 + 3, 21))
    shadow = shadow.filter(ImageFilter.GaussianBlur(4))
    shadow.putalpha(shadow.getchannel("A").point(lambda value: value // 2))
    pack_icon.alpha_composite(shadow)
    pack_icon.alpha_composite(icon_logo, ((PACK_ICON_SIZE[0] - icon_logo.width) // 2, 18))
    pack_icon.save(project / "pack.png")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("instance", type=Path)
    parser.add_argument("project", type=Path)
    arguments = parser.parse_args()
    build(arguments.instance, arguments.project)
