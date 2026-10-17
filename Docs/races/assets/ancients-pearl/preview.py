"""Actual exported material / R7 mesh playback on a neutral CPU stage, not game footage."""
from pathlib import Path
import sys, importlib.util, math, struct
import numpy as np
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT.parent/'life-chorus'))
spec=importlib.util.spec_from_file_location('stage',ROOT.parent/'life-chorus/preview.py')
stage=importlib.util.module_from_spec(spec);spec.loader.exec_module(stage)
stage.ZOOM=3;stage.BASE_Y=310
def project(point):
    x,y,z=point;angle=math.radians(25)
    return np.array([320+(x-z)/math.sqrt(2)*66,310+((x+z)/math.sqrt(2)*math.sin(angle)-y*math.cos(angle))*66])
stage.project=project
RES=ROOT.parents[3]/'Mods/JEMVisualSandboxR3/src/main/resources/assets/jem_visual_sandbox'

def main():
    data=(RES/'workbench/shield.mesh').read_bytes()
    magic,frames,faces,step=struct.unpack_from('>iiif',data);offset=16;uv=[]
    for face in range(faces):uv.append(struct.unpack_from('>8f',data,offset+1));offset+=33
    model=np.array(struct.unpack_from('>'+str(frames*faces*12)+'f',data,offset)).reshape(frames,faces,4,3)[10]
    center=(model.min(axis=(0,1))+model.max(axis=(0,1)))/2
    model=(model-center)*(.95/(model[:,:,1].max()-model[:,:,1].min()))
    shield=np.array(Image.open(RES/'workbench/shield-0.png').convert('RGBA'))
    strip=Image.open(RES/'ancients_pearl/guardian_sphere.png').convert('RGBA')
    images=[]
    for age in range(0,160,2):
        background='#202a32' if age<80 else '#c2c6b3'
        base=Image.new('RGB',(stage.W,stage.H),background);d=ImageDraw.Draw(base)
        for x in [-2,0,2]:d.line([tuple(stage.project((x,0,-3))),tuple(stage.project((x,0,3)))],fill='#667071')
        # Neutral cuboid scale cue, not an invented in-game player render.
        for y in [.3,.9,1.5]:
            a=stage.project((0,y,0));d.rectangle((a[0]-7,a[1]-10,a[0]+7,a[1]+10),outline='#87989c',width=2)
        canvas=np.array(base).astype(float);draws=[]
        local=age%80
        if local<30:
            tex=np.array(strip.crop((0,(local//2%8)*64,128,(local//2%8+1)*64)))
            envelope=min(1,local/3,(30-local)/10)
            radius=1.45*(.88+.12*min(1,local/4))
            for y in range(16):
                for x in range(32):
                    points=[];coords=[]
                    alphas=[]
                    for u,v in [(x/32,y/16),((x+1)/32,y/16),((x+1)/32,(y+1)/16),(x/32,(y+1)/16)]:
                        longitude=u*math.pi*2+local*.018;latitude=(v-.5)*math.pi
                        normal=np.array((math.cos(longitude)*math.cos(latitude),math.sin(latitude),math.sin(longitude)*math.cos(latitude)))
                        point=np.array((0,1.35,0))+normal*radius
                        points.append(point)
                        direction=np.array((8,8,8))-point;direction/=np.linalg.norm(direction)
                        edge=1-abs(np.dot(normal,direction));alphas.append(envelope*(.12+.58*edge*edge))
                        coords.append((u,1-v))
                    panel=tex.copy();panel[:,:,3]=(panel[:,:,3]*sum(alphas)/4).astype(np.uint8)
                    draws.append((sum(p[0]+p[2] for p in points)/4,points,coords,panel))
        else:
            for i in range(4):
                a=math.radians(i*90+(local-30)*1.8);yaw=-a+math.pi*1.5
                rot=np.array([[math.cos(yaw),0,math.sin(yaw)],[0,1,0],[-math.sin(yaw),0,math.cos(yaw)]])
                placed=model@rot.T+np.array([math.cos(a)*1.45,1.05+math.sin(a*2)*.05,math.sin(a)*1.45])
                for j,face in enumerate(placed):draws.append((face[:,0].mean()+face[:,2].mean(),face,np.array(uv[j]).reshape(4,2),shield))
        for _,points,coords,tex in sorted(draws,key=lambda v:v[0]):stage.quad(canvas,points,tex,coords)
        image=Image.fromarray(canvas.astype(np.uint8));d=ImageDraw.Draw(image)
        d.text((12,12),'EXPORTED MATERIAL + R7 MESH / NOT MINECRAFT',fill='#ba8034')
        d.text((12,29),'Guardian shell' if local<30 else 'Chorus: four enlarged outward shields',fill='#ba8034')
        images.append(image)
    images[0].save(ROOT/'spatial-playback.gif',save_all=True,append_images=images[1:],duration=100,loop=0)
    images[8].save(ROOT/'guardian-spatial.png');images[26].save(ROOT/'shields-spatial.png')

if __name__=='__main__':main()
