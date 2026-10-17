"""External pixel atlas and cuboid/bone/keyframe contract for Blockbench MCP."""
from pathlib import Path
import json,math,base64,zipfile,io,xml.etree.ElementTree as ET
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parent

def main():
    layers=[Image.new('RGBA',(64,32)) for _ in range(3)]
    for slot in range(4):
        x=slot*16;d=[ImageDraw.Draw(l) for l in layers]
        d[0].rectangle((x+2,4,x+13,11),fill='#93501f')
        d[0].rectangle((x+4,2,x+11,13),fill='#93501f')
        d[1].rectangle((x+3,5,x+12,10),fill='#e2a738')
        d[1].rectangle((x+5,3,x+10,12),fill='#e2a738')
        d[1].line([(x+4,4),(x+11,4),(x+12,5)],fill='#ffe096',width=1)
        d[1].line([(x+4,11),(x+11,11)],fill='#b87724',width=1)
        paths=[[(5,8),(5,6),(7,6),(7,5),(10,5),(10,8),(9,8),(9,10)],
               [(5,6),(7,6),(7,8),(10,8),(10,10),(8,10)],
               [(5,10),(5,8),(7,8),(7,5),(10,5),(10,7)],
               [(5,5),(5,7),(10,7),(10,10),(8,10),(8,9)]]
        d[2].line([(x+a,b) for a,b in paths[slot]],fill='#ffedb0',width=1)
        d[2].rectangle((x+7,7,x+8,8),fill='#fff7d8')
    d=ImageDraw.Draw(layers[0]);d.rectangle((0,16,31,31),fill='#9e5722');d.rectangle((32,16,47,31),fill='#edb649');d.rectangle((48,16,63,31),fill='#fff5cd')
    atlas=Image.new('RGBA',(64,32))
    for l in layers:atlas.alpha_composite(l)
    atlas.save(ROOT/'atlas.png')
    # Standard layered OpenRaster, with embedded PNG layers and merged preview.
    doc=ET.Element('image',w='64',h='32',name='Guardian mosaic');stack=ET.SubElement(doc,'stack')
    with zipfile.ZipFile(ROOT/'atlas.ora','w') as z:
        z.writestr('mimetype','image/openraster')
        for i,name in reversed(list(enumerate(['amber structure','gold relief','engraving and ivory']))):
            src=f'data/layer{i}.png';ET.SubElement(stack,'layer',name=name,src=src,opacity='1.0',visibility='visible',**{'composite-op':'svg:src-over','x':'0','y':'0'})
            b=io.BytesIO();layers[i].save(b,format='PNG');z.writestr(src,b.getvalue())
        b=io.BytesIO();atlas.save(b,format='PNG');z.writestr('mergedimage.png',b.getvalue());z.writestr('stack.xml',ET.tostring(doc))
    ops=[dict(op='add_texture',name='guardian-gold',source='data:image/png;base64,'+base64.b64encode((ROOT/'atlas.png').read_bytes()).decode(),width=64,height=32,render_mode='emissive'),dict(op='add_animation',name='guardian_mosaic',length=1.5,snapping=20)]
    panels=[]
    for row,(elevation,count) in enumerate([(12,16),(32,12),(52,8),(72,4),(90,1)]):
        for i in range(count):
            yaw=i*360/count+(row%2)*180/count;a=math.radians(yaw);e=math.radians(elevation)
            center=[math.sin(a)*math.cos(e)*35.2,1.6+math.sin(e)*35.2,math.cos(a)*math.cos(e)*35.2]
            name=f'plate_{row}_{i:02}';panels.append(dict(name=name,center=center,row=row,yaw=yaw))
            ops.append(dict(op='add_group',name=name,origin=center,rotation=[-elevation,yaw,(-1 if i%2 else 1)*6]))
            slot=(i+row)%4*16
            # Four independent cuboids, not a sphere fragment or polygon mesh.
            for part,lo,hi,uv in [('body',[-8,-5,-.65],[8,5,.65],[slot+2,4,slot+14,12]),('upper',[-5.3,5,-.65],[5.3,8,.65],[slot+4,2,slot+12,4]),('lower',[-5.3,-8,-.65],[5.3,-5,.65],[slot+4,12,slot+12,14]),('core',[-.8,-.8,.66],[.8,.8,1.0],[48,16,50,18])]:
                faces={side:dict(uv=uv if side in ['north','south'] else [0,16,4,20],texture=0) for side in ['north','south','east','west','up','down']}
                ops.append(dict(op='add_cube',name=name+'_'+part,parent=name,origin=center,box_uv=False,**{'from':[center[j]+lo[j] for j in range(3)],'to':[center[j]+hi[j] for j in range(3)]},faces=faces))
            delay=row*.018+(i%3)*.012
            for t,v in [(0,.001),(delay+.02,.001),(delay+.20,1),(.65,1),(.98,1.025),(1.10+(i%3)*.025,1),(1.5,.001)]:
                ops.append(dict(op='set_keyframe',animation='guardian_mosaic',bone=name,channel='scale',time=t,value=[v]*3))
            for t,v in [(0,[0,0,-3]),(.25+delay,[0,0,0]),(.75,[0,(i%3-1)*.4,.25]),(1.08,[0,0,0]),(1.5,[0,1.5,8+(i%3)])]:
                ops.append(dict(op='set_keyframe',animation='guardian_mosaic',bone=name,channel='position',time=t,value=v))
            ops.append(dict(op='set_keyframe',animation='guardian_mosaic',bone=name,channel='rotation',time=1.08,value=[0,0,0]))
            ops.append(dict(op='set_keyframe',animation='guardian_mosaic',bone=name,channel='rotation',time=1.5,value=[i%2*16-8,0,i%3*12-12]))
    for i in range(12):
        a=i*math.pi/6;name=f'rune_spark_{i:02}';center=[math.sin(a)*28,10+(i%4)*5,math.cos(a)*28]
        ops.append(dict(op='add_group',name=name,origin=center,rotation=[0,i*30,45]))
        ops.append(dict(op='add_cube',name=name+'_ivory',parent=name,origin=center,box_uv=False,**{'from':[center[j]-.65 for j in range(3)],'to':[center[j]+.65 for j in range(3)]},faces={s:dict(uv=[48,16,50,18],texture=0) for s in ['north','south','east','west','up','down']}))
        for t,v in [(0,.001),(.2+(i%3)*.04,.001),(.5,1),(.95,1),(1.5,.001)]:ops.append(dict(op='set_keyframe',animation='guardian_mosaic',bone=name,channel='scale',time=t,value=[v]*3))
        for t,v in [(0,[0,0,0]),(.85,[0,3,0]),(1.5,[math.sin(a)*7,7,math.cos(a)*7])]:ops.append(dict(op='set_keyframe',animation='guardian_mosaic',bone=name,channel='position',time=t,value=v))
    (ROOT/'operations.json').write_text(json.dumps(ops,indent=2)+'\n')
    (ROOT/'structure.json').write_text(json.dumps(dict(plates=41,cuboids=176,sparks=12,frames=31,seconds=1.5,panels=panels),indent=2)+'\n')
    print('External atlas, layered ORA and',len(ops),'MCP operations prepared')

if __name__=='__main__':main()
