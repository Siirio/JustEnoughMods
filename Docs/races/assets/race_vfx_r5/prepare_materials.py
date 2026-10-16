"""Mechanical native-pixel/UV preparation of an original AI-painted source.

No random marks or procedural material invention. Source sheet and layered ORA
remain editable; named face roles determine value treatment and UV routing.
"""
from pathlib import Path
from PIL import Image
from zipfile import ZipFile, ZIP_STORED
from io import BytesIO
import json, hashlib, shutil

ROOT=Path(__file__).resolve().parent
SOURCE=ROOT/'Painting'/'material-paint-source.png'
ORIGINAL=Path('C:/Users/user/.codex/generated_images/01a145f0-2da1-7cb3-a920-2a0ae5fdea9f/exec-b1b00bc5-0421-44bc-b6d4-cdd209891ab9.png')
SOURCE.parent.mkdir(exist_ok=True)
if not SOURCE.exists(): shutil.copyfile(ORIGINAL,SOURCE)
source=Image.open(SOURCE).convert('RGB')
races=['nether','ancestors','void','sculk','bloom']
roles={
 'nether':['basalt crust','cooled iron-russet plate','molten cut surface'],
 'ancestors':['warm pale guardian stone','oxidized bronze joint','amber inlay'],
 'void':['obsidian crust','violet fractured crystal','luminous inner crystal'],
 'sculk':['ink-teal growth','ivory horn','cyan colony'],
 'bloom':['bark','leaf and moss','pink blossom']}
def png(im):
    b=BytesIO();im.save(b,format='PNG');return b.getvalue()
def value(im,factor):
    return im.point([min(255,round(i*factor)) for i in range(256)]*3)
for row,race in enumerate(races):
    folder=ROOT/race;folder.mkdir(exist_ok=True)
    base=Image.new('RGBA',(48,48))
    direction=Image.new('RGBA',(48,48))
    emission=Image.new('RGBA',(48,48),(0,0,0,255))
    for col in range(3):
        crop=source.crop((round(col*source.width/3),round(row*source.height/5),round((col+1)*source.width/3),round((row+1)*source.height/5)))
        # Each painted swatch is reduced to the intended native 16x16 material;
        # limited palette removes generated subpixel/AA color drift.
        tile=crop.resize((16,16),Image.Resampling.NEAREST).quantize(colors=7,method=Image.Quantize.MEDIANCUT,dither=Image.Dither.NONE).convert('RGB')
        for face_row,factor in enumerate([1.12,0.88,0.52]):
            base.paste(tile,(col*16,face_row*16))
            treatment=value(tile,factor)
            direction.paste(treatment,(col*16,face_row*16))
            if col==2 and race in ('nether','sculk','void') and face_row==0:
                # Reserved emission mask; no runtime integration is claimed.
                for y in range(16):
                    for x in range(16):
                        rgb=treatment.getpixel((x,y))
                        if max(rgb)>170:emission.putpixel((col*16+x,y),(*rgb,255))
    composite=Image.alpha_composite(base,direction)
    composite.save(folder/'materials.png')
    emission.save(folder/'emission-proposal.png')
    composite.resize((576,576),Image.Resampling.NEAREST).save(folder/'materials-nearest.png')
    with ZipFile(folder/'materials.ora','w',ZIP_STORED) as z:
        z.writestr('mimetype','image/openraster')
        z.writestr('stack.xml','<image version="0.0.3" w="48" h="48" name="'+race+' materials"><stack><layer name="Directional face values - top side underside" src="data/directional.png" opacity="1.0" visibility="visible" composite-op="svg:src-over"/><layer name="Original painted material clusters" src="data/base.png" opacity="1.0" visibility="visible" composite-op="svg:src-over"/></stack></image>')
        z.writestr('data/base.png',png(base));z.writestr('data/directional.png',png(direction));z.writestr('mergedimage.png',png(composite));z.writestr('Thumbnails/thumbnail.png',png(composite.resize((256,256),Image.Resampling.NEAREST)))
    (folder/'materials.json').write_text(json.dumps({'race':race,'columns':roles[race],'rows':['upper cut/light plane','side plane','underside/root shade'],'tile':16,'atlas':[48,48],'density':'1 texel/model unit wherever integer face size permits;16 model units/block','source_sha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),'pipeline':'original imagegen flat painted sheet -> nearest native reduction ->7-color material palette -> explicit face-value layers -> native BB import','emission':'proposal mask only; native preview has no bloom or dynamic lighting'},indent=2)+'\n',encoding='utf-8')
print('Prepared five native48x48 atlases and editable ORA sources from original painted swatches.')
