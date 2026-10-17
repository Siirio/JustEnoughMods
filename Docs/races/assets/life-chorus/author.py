"""External pixel cels: editable Aseprite layers, exported without image smoothing."""
from pathlib import Path
import importlib.util, math, json, hashlib
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parent
REPO=ROOT.parents[3]
OUT=REPO/'Mods/JEMVisualSandboxR3/src/main/resources/assets/jem_visual_sandbox/life_chorus'
spec=importlib.util.spec_from_file_location('cel_source',ROOT.parent/'fiery-slash/author.py')
common=importlib.util.module_from_spec(spec);spec.loader.exec_module(common)
common.NAMES=['root and wave shadows','leaf and wave bodies','material color','veins and lit lips','warm gold responses']
LEAF=[(0,10),(-5,5),(-7,-1),(-4,-8),(2,-13),(6,-8),(8,-2),(5,5)]

def leaf(layers,x,y,s=1,turn=0,gold=False):
    def pt(p):
        a,b=p;return (round(x+s*(a*math.cos(turn)-b*math.sin(turn))),round(y+s*(a*math.sin(turn)+b*math.cos(turn))))
    colors=('#735925','#bf983a','#f4d584') if gold else ('#233e28','#4f813d','#a4c463')
    ImageDraw.Draw(layers[1]).polygon([pt(p) for p in LEAF],fill=colors[0])
    ImageDraw.Draw(layers[2]).polygon([pt(p) for p in [(0,8),(-3,1),(-3,-5),(2,-11),(4,-6),(4,1)]],fill=colors[1])
    ImageDraw.Draw(layers[3]).line([pt(p) for p in [(0,8),(0,0),(2,-10)]],fill=colors[2],width=max(1,round(s)))

def cel(name,f):
    layers=[Image.new('RGBA',(96,96)) for _ in range(5)]
    d=[ImageDraw.Draw(l) for l in layers]
    if f==23:return layers
    if name=='awakening':
        # Four unlike branching roots pull inward; no continuous orange rods.
        for i,points in enumerate([[(3,67),(17,60),(22,49),(36,48),(44,44)],[(74,91),(65,78),(67,64),(55,54)],[(92,21),(77,24),(72,36),(57,40)],[(24,4),(30,18),(29,29),(39,37)]]):
            count=max(2,len(points)-f//2);path=points[-count:];d[0].line(path,fill='#392d23',width=5);d[1].line(path,fill='#596a31',width=3);d[3].line(path[-2:],fill='#a5bd68',width=1)
            x,y=path[0];leaf(layers,x,y,.5,(i-1)*.8)
        for i,(x,y) in enumerate([(48,16),(71,25),(81,48),(70,72),(48,81),(24,71),(15,48),(25,25)]):
            if f>=i: d[4].polygon([(x,y-2),(x+2,y),(x,y+2),(x-2,y)],fill='#f1d688')
    elif name in ('life_band','sonic_band'):
        sonic=name=='sonic_band'
        dark,mid,light=('#103741','#347a88','#b4e8df') if sonic else ('#355e38','#83b657','#d1e9a1')
        # Tile spans the wave tangent. Unequal chipped lips, broad lobes and gaps
        # are painted into the material; renderer supplies propagation only.
        paths=[[(0,54),(12,49),(19,51),(24,41),(33,40),(39,43),(45,38),(54,42),(61,40),(70,49),(79,47),(87,52),(96,54)],[(0,67),(10,62),(18,65),(25,57),(34,60),(40,52),(49,55),(55,53),(64,61),(74,58),(82,65),(96,67)]]
        drift=[0,-1,-2,-1,1,2,1,0][f%8]
        if not sonic:
            # Revision 2: replace the weak pair of lines with connected leaf
            # masses. Unequal shoulders make the bright lip a material edge.
            body=[(0,54),(12,49),(17,51),(22,38),(31,32),(39,43),(45,37),(53,42),(60,32),(68,40),(70,49),(80,46),(88,51),(96,54),(96,66),(83,64),(77,69),(68,61),(60,64),(52,56),(44,59),(35,64),(27,59),(19,68),(10,62),(0,67)]
            body=[(x,y+drift) for x,y in body];d[0].polygon(body,fill='#24442c')
            d[1].polygon([(x,y-2) for x,y in body],fill='#649847')
        for i,path in enumerate(paths):
            path=[(x,y+drift*(1 if i==0 else -1)) for x,y in path]
            d[0].line(path,fill=dark,width=10-i*5);d[2].line(path,fill=mid,width=6-i*3)
            d[3].line(path[2:6],fill=light,width=2)
        if sonic:
            for x,y in [(14,31),(49,21),(76,32)]:
                d[1].polygon([(x-8,y+10),(x-7,y+1),(x,y-5),(x+9,y),(x+6,y+9),(x,y+13)],fill=dark)
                d[3].line([(x-5,y+6),(x-3,y),(x+3,y-2),(x+6,y+2)],fill=light,width=2)
        else:
            for i,(x,y) in enumerate([(22,39),(48,38),(65,40)]):leaf(layers,x,y,.65,(-1+i)*.7)
            for x,y in [(31,61),(66,57),(89,69)]:d[4].rectangle((x,y,x+1,y+1),fill='#ead195')
    elif name=='answer':
        # Independent rising leaf poses and bent stems, followed by three crowns.
        rise=min(1,f/15)
        for i,(x,y,t) in enumerate([(26,81,-.8),(66,70,.6),(40,51,-.4),(67,38,.9)]):
            yy=y-round(25*rise);xx=x+round(4*math.sin((f+i*4)*.32))
            if f<18:
                d[0].line([(xx-4,yy+16),(xx-7,yy+8),(xx,yy)],fill='#2f5332',width=2)
                leaf(layers,xx,yy,.55+rise*.08,t,gold=i==1)
        if f>=11:
            for i,(x,y,t) in enumerate([(30,18,-.55),(48,12,0),(66,18,.55)]):
                if f<22-i:leaf(layers,x,y-round((f-11)*.4),.48,t,gold=i==1)
        for i,(x,y) in enumerate([(19,68),(73,55),(51,41),(37,29)]):
            yy=y-f;d[4].polygon([(x,yy-2),(x+1,yy),(x,yy+2),(x-1,yy)],fill='#eed69c')
    elif name=='crumble':
        for i,(x,y) in enumerate([(27,51),(46,39),(65,54),(48,66)]):
            leaf(layers,x+(i-2)*f//3,y-f,.65*(1-f/26),i*.9,gold=True)
    elif name=='landing':
        for x,y,sx,sy in [(24,24,1,1),(72,24,-1,1),(24,72,1,-1),(72,72,-1,-1)]:
            d[1].line([(x+sx*9,y),(x,y),(x,y+sy*7)],fill='#674294',width=4)
            d[3].line([(x+sx*7,y),(x,y),(x,y+sy*5)],fill='#c69aef',width=2)
        d[4].rectangle((47,47,49,49),fill='#dfc3ff')
    else:raise ValueError(name)
    return layers

def main():
    OUT.mkdir(parents=True,exist_ok=True);records=[]
    names=['awakening','life_band','answer','crumble','sonic_band','landing']
    board=Image.new('RGB',(768,len(names)*148),'#141d22');draw=ImageDraw.Draw(board)
    for row,name in enumerate(names):
        cels=[cel(name,f) for f in range(24)];common.aseprite(ROOT/(name+'.aseprite'),cels)
        strip=Image.new('RGBA',(96,96*24))
        for f,ls in enumerate(cels):strip.paste(common.composite(ls),(0,f*96))
        strip.save(OUT/(name+'.png'))
        for col,f in enumerate([0,4,9,14,18,21]):
            im=common.composite(cels[f]).resize((128,128),Image.Resampling.NEAREST);board.paste(im,(col*128,row*148+18),im)
            draw.text((col*128+2,row*148+2),name+' '+str(f),fill='white')
        records.append(dict(id=name,frames=24,source=name+'.aseprite',sha256=hashlib.sha256((OUT/(name+'.png')).read_bytes()).hexdigest()))
    board.save(ROOT/'contact.png');(ROOT/'assets.json').write_text(json.dumps(records,indent=2)+'\n',encoding='utf-8')

if __name__=='__main__':main()
