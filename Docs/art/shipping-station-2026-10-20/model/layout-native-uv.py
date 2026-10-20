"""Produce face-sized UV islands and structured Blockbench edit operations.

No pixel authoring here: paint-native-materials.lua paints these labelled islands
in Aseprite. One source texel per model unit, rounded up with a one-pixel minimum
for fractional thin geometry. Never squash a complete16x16 swatch onto a rail.
"""
import json,math,sys
from pathlib import Path

root=Path(__file__).resolve().parent
model=json.loads((root/'shipping-station.bbmodel').read_text(encoding='utf-8'))
islands={};faces=[]
def material(name,side,w,h):
    if name.startswith(('front_board','back_board','left_board','right_board')):
        broad=side in (('north','south') if name.startswith(('front','back')) else ('east','west'))
        variant={'1.4':0,'4':1,'6.7':2}[name.rsplit('_',1)[1]]
        return ('wood_board' if broad else 'wood_top' if side=='up' else 'wood_under' if side=='down' else 'wood_end',variant if broad else 0)
    if name.startswith('corner_'):
        return ('wood_end' if side in ('up','down') else 'wood_post',0)
    if name in ('floor','supported_inner_cargo_tray'):
        return ('wood_floor' if side=='up' else 'wood_under' if side=='down' else 'wood_board',0)
    if name.endswith('_rail') or name=='mast_mount_board':
        return ('wood_top' if side=='up' else 'wood_under' if side=='down' else 'wood_end' if w<=2 else 'wood_board',1)
    if name.startswith('strap_'):return ('iron_strap',0)
    if name.startswith('pan_floor'):return ('iron_bowl' if side=='up' else 'iron_under',0)
    if name.startswith('pan_hanger'):return ('iron_hanger',0)
    if name.startswith('pan_'):return ('iron_rim_top' if side=='up' else 'iron_under' if side=='down' else 'iron_rim',0)
    if name in ('mast_foot','center_pivot') or name.startswith('beam_end'):
        return ('brass_top' if side=='up' else 'brass_under' if side=='down' else 'brass_body',0)
    return ('iron_top' if side=='up' else 'iron_under' if side=='down' else 'iron_structure',0)

for e in model['elements']:
    dx,dy,dz=[b-a for a,b in zip(e['from'],e['to'])]
    for side in ['north','south','east','west','up','down']:
        a,b=(dx,dy) if side in ('north','south') else (dz,dy) if side in ('east','west') else (dx,dz)
        w,h=max(1,math.ceil(a-1e-8)),max(1,math.ceil(b-1e-8))
        role,v=material(e['name'],side,w,h)
        key=f'{role}_{v}_{w}x{h}'
        islands.setdefault(key,{'name':key,'material':role,'variant':v,'width':w,'height':h,'users':[]})['users'].append(e['name']+'/'+side)
        faces.append({'node':e['name'],'side':side,'island':key,'model_size':[a,b],'texel_size':[w,h]})

# Pack largest islands first, deliberately reuse only the same part/material/size.
occupied=set()
for island in sorted(islands.values(),key=lambda i:(-i['height'],-i['width'],i['name'])):
    w,h=island['width']+2,island['height']+2
    pos=next(((x,y) for y in range(64-h+1) for x in range(64-w+1)
              if all((xx,yy) not in occupied for yy in range(y,y+h) for xx in range(x,x+w))),None)
    if pos is None:raise RuntimeError('Actual island layout does not fit64x64; revise explicit atlas contract.')
    x,y=pos;island['x']=x+1;island['y']=y+1
    occupied.update((xx,yy) for yy in range(y,y+h) for xx in range(x,x+w))

layout={'schema':1,'atlas':[64,64],'density':'1texel/model unit, ceil with min1 on fractional thin geometry','gutter':1,'islands':list(islands.values()),'faces':faces}
(root/'native-uv-layout.json').write_text(json.dumps(layout,indent=2)+'\n',encoding='utf-8')
ops={}
for f in faces:
    i=islands[f['island']];x,y,w,h=i['x'],i['y'],i['width'],i['height']
    ops.setdefault(f['node'],{})[f['side']]={'texture':model['textures'][0]['uuid'],'uv':[x/4,y/4,(x+w)/4,(y+h)/4]}
(root/'native-uv-operations.json').write_text(json.dumps([{'op':'update_node','target':n,'faces':f} for n,f in ops.items()],indent=2)+'\n',encoding='utf-8')
print(json.dumps({'islands':len(islands),'faces':len(faces),'canvas':[64,64],'packed_cells_including_gutters':len(occupied)}))
