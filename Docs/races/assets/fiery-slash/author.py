"""External native-pixel cel authoring. Explicit painted contours, no polar rings/noise.

RGBA layers and Aseprite timeline are the editable source. PNG strips are runtime
exports; the GIF is a real playback of those same exported cels, not game footage.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageChops
import struct, zlib, json, hashlib

ROOT = Path(__file__).resolve().parent
EXPORT = ROOT.parents[3] / 'Mods/JEMVisualSandboxR3/src/main/resources/assets/jem_visual_sandbox/fire_slash'
SIZE = 96
COLORS = ['#a91e25', '#ed3820', '#ff801b', '#ffce32', '#fff18a']
# Each stroke has its own outer body, inner hot sweep, and counterflow tongues.
# Coordinates are deliberately drawn pixel contours, not an ellipse/ribbon generator.
STROKES = [
 dict(name='rising_hook', body=[(9,82),(14,65),(20,49),(29,34),(42,23),(55,16),(73,12),(88,14),(75,18),(64,23),(58,31),(64,27),(61,38),(53,39),(50,48),(43,50),(40,60),(34,61),(32,69),(25,68),(27,76),(20,75),(17,84)],
      hot=[(13,77),(23,51),(36,34),(52,24),(72,17),(62,23),(50,32),(42,41),(36,51),(29,58),(24,71),(21,67),(18,78)],
      tongues=[[(28,56),(29,42),(35,31),(33,43),(37,41),(34,51)],[(44,38),(48,23),(58,16),(53,27),(56,25),(52,35)],[(21,70),(18,58),(23,46),(22,60),(26,57),(25,68)]],
      tears=[[(27,65),(39,47),(34,61),(30,69)],[(46,39),(58,29),(52,39),(48,44)]], flow=(1,-1)),
 dict(name='return_sweep', body=[(7,46),(19,57),(34,62),(52,61),(69,54),(82,45),(89,33),(87,51),(81,63),(68,74),(50,81),(30,81),(16,76),(24,75),(14,70),(27,72),(19,64),(31,68),(30,61),(20,60)],
      hot=[(12,53),(30,65),(49,68),(67,61),(81,49),(75,62),(60,73),(45,76),(28,73),(35,72),(24,66)],
      tongues=[[(35,76),(31,63),(38,56),(37,66),(43,62),(44,72)],[(56,71),(62,54),(72,45),(68,58),(75,54),(66,68)],[(22,72),(15,65),(15,58),(23,64),(25,60),(29,71)]],
      tears=[[(39,78),(45,67),(50,66),(47,77)],[(65,69),(75,55),(72,67),(66,73)]], flow=(-1,0)),
 dict(name='descending_cleave', body=[(27,7),(44,14),(60,27),(72,42),(81,60),(84,78),(78,89),(71,83),(73,76),(64,79),(66,70),(56,72),(58,63),(50,64),(48,53),(41,51),(40,43),(31,39),(36,35),(26,24),(34,28),(29,18)],
      hot=[(32,12),(48,24),(62,43),(73,64),(78,82),(72,77),(70,66),(65,69),(63,57),(56,50),(55,43),(47,36),(46,29)],
      tongues=[[(49,44),(34,28),(28,15),(37,25),(40,24),(46,36)],[(61,62),(44,53),(38,44),(50,49),(49,43),(58,52)],[(72,76),(57,74),(50,67),(61,70),(59,65),(69,68)]],
      tears=[[(55,47),(64,56),(64,65),(59,61)],[(43,30),(54,40),(50,39),(44,36)]], flow=(0,1))
]
NAMES = ['ember fringe', 'flame body', 'hot flow', 'yellow core', 'breakaway embers']

def blank(): return Image.new('RGBA',(SIZE,SIZE))
def poly(im,points,color): ImageDraw.Draw(im).polygon(points, fill=color)
def shifted(points,dx,dy): return [(x+dx,y+dy) for x,y in points]

def cel(stroke,f,revision):
    layers=[blank() for _ in NAMES]
    if f==9: return layers
    # Explicit nonlinear reveal: fast travel, brief full silhouette, then erosion.
    reveal=[.12,.40,.82,1,1,1,1,1,1][f]
    body=stroke['body']; hot=stroke['hot']; vx,vy=stroke['flow']
    if revision!='r1':
        # A broader, newly drawn impact body per strike. No recoloring/mirroring
        # of a common crescent. Frame-specific control points animate its tongues.
        silhouettes = {
          'rising_hook':([(8,84),(12,60),(20,39),(35,23),(52,13),(73,10),(89,16),(72,18),(60,24),(56,33),(65,29),(59,42),(50,42),(49,54),(42,50),(39,66),(32,60),(30,77),(23,73),(20,87),(16,79)],[(12,77),(19,48),(33,29),(50,19),(71,14),(61,21),(47,31),(40,44),(34,45),(30,60),(26,57),(20,75),(19,65)]),
          'return_sweep':([(6,38),(18,55),(36,60),(53,57),(69,46),(83,29),(87,17),(89,40),(82,59),(70,75),(51,85),(29,87),(12,77),(21,77),(9,65),(22,71),(17,55),(30,65),(33,57),(40,66),(51,63)],[(12,50),(29,64),(45,67),(62,59),(75,45),(82,32),(78,51),(64,71),(47,79),(28,79),(36,76),(21,68),(31,70)]),
          'descending_cleave':([(21,7),(44,13),(62,25),(77,43),(86,65),(84,86),(75,92),(67,83),(70,74),(57,79),(60,65),(49,68),(49,57),(38,58),(40,46),(30,45),(35,35),(23,30),(32,28),(24,20)],[(29,11),(49,23),(65,44),(76,65),(79,83),(72,75),(70,60),(63,63),(60,50),(53,46),(52,37),(42,29),(42,24)])}
        body,hot=silhouettes[stroke['name']]
        # Separate leading-edge acceleration from lagging fringe, in native pixels.
        # These per-cel poses are not a transform of the whole finished sprite.
        poses=[(-5,6),(-3,4),(0,1),(3,-2),(5,-3),(6,-2),(7,0),(7,1),(8,2)]
        dx,dy=poses[f]
        def pose(points):
            return [(x+round(dx*(y/95 if stroke['name']=='return_sweep' else x/95)),y+round(dy*x/95)) for x,y in points]
        body,hot=pose(body),pose(hot)
    poly(layers[0],shifted(body,-1 if f<4 else -2,2),COLORS[0])
    poly(layers[1],body,COLORS[1]); poly(layers[2],hot,COLORS[2])
    # Inner flow is an asymmetrical broken highlight, not an outline stroke.
    if revision=='r1':poly(layers[3],hot[1:-2],COLORS[3])
    else:
        # Narrow hot edge with unequal thickness; the orange body remains visible.
        edge=hot[:7]+[(round((x+48)/2),round((y+48)/2)) for x,y in reversed(hot[2:7])]
        poly(layers[3],edge,COLORS[3])
    for i,t in enumerate(stroke['tongues']):
        dx=[0,-1,0,1,2,1,0,-1,-2][(f+i)%9]
        dy=[1,0,-1,-2,-1,0,1,2,1][(f+2*i)%9]
        poly(layers[2],shifted(t,dx,dy),COLORS[2])
        if f in (2,3,4):
            t2=[(round((x*2+t[0][0])/3),round((y*2+t[0][1])/3)) for x,y in t]
            poly(layers[3],shifted(t2,dx,dy),COLORS[3])
    # Every cutout follows a designed prior tongue; breakup keeps spatial ancestry.
    for i,tear in enumerate(stroke['tears']):
        if f>=3+i:
            for layer in layers[:4]:poly(layer,shifted(tear,(f-4)*vx,(f-4)*vy),(0,0,0,0))
    # Wipe along the actual slash flow, without scaling/rotating the finished shape.
    if f<3:
        for layer in layers:
            pix=layer.load()
            for y in range(SIZE):
                for x in range(SIZE):
                    progress=(x+(95-y))/190 if stroke['name']=='rising_hook' else ((95-x)/95 if stroke['name']=='return_sweep' else y/95)
                    if progress>reveal:pix[x,y]=(0,0,0,0)
    # Art-directed breakup masks open slits through the broad belly over time.
    if f>=5:
        for layer in layers[:4]:
            for j in range(f-4):
                if stroke['name']=='rising_hook':cut=[(8+9*j,85),(16+9*j,67-5*j),(23+9*j,54-5*j),(18+9*j,80)]
                elif stroke['name']=='return_sweep':cut=[(10+14*j,85),(18+14*j,65),(23+14*j,60),(21+14*j,85)]
                else:cut=[(20,17+14*j),(54+3*j,31+14*j),(70+2*j,37+14*j),(26,29+14*j)]
                poly(layer,cut,(0,0,0,0))
        if f>=7:
            # Shrinking residue is no longer a complete crescent.
            mask=Image.new('L',(96,96));md=ImageDraw.Draw(mask)
            for i,t in enumerate(stroke['tongues']):
                x,y=t[0];d=9 if f==7 else 4
                if revision=='r1' or revision=='r2':md.polygon([(x-d,y),(x,y-d),(x+d,y),(x,y+d)],fill=255)
                else:
                    md.polygon([(x-d,y+3),(x-d//2,y-1),(x+1,y-d),(x+2,y-d//2),(x+d,y-d),(x+d//2,y+2),(x,y+d)],fill=255)
            for layer in layers[:4]:layer.putalpha(ImageChops.multiply(layer.getchannel('A'),mask))
    if f>=3:
        for i,t in enumerate(stroke['tongues']):
            x,y=t[2];age=f-3;dx=age*(vx+(-1 if i%2 else 1));dy=age*(vy-1)
            x+=dx;y+=dy;w=max(1,4-age//2)
            poly(layers[4],[(x-w,y+3),(x-w,y),(x+1,y-w-3),(x,y),(x+w,y-1),(x+w-1,y+3),(x,y+w)],COLORS[2 if f<7 else 1])
            ImageDraw.Draw(layers[4]).point((x,y),fill=COLORS[3])
    if revision=='r4' and f in (7,8):
        # Redraw the release as surviving torn tongues, rather than clipping
        # the entire body to diamonds. Each piece continues the prior local flow.
        release={
          'rising_hook': [[(14,78),(18,60),(25,45),(34,35),(28,48),(27,58),(21,65),(22,58)],[(41,38),(49,26),(63,18),(78,16),(63,23),(56,32),(53,28),(47,38)]],
          'return_sweep': [[(18,70),(30,76),(45,76),(57,70),(50,77),(34,81),(28,78),(24,79)],[(57,69),(69,57),(80,41),(77,57),(69,70),(66,66),(62,72)]],
          'descending_cleave': [[(38,23),(47,29),(59,43),(63,55),(57,48),(54,49),(50,40),(47,39)],[(65,57),(72,66),(78,81),(73,86),(71,78),(66,80),(68,69),(64,68)]]}
        layers[:4]=[blank() for _ in range(4)]
        for j,points in enumerate(release[stroke['name']]):
            if f==8:
                # Shrink individual torn lobes about their own live tip.
                cx,cy=points[3];points=[(round(cx+(x-cx)*.5)+vx*3,round(cy+(y-cy)*.5)+vy*3) for x,y in points]
            poly(layers[1],points,COLORS[1]);poly(layers[2],points[1:-1],COLORS[2])
            if f==7:poly(layers[3],points[2:5],COLORS[3])
    return layers

def composite(layers):
    out=blank()
    for l in layers:out.alpha_composite(l)
    return out

def escalating_cel(stroke, hit, frame, count):
    """R5: continuous fire masses, independently authored escalating releases.

    No interior slash/erosion masks. Only the silhouette's natural flame valleys
    separate its tongues. Size, density, impact shape and lifetime escalate.
    """
    layers=[blank() for _ in NAMES]
    if frame==count-1:return layers
    bodies=[
      [(23,72),(27,56),(34,43),(45,34),(58,29),(73,30),(62,33),(56,39),(52,47),(47,46),(44,55),(39,54),(36,64),(32,61),(29,73)],
      [(8,43),(18,54),(34,59),(52,57),(68,49),(80,37),(87,25),(85,45),(78,60),(66,73),(50,81),(31,83),(15,75),(23,75),(15,65),(27,70),(24,59),(35,66),(39,60),(47,65),(59,60)],
      [(16,5),(39,9),(59,22),(74,40),(83,58),(86,79),(78,91),(68,88),(66,78),(54,83),(57,70),(45,74),(47,63),(34,64),(39,52),(27,48),(35,42),(23,34),(32,32),(22,21),(29,24)]
    ]
    flows=[
      [(27,65),(36,45),(48,36),(65,31),(56,37),(48,47),(42,47),(38,58),(35,55),(31,67)],
      [(15,54),(29,64),(45,66),(61,59),(77,45),(81,36),(78,53),(65,68),(49,76),(32,76),(37,74),(24,68),(33,70)],
      [(24,10),(43,19),(60,37),(74,60),(79,80),(73,86),(69,74),(61,76),(61,63),(53,62),(52,51),(43,46),(44,38),(33,29),(36,28)]
    ]
    cores=[
      [(29,61),(40,44),(51,36),(62,33),(51,40),(43,49),(39,49),(34,61)],
      [(22,63),(40,69),(56,65),(70,56),(77,47),(69,64),(53,73),(39,74)],
      [(34,17),(48,30),(62,51),(73,75),(73,82),(67,69),(61,65),(57,52),(48,42),(48,35)]
    ]
    # Clearly separated action keys: release travels rapidly, the final impact
    # settles longer. Shape deformation affects the fringe more than the core.
    peak=3 if hit<2 else 4
    release=5 if hit<2 else 7
    dx=[-5,-3,0,2,3,4,5,6][min(frame,7)]
    dy=[5,3,0,-1,-2,-1,0,1][min(frame,7)]
    def posed(points,lag=1):
        return [(x+round(dx*(x/95)*lag),y+round(dy*(y/95)*lag)) for x,y in points]
    if frame<=release:
        poly(layers[0],posed(shifted(bodies[hit],-1,2)),COLORS[0])
        poly(layers[1],posed(bodies[hit]),COLORS[1])
        poly(layers[2],posed(flows[hit],.65),COLORS[2])
        poly(layers[3],posed(cores[hit],.4),COLORS[3])
        # A hot curved lobe at the terminal contact, only on the stronger hits.
        if hit==2 and 4<=frame<=7:
            crest=[(37,77),(42,66),(44,75),(51,59),(53,73),(61,62),(61,76),(69,70),(72,83),(83,77),(77,89),(64,93),(50,90)]
            poly(layers[1],crest,COLORS[1]);poly(layers[2],crest[1:-2],COLORS[2])
            poly(layers[3],[(49,78),(53,70),(57,80),(63,76),(67,88),(55,88)],COLORS[3])
        if frame<peak:
            # Leading-edge reveal is over after the anticipation/release keys;
            # no slicing or negative strips remain inside the fire itself.
            amount=[.18,.50,.86,1][min(frame,3)]
            for layer in layers:
                pixels=layer.load()
                for y in range(96):
                    for x in range(96):
                        travel=(x+95-y)/190 if hit==0 else ((95-x)/95 if hit==1 else y/95)
                        if travel>amount:pixels[x,y]=(0,0,0,0)
    else:
        # Release drawn as coherent flame tongues, not cut-out slash patterns.
        tails=[
          [[(28,67),(33,50),(42,41),(37,53),(36,61),(32,60)],[(46,41),(54,34),(66,32),(57,37),(53,43),(51,39)]],
          [[(18,69),(33,76),(49,76),(59,70),(51,79),(34,82),(26,78)],[(57,71),(70,58),(81,42),(78,57),(72,68),(66,76),(66,69)]],
          [[(31,20),(44,29),(55,44),(59,56),(52,48),(48,48),(44,36)],[(63,57),(73,71),(79,85),(73,90),(68,81),(62,83),(64,72)],[(42,81),(48,72),(50,81),(57,68),(59,82),(67,79),(69,89),(55,92)]]
        ][hit]
        age=frame-release-1;length=count-release-2
        factor=max(.22,1-age/max(1,length)*.8)
        for j,points in enumerate(tails):
            cx,cy=points[2]
            points=[(round(cx+(x-cx)*factor)+age*(1 if hit<2 else 0),round(cy+(y-cy)*factor)-age) for x,y in points]
            poly(layers[1],points,COLORS[1]);poly(layers[2],points[1:-1],COLORS[2])
            if age<2:poly(layers[3],points[1:4],COLORS[3])
    # Compact first-hit embers; more numerous, delayed flame knots for the finisher.
    sites=[[(37,42)],[(28,63),(66,54),(78,42)],[(40,29),(60,52),(75,72),(50,81),(64,84)]]
    if frame>=peak:
        for j,(x,y) in enumerate(sites[hit]):
            age=frame-peak-j//2
            if age<0:continue
            x+=age*(1 if j%2 else -1);y-=age
            w=max(1,3-age//3)
            poly(layers[4],[(x-w,y+2),(x-w,y),(x+1,y-w-3),(x,y-1),(x+w,y-2),(x+w,y+2),(x,y+w)],COLORS[2 if age<5 else 1])
    return layers

def aseprite(path,cels):
    def chunk(t,b):return struct.pack('<IH',len(b)+6,t)+b
    def string(s):b=s.encode();return struct.pack('<H',len(b))+b
    frames=[]
    for f,layers in enumerate(cels):
        chunks=[]
        if f==0:
            for name in NAMES:chunks.append(chunk(0x2004,struct.pack('<6HB3x',3,0,0,0,0,0,255)+string(name)))
        for i,im in enumerate(layers):
            data=struct.pack('<HhhBHh5xHH',i,0,0,255,2,0,SIZE,SIZE)+zlib.compress(im.tobytes())
            chunks.append(chunk(0x2005,data))
        payload=b''.join(chunks);frames.append(struct.pack('<IHHH2xI',len(payload)+16,0xF1FA,len(chunks),50,0)+payload)
    h=bytearray(128);struct.pack_into('<I5HIH',h,0,128+sum(map(len,frames)),0xA5E0,len(frames),96,96,32,1,50);h[34]=1;h[35]=1
    path.write_bytes(bytes(h)+b''.join(frames))

def main():
    import argparse
    parser=argparse.ArgumentParser();parser.add_argument('--revision',default='r1');args=parser.parse_args()
    dest=ROOT/args.revision;dest.mkdir(exist_ok=True);EXPORT.mkdir(parents=True,exist_ok=True)
    counts=[10,12,16] if args.revision=='r5' else [10,10,10]
    allframes=[];receipt=[];board=Image.new('RGB',(max(counts)*96,3*118),(23,25,33));bd=ImageDraw.Draw(board)
    for row,stroke in enumerate(STROKES):
        cels=[escalating_cel(stroke,row,f,counts[row]) if args.revision=='r5' else cel(stroke,f,args.revision) for f in range(counts[row])];frames=[composite(l) for l in cels];allframes+=frames
        source=dest/(stroke['name']+'.aseprite');aseprite(source,cels)
        sheet=Image.new('RGBA',(96,96*counts[row]))
        for f,im in enumerate(frames):
            sheet.paste(im,(0,f*96))
            board.paste(im,(f*96,row*118+20),im)
        bd.text((4,row*118+3),f'{row+1} / {stroke["name"]} / 50ms per cel',fill='white')
        sheet.save(dest/(stroke['name']+'-strip.png'));sheet.save(EXPORT/(stroke['name']+'.png'))
        receipt.append({'id':stroke['name'],'source':str(source.relative_to(ROOT)),'width':96,'height':96,'frames':counts[row],'frameTicks':1,'origin':[48,48],'pixelContentSha256':hashlib.sha256(sheet.tobytes()).hexdigest(),'sourceFileSha256':hashlib.sha256(source.read_bytes()).hexdigest(),'exportFileSha256':hashlib.sha256((dest/(stroke['name']+'-strip.png')).read_bytes()).hexdigest()})
    board.save(dest/'contact.png')
    previews=[]
    for i,im in enumerate(allframes+[blank()]*12):
        canvas=Image.new('RGB',(768,340),(24,28,38));d=ImageDraw.Draw(canvas)
        hit=0 if i<counts[0] else 1 if i<sum(counts[:2]) else 2
        width=round([2.4,3.6,5.0][hit]/5*288) if args.revision=='r5' else 288
        shown=im.resize((width,width),Image.Resampling.NEAREST)
        for x,bg in [(0,(24,28,38)),(384,(157,168,158))]:
            d.rectangle((x,0,x+383,339),fill=bg)
            canvas.paste(shown,(x+(384-width)//2,26+(288-width)),shown)
            d.text((x+12,315),'EXPORTED PIXEL CELS / '+('DARK' if x==0 else 'LIGHT'),fill=(255,235,190) if x==0 else (20,30,25))
        previews.append(canvas)
    previews[0].save(dest/'three-hit.gif',save_all=True,append_images=previews[1:],duration=50,loop=0,optimize=False)
    (dest/'assets.json').write_text(json.dumps({'revision':args.revision,'assets':receipt,'startsTicks':[0,10,22] if args.revision=='r5' else [0,10,20],'impactTicks':[4,14,27] if args.revision=='r5' else [4,14,24],'preview':'same runtime cels; no Minecraft footage'},indent=2)+'\n')
    print(dest)

if __name__=='__main__': main()
