"""Compose native captures only; no image-generated animation or repainted render."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import json
r=Path(__file__).resolve().parent
font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',24)
small=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',18)
title=ImageFont.truetype('C:/Windows/Fonts/arialbd.ttf',34)
bg=(114,123,112)
names=[('nether','INFERNAL / 3 successive strikes'),('ancestors','ANCESTORS / Interlocking shield fan'),('void','VOID / Compressed split seam'),('sculk','SCULK / Branching ground crawl'),('bloom','BLOOM / Inward root bower')]
def flat(p,size=None):
 im=Image.open(p).convert('RGBA');out=Image.new('RGBA',im.size,(*bg,255));out.alpha_composite(im);out=out.convert('RGB')
 return out.resize(size,Image.Resampling.NEAREST) if size else out
board=Image.new('RGB',(1800,1420),(29,34,37));d=ImageDraw.Draw(board)
d.text((25,16),'R6 / Different spatial patterns and motion per ability',font=title,fill='white')
d.text((25,62),'Actual native Blockbench models, UV materials and clips / BEFORE INSTALLATION',font=small,fill='#c7d4cb')
summary={}
for i,(race,label)in enumerate(names):
 folder=r/race;x=(i%3)*600;y=105+(i//3)*630
 board.paste(flat(folder/'hero.png',(580,580)),(x+10,y));d.text((x+13,y+587),label,font=font,fill='white')
 frames=[flat(folder/'Frames'/f'{j:03d}.png')for j in range(45)]
 frames[0].save(folder/'preview.gif',save_all=True,append_images=frames[1:],duration=[50]*44+[800],loop=0,disposal=2,optimize=False)
 source=json.loads((folder/'signature.bbmodel').read_text(encoding='utf-8'))
 summary[race]={'source':race+'/signature.bbmodel','cubes':len(source['elements'])-1,'frames':45,'fps':20,'approval':'PENDING','runtime':'NOT_INSTALLED','pattern':label.split('/ ',1)[1]}
d.text((1220,850),'Infernal only:',font=title,fill='white')
d.text((1220,900),'1 impact -> 2 -> 3',font=font,fill='#ffda9a')
for col,j in enumerate([5,11,18]):
 board.paste(flat(r/'nether/Frames'/f'{j:03d}.png',(180,180)),(1220+col*190,950));d.text((1240+col*190,1138),f'{j/20:.2f}s',font=small,fill='white')
d.text((1220,1200),'Warm/light around all FOUR side faces',font=small,fill='#c7d4cb')
d.text((1220,1232),'Progressively dark toward the base',font=small,fill='#c7d4cb')
board.save(r/'R6-patterns.jpg',quality=95,subsampling=0)
thermal=Image.new('RGB',(1440,800),(29,34,37));td=ImageDraw.Draw(thermal)
td.text((22,12),'Infernal exterior: upper perimeter light on every side -> dark base',font=font,fill='white')
thermal.paste(flat(r/'nether/hero.png'),(0,70));thermal.paste(flat(r/'nether/back.png'),(720,70));thermal.save(r/'Infernal-perimeter.jpg',quality=95,subsampling=0)
seq=Image.new('RGB',(1800,410),(29,34,37));sd=ImageDraw.Draw(seq)
for col,j in enumerate([0,5,11,18,28,44]):
 seq.paste(flat(r/'nether/Frames'/f'{j:03d}.png',(300,300)),(col*300,70));sd.text((col*300+15,20),f'{j/20:.2f}s',font=font,fill='white')
seq.save(r/'Infernal-sequence.jpg',quality=95,subsampling=0)
(r/'review.json').write_text(json.dumps(summary,indent=2)+'\n',encoding='utf-8')
print('Composed five native20Hz GIFs, pattern board, perimeter and 1-2-3 evidence.')
