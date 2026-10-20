"""Build structured Blockbench MCP operations; native alpha is read by Aseprite.

The extruded native sprites are editor previews only. Runtime uses ItemRenderer,
not these duplicated planes or any bundled vanilla image. Run against a fresh
copy of revision r1; the source model must not be open while headless editing.
"""
import json,sys
from pathlib import Path
stage=Path(sys.argv[1]); model=json.loads((stage/'shipping-station-native-r2.bbmodel').read_text())
alpha=json.loads((stage/'native-alpha.json').read_text())
textures={t['name'].removesuffix('.png'):t['uuid'] for t in model['textures']}
ops=[{'op':'remove_node','target':'native_goods_preview'},
     {'op':'add_group','name':'native_goods_preview_r2','origin':[8,8,8]}]
anchors=[]
faces6=['north','south','east','west','up','down']
def cube(name,a,b,faces,rotation=None,origin=None):
    op={'op':'add_cube','name':name,'from':a,'to':b,'parent':'native_goods_preview_r2','faces':faces}
    if rotation:op.update(rotation=rotation,origin=origin)
    ops.append(op)
def textured(name,a,b,tex,uv=[0,0,64,32],top=None):
    f={k:{'texture':tex,'uv':uv} for k in faces6}
    if top:
        f['up']={'texture':top,'uv':uv};f['down']={'texture':top,'uv':uv}
    cube(name,a,b,f)
atlas=model['textures'][0]['uuid']
textured('supported_inner_cargo_tray',[2.25,6.45,2.25],[13.75,6.95,10.5],atlas,[4,0,8,8])
# Four genuine oak LOGS, including end grain. No plank commodity.
for n,(x,y,z) in enumerate([(10.55,5.9,3),(10.55,5.9,6.05),(10.55,8.9,3),(10.55,8.9,6.05)]):
    textured('native_oak_log_'+str(n+1),[x,y,z],[x+3,y+3,z+3],textures['block_oak_log'],top=textures['oak_log_top'])
    anchors.append({'item':'minecraft:oak_log','position':[x+1.5,y+1.5,z+1.5],'rotation':[0,0,0],'scale':3/16})
def sprite(name,kind,x,y,z,size,angle):
    """Minecraft generated-item silhouette, front/back and alpha-boundary extrusion."""
    s=size/16;d=s; origin=[x+size/2,y+size/2,z+d/2]
    rot=[angle,0,0];tex=textures[kind]
    cube(name+'_faces',[x,y,z],[x+size,y+size,z+d],{
        'north':{'texture':tex,'uv':[0,0,64,32]},
        'south':{'texture':tex,'uv':[64,0,0,32]}},rot,origin)
    rows=alpha[kind]
    def opaque(px,py):return 0<=px<16 and 0<=py<16 and rows[py][px]=='1'
    # Match only native exposed boundary pixels; no opaque rectangular cardboard edge.
    for py in range(16):
        for px in range(16):
            if not opaque(px,py):continue
            a=[x+px*s,y+(15-py)*s,z];b=[a[0]+s,a[1]+s,z+d]
            uv=[px*4,py*2,(px+1)*4,(py+1)*2]
            for dx,dy,side in [(-1,0,'west'),(1,0,'east'),(0,-1,'up'),(0,1,'down')]:
                if opaque(px+dx,py+dy):continue
                aa=a.copy();bb=b.copy()
                axis=0 if dx else 1
                pos=a[axis] if side in ['west','down'] else b[axis]
                aa[axis]=bb[axis]=pos
                cube(f'{name}_edge_{px}_{py}_{side}',aa,bb,{side:{'texture':tex,'uv':uv}},rot,origin)
    anchors.append({'item':'minecraft:'+kind,'position':origin,'rotation':rot,'scale':size/16,'native_depth':1})
# Four readable diamonds and four fish: separate sizes/lean/height, above rim8.8.
for n,a in enumerate([(2.5,7.25,3.4,3.8,-22.5),(4.3,7.15,4.8,3.7,22.5),(2.8,8.25,6.2,3.9,-22.5),(4.5,8.65,7.1,3.5,22.5)]):
    sprite('diamond_'+str(n+1),'diamond',*a)
for n,a in enumerate([(6.5,7.1,3.0,4.2,-22.5),(6.2,7.65,4.6,4.5,22.5),(6.7,8.1,6.0,4.2,-22.5),(6.1,8.6,7.3,4.1,22.5)]):
    sprite('fish_'+str(n+1),'salmon' if n%2==0 else 'cod',*a)
print(json.dumps({'file':'shipping-station-20261020/shipping-station-native-r2.bbmodel','expected_revision':'a2582c8e6dfc9b72','operations':ops}))
(Path(__file__).parent/'goods-anchors.json').write_text(json.dumps({'schema':1,'status':'external-candidate-not-installed','units':'Blockbench model units,16 per block','display':'FIXED; native ItemRenderer with explicit anchor poses; no copy of preview sprite planes','items':anchors},ensure_ascii=False,indent=2)+'\n')
