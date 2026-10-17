"""External layered pixel cels for the continuous ground cut and actual utilities."""
from pathlib import Path
from PIL import Image,ImageDraw
import importlib.util,json,hashlib
ROOT=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('elemental',ROOT/'author.py');art=importlib.util.module_from_spec(spec);spec.loader.exec_module(art)
OUT=art.OUT
def blank():return Image.new('RGBA',(96,96))
def poly(im,p,c):ImageDraw.Draw(im).polygon(p,fill=c)
def line(im,p,c,w):ImageDraw.Draw(im).line(p,fill=c,width=w)
def leaf(im,x,y,size,c):poly(im,[(x,y+size),(x-size,y),(x-2,y-size),(x+1,y-size*2),(x+size,y-1)],c)
def cel(name,f):
    layers=[blank() for _ in range(5)]
    if f==23:return layers
    opening=min(1,(f+1)/5);fade=min(1,(23-f)/6);power=opening*fade
    if name=='ground_fissure':
        # One connected unequal tear with broad dark crust and hot split lips.
        spine=[(47,96),(43,82),(50,71),(44,57),(51,44),(46,31),(50,17),(48,0)]
        for i,(color,width) in enumerate([('#211219',29),('#661c13',21),('#c23d10',13),('#ff7819',7),('#ffb638',3)]):
            line(layers[i],spine,color,max(1,round(width*power)))
        for points in [[(45,81),(32,73),(29,60)],[(49,48),(64,41),(68,28)],[(47,26),(36,19),(31,5)]]:
            line(layers[1],points,'#501713',max(1,round(7*power)));line(layers[3],points,'#ed5814',max(1,round(3*power)))
        for x,y in [(40,66),(58,37),(44,14)]:poly(layers[4],[(x,y),(x+2,y-2),(x+3,y),(x+1,y+3)],'#ffaf2b')
    elif name=='fire_wall':
        sway=[0,2,4,2,0,-2,-3,-1][f%8]
        shapes=[[(0,95),(0,63),(8,51),(12,26),(19,41),(23,17),(29,40),(35,50),(40,74),(50,95)],[(27,95),(33,64),(45,51),(49,21),(56,7),(54,36),(64,28),(62,59),(69,77),(75,95)],[(57,95),(63,78),(75,63),(77,42),(85,25),(83,55),(92,48),(95,71),(96,95)]]
        rhythm=[-3,-1,2,5,3,0,-2,-4]
        for index,points in enumerate(shapes):
            pulse=rhythm[(f+index*3)%8];lean=rhythm[(f+index*3+2)%8]
            points=[(round(x+lean*(95-y)/75),round(95+(y-95+pulse*(95-y)/60)*power)) for x,y in points];poly(layers[1],points,'#a72a10');poly(layers[2],[(round(48+(x-48)*.89),y+7) for x,y in points],'#f05b0b')
        core=[(0,95),(10,79),(16,63),(25,83),(37,86),(49,63),(54,43),(60,79),(71,89),(83,65),(91,87),(96,95)]
        poly(layers[3],[(x+round(sway*(95-y)/60),round(95+(y-95)*power)) for x,y in core],'#ff9721' if f<18 else '#d64b0b')
        line(layers[4],[(0,94),(22,91),(37,94),(54,89),(74,94),(96,91)],'#ffba3b',3)
    elif name=='void_orb':
        offset=[0,1,2,1,0,-1,-2,-1][f%8]
        contour=[(45,20),(62,23),(73,38),(70,57),(56,72),(37,67),(23,52),(26,34)]
        poly(layers[0],[(x+offset,y) for x,y in contour],'#211a38');line(layers[1],contour+[contour[0]],'#7143ac',5)
        poly(layers[2],[(28,42),(37,29),(54,27),(44,35),(39,49),(48,61),(62,59),(54,68),(37,62)],'#b880ec')
        poly(layers[3],[(32,39),(40,32),(49,31),(41,38),(39,47),(35,48)],'#ead5ff')
        for x,y in [(20,28),(70,65),(78,35)]:leaf(layers[4],x,y-f%5,2,'#9961d3')
    elif name=='gravity':
        for i,(x,y) in enumerate([(24,75),(67,71),(42,47),(77,37)]):
            lift=(f*3+i*7)%33;poly(layers[1],[(x,y-lift),(x-6,y-lift+16),(x-1,y-lift+11),(x+3,y-lift+20),(x+8,y-lift+4)],'#573d8c')
            line(layers[3],[(x-3,y-lift+10),(x,y-lift+1),(x+3,y-lift+6)],'#c8a0f0',2)
    elif name=='path_leaf':
        for i,(x,y,s) in enumerate([(32,72,10),(56,60,12),(69,77,8)]):
            size=max(1,round(s*power));paint=Image.open(ROOT/'leaf.png').convert('RGBA').resize((size*2,size*2),Image.Resampling.NEAREST);layers[2].alpha_composite(paint,(x-size,y-size*2));line(layers[1],[(48,88),(x,y)],'#406033',max(1,round(2*power)))
    elif name=='echo_scan':
        scale=.15+f*.034
        contour=[(18,53),(15,37),(26,20),(43,14),(57,17),(72,29),(78,45),(74,63),(60,77),(42,80),(25,72),(18,57)]
        points=[(round(48+(x-48)*scale),round(48+(y-48)*scale)) for x,y in contour]
        for i in [0,4,8]:
            path=points[i:i+3];line(layers[0],path,'#0e2935',9);line(layers[1],path,'#23656d',6);line(layers[3],path,'#73cad0',2)
            x,y=path[1];poly(layers[2],[(x-4,y+3),(x-6,y-1),(x-1,y-5),(x+3,y-3),(x+4,y+2)],'#3c98a2')
        for i,(x,y) in enumerate(points):
            if (i+f)%3==0:poly(layers[4],[(x-2,y+3),(x,y-4),(x+3,y-1)],'#b0e3d8')
    elif name=='echo_absorb':
        distance=max(0,34-f*1.5)
        for i,(dx,dy) in enumerate([(-1,-.4),(.8,-.7),(.2,1)]):
            x,y=round(48+dx*distance),round(48+dy*distance);poly(layers[1],[(x-8,y+2),(x-7,y-4),(x-1,y-8),(x+5,y-5),(x+4,y),(x+1,y+5)],'#154956');poly(layers[3],[(x-4,y+1),(x-2,y-4),(x+2,y-4),(x+3,y),(x,y+3)],'#7ed3d1');line(layers[2],[(x-8*dx,y-8*dy),(x-4*dx+3,y-4*dy-2),(x,y)],'#328b9b',3)
    elif name=='lava_drink':
        for i,(x,y) in enumerate([(31,75),(47,69),(65,74),(38,51)]):
            rise=(f*2+i*3)%26;poly(layers[1],[(x-3,y-rise+5),(x-4,y-rise),(x,y-rise-7),(x+4,y-rise),(x+2,y-rise+5)],'#cb3e0c');poly(layers[3],[(x-1,y-rise+2),(x,y-rise-4),(x+2,y-rise)],'#ffad28')
    else: # chorus / guardian forming: gold wing-like material, not the shield itself.
        for i,(x,y) in enumerate([(21,75),(42,82),(67,74),(79,58)]):
            lift=f+i*2;poly(layers[1],[(x,y-lift),(x-5,y-lift-13),(x-1,y-lift-24),(x+5,y-lift-16),(x+9,y-lift-3)],'#a97629');poly(layers[3],[(x,y-lift-4),(x-1,y-lift-17),(x+3,y-lift-13),(x+5,y-lift-4)],'#f2cf74')
    return layers
def main():
    names=['ground_fissure','fire_wall','void_orb','gravity','path_leaf','echo_scan','echo_absorb','lava_drink','chorus'];records=[]
    board=Image.new('RGB',(6*192,len(names)*215),(20,25,34));draw=ImageDraw.Draw(board)
    for row,name in enumerate(names):
        cels=[cel(name,f) for f in range(24)];art.common.aseprite(ROOT/(name+'.aseprite'),cels);strip=Image.new('RGBA',(96,2304));frames=[]
        for f,layers in enumerate(cels):
            im=art.common.composite(layers);strip.paste(im,(0,f*96));plate=Image.new('RGB',(384,410),(20,25,34));plate.paste(im.resize((384,384),Image.Resampling.NEAREST),(0,0),im.resize((384,384),Image.Resampling.NEAREST));ImageDraw.Draw(plate).text((8,392),name+' / ASSET CELS, NOT GAME',fill='white');frames.append(plate)
        strip.save(OUT/(name+'.png'));frames[0].save(ROOT/(name+'.gif'),save_all=True,append_images=frames[1:]+[frames[-1]]*8,duration=50,loop=0)
        for col,f in enumerate([0,4,8,12,18,22]):
            im=art.common.composite(cels[f]).resize((192,192),Image.Resampling.NEAREST);board.paste(im,(col*192,row*215+18),im);draw.text((col*192+4,row*215+2),name+' '+str(f),fill='white')
        records.append({'id':name,'frames':24,'source':name+'.aseprite','sha256':hashlib.sha256((OUT/(name+'.png')).read_bytes()).hexdigest()})
    board.save(ROOT/'followup-contact.png');(ROOT/'followup-assets.json').write_text(json.dumps(records,indent=2)+'\n',encoding='utf-8')
    print('Authored nine external layered flipbooks and moving previews.')
if __name__=='__main__':main()
