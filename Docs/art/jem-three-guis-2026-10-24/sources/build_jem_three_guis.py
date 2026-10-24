import os
import json
import hashlib
import subprocess
from pathlib import Path
from PIL import Image
import numpy as np

OUT_ROOT = Path("C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/jem-three-guis-2026-10-24")
SRC_DIR = OUT_ROOT / "sources"
CAND_DIR = OUT_ROOT / "candidates"
PREV_DIR = OUT_ROOT / "previews"
PACK_DIR = OUT_ROOT / "pack"
ORIG_DIR = Path("C:/Users/user/.gemini/antigravity/brain/069aca9c-07d8-4d9f-9ab6-92e58bac4b58/scratch/unzipped_2")
ASEPRITE_EXE = Path("C:/Users/user/.codex/tools/aseprite/build-1.3.18.6/bin/aseprite.exe")

for d in [SRC_DIR, CAND_DIR, PREV_DIR, PACK_DIR]:
    d.mkdir(parents=True, exist_ok=True)


def rgba(hex_str, a=255):
    h = hex_str.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


# Palette constants
C_OUTLINE_DARK = rgba("#232830")
C_SEAM_DEEP = rgba("#1A1E24")
C_SEAM_MID = rgba("#2A303A")

# Brushed polished silver-gray palette (Shared Inventory + Player Inventory)
C_SILV_SPEC = rgba("#F6F9FF")
C_SILV_HI = rgba("#E4ECF6")
C_SILV_LIGHT = rgba("#D4DDE9")
C_SILV_BASE = rgba("#C7D0DD")
C_SILV_MID = rgba("#B8C2D1")
C_SILV_SHAD = rgba("#8892A4")
C_SILV_DEEP = rgba("#5C6575")

# Recessed 3D slot palette (calm, low-contrast interior for item clarity)
C_SLOT_TOP_RIM = rgba("#2B313B")
C_SLOT_LEFT_RIM = rgba("#343B47")
C_SLOT_CORNER_TL = rgba("#222730")
C_SLOT_BOT_RIM = rgba("#EEF3FA")
C_SLOT_RIGHT_RIM = rgba("#DCE4F0")
C_SLOT_CORNER_BR = rgba("#9BA5B6")
C_SLOT_INT_OVERHANG1 = rgba("#353B46")
C_SLOT_INT_OVERHANG2 = rgba("#3E4552")
C_SLOT_INT_LEFT = rgba("#3C4350")
C_SLOT_INT_CENTER = rgba("#474E5B")
C_SLOT_INT_RIGHT = rgba("#4E5664")
C_SLOT_INT_SHELF1 = rgba("#505866")
C_SLOT_INT_SHELF2 = rgba("#57606E")

# Warm Honey-Oak Plank Sign palette (brightened interior for >= 4.4:1 contrast with 0x404040 dynamic labels)
C_OAK_OUTLINE = rgba("#3B2312")
C_OAK_TOP_HI = rgba("#F4CE8B")
C_OAK_FACE_HI = rgba("#E6B370")
C_OAK_FACE_BASE = rgba("#DEA865")
C_OAK_FACE_GRAIN = rgba("#D49C58")
C_OAK_BOT_SHAD = rgba("#8E592B")
C_OAK_PIN = rgba("#633E20")
C_OAK_PIN_HI = rgba("#F7D99C")

# Forged Iron Anvil palette
C_IRON_OUTLINE = rgba("#15181F")
C_IRON_HI = rgba("#7E8A9E")
C_IRON_LIGHT = rgba("#5E687A")
C_IRON_BASE = rgba("#3E4552")
C_IRON_MID = rgba("#343A46")
C_IRON_DARK = rgba("#282D37")
C_IRON_WELL = rgba("#20242C")
C_EMBER_CORE = rgba("#F5A64C")
C_EMBER_GLOW = rgba("#D96B27")
C_EMBER_DIM = rgba("#8F3B1B")

# Librarian Bookshop Oak & Burgundy palette
C_WOOD_OUTLINE = rgba("#2D190B")
C_WOOD_SPEC = rgba("#E8B974")
C_WOOD_LIGHT = rgba("#C98B49")
C_WOOD_BASE = rgba("#B37638")
C_WOOD_MID = rgba("#9A6128")
C_WOOD_DARK = rgba("#6E4219")
C_WOOD_DEEP = rgba("#4A2B0F")

C_BURG_DEEP = rgba("#2B0F15")
C_BURG_DARK = rgba("#3D161E")
C_BURG_BASE = rgba("#4E1E27")
C_BURG_MID = rgba("#5C2530")
C_BURG_HI = rgba("#70303C")
C_GOLD_HI = rgba("#F5D77F")
C_GOLD_BASE = rgba("#D9AE4C")
C_GOLD_SHAD = rgba("#966F24")


def fill_rect(arr, x0, y0, x1, y1, color):
    """Inclusive rectangle fill [x0..x1, y0..y1]."""
    arr[y0:y1 + 1, x0:x1 + 1] = color


def draw_recessed_slot_18x18(arr, sx, sy, top_rim=C_SLOT_TOP_RIM, left_rim=C_SLOT_LEFT_RIM,
                             bot_rim=C_SLOT_BOT_RIM, right_rim=C_SLOT_RIGHT_RIM,
                             corner_tl=C_SLOT_CORNER_TL, corner_br=C_SLOT_CORNER_BR):
    """Draw an 18x18 slot frame at (sx, sy) with a quiet 3D recessed 16x16 interior at (sx+1, sy+1)."""
    fill_rect(arr, sx, sy, sx + 17, sy, top_rim)
    fill_rect(arr, sx, sy, sx, sy + 17, left_rim)
    fill_rect(arr, sx, sy + 17, sx + 17, sy + 17, bot_rim)
    fill_rect(arr, sx + 17, sy, sx + 17, sy + 17, right_rim)
    arr[sy, sx] = corner_tl
    arr[sy, sx + 17] = corner_br
    arr[sy + 17, sx] = corner_br
    arr[sy + 17, sx + 17] = bot_rim

    ix, iy = sx + 1, sy + 1
    fill_rect(arr, ix, iy, ix + 15, iy + 15, C_SLOT_INT_CENTER)
    fill_rect(arr, ix, iy, ix + 15, iy, C_SLOT_INT_OVERHANG1)
    fill_rect(arr, ix, iy + 1, ix + 15, iy + 1, C_SLOT_INT_OVERHANG2)
    fill_rect(arr, ix, iy + 2, ix, iy + 14, C_SLOT_INT_LEFT)
    fill_rect(arr, ix + 15, iy + 2, ix + 15, iy + 14, C_SLOT_INT_RIGHT)
    fill_rect(arr, ix + 1, iy + 14, ix + 15, iy + 14, C_SLOT_INT_SHELF1)
    fill_rect(arr, ix, iy + 15, ix + 15, iy + 15, C_SLOT_INT_SHELF2)
    arr[iy, ix] = rgba("#2F3540")


def draw_oak_plaque(arr, x0, y0, x1, y1):
    """Draw a warm honey-oak plank sign at [x0..x1, y0..y1] with high-contrast calm face for 0x404040 text."""
    fill_rect(arr, x0, y0, x1, y1, C_OAK_OUTLINE)
    fill_rect(arr, x0 + 1, y0 + 1, x1 - 1, y1 - 1, C_OAK_FACE_BASE)
    # Sunlit top edge and shaded bottom edge
    fill_rect(arr, x0 + 1, y0 + 1, x1 - 1, y0 + 1, C_OAK_TOP_HI)
    fill_rect(arr, x0 + 1, y0 + 2, x0 + 1, y1 - 2, C_OAK_FACE_HI)
    fill_rect(arr, x0 + 1, y1 - 1, x1 - 1, y1 - 1, C_OAK_BOT_SHAD)
    fill_rect(arr, x1 - 1, y0 + 2, x1 - 1, y1 - 1, C_OAK_BOT_SHAD)

    # Sparse, gentle horizontal wood grain strokes (keeping text zone calm and >= 4.4:1 contrast)
    w = x1 - x0 + 1
    h = y1 - y0 + 1
    if h >= 10:
        fill_rect(arr, x0 + 5, y0 + 2, x0 + w // 3, y0 + 2, C_OAK_FACE_HI)
        fill_rect(arr, x0 + w // 2, y0 + h - 3, x1 - 6, y0 + h - 3, C_OAK_FACE_GRAIN)
        fill_rect(arr, x0 + 8, y0 + h - 2, x0 + (2 * w) // 3, y0 + h - 2, C_OAK_FACE_GRAIN)

    # Place subtle corner pins on the 1px inner bevel corners (x0+1, x1-1) so they never touch dynamic text glyphs!
    for px, py in [(x0 + 1, y0 + 1), (x1 - 1, y0 + 1), (x0 + 1, y1 - 1), (x1 - 1, y1 - 1)]:
        arr[py, px] = C_OAK_PIN
    arr[y0 + 1, x0 + 2] = C_OAK_PIN_HI
    arr[y0 + 1, x1 - 2] = C_OAK_PIN_HI


def draw_diagonal_shine(arr, x_start, y_start, length, thickness=1, color=C_SILV_SPEC, secondary=C_SILV_HI):
    """Draw a static diagonal reflection mark (top-right to bottom-left)."""
    for i in range(length):
        x = x_start - i
        y = y_start + i
        if 0 <= y < arr.shape[0] and 0 <= x < arr.shape[1]:
            arr[y, x] = color
            if thickness >= 2 and x + 1 < arr.shape[1]:
                arr[y, x + 1] = secondary


def build_shared_inventory_master():
    """
    Build the 176x96 shared inventory/hotbar master covering y=70..165:
    - y=70..79 (dy=0..9): shared warm honey-oak 'Inventory' sign at x=4..73 (70x10)
    - y=80..165 (dy=10..95): shared 176x86 lower silver 9x3 inventory + raised 9x1 hotbar module
    """
    H, W = 96, 176
    L_panel = np.zeros((H, W, 4), dtype=np.uint8)
    L_slots = np.zeros((H, W, 4), dtype=np.uint8)
    L_shines = np.zeros((H, W, 4), dtype=np.uint8)
    L_plaque = np.zeros((H, W, 4), dtype=np.uint8)

    # 1. Lower silver panel body (dy=10..95 corresponds to y=80..165)
    fill_rect(L_panel, 0, 10, 175, 66, C_SILV_BASE)
    # Hanging drop-down seam at y=80..81 (dy=10..11) so lower inventory hangs behind upper section
    fill_rect(L_panel, 0, 10, 175, 10, C_SEAM_DEEP)
    fill_rect(L_panel, 1, 11, 174, 11, C_SEAM_MID)
    # Outer left/right borders of 9x3 block
    fill_rect(L_panel, 0, 10, 0, 66, C_OUTLINE_DARK)
    fill_rect(L_panel, 175, 10, 175, 66, C_OUTLINE_DARK)
    # Bright left/top inner facet of 9x3 block
    fill_rect(L_panel, 1, 12, 174, 12, C_SILV_SPEC)
    fill_rect(L_panel, 1, 12, 2, 65, C_SILV_SPEC)
    fill_rect(L_panel, 3, 13, 3, 65, C_SILV_HI)
    # Right/bottom shaded bevel of 9x3 block
    fill_rect(L_panel, 173, 13, 174, 65, C_SILV_SHAD)
    fill_rect(L_panel, 174, 12, 174, 66, C_SILV_DEEP)
    fill_rect(L_panel, 2, 66, 173, 66, C_SILV_SHAD)

    # Subtle horizontal brushed-metal bands on outer rails (outside slots)
    for dy_band in [20, 38, 56]:
        fill_rect(L_panel, 3, dy_band, 6, dy_band, C_SILV_LIGHT)
        fill_rect(L_panel, 169, dy_band, 172, dy_band, C_SILV_LIGHT)

    # 2. Separation trench at y=137..138 (dy=67..68) & Raised Forward Hotbar Strip y=139..165 (dy=69..95)
    fill_rect(L_panel, 0, 67, 175, 67, C_SEAM_DEEP)
    fill_rect(L_panel, 0, 68, 175, 68, C_SEAM_MID)
    fill_rect(L_panel, 0, 67, 2, 68, C_OUTLINE_DARK)
    fill_rect(L_panel, 173, 67, 175, 68, C_OUTLINE_DARK)

    # Raised hotbar body y=139..165 (dy=69..95)
    fill_rect(L_panel, 0, 69, 175, 95, C_SILV_LIGHT)
    fill_rect(L_panel, 4, 71, 171, 92, C_SILV_BASE)
    fill_rect(L_panel, 0, 69, 0, 95, C_OUTLINE_DARK)
    fill_rect(L_panel, 175, 69, 175, 95, C_OUTLINE_DARK)
    fill_rect(L_panel, 0, 95, 175, 95, C_OUTLINE_DARK)
    # Raised forward top/left bright specular bevel on hotbar
    fill_rect(L_panel, 1, 69, 174, 69, C_SILV_SPEC)
    fill_rect(L_panel, 1, 70, 173, 70, C_SILV_HI)
    fill_rect(L_panel, 1, 69, 2, 93, C_SILV_SPEC)
    fill_rect(L_panel, 3, 70, 3, 92, C_SILV_HI)
    # Raised hotbar bottom/right heavy metallic bevel
    fill_rect(L_panel, 2, 93, 173, 93, C_SILV_SHAD)
    fill_rect(L_panel, 1, 94, 174, 94, C_SILV_DEEP)
    fill_rect(L_panel, 173, 70, 173, 93, C_SILV_SHAD)
    fill_rect(L_panel, 174, 69, 174, 94, C_SILV_DEEP)
    # Corner bracket rivets on the raised hotbar
    for rx, ry in [(4, 72), (4, 89), (171, 72), (171, 89)]:
        L_panel[ry, rx] = C_SILV_DEEP
        L_panel[ry - 1, rx] = C_SILV_SPEC

    # Restore exact rounded bottom-left and bottom-right corner alpha from vanilla 176x166 panel
    L_panel[92, 1] = C_OUTLINE_DARK
    L_panel[93, 1] = C_OUTLINE_DARK
    L_panel[93, 2] = C_OUTLINE_DARK
    L_panel[94, 2] = C_OUTLINE_DARK
    L_panel[94, 3] = C_OUTLINE_DARK
    L_panel[93, 174] = C_OUTLINE_DARK
    L_panel[94, 173] = C_OUTLINE_DARK
    for cx, cy in [(0, 93), (0, 94), (1, 94), (175, 94), (0, 95), (1, 95), (2, 95), (174, 95), (175, 95)]:
        L_panel[cy, cx] = (0, 0, 0, 0)

    # 3. 9x3 Player Inventory Slots (x=7+18*c, y=83+18*r -> dy=13+18*r) & 9x1 Hotbar Slots (y=141 -> dy=71)
    for r in range(3):
        sy = 13 + 18 * r
        for c in range(9):
            sx = 7 + 18 * c
            draw_recessed_slot_18x18(L_slots, sx, sy)
    for c in range(9):
        sx = 7 + 18 * c
        draw_recessed_slot_18x18(L_slots, sx, 71)

    # 4. C1 Refined static diagonal reflection marks on silver frame rails & hotbar apron
    # (Clean slash clusters of varied length, width, and spacing on rails/lips; zero stray dots on dark slot corners)
    # Cluster A: top-left rail longer catchlight + short companion
    draw_diagonal_shine(L_shines, 6, 13, 5, thickness=2, color=C_SILV_SPEC, secondary=C_SILV_HI)
    draw_diagonal_shine(L_shines, 5, 21, 3, thickness=1, color=C_SILV_SPEC)
    # Cluster B: mid-left rail small paired glint
    draw_diagonal_shine(L_shines, 5, 44, 4, thickness=1, color=C_SILV_SPEC)
    draw_diagonal_shine(L_shines, 6, 47, 3, thickness=1, color=C_SILV_HI)
    # Cluster C: top-right rail slanted slash + companion
    draw_diagonal_shine(L_shines, 172, 23, 5, thickness=2, color=C_SILV_SPEC, secondary=C_SILV_HI)
    draw_diagonal_shine(L_shines, 172, 32, 3, thickness=1, color=C_SILV_SPEC)
    # Cluster D: lower-right rail short glint
    draw_diagonal_shine(L_shines, 171, 53, 3, thickness=1, color=C_SILV_SPEC)
    # Cluster E: raised hotbar left glint, mid-apron catchlight, and bottom-right longer diagonal slash
    draw_diagonal_shine(L_shines, 6, 74, 4, thickness=2, color=C_SILV_SPEC, secondary=C_SILV_HI)
    draw_diagonal_shine(L_shines, 76, 89, 3, thickness=1, color=C_SILV_SPEC)
    draw_diagonal_shine(L_shines, 165, 89, 4, thickness=2, color=C_SILV_SPEC, secondary=C_SILV_HI)
    draw_diagonal_shine(L_shines, 158, 90, 3, thickness=1, color=C_SILV_SPEC)

    # 5. Shared Warm Honey-Oak 'Inventory' Plaque at y=70..79 (dy=0..9), x=4..73 (70x10)
    draw_oak_plaque(L_plaque, 4, 0, 73, 9)

    layers = [
        ("01_silver_panel_and_seams", L_panel),
        ("02_recessed_3d_slots", L_slots),
        ("03_varied_frame_shines", L_shines),
        ("04_inventory_wood_plaque", L_plaque),
    ]
    comp = np.zeros((H, W, 4), dtype=np.uint8)
    for _, lay in layers:
        mask = lay[:, :, 3] > 0
        comp[mask] = lay[mask]
    return layers, comp


def build_inventory_gui(shared_comp):
    """
    Build inventory.png (256x256 RGBA):
    - Preserves all auxiliary sprites outside [0..175, 0..165] byte-identical to inventory-original.png
    - Pastes exact shared_comp[10:96, 0:176] into [80:166, 0:176]
    - Authors upper pale neutral-gray equipment & crafting panel [0:80, 0:176]
    """
    orig = np.array(Image.open(ORIG_DIR / "inventory-original.png").convert("RGBA"))
    H, W = 256, 256

    L_aux = orig.copy()
    L_aux[0:166, 0:176] = 0

    L_shared = np.zeros((H, W, 4), dtype=np.uint8)
    L_shared[80:166, 0:176] = shared_comp[10:96, 0:176]

    L_upper_panel = np.zeros((H, W, 4), dtype=np.uint8)
    L_upper_slots = np.zeros((H, W, 4), dtype=np.uint8)
    L_upper_plaque = np.zeros((H, W, 4), dtype=np.uint8)

    # Upper panel body [0..175, 0..79]
    fill_rect(L_upper_panel, 0, 0, 175, 79, C_SILV_BASE)
    fill_rect(L_upper_panel, 0, 0, 175, 0, C_OUTLINE_DARK)
    fill_rect(L_upper_panel, 0, 0, 0, 79, C_OUTLINE_DARK)
    fill_rect(L_upper_panel, 175, 0, 175, 79, C_OUTLINE_DARK)
    # Top/left bright specular facet
    fill_rect(L_upper_panel, 1, 1, 174, 2, C_SILV_SPEC)
    fill_rect(L_upper_panel, 1, 1, 2, 78, C_SILV_SPEC)
    fill_rect(L_upper_panel, 3, 3, 172, 3, C_SILV_HI)
    fill_rect(L_upper_panel, 3, 3, 3, 78, C_SILV_HI)
    # Right/bottom shaded bevel of upper section (before y=80 drop seam)
    fill_rect(L_upper_panel, 173, 3, 174, 78, C_SILV_SHAD)
    fill_rect(L_upper_panel, 174, 2, 174, 79, C_SILV_DEEP)
    fill_rect(L_upper_panel, 2, 79, 173, 79, C_SILV_DEEP)

    # Restore exact top-left and top-right rounded corner alpha of inventory-original.png
    L_upper_panel[1, 1] = C_OUTLINE_DARK
    L_upper_panel[1, 2] = C_OUTLINE_DARK
    L_upper_panel[2, 1] = C_OUTLINE_DARK
    L_upper_panel[1, 173] = C_OUTLINE_DARK
    L_upper_panel[2, 174] = C_OUTLINE_DARK
    L_upper_panel[3, 174] = C_OUTLINE_DARK
    for cx, cy in [(0, 0), (1, 0), (0, 1), (173, 0), (174, 0), (175, 0), (174, 1), (175, 1), (175, 2)]:
        L_upper_panel[cy, cx] = (0, 0, 0, 0)

    # Subtle recessed framing plate around equipment bay [6..94, 6..78] & 2x2 crafting bay [95..171, 15..54]
    fill_rect(L_upper_panel, 6, 6, 76, 78, C_SILV_SHAD)
    fill_rect(L_upper_panel, 95, 15, 171, 54, C_SILV_MID)
    fill_rect(L_upper_panel, 95, 15, 171, 15, C_SILV_SHAD)
    fill_rect(L_upper_panel, 95, 15, 95, 54, C_SILV_SHAD)
    fill_rect(L_upper_panel, 95, 54, 171, 54, C_SILV_HI)
    fill_rect(L_upper_panel, 171, 15, 171, 54, C_SILV_HI)

    # Subtle recessed cradle for the dynamic 20x18 recipe book button at (104, 61) -> [103..124, 60..78]
    fill_rect(L_upper_panel, 103, 60, 124, 78, C_SILV_SHAD)
    fill_rect(L_upper_panel, 104, 61, 123, 77, C_SILV_MID)

    # Dark recessed player preview box at [25..75, 7..78] (live entity renders dynamically at leftPos+51, topPos+75)
    fill_rect(L_upper_panel, 25, 7, 75, 78, C_SLOT_TOP_RIM)
    fill_rect(L_upper_panel, 25, 78, 75, 78, C_SLOT_BOT_RIM)
    fill_rect(L_upper_panel, 75, 7, 75, 78, C_SLOT_RIGHT_RIM)
    fill_rect(L_upper_panel, 26, 8, 74, 77, rgba("#181C24"))
    fill_rect(L_upper_panel, 26, 8, 74, 10, rgba("#11141A"))
    fill_rect(L_upper_panel, 26, 8, 27, 77, rgba("#13171E"))
    # Grounded stage floor dais at y=70..77
    fill_rect(L_upper_panel, 26, 70, 74, 70, rgba("#2E3542"))
    fill_rect(L_upper_panel, 26, 71, 74, 75, rgba("#232934"))
    fill_rect(L_upper_panel, 26, 76, 74, 77, rgba("#1B2029"))

    # Upper slots:
    for r in range(4):
        draw_recessed_slot_18x18(L_upper_slots, 7, 7 + 18 * r)
    draw_recessed_slot_18x18(L_upper_slots, 76, 61)
    for r in range(2):
        for c in range(2):
            draw_recessed_slot_18x18(L_upper_slots, 97 + 18 * c, 17 + 18 * r)
    # Crafting result slot at (153, 27) with a 1px raised silver collar
    fill_rect(L_upper_slots, 152, 26, 171, 45, C_SILV_SHAD)
    fill_rect(L_upper_slots, 152, 26, 171, 26, C_SILV_SPEC)
    fill_rect(L_upper_slots, 152, 26, 152, 45, C_SILV_SPEC)
    draw_recessed_slot_18x18(L_upper_slots, 153, 27)

    # Crisp sculpted crafting arrow at x=135..150, y=29..37
    fill_rect(L_upper_slots, 135, 32, 145, 35, C_SLOT_TOP_RIM)
    fill_rect(L_upper_slots, 136, 33, 145, 34, C_SILV_SPEC)
    for i, (y0, y1) in enumerate([(29, 37), (30, 36), (31, 35), (32, 34), (33, 33)]):
        ax = 145 + i
        fill_rect(L_upper_slots, ax, y0, ax, y1, C_SLOT_TOP_RIM)
        if y1 > y0:
            fill_rect(L_upper_slots, ax, y0 + 1, ax, y1 - 1, C_SILV_SPEC)

    # Warm honey-oak 'Crafting' plaque at x=92..166, y=3..14 (behind titleLabelX=97, titleLabelY=6)
    draw_oak_plaque(L_upper_plaque, 92, 3, 166, 14)

    # Varied static diagonal reflection marks on the upper silver frame
    draw_diagonal_shine(L_upper_plaque, 6, 4, 4, thickness=2, color=C_SILV_SPEC, secondary=C_SILV_HI)
    draw_diagonal_shine(L_upper_plaque, 86, 8, 5, thickness=2, color=C_SILV_SPEC, secondary=C_SILV_HI)
    draw_diagonal_shine(L_upper_plaque, 88, 15, 3, thickness=1, color=C_SILV_SPEC)
    draw_diagonal_shine(L_upper_plaque, 172, 58, 5, thickness=2, color=C_SILV_SPEC, secondary=C_SILV_HI)
    draw_diagonal_shine(L_upper_plaque, 142, 63, 4, thickness=1, color=C_SILV_SPEC)

    layers = [
        ("01_protected_auxiliary_sprites", L_aux),
        ("02_shared_lower_inventory_master", L_shared),
        ("03_upper_silver_equipment_panel", L_upper_panel),
        ("04_upper_slots_and_crafting_arrow", L_upper_slots),
        ("05_crafting_wood_plaque_and_shines", L_upper_plaque),
    ]
    comp = np.zeros((H, W, 4), dtype=np.uint8)
    for _, lay in layers:
        mask = lay[:, :, 3] > 0
        comp[mask] = lay[mask]
    return layers, comp


def build_anvil_gui(shared_comp):
    """
    Build anvil.png (256x256 RGBA):
    - Styles auxiliary rename sprites (0,166,110,16) & (0,182,110,16) and error cross (176,0,28,21) with exact alpha
    - Pastes exact shared_comp[10:96, 0:176] into [80:166, 0:176] AND exact shared_comp[0:10, 4:74] plaque at [70:80, 4:74]
    - Authors upper charcoal/blue-gray forged-iron workstation [0:80, 0:176]
    """
    orig = np.array(Image.open(ORIG_DIR / "anvil-original.png").convert("RGBA"))
    H, W = 256, 256

    L_aux = np.zeros((H, W, 4), dtype=np.uint8)
    L_shared = np.zeros((H, W, 4), dtype=np.uint8)
    L_shared[80:166, 0:176] = shared_comp[10:96, 0:176]

    L_upper_iron = np.zeros((H, W, 4), dtype=np.uint8)
    L_anvil_art = np.zeros((H, W, 4), dtype=np.uint8)
    L_work_slots = np.zeros((H, W, 4), dtype=np.uint8)
    L_plaques = np.zeros((H, W, 4), dtype=np.uint8)

    # 1. Upper forged-iron workstation body [0..175, 0..79]
    fill_rect(L_upper_iron, 0, 0, 175, 79, C_IRON_BASE)
    fill_rect(L_upper_iron, 0, 0, 175, 0, C_IRON_OUTLINE)
    fill_rect(L_upper_iron, 0, 0, 0, 79, C_IRON_OUTLINE)
    fill_rect(L_upper_iron, 175, 0, 175, 79, C_IRON_OUTLINE)
    fill_rect(L_upper_iron, 1, 1, 174, 2, C_IRON_HI)
    fill_rect(L_upper_iron, 1, 1, 2, 78, C_IRON_HI)
    fill_rect(L_upper_iron, 3, 3, 172, 3, C_IRON_LIGHT)
    fill_rect(L_upper_iron, 3, 3, 3, 78, C_IRON_LIGHT)
    fill_rect(L_upper_iron, 173, 3, 174, 78, C_IRON_DARK)
    fill_rect(L_upper_iron, 174, 2, 174, 79, C_IRON_OUTLINE)
    fill_rect(L_upper_iron, 1, 79, 174, 79, C_IRON_OUTLINE)

    # Restore exact top-left and top-right rounded corner alpha of anvil-original.png
    L_upper_iron[1, 1] = C_IRON_OUTLINE
    L_upper_iron[1, 2] = C_IRON_OUTLINE
    L_upper_iron[2, 1] = C_IRON_OUTLINE
    L_upper_iron[1, 173] = C_IRON_OUTLINE
    L_upper_iron[2, 174] = C_IRON_OUTLINE
    L_upper_iron[3, 174] = C_IRON_OUTLINE
    for cx, cy in [(0, 0), (1, 0), (0, 1), (173, 0), (174, 0), (175, 0), (174, 1), (175, 1), (175, 2)]:
        L_upper_iron[cy, cx] = (0, 0, 0, 0)

    # Recessed dark-iron inner forge bay [6..169, 16..66]
    fill_rect(L_upper_iron, 6, 16, 169, 66, C_IRON_MID)
    fill_rect(L_upper_iron, 6, 16, 169, 16, C_IRON_DARK)
    fill_rect(L_upper_iron, 6, 16, 6, 66, C_IRON_DARK)
    fill_rect(L_upper_iron, 6, 66, 169, 66, C_IRON_LIGHT)
    fill_rect(L_upper_iron, 169, 16, 169, 66, C_IRON_LIGHT)

    # Calm dark-iron cost readout strip at [75..172, 67..78] so vanilla green/red cost text at y=69 is super clear
    fill_rect(L_upper_iron, 75, 67, 172, 78, C_IRON_DARK)
    fill_rect(L_upper_iron, 76, 68, 171, 77, rgba("#242933"))

    # Forged iron rivets & hanging chain links on left/right pillars
    for lx in [5, 168]:
        for ly in range(4, 16, 4):
            fill_rect(L_upper_iron, lx, ly, lx + 2, ly + 2, C_IRON_OUTLINE)
            L_upper_iron[ly, lx + 1] = C_IRON_HI

    # 2. Compact 3D Forged Anvil Silhouette at [10..50, 17..41] + subtle smithing tongs & ember accent
    # Horn (left): stepped tapered horn x=10..20, y=19..24
    fill_rect(L_anvil_art, 10, 21, 20, 23, C_IRON_OUTLINE)
    fill_rect(L_anvil_art, 13, 20, 20, 24, C_IRON_OUTLINE)
    fill_rect(L_anvil_art, 11, 21, 20, 22, C_SILV_LIGHT)
    fill_rect(L_anvil_art, 14, 23, 20, 23, C_IRON_LIGHT)
    # Main face & table: x=20..46, y=17..25 (clean unmistakable anvil top without any bump)
    fill_rect(L_anvil_art, 20, 17, 46, 25, C_IRON_OUTLINE)
    fill_rect(L_anvil_art, 21, 18, 45, 19, C_SILV_SPEC)
    fill_rect(L_anvil_art, 21, 20, 45, 22, C_IRON_HI)
    fill_rect(L_anvil_art, 21, 23, 45, 24, C_IRON_DARK)
    # Hardy hole dark pixel on top face
    fill_rect(L_anvil_art, 41, 18, 42, 19, C_IRON_OUTLINE)
    # Heel tail (right): x=46..50, y=19..23
    fill_rect(L_anvil_art, 46, 19, 50, 23, C_IRON_OUTLINE)
    fill_rect(L_anvil_art, 46, 20, 49, 21, C_SILV_LIGHT)
    fill_rect(L_anvil_art, 46, 22, 48, 22, C_IRON_LIGHT)
    # Curved waist: x=24..42, y=26..33
    fill_rect(L_anvil_art, 24, 26, 42, 28, C_IRON_OUTLINE)
    fill_rect(L_anvil_art, 26, 28, 40, 33, C_IRON_OUTLINE)
    fill_rect(L_anvil_art, 25, 26, 40, 27, C_IRON_DARK)
    fill_rect(L_anvil_art, 27, 28, 38, 33, C_IRON_DARK)
    fill_rect(L_anvil_art, 28, 28, 35, 32, C_IRON_LIGHT)
    # Heavy stepped forge base: x=16..48, y=34..41
    fill_rect(L_anvil_art, 16, 34, 48, 41, C_IRON_OUTLINE)
    fill_rect(L_anvil_art, 18, 34, 46, 35, C_IRON_HI)
    fill_rect(L_anvil_art, 17, 36, 47, 40, C_IRON_DARK)
    fill_rect(L_anvil_art, 19, 37, 33, 39, C_IRON_LIGHT)
    # Subtle warm ember glow beneath anvil base [27..38, 41]
    fill_rect(L_anvil_art, 27, 41, 38, 41, C_EMBER_DIM)
    fill_rect(L_anvil_art, 29, 41, 36, 41, C_EMBER_GLOW)
    fill_rect(L_anvil_art, 31, 41, 34, 41, C_EMBER_CORE)

    # Right-side forged smithing tongs hanging on a bracket at x=156..165, y=45..62 (with small ember rivet)
    fill_rect(L_anvil_art, 158, 45, 163, 47, C_IRON_OUTLINE)
    fill_rect(L_anvil_art, 159, 46, 162, 46, C_IRON_HI)
    # Tongs jaws & handles
    fill_rect(L_anvil_art, 157, 48, 159, 53, C_IRON_OUTLINE)
    fill_rect(L_anvil_art, 162, 48, 164, 53, C_IRON_OUTLINE)
    fill_rect(L_anvil_art, 158, 48, 158, 52, C_IRON_HI)
    fill_rect(L_anvil_art, 163, 48, 163, 52, C_IRON_LIGHT)
    fill_rect(L_anvil_art, 159, 52, 162, 55, C_IRON_OUTLINE)
    L_anvil_art[53, 160] = C_EMBER_GLOW
    L_anvil_art[53, 161] = C_EMBER_CORE
    fill_rect(L_anvil_art, 158, 55, 159, 61, C_IRON_OUTLINE)
    fill_rect(L_anvil_art, 162, 55, 163, 61, C_IRON_OUTLINE)
    fill_rect(L_anvil_art, 158, 56, 158, 60, C_IRON_LIGHT)
    fill_rect(L_anvil_art, 162, 56, 162, 60, C_IRON_LIGHT)

    # 3. Rename field cradle at [59..168, 20..35] & Workstation Slots [26,46], [75,46], [133,46]
    fill_rect(L_work_slots, 58, 19, 169, 36, C_IRON_OUTLINE)
    fill_rect(L_work_slots, 59, 20, 168, 35, rgba("#1E222A"))
    fill_rect(L_work_slots, 59, 36, 169, 36, C_IRON_HI)

    for sx in [26, 75, 133]:
        fill_rect(L_work_slots, sx - 1, 45, sx + 18, 64, C_IRON_OUTLINE)
        draw_recessed_slot_18x18(
            L_work_slots, sx, 46,
            top_rim=C_IRON_OUTLINE, left_rim=C_IRON_DARK,
            bot_rim=C_IRON_HI, right_rim=C_IRON_LIGHT
        )

    # Chiseled forged '+' sign at x=54..64, y=50..60
    fill_rect(L_work_slots, 58, 50, 61, 60, C_IRON_OUTLINE)
    fill_rect(L_work_slots, 54, 54, 64, 57, C_IRON_OUTLINE)
    fill_rect(L_work_slots, 59, 51, 60, 59, C_IRON_HI)
    fill_rect(L_work_slots, 55, 55, 63, 56, C_IRON_HI)

    # Chiseled forged combine arrow at x=101..123, y=49..60
    fill_rect(L_work_slots, 101, 53, 116, 57, C_IRON_OUTLINE)
    fill_rect(L_work_slots, 102, 54, 116, 55, C_IRON_HI)
    fill_rect(L_work_slots, 102, 56, 116, 56, C_IRON_LIGHT)
    for i, (y0, y1) in enumerate([(49, 61), (50, 60), (51, 59), (52, 58), (53, 57), (54, 56), (55, 55)]):
        ax = 116 + i
        fill_rect(L_work_slots, ax, y0, ax, y1, C_IRON_OUTLINE)
        if y1 > y0:
            fill_rect(L_work_slots, ax, y0 + 1, ax, y1 - 1, C_IRON_HI)

    # 4. Plaques:
    draw_oak_plaque(L_plaques, 55, 3, 154, 15)
    # Exact shared 'Inventory' honey-oak plaque slice from shared_comp[0:10, 4:74] at [70:80, 4:74]
    L_plaques[70:80, 4:74] = shared_comp[0:10, 4:74]

    # 5. Auxiliary sprites with exact original alpha masks:
    # Active rename field sprite at (0, 166, 110, 16)
    fill_rect(L_aux, 0, 166, 109, 181, C_IRON_OUTLINE)
    fill_rect(L_aux, 1, 167, 108, 180, rgba("#282E3A"))
    fill_rect(L_aux, 1, 167, 108, 168, rgba("#1B1F27"))
    fill_rect(L_aux, 1, 180, 108, 180, C_IRON_HI)
    fill_rect(L_aux, 108, 167, 108, 180, C_IRON_LIGHT)
    for rx, ry in [(2, 168), (107, 168), (2, 179), (107, 179)]:
        L_aux[ry, rx] = rgba("#C8844B")

    # Disabled rename field sprite at (0, 182, 110, 16)
    fill_rect(L_aux, 0, 182, 109, 197, C_IRON_OUTLINE)
    fill_rect(L_aux, 1, 183, 108, 196, rgba("#1C2028"))
    fill_rect(L_aux, 1, 183, 108, 184, rgba("#14171D"))
    fill_rect(L_aux, 1, 196, 108, 196, C_IRON_BASE)

    # C1 Fix for Error Cross Sprite at (176, 0, 28, 21):
    # Because anvil-original.png has alpha=255 across all 28x21 pixels at (176,0) and blits it directly onto (99,45),
    # we first copy our own panel's (99..126, 45..65) background+arrow into (176..203, 0..20) and then composite the
    # crimson/ember X where anvil-original.png[0:21, 176:204] differs from anvil-original.png[45:66, 99:127]!
    panel_preview = L_upper_iron.copy()
    mask_ws = L_work_slots[:, :, 3] > 0
    panel_preview[mask_ws] = L_work_slots[mask_ws]
    L_aux[0:21, 176:204] = panel_preview[45:66, 99:127]
    orig_err = orig[0:21, 176:204]
    orig_arrow_patch = orig[45:66, 99:127]
    x_diff_mask = np.any(orig_err[:, :, :3] != orig_arrow_patch[:, :, :3], axis=2)
    for dy in range(21):
        for dx in range(28):
            if x_diff_mask[dy, dx]:
                r, g, b, a = orig_err[dy, dx]
                lum = int(0.299 * r + 0.587 * g + 0.114 * b)
                if lum < 65:
                    L_aux[dy, 176 + dx] = (38, 12, 16, 255)
                elif lum < 120:
                    L_aux[dy, 176 + dx] = (182, 38, 42, 255)
                else:
                    L_aux[dy, 176 + dx] = (235, 76, 64, 255)

    layers = [
        ("01_auxiliary_sprites_exact_alpha", L_aux),
        ("02_shared_lower_inventory_master", L_shared),
        ("03_upper_forged_iron_frame", L_upper_iron),
        ("04_anvil_silhouette_and_embers", L_anvil_art),
        ("05_rename_well_slots_plus_arrow", L_work_slots),
        ("06_wood_title_and_shared_plaques", L_plaques),
    ]
    comp = np.zeros((H, W, 4), dtype=np.uint8)
    for _, lay in layers:
        mask = lay[:, :, 3] > 0
        comp[mask] = lay[mask]
    return layers, comp


def build_villager2_gui(shared_comp):
    """
    Build villager2.png (512x256 RGBA, Librarian-scoped candidate):
    - Preserves all active auxiliary sprites in villager2-original.png with exact UV bounds & alpha
    - Makes legacy trade-button backplate interior at (280..365, 74..98) transparent while keeping a 1px frame at (279..366, 73..99)
    - Pastes exact shared_comp[10:96, 0:176] into [80:166, 100:276] AND exact shared_comp[0:10, 4:74] plaque at [70:80, 103:173]
    - Authors warm honey-oak bookshop frame, burgundy trade list counter, colorful book shelves, grounded lectern on counter, and calm coin-price counter apron
    """
    orig = np.array(Image.open(ORIG_DIR / "villager2-original.png").convert("RGBA"))
    H, W = 256, 512

    L_aux = orig.copy()
    L_aux[0:166, 0:276] = 0
    # Make legacy 88x27 trade-button backplate interior (x=280..365, y=74..98) transparent, keeping 1px frame at (279..366, 73..99)
    L_aux[74:99, 280:366] = (0, 0, 0, 0)
    for x in range(279, 367):
        L_aux[73, x] = C_WOOD_OUTLINE
        L_aux[99, x] = C_GOLD_SHAD
    for y in range(73, 100):
        L_aux[y, 279] = C_WOOD_OUTLINE
        L_aux[y, 366] = C_GOLD_SHAD

    # Style scroller sprites at (0, 199, 6, 27) and (6, 199, 6, 27) with exact alpha=255
    fill_rect(L_aux, 0, 199, 5, 225, C_WOOD_OUTLINE)
    fill_rect(L_aux, 1, 200, 4, 224, C_GOLD_BASE)
    fill_rect(L_aux, 1, 200, 4, 201, C_GOLD_HI)
    fill_rect(L_aux, 1, 200, 1, 223, C_GOLD_HI)
    for gy in [208, 212, 216]:
        fill_rect(L_aux, 2, gy, 3, gy, C_WOOD_DEEP)
    fill_rect(L_aux, 6, 199, 11, 225, C_WOOD_OUTLINE)
    fill_rect(L_aux, 7, 200, 10, 224, C_WOOD_DARK)
    fill_rect(L_aux, 7, 200, 10, 201, C_WOOD_MID)

    L_shared = np.zeros((H, W, 4), dtype=np.uint8)
    L_shared[80:166, 100:276] = shared_comp[10:96, 0:176]

    L_oak_frame = np.zeros((H, W, 4), dtype=np.uint8)
    L_bookshop = np.zeros((H, W, 4), dtype=np.uint8)
    L_trade_slots = np.zeros((H, W, 4), dtype=np.uint8)
    L_plaques = np.zeros((H, W, 4), dtype=np.uint8)

    # 1. Warm honey-oak outer frame & burgundy trade-offer counter
    fill_rect(L_oak_frame, 0, 0, 99, 165, C_WOOD_BASE)
    fill_rect(L_oak_frame, 100, 0, 275, 79, C_WOOD_BASE)

    fill_rect(L_oak_frame, 0, 0, 275, 0, C_WOOD_OUTLINE)
    fill_rect(L_oak_frame, 0, 0, 0, 165, C_WOOD_OUTLINE)
    fill_rect(L_oak_frame, 0, 165, 99, 165, C_WOOD_OUTLINE)
    fill_rect(L_oak_frame, 99, 80, 99, 165, C_WOOD_OUTLINE)
    fill_rect(L_oak_frame, 275, 0, 275, 79, C_WOOD_OUTLINE)
    fill_rect(L_oak_frame, 100, 79, 275, 79, C_WOOD_OUTLINE)

    fill_rect(L_oak_frame, 1, 1, 274, 2, C_WOOD_SPEC)
    fill_rect(L_oak_frame, 1, 1, 2, 164, C_WOOD_SPEC)
    fill_rect(L_oak_frame, 3, 3, 272, 3, C_WOOD_LIGHT)
    fill_rect(L_oak_frame, 3, 3, 3, 162, C_WOOD_LIGHT)
    fill_rect(L_oak_frame, 2, 163, 98, 164, C_WOOD_DARK)
    fill_rect(L_oak_frame, 97, 3, 98, 164, C_WOOD_DARK)
    fill_rect(L_oak_frame, 273, 3, 274, 78, C_WOOD_DARK)

    # Restore exact rounded corner alpha on the outer panel
    L_oak_frame[1, 1] = C_WOOD_OUTLINE
    L_oak_frame[1, 2] = C_WOOD_OUTLINE
    L_oak_frame[2, 1] = C_WOOD_OUTLINE
    L_oak_frame[1, 273] = C_WOOD_OUTLINE
    L_oak_frame[2, 274] = C_WOOD_OUTLINE
    L_oak_frame[3, 274] = C_WOOD_OUTLINE
    L_oak_frame[162, 1] = C_WOOD_OUTLINE
    L_oak_frame[163, 1] = C_WOOD_OUTLINE
    L_oak_frame[163, 2] = C_WOOD_OUTLINE
    L_oak_frame[164, 2] = C_WOOD_OUTLINE
    L_oak_frame[164, 3] = C_WOOD_OUTLINE
    for cx, cy in [
        (0, 0), (1, 0), (0, 1),
        (273, 0), (274, 0), (275, 0), (274, 1), (275, 1), (275, 2),
        (0, 163), (0, 164), (1, 164), (0, 165), (1, 165), (2, 165)
    ]:
        L_oak_frame[cy, cx] = (0, 0, 0, 0)

    # Burgundy inset trade-offer well at [5..92, 18..157] (7 rows of 88x20 offers)
    fill_rect(L_oak_frame, 4, 17, 93, 158, C_WOOD_OUTLINE)
    fill_rect(L_oak_frame, 5, 18, 92, 157, C_BURG_BASE)
    fill_rect(L_oak_frame, 5, 18, 92, 19, C_BURG_DEEP)
    fill_rect(L_oak_frame, 5, 18, 6, 157, C_BURG_DEEP)
    for r in range(7):
        ry = 18 + 20 * r
        fill_rect(L_oak_frame, 7, ry + 1, 90, ry + 1, C_BURG_MID)
        fill_rect(L_oak_frame, 6, ry + 19, 91, ry + 19, C_BURG_DARK)
    # Scrollbar recessed channel at [94..99, 18..157]
    fill_rect(L_oak_frame, 93, 17, 99, 158, C_WOOD_OUTLINE)
    fill_rect(L_oak_frame, 94, 18, 99, 157, C_WOOD_DEEP)
    fill_rect(L_oak_frame, 95, 19, 98, 156, rgba("#231307"))

    # 2. C1 Refined Upper-Right Bookshop Alcove, Grounded Counter Lectern, & Calm Counter Apron [104..270, 21..69]
    fill_rect(L_bookshop, 104, 21, 270, 57, C_WOOD_OUTLINE)
    fill_rect(L_bookshop, 105, 22, 269, 56, C_BURG_DARK)

    # Continuous upper bookshelf gallery spanning y=22..30 across the top of the bookshop window
    fill_rect(L_bookshop, 105, 22, 269, 23, C_WOOD_DARK)
    fill_rect(L_bookshop, 105, 30, 269, 31, C_WOOD_BASE)
    fill_rect(L_bookshop, 105, 31, 269, 31, C_WOOD_OUTLINE)
    # Mid shelves on left (x=105..132, y=42..43) and right (x=243..269, y=42..43)
    fill_rect(L_bookshop, 105, 42, 132, 43, C_WOOD_BASE)
    fill_rect(L_bookshop, 243, 42, 269, 43, C_WOOD_BASE)
    # Vertical bookcase dividers at x=132..133 and x=242..243
    fill_rect(L_bookshop, 132, 22, 133, 56, C_WOOD_DARK)
    fill_rect(L_bookshop, 242, 22, 243, 56, C_WOOD_DARK)

    book_palette = [
        (rgba("#A83232"), rgba("#CC4848"), C_GOLD_HI),  # Ruby red tome
        (rgba("#28756A"), rgba("#3B9C8E"), C_GOLD_HI),  # Teal tome
        (rgba("#315699"), rgba("#4975C4"), C_GOLD_HI),  # Sapphire blue tome
        (rgba("#673B99"), rgba("#8854C2"), C_GOLD_HI),  # Violet tome
        (rgba("#8C5A2B"), rgba("#B57A42"), C_GOLD_HI),  # Leather tome
    ]
    # Books along upper gallery (y=24..29) and side mid shelves (y=33..41)
    for shelf_y0, shelf_y1, x_list in [
        (24, 29, [106, 110, 114, 119, 123, 127, 136, 141, 145, 150, 155, 160, 165, 170, 175, 180, 185, 190, 200, 205, 210, 216, 221, 226, 231, 236, 246, 250, 255, 259, 264]),
        (33, 41, [106, 111, 115, 120, 125]),
        (33, 41, [246, 251, 256, 261, 265]),
    ]:
        for idx, bx in enumerate(x_list):
            b_dark, b_light, b_gold = book_palette[(idx + shelf_y0) % len(book_palette)]
            bw = 3 if idx % 2 == 0 else 4
            top_trim = shelf_y0 + (idx % 2)
            fill_rect(L_bookshop, bx, top_trim, bx + bw - 1, shelf_y1, b_dark)
            fill_rect(L_bookshop, bx + 1, top_trim, bx + bw - 2, shelf_y1, b_light)
            L_bookshop[top_trim + 1, bx + 1] = b_gold

    # Central carved oak bookcase divider post at x=195..198, y=22..30 with brass accent
    fill_rect(L_bookshop, 195, 22, 198, 30, C_WOOD_OUTLINE)
    fill_rect(L_bookshop, 196, 22, 197, 30, C_WOOD_BASE)
    fill_rect(L_bookshop, 196, 25, 197, 27, C_GOLD_HI)

    # Left lower counter bay (x=107..131, y=44..56):
    # Physically supported sloped oak lectern resting directly on the counter ledge (y=44..56) with open book on top!
    # Lectern wooden pillar & base planted on counter at y=50..56
    fill_rect(L_bookshop, 113, 50, 125, 56, C_WOOD_OUTLINE)
    fill_rect(L_bookshop, 115, 50, 123, 54, C_WOOD_MID)
    fill_rect(L_bookshop, 112, 54, 126, 56, C_WOOD_LIGHT)
    # Sloped lectern desk + open parchment book at x=108..130, y=44..50
    fill_rect(L_bookshop, 108, 44, 130, 50, C_WOOD_OUTLINE)
    fill_rect(L_bookshop, 109, 45, 118, 49, rgba("#F2E4C4"))
    fill_rect(L_bookshop, 120, 45, 129, 49, rgba("#E4D2AC"))
    fill_rect(L_bookshop, 119, 45, 119, 49, C_WOOD_DARK)
    for py in [46, 48]:
        fill_rect(L_bookshop, 111, py, 117, py, rgba("#8C6F4E"))
        fill_rect(L_bookshop, 121, py, 127, py, rgba("#8C6F4E"))
    # Gold bookmark ribbon hanging from book onto lectern stand
    fill_rect(L_bookshop, 119, 49, 120, 52, C_GOLD_HI)

    # Right lower counter bay (x=245..268, y=44..56):
    # Grounded stack of 2 horizontal books (x=245..255, y=50..56) + brass ink pot & feather quill (x=257..266, y=45..56)
    fill_rect(L_bookshop, 245, 50, 255, 56, C_WOOD_OUTLINE)
    fill_rect(L_bookshop, 246, 51, 254, 52, rgba("#3B9C8E"))
    fill_rect(L_bookshop, 246, 53, 254, 53, rgba("#EADAB8"))
    fill_rect(L_bookshop, 246, 54, 254, 55, rgba("#CC4848"))
    # Brass inkwell resting on counter at y=50..56
    fill_rect(L_bookshop, 257, 50, 264, 56, C_WOOD_OUTLINE)
    fill_rect(L_bookshop, 258, 51, 263, 55, rgba("#252B38"))
    fill_rect(L_bookshop, 258, 51, 263, 51, C_GOLD_HI)
    for qi in range(6):
        L_bookshop[50 - qi, 261 + (qi // 2)] = rgba("#F4EFE0")
        L_bookshop[50 - qi, 262 + (qi // 2)] = rgba("#D9CEB4")

    # Calm Warm Honey-Oak Counter Apron at [104..270, 58..69] (for JEM Economy coin price line at y=59..71 in 0x404040)
    fill_rect(L_bookshop, 104, 58, 270, 69, C_WOOD_OUTLINE)
    fill_rect(L_bookshop, 105, 58, 269, 59, C_OAK_TOP_HI)
    fill_rect(L_bookshop, 105, 60, 269, 67, C_OAK_FACE_BASE)
    fill_rect(L_bookshop, 105, 68, 269, 68, C_OAK_BOT_SHAD)

    # 3. XP Bar Trough (135..238, 15..21) & Trade Slots (135,36), (161,36), Result (215,32 / 220,37) + Trade Arrow
    fill_rect(L_trade_slots, 135, 15, 238, 21, C_WOOD_OUTLINE)
    fill_rect(L_trade_slots, 136, 16, 237, 20, C_WOOD_DEEP)

    for sx in [135, 161]:
        fill_rect(L_trade_slots, sx - 1, 35, sx + 18, 54, C_WOOD_OUTLINE)
        draw_recessed_slot_18x18(
            L_trade_slots, sx, 36,
            top_rim=C_WOOD_OUTLINE, left_rim=C_WOOD_DEEP,
            bot_rim=C_GOLD_HI, right_rim=C_GOLD_BASE
        )

    # Ornate 26x26 Gilded Showcase Frame at (215, 32) around 18x18 Result Slot at (219, 36) [16x16 interior at 220, 37]
    fill_rect(L_trade_slots, 215, 32, 240, 57, C_WOOD_OUTLINE)
    fill_rect(L_trade_slots, 216, 33, 239, 56, C_GOLD_SHAD)
    fill_rect(L_trade_slots, 216, 33, 239, 34, C_GOLD_HI)
    fill_rect(L_trade_slots, 216, 33, 217, 56, C_GOLD_HI)
    fill_rect(L_trade_slots, 218, 35, 237, 54, C_WOOD_DEEP)
    draw_recessed_slot_18x18(
        L_trade_slots, 219, 36,
        top_rim=C_WOOD_OUTLINE, left_rim=C_WOOD_DEEP,
        bot_rim=C_GOLD_HI, right_rim=C_GOLD_BASE
    )

    # Trade Arrow at x=186..208, y=40..50
    fill_rect(L_trade_slots, 186, 43, 201, 47, C_WOOD_OUTLINE)
    fill_rect(L_trade_slots, 187, 44, 201, 45, C_GOLD_HI)
    fill_rect(L_trade_slots, 187, 46, 201, 46, C_GOLD_BASE)
    for i, (y0, y1) in enumerate([(40, 50), (41, 49), (42, 48), (43, 47), (44, 46), (45, 45)]):
        ax = 202 + i
        fill_rect(L_trade_slots, ax, y0, ax, y1, C_WOOD_OUTLINE)
        if y1 > y0:
            fill_rect(L_trade_slots, ax, y0 + 1, ax, y1 - 1, C_GOLD_HI)

    # 4. Warm Honey-Oak Title Plaques:
    draw_oak_plaque(L_plaques, 14, 3, 90, 15)
    draw_oak_plaque(L_plaques, 124, 3, 250, 14)
    # Exact shared 'Inventory' honey-oak plaque slice from shared_comp[0:10, 4:74] at [70:80, 103:173]
    L_plaques[70:80, 103:173] = shared_comp[0:10, 4:74]

    layers = [
        ("01_auxiliary_sprites_and_button_frame", L_aux),
        ("02_shared_lower_inventory_master", L_shared),
        ("03_warm_oak_frame_and_burgundy_well", L_oak_frame),
        ("04_bookshop_shelves_lectern_and_counter", L_bookshop),
        ("05_xp_trough_trade_slots_and_arrow", L_trade_slots),
        ("06_wood_title_and_shared_plaques", L_plaques),
    ]
    comp = np.zeros((H, W, 4), dtype=np.uint8)
    for _, lay in layers:
        mask = lay[:, :, 3] > 0
        comp[mask] = lay[mask]
    return layers, comp


def save_layered_aseprite(asset_name, layers, out_ase_path, out_png_path):
    """Save layer PNGs, assemble a native multi-layer .aseprite file via Aseprite CLI, and export the final PNG."""
    layer_dir = SRC_DIR / f"_layers_{asset_name}"
    layer_dir.mkdir(parents=True, exist_ok=True)
    layer_entries = []
    for lname, larr in layers:
        lpath = layer_dir / f"{lname}.png"
        Image.fromarray(larr, "RGBA").save(lpath)
        layer_entries.append((lname, str(lpath).replace("\\", "/")))

    h, w = layers[0][1].shape[:2]
    lua_lines = [
        f'local spr = Sprite({w}, {h}, ColorMode.RGB)',
        'spr.filename = "' + str(out_ase_path).replace("\\", "/") + '"',
        'local defaultLayer = spr.layers[1]',
    ]
    for idx, (lname, lpath) in enumerate(layer_entries):
        lua_lines.append(f'local lay_{idx} = spr:newLayer()')
        lua_lines.append(f'lay_{idx}.name = "{lname}"')
        lua_lines.append(f'local img_{idx} = Image{{ fromFile="{lpath}" }}')
        lua_lines.append(f'spr:newCel(lay_{idx}, 1, img_{idx}, Point(0, 0))')
    lua_lines.append('spr:deleteLayer(defaultLayer)')
    lua_lines.append(f'spr:saveAs("{str(out_ase_path).replace(chr(92), "/")}")')
    lua_lines.append(f'spr:saveCopyAs("{str(out_png_path).replace(chr(92), "/")}")')
    lua_path = layer_dir / f"build_{asset_name}.lua"
    lua_path.write_text("\n".join(lua_lines), encoding="utf-8")

    subprocess.run([str(ASEPRITE_EXE), "-b", "--script", str(lua_path)], check=True)


def main():
    print("Building C1 shared_inventory_master...")
    shared_layers, shared_comp = build_shared_inventory_master()
    save_layered_aseprite(
        "shared_inventory_master",
        shared_layers,
        SRC_DIR / "shared_inventory_master.aseprite",
        CAND_DIR / "shared_inventory_master.png",
    )
    Image.fromarray(shared_comp[10:96, 0:176], "RGBA").save(CAND_DIR / "shared_inventory_lower_176x86.png")

    print("Building C1 inventory.aseprite -> inventory.png...")
    inv_layers, inv_comp = build_inventory_gui(shared_comp)
    save_layered_aseprite(
        "inventory",
        inv_layers,
        SRC_DIR / "inventory.aseprite",
        CAND_DIR / "inventory.png",
    )

    print("Building C1 anvil.aseprite -> anvil.png...")
    anv_layers, anv_comp = build_anvil_gui(shared_comp)
    save_layered_aseprite(
        "anvil",
        anv_layers,
        SRC_DIR / "anvil.aseprite",
        CAND_DIR / "anvil.png",
    )

    print("Building C1 villager2.aseprite -> villager2.png...")
    vil_layers, vil_comp = build_villager2_gui(shared_comp)
    save_layered_aseprite(
        "villager2",
        vil_layers,
        SRC_DIR / "villager2.aseprite",
        CAND_DIR / "villager2.png",
    )

    inv_exp = np.array(Image.open(CAND_DIR / "inventory.png").convert("RGBA"))
    anv_exp = np.array(Image.open(CAND_DIR / "anvil.png").convert("RGBA"))
    vil_exp = np.array(Image.open(CAND_DIR / "villager2.png").convert("RGBA"))
    sh_exp = np.array(Image.open(CAND_DIR / "shared_inventory_master.png").convert("RGBA"))

    assert inv_exp.shape == (256, 256, 4)
    assert anv_exp.shape == (256, 256, 4)
    assert vil_exp.shape == (256, 512, 4)
    assert sh_exp.shape == (96, 176, 4)

    eq_inv_anv = bool(np.array_equal(inv_exp[80:166, 0:176], anv_exp[80:166, 0:176]))
    eq_inv_vil = bool(np.array_equal(inv_exp[80:166, 0:176], vil_exp[80:166, 100:276]))
    eq_master = bool(np.array_equal(inv_exp[80:166, 0:176], sh_exp[10:96, 0:176]))
    eq_plaque_anv_vil = bool(np.array_equal(anv_exp[70:80, 4:74], vil_exp[70:80, 103:173]))
    eq_plaque_master = bool(np.array_equal(anv_exp[70:80, 4:74], sh_exp[0:10, 4:74]))

    print(f"Shared lower 176x86 pixel equality: inv==anv={eq_inv_anv}, inv==vil={eq_inv_vil}, inv==master={eq_master}")
    print(f"Shared 'Inventory' 70x10 plaque equality: anv==vil={eq_plaque_anv_vil}, anv==master={eq_plaque_master}")
    assert eq_inv_anv and eq_inv_vil and eq_master and eq_plaque_anv_vil and eq_plaque_master

    orig_inv = np.array(Image.open(ORIG_DIR / "inventory-original.png").convert("RGBA"))
    orig_anv = np.array(Image.open(ORIG_DIR / "anvil-original.png").convert("RGBA"))
    orig_vil = np.array(Image.open(ORIG_DIR / "villager2-original.png").convert("RGBA"))

    inv_alpha_diff = int(np.sum((inv_exp[:, :, 3] > 0) != (orig_inv[:, :, 3] > 0)))
    anv_alpha_diff = int(np.sum((anv_exp[:, :, 3] > 0) != (orig_anv[:, :, 3] > 0)))

    inv_aux_mask = np.ones((256, 256), dtype=bool)
    inv_aux_mask[0:166, 0:176] = False
    inv_aux_identical = bool(np.array_equal(inv_exp[inv_aux_mask], orig_inv[inv_aux_mask]))

    vil_alpha_delta_map = (vil_exp[:, :, 3] > 0) != (orig_vil[:, :, 3] > 0)
    vil_total_alpha_diff = int(np.sum(vil_alpha_delta_map))
    vil_btn_alpha_diff = int(np.sum(vil_alpha_delta_map[74:99, 280:366]))
    vil_corner_alpha_diff = int(np.sum(vil_alpha_delta_map[163:166, 100:103]))
    vil_unexplained_alpha_diff = vil_total_alpha_diff - vil_btn_alpha_diff - vil_corner_alpha_diff

    print(f"Alpha diffs: inventory={inv_alpha_diff} (aux byte-identical={inv_aux_identical}), anvil={anv_alpha_diff}")
    print(f"Villager2 alpha diffs: total={vil_total_alpha_diff} (button_interior={vil_btn_alpha_diff}, shared_corner={vil_corner_alpha_diff}, unexplained={vil_unexplained_alpha_diff})")
    assert inv_alpha_diff == 0 and inv_aux_identical
    assert anv_alpha_diff == 0
    assert vil_unexplained_alpha_diff == 0

    print("ALL C1 TECHNICAL GATES PASSED!")


if __name__ == "__main__":
    main()
