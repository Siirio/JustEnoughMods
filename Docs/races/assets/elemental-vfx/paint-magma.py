"""External layered magma painting. Import exports; never paint in Blockbench."""
from pathlib import Path
from PIL import Image, ImageDraw
import io, zipfile, xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parent
STOPS=[(0,(255,155,36)),(7,(235,85,15)),(14,(137,40,17)),(23,(38,19,17)),(31,(9,10,13))]

def png(im):
    out=io.BytesIO();im.save(out,format='PNG');return out.getvalue()

def source(name,layers):
    merged=Image.new('RGBA',(32,32))
    for _,im in layers:merged.alpha_composite(im)
    merged.save(ROOT/(name+'.png'))
    tree=ET.Element('image',{'w':'32','h':'32','name':name});stack=ET.SubElement(tree,'stack')
    with zipfile.ZipFile(ROOT/(name+'.ora'),'w') as z:
        z.writestr('mimetype','image/openraster',compress_type=zipfile.ZIP_STORED)
        for i,(label,im) in reversed(list(enumerate(layers))):
            src=f'data/layer{i}.png';z.writestr(src,png(im))
            ET.SubElement(stack,'layer',{'name':label,'src':src,'opacity':'1.0','visibility':'visible','composite-op':'svg:src-over','x':'0','y':'0'})
        z.writestr('stack.xml',ET.tostring(tree));z.writestr('mergedimage.png',png(merged))

base=Image.new('RGBA',(32,32));d=ImageDraw.Draw(base)
for y in range(32):
    a,b=next((a,b) for a,b in zip(STOPS,STOPS[1:]) if a[0]<=y<=b[0])
    t=(y-a[0])/(b[0]-a[0]);color=tuple(round(a[1][k]*(1-t)+b[1][k]*t) for k in range(3))+(255,)
    d.line([(0,y),(31,y)],fill=color)
crust=Image.new('RGBA',(32,32));d=ImageDraw.Draw(crust)
# Unequal connected cooled plates; hot gaps widen near the upper edge.
for points,color in [([(0,3),(5,2),(8,5),(6,9),(0,11)],'#b14317'),([(13,0),(20,0),(23,3),(19,6),(15,5)],'#d65616'),([(27,4),(31,3),(31,12),(26,14),(23,10)],'#853016'),([(7,11),(13,8),(19,10),(19,15),(13,18),(7,16)],'#652317'),([(0,17),(5,15),(8,19),(6,24),(0,26)],'#281617'),([(17,21),(22,18),(29,21),(31,27),(25,29),(19,27)],'#1b1113'),([(8,27),(13,24),(17,28),(15,31),(5,31)],'#0b0c10')]:d.polygon(points,fill=color)
accents=Image.new('RGBA',(32,32));d=ImageDraw.Draw(accents)
for points,color in [([(0,1),(5,0),(8,2),(6,3),(1,3)],'#ffb43c'),([(14,0),(19,0),(22,2),(19,2)],'#ffac2d'),([(8,6),(11,4),(14,5),(13,7),(10,8)],'#ff881d'),([(23,7),(26,5),(28,7),(25,9)],'#e85a13'),([(8,12),(12,10),(16,11),(13,12)],'#a63b18'),([(2,20),(5,18),(6,20)],'#4b2118')]:d.polygon(points,fill=color)
source('rock',[('orange-to-charcoal whole-height heat',base),('cooled basalt plates',crust),('hot fracture lips',accents)])
seam=Image.new('RGBA',(32,32));d=ImageDraw.Draw(seam)
for points,color in [([(3,0),(9,0),(12,7),(8,12),(10,17),(6,20),(5,11),(8,6)],'#ff951d'),([(20,0),(24,0),(22,5),(25,9),(21,13),(19,10),(21,6)],'#ffad29'),([(8,17),(11,15),(13,20),(10,24),(8,23)],'#6c2315')]:d.polygon(points,fill=color)
source('molten',[('same global heat ramp',base),('seated molten fractures',seam)])
print('Exported layered magma materials: orange upper faces, dark lower faces.')
