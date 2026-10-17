"""Single connected thick hemisphere and one wrapped pixel-wave material.

Shared position indices, per-corner UV seam and analytic smooth normals are
authored together. This is not a grid of independently placed icons or panels.
"""
from pathlib import Path
import math,json,base64,io,zipfile,xml.etree.ElementTree as ET
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parent
N=16
ELEVATIONS=[0,15,30,45,60,75]
RADIUS=2.2
THICKNESS=.04
BASE=.1

def material():
    size=(256,64);layers=[Image.new('RGBA',size) for _ in range(4)]
    ImageDraw.Draw(layers[0]).rectangle((0,0,255,63),fill=(126,77,22,24))
    # One continuous painted wave, shared across the circumference. No sherd,
    # plaque or item silhouettes; deliberate stepped gold/ivory clusters.
    wave=[(0,42),(16,41),(27,36),(38,32),(48,25),(61,22),(74,21),(86,24),(98,30),(111,35),(126,40),(142,48),(156,51),(168,48),(181,43),(193,36),(208,31),(223,29),(235,33),(246,39),(256,42)]
    body=wave+[(x,y+7) for x,y in reversed(wave)]
    ImageDraw.Draw(layers[1]).polygon(body,fill=(229,155,36,146))
    ImageDraw.Draw(layers[2]).line(wave,fill=(255,210,91,195),width=3)
    for points in [wave[2:8],wave[11:16],wave[18:22]]:
        ImageDraw.Draw(layers[3]).line(points,fill=(255,245,202,225),width=1)
    curls=[[(39,32),(43,24),(51,18),(60,16),(67,18),(68,22),(64,25),(59,24),(58,21)],
           [(152,49),(162,39),(172,32),(180,31),(185,34),(183,38),(178,39),(175,36)],
           [(210,31),(218,22),(227,18),(234,19),(237,23),(234,26),(229,26)]]
    for p in curls:
        ImageDraw.Draw(layers[1]).line(p,fill=(226,151,32,152),width=5)
        ImageDraw.Draw(layers[2]).line([(x,y-1) for x,y in p],fill=(255,216,107,198),width=2)
    for x,y in [(28,21),(90,15),(119,25),(143,32),(196,20),(245,23),(73,44),(104,47)]:
        ImageDraw.Draw(layers[3]).polygon([(x,y-2),(x+1,y),(x,y+2),(x-1,y)],fill=(255,245,209,210))
    # The polar cap is intentionally uniform: no longitudinal rune pinching.
    for l in layers[1:]:ImageDraw.Draw(l).rectangle((0,0,255,9),fill=(0,0,0,0))
    atlas=Image.new('RGBA',size)
    for l in layers:atlas.alpha_composite(l)
    # Identical seam columns, nearest sampling and repeat keep the wrap joined.
    atlas.paste(atlas.crop((0,0,1,64)),(255,0))
    atlas.save(ROOT/'gold-wave.png')
    doc=ET.Element('image',w='256',h='64',name='Connected guardian wave');stack=ET.SubElement(doc,'stack')
    with zipfile.ZipFile(ROOT/'gold-wave.ora','w') as z:
        z.writestr('mimetype','image/openraster')
        for i,name in reversed(list(enumerate(['energy body','amber flow','gold crest','ivory cores']))):
            src=f'data/{i}.png';ET.SubElement(stack,'layer',name=name,src=src,opacity='1.0',visibility='visible',**{'composite-op':'svg:src-over','x':'0','y':'0'})
            data=io.BytesIO();layers[i].save(data,format='PNG');z.writestr(src,data.getvalue())
        data=io.BytesIO();atlas.save(data,format='PNG');z.writestr('mergedimage.png',data.getvalue());z.writestr('stack.xml',ET.tostring(doc))

def geometry():
    vertices={};faces=[]
    for layer,r in [('outer',RADIUS),('inner',RADIUS-THICKNESS)]:
        for row,degrees in enumerate(ELEVATIONS):
            phi=math.radians(degrees)
            for j in range(N):
                theta=2*math.pi*j/N
                vertices[f'{layer}_{row}_{j}']=[16*r*math.cos(phi)*math.sin(theta),16*(BASE+r*math.sin(phi)),16*r*math.cos(phi)*math.cos(theta)]
        vertices[layer+'_pole']=[0,16*(BASE+r),0]
        for row in range(len(ELEVATIONS)-1):
            bottom=64*(1-ELEVATIONS[row]/90);top=64*(1-ELEVATIONS[row+1]/90)
            for j in range(N):
                k=(j+1)%N;u0=j*256/N;u1=(j+1)*256/N
                ids=[f'{layer}_{row}_{j}',f'{layer}_{row}_{k}',f'{layer}_{row+1}_{k}',f'{layer}_{row+1}_{j}']
                uv=[[u0,bottom],[u1,bottom],[u1,top],[u0,top]]
                if layer=='inner':ids.reverse();uv.reverse()
                faces.append(dict(vertices=ids,uv=uv,texture=0))
        row=len(ELEVATIONS)-1;v=64*(1-ELEVATIONS[-1]/90)
        for j in range(N):
            ids=[f'{layer}_{row}_{j}',f'{layer}_{row}_{(j+1)%N}',layer+'_pole']
            uv=[[j*256/N,v],[(j+1)*256/N,v],[(j+.5)*256/N,0]]
            if layer=='inner':ids.reverse();uv.reverse()
            faces.append(dict(vertices=ids,uv=uv,texture=0))
    # Close the shell's bottom thickness with one joined rim, no separate rings.
    for j in range(N):
        k=(j+1)%N
        faces.append(dict(vertices=[f'outer_0_{k}',f'outer_0_{j}',f'inner_0_{j}',f'inner_0_{k}'],uv=[[0,63],[1,63],[1,64],[0,64]],texture=0))
    return vertices,faces

def main():
    material();vertices,faces=geometry()
    source='data:image/png;base64,'+base64.b64encode((ROOT/'gold-wave.png').read_bytes()).decode()
    ops=[dict(op='add_texture',name='gold-wave',source=source,width=256,height=64,render_mode='default'),dict(op='add_group',name='guardian_shell',origin=[0,1.6,0]),dict(op='add_mesh',name='connected_hemisphere',parent='guardian_shell',origin=[0,0,0],vertices=vertices,faces=faces,texture=0),dict(op='add_animation',name='guardian_shell',length=1.5,snapping=20)]
    for t,s in [(0,.92),(.2,1),(1.1,1),(1.5,1.06)]:ops.append(dict(op='set_keyframe',animation='guardian_shell',bone='guardian_shell',channel='scale',time=t,value=[s]*3))
    (ROOT/'operations.json').write_text(json.dumps(ops,indent=2)+'\n')
    (ROOT/'structure.json').write_text(json.dumps(dict(radius=RADIUS,thickness=THICKNESS,baseY=BASE,vertices=len(vertices),faces=len(faces),azimuthSegments=N,elevations=ELEVATIONS,texelsPerBlock=256/(2*math.pi*RADIUS)),indent=2)+'\n')
    print('Connected dome:',len(vertices),'shared vertices,',len(faces),'faces; one mesh')

if __name__=='__main__':main()
