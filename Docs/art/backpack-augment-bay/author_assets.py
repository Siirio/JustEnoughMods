from pathlib import Path
from PIL import Image, ImageDraw
from io import BytesIO
from zipfile import ZipFile, ZIP_STORED
from xml.etree.ElementTree import Element, SubElement, tostring
import json
ROOT=Path(__file__).resolve().parents[3]
ART=Path(__file__).resolve().parent
OUT=ROOT/'Mods/JEMCompat/src/main/resources/assets/jemcompat/textures'
P={'ink':'#382e25','edge':'#70573e','wood':'#906d48','grain':'#a78256','light':'#c3a078','cream':'#eedbc0','paper':'#e5cfaf','rim':'#fff0d6','shade':'#bba381','recess':'#80725d','shadow':'#514b3e','gold':'#c99a42','shine':'#f2d47d','iron':'#b4b7ac','steel':'#69746c','teal':'#5b9180','moss':'#385b51'}
def layer(w,h): return Image.new('RGBA',(w,h),(0,0,0,0))
def rectangle(im,box,color): ImageDraw.Draw(im).rectangle(box,fill=P.get(color,color))
def export(name,layers,folder):
 w,h=layers[0][1].size
 merged=layer(w,h)
 for _,im in layers: merged.alpha_composite(im)
 dest=OUT/folder/(name+'.png'); dest.parent.mkdir(parents=True,exist_ok=True); merged.save(dest)
 doc=Element('image',{'version':'0.0.3','w':str(w),'h':str(h),'name':name}); stack=SubElement(doc,'stack')
 with ZipFile(ART/(name+'.ora'),'w') as archive:
  archive.writestr('mimetype','image/openraster',compress_type=ZIP_STORED)
  for i,(title,im) in reversed(list(enumerate(layers))):
   path=f'data/layer{i}.png'; stream=BytesIO(); im.save(stream,format='PNG'); archive.writestr(path,stream.getvalue())
   SubElement(stack,'layer',{'name':title,'src':path,'opacity':'1.0','visibility':'visible','composite-op':'svg:src-over','x':'0','y':'0'})
  archive.writestr('stack.xml',tostring(doc,encoding='utf-8',xml_declaration=True))
  stream=BytesIO(); merged.save(stream,format='PNG'); archive.writestr('mergedimage.png',stream.getvalue())
 return merged

def panel(w,h):
 im=layer(w,h)
 rectangle(im,(1,0,w-2,h-1),'ink'); rectangle(im,(0,1,w-1,h-2),'ink')
 rectangle(im,(1,1,w-2,h-2),'edge'); rectangle(im,(2,2,w-3,h-3),'light')
 rectangle(im,(3,3,w-4,h-4),'cream'); rectangle(im,(3,h-5,w-4,h-4),'shade')
 rectangle(im,(4,3,w-5,3),'rim'); rectangle(im,(w-4,4,w-4,h-5),'shade')
 return im

def slot(im,x,y,result=False):
 rectangle(im,(x,y,x+17,y+17),'shadow'); rectangle(im,(x+1,y+1,x+16,y+16),'recess')
 rectangle(im,(x+1,y+1,x+16,y+1),'edge'); rectangle(im,(x+1,y+1,x+1,y+16),'edge')
 rectangle(im,(x,y+17,x+17,y+17),'rim'); rectangle(im,(x+17,y,x+17,y+17),'rim')
 if result:
  rectangle(im,(x-2,y-2,x+19,y-2),'gold'); rectangle(im,(x-2,y-2,x-2,y+19),'gold')
  rectangle(im,(x-1,y+19,x+19,y+19),'edge'); rectangle(im,(x+19,y-1,x+19,y+18),'edge')

base=panel(176,220); slots=layer(176,220); marks=layer(176,220)
# Three real work positions: backpack clamp, payment, optional workstation component.
for x in [34,79,124]: slot(slots,x,38)
rectangle(marks,(31,36,31,58),'gold');rectangle(marks,(32,35,54,35),'gold');rectangle(marks,(55,36,55,58),'edge')
rectangle(marks,(34,60,52,61),'edge');rectangle(marks,(80,60,96,61),'shade');rectangle(marks,(125,60,141,61),'shade')
# Intentional leather hinge strips at the seam, rather than noise across the content area.
for x in [10,158]:
 rectangle(marks,(x,115,x+7,124),'wood'); rectangle(marks,(x+1,115,x+2,122),'grain');rectangle(marks,(x+4,118,x+5,119),'gold')
rectangle(marks,(22,119,153,119),'shade');rectangle(marks,(22,120,153,120),'rim')
for row in range(3):
 for col in range(9): slot(slots,7+col*18,134+row*18)
for col in range(9): slot(slots,7+col*18,192)
export('augment_bay',[('Parchment and leather frame',base),('Recessed native-size slots',slots),('Backpack clamp and hinge hardware',marks)],'gui')
base=panel(176,76); slots=layer(176,76); marks=layer(176,76)
for row in range(3):
 for col in range(3): slot(slots,29+col*18,15+row*18)
slot(slots,123,33,True)
# Compact hand-stepped fabrication arrow, 1px edges, no glow.
rectangle(marks,(94,39,107,42),'edge');rectangle(marks,(105,35,107,46),'edge');rectangle(marks,(108,37,109,44),'edge');rectangle(marks,(110,39,111,42),'edge')
rectangle(marks,(95,39,106,39),'light'); rectangle(marks,(105,36,106,37),'light')
export('backpack_workbench',[('Matching parchment housing',base),('Nine ingredients and one framed result',slots),('Fabrication direction',marks)],'gui')
# Hand-authored 12px native augment icon. A strapped three-by-three cutting mat.
icon=['............','..EEEEEEEE..','.ELLLLLLLLE.','.ELSSSSSSLE.','.ELSTSTSTLE.','.ELSSSSSSLE.','.ELSTSTSTLE.','.ELSSSSSSLE.','.ELSTSTSTLE.','.ELSSSSSSLE.','..EEEEEEEE..','............']
colors={'.':None,'E':'ink','L':'gold','S':'moss','T':'teal'}
im=layer(12,12)
for y,row in enumerate(icon):
 for x,c in enumerate(row):
  if colors[c]: im.putpixel((x,y),Image.new('RGBA',(1,1),P[colors[c]]).getpixel((0,0)))
export('workbench_augment',[('Cutting mat and brass clamp',im)],'gui')
# Original block faces, each natively16x16, limited shared oak/brass/green palette.
faces={
'augment_bay_top':[
'EEEEEEEEEEEEEEEE','ELLLLLLLLLLLLLLE','ELWWWWWWWWWWWWLE','ELWEEEEEEEEEEWLE','ELWESSSSSSSSEWLE','ELWESTSTSTSSEWLE','ELWESSSSSSSSEWLE','ELWESTSTSTSSEWLE','ELWESSSSSSSSEWLE','ELWESTSTSTSSEWLE','ELWESSSSSSSSEWLE','ELWEEEEEEEEEEWLE','ELWWWWWWWWWWWWLE','ELLLLGGGGGGLLLLE','EEEEEGHIHGEEEEEE','EEEEEGGGGGEEEEEE'],
'augment_bay_front':[
'EEEEEEEEEEEEEEEE','ELLLLLLLLLLLLLLE','EWWWWWWWWWWWWWWE','EWGGGGGGGGGGGGWE','EWGEEEEEEEEEEGWE','EWGEWWWWWWWWEGWE','EWGEWWGHHGWWEGWE','EWGEWWGHHGWWEGWE','EWGEWWWWWWWWEGWE','EWGEEEEEEEEEEGWE','EWGGGGGGGGGGGGWE','EWWWWWWWWWWWWWWE','ELLLEEEEEEEELLLE','EWWE........EWWE','EWWE........EWWE','EEEE........EEEE'],
'augment_bay_side':[
'EEEEEEEEEEEEEEEE','ELLLLLLLLLLLLLLE','EWWWWWWWWWWWWWWE','EWGGGGGGGGGGGGWE','EWGWWWWWWWWWWGWE','EWGWWWWWWWWWWGWE','EWGWWGGGGGGWWGWE','EWGWWGWWWWGWWGWE','EWGWWGGGGGGWWGWE','EWGWWWWWWWWWWGWE','EWGGGGGGGGGGGGWE','EWWWWWWWWWWWWWWE','ELLLEEEEEEEELLLE','EWWE........EWWE','EWWE........EWWE','EEEE........EEEE']}
color={'.':'ink','E':'ink','L':'light','W':'wood','G':'edge','S':'moss','T':'teal','H':'gold','I':'shine'}
for name,rows in faces.items():
 im=layer(16,16)
 for y,row in enumerate(rows):
  for x,c in enumerate(row): im.putpixel((x,y),Image.new('RGBA',(1,1),P[color[c]]).getpixel((0,0)))
 export(name,[('Hand-painted native16px face',im)],'block')
# Contact sheet is an inspection aid, never a shipped texture.
sheet=layer(704,300)
for path,box,scale in [('augment_bay',(0,0),1),('backpack_workbench',(190,0),1),('workbench_augment',(390,0),8),('augment_bay_top',(500,0),8),('augment_bay_front',(500,140),8)]:
 folder='gui' if path in ['augment_bay','backpack_workbench','workbench_augment'] else 'block'
 im=Image.open(OUT/folder/(path+'.png')).convert('RGBA')
 if scale!=1: im=im.resize((im.width*scale,im.height*scale),Image.Resampling.NEAREST)
 sheet.alpha_composite(im,box)
sheet.save(ART/'contact-sheet.png')
