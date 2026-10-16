"""Revalue the original painted basalt/crust along the pillar's whole height.

No new material marks are generated. A native-pixel value ramp preserves
the painted clusters; UVs below route global lower/upper pillar regions into
the corresponding part of this ramp rather than resetting at each cuboid.
"""
from pathlib import Path
from PIL import Image
from zipfile import ZipFile,ZIP_STORED
from io import BytesIO
import json
r=Path(__file__).resolve().parent
source=Image.open(r.parent/'race_vfx_r5/nether/materials.png').convert('RGBA')
base=source.copy();layer=Image.new('RGBA',(48,48));out=base.copy()
# Keep the already painted magma clusters, but remove pale cream from the
# molten cap as well as the full exterior. Hottest colors stay saturated.
for y in range(16):
 for x in range(32,48):
  red,green,blue,alpha=out.getpixel((x,y))
  if red>150:
   color=(red,min(green,190),min(blue,32),alpha)
   out.putpixel((x,y),color);layer.putpixel((x,y),color)
for col in range(3):
 tile=source.crop((col*16,16,col*16+16,32))
 for y in range(16):
  ramp=y/15*4;band=min(3,int(ramp));blend=ramp-band
  lerp=lambda values:values[band]*(1-blend)+values[band+1]*blend
  factor=lerp([2.25,1.75,1.25,.78,.38]) if col<2 else lerp([1.1,1.0,.9,.72,.5])
  warmth=(lerp([24,13,4,0,0]),lerp([15,7,1,0,0]),lerp([2,1,0,0,0]))
  for x in range(16):
   rgb=tile.getpixel((x,y))[:3]
   color=tuple(min(255,round(v*factor+warmth[c])) for c,v in enumerate(rgb))
   if col<2:
    # Heat wraps all FOUR exterior sides, not only the top-facing cap.
    # The same painted cluster value selects an intentional coarse heat ramp.
    value=max(rgb)
    heat=(min(255,195+value*.9-y/15*80),max(8,min(190,35+value*1.6-y/15*60)),max(2,min(26,3+value*.18)))
    mix=lerp([.98,.90,.72,.28,0])
    color=tuple(round(color[c]*(1-mix)+heat[c]*mix) for c in range(3))
   color=tuple(color)+(255,)
   out.putpixel((col*16+x,16+y),color);layer.putpixel((col*16+x,16+y),color)
out.save(r/'nether/materials.png')
out.resize((576,576),Image.Resampling.NEAREST).save(r/'nether/materials-nearest.png')
def png(im):
 b=BytesIO();im.save(b,'PNG');return b.getvalue()
with ZipFile(r/'nether/materials.ora','w',ZIP_STORED)as z:
 z.writestr('mimetype','image/openraster')
 z.writestr('stack.xml','<image version="0.0.3" w="48" h="48"><stack><layer name="Whole-pillar perimeter thermal gradient at native pixel rows" src="data/thermal.png" opacity="1" visibility="visible" composite-op="svg:src-over"/><layer name="Original painted material clusters and face values" src="data/base.png" opacity="1" visibility="visible" composite-op="svg:src-over"/></stack></image>')
 z.writestr('data/base.png',png(base));z.writestr('data/thermal.png',png(layer));z.writestr('mergedimage.png',png(out))
p=r/'nether/materials.json';d=json.loads(p.read_text(encoding='utf-8'));d['height_gradient']='ALL FOUR exterior side faces: saturated magma orange/yellow-orange upper perimeter, orange-red and ember middle, dark charcoal base; NO cream/beige; side v16..32 follows WHOLE pillar y/h, not a cap or per-cuboid reset';p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
print('Nether: exterior crust now uses a whole-height native-pixel thermal gradient.')
