"""Compose review aids from genuine native captures; never repaint model pixels."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import json

root=Path(__file__).resolve().parent
font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',26)
small=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',19)
title=ImageFont.truetype('C:/Windows/Fonts/arialbd.ttf',36)
bg=(114,123,112)
races=[('nether','NETHER / Infernal eruption'),('ancestors','ANCESTORS / Guardian ward'),('void','VOID / Split rift'),('sculk','SCULK / Vein emergence'),('bloom','BLOOM / Verdant growth')]
def flat(p,size=None):
    im=Image.open(p).convert('RGBA')
    out=Image.new('RGBA',im.size,(*bg,255));out.alpha_composite(im);out=out.convert('RGB')
    return out.resize(size,Image.Resampling.NEAREST) if size else out
board=Image.new('RGB',(1800,1400),(29,34,37));draw=ImageDraw.Draw(board)
draw.text((30,20),'R5 / M-C construction + T-A painting',font=title,fill='white')
draw.text((30,67),'Actual Blockbench captures / editable cuboids and native UV materials / PRE-INSTALL CANDIDATES',font=small,fill='#b9c8c2')
poses=Image.new('RGB',(1500,5*300+60),(29,34,37));pd=ImageDraw.Draw(poses);pd.text((15,12),'Native clip poses: 0.0 / 0.4 / 0.8 / 1.4 / 2.2 seconds',font=font,fill='white')
summary={}
for i,(race,label) in enumerate(races):
    folder=root/race
    x=(i%3)*600;y=115+(i//3)*630
    board.paste(flat(folder/'hero.png',(580,580)),(x+10,y))
    draw.text((x+16,y+587),label,font=font,fill='white')
    frames=[flat(folder/'Frames'/f'{n:03d}.png') for n in range(23)]
    frames[0].save(folder/'preview.gif',save_all=True,append_images=frames[1:],duration=[100]*22+[800],loop=0,disposal=2,optimize=False)
    for col,n in enumerate([0,4,8,14,22]):poses.paste(flat(folder/'Frames'/f'{n:03d}.png',(300,300)),(col*300,60+i*300))
    model=json.loads((folder/'signature.bbmodel').read_text(encoding='utf-8'))
    summary[race]={'cubes_excluding_stage':len(model['elements'])-1,'source':race+'/signature.bbmodel','texture':race+'/materials.png','layered_source':race+'/materials.ora','preview':race+'/preview.gif','native_captures':23,'approval':'PENDING','runtime':'NOT_INSTALLED'}
draw.text((1230,850),'Selected rules:',font=title,fill='white')
for j,line in enumerate(['Angular rotated cuboids','Unequal fitted layers','Shared grounded masses','Purposeful material roles','Light tops / dark bases','E excluded except hat / moss']):draw.text((1230,910+j*42),line,font=font,fill='#c9d8cc')
board.save(root/'R5-comparison.jpg',quality=95,subsampling=0)
poses.save(root/'R5-key-poses.jpg',quality=95,subsampling=0)
(root/'review.json').write_text(json.dumps(summary,indent=2)+'\n',encoding='utf-8')
print('Composed five GIFs, native capture comparison and key-pose sheet; no object pixels repainted.')
