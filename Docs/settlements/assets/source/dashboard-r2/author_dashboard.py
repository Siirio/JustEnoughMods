"""Original native pixel dashboard art. Reopen the ORA stack to produce runtime PNGs.

No reference pixels are imported. This is an art author/export operation, not a test.
"""
from pathlib import Path
from PIL import Image, ImageDraw
import io, json, zipfile, xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
OUT = ROOT.parents[4] / 'Mods/JEMSettlements/src/main/resources/assets/jem_settlements/textures/gui/dashboard'
W, H = 256, 128
layers = {n: Image.new('RGBA', (W, H)) for n in ['01_material_planes', '02_recess_and_edges', '03_corner_craft', '04_cloth_and_iron', '05_leaves_and_light']}
regions = {}

def box(n, xy, c): ImageDraw.Draw(layers[n]).rectangle(xy, fill=c)
def poly(n, points, c): ImageDraw.Draw(layers[n]).polygon(points, fill=c)
def px(n, x, y, c): box(n, (x, y, x, y), c)

def frame(name, x, y, fill, rim, light, low):
    regions[name] = [x, y, 24, 24]
    box('01_material_planes', (x+1,y+1,x+22,y+22), '#241f20')
    box('01_material_planes', (x+3,y+3,x+20,y+20), fill)
    box('02_recess_and_edges', (x+2,y+1,x+21,y+1), light)
    box('02_recess_and_edges', (x+1,y+2,x+1,y+21), rim)
    box('02_recess_and_edges', (x+2,y+22,x+21,y+22), low)
    box('02_recess_and_edges', (x+22,y+2,x+22,y+21), low)
    box('02_recess_and_edges', (x+3,y+2,x+20,y+2), rim)
    box('02_recess_and_edges', (x+2,y+3,x+2,y+20), light)
    box('02_recess_and_edges', (x+3,y+21,x+20,y+21), rim)
    box('02_recess_and_edges', (x+21,y+3,x+21,y+20), rim)
    # Broad bevel planes keep the physical rim legible at actual GUI density.
    box('02_recess_and_edges', (x+4,y+3,x+19,y+3), light)
    box('02_recess_and_edges', (x+3,y+4,x+3,y+19), rim)
    box('02_recess_and_edges', (x+4,y+20,x+19,y+20), low)
    box('02_recess_and_edges', (x+20,y+4,x+20,y+19), low)
    for dx,dy,sx,sy in [(1,1,1,1),(22,1,-1,1),(1,22,1,-1),(22,22,-1,-1)]:
        px('03_corner_craft', x+dx,y+dy,rim)
        box('03_corner_craft',(min(x+dx+sx,x+dx+3*sx),min(y+dy+sy,y+dy+3*sy),max(x+dx+sx,x+dx+3*sx),max(y+dy+sy,y+dy+3*sy)),rim)
        px('03_corner_craft',x+dx+sx,y+dy+sy,light)
        px('03_corner_craft',x+dx+2*sx,y+dy+2*sy,low)
        px('03_corner_craft',x+dx+3*sx,y+dy+2*sy,light)
        px('03_corner_craft',x+dx+2*sx,y+dy+3*sy,rim)

frame('stone_frame',0,0,'#30323d','#686b76','#999ba2','#22232b')
frame('paper_frame',24,0,'#ead9b8','#b29672','#fff0d1','#796650')
frame('dark_frame',48,0,'#30323d','#775739','#b08b5a','#3c2b22')
frame('wood_frame',72,0,'#765234','#916b43','#c79c63','#38291f')
frame('control',96,0,'#50525e','#787c89','#a5a8b1','#272933')
frame('control_hover',120,0,'#656979','#b1a16b','#e9d79f','#34333c')
frame('control_selected',144,0,'#236fae','#c8a55b','#fff1a0','#113955')
frame('control_disabled',168,0,'#41434b','#62636b','#797b82','#2a2b31')
frame('card',192,0,'#f0e3c9','#c1a987','#fff1d7','#9e8668')
frame('track',216,0,'#44464d','#777b82','#a1a4a7','#24252b')

# Seam-free quiet tiles. Material marks are sparse low-contrast clusters, not random noise.
for name,x,y,color in [('paper',0,32,'#ead9b8'),('stone',32,32,'#30323d'),('wood',64,32,'#765234'),('blue',96,32,'#236fae')]:
    regions[name]=[x,y,32,32];box('01_material_planes',(x,y,x+31,y+31),color)
box('02_recess_and_edges',(3,37,10,37),'#e7d5b4');box('02_recess_and_edges',(22,53,28,53),'#edddbe')
box('02_recess_and_edges',(7,58,9,59),'#e7d5b4');box('02_recess_and_edges',(25,40,27,40),'#edddbe')
for x,y in [(34,36),(50,51),(37,60)]:
    box('02_recess_and_edges',(x,y,x+10,y+5),'#343641');box('02_recess_and_edges',(x,y+6,x+8,y+6),'#2b2d37')
for yy in [34,45,55,62]:box('02_recess_and_edges',(64,yy,95,yy),'#68472e')
for xx,yy,ww in [(68,37,12),(83,48,8),(66,58,18)]:box('02_recess_and_edges',(xx,yy,xx+ww,yy),'#89613d')
box('02_recess_and_edges',(98,36,110,36),'#2877b7');box('02_recess_and_edges',(116,55,125,55),'#216aa6')

# Blue fabric pennant: fixed 16x32, asymmetric folds and a woven cream chevron.
regions['banner']=[136,32,16,32]
poly('04_cloth_and_iron',[(137,32),(150,32),(150,58),(144,63),(137,58)],'#1d2939')
poly('04_cloth_and_iron',[(139,34),(148,34),(148,57),(144,61),(139,57)],'#245c91')
box('04_cloth_and_iron',(140,34,142,56),'#377eb4');box('04_cloth_and_iron',(147,34,148,56),'#173f6d')
poly('04_cloth_and_iron',[(139,43),(144,48),(148,43),(148,49),(144,54),(139,49)],'#d8d9c7')
box('04_cloth_and_iron',(136,32,151,33),'#c8a55b');px('04_cloth_and_iron',137,32,'#f1da96')

# Hanging lantern, dark iron cage and stepped warm glass, no soft glow baked in.
regions['lantern']=[160,32,16,28]
box('04_cloth_and_iron',(167,32,168,37),'#292c32');box('04_cloth_and_iron',(166,32,166,35),'#7b7e80')
box('04_cloth_and_iron',(164,37,171,39),'#25282e');box('04_cloth_and_iron',(162,40,173,54),'#20242b')
box('04_cloth_and_iron',(163,41,172,53),'#a86323');box('05_leaves_and_light',(164,43,171,51),'#ffc34b')
box('05_leaves_and_light',(165,44,166,50),'#fff4ba');box('05_leaves_and_light',(169,44,170,50),'#ffe384')
box('04_cloth_and_iron',(167,42,168,52),'#82603c');box('04_cloth_and_iron',(162,40,173,41),'#64717a')
box('04_cloth_and_iron',(163,53,172,55),'#414c58');box('04_cloth_and_iron',(164,56,171,56),'#242a33')
px('04_cloth_and_iron',163,40,'#a3a6a1');box('04_cloth_and_iron',(163,42,163,50),'#5d6568')

# Broad voxel leaf clusters on a bent vine; independent silhouette from frame.
regions['vine']=[184,32,40,32]
poly('05_leaves_and_light',[(184,35),(189,35),(194,42),(204,45),(211,54),(214,54),(214,57),(209,57),(202,48),(192,46)],'#334825')
def leaf(x,y,flip=False):
    shape=[(x,y+2),(x+2,y),(x+6,y),(x+8,y+3),(x+7,y+6),(x+4,y+8),(x+1,y+6)]
    poly('05_leaves_and_light',shape,'#304323')
    poly('05_leaves_and_light',[(x+1,y+2),(x+3,y+1),(x+6,y+1),(x+7,y+3),(x+5,y+6),(x+2,y+5)],'#638844')
    box('05_leaves_and_light',(x+2,y+2,x+4,y+3),'#8da852');px('05_leaves_and_light',x+6,y+4,'#486b33')
for xx,yy in [(184,32),(191,38),(199,38),(201,47),(209,49),(213,54),(189,47)]:leaf(xx,yy)

# Three original 16px face silhouettes; expressions stay distinct without relying on color.
for name,x,base,hi,shade in [('sad',0,'#e15d50','#ff9b7e','#913b35'),('neutral',24,'#edbc43','#ffe38a','#ab742c'),('happy',48,'#77bf4e','#c6ed7d','#3e7a37')]:
    y=80;regions['mood_'+name]=[x,y,16,16]
    poly('04_cloth_and_iron',[(x+5,y),(x+10,y),(x+14,y+4),(x+14,y+11),(x+10,y+15),(x+5,y+15),(x+1,y+11),(x+1,y+4)],'#302b28')
    poly('04_cloth_and_iron',[(x+5,y+1),(x+10,y+1),(x+13,y+4),(x+13,y+11),(x+10,y+14),(x+5,y+14),(x+2,y+11),(x+2,y+4)],shade)
    poly('04_cloth_and_iron',[(x+5,y+2),(x+10,y+2),(x+12,y+4),(x+12,y+10),(x+10,y+12),(x+5,y+12),(x+3,y+10),(x+3,y+4)],base)
    box('05_leaves_and_light',(x+5,y+2,x+9,y+2),hi);box('05_leaves_and_light',(x+3,y+4,x+3,y+6),hi)
    box('05_leaves_and_light',(x+5,y+5,x+5,y+7),'#342b26');box('05_leaves_and_light',(x+10,y+5,x+10,y+7),'#342b26')
    if name=='happy':
        box('05_leaves_and_light',(x+5,y+9,x+5,y+10),'#342b26');box('05_leaves_and_light',(x+6,y+11,x+9,y+11),'#342b26');box('05_leaves_and_light',(x+10,y+9,x+10,y+10),'#342b26')
    elif name=='neutral':box('05_leaves_and_light',(x+5,y+10,x+10,y+10),'#342b26')
    else:
        box('05_leaves_and_light',(x+6,y+9,x+9,y+9),'#342b26');px('05_leaves_and_light',x+5,y+10,'#342b26');px('05_leaves_and_light',x+10,y+10,'#342b26')

def png(im):
    b=io.BytesIO();im.save(b,format='PNG');return b.getvalue()

merged=Image.new('RGBA',(W,H))
for im in layers.values():merged.alpha_composite(im)
xml=ET.Element('image',{'w':str(W),'h':str(H),'version':'0.0.3','name':'Settlements dashboard R2 editable materials and ornaments'})
stack=ET.SubElement(xml,'stack')
ordered=list(reversed(list(layers.items())))
for i,(name,im) in enumerate(ordered):ET.SubElement(stack,'layer',{'name':name,'src':f'data/layer{i}.png','opacity':'1.0','visibility':'visible','composite-op':'svg:src-over','x':'0','y':'0'})
with zipfile.ZipFile(ROOT/'dashboard-r2.ora','w') as z:
    z.writestr('mimetype','image/openraster',compress_type=zipfile.ZIP_STORED)
    z.writestr('stack.xml',ET.tostring(xml,encoding='utf-8',xml_declaration=True))
    z.writestr('mergedimage.png',png(merged));z.writestr('Thumbnails/thumbnail.png',png(merged))
    for i,(name,im) in enumerate(ordered):z.writestr(f'data/layer{i}.png',png(im))

# Export from the saved editable source, not from an independent render approximation.
with zipfile.ZipFile(ROOT/'dashboard-r2.ora') as z:
    doc=ET.fromstring(z.read('stack.xml'));export=Image.new('RGBA',(W,H))
    for node in reversed(list(doc.find('stack'))):export.alpha_composite(Image.open(io.BytesIO(z.read(node.attrib['src']))).convert('RGBA'))
OUT.mkdir(parents=True,exist_ok=True)
for name,(x,y,w,h) in regions.items():export.crop((x,y,x+w,y+h)).save(OUT/(name+'.png'))
export.save(ROOT/'atlas-native1x.png');export.resize((W*4,H*4),Image.Resampling.NEAREST).save(ROOT/'atlas-nearest4x.png')
(ROOT/'regions.json').write_text(json.dumps(regions,indent=2)+'\n',encoding='utf-8')
print(f'Layered ORA reopened; exported {len(regions)} native RGBA textures to {OUT}')
