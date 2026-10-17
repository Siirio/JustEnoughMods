"""One-shot extension of the explicitly approved two-cel silhouette animation.

Author native pixels externally; use the unchanged Blockbench dome for preview.
Historical approved-loop files are immutable inputs, not regenerated outputs.
"""
from pathlib import Path
import base64, io, json, zipfile
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt, median_filter
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
OUT = ROOT / 'one-shot'
W, H, COUNT, FPS = 256, 64, 36, 24


def signed_distance(mask):
    tiled = np.tile(mask, (1, 3))
    if not tiled.any():
        return np.full(mask.shape, -80.)
    return (distance_transform_edt(tiled) - distance_transform_edt(~tiled))[:, W:2*W]


def ease(t):
    return t*t*(3-2*t)


def main():
    OUT.mkdir(exist_ok=True)
    model = json.loads((ROOT/'approved-loop/original-user-frames.bbmodel').read_text(encoding='utf-8'))
    raw = base64.b64decode(model['textures'][0]['source'].split(',', 1)[1])
    source = Image.open(io.BytesIO(raw)).convert('RGBA')
    keys = [np.array(source.crop((0, i*H, W, (i+1)*H))) for i in range(2)]
    # Keep the approved signed-distance interpolation of coherent authored masks.
    original_palette = [(255,223,21,255), (255,238,130,255),
                        (255,241,241,255), (255,255,255,255)]
    distances = []
    for cel in keys:
        indices = np.zeros((H, W), dtype=np.uint8)
        for rank, color in enumerate(original_palette, 1):
            indices[np.all(cel == color, axis=2)] = rank
        distances.append([signed_distance(indices >= rank) for rank in range(1, 5)])
    y, x = np.mgrid[:H, :W]
    palette = np.array([
        [141, 59, 15], [177, 78, 18], [212, 104, 22], [239, 137, 29],
        [255, 173, 42], [255, 203, 66], [255, 225, 105], [255, 237, 152],
        [255, 246, 199], [255, 252, 231],
    ], dtype=np.uint8)
    frames = []
    body_layers, crest_layers = [], []
    coverage = np.zeros((H, W), dtype=bool)
    # Monotonic reveal is the one-shot extension. Flow can move around the shell,
    # but pixels already reached cannot become an uncovered hole behind the wave.
    for i in range(COUNT):
        progress = min(1., i/29)
        travel = ease(progress)
        morph = ease(min(1., progress*1.6))
        regions = [(1-morph)*distances[0][rank] + morph*distances[1][rank]
                   for rank in range(4)]
        field = regions[0]
        boundary = np.full(W, np.nan)
        for u in range(W):
            rows = np.flatnonzero(field[:, u] > 0)
            runs = np.split(rows, np.flatnonzero(np.diff(rows) > 1)+1)
            run = max(runs, key=len)
            if len(run) >= 4:
                boundary[u] = run[0]
        valid = np.flatnonzero(np.isfinite(boundary))
        boundary = np.interp(np.arange(W), valid, boundary[valid], period=W)
        # Trace the main painted mass, not detached single-pixel brush crumbs.
        # This conditions the motion path, never blurs/resamples exported pixels.
        boundary = median_filter(boundary, size=7, mode='wrap')
        boundary -= np.linspace(0, boundary[-1]-boundary[0], W)
        # A full 2pi travel in U, with wrap interpolation at native texel resolution.
        sample_x = (np.arange(W) - W*travel) % (W-1)
        contour = np.interp(sample_x, np.arange(W), boundary)
        contour = (contour - contour.mean())*.62
        front = 82 - 112*travel + contour
        depth = y-front[None, :]
        if i == 0:
            depth[:] = -100
        if progress == 1:
            depth = y+24.
        coverage |= depth >= 0
        # Palette steps describe amber depth, golden body and an ivory moving core.
        # No resampling filter/blur; all alphas belong to exact native texels.
        vertical = 1-y/(H-1)
        body_value = .60+.22*vertical+.04*np.sin((x/W)*2*np.pi-y*.09)
        body_index = np.clip(np.floor(body_value*9), 4, 8).astype(int)
        body = np.zeros((H, W, 4), dtype=np.uint8)
        body[:, :, :3] = palette[body_index]
        body[:, :, 3] = np.where(coverage, np.round(110+25*vertical), 0).astype('uint8')
        # A narrow rim travels with the changing silhouette and relaxes into the
        # transparent field. Discrete value bands keep the native pixel language.
        crest_strength = np.clip(1-np.abs(depth-2)/12, 0, 1)
        # Retain the approved nested light regions as subtle authored crest
        # accents, while the new palette/alpha supplies the requested gradients.
        accents = np.maximum.reduce([np.clip((region+4)/8, 0, 1)
                                     for region in regions[1:]])
        accents = np.roll(accents, round(W*travel), axis=1)
        crest_strength *= .90+.10*accents
        crest_index = np.clip((crest_strength*9).astype(int), 0, 9)
        crest = np.zeros_like(body)
        crest[:, :, :3] = palette[crest_index]
        crest[:, :, 3] = np.where(coverage, np.round(crest_strength*188), 0).astype('uint8')
        # Match seam columns, including the full-coverage settled state.
        body[:, -1] = body[:, 0]
        crest[:, -1] = crest[:, 0]
        b, c = Image.fromarray(body), Image.fromarray(crest)
        frame = Image.alpha_composite(b, c)
        frames.append(frame)
        body_layers.append(b)
        crest_layers.append(c)
    atlas = Image.new('RGBA', (W, H*COUNT))
    for i, frame in enumerate(frames):
        atlas.paste(frame, (0, i*H))
    atlas.save(OUT/'gold-wave-one-shot.png')
    buf = io.BytesIO(); atlas.save(buf, format='PNG')
    model['name'] = 'guardian-one-shot'
    model['textures'][0].update(
        name='gold-wave-one-shot', path='', width=W, height=H*COUNT,
        uv_width=W, uv_height=H, fps=FPS, frame_interpolate=False,
        render_mode='emissive', internal=True, saved=False,
        source='data:image/png;base64,'+base64.b64encode(buf.getvalue()).decode())
    clip = model['animations'][0]
    clip.update(name='guardian_appear_once', loop='hold', length=(COUNT-1)/FPS, snapping=FPS)
    for animator in clip['animators'].values():
        for key in animator.get('keyframes', []):
            key['time'] *= clip['length']/1.5
    (OUT/'guardian-one-shot.bbmodel').write_text(json.dumps(model, indent=2)+'\n', encoding='utf-8')
    timing = dict(frameWidth=W, frameHeight=H, frames=COUNT, fps=FPS,
                  durationSeconds=COUNT/FPS, playback='once_hold_last',
                  revealCompleteFrame=29, fullSurfaceCoverage=True,
                  blend='emissive straight alpha; body 43-53 percent; composite crest up to 88 percent',
                  runtimeFrame='min(floor(ageSeconds * fps), frames - 1)')
    (OUT/'timing.json').write_text(json.dumps(timing, indent=2)+'\n')
    # External native-size editable cels, with body and moving crest separated.
    doc=ET.Element('image', w=str(W), h=str(H), name='Guardian one-shot 24 FPS')
    stack=ET.SubElement(doc, 'stack')
    with zipfile.ZipFile(OUT/'gold-wave-cels.ora', 'w') as z:
        z.writestr('mimetype', 'image/openraster')
        for i in reversed(range(COUNT)):
            group=ET.SubElement(stack, 'stack', name=f'Frame {i+1:02} — 41.667 ms',
                                visibility='visible' if i==29 else 'hidden')
            for name, image in [('ivory crest',crest_layers[i]),('amber field',body_layers[i])]:
                path=f'data/{i:02}-{name}.png'; data=io.BytesIO();image.save(data, format='PNG')
                z.writestr(path,data.getvalue())
                ET.SubElement(group, 'layer', name=name, src=path, opacity='1.0', x='0', y='0',
                              visibility='visible', **{'composite-op':'svg:src-over'})
        z.writestr('stack.xml', ET.tostring(doc))
        data=io.BytesIO();frames[29].save(data,format='PNG');z.writestr('mergedimage.png',data.getvalue())
    board=Image.new('RGB',(W*4,84*9),(35,39,46)); draw=ImageDraw.Draw(board)
    playback=[]
    for i,frame in enumerate(frames):
        stage=Image.new('RGBA',(W,H),(35,39,46,255));stage.alpha_composite(frame)
        playback.append(stage.convert('RGB').resize((W*3,H*3),Image.Resampling.NEAREST))
        xx,yy=(i%4)*W,(i//4)*84
        board.paste(stage.convert('RGB'),(xx,yy+20));draw.text((xx+4,yy+3),f'{i+1:02} / {i/FPS:.2f}s',fill='white')
    board.save(OUT/'contact-sheet.png')
    # GIF has no loop extension: this really plays once and stays fully covered.
    playback[0].save(OUT/'one-shot.gif',save_all=True,append_images=playback[1:],
                     duration=[40]*(COUNT-1)+[1600],optimize=False)
    print('Authored one-shot:36 frames; full coverage by frame30; emissive alpha material.')


if __name__ == '__main__':
    main()
