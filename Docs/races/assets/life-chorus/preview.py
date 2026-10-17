"""Playback of exported cels and actual baked shield quads; NOT Minecraft footage.

The neutral stage has no invented gameplay entities. Markers show where the
caster and two healing recipients would be. Orthographic CPU texture projection
is a presentation preview, not evidence of Minecraft lighting or collisions.
"""
from pathlib import Path
import math,struct
import numpy as np
from PIL import Image,ImageDraw
import author
ROOT=Path(__file__).resolve().parent
RES=author.REPO/'Mods/JEMVisualSandboxR3/src/main/resources/assets/jem_visual_sandbox'
W,H=640,390
ZOOM=1
BASE_Y=220
def project(p):
    x,y,z=p
    return np.array([320+(x-z)*14*ZOOM,BASE_Y+(x+z)*6*ZOOM-y*24*ZOOM])

def triangle(canvas,points,uv,texture):
    p=np.array(points);lo=np.maximum(np.floor(p.min(axis=0)).astype(int),[0,0]);hi=np.minimum(np.ceil(p.max(axis=0)).astype(int),[W-1,H-1])
    if np.any(hi<lo):return
    a,b,c=p;den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
    if abs(den)<.01:return
    xx,yy=np.meshgrid(np.arange(lo[0],hi[0]+1),np.arange(lo[1],hi[1]+1));xx=xx+.5;yy=yy+.5
    wa=((b[1]-c[1])*(xx-c[0])+(c[0]-b[0])*(yy-c[1]))/den
    wb=((c[1]-a[1])*(xx-c[0])+(a[0]-c[0])*(yy-c[1]))/den;wc=1-wa-wb
    mask=(wa>=0)&(wb>=0)&(wc>=0)
    u=wa*uv[0][0]+wb*uv[1][0]+wc*uv[2][0];v=wa*uv[0][1]+wb*uv[1][1]+wc*uv[2][1]
    tx=np.clip((u*texture.shape[1]).astype(int),0,texture.shape[1]-1);ty=np.clip((v*texture.shape[0]).astype(int),0,texture.shape[0]-1)
    color=texture[ty,tx];a=color[:,:,3:4]/255*mask[:,:,None]
    region=canvas[lo[1]:hi[1]+1,lo[0]:hi[0]+1];region[:]=region*(1-a)+color[:,:,:3]*a

def quad(canvas,points,texture,uv=None):
    if uv is None:uv=[(0,0),(1,0),(1,1),(0,1)]
    for indices in [(0,1,2),(0,2,3)]:triangle(canvas,[project(points[i]) for i in indices],[uv[i] for i in indices],texture)

def main():
    data=(RES/'workbench/shield.mesh').read_bytes();magic,frames,faces,step=struct.unpack_from('>iiif',data);assert magic==0x4a523301
    offset=16;uv=[]
    for face in range(faces):uv.append(struct.unpack_from('>8f',data,offset+1));offset+=33
    poses=np.array(struct.unpack_from('>'+str(frames*faces*12)+'f',data,offset)).reshape(frames,faces,4,3)
    model=poses[10];center=(model.min(axis=(0,1))+model.max(axis=(0,1)))/2;model=(model-center)*(.48/(model[:,:,1].max()-model[:,:,1].min()))
    texture=np.array(Image.open(RES/'workbench/shield-0.png').convert('RGBA'))
    strips={n:Image.open(author.OUT/(n+'.png')).convert('RGBA') for n in ['gold_rune','gold_swirl','gold_motes','gold_front']}
    def cel(name,f):return np.array(strips[name].crop((0,256*min(15,max(0,f)),384,256*(min(15,max(0,f))+1))))
    def billboard(canvas,at,name,f,size):
        x,y,z=at;side=size*.75/math.sqrt(2);quad(canvas,[(x-side,y+size/2,z+side),(x+side,y+size/2,z-side),(x+side,y-size/2,z-side),(x-side,y-size/2,z+side)],cel(name,f))
    def rune(canvas,r,frame,opacity=1,file='gold_rune'):
        tex=cel(file,frame);tex[:,:,3]=(tex[:,:,3]*opacity).astype(np.uint8)
        quad(canvas,[(-1.6*r,.03,-1.1*r),(1.6*r,.03,-1.1*r),(1.6*r,.03,1.1*r),(-1.6*r,.03,1.1*r)],tex)
    images=[]
    for age in range(0,132):
        global ZOOM,BASE_Y
        ZOOM=4 if age<7 or age>=37 else 1
        BASE_Y=270 if ZOOM==4 else 220
        base=Image.new('RGB',(W,H),'#162028');d=ImageDraw.Draw(base)
        for x in range(-16,17,2):
            d.line([tuple(project((x,0,-16))),tuple(project((x,0,16)))],fill='#223139');d.line([tuple(project((-16,0,x))),tuple(project((16,0,x)))],fill='#223139')
        for at,label in [((0,0,0),'caster'),((4,0,2),'ally'),((-3,0,4),'animal')]:
            x,y=project(at);d.rectangle((x-3,y-3,x+3,y+3),fill='#7a8e93');d.text((x+5,y),label,fill='#9eb0b4')
        canvas=np.array(base).astype(float)
        if age<7:rune(canvas,1.4,min(7,age),.8)
        if 7<=age<19:
            rune(canvas,1.4+(age-7)*.15,min(10,4+(age-7)//2),.75)
            rune(canvas,16*(age-6)/12,min(10,4+(age-7)//2),1,'gold_front')
        if 19<=age<31:
            rune(canvas,3.2,min(15,10+(age-19)//2),(31-age)/16)
            rune(canvas,16,min(15,10+(age-19)//2),(31-age)/12,'gold_front')
        if 12<=age<27:rune(canvas,16*(age-11)/15,10,.2*(27-age)/15,'gold_front')
        if age<14:billboard(canvas,(0,1.1,0),'gold_swirl',min(15,int(age*16/14)),2.5)
        for at,start in [((0,1,0),13),((4,1,2),13),((-3,.6,4),13)]:
            if start<=age<start+24:billboard(canvas,at,'gold_motes',min(15,int((age-start)*16/24)),1.7)
        if 18<=age<118:
            alpha=min(1,(age-18)/4,(118-age)/10);rune(canvas,1.25,7+(age//5)%3,alpha*.34);tex=texture.copy();tex[:,:,3]=(tex[:,:,3]*alpha).astype(np.uint8)
            draws=[]
            for i in range(4):
                a=math.radians(i*90+(age-18)*1.8);yaw=-a+math.pi*1.5
                rot=np.array([[math.cos(yaw),0,math.sin(yaw)],[0,1,0],[-math.sin(yaw),0,math.cos(yaw)]])
                placed=model@rot.T+np.array([math.cos(a)*1.06,.88+math.sin(a*2)*.05,math.sin(a)*1.06])
                for j,face in enumerate(placed):draws.append((face[:,0].mean()+face[:,2].mean(),face,j))
            for _,face,j in sorted(draws,key=lambda v:v[0]):quad(canvas,face,tex,np.array(uv[j]).reshape(4,2))
        if 118<=age<130:
            for i in range(4):
                a=math.radians(i*90+180);drift=(age-118)*.025;billboard(canvas,(math.cos(a)*(1.06+drift),.88+drift,math.sin(a)*(1.06+drift)),'gold_motes',min(15,11+(age-118)//3),.65)
        frame=Image.fromarray(canvas.astype(np.uint8));d=ImageDraw.Draw(frame);d.rectangle((0,0,W,40),fill='#0c141b');d.text((12,8),'LIFE CHORUS | original gold cels + outward R7 shield mesh',fill='#c5d9b7');d.text((12,23),'ASSET CHOREOGRAPHY / NOT MINECRAFT FOOTAGE',fill='#f0d793')
        d.rectangle((0,H-27,W,H),fill='#0c141b');d.text((12,H-19),f'{age/20:4.2f}s | '+('Awakening' if age<7 else 'Life pulse + creature responses' if age<37 else 'Four shields / server protection 25%' if age<118 else 'Golden leaf release'),fill='white')
        images.append(frame)
    images[0].save(ROOT/'life-chorus.gif',save_all=True,append_images=images[1:]+[images[-1]]*10,duration=50,loop=0,optimize=True)
    images[22].save(ROOT/'choreography.png')
if __name__=='__main__':main()
