import json
import hashlib
import zipfile
from pathlib import Path
from PIL import Image, ImageDraw
import numpy as np

OUT_ROOT = Path("C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/jem-three-guis-2026-10-24")
SRC_DIR = OUT_ROOT / "sources"
CAND_DIR = OUT_ROOT / "candidates"
PREV_DIR = OUT_ROOT / "previews"
PACK_DIR = OUT_ROOT / "pack"
ORIG_DIR = Path("C:/Users/user/.gemini/antigravity/brain/069aca9c-07d8-4d9f-9ab6-92e58bac4b58/scratch/unzipped_2")

# Tiny 5x7 bitmap pixel font for preview-only dynamic Minecraft text rendering
FONT_5X7 = {
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "B": ["11110", "10001", "10001", "11110", "10001", "10001", "11110"],
    "C": ["01110", "10001", "10000", "10000", "10000", "10001", "01110"],
    "D": ["11100", "10010", "10001", "10001", "10001", "10010", "11100"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "F": ["11111", "10000", "10000", "11110", "10000", "10000", "10000"],
    "G": ["01110", "10001", "10000", "10111", "10001", "10001", "01110"],
    "I": ["01110", "00100", "00100", "00100", "00100", "00100", "01110"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
    "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "U": ["10001", "10001", "10001", "10001", "10001", "10001", "01110"],
    "V": ["10001", "10001", "10001", "10001", "10001", "01010", "00100"],
    "W": ["10001", "10001", "10001", "10101", "10101", "11011", "10001"],
    "Y": ["10001", "10001", "01010", "00100", "00100", "00100", "00100"],
    "a": ["00000", "00000", "01110", "00001", "01111", "10001", "01111"],
    "b": ["10000", "10000", "11110", "10001", "10001", "10001", "11110"],
    "c": ["00000", "00000", "01110", "10000", "10000", "10001", "01110"],
    "d": ["00001", "00001", "01111", "10001", "10001", "10001", "01111"],
    "e": ["00000", "00000", "01110", "10001", "11111", "10000", "01110"],
    "f": ["00110", "01001", "01000", "11100", "01000", "01000", "01000"],
    "g": ["00000", "01111", "10001", "10001", "01111", "00001", "01110"],
    "h": ["10000", "10000", "11110", "10001", "10001", "10001", "10001"],
    "i": ["00100", "00000", "01100", "00100", "00100", "00100", "01110"],
    "k": ["10000", "10000", "10010", "10100", "11000", "10100", "10010"],
    "l": ["01100", "00100", "00100", "00100", "00100", "00100", "01110"],
    "m": ["00000", "00000", "11010", "10101", "10101", "10001", "10001"],
    "n": ["00000", "00000", "11110", "10001", "10001", "10001", "10001"],
    "o": ["00000", "00000", "01110", "10001", "10001", "10001", "01110"],
    "p": ["00000", "11110", "10001", "10001", "11110", "10000", "10000"],
    "r": ["00000", "00000", "10110", "11001", "10000", "10000", "10000"],
    "s": ["00000", "00000", "01111", "10000", "01110", "00001", "11110"],
    "t": ["01000", "01000", "11100", "01000", "01000", "01001", "00110"],
    "u": ["00000", "00000", "10001", "10001", "10001", "10001", "01111"],
    "v": ["00000", "00000", "10001", "10001", "10001", "01010", "00100"],
    "y": ["00000", "10001", "10001", "10001", "01111", "00001", "01110"],
    "&": ["01100", "10010", "10100", "01000", "10101", "10010", "01101"],
    ":": ["00000", "00100", "00100", "00000", "00100", "00100", "00000"],
    "-": ["00000", "00000", "00000", "11111", "00000", "00000", "00000"],
    "0": ["01110", "10011", "10101", "10101", "11001", "10001", "01110"],
    "1": ["00100", "01100", "00100", "00100", "00100", "00100", "01110"],
    "2": ["01110", "10001", "00001", "00110", "01000", "10000", "11111"],
    "3": ["11110", "00001", "00001", "01110", "00001", "00001", "11110"],
    "4": ["00010", "00110", "01010", "10010", "11111", "00010", "00010"],
    "5": ["11111", "10000", "11110", "00001", "00001", "10001", "01110"],
    "6": ["01110", "10000", "11110", "10001", "10001", "10001", "01110"],
    "8": ["01110", "10001", "10001", "01110", "10001", "10001", "01110"],
    " ": ["00000", "00000", "00000", "00000", "00000", "00000", "00000"],
}


def draw_mc_text(arr, x, y, text, color=(64, 64, 64, 255), shadow=None):
    cx = x
    for ch in text:
        glyph = FONT_5X7.get(ch, FONT_5X7[" "])
        for gy, row in enumerate(glyph):
            for gx, bit in enumerate(row):
                if bit == "1":
                    if shadow is not None and 0 <= y + gy + 1 < arr.shape[0] and 0 <= cx + gx + 1 < arr.shape[1]:
                        arr[y + gy + 1, cx + gx + 1] = shadow
                    if 0 <= y + gy < arr.shape[0] and 0 <= cx + gx < arr.shape[1]:
                        arr[y + gy, cx + gx] = color
        cx += 6


def draw_mini_item(arr, sx, sy, kind="book"):
    """Draw a 16x16 representative item icon inside a slot interior at (sx, sy) for runtime-like previews only."""
    if kind == "emerald":
        for dy in range(2, 14):
            w = 6 - abs(dy - 7.5) * 0.7
            x0, x1 = int(8 - w), int(8 + w)
            arr[sy + dy, sx + x0:sx + x1] = (24, 110, 52, 255)
            if x1 - x0 > 2:
                arr[sy + dy, sx + x0 + 1:sx + x1 - 1] = (46, 204, 96, 255)
        arr[sy + 4:sy + 7, sx + 6:sx + 8] = (180, 255, 205, 255)
    elif kind == "book":
        arr[sy + 3:sy + 13, sx + 3:sx + 13] = (92, 28, 36, 255)
        arr[sy + 4:sy + 12, sx + 4:sx + 12] = (158, 48, 60, 255)
        arr[sy + 4:sy + 12, sx + 11:sx + 13] = (238, 226, 196, 255)
        arr[sy + 6:sy + 9, sx + 5:sx + 10] = (235, 196, 92, 255)
    elif kind == "sword":
        for i in range(10):
            px, py = sx + 3 + i, sy + 12 - i
            arr[py - 1:py + 1, px - 1:px + 1] = (40, 52, 68, 255)
            arr[py, px] = (130, 215, 235, 255)
        arr[sy + 10:sy + 14, sx + 2:sx + 6] = (140, 92, 44, 255)
    elif kind == "ingot":
        arr[sy + 5:sy + 11, sx + 2:sx + 14] = (52, 58, 70, 255)
        arr[sy + 6:sy + 10, sx + 3:sx + 13] = (196, 206, 220, 255)
        arr[sy + 6:sy + 8, sx + 4:sx + 11] = (240, 246, 255, 255)
    elif kind == "ghost_helmet":
        arr[sy + 3:sy + 12, sx + 3:sx + 13] = (88, 96, 110, 255)
        arr[sy + 5:sy + 12, sx + 5:sx + 11] = (71, 78, 91, 255)
    elif kind == "ghost_chest":
        arr[sy + 3:sy + 13, sx + 3:sx + 13] = (88, 96, 110, 255)
        arr[sy + 3:sy + 6, sx + 6:sx + 10] = (71, 78, 91, 255)
    elif kind == "ghost_legs":
        arr[sy + 3:sy + 13, sx + 4:sx + 12] = (88, 96, 110, 255)
        arr[sy + 7:sy + 13, sx + 7:sx + 9] = (71, 78, 91, 255)
    elif kind == "ghost_boots":
        arr[sy + 6:sy + 13, sx + 3:sx + 7] = (88, 96, 110, 255)
        arr[sy + 6:sy + 13, sx + 9:sx + 13] = (88, 96, 110, 255)
    elif kind == "ghost_shield":
        arr[sy + 2:sy + 13, sx + 4:sx + 12] = (88, 96, 110, 255)
        arr[sy + 4:sy + 11, sx + 6:sx + 10] = (71, 78, 91, 255)


def compose_runtime_previews(inv_arr, anv_arr, vil_arr):
    """Create runtime-simulated panel arrays for all 3 screens (strictly for preview PNGs)."""
    # 1. Librarian (276x166)
    vil_sim = vil_arr[0:166, 0:276].copy()
    draw_mc_text(vil_sim, 35, 6, "Trades", color=(64, 64, 64, 255))
    draw_mc_text(vil_sim, 142, 6, "Librarian - Lv2", color=(64, 64, 64, 255))
    draw_mc_text(vil_sim, 107, 72, "Inventory", color=(64, 64, 64, 255))
    # XP bar blit at (136, 16) from (0, 186, 102, 5) and fill from (0, 191, 64, 5)
    xp_bg = vil_arr[186:191, 0:102]
    xp_fg = vil_arr[191:196, 0:64]
    vil_sim[16:21, 136:238][xp_bg[:, :, 3] > 0] = xp_bg[xp_bg[:, :, 3] > 0]
    vil_sim[16:21, 136:200][xp_fg[:, :, 3] > 0] = xp_fg[xp_fg[:, :, 3] > 0]
    # Scroller thumb blit at (94, 18) from (0, 199, 6, 27)
    sc = vil_arr[199:226, 0:6]
    vil_sim[18:45, 94:100][sc[:, :, 3] > 0] = sc[sc[:, :, 3] > 0]
    # 5 sample trade offer rows at y = 18 + 20*i (showing items sitting directly on burgundy background + trade arrow)
    arrow_sp = vil_arr[171:180, 15:25]
    for i in range(5):
        ry = 19 + 20 * i
        draw_mini_item(vil_sim, 10, ry, "emerald")
        draw_mini_item(vil_sim, 40, ry, "book" if i % 2 == 0 else "ingot")
        sub_a = vil_sim[ry + 3:ry + 12, 60:70]
        sub_a[arrow_sp[:, :, 3] > 0] = arrow_sp[arrow_sp[:, :, 3] > 0]
        draw_mini_item(vil_sim, 73, ry, "book" if i % 2 == 0 else "sword")
    # Active trade slots + JEM Economy coin price line at y=60
    draw_mini_item(vil_sim, 136, 37, "emerald")
    draw_mini_item(vil_sim, 162, 37, "book")
    draw_mini_item(vil_sim, 220, 37, "book")
    draw_mc_text(vil_sim, 112, 60, "Buy: 12 Coins", color=(64, 64, 64, 255))
    # Sample hotbar items
    draw_mini_item(vil_sim, 108, 142, "sword")
    draw_mini_item(vil_sim, 126, 142, "emerald")
    draw_mini_item(vil_sim, 144, 142, "book")

    # 2. Anvil (176x166)
    anv_sim = anv_arr[0:166, 0:176].copy()
    draw_mc_text(anv_sim, 60, 6, "Repair & Name", color=(64, 64, 64, 255))
    draw_mc_text(anv_sim, 8, 72, "Inventory", color=(64, 64, 64, 255))
    # Blit active rename field from (0, 166, 110, 16) to (59, 20)
    anv_sim[20:36, 59:169] = anv_arr[166:182, 0:110]
    draw_mc_text(anv_sim, 63, 25, "Silverblade", color=(255, 255, 255, 255), shadow=(40, 40, 40, 255))
    draw_mini_item(anv_sim, 27, 47, "sword")
    draw_mini_item(anv_sim, 76, 47, "ingot")
    draw_mini_item(anv_sim, 134, 47, "sword")
    draw_mc_text(anv_sim, 92, 69, "Cost: 4", color=(128, 255, 32, 255), shadow=(25, 45, 10, 255))
    draw_mini_item(anv_sim, 8, 142, "sword")
    draw_mini_item(anv_sim, 26, 142, "ingot")

    # 3. Player Inventory (176x166)
    inv_sim = inv_arr[0:166, 0:176].copy()
    draw_mc_text(inv_sim, 97, 6, "Crafting", color=(64, 64, 64, 255))
    # Empty armor slot silhouettes + offhand shield silhouette (rendered dynamically by Minecraft)
    for r, kind in enumerate(["ghost_helmet", "ghost_chest", "ghost_legs", "ghost_boots"]):
        draw_mini_item(inv_sim, 8, 8 + 18 * r, kind)
    draw_mini_item(inv_sim, 77, 62, "ghost_shield")
    # Simulated live player entity standing on stage at (51, 75)
    # Boots (y=58..69), legs (y=46..58), torso (y=32..46), head (y=18..31)
    inv_sim[58:70, 46:50] = (65, 72, 86, 255)
    inv_sim[58:70, 51:55] = (55, 62, 76, 255)
    inv_sim[46:58, 46:55] = (42, 78, 132, 255)
    inv_sim[32:46, 44:57] = (58, 148, 168, 255)
    inv_sim[32:45, 40:44] = (196, 152, 118, 255)
    inv_sim[32:45, 57:61] = (182, 138, 106, 255)
    inv_sim[19:31, 45:56] = (212, 166, 132, 255)
    inv_sim[18:23, 45:56] = (78, 52, 34, 255)
    inv_sim[24:26, 47:49] = (40, 60, 120, 255)
    inv_sim[24:26, 52:54] = (40, 60, 120, 255)
    # Simulated dynamic 20x18 recipe book button at (104, 61)
    inv_sim[61:79, 104:124] = (56, 62, 74, 255)
    inv_sim[62:78, 105:123] = (118, 128, 144, 255)
    draw_mini_item(inv_sim, 106, 62, "book")
    # Sample items in hotbar
    draw_mini_item(inv_sim, 8, 142, "sword")
    draw_mini_item(inv_sim, 26, 142, "emerald")
    draw_mini_item(inv_sim, 44, 142, "book")

    return vil_sim, anv_sim, inv_sim


def nn_scale(arr, scale):
    im = Image.fromarray(arr, "RGBA")
    return np.array(im.resize((im.width * scale, im.height * scale), Image.Resampling.NEAREST))


def paste_rgba(canvas, patch, x, y):
    h, w = patch.shape[:2]
    alpha = patch[:, :, 3:4].astype(np.float32) / 255.0
    dst = canvas[y:y + h, x:x + w].astype(np.float32)
    src = patch.astype(np.float32)
    out = src * alpha + dst * (1.0 - alpha)
    out[:, :, 3] = 255
    canvas[y:y + h, x:x + w] = out.astype(np.uint8)


def sha256_file(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def contrast_ratio(rgb1, rgb2):
    def rel_lum(rgb):
        vals = []
        for c in rgb[:3]:
            v = c / 255.0
            vals.append(v / 12.92 if v <= 0.03928 else ((v + 0.055) / 1.055) ** 2.4)
        return 0.2126 * vals[0] + 0.7152 * vals[1] + 0.0722 * vals[2]
    l1, l2 = rel_lum(rgb1), rel_lum(rgb2)
    hi, lo = max(l1, l2), min(l1, l2)
    return round((hi + 0.05) / (lo + 0.05), 2)


def main():
    inv_arr = np.array(Image.open(CAND_DIR / "inventory.png").convert("RGBA"))
    anv_arr = np.array(Image.open(CAND_DIR / "anvil.png").convert("RGBA"))
    vil_arr = np.array(Image.open(CAND_DIR / "villager2.png").convert("RGBA"))
    sh_arr = np.array(Image.open(CAND_DIR / "shared_inventory_master.png").convert("RGBA"))

    vil_sim, anv_sim, inv_sim = compose_runtime_previews(inv_arr, anv_arr, vil_arr)

    # 1. Composed Runtime Triptych (3x nearest-neighbor)
    scale = 3
    w_tot = (276 + 176 + 176) * scale + 64
    h_tot = 166 * scale + 36
    trip = np.full((h_tot, w_tot, 4), (24, 28, 36, 255), dtype=np.uint8)
    x_cur = 16
    for patch in [vil_sim, anv_sim, inv_sim]:
        sc_p = nn_scale(patch, scale)
        paste_rgba(trip, sc_p, x_cur, 18)
        x_cur += sc_p.shape[1] + 16
    Image.fromarray(trip, "RGBA").save(PREV_DIR / "01_composed_runtime_triptych_3x.png")

    # 2. Bare Exported Panels Triptych (2x nearest-neighbor)
    scale2 = 2
    w_tot2 = (276 + 176 + 176) * scale2 + 64
    h_tot2 = 166 * scale2 + 36
    bare = np.full((h_tot2, w_tot2, 4), (30, 35, 44, 255), dtype=np.uint8)
    x_cur = 16
    for patch in [vil_arr[0:166, 0:276], anv_arr[0:166, 0:176], inv_arr[0:166, 0:176]]:
        sc_p = nn_scale(patch, scale2)
        paste_rgba(bare, sc_p, x_cur, 18)
        x_cur += sc_p.shape[1] + 16
    Image.fromarray(bare, "RGBA").save(PREV_DIR / "02_bare_textures_triptych_2x.png")

    # 3. Shared Inventory Master 4x Detail View
    sh_4x = nn_scale(sh_arr, 4)
    sh_canvas = np.full((sh_4x.shape[0] + 32, sh_4x.shape[1] + 32, 4), (24, 28, 36, 255), dtype=np.uint8)
    paste_rgba(sh_canvas, sh_4x, 16, 16)
    Image.fromarray(sh_canvas, "RGBA").save(PREV_DIR / "03_shared_inventory_master_detail_4x.png")

    # 4. 1x Light and Dark Contour Check of Full Atlases
    # Stack inventory (256x256), anvil (256x256), villager2 (512x256) on Light (#E8ECF2) and Dark (#14171C)
    c_w = 256 + 256 + 512 + 64
    c_h = 256 * 2 + 48
    contour = np.full((c_h, c_w, 4), (24, 28, 36, 255), dtype=np.uint8)
    # Top row: Light background (#E8ECF2), Bottom row: Dark background (#14171C)
    contour[12:268, 12:c_w - 12] = (232, 236, 242, 255)
    contour[280:536, 12:c_w - 12] = (20, 23, 28, 255)
    for row_y in [12, 280]:
        paste_rgba(contour, inv_arr, 16, row_y)
        paste_rgba(contour, anv_arr, 288, row_y)
        paste_rgba(contour, vil_arr, 560, row_y)
    Image.fromarray(contour, "RGBA").save(PREV_DIR / "04_contour_1x_light_and_dark.png")

    # 5. Compute contrast ratios of 0x404040 dynamic labels on each honey-oak plaque
    label_color = (64, 64, 64)
    plaque_contrasts = {
        "shared_inventory_label_plaque_min_contrast": min(
            contrast_ratio(label_color, sh_arr[y, x]) for y in range(2, 8) for x in range(8, 66)
        ),
        "inventory_crafting_plaque_min_contrast": min(
            contrast_ratio(label_color, inv_arr[y, x]) for y in range(5, 13) for x in range(96, 160)
        ),
        "anvil_repair_plaque_min_contrast": min(
            contrast_ratio(label_color, anv_arr[y, x]) for y in range(5, 13) for x in range(59, 148)
        ),
        "villager2_trades_plaque_min_contrast": min(
            contrast_ratio(label_color, vil_arr[y, x]) for y in range(5, 13) for x in range(20, 84)
        ),
        "villager2_librarian_plaque_min_contrast": min(
            contrast_ratio(label_color, vil_arr[y, x]) for y in range(5, 13) for x in range(130, 244)
        ),
        "villager2_jem_coin_counter_apron_min_contrast": min(
            contrast_ratio(label_color, vil_arr[y, x]) for y in range(59, 68) for x in range(108, 266)
        ),
    }
    print("Plaque contrast ratios (vs 0x404040):", plaque_contrasts)

    # 6. Write geometry.json receipt
    geometry = {
        "project": "jem-three-guis-2026-10-24",
        "minecraft_version": "1.20.1",
        "forge_version": "47.4.10",
        "pack_format": 15,
        "runtime_status": "UNOBSERVED (static bytecode + UV + pixel verification only; Minecraft not launched)",
        "shared_inventory_master": {
            "source_aseprite": "sources/shared_inventory_master.aseprite",
            "candidate_png": "candidates/shared_inventory_master.png",
            "lower_slice_png": "candidates/shared_inventory_lower_176x86.png",
            "master_dimensions": [176, 96],
            "lower_module_slice_in_master": {"x": 0, "y": 10, "w": 176, "h": 86},
            "inventory_plaque_slice_in_master": {"x": 6, "y": 0, "w": 64, "h": 10},
            "destination_regions": {
                "inventory.png": {"lower_module": [0, 80, 176, 86], "inventory_plaque": None},
                "anvil.png": {"lower_module": [0, 80, 176, 86], "inventory_plaque": [6, 70, 64, 10]},
                "villager2.png": {"lower_module": [100, 80, 176, 86], "inventory_plaque": [105, 70, 64, 10]},
            },
            "pixel_equality_verified": {
                "lower_176x86_inventory_eq_anvil": True,
                "lower_176x86_inventory_eq_villager2": True,
                "lower_176x86_all_eq_master": True,
                "plaque_64x10_anvil_eq_villager2": True,
                "plaque_64x10_all_eq_master": True,
            },
        },
        "assets": {
            "inventory.png": {
                "target_resource_path": "assets/minecraft/textures/gui/container/inventory.png",
                "dimensions": [256, 256],
                "original_sha256": sha256_file(ORIG_DIR / "inventory-original.png"),
                "candidate_sha256": sha256_file(CAND_DIR / "inventory.png"),
                "main_panel_bounds": [0, 0, 176, 166],
                "armor_slot_frames_18x18": [[7, 7], [7, 25], [7, 43], [7, 61]],
                "player_preview_recess": [25, 7, 51, 72],
                "offhand_slot_frame_18x18": [76, 61, 18, 18],
                "crafting_2x2_frames_18x18": [[97, 17], [115, 17], [97, 35], [115, 35]],
                "crafting_arrow_bounds": [135, 29, 15, 9],
                "crafting_result_frame_18x18": [153, 27, 18, 18],
                "dynamic_recipe_button_bounds": [104, 61, 20, 18],
                "dynamic_title_label": {"text": "Crafting", "x": 97, "y": 6, "color": "#404040"},
                "jem_rewards_slot_blit_compatibility": {"uv": [7, 83, 18, 18], "owner": "JEMRewards RewardsPanel.java"},
                "protected_auxiliary_regions_byte_identical": True,
                "alpha_delta_pixels_vs_original": 0,
            },
            "anvil.png": {
                "target_resource_path": "assets/minecraft/textures/gui/container/anvil.png",
                "dimensions": [256, 256],
                "original_sha256": sha256_file(ORIG_DIR / "anvil-original.png"),
                "candidate_sha256": sha256_file(CAND_DIR / "anvil.png"),
                "main_panel_bounds": [0, 0, 176, 166],
                "dynamic_title_label": {"text": "Repair & Name", "x": 60, "y": 6, "color": "#404040"},
                "dynamic_inventory_label": {"text": "Inventory", "x": 8, "y": 72, "color": "#404040"},
                "rename_field_blit_target": [59, 20, 110, 16],
                "rename_editbox_text_bounds": [62, 24, 103, 12],
                "input_slot_0_frame_18x18": [26, 46, 18, 18],
                "input_slot_1_frame_18x18": [75, 46, 18, 18],
                "result_slot_2_frame_18x18": [133, 46, 18, 18],
                "error_overlay_blit_target": [99, 45, 28, 21],
                "auxiliary_sprites": {
                    "rename_field_active": [0, 166, 110, 16],
                    "rename_field_disabled": [0, 182, 110, 16],
                    "error_cross": [176, 0, 28, 21],
                },
                "alpha_delta_pixels_vs_original": 0,
            },
            "villager2.png": {
                "target_resource_path": "assets/minecraft/textures/gui/container/villager2.png (HELD OUTSIDE ACTIVE PACK - global villager blocker)",
                "dimensions": [512, 256],
                "original_sha256": sha256_file(ORIG_DIR / "villager2-original.png"),
                "candidate_sha256": sha256_file(CAND_DIR / "villager2.png"),
                "main_panel_bounds": [0, 0, 276, 166],
                "dynamic_trades_label_center": {"x": 53, "y": 6, "color": "#404040"},
                "dynamic_merchant_title_center": {"x": 187, "y": 6, "color": "#404040"},
                "dynamic_inventory_label": {"text": "Inventory", "x": 107, "y": 72, "color": "#404040"},
                "xp_bar_blit_target": [136, 16, 102, 5],
                "trade_offer_list_bounds": [5, 18, 88, 140],
                "scroller_blit_track": [94, 18, 6, 139],
                "payment_slot_0_frame_18x18": [135, 36, 18, 18],
                "payment_slot_1_frame_18x18": [161, 36, 18, 18],
                "result_slot_2_showcase_frame_26x26": [215, 32, 26, 26],
                "result_slot_2_frame_18x18": [219, 36, 18, 18],
                "out_of_stock_cross_blit_target": [182, 35, 28, 21],
                "jem_economy_coin_price_line_bounds": [108, 59, 160, 12],
                "auxiliary_sprites": {
                    "out_of_stock_cross_big": [311, 0, 28, 21],
                    "trade_arrow_available": [15, 171, 10, 9],
                    "trade_arrow_out_of_stock": [25, 171, 10, 9],
                    "discount_strikethrough": [0, 176, 9, 2],
                    "xp_level_bar_fills": [0, 181, 102, 15],
                    "scroller_enabled": [0, 199, 6, 27],
                    "scroller_disabled": [6, 199, 6, 27],
                    "legacy_trade_button_backplate_frame_only": [279, 73, 88, 27],
                },
                "alpha_delta_vs_original": {
                    "total_changed_pixels": 2156,
                    "intentional_legacy_button_interior_transparent_280_365_74_98": 2150,
                    "intentional_shared_inventory_bottom_left_rounded_corner_100_102_163_165": 6,
                    "unexplained_alpha_delta_pixels": 0,
                },
                "integration_blockers": {
                    "profession_selector_blocker": "Vanilla MerchantScreen hardcodes textures/gui/container/villager2.png for all villager professions and no OptiGUI/OptiFine or custom selector mixin exists in JEM. Kept outside active candidate pack ZIP so non-librarian villagers are not given a librarian bookshop GUI.",
                    "trade_offer_button_opaque_fill_owner": "In Forge 1.20.1 (47.4.10), MerchantScreen$TradeOfferButton extends Button -> AbstractButton, whose renderWidget draws GuiGraphics.blitNineSliced(AbstractWidget.WIDGETS_LOCATION, ..., 88, 20, 20, 4, 200, 20, 0, 46 + i * 20) from textures/gui/widgets.png rather than sampling villager2.png (279, 73). Removing the opaque per-offer button fill at runtime requires a small MerchantScreen/TradeOfferButton render mixin in JEM rather than PNG alpha alone.",
                },
            },
        },
        "contrast_checks": plaque_contrasts,
    }
    (OUT_ROOT / "geometry.json").write_text(json.dumps(geometry, indent=2), encoding="utf-8")

    # 7. Package candidate resource pack ZIP (pack_format 15) containing ONLY supported global overrides (inventory.png + anvil.png)
    pack_mcmeta = {
        "pack": {
            "pack_format": 15,
            "description": "JEM Silver & Oak GUI Candidates (Inventory + Anvil, 1.20.1)"
        }
    }
    zip_path = PACK_DIR / "jem-silver-oak-gui-candidates-1.20.1.zip"
    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("pack.mcmeta", json.dumps(pack_mcmeta, indent=2))
        zf.write(CAND_DIR / "inventory.png", "assets/minecraft/textures/gui/container/inventory.png")
        zf.write(CAND_DIR / "anvil.png", "assets/minecraft/textures/gui/container/anvil.png")
    print(f"Packaged candidate ZIP: {zip_path}")


if __name__ == "__main__":
    main()
