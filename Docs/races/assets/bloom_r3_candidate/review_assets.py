"""Requested asset inspection and presentation assembly, not a gameplay test suite."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageChops, ImageStat
import base64
import hashlib
import json
import math

ROOT = Path(__file__).resolve().parent
source = json.loads((ROOT / 'verdant_grasp_r3.bbmodel').read_text(encoding='utf-8'))
audit = {'state': 'SOURCE_EDITABLE_CANDIDATE', 'game_installed': False, 'materials': {}, 'warnings': []}
for name in ['bark', 'foliage', 'bud', 'stage']:
    path = ROOT / f'{name}.png'
    im = Image.open(path).convert('RGBA')
    colors = im.getcolors(1024)
    singleton = 0
    for y in range(im.height):
        for x in range(im.width):
            color = im.getpixel((x,y))
            neighbours = [(x+dx,y+dy) for dx,dy in [(1,0),(-1,0),(0,1),(0,-1)] if 0 <= x+dx < im.width and 0 <= y+dy < im.height]
            if all(im.getpixel(p) != color for p in neighbours):
                singleton += 1
    embedded = next(t for t in source['textures'] if t['name'] == f'r3_{name}')
    embedded_bytes = base64.b64decode(embedded['source'].split(',',1)[1])
    assert im.size == (16,16), f'{name}: wrong native dimensions'
    assert len(colors) <= 4, f'{name}: uncontrolled palette'
    assert im.getextrema()[3] == (255,255), f'{name}: unintended material transparency'
    assert path.read_bytes() == embedded_bytes, f'{name}: exported material differs from editable source'
    audit['materials'][name] = {'size': [16,16], 'colors': len(colors), 'alpha': 'opaque by design', 'isolated_color_pixels': singleton, 'embedded_export_match': True}
    if singleton: audit['warnings'].append(f'{name}: {singleton} isolated color pixels need artistic review, not automatic deletion')

clip = source['animations'][0]
assert math.isclose(clip['length'], 2.2)
element_ids = [e['uuid'] for e in source['elements']]
assert len(set(element_ids)) == len(element_ids)
assert all(0 <= k['time'] <= 2.2 for a in clip['animators'].values() for k in a.get('keyframes',[]))
audit['animation'] = {'duration_seconds': 2.2, 'fps': 20, 'native_frames': 45, 'unique_elements': len(element_ids), 'stable_rig': True, 'loop_mode': clip['loop'], 'review': 'once-only action; replay pause is presentation only'}

def native(path, size=None):
    im = Image.open(path).convert('RGBA')
    bg = Image.new('RGBA', im.size, (182,190,180,255))
    bg.alpha_composite(im)
    return bg.convert('RGB').resize(size,Image.Resampling.NEAREST) if size else bg.convert('RGB')

paths = sorted((ROOT/'frames').glob('*.png'))
assert len(paths) == 45
assert all(Image.open(p).size == (600,600) for p in paths)
frames = [native(p) for p in paths]
changes = [sum(ImageStat.Stat(ImageChops.difference(a,b)).mean)/3 for a,b in zip(frames,frames[1:])]
audit['animation']['max_adjacent_mean_rgb_change'] = round(max(changes),3)
audit['animation']['peak_change_tick'] = changes.index(max(changes))+1
audit['animation']['start_end_pixels_identical'] = ImageChops.difference(frames[0],frames[-1]).getbbox() is None
if max(changes) > 12: audit['warnings'].append('Large adjacent frame change: inspect the growth/retraction interval')
frames[0].save(ROOT/'verdant-grasp-r3.gif',save_all=True,append_images=frames[1:],duration=[50]*44+[650],loop=0,optimize=True)

font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',18)
sheet=Image.new('RGB',(1200,670),'#e3e5df');draw=ImageDraw.Draw(sheet)
for i,index in enumerate([0,4,8,13,18,30,36,44]):
    x=(i%4)*300;y=(i//4)*335
    sheet.paste(frames[index].resize((300,300),Image.Resampling.NEAREST),(x,y+30))
    draw.text((x+12,y+3),f'{index/20:.2f}s / tick {index}',font=font,fill='#26332a')
sheet.save(ROOT/'keyposes-contact-sheet.png')
views=Image.new('RGB',(1200,640),'#e3e5df');draw=ImageDraw.Draw(views)
for i,name in enumerate(['hero','front','side','back']):
    x=(i%2)*600;y=(i//2)*320
    views.paste(native(ROOT/f'view-{name}.png',(300,300)),(x+150,y+20))
    draw.text((x+12,y+4),name,font=font,fill='#26332a')
views.save(ROOT/'angles-contact-sheet.png')
materials=Image.new('RGB',(640,210),'#e3e5df');draw=ImageDraw.Draw(materials)
for i,name in enumerate(['bark','foliage','bud','stage']):
    im=Image.open(ROOT/f'{name}.png').convert('RGB')
    materials.paste(im,(i*160+8,25));materials.paste(im.resize((144,144),Image.Resampling.NEAREST),(i*160+8,50))
    draw.text((i*160+8,2),name+' 1x / 9x',font=font,fill='#26332a')
materials.save(ROOT/'materials-nearest.png')
native(ROOT/'view-hero.png').save(ROOT/'hero.png')
audit['source_sha256']=hashlib.sha256((ROOT/'verdant_grasp_r3.bbmodel').read_bytes()).hexdigest()
audit['limitations']=['Not Minecraft footage or user approval','Preview stage is excluded from any future runtime asset','Pinned plugin offscreen disposal reports onContextMenu undefined; source/export/captures completed before that optional cleanup failure']
(ROOT/'asset-audit.json').write_text(json.dumps(audit,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(audit,ensure_ascii=False,indent=2))
