"""Export the authored OpenRaster stack and compose clearly labelled offline review sheets."""
from pathlib import Path
from PIL import Image,ImageDraw
import argparse,zipfile,xml.etree.ElementTree as ET,io,json,hashlib
ROOT=Path(__file__).resolve().parent
REVIEW=ROOT.parents[1]/'review'/'control-family-r1'
def export(source):
 with zipfile.ZipFile(source) as z:
  root=ET.fromstring(z.read('stack.xml'));out=Image.new('RGBA',(int(root.attrib['w']),int(root.attrib['h'])))
  stack=root.find('stack')
  for node in reversed(list(stack)):
   if node.attrib.get('visibility','visible')!='visible':continue
   im=Image.open(io.BytesIO(z.read(node.attrib['src']))).convert('RGBA')
   opacity=float(node.attrib.get('opacity','1'))
   if opacity!=1:im.putalpha(im.getchannel('A').point(lambda a:round(a*opacity)))
   out.alpha_composite(im,(int(node.attrib.get('x',0)),int(node.attrib.get('y',0))))
 return out

def tiled_resize(src,size,slice_size=3):
 w,h=size;sw,sh=src.size;s=slice_size;out=Image.new('RGBA',size)
 # Fixed corners and exact integer nearest repeat for every middle pixel.
 for yy in range(h):
  sy=yy if yy<s else sh-(h-yy) if yy>=h-s else s+(yy-s)%(sh-2*s)
  for xx in range(w):
   sx=xx if xx<s else sw-(w-xx) if xx>=w-s else s+(xx-s)%(sw-2*s)
   out.putpixel((xx,yy),src.getpixel((sx,sy)))
 return out

class NativeText:
 def __init__(self,jar):
  self.glyphs={}
  with zipfile.ZipFile(jar) as z:
   providers=json.loads(z.read('assets/minecraft/font/include/default.json'))['providers']
   for p in providers:
    if p['type']!='bitmap':continue
    ns,path=p['file'].split(':');img=Image.open(io.BytesIO(z.read('assets/'+ns+'/textures/'+path))).convert('RGBA');rows=p['chars'];cw=img.width//len(rows[0]);ch=img.height//len(rows)
    for y,row in enumerate(rows):
     for x,c in enumerate(row):
      if c in ('\x00',' '):continue
      glyph=img.crop((x*cw,y*ch,(x+1)*cw,(y+1)*ch));bbox=glyph.getchannel('A').getbbox()
      if bbox:
       glyph=glyph.crop((0,0,bbox[2],ch));self.glyphs[c]=glyph
 def width(self,text):return sum((self.glyphs[c].width+1 if c in self.glyphs else 4) for c in text)
 def draw(self,target,text,x,y,color='#352c20',center=False,shadow=False):
  if center:x-=self.width(text)//2
  for c in text:
   glyph=self.glyphs.get(c)
   if glyph is None:x+=4;continue
   if shadow:
    shade=Image.new('RGBA',glyph.size,'#39392f');shade.putalpha(glyph.getchannel('A'));target.alpha_composite(shade,(x+1,y+1))
   colored=Image.new('RGBA',glyph.size,color);colored.putalpha(glyph.getchannel('A'));target.alpha_composite(colored,(x,y));x+=glyph.width+1

def review_sheet(sprites,text,background):
 # Offline control placement only, with no world/financial/npc sample state.
 out=Image.new('RGBA',(384,272),background)
 out.alpha_composite(tiled_resize(sprites['settlement_panel'],(352,240)),(16,16))
 text.draw(out,'CANDIDATE / ART ONLY',32,27)
 text.draw(out,'Кнопки / Controls',32,43)
 for i,state in enumerate(['default','hover','selected','disabled']):
  out.alpha_composite(tiled_resize(sprites['settlement_tabs'].crop((0,i*20,16,(i+1)*20)),(88,20)),(24,64+i*24))
  text.draw(out,['Карта','Жители','Казна','Настройки'][i],68,70+i*24,color='#f1ead6',center=True,shadow=True)
 for i,(name,label) in enumerate([('settlement_button','Подтвердить расширение'),('settlement_button_hover','Подтвердить расширение'),('settlement_button_pressed','Подтвердить расширение'),('settlement_button_disabled','Подтвердить расширение')]):
  out.alpha_composite(tiled_resize(sprites[name],(224,20)),(128,64+i*24))
  text.draw(out,label,240,70+i*24+(1 if i==2 else 0),color='#e9e4d4' if i==3 else '#fff5d8',center=True,shadow=True)
 # Separate labelled bars; fill fractions are art swatches, not gameplay claims.
 for y,label,color,fill in [(177,'Счастье / Happiness','#6e984f',.75),(205,'Сытость / Satiety','#c47b36',.25)]:
  text.draw(out,label,128,y)
  track=tiled_resize(sprites['status_track'],(224,8),2);out.alpha_composite(track,(128,y+11))
  mask=Image.new('RGBA',(int(220*fill),4),color);mask.putalpha(sprites['status_fill'].resize(mask.size,Image.Resampling.NEAREST).getchannel('A'));out.alpha_composite(mask,(130,y+13))
 ImageDraw.Draw(out).rectangle((126,62,353,85),outline='#e0c677',width=1)
 text.draw(out,'Default+focus / Hover / Pressed / Disabled',32,237)
 text.draw(out,'OFFLINE SHEET - NO GAME CAPTURE',16,260,color='#ffffff' if background=='#252820' else '#252820')
 return out

def main():
 parser=argparse.ArgumentParser();parser.add_argument('--review-font-jar',required=True);args=parser.parse_args()
 atlas=export(ROOT/'control-family-r1.ora');regions=json.loads((ROOT/'regions.json').read_text(encoding='utf-8'));dest=ROOT/'exports';dest.mkdir(exist_ok=True);REVIEW.mkdir(parents=True,exist_ok=True)
 sprites={}
 for name,region in regions.items():
  if name.startswith('tab_'):continue
  x,y,w,h=region['rect'];im=atlas.crop((x,y,x+w,y+h));im.save(dest/(name+'.png'));sprites[name]=im
 atlas.save(REVIEW/'atlas-native1x.png');atlas.resize((448,448),Image.Resampling.NEAREST).save(REVIEW/'atlas-nearest4x.png')
 native=NativeText(args.review_font_jar)
 for name,bg in [('dark','#252820'),('light','#a6bd88')]:
  sheet=review_sheet(sprites,native,bg);sheet.save(REVIEW/(name+'-native1x.png'));sheet.resize((1536,1088),Image.Resampling.NEAREST).save(REVIEW/(name+'-nearest4x.png'))
 manifest={'candidate':'settlements-control-family-r1','status':'CANDIDATE_NOT_APPROVED','evidenceState':'SOURCE_EDITABLE','runtimeGate':'WAITING_FOR_REAL_GAME_CAPTURE','integrated':False,'source':'control-family-r1.ora','canvas':[112,112],'layers':['01_surfaces','02_edge_undercuts','03_bevel_planes','04_fixed_corners','05_state_accents'],'sprites':{},'exportProcedure':'python export_family.py --review-font-jar <Minecraft 1.20.1 client.jar>','fontUse':'Installed vanilla bitmap font rendered only into labelled review sheets; font atlas not retained or exported. Sprites contain no text.','provenance':'Original hand-specified pixel clusters by Codex for this candidate; no reference pixels cropped/copied. Existing user-labelled references inspected as material/construction guidance.','license':'New original candidate files belong to this project; no third-party asset license is asserted. Vanilla fonts remain Mojang property and appear only as rendered review labels.','alpha':'Sprite pixels are binary 0/255; transparent outer corners, no antialiasing.','scaling':'3px fixed corners and tiled center/edges; track2px fixed end caps; fill mask1x4 tinted by renderer.','focus':'Focus outline is programmatic, not included in sprite. Text and pressed1px offset remain runtime-owned.'}
 for name,im in sprites.items():
  path=dest/(name+'.png');manifest['sprites'][name]={'path':'exports/'+path.name,'size':list(im.size),'sourceRect':regions[name]['rect'],'slice':regions[name]['slice'],'sha256':hashlib.sha256(path.read_bytes()).hexdigest()}
 manifest['sprites']['settlement_tabs']['cells']=[{'name':n,'rect':[0,i*20,16,20]} for i,n in enumerate(['default','hover','selected','disabled'])]
 (ROOT/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
 import platform,PIL
 evidence={'operation':'ORA stack compositing and sprite export, not a test or gameplay capture','python':platform.python_version(),'pillow':PIL.__version__,'sourceSha256':hashlib.sha256((ROOT/'control-family-r1.ora').read_bytes()).hexdigest(),'spritesExported':len(sprites),'reviewSheets':['dark-native1x.png','dark-nearest4x.png','light-native1x.png','light-nearest4x.png'],'sourceFormat':'OpenRaster 0.0.3','sourceReopenedBy':'Python zipfile + ElementTree + Pillow native PNG decoder','limitations':'No desktop editor opening or game renderer capture was performed.'}
 (ROOT/'export-evidence.json').write_text(json.dumps(evidence,indent=2)+'\n',encoding='utf-8')
 print('Exported8 sprites from layered ORA; native1x and nearest4x light/dark sheets written. No runtime integration.')
if __name__=='__main__':main()
