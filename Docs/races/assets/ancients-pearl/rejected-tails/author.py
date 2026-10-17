"""New externally authored layered pixel cels; never sample reference artwork."""
from pathlib import Path
import struct, zlib, json, hashlib
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parent
OUT=ROOT.parents[3]/'Mods/JEMVisualSandboxR3/src/main/resources/assets/jem_visual_sandbox/ancients_pearl'
NAMES=['shadow fringe','colored body','bright flow','ivory core','detached pieces']

def aseprite(path,cels,size):
    def chunk(t,b): return struct.pack('<IH',len(b)+6,t)+b
    frames=[]
    for f,layers in enumerate(cels):
        chunks=[]
        if f==0:
            for name in NAMES:
                b=name.encode();chunks.append(chunk(0x2004,struct.pack('<6HB3x',3,0,0,0,0,0,255)+struct.pack('<H',len(b))+b))
        for i,im in enumerate(layers):
            chunks.append(chunk(0x2005,struct.pack('<HhhBHh5xHH',i,0,0,255,2,0,*size)+zlib.compress(im.tobytes())))
        b=b''.join(chunks);frames.append(struct.pack('<IHHH2xI',len(b)+16,0xF1FA,len(chunks),100,0)+b)
    h=bytearray(128);struct.pack_into('<I5HIH',h,0,128+sum(map(len,frames)),0xA5E0,len(cels),*size,32,1,100);h[34]=h[35]=1
    path.write_bytes(bytes(h)+b''.join(frames))

def layers(size): return [Image.new('RGBA',size) for _ in NAMES]
def composite(ls):
    out=Image.new('RGBA',ls[0].size)
    for l in ls: out.alpha_composite(l)
    return out

# Eight authored flow poses: crest, separation, lag, extension, curl, collapse,
# second crest, recovery. Tongue tips change independently of the bright neck.
TIPS=[(5,2,0),(9,-1,2),(13,-3,3),(8,0,1),(3,3,-1),(7,4,-2),(12,1,0),(8,-1,1)]
def trail(f):
    ls=layers((96,32));d=[ImageDraw.Draw(l) for l in ls];a,b,c=TIPS[f]
    d[0].polygon([(94,12),(82,7),(70,8),(57,5+b),(67,11),(44,9),(27,6+b),(38,12),(13+a,12),(3+a,15),(38,18),(20+a,23),(48,21),(61,27+c),(58,21),(77,23),(92,20)],fill='#491a79')
    d[1].polygon([(94,13),(82,9),(66,10),(55,8+b),(65,14),(45,12),(25+a,13),(39,16),(12+a,17),(43,19),(34+a,23),(56,20),(67,24+c),(64,19),(80,21),(94,18)],fill='#9b3ed3')
    d[2].polygon([(94,14),(83,11),(73,12),(66,10),(72,15),(54,14),(45,12+b),(50,16),(34+a,17),(57,18),(73,17),(80,19),(94,17)],fill='#d887f2')
    d[3].polygon([(94,14),(85,13),(79,14),(72,13),(75,15),(62,16),(80,17),(94,16)],fill='#f8d9ff')
    # Unequal small breakaways preserve the flow's direction; no random cloud.
    for x,y,w in [(6+a,10+b,4),(21+a,26+c,3),(37+a,4+b,5),(58+a//2,28,2)]:
        d[4].rectangle((x,y,x+w,y+1),fill='#a54ddd')
    return ls

# Deliberate scroll-shaped streams, unwrapped onto the shell. These are painted
# material strokes with open centers, not geometric sphere outlines or hex tiles.
SCROLLS=[[(3,50),(10,44),(17,33),(20,22),(18,13),(13,9),(8,11),(7,17),(11,20),(15,18),(14,14)],
         [(35,60),(45,54),(54,44),(59,30),(56,21),(50,18),(45,22),(46,28),(51,29),(53,25)],
         [(76,55),(88,47),(97,36),(99,23),(94,15),(87,14),(82,19),(84,25),(90,26),(93,21)],
         [(104,62),(115,52),(122,40),(126,27)]]
def shell(f):
    ls=layers((128,64));d=[ImageDraw.Draw(l) for l in ls]
    for i,path in enumerate(SCROLLS):
        # Small independently lagging crest bends; original negative spaces stay.
        drift=[0,1,2,1,0,-1,-2,-1][(f+i*2)%8]
        p=[(x,y+(drift if j<5 else 0)) for j,(x,y) in enumerate(path)]
        d[0].line(p,fill='#9b6226',width=7)
        d[1].line(p,fill='#d9a83d',width=5)
        d[2].line([(x-1,y-1) for x,y in p],fill='#ffe294',width=2)
        d[3].line(p[2:5],fill='#fff5d2',width=1)
    for i,(x,y) in enumerate([(29,15),(38,38),(69,24),(75,46),(109,9),(117,29)]):
        dy=[0,-1,-2,-1,0,1,2,1][(f+i)%8]
        d[4].polygon([(x,y-2+dy),(x+1,y+dy),(x,y+2+dy),(x-1,y+dy)],fill='#ffe7a0')
    return ls

def impact(f):
    ls=layers((32,32));d=[ImageDraw.Draw(l) for l in ls]
    # Fractured outward wisps followed by four surviving pieces, then clear.
    if f==7:return ls
    poses=[(2,7),(4,10),(7,12),(9,10),(11,7),(12,4),(13,2)]
    r,w=poses[f]
    for i,(dx,dy) in enumerate([(1,-1),(-1,-1),(1,1),(-1,1)]):
        x,y=16+dx*r,16+dy*r
        d[1].polygon([(x-dx*w,y),(x,y-dy*2),(x+dx*2,y+dy*3),(x-dx*3,y+dy)],fill='#9c44d1')
        d[3].line([(x-dx*w//2,y),(x,y+dy)],fill='#efbfff',width=1)
    return ls

def main():
    OUT.mkdir(parents=True,exist_ok=True);frames={};rows=[]
    for name,fn,size in [('pearl_afterfire',trail,(96,32)),('guardian_scrolls',shell,(128,64)),('pearl_breakup',impact,(32,32))]:
        cels=[fn(f) for f in range(8)];images=[composite(ls) for ls in cels];frames[name]=images
        aseprite(ROOT/(name+'.aseprite'),cels,size)
        strip=Image.new('RGBA',(size[0],size[1]*8))
        for f,im in enumerate(images):strip.paste(im,(0,f*size[1]))
        strip.save(OUT/(name+'.png'))
        rows.append(dict(name=name,size=size,frames=8,frameMs=100,layers=NAMES,sha256=hashlib.sha256((OUT/(name+'.png')).read_bytes()).hexdigest()))
    board=Image.new('RGB',(1024,264),'#20222a')
    for row,name in enumerate(frames):
        ImageDraw.Draw(board).text((4,row*88),name,fill='white')
        for f,im in enumerate(frames[name]):
            board.paste(im,(f*128,row*88+20),im)
    board.save(ROOT/'contact.png')
    preview=[]
    for f in range(40):
        canvas=Image.new('RGB',(768,300),'#252932' if f<20 else '#c1c7b7');d=ImageDraw.Draw(canvas)
        d.text((12,10),'ACTUAL EXPORTED CEL PLAYBACK - NOT MINECRAFT',fill='#e9b55f' if f<20 else '#332244')
        cel=frames['pearl_afterfire'][f%8].resize((576,192),Image.Resampling.NEAREST)
        canvas.paste(cel,(80,35),cel)
        # Scale cue only; native Minecraft pearl remains untouched at runtime.
        d.ellipse((635,114,667,146),fill='#135e58',outline='#a3d6a7',width=3)
        shellcel=frames['guardian_scrolls'][f%8].resize((256,128),Image.Resampling.NEAREST)
        canvas.paste(shellcel,(255,165),shellcel)
        preview.append(canvas)
    preview[0].save(ROOT/'asset-playback.gif',save_all=True,append_images=preview[1:],duration=100,loop=0)
    (ROOT/'assets.json').write_text(json.dumps(rows,indent=2)+'\n')

if __name__=='__main__':main()
