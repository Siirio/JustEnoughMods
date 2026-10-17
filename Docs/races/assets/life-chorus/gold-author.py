"""Import original generated production cels at native size, with editable timelines.

No reference cropping, resizing, pixelation filters or alpha repainting. Sheets
were requested as production art. Cell extraction is a lossless format export;
transparent RGB haze is retained but alpha zero means it is never drawn.
"""
from pathlib import Path
from PIL import Image,ImageDraw
import struct,zlib,json,hashlib
ROOT=Path(__file__).resolve().parent
OUT=ROOT.parents[3]/'Mods/JEMVisualSandboxR3/src/main/resources/assets/jem_visual_sandbox/life_chorus'
WIDTH,HEIGHT=384,256

def timeline(path,cels,names):
    def chunk(t,b):return struct.pack('<IH',len(b)+6,t)+b
    frames=[]
    for f,layers in enumerate(cels):
        chunks=[]
        if f==0:
            for name in names:
                label=name.encode();chunks.append(chunk(0x2004,struct.pack('<6HB3x',3,0,0,0,0,0,255)+struct.pack('<H',len(label))+label))
        for i,im in enumerate(layers):chunks.append(chunk(0x2005,struct.pack('<HhhBHh5xHH',i,0,0,255,2,0,WIDTH,HEIGHT)+zlib.compress(im.tobytes())))
        payload=b''.join(chunks);frames.append(struct.pack('<IHHH2xI',len(payload)+16,0xF1FA,len(chunks),50,0)+payload)
    header=bytearray(128);struct.pack_into('<I5HIH',header,0,128+sum(map(len,frames)),0xA5E0,len(frames),WIDTH,HEIGHT,32,1,50);header[34:36]=b'\x01\x01'
    path.write_bytes(bytes(header)+b''.join(frames))

def main():
    records=[];board=Image.new('RGB',(6*192,4*150),'#162029');draw=ImageDraw.Draw(board)
    for row,name in enumerate(['gold_rune','gold_swirl','gold_motes','gold_front']):
        sheet=Image.open(ROOT/'production-sheets'/(name+'.png')).convert('RGBA')
        if sheet.size!=(1536,1024):raise ValueError('Native production cell contract changed')
        cels=[];offsets=[];strip=Image.new('RGBA',(WIDTH,HEIGHT*16))
        for f in range(16):
            # Preserve source pixels, alpha, frame order and native dimensions.
            cel=sheet.crop(((f%4)*WIDTH,(f//4)*HEIGHT,(f%4+1)*WIDTH,(f//4+1)*HEIGHT))
            dx=dy=0
            if name=='gold_rune':
                # Align the persistent ivory center diamond; generated grid
                # centers differ slightly. Lossless integer placement only.
                points=[(x,y) for y in range(80,176) for x in range(144,240) if min(cel.getpixel((x,y))[:3])>235 and cel.getpixel((x,y))[3]>190]
                if not points:raise ValueError('Central rune pivot missing')
                dx=192-round(sum(x for x,y in points)/len(points));dy=128-round(sum(y for x,y in points)/len(points))
                aligned=Image.new('RGBA',(WIDTH,HEIGHT));aligned.alpha_composite(cel,(dx,dy));cel=aligned
            elif name=='gold_front':
                if f<=10:
                    bounds=cel.getchannel('A').point(lambda a:255 if a>=190 else 0).getbbox()
                    if not bounds:raise ValueError('Wave contour missing')
                    dx=192-round((bounds[0]+bounds[2])/2);dy=128-round((bounds[1]+bounds[3])/2)
                else:dx,dy=offsets[4+f%4]
                aligned=Image.new('RGBA',(WIDTH,HEIGHT));aligned.alpha_composite(cel,(dx,dy));cel=aligned
            offsets.append([dx,dy])
            cels.append([cel]);strip.paste(cel,(0,f*HEIGHT))
        timeline(ROOT/(name+'.aseprite'),cels,['original RGBA production paint'])
        strip.save(OUT/(name+'.png'))
        for col,f in enumerate([0,3,6,9,12,15]):
            # Review thumbnails only. Runtime is never resampled.
            im=cels[f][0].resize((192,128),Image.Resampling.NEAREST);board.paste(im,(col*192,row*150+18),im);draw.text((col*192+4,row*150+2),name+' '+str(f),fill='#f4d89b')
        records.append(dict(id=name,width=WIDTH,height=HEIGHT,frames=16,durationMs=50,pivot=[192,128],integerOffsets=offsets,source=name+'.aseprite',nativePixelsNoResampling=True,exportSha256=hashlib.sha256((OUT/(name+'.png')).read_bytes()).hexdigest()))
    board.save(ROOT/'gold-contact.png');(ROOT/'gold-assets.json').write_text(json.dumps(records,indent=2)+'\n',encoding='utf-8')

if __name__=='__main__':main()
