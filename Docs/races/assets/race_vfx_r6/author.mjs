// Native Blockbench geometry/UV/clip authoring. Painting is external and layered.
import {Client} from 'file:///C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js';
import {StreamableHTTPClientTransport} from 'file:///C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin/node_modules/@modelcontextprotocol/sdk/dist/esm/client/streamableHttp.js';
import {mkdir,writeFile,readFile} from 'node:fs/promises';
import {createHash} from 'node:crypto';
const client=new Client({name:'jem-race-specific-r6',version:'6.0.0'});
await client.connect(new StreamableHTTPClientTransport(new URL('http://127.0.0.1:3000/bb-mcp')));
async function call(name,args){const r=await client.callTool({name,arguments:args},undefined,{timeout:180000});if(r.isError)throw new Error(name+': '+JSON.stringify(r.content));return r;}
const root=new URL('.',import.meta.url);
let race,folder,parts,bones,restRotations,thermalSpan;
async function group(name,parent='cluster',origin=[0,0,0],rotation=[0,0,0]){restRotations[name]=rotation;await call('add_group',{name,parent,origin,rotation});}
async function box(parent,material,name,from,to,rotation=[0,0,0],origin=from,top=material){
 await call('place_cube',{group:parent,texture:'r5_materials',elements:[{name,from,to,origin,rotation}],faces:true});
 const size=to.map((n,i)=>n-from[i]);
 const uv=(w,h,m,row,turn=0)=>{
   w=Math.min(16,Math.max(1,Math.round(w)));h=Math.min(16,Math.max(1,Math.round(h)));
   // Sample named face regions at stable native density, including both ends
   // of the painted swatch rather than mapping every face to one corner.
   const x=turn?(16-w):Math.floor((16-w)/2),y=Math.floor((16-h)/2);
   if(race==='nether'&&row===1&&thermalSpan){
     const clamp=n=>Math.max(0,Math.min(1,n));
     return [m*16+x,16+(1-clamp(to[1]/thermalSpan))*16,m*16+x+w,16+(1-clamp(from[1]/thermalSpan))*16];
   }
   return [m*16+x,row*16+y,m*16+x+w,row*16+y+h];
 };
 const [x,y,z]=size;
 const texture=m=>race==='nether'&&m<2?'r6_thermal_exterior':m===2&&['nether','void','sculk'].includes(race)?'r5_core_emissive':'r5_materials';
 await call('set_cube_uv',{id:name,box_uv:false,faces:{north:{uv:uv(x,y,material,1),texture:texture(material)},south:{uv:uv(x,y,material,1,1),texture:texture(material)},west:{uv:uv(z,y,material,1),texture:texture(material)},east:{uv:uv(z,y,material,1,1),texture:texture(material)},up:{uv:uv(x,z,top,0),texture:texture(top)},down:{uv:uv(x,z,material,2),texture:texture(material)}}});
 parts.push({name,material:['substrate','functional plate','core'][material],topMaterial:top,from,to,rotation,origin,parent});
}
// Independent spatial compositions. No universal radial cluster constructor.
const rise=(height,delay=.1,impact=.65)=>[
 {time:0,position:[0,-height,0]},{time:delay,position:[0,-height,0]},
 {time:impact,position:[0,.5,0]},{time:impact+.14,position:[0,0,0]},
 {time:1.55,position:[0,0,0]},{time:2.2,position:[0,-height,0]}];
async function segment(parent,mat,name,a,b,width,depth=width,top=mat){
 const d=b.map((v,i)=>v-a[i]),length=Math.hypot(...d);
 const rotation=[Math.asin(d[2]/length)*180/Math.PI,0,Math.atan2(-d[0],d[1])*180/Math.PI];
 await box(parent,mat,name,[a[0]-width/2,a[1],a[2]-depth/2],[a[0]+width/2,a[1]+length,a[2]+depth/2],rotation,a,top);
}
async function nether(){
 // Three separately anchored strikes, not a five-way simultaneous flower.
 bones.cluster=[{time:0,position:[0,0,0]},{time:2.2,position:[0,0,0]}];
 const columns=[[-8,4,19,-11],[0,0,28,5],[8,-5,23,14]];
 for(const [i,[x,z,h,lean]]of columns.entries()){
  const id='strike_'+(i+1),t=[.18,.50,.82][i];thermalSpan=h;
  await group(id,'cluster',[x,0,z],[z*.8,0,-lean]);
  bones[id]=[{time:0,position:[0,-h-8,0]},{time:t-.08,position:[0,-h-8,0]},
    {time:t+.02,position:[0,1.2,0]},{time:t+.13,position:[0,0,0]},
    {time:1.48+i*.09,position:[0,0,0]},{time:2.05+i*.075,position:[0,-h-8,0]},{time:2.2,position:[0,-h-8,0]}];
  await box(id,0,id+'_buried_foot',[x-4.5,-2,z-4],[x+4.5,4,z+4],[0,12,0],[x,0,z]);
  await box(id,0,id+'_lower_crust',[x-3.4,2,z-3.2],[x+3.4,12,z+3.2],[0,-8,0],[x,2,z]);
  await box(id,0,id+'_upper_crust',[x-2.9,10,z-2.9],[x+2.9,h,z+2.9],[4,8,i%2?5:-5],[x,11,z],2);
  await box(id,1,id+'_local_cooling_shelf',[x-4.7,7,z+.2],[x+.5,9,z+3.7],[8,11,-10],[x-3,8,z+2]);
  await box(id,0,id+'_broken_lit_lip',[x-3.7,h-3,z-3.1],[x+.4,h+1,z+.5],[-8,-9,5],[x-2,h-2,z-1]);
  if(i!==1)await box(id,1,id+'_attached_growth',[x+2.3,11,z-.5],[x+5,15,z+2],[9,10,-24],[x+2.7,12,z],2);
  await box(id,2,id+'_hot_fissure',[x-.7,h-7,z-3.2],[x+.8,h-1,z-2.87],[0,0,-4],[x,h-6,z-3]);
 }
}
async function ancestors(){
 // Broad forward-facing overlap, inspired by a raised shield, not horns.
 bones.cluster=rise(27,.05,.55);
 await box('cluster',1,'low_connected_ward_foot',[-13,-1,-2],[13,2,3],[0,0,0],[0,0,0]);
 for(const [i,[x,y,h,w,yaw]]of [[-9,2,17,9,24],[0,1,24,11,0],[9,2,19,9,-24]].entries()){
  const id='shield_'+i;await group(id,'cluster',[x,y,0],[0,yaw,0]);
  await box(id,0,id+'_broad_stone_face',[x-w/2,y,-1],[x+w/2,y+h,2],[0,0,i===0?-3:i===2?3:0],[x,y,0]);
  await box(id,0,id+'_stepped_lower_tip',[x-w/2+1,y-1,-.8],[x+w/2-1,y+3,2.2]);
  await box(id,1,id+'_edge_reinforcement',[x-w/2-.2,y+2,-1.3],[x-w/2+1,y+h-1,2.4]);
  await box(id,1,id+'_fitted_cross_plate',[x-w/2+.4,y+h*.58,-1.6],[x+w/2-.5,y+h*.58+1.8,-.7],[0,0,i-1],[x,y+h*.58,0]);
  if(i===1){
   await box(id,1,'central_bronze_boss',[-2.5,10,-2.1],[2.5,15,-.8],[0,0,0],[0,12,0]);
   await box(id,2,'small_amber_ward_seal',[-1.1,11.3,-2.35],[1.1,13.7,-2]);
  }
  bones[id]=[{time:0,rotation:[0,i===0?65:i===2?-65:0,0]},{time:.22,rotation:[0,i===0?65:i===2?-65:0,0]},
   {time:.72,rotation:[0,yaw,0]},{time:1.5,rotation:[0,yaw,0]},{time:2.2,rotation:[0,i===0?65:i===2?-65:0,0]}];
 }
}
async function voidRift(){
 // Wide horizontal compression/separation; the gap is the focal shape.
 bones.cluster=[{time:0,scale:[.001,.001,.001]},{time:.16,scale:[1,.2,1]},{time:.4,scale:[1,1,1]},
  {time:1.5,scale:[1,1,1]},{time:1.95,scale:[1,.12,1]},{time:2.2,scale:[.001,.001,.001]}];
 await box('cluster',0,'lower_obsidian_jaw',[-13,0,-3],[12,4,3],[0,5,-3],[0,2,0]);
 await box('cluster',1,'lower_crystal_cut',[-10,3,-3.3],[8,4.4,-2.8],[0,0,-3],[0,3,0]);
 await box('cluster',0,'lower_broken_offset',[-9,1,-4],[4,3,-2.4],[0,-8,5],[0,2,-3]);
 await group('upper_jaw','cluster',[0,9,0],[0,-5,3]);
 await box('upper_jaw',0,'upper_obsidian_jaw',[-11,7,-2.7],[13,11,3.3]);
 await box('upper_jaw',1,'upper_inner_shelf',[-8,6.1,-3.1],[10,7.5,-2.6],[0,0,2],[0,7,0]);
 await box('upper_jaw',0,'upper_unequal_thickening',[-5,10,-2.4],[10,12.5,2.9],[0,8,-4],[0,10,0]);
 await box('cluster',0,'left_rear_hinge',[-12,2,1.7],[-9,12,3.8],[0,0,-9],[-11,3,2]);
 await box('cluster',0,'right_short_hinge',[9,2,1.8],[12,10,3.8],[0,0,9],[10,3,2]);
 await box('cluster',2,'inner_rift_focal_strip',[-7,4.4,2.7],[6,5.5,3.1],[0,0,-4],[0,5,3]);
 await box('upper_jaw',2,'offset_upper_rift_edge',[-3,5.9,2.5],[8,6.6,2.9],[0,0,4],[0,6,3]);
 bones.upper_jaw=[{time:0,position:[0,-5,0]},{time:.2,position:[0,-5,0]},{time:.47,position:[0,3.5,0]},
  {time:.67,position:[0,2.5,0]},{time:1.45,position:[0,2.5,0]},{time:1.88,position:[0,-5,0]},{time:2.2,position:[0,-5,0]}];
}
async function sculk(){
 // Connected dendritic floor network and a traveling activation front.
 bones.cluster=[{time:0,position:[0,0,0]},{time:2.2,position:[0,0,0]}];
 const paths=[
  [[0,0,14],[0,0,5],0],[[0,0,5],[-1,0,-4],.10],[[-1,0,-4],[1,0,-15],.20],
  [[0,0,5],[-9,0,0],.15],[[-9,0,0],[-14,0,-6],.27],[[-9,0,0],[-16,0,3],.31],
  [[-1,0,-4],[8,0,-8],.25],[[8,0,-8],[14,0,-14],.37],[[8,0,-8],[16,0,-5],.41]];
 for(const [i,[a,b,delay]]of paths.entries()){
  const id='vein_branch_'+i;await group(id,'cluster',a);
  // Low rectangular carriers are laid along the ground, not stood upright.
  const dx=b[0]-a[0],dz=b[2]-a[2],len=Math.hypot(dx,dz),yaw=Math.atan2(dx,dz)*180/Math.PI;
  await box(id,0,id+'_skin',[a[0]-2,-.2,a[2]],[a[0]+2,1.25,a[2]+len],[0,yaw,0],a);
  await box(id,2,id+'_luminous_channel',[a[0]-.65,1.2,a[2]+.3],[a[0]+.65,1.55,a[2]+len-.1],[0,yaw,0],a);
  if(i===1||i===3||i===6)await box(id,0,id+'_attached_nodule',[a[0]-2.6,.5,a[2]-.8],[a[0]+2.7,3.5,a[2]+2.8],[0,yaw-12,7],a);
  if(i===3||i===6)await box(id,1,id+'_ivory_rim',[a[0]-2.4,1.0,a[2]],[a[0]-1.5,2,a[2]+4],[0,yaw,0],a);
  bones[id]=[{time:0,scale:[.01,.01,.01]},{time:.08+delay,scale:[.01,.01,.01]},
   {time:.35+delay,scale:[1,1,1]},{time:1.45,scale:[1,1,1]},{time:1.85+delay*.5,scale:[.01,.01,.01]},{time:2.2,scale:[.01,.01,.01]}];
 }
}
async function bloom(){
 // Inward bower: empty enclosure below three converging woody arch ribs.
 bones.cluster=rise(30,.08,.67);
 const ribs=[
  [[-13,0,4],[-9,10,2],[-2,18,0],[2,20,0]],
  [[12,0,5],[9,9,3],[4,16,1],[0,19,0]],
  [[0,0,-11],[-2,11,-7],[0,19,-1]]];
 for(const [i,nodes]of ribs.entries()){
  const id='arch_rib_'+i;await group(id,'cluster',nodes[0]);
  for(let j=0;j<nodes.length-1;j++)await segment(id,0,id+'_wood_'+j,nodes[j],nodes[j+1],j===0?3.9:j===1?3:2.2,j===0?3.6:2.8);
  const [x,y,z]=nodes[1];
  await box(id,1,id+'_joint_moss',[x-2.1,y-2,z-2],[x+2.2,y+.4,z+2.1],[7,i*8,-8],[x,y,z]);
  bones[id]=[{time:0,rotation:[0,i===0?-9:i===1?9:0,0]},{time:.35,rotation:[0,i===0?-9:i===1?9:0,0]},
   {time:.9,rotation:[0,0,0]},{time:1.5,rotation:[0,0,0]},{time:2.2,rotation:[0,i===0?-9:i===1?9:0,0]}];
 }
 await group('leaf_canopy','cluster',[0,18,0]);
 await box('leaf_canopy',1,'left_overlapping_canopy',[-8,16,-3],[1,19,4],[5,-12,12],[-2,17,0]);
 await box('leaf_canopy',1,'right_overlapping_canopy',[-1,18,-2],[7,20.5,5],[-7,18,-14],[2,18,0]);
 await box('leaf_canopy',1,'back_short_leaf_layer',[-4,17,-6],[3,19.5,0],[12,7,-4],[0,18,-2]);
 await box('leaf_canopy',2,'one_pink_blossom',[-4.8,19,-1.5],[-1.8,21.4,1.5],[4,8,-7],[-3,19,0]);
 await box('leaf_canopy',2,'smaller_side_blossom',[3,18.7,1],[5.2,20.6,3.2],[-5,-9,11],[4,19,2]);
 bones.leaf_canopy=[{time:0,scale:[.08,.08,.08]},{time:.55,scale:[.08,.08,.08]},
  {time:1.05,scale:[1,1,1]},{time:1.5,scale:[1,1,1]},{time:2.2,scale:[.08,.08,.08]}];
}
async function image(file,r){const c=r.content.find(c=>c.type==='image');if(!c)throw new Error('No native capture');await writeFile(new URL(file,folder),Buffer.from(c.data,'base64'));}
async function saveAndReview(){
 const r=await call('export_model',{codec_id:'project',max_content_length:2000000});
 const model=JSON.parse(r.content.find(c=>c.type==='text').text);if(model.truncated)throw new Error('Truncated model');
 await writeFile(new URL('signature.bbmodel',folder),model.content);
 const views=await call('list_views',{});
 const view=JSON.stringify(views).includes('race_r5_review')?'race_r5_review':JSON.stringify(views).includes('race_r4_nether')?'race_r4_nether':'race_r5_review';
 if(!JSON.stringify(views).includes(view))await call('create_offscreen_view',{id:view,width:720,height:720,antialias:false,copy_view:'none'});
 await call('resize_offscreen_view',{view,width:720,height:720});
 const target=[0,race==='sculk'?1:11,0],cameras={hero:[54,37,-64],front:[0,23,-85],side:[85,23,0],back:[0,23,85]};
 await call('animation_timeline',{action:'set_time',time:1.1});
 for(const [name,position]of Object.entries(cameras))await image(name+'.png',await call('set_camera_angle',{view,position,target,projection:'perspective',fov:35}));
 await call('set_camera_angle',{view,position:cameras.hero,target,projection:'perspective',fov:35});
 await mkdir(new URL('Frames/',folder),{recursive:true});
 for(let frame=0;frame<=44;frame++){
   await call('animation_timeline',{action:'set_time',time:frame/20});
   await image('Frames/'+String(frame).padStart(3,'0')+'.png',await call('capture_screenshot',{view,format:'png'}));
 }
 await call('animation_timeline',{action:'set_time',time:1.1});
 await writeFile(new URL('parts.json',folder),JSON.stringify({race,selectedGeometry:'M-C',selectedPainting:'T-A',nativeCubes:parts.length,quads:parts.length*6,materials:3,parts,sourceSha256:createHash('sha256').update(model.content).digest('hex'),editor:'Blockbench5.2.2 native MCP',frames:'45 genuine20Hz editor captures; not runtime evidence',runtime:'not installed, no game launch'},null,2)+'\n');
 console.log(race+': '+parts.length+' cubes; native editable source, UVs, four views and45 moving frames');
}
try{
 const requested=process.argv.slice(2);
 for(const [id,build]of [['nether',nether],['ancestors',ancestors],['void',voidRift],['sculk',sculk],['bloom',bloom]]){
   if(requested.length&&!requested.includes(id))continue;
   race=id;folder=new URL(id+'/',root);parts=[];bones={};restRotations={};
   await call('create_project',{name:'JEM '+id+' R6 distinct pattern',format:'free'});
   const pixels=await readFile(new URL('materials.png',folder));
   await call('create_texture',{name:'r5_materials',data:'data:image/png;base64,'+pixels.toString('base64'),width:48,height:48,uv_width:48,uv_height:48});
   if(['nether','void','sculk'].includes(id))await call('create_texture',{name:'r5_core_emissive',data:'data:image/png;base64,'+pixels.toString('base64'),width:48,height:48,uv_width:48,uv_height:48,render_mode:'emissive'});
   if(id==='nether')await call('create_texture',{name:'r6_thermal_exterior',data:'data:image/png;base64,'+pixels.toString('base64'),width:48,height:48,uv_width:48,uv_height:48,render_mode:'emissive'});
   await call('create_texture',{name:'preview_stage',width:16,height:16,fill_color:'#727b70'});
   await group('preview_stage_OMIT_FROM_RUNTIME','root');
   await call('place_cube',{group:'preview_stage_OMIT_FROM_RUNTIME',texture:'preview_stage',elements:[{name:'neutral_ground_plinth',from:[-128,-64,-128],to:[128,-.3,128]}],faces:true});
   await group('cluster','root');await build();
   // Native rotation keys are offsets from the group's rest rotation. The
   // construction brief above specifies desired absolute local angles.
   const nativeBones=Object.fromEntries(Object.entries(bones).map(([bone,keys])=>[bone,keys.map(key=>key.rotation?{...key,rotation:key.rotation.map((v,i)=>v-(restRotations[bone]?.[i]??0))}:key)]));
   await call('create_animation',{name:id+'_signature_r6',animation_length:2.2,loop:false,bones:nativeBones});
   await call('set_mode',{mode_id:'animate'});await call('animation_timeline',{action:'set_fps',fps:20});
   await saveAndReview();
 }
}finally{await client.close();}
