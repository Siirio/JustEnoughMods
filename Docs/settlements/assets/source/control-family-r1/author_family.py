"""Author the single R1 native-pixel candidate. No game resources are modified."""
from pathlib import Path
from PIL import Image,ImageDraw
import io,zipfile,xml.etree.ElementTree as ET,json
ROOT=Path(__file__).resolve().parent
W=112;H=112
P={'outline':'#292824','deep':'#47483f','shadow':'#55534c','stone':'#807d70','light':'#b6b09d','paper':'#c9b88d','paper_light':'#e6d7ae','gold':'#b89b4e','gold_light':'#e0c677'}
layers={n:Image.new('RGBA',(W,H)) for n in ['01_surfaces','02_edge_undercuts','03_bevel_planes','04_fixed_corners','05_state_accents']}
regions={}
def pixel(layer,x,y,color):layers[layer].putpixel((x,y),Image.new('RGBA',(1,1),color).getpixel((0,0)))
def line(layer,box,color):ImageDraw.Draw(layers[layer]).rectangle(box,fill=color)
def control(name,x,y,w,h,kind='button',state='default'):
 regions[name]={'rect':[x,y,w,h],'slice':[3,3,3,3]}
 surface=P['paper'] if kind=='panel' else {'default':'#807d70','hover':'#96917e','pressed':'#716c60','disabled':'#66675e','selected':'#807d70'}[state]
 # Planes are deliberately quiet: edge clusters carry material, never tiled random noise.
 line('01_surfaces',(x+2,y+2,x+w-3,y+h-3),surface)
 for yy in range(1,h-1):
  pixel('02_edge_undercuts',x,y+yy,P['outline']);pixel('02_edge_undercuts',x+w-1,y+yy,P['outline'])
 line('02_edge_undercuts',(x+1,y,x+w-2,y),P['outline'])
 line('02_edge_undercuts',(x+1,y+h-1,x+w-2,y+h-1),P['outline'])
 # Surface above edge layer; retain fixed 3px rim, center fills through surface layer.
 
 hi=P['light'] if state!='disabled' else '#85867a'
 low=P['shadow'] if state!='disabled' else '#55564e'
 if state=='hover':hi=P['paper_light']
 if state=='pressed':hi,low=low,hi
 line('03_bevel_planes',(x+2,y+1,x+w-3,y+1),hi)
 line('03_bevel_planes',(x+1,y+2,x+1,y+h-3),hi)
 line('03_bevel_planes',(x+2,y+h-2,x+w-3,y+h-2),low)
 line('03_bevel_planes',(x+w-2,y+2,x+w-2,y+h-3),low)
 if kind=='panel':
  line('03_bevel_planes',(x+3,y+2,x+w-4,y+2),'#a8a18c')
  line('03_bevel_planes',(x+2,y+3,x+2,y+h-4),'#a8a18c')
  line('03_bevel_planes',(x+3,y+h-3,x+w-4,y+h-3),'#7a715e')
  line('03_bevel_planes',(x+w-3,y+3,x+w-3,y+h-4),'#7a715e')
 # Hand-placed clipped stone shoulders. Exterior corners remain transparent.
 for dx,dy,c in [(1,1,hi),(w-2,1,P['shadow']),(1,h-2,P['shadow']),(w-2,h-2,low),(2,2,P['stone']),(w-3,2,P['light']),(2,h-3,P['light']),(w-3,h-3,P['shadow'])]:pixel('04_fixed_corners',x+dx,y+dy,c)
 # Two quiet edge chips are held in the fixed end caps, so resizing cannot repeat them.
 pixel('04_fixed_corners',x+1,y+4,P['stone'] if state!='disabled' else '#707166')
 pixel('04_fixed_corners',x+w-2,y+h-5,P['deep'])
 if state=='hover':
  line('05_state_accents',(x+3,y+2,x+w-4,y+2),'#c7bea1')
 if state=='selected':
  line('05_state_accents',(x+1,y+3,x+1,y+h-4),P['gold'])
  pixel('05_state_accents',x+2,y+2,P['gold_light']);pixel('05_state_accents',x+2,y+h-3,P['gold'])
control('settlement_panel',0,0,16,16,'panel')
for x,state in zip([24,44,64,84],['default','hover','pressed','disabled']):control('settlement_button'+('' if state=='default' else '_'+state),x,0,16,20,state=state)
for i,state in enumerate(['default','hover','selected','disabled']):control('tab_'+state,0,24+i*20,16,20,kind='tab',state=state)
regions['settlement_tabs']={'rect':[0,24,16,80],'slice':[3,3,3,3],'cells':['default','hover','selected','disabled']}
regions['status_track']={'rect':[24,28,16,8],'slice':[2,2,2,2]}
line('01_surfaces',(25,29,38,34),'#47483f');line('02_edge_undercuts',(25,28,38,28),P['outline']);line('02_edge_undercuts',(24,29,24,34),P['outline']);line('02_edge_undercuts',(39,29,39,34),P['outline']);line('02_edge_undercuts',(25,35,38,35),P['outline']);line('03_bevel_planes',(25,29,38,29),'#66675b');line('03_bevel_planes',(25,34,38,34),'#b6b09d');pixel('04_fixed_corners',25,34,'#807d70');pixel('04_fixed_corners',38,29,'#292824')
regions['status_fill']={'rect':[44,28,1,4],'slice':[0,0,0,0]};line('01_surfaces',(44,28,44,31),'#ffffff')
def png(im):b=io.BytesIO();im.save(b,format='PNG');return b.getvalue()
merged=Image.new('RGBA',(W,H))
for im in layers.values():merged.alpha_composite(im)
image=ET.Element('image',{'w':str(W),'h':str(H),'name':'Settlements core controls R1 CANDIDATE_NOT_APPROVED','version':'0.0.3'})
stack=ET.SubElement(image,'stack')
for i,(name,im) in enumerate(reversed(list(layers.items()))):ET.SubElement(stack,'layer',{'name':name,'src':f'data/layer{i:02d}.png','opacity':'1.0','visibility':'visible','composite-op':'svg:src-over','x':'0','y':'0'})
with zipfile.ZipFile(ROOT/'control-family-r1.ora','w') as z:
 z.writestr('mimetype','image/openraster',compress_type=zipfile.ZIP_STORED)
 z.writestr('stack.xml',ET.tostring(image,encoding='utf-8',xml_declaration=True))
 z.writestr('mergedimage.png',png(merged))
 z.writestr('Thumbnails/thumbnail.png',png(merged))
 for i,(name,im) in enumerate(reversed(list(layers.items()))):z.writestr(f'data/layer{i:02d}.png',png(im))
(ROOT/'regions.json').write_text(json.dumps(regions,indent=2)+'\n',encoding='utf-8')
print('Authored layered ORA: '+str(ROOT/'control-family-r1.ora'))
