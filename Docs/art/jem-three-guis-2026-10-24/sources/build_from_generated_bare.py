import json
import hashlib
import subprocess
import zipfile
from pathlib import Path
from PIL import Image
import numpy as np

OUT_ROOT = Path("C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/jem-three-guis-2026-10-24")
SRC_DIR = OUT_ROOT / "sources"
CAND_DIR = OUT_ROOT / "candidates"
PREV_DIR = OUT_ROOT / "previews"
PACK_DIR = OUT_ROOT / "pack"
ORIG_DIR = Path("C:/Users/user/.gemini/antigravity/brain/069aca9c-07d8-4d9f-9ab6-92e58bac4b58/scratch/unzipped_2")
GEN_DIR = Path("C:/Users/user/.gemini/antigravity/brain/069aca9c-07d8-4d9f-9ab6-92e58bac4b58")
ASEPRITE_EXE = Path("C:/Users/user/.codex/tools/aseprite/build-1.3.18.6/bin/aseprite.exe")


def sample_piecewise(src_arr, x_knots_mc, x_knots_gen, y_knots_mc, y_knots_gen, out_w, out_h):
    """
    Piecewise-linear area-median resampling from high-res generated pixel art (src_arr)
    onto an exact (out_h, out_w) Minecraft UV grid defined by control knots.
    """
    mc_xs = np.arange(out_w + 1, dtype=np.float64)
    mc_ys = np.arange(out_h + 1, dtype=np.float64)
    gen_x_bounds = np.interp(mc_xs, x_knots_mc, x_knots_gen)
    gen_y_bounds = np.interp(mc_ys, y_knots_mc, y_knots_gen)

    out = np.zeros((out_h, out_w, 4), dtype=np.uint8)
    H_s, W_s = src_arr.shape[:2]

    for y in range(out_h):
        gy0 = int(np.floor(gen_y_bounds[y]))
        gy1 = int(np.ceil(gen_y_bounds[y + 1]))
        gy0 = max(0, min(H_s - 1, gy0))
        gy1 = max(gy0 + 1, min(H_s, gy1))
        span_y = gy1 - gy0
        if span_y >= 3:
            cy0 = gy0 + span_y // 4
            cy1 = max(cy0 + 1, gy1 - span_y // 4)
        else:
            cy0, cy1 = gy0, gy1

        for x in range(out_w):
            gx0 = int(np.floor(gen_x_bounds[x]))
            gx1 = int(np.ceil(gen_x_bounds[x + 1]))
            gx0 = max(0, min(W_s - 1, gx0))
            gx1 = max(gx0 + 1, min(W_s, gx1))
            span_x = gx1 - gx0
            if span_x >= 3:
                cx0 = gx0 + span_x // 4
                cx1 = max(cx0 + 1, gx1 - span_x // 4)
            else:
                cx0, cx1 = gx0, gx1

            patch = src_arr[cy0:cy1, cx0:cx1, :3]
            med = np.median(patch.reshape(-1, 3), axis=0)
            out[y, x, 0] = int(round(med[0]))
            out[y, x, 1] = int(round(med[1]))
            out[y, x, 2] = int(round(med[2]))
            out[y, x, 3] = 255
    return out


def extract_canonical_silver_slot_18x18(inv_gen):
    """
    Extract a crisp, noise-free 18x18 silver slot cell by taking the pixelwise median across
    the 9x3 lower inventory slots in inventory_bare_gen, and ensuring a calm 16x16 interior.
    """
    # In inv_gen, the 9 columns span x = 40.0 .. 826.0 (pitch = 87.33px per 18px slot)
    # Wait: earlier we saw x edges at:
    # Col 0: 40..118 (78px), Col 1: 118..196 (78px), ..., Col 8: 666..744 (78px), Col 9: 744..822 (78px)!
    # Aha! inv_gen drew TEN columns (78.2px each from 40 to 822)!
    # So each individual slot in inv_gen is 78.2px wide!
    slot_samples = []
    for r in range(3):
        sy0 = 573.0 + r * 80.0
        sy1 = sy0 + 80.0
        for c in range(9):
            sx0 = 40.0 + c * 78.22
            sx1 = sx0 + 78.22
            cell = sample_piecewise(
                inv_gen,
                [0, 1, 17, 18], [sx0, sx0 + 4.5, sx1 - 4.5, sx1],
                [0, 1, 17, 18], [sy0, sy0 + 4.5, sy1 - 4.5, sy1],
                18, 18
            )
            slot_samples.append(cell)
    med_slot = np.median(np.stack(slot_samples, axis=0), axis=0).astype(np.uint8)
    interior = med_slot[1:17, 1:17, :3].astype(np.float32)
    backwall_color = np.median(interior[2:14, 2:14].reshape(-1, 3), axis=0).astype(np.uint8)
    overhang1_color = np.median(interior[0:1, 1:15].reshape(-1, 3), axis=0).astype(np.uint8)
    overhang2_color = np.median(interior[1:2, 1:15].reshape(-1, 3), axis=0).astype(np.uint8)
    left_wall_color = np.median(interior[2:14, 0:1].reshape(-1, 3), axis=0).astype(np.uint8)
    right_wall_color = np.median(interior[2:14, 15:16].reshape(-1, 3), axis=0).astype(np.uint8)
    shelf_color = np.median(interior[14:16, 1:15].reshape(-1, 3), axis=0).astype(np.uint8)

    for dy in range(1, 17):
        for dx in range(1, 17):
            if dy == 1:
                rgb = overhang1_color
            elif dy == 2:
                rgb = overhang2_color
            elif dy >= 15:
                rgb = shelf_color
            elif dx == 1:
                rgb = left_wall_color
            elif dx == 16:
                rgb = right_wall_color
            else:
                rgb = backwall_color
            med_slot[dy, dx, :3] = rgb
            med_slot[dy, dx, 3] = 255
    return med_slot


def save_aseprite_from_png(png_path, ase_path):
    lua_path = SRC_DIR / "_tmp_save.lua"
    p_in = str(png_path).replace("\\", "/")
    p_out = str(ase_path).replace("\\", "/")
    lua_path.write_text(
        f'local spr = app.open("{p_in}")\nspr:saveAs("{p_out}")\nspr:close()\n',
        encoding="utf-8"
    )
    subprocess.run([str(ASEPRITE_EXE), "-b", "--script", str(lua_path)], check=True)
    lua_path.unlink(missing_ok=True)


def build_all():
    inv_gen = np.array(Image.open(GEN_DIR / "inventory_bare_gen_1792827877375.jpg").convert("RGBA"))
    anv_gen = np.array(Image.open(GEN_DIR / "anvil_bare_gen_1792827921853.jpg").convert("RGBA"))
    vil_gen = np.array(Image.open(GEN_DIR / "villager2_bare_gen_1792828025711.jpg").convert("RGBA"))

    orig_inv = np.array(Image.open(ORIG_DIR / "inventory-original.png").convert("RGBA"))
    orig_anv = np.array(Image.open(ORIG_DIR / "anvil-original.png").convert("RGBA"))
    orig_vil = np.array(Image.open(ORIG_DIR / "villager2-original.png").convert("RGBA"))

    slot_18 = extract_canonical_silver_slot_18x18(inv_gen)

    # =========================================================================
    # 1. SHARED INVENTORY & HOTBAR MASTER (176x96: y=70..165)
    # =========================================================================
    # Note: inv_gen drew 10 columns (40..822) and 4 rows (573..889).
    # We map the first 9 columns (40..744) to x_mc = 7..169, and map the right silver border (822..855)
    # directly to x_mc = 169..176 (skipping the extra 10th column 744..822 so there is ZERO slit on the right rail!).
    shared_master = np.zeros((96, 176, 4), dtype=np.uint8)

    # Left rail + 9 columns (x_mc = 0..169 from x_gen = 4..744)
    x_knots_left9_mc = [0, 7] + [7 + 18 * c for c in range(1, 10)]
    x_knots_left9_gen = [4.0, 40.0] + [40.0 + 78.22 * c for c in range(1, 10)]

    # 9x3 section (dy = 10..66, 57px tall) from y_gen = 550..813 (skipping extra 4th row 813..889)
    y_knots_9x3_mc = [0, 3, 21, 39, 57]
    y_knots_9x3_gen = [550.0, 573.0, 653.0, 733.0, 813.0]
    rrail_9x3 = sample_piecewise(inv_gen, [0, 7], [814.0, 852.0], [0, 57], [40.0, 440.0], 7, 57)
    shared_master[10:67, 0:169] = sample_piecewise(inv_gen, x_knots_left9_mc, x_knots_left9_gen, y_knots_9x3_mc, y_knots_9x3_gen, 169, 57)
    shared_master[10:67, 169:176] = rrail_9x3

    # Raised hotbar section (dy = 67..95, 29px tall) from y_gen = 890..1020
    y_knots_hb_mc = [0, 2, 4, 22, 29]
    y_knots_hb_gen = [890.0, 898.0, 910.0, 988.0, 1020.0]
    rrail_hb = sample_piecewise(inv_gen, [0, 7], [814.0, 852.0], [0, 29], [360.0, 495.0], 7, 29)
    shared_master[67:96, 0:169] = sample_piecewise(inv_gen, x_knots_left9_mc, x_knots_left9_gen, y_knots_hb_mc, y_knots_hb_gen, 169, 29)
    shared_master[67:96, 169:176] = rrail_hb

    # Stamp crisp median-filtered 18x18 slot cells at the exact 9x3 and 9x1 coordinates
    for r in range(3):
        for c in range(9):
            shared_master[13 + 18 * r:13 + 18 * r + 18, 7 + 18 * c:7 + 18 * c + 18] = slot_18
    for c in range(9):
        shared_master[71:89, 7 + 18 * c:7 + 18 * c + 18] = slot_18

    # Shared 'Inventory' honey-oak wooden sign at y_mc = 70..79 (dy=0..9), x=6..69 (64x10)
    plaque_64x10 = sample_piecewise(
        inv_gen,
        [0, 2, 62, 64], [61.0, 70.0, 302.0, 311.0],
        [0, 1, 9, 10], [518.0, 523.0, 559.0, 564.0],
        64, 10
    )
    shared_master[0:10, 6:70] = plaque_64x10

    for cx, cy in [(0, 93), (0, 94), (1, 94), (175, 94), (0, 95), (1, 95), (2, 95), (174, 95), (175, 95)]:
        shared_master[cy, cx] = (0, 0, 0, 0)

    # =========================================================================
    # 2. PLAYER INVENTORY (inventory.png, 256x256 RGBA)
    # =========================================================================
    inv_out = orig_inv.copy()
    inv_out[0:166, 0:176] = 0
    inv_out[80:166, 0:176] = shared_master[10:96, 0:176]

    # Sample upper panel [0:80, 0:176] cleanly without any ghost sign/slot in the middle column (x=76..96):
    # Left equipment bay [0:80, 0:76] (armor column x=7..24 + black player preview box x=25..75)
    inv_up_left = sample_piecewise(
        inv_gen,
        [0, 7, 25, 76],
        [4.0, 40.0, 120.0, 382.0],
        [0, 7, 79, 80],
        [7.0, 42.0, 440.0, 498.0],
        76, 80
    )
    # Right crafting bay [0:80, 96:176] (wooden Crafting sign, 2x2 grid, arrow, result slot)
    inv_up_right = sample_piecewise(
        inv_gen,
        [0, 1, 19, 37, 57, 75, 80],
        [430.0, 435.0, 520.0, 606.0, 712.0, 804.0, 852.0],
        [0, 3, 15, 17, 27, 35, 45, 53, 80],
        [7.0, 36.0, 112.0, 136.0, 184.0, 222.0, 278.0, 312.0, 498.0],
        80, 80
    )
    # Center bridge [0:80, 76:96] sampled from clean silver surface in inv_gen (x=384..426)
    inv_up_mid = sample_piecewise(
        inv_gen,
        [0, 20], [384.0, 426.0],
        [0, 7, 60, 79, 80], [7.0, 42.0, 350.0, 440.0, 498.0],
        20, 80
    )
    inv_out[0:80, 0:76] = inv_up_left
    inv_out[0:80, 76:96] = inv_up_mid
    inv_out[0:80, 96:176] = inv_up_right

    # Clean the region below the 2x2 crafting grid [54:76, 95:135] with smooth brushed silver from inv_gen[340:440, 520:680]
    silver_patch = sample_piecewise(inv_gen, [0, 40], [520.0, 680.0], [0, 22], [340.0, 440.0], 40, 22)
    inv_out[54:76, 95:135] = silver_patch

    # Stamp clean 18x18 slots at all upper inventory slot coordinates:
    for r in range(4):
        inv_out[7 + 18 * r:25 + 18 * r, 7:25] = slot_18
    inv_out[61:79, 76:94] = slot_18
    for r in range(2):
        for c in range(2):
            inv_out[17 + 18 * r:35 + 18 * r, 97 + 18 * c:115 + 18 * c] = slot_18
    inv_out[27:45, 153:171] = slot_18

    # Ensure top silver border above x=80..96 is continuous before the wooden Crafting plaque at x=92..166
    plaque_craft = sample_piecewise(
        inv_gen,
        [0, 2, 72, 74], [433.0, 442.0, 772.0, 781.0],
        [0, 1, 11, 12], [36.0, 42.0, 104.0, 110.0],
        74, 12
    )
    inv_out[3:15, 92:166] = plaque_craft

    for cx, cy in [(0, 0), (1, 0), (0, 1), (173, 0), (174, 0), (175, 0), (174, 1), (175, 1), (175, 2)]:
        inv_out[cy, cx] = (0, 0, 0, 0)

    # =========================================================================
    # 3. ANVIL GUI (anvil.png, 256x256 RGBA)
    # =========================================================================
    anv_out = orig_anv.copy()
    anv_out[0:166, 0:176] = 0
    anv_out[80:166, 0:176] = shared_master[10:96, 0:176]

    anv_upper = np.zeros((80, 176, 4), dtype=np.uint8)
    # Band A (y=0..16): Top forged-iron header + wooden 'Repair & Name' sign centered at x=52..154
    anv_upper[0:16, 0:176] = sample_piecewise(
        anv_gen,
        [0, 52, 154, 176], [0.0, 225.0, 695.0, 858.0],
        [0, 3, 15, 16], [0.0, 8.0, 82.0, 92.0],
        176, 16
    )
    # Band B (y=16..44, 28px tall): Left pillar + 3D forged anvil (x=0..58 from 0..268),
    # Rename box (x=58..169 from 268..760), Right chain pillar (x=169..176 from 760..858)!
    anv_upper[16:44, 0:176] = sample_piecewise(
        anv_gen,
        [0, 12, 54, 59, 169, 176],
        [0.0, 64.0, 226.0, 270.0, 759.0, 858.0],
        [0, 4, 20, 28],
        [92.0, 125.0, 234.0, 342.0],
        176, 28
    )
    # Re-sample the 3D forged anvil at natural proportions into [15:44, 11:51] so its horn, table, waist, and base are super crisp:
    anvil_prop = sample_piecewise(
        anv_gen,
        [0, 40], [64.0, 230.0],
        [0, 29], [162.0, 342.0],
        40, 29
    )
    anv_upper[15:44, 11:51] = anvil_prop

    # Band C (y=44..80, 36px tall): 3 slots at (26,46), (75,46), (133,46), plus sign, arrow, and lower iron frame
    anv_upper[44:80, 0:176] = sample_piecewise(
        anv_gen,
        [0, 26, 44, 75, 93, 133, 151, 176],
        [0.0, 226.0, 314.0, 414.0, 503.0, 619.0, 712.0, 858.0],
        [0, 2, 20, 36],
        [355.0, 372.0, 460.0, 512.0],
        176, 36
    )

    # Composite the glowing warm ember lantern at natural proportions into [41:63, 152:168] (right of Result Slot 133..150)
    lantern_prop = sample_piecewise(
        anv_gen,
        [0, 16], [656.0, 748.0],
        [0, 22], [224.0, 338.0],
        16, 22
    )
    anv_upper[41:63, 152:168] = lantern_prop

    # Stamp clean 18x18 dark-iron slots at (26,46), (75,46), (133,46)
    anv_slot_samples = []
    for sx0, sx1 in [(226.0, 314.0), (414.0, 503.0), (619.0, 712.0)]:
        s = sample_piecewise(
            anv_gen,
            [0, 1, 17, 18], [sx0, sx0 + 5.0, sx1 - 5.0, sx1],
            [0, 1, 17, 18], [372.0, 377.0, 455.0, 460.0],
            18, 18
        )
        anv_slot_samples.append(s)
    anv_slot_18 = np.median(np.stack(anv_slot_samples, axis=0), axis=0).astype(np.uint8)
    anv_slot_18[1:17, 1:17] = slot_18[1:17, 1:17]
    for sx in [26, 75, 133]:
        anv_upper[46:64, sx:sx + 18] = anv_slot_18

    # Paste exact shared 'Inventory' honey-oak plaque at [70:80, 6:70]
    anv_upper[70:80, 6:70] = shared_master[0:10, 6:70]
    anv_out[0:80, 0:176] = anv_upper

    for cx, cy in [(0, 0), (1, 0), (0, 1), (173, 0), (174, 0), (175, 0), (174, 1), (175, 1), (175, 2)]:
        anv_out[cy, cx] = (0, 0, 0, 0)

    # Auxiliary rename field sprites at (0,166,110,16) and (0,182,110,16)
    ren_active = anv_upper[20:36, 59:169].copy()
    anv_out[166:182, 0:110] = ren_active
    ren_disabled = ren_active.copy()
    ren_disabled[:, :, :3] = (ren_disabled[:, :, :3].astype(np.float32) * 0.72).astype(np.uint8)
    anv_out[182:198, 0:110] = ren_disabled

    # Seamless error cross at (176, 0, 28, 21)
    anv_out[0:21, 176:204] = anv_out[45:66, 99:127]
    orig_err = orig_anv[0:21, 176:204]
    orig_arrow_patch = orig_anv[45:66, 99:127]
    x_diff_mask = np.any(orig_err[:, :, :3] != orig_arrow_patch[:, :, :3], axis=2)
    for dy in range(21):
        for dx in range(28):
            if x_diff_mask[dy, dx]:
                r, g, b, a = orig_err[dy, dx]
                lum = int(0.299 * r + 0.587 * g + 0.114 * b)
                if lum < 65:
                    anv_out[dy, 176 + dx] = (38, 12, 16, 255)
                elif lum < 120:
                    anv_out[dy, 176 + dx] = (182, 38, 42, 255)
                else:
                    anv_out[dy, 176 + dx] = (235, 76, 64, 255)

    # =========================================================================
    # 4. LIBRARIAN TRADING GUI (villager2.png, 512x256 RGBA)
    # =========================================================================
    vil_out = orig_vil.copy()
    vil_out[0:166, 0:276] = 0
    vil_out[74:99, 280:366] = (0, 0, 0, 0)
    vil_out[80:166, 100:276] = shared_master[10:96, 0:176]

    # Left Trade Column [0:166, 0:100] from vil_gen[13:843, 1:628]
    vil_left = sample_piecewise(
        vil_gen,
        [0, 5, 14, 90, 93, 100],
        [1.0, 32.0, 170.0, 465.0, 588.0, 628.0],
        [0, 3, 15, 18, 158, 166],
        [13.0, 20.0, 74.0, 92.0, 795.0, 843.0],
        100, 166
    )
    vil_out[0:166, 0:100] = vil_left

    # Upper-Right Bookshop & Counter [0:80, 100:276] (176x80) from vil_gen[13:424, 638:1262]:
    vil_up_right = np.zeros((80, 176, 4), dtype=np.uint8)
    # Band 1 (y=0..35, 35px tall): Wooden 'Librarian' sign + bookshelves + open book on lectern + inkwell/quill + glowing lantern
    # Sampled with uniform horizontal proportions across x=638..1262 so nothing is distorted!
    vil_up_right[0:35, 0:176] = sample_piecewise(
        vil_gen,
        [0, 30, 146, 176],
        [638.0, 790.0, 1110.0, 1262.0],
        [0, 3, 14, 35],
        [13.0, 18.0, 72.0, 295.0],
        176, 35
    )
    # Band 2 (y=35..80, 45px tall): Burgundy trading counter with exact 1-to-1 slot knots for (135,36), (161,36), and (219,36)!
    # In vil_gen: Slot 0 is 933..995, Slot 1 is 1012..1073, Result Slot 2 is 1148..1210!
    vil_up_right[35:80, 0:176] = sample_piecewise(
        vil_gen,
        [0, 35, 53, 61, 79, 119, 137, 176],
        [638.0, 933.0, 995.0, 1012.0, 1073.0, 1148.0, 1210.0, 1262.0],
        [0, 1, 19, 34, 45],
        [295.0, 306.0, 371.0, 412.0, 424.0],
        176, 45
    )
    # Stamp crisp 18x18 trade slots at dx = 35 (135,36), dx = 61 (161,36), and dx = 119 (219,36)
    vil_slot_18 = sample_piecewise(
        vil_gen,
        [0, 1, 17, 18], [933.0, 938.0, 990.0, 995.0],
        [0, 1, 17, 18], [306.0, 311.0, 366.0, 371.0],
        18, 18
    )
    vil_slot_18[1:17, 1:17] = slot_18[1:17, 1:17]
    for dx in [35, 61, 119]:
        vil_up_right[36:54, dx:dx + 18] = vil_slot_18

    # Paste exact shared 'Inventory' honey-oak plaque at [70:80, 105:169] (dx = 5..69)
    vil_up_right[70:80, 5:69] = shared_master[0:10, 6:70]
    vil_out[0:80, 100:276] = vil_up_right

    # Clean any dark-gray background pixels (#484944) at the stepped outer corners of vil_out[0:166, 0:276]
    # by filling corner bevels from the adjacent oak frame so the 276x166 silhouette is crisp
    for y in range(0, 15):
        for x in range(0, 15):
            r, g, b, _ = vil_out[y, x]
            if abs(int(r) - 72) < 12 and abs(int(g) - 73) < 12 and abs(int(b) - 68) < 12:
                vil_out[y, x] = (58, 34, 16, 255)
        for x in range(255, 276):
            r, g, b, _ = vil_out[y, x]
            if abs(int(r) - 72) < 12 and abs(int(g) - 73) < 12 and abs(int(b) - 68) < 12:
                vil_out[y, x] = (58, 34, 16, 255)

    for cx, cy in [
        (0, 0), (1, 0), (0, 1),
        (273, 0), (274, 0), (275, 0), (274, 1), (275, 1), (275, 2),
        (0, 163), (0, 164), (1, 164), (0, 165), (1, 165), (2, 165)
    ]:
        vil_out[cy, cx] = (0, 0, 0, 0)

    # Save PNGs and update .aseprite files
    Image.fromarray(shared_master, "RGBA").save(CAND_DIR / "shared_inventory_master.png")
    Image.fromarray(shared_master[10:96, 0:176], "RGBA").save(CAND_DIR / "shared_inventory_lower_176x86.png")
    Image.fromarray(inv_out, "RGBA").save(CAND_DIR / "inventory.png")
    Image.fromarray(anv_out, "RGBA").save(CAND_DIR / "anvil.png")
    Image.fromarray(vil_out, "RGBA").save(CAND_DIR / "villager2.png")

    save_aseprite_from_png(CAND_DIR / "shared_inventory_master.png", SRC_DIR / "shared_inventory_master.aseprite")
    save_aseprite_from_png(CAND_DIR / "inventory.png", SRC_DIR / "inventory.aseprite")
    save_aseprite_from_png(CAND_DIR / "anvil.png", SRC_DIR / "anvil.aseprite")
    save_aseprite_from_png(CAND_DIR / "villager2.png", SRC_DIR / "villager2.aseprite")

    assert np.array_equal(inv_out[80:166, 0:176], anv_out[80:166, 0:176])
    assert np.array_equal(inv_out[80:166, 0:176], vil_out[80:166, 100:276])
    assert np.array_equal(anv_out[70:80, 6:70], vil_out[70:80, 105:169])
    assert int(np.sum((inv_out[:, :, 3] > 0) != (orig_inv[:, :, 3] > 0))) == 0
    assert int(np.sum((anv_out[:, :, 3] > 0) != (orig_anv[:, :, 3] > 0))) == 0
    print("ALL REFINED GENERATED-TO-NATIVE UV GATES PASSED!")


if __name__ == "__main__":
    build_all()
