"""Continue the user's two cels without changing their endpoint pixels or geometry."""
from pathlib import Path
import base64, io, json, shutil, zipfile
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt
import xml.etree.ElementTree as ET

ROOT = Path(__file__).parent / 'guardian-smooth'
ROOT.mkdir(exist_ok=True)
source = Path('C:/Users/user/AppData/Local/Temp/guardian-user-before.bbmodel')
shutil.copy2(source, ROOT/'original-user-frames.bbmodel')
model = json.loads(source.read_text(encoding='utf-8'))
texture = model['textures'][0]
sheet = Image.open(io.BytesIO(base64.b64decode(texture['source'].split(',',1)[1]))).convert('RGBA')
sheet.save(ROOT/'original-user-strip.png')
a,b = [np.array(sheet.crop((0,i*64,256,(i+1)*64))) for i in range(2)]
palette = [(255,223,21,255),(255,238,130,255),(255,241,241,255),(255,255,255,255)]

def sdf(mask):
    # Horizontal wrap makes interpolation continuous at the UV seam.
    tiled = np.tile(mask,(1,3))
    if not tiled.any(): return np.full(mask.shape,-80.)
    return (distance_transform_edt(tiled)-distance_transform_edt(~tiled))[:,256:512]

# Nested material regions let bright cores shrink coherently instead of
# cross-fading whole pictures or introducing interpolated palette colors.
masks=[]
for cel in [a,b]:
    indices=np.zeros((64,256),dtype=np.uint8)
    for rank,color in enumerate(palette,1):
        indices[np.all(cel==color,axis=2)]=rank
    masks.append([sdf(indices>=rank) for rank in range(1,5)])

def between(t):
    if t==0:return Image.fromarray(a)
    if t==1:return Image.fromarray(b)
    out=np.zeros_like(a)
    for rank,color in enumerate(palette):
        field=(1-t)*masks[0][rank]+t*masks[1][rank]
        out[field>0]=color
    return Image.fromarray(out)

frames=[]
for i in range(24):
    phase=i/12 if i<=12 else (24-i)/12
    t=phase*phase*(3-2*phase)
    frames.append(between(t))
atlas=Image.new('RGBA',(256,64*len(frames)))
for i,f in enumerate(frames):atlas.paste(f,(0,i*64))
atlas.save(ROOT/'gold-wave-smooth.png')
buf=io.BytesIO();atlas.save(buf,format='PNG')
texture.update(source='data:image/png;base64,'+base64.b64encode(buf.getvalue()).decode(),
               name='gold-wave-smooth',path='',width=256,height=1536,uv_width=256,uv_height=64,
               fps=20,frame_interpolate=False,internal=True,saved=False)
model['name']='guardian-smooth'
(ROOT/'guardian-smooth.bbmodel').write_text(json.dumps(model,indent=2),encoding='utf-8')
# Editable ORA: one native-size cel layer per timeline frame, plus explicit timing.
doc=ET.Element('image',w='256',h='64',name='Guardian wave — 24 cels at 20 FPS')
stack=ET.SubElement(doc,'stack')
with zipfile.ZipFile(ROOT/'gold-wave-cels.ora','w') as z:
    z.writestr('mimetype','image/openraster')
    for i,f in reversed(list(enumerate(frames))):
        path=f'data/frame-{i:02}.png';data=io.BytesIO();f.save(data,format='PNG');z.writestr(path,data.getvalue())
        ET.SubElement(stack,'layer',name=f'Frame {i+1:02} — 50 ms',src=path,opacity='1.0',visibility='visible' if i==0 else 'hidden',x='0',y='0',**{'composite-op':'svg:src-over'})
    z.writestr('stack.xml',ET.tostring(doc));data=io.BytesIO();frames[0].save(data,format='PNG');z.writestr('mergedimage.png',data.getvalue())
previews=[]
board=Image.new('RGB',(4*256,6*84),(35,39,46));draw=ImageDraw.Draw(board)
for i,f in enumerate(frames):
    stage=Image.new('RGBA',f.size,(35,39,46,255));stage.alpha_composite(f)
    previews.append(stage.convert('RGB').resize((768,192),Image.Resampling.NEAREST))
    x,y=(i%4)*256,(i//4)*84;board.paste(stage.convert('RGB'),(x,y+20));draw.text((x+4,y+3),f'{i+1:02}'+(' USER 1' if i==0 else ' USER 2' if i==12 else ''),fill='white')
board.save(ROOT/'contact-sheet.png')
previews[0].save(ROOT/'preview.gif',save_all=True,append_images=previews[1:],duration=50,loop=0,optimize=False)
(ROOT/'README.txt').write_text('24 frames, 20 FPS, 1.2-second eased forward/reverse loop. Frame size 256x64. Original user cels are preserved exactly at timeline frames 1 and 13. Open guardian-smooth.bbmodel, Paint mode, and press texture Play. ORA contains individually editable cel layers; export uses a vertical PNG strip. Intermediates use pixel silhouette/material-region interpolation with horizontal UV wrap, no blur or blended colors. Geometry and UVs unchanged. Original project/strip are backed up alongside. This is an editor candidate; custom JEM renderer does not yet play this strip. No runtime installation.\n',encoding='utf-8')
print(ROOT)
