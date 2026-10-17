"""Authored external pixel cels: rift aperture and sculk propagation.

Rift interior is a mask for the actual vanilla End portal render type at runtime.
The preview uses a labelled neutral interior, never fabricated game footage.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageChops, ImageFilter
import importlib.util, json, hashlib

ROOT=Path(__file__).resolve().parent
PROJECT=ROOT.parents[3]
OUT=PROJECT/'Mods/JEMVisualSandboxR3/src/main/resources/assets/jem_visual_sandbox/elemental'
spec=importlib.util.spec_from_file_location('pixel_source',ROOT.parent/'fiery-slash/author.py')
common=importlib.util.module_from_spec(spec);spec.loader.exec_module(common)
common.NAMES=['outer fringe','body','core','highlights','fragments']

def blank():return Image.new('RGBA',(96,96))
def polygon(im,points,color):ImageDraw.Draw(im).polygon(points,fill=color)
def line(im,points,color,width):ImageDraw.Draw(im).line(points,fill=color,width=width)

def rift(f):
    layers=[blank() for _ in range(5)];mask=blank()
    # Thin at the two tips, broad at the belly. Diagonal hand-drawn aperture.
    contour=[(13,88),(23,65),(31,51),(43,36),(61,23),(84,8),(74,29),(68,47),(55,62),(38,74)]
    amount=0 if f<3 else min(1,(f-2)/8) if f<11 else 1 if f<14 else max(0,(23-f)/9)
    if amount:
        # Edges peel in opposite directions, rather than scaling the whole cut.
        points=[]
        for x,y in contour:
            center=(96-y)*.84+5
            points.append((round(center+(x-center)*amount),y))
        polygon(mask,points,'white')
        # Deliberately asymmetric cold rim and drifting knots, no round portal frame.
        for i,(color,width) in enumerate([('#321c50',5),('#7644ac',3),('#c697ef',1)]):
            line(layers[i],points+[points[0]],color,width)
        for j,(x,y) in enumerate([(24,66),(38,48),(61,33),(70,22)]):
            age=(f+j*2)%8;xx=x+(-1 if j%2 else 1)*age;yy=y-age
            polygon(layers[4],[(xx,yy+2),(xx+1,yy-2),(xx+3,yy-3),(xx+2,yy+1)],'#b77ede')
    elif f<3:
        line(layers[3],[(17,84),(48,47),(81,12)],'#9779bd',1)
    return layers,mask

def sculk(f):
    layers=[blank() for _ in range(5)]
    if f>=23:return layers
    # One connected, unequal branching membrane. Travel progresses along trunk.
    paths=[[(47,88),(43,69),(50,52),(46,34),(52,12)],[(47,67),(29,58),(21,42),(12,37)],[(48,53),(68,44),(76,25),(85,20)],[(46,36),(31,27),(25,13)]]
    for i,path in enumerate(paths):
        phase=f-i*2
        if phase<0:continue
        count=min(len(path),2+phase//2)
        active=path[:count]
        if f>16:active=active[min(len(active)-1,(f-16)//2):]
        line(layers[0],active,'#081b24',17 if i==0 else 13)
        line(layers[1],active,'#133b43',13 if i==0 else 9)
        line(layers[2],active,'#246567',5)
        # Broken painted veins on broad tissue; no continuous cyan pipe outline.
        for j,(x,y) in enumerate(active):
            if (f+j+i)%5<3:polygon(layers[3],[(x-2,y+1),(x-3,y-2),(x,y-3),(x+2,y),(x+1,y+2)],'#68c8c0')
        x,y=active[-1];polygon(layers[4],[(x-4,y+1),(x-3,y-2),(x+2,y-3),(x+3,y),(x,y+3)],'#69d5cc')
    # Tissue edge lobes attached to the substrate, not raised pipes.
    for i,(x,y) in enumerate([(43,67),(50,52),(30,58),(68,44)]):
        if 5+i<f<19+i:polygon(layers[1],[(x-5,y+2),(x-7,y-3),(x-2,y-5),(x+1,y-2),(x+5,y-3),(x+4,y+3)],'#15474c')
    return layers

def main():
    ROOT.mkdir(exist_ok=True);OUT.mkdir(parents=True,exist_ok=True)
    rows=[];board=Image.new('RGB',(8*192,2*220),(22,27,36));draw=ImageDraw.Draw(board)
    for row,(name,count,fn) in enumerate([('rift',24,rift),('sculk',24,sculk)]):
        cels=[];frames=[];masks=[]
        for f in range(count):
            value=fn(f)
            if name=='rift':layers,mask=value;masks.append(mask)
            else:layers=value
            cels.append(layers);frame=common.composite(layers)
            if name=='rift':
                interior=Image.new('RGBA',(96,96),'#070b18');interior.putalpha(mask.getchannel('A'));interior.alpha_composite(frame);frame=interior
            frames.append(frame)
        common.aseprite(ROOT/(name+'.aseprite'),cels)
        strip=Image.new('RGBA',(96,count*96))
        for f,layers in enumerate(cels):strip.paste(common.composite(layers),(0,f*96))
        strip.save(OUT/(name+'.png'));strip.save(ROOT/(name+'-strip.png'))
        if masks:
            atlas=Image.new('RGBA',(96,count*96))
            for f,mask in enumerate(masks):atlas.paste(mask,(0,f*96))
            atlas.save(OUT/'rift-mask.png');atlas.save(ROOT/'rift-mask.png')
        playback=[]
        for frame in frames+[blank()]*10:
            panel=Image.new('RGB',(480,420),(22,27,36));panel.paste(frame.resize((384,384),Image.Resampling.NEAREST),(48,12),frame.resize((384,384),Image.Resampling.NEAREST));ImageDraw.Draw(panel).text((12,398),name+(' / APERTURE ONLY; NATIVE SHADER IN GAME' if name=='rift' else ' / EXPORTED PIXEL CELS'),fill='white');playback.append(panel)
        playback[0].save(ROOT/(name+'.gif'),save_all=True,append_images=playback[1:],duration=50,loop=0)
        for col in range(8):
            f=round(col*(count-1)/7);im=frames[f].resize((192,192),Image.Resampling.NEAREST);board.paste(im,(col*192,row*220+20),im);draw.text((col*192+4,row*220+3),name+' '+str(f),fill='white')
        rows.append({'id':name,'frames':count,'frameTicks':1,'dimensions':[96,96],'source':name+'.aseprite','sha256':hashlib.sha256((OUT/(name+'.png')).read_bytes()).hexdigest()})
    board.save(ROOT/'contact.png');(ROOT/'assets.json').write_text(json.dumps(rows,indent=2)+'\n',encoding='utf-8')
    print('Authored rift and sculk timelines; Bloom uses the separate adaptive Blockbench wrap.')

if __name__=='__main__':main()
