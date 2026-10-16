// Native Blockbench geometry/UV/clip authoring. Painting is external and layered.
import {Client} from 'file:///C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js';
import {StreamableHTTPClientTransport} from 'file:///C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin/node_modules/@modelcontextprotocol/sdk/dist/esm/client/streamableHttp.js';
import {mkdir,writeFile,readFile} from 'node:fs/promises';
import {createHash} from 'node:crypto';
const client=new Client({name:'jem-selected-c-a-r5',version:'5.0.0'});
await client.connect(new StreamableHTTPClientTransport(new URL('http://127.0.0.1:3000/bb-mcp')));
async function call(name,args){const r=await client.callTool({name,arguments:args},undefined,{timeout:180000});if(r.isError)throw new Error(name+': '+JSON.stringify(r.content));return r;}
const root=new URL('.',import.meta.url);
let race,folder,parts,bones,restRotations;
async function group(name,parent='cluster',origin=[0,0,0],rotation=[0,0,0]){restRotations[name]=rotation;await call('add_group',{name,parent,origin,rotation});}
async function box(parent,material,name,from,to,rotation=[0,0,0],origin=from,top=material){
 await call('place_cube',{group:parent,texture:'r5_materials',elements:[{name,from,to,origin,rotation}],faces:true});
 const size=to.map((n,i)=>n-from[i]);
 const uv=(w,h,m,row,turn=0)=>{
   w=Math.min(16,Math.max(1,Math.round(w)));h=Math.min(16,Math.max(1,Math.round(h)));
   // Sample named face regions at stable native density, including both ends
   // of the painted swatch rather than mapping every face to one corner.
   const x=turn?(16-w):Math.floor((16-w)/2),y=Math.floor((16-h)/2);
   return [m*16+x,row*16+y,m*16+x+w,row*16+y+h];
 };
 const [x,y,z]=size;
 const texture=m=>m===2&&['nether','void','sculk'].includes(race)?'r5_core_emissive':'r5_materials';
 await call('set_cube_uv',{id:name,box_uv:false,faces:{north:{uv:uv(x,y,material,1),texture:texture(material)},south:{uv:uv(x,y,material,1,1),texture:texture(material)},west:{uv:uv(z,y,material,1),texture:texture(material)},east:{uv:uv(z,y,material,1,1),texture:texture(material)},up:{uv:uv(x,z,top,0),texture:texture(top)},down:{uv:uv(x,z,material,2),texture:texture(material)}}});
 parts.push({name,material:['substrate','functional plate','core'][material],topMaterial:top,from,to,rotation,origin,parent});
}
const bloomRise=[{time:0,position:[0,-38,0]},{time:.12,position:[0,-38,0]},{time:.64,position:[0,.6,0]},{time:.82,position:[0,0,0]},{time:1.50,position:[0,0,0]},{time:2.2,position:[0,-38,0]}];
async function foundation(radius=10){
 if(race==='bloom'){
   await box('cluster',0,'shared_root_knot',[-4,-2,-4],[4,3,4],[0,11,0],[0,0,0]);
   await box('cluster',0,'buried_cross_root',[-7,-1,-2],[7,1.6,2],[0,-17,5],[0,0,0]);
   return;
 }
 if(race==='nether'){
   await box('cluster',0,'shared_magma_crust_knot',[-5,-2,-5],[5,4,5],[0,11,0],[0,0,0]);
   await box('cluster',0,'fractured_ground_crust',[-9,-1,-5],[7,1.8,6],[3,-18,-4],[0,0,0]);
   await box('cluster',1,'short_side_crust',[-6,-1,-8],[5,2.2,6],[-5,23,3],[0,0,0]);
   return;
 }
 await box('cluster',0,'shared_earth_anchor',[-radius,-2,-radius*.7],[radius,2,radius*.7],[0,8,0],[0,0,0]);
 await box('cluster',0,'cross_ground_mass',[-radius*.7,-1.6,-radius],[radius*.7,2.8,radius],[0,-11,0],[0,0,0]);
}
async function nether(){
 await foundation(10);
 for(const [i,yaw] of [10,82,157,231,310].entries()){
   const id='eruption_'+i,h=[20,27,22,17,24][i],w=[6,7,6,5,6][i];
   await group(id,'cluster',[0,0,0],[0,yaw,0]);
   await box(id,0,id+'_buried_heel',[-w/2,-1,1],[w/2,6,9],[7,0,i-2],[0,0,3]);
   await group(id+'_lean',id,[0,4,4],[20+i*2,0,i%2?5:-4]);
   await box(id+'_lean',0,id+'_basalt_body',[-w/2,2,1],[w/2,14,7],[0,0,0],[0,4,4]);
   await box(id+'_lean',1,id+'_horizontal_crust',[-w/2-1.5,6,3.6],[.9,8,8.1],[7,13,-9],[-w/2,7,5]);
   await group(id+'_head',id+'_lean',[0,12,4],[11,7,i%2?-8:7]);
   await box(id+'_head',0,id+'_outward_head',[-w/2+.5,11,1.3],[w/2-.5,h,6.7],[0,0,0],[0,12,4],2);
   if(i===1||i===3||i===4)await box(id+'_head',0,id+'_broken_shoulder',[-w/2-.8,10,2],[w/2+1.7,15,7.4],[8,17,i%2?-13:11],[0,12,4]);
   await box(id+'_head',0,id+'_unequal_brow',[-w/2-.4,h-2.2,1],[.7,h+.8,4.2],[-7,-9,6],[-w/2,h-2,2]);
   if(i===0||i===3)await box(id+'_head',2,id+'_exposed_hot_seam',[-.6,h-5.5,.94],[1.1,h-1.4,1.35],[0,0,-7],[0,h-4,1.2]);
   if(i===1||i===4)await box(id+'_lean',1,id+'_attached_secondary_growth',[w/2-.8,9,2],[w/2+2.5,13.4,5.5],[13,8,-24],[w/2,10,4],2);
   bones[id+'_lean']=[{time:0,rotation:[7,0,0]},{time:.27+i*.025,rotation:[7,0,0]},{time:.65+i*.025,rotation:[22+i*2,0,i%2?5:-4]},{time:.9,rotation:[20+i*2,0,i%2?5:-4]},{time:1.5,rotation:[20+i*2,0,i%2?5:-4]},{time:2.2,rotation:[7,0,0]}];
 }
}
async function ancestors(){
 await foundation(9);
 await box('cluster',1,'bronze_ground_bridge',[-9,1,-3],[9,3.4,3],[0,0,-4],[0,2,0]);
 for(const side of [-1,1]){
   const id=side<0?'left_guard':'right_guard';
   await group(id,'cluster',[side*5,2,0],[0,side*12,-side*13]);
   await box(id,0,id+'_rooted_pier',[side*5-3,0,-3],[side*5+3,12,3]);
   await box(id,1,id+'_lower_brace',[side*5-3.8,5,-3.5],[side*5+3.1,7,3.7],[0,side*9,side*5],[side*5,6,0]);
   await group(id+'_shoulder',id,[side*5,10,0],[8,side*8,-side*9]);
   await box(id+'_shoulder',0,id+'_palm',[side*5-4,9,-3.2],[side*5+4,18,3.4]);
   await box(id+'_shoulder',1,id+'_horizontal_pauldron',[side*5-5,13,-3.8],[side*5+4.5,15.2,4.1],[9,-side*11,side*7],[side*5,14,0]);
   for(let finger=0;finger<3;finger++){
      const x=side*5+(finger-1)*2.6,y=[22,25,23][finger];
      await box(id+'_shoulder',0,id+'_finger_'+finger,[x-1.15,16.5,-2.3],[x+1.15,y,2.4],[6+finger*3,0,(finger-1)*-7],[x,17,0]);
   }
   await box(id+'_shoulder',0,id+'_opposed_thumb',[side*5-side*4.5-1.5,9,-.4],[side*5-side*4.5+1.5,15,3.4],[17,side*11,side*27],[side*5-side*3.3,10,1]);
   await box(id+'_shoulder',2,id+'_amber_inlay',[side*5-1,13,-3.55],[side*5+1,16,-3.15]);
   if(side===1)await box(id,1,'single_bronze_repair',[side*5+2.6,8,-2],[side*5+4.5,12,1.8],[0,10,-13],[side*5+3,9,0]);
   bones[id+'_shoulder']=[{time:0,rotation:[18,side*8,side*9]},{time:.3,rotation:[18,side*8,side*9]},{time:.75,rotation:[8,side*8,-side*9]},{time:1.5,rotation:[8,side*8,-side*9]},{time:2.2,rotation:[18,side*8,side*9]}];
 }
}
async function voidRift(){
 await foundation(8);
 for(const side of [-1,1]){
   const id=side<0?'left_tear':'right_tear';await group(id,'cluster',[side*3,2,0],[4,side*15,-side*18]);
   await box(id,0,id+'_root', [side*3-3.5,-1,-2.9],[side*3+3.5,10,3]);
   const height=side<0?25:20;
   await box(id,0,id+'_upper_lip',[side*3-3.6,8,-2.3],[side*3+3, height,2.4],[5,side*9,side*10],[side*3,9,0],1);
   await box(id,1,id+'_lateral_crystal_shelf',[side*3-3.6,7,-2.6],[side*3+.5,9,3.2],[8,side*17,side*11],[side*3,8,0]);
   await box(id,2,id+'_attached_inner_tear',[side*3-side*2.2-.6,4,-3],[side*3-side*2.2+.6,11,-2.5],[0,0,side*8],[side*3,6,0]);
   await box(id,1,id+'_jagged_inner_blade',[side*3-2.8,12,-2.8],[side*3+2.8,16,2.6],[7,0,side*22],[side*3,13,0]);
   await box(id,0,id+'_broken_outer_step',[side*3+side*2-2.6,14,-2],[side*3+side*2+2.6,height-1,2.2],[7,side*6,-side*12],[side*3,15,0],2);
   if(side===-1)await box(id,1,'attached_crystal_offshoot',[side*3-4.5,12,-1.9],[side*3-1.2,16.5,1.9],[0,-8,24],[side*3-2.3,12,0],2);
   bones[id]=[{time:0,rotation:[4,side*15,side*3]},{time:.24,rotation:[4,side*15,side*3]},{time:.5,rotation:[4,side*15,-side*22]},{time:.7,rotation:[4,side*15,-side*18]},{time:1.45,rotation:[4,side*15,-side*18]},{time:2.2,rotation:[4,side*15,side*3]}];
 }
}
async function sculk(){
 await foundation(10);
 await box('cluster',0,'front_connected_vein',[-4,-.4,6],[4,1.5,18],[0,-7,0],[0,0,8]);
 await box('cluster',2,'vein_colony',[-1.7,1.3,8],[1.7,1.8,14],[0,-7,0],[0,0,8]);
 for(const [i,yaw]of[35,128,219,307].entries()){
   const id='horn_'+i;await group(id,'cluster',[0,0,0],[0,yaw,0]);
   await box(id,0,id+'_connected_socket',[-3.8,0,3],[3.8,5,9],[0,0,i-2],[0,2,5]);
   await group(id+'_flare',id,[0,3,6],[22+i*3,0,i%2?7:-6]);
   await box(id+'_flare',1,id+'_ivory_base',[-2.7,2,3.5],[2.7,11,8.5]);
   await box(id+'_flare',0,id+'_skin_collar',[-3.2,3,3.1],[3.4,5.4,9],[6,11,-7],[0,4,6]);
   await box(id+'_flare',1,id+'_outward_tip',[-1.6,9,4.5],[1.6,17+(i%2),7.5],[13,0,i%2?-9:8],[0,10,6]);
   await box(id+'_flare',2,id+'_root_colony',[-1.4,4,3.2],[1.4,7,3.65]);
   if(i===2)await box(id+'_flare',1,'one_attached_horn_growth',[1.4,8,5],[3.9,11,7.4],[12,0,-25],[2,8,6]);
   bones[id+'_flare']=[{time:0,rotation:[3,0,0]},{time:.3+i*.04,rotation:[3,0,0]},{time:.85+i*.02,rotation:[22+i*3,0,i%2?7:-6]},{time:1.5,rotation:[22+i*3,0,i%2?7:-6]},{time:2.2,rotation:[3,0,0]}];
 }
}
async function bloom(){
 await foundation(8);
 for(const [i,yaw]of[12,86,158,232,309].entries()){
   const id='root_'+i,h=[18,22,20,17,23][i];await group(id,'cluster',[0,0,0],[0,yaw,0]);
   await box(id,0,id+'_root_heel',[-2.5,-1,2],[2.5,5,9],[7,0,i-2],[0,2,5]);
   await group(id+'_branch',id,[0,4,5],[17+i*2,0,i%2?5:-4]);
   await box(id+'_branch',0,id+'_branch_stem',[-1.8,3,3.4],[1.8,14,6.6]);
   await box(id+'_branch',0,id+'_angled_tip',[-1.2,12,4],[1.2,h,6],[13,6,i%2?-12:11],[0,13,5]);
   await box(id+'_branch',1,id+'_moss_collar',[-2.2,4.3,3.1],[2.4,6.3,7],[9,11,-8],[0,5,5]);
   await group(id+'_crown',id+'_branch',[0,12,5],[0,0,0]);
   await box(id+'_crown',1,id+'_left_leaf',[-6,11.5,2.7],[1.2,14,7.6],[0,-8,16],[-1,12,5]);
   await box(id+'_crown',1,id+'_right_leaf',[-.8,13,3.1],[5.3,15.5,7.8],[8,14,-21],[.6,13.4,5]);
   await box(id+'_crown',1,id+'_short_leaf_end',[-6.7,12.4,3.3],[-3.4,14.3,6.8],[6,-13,22],[-4,12.6,5]);
   if(i===0||i===3)await box(id+'_branch',0,id+'_branch_knot',[.9,7,3.7],[3.3,10,6.2],[13,9,-29],[1.5,8,5]);
   if(i===1||i===4){
      await box(id+'_branch',2,id+'_flower_bud',[-1.8,h-2.5,3.5],[1.8,h+.8,6.8],[11,0,-9],[0,h-2,5]);
      await box(id+'_branch',1,id+'_sepal',[-2.1,h-3.2,3.2],[2,h-1.9,7],[7,12,6],[0,h-2,5]);
   }
   bones[id+'_branch']=[{time:0,rotation:[4,0,0]},{time:.18+i*.04,rotation:[4,0,0]},{time:.72+i*.025,rotation:[17+i*2,0,i%2?5:-4]},{time:1.5,rotation:[17+i*2,0,i%2?5:-4]},{time:2.2,rotation:[4,0,0]}];
   bones[id+'_crown']=[{time:0,scale:[.1,.1,.1]},{time:.45+i*.04,scale:[.1,.1,.1]},{time:.95,scale:[1,1,1]},{time:1.5,scale:[1,1,1]},{time:2.2,scale:[.1,.1,.1]}];
 }
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
 const target=[0,race==='sculk'?7:11,0],cameras={hero:[54,37,64],front:[0,23,85],side:[85,23,0],back:[0,23,-85]};
 await call('animation_timeline',{action:'set_time',time:1.1});
 for(const [name,position]of Object.entries(cameras))await image(name+'.png',await call('set_camera_angle',{view,position,target,projection:'perspective',fov:35}));
 await call('set_camera_angle',{view,position:cameras.hero,target,projection:'perspective',fov:35});
 await mkdir(new URL('Frames/',folder),{recursive:true});
 for(let frame=0;frame<=22;frame++){
   await call('animation_timeline',{action:'set_time',time:frame/10});
   await image('Frames/'+String(frame).padStart(3,'0')+'.png',await call('capture_screenshot',{view,format:'png'}));
 }
 await call('animation_timeline',{action:'set_time',time:1.1});
 await writeFile(new URL('parts.json',folder),JSON.stringify({race,selectedGeometry:'M-C',selectedPainting:'T-A',nativeCubes:parts.length,quads:parts.length*6,materials:3,parts,sourceSha256:createHash('sha256').update(model.content).digest('hex'),editor:'Blockbench5.2.2 native MCP',frames:'23 genuine10Hz editor captures; not runtime evidence',runtime:'not installed, no game launch'},null,2)+'\n');
 console.log(race+': '+parts.length+' cubes; native editable source, UVs, four views and23 moving frames');
}
try{
 const requested=process.argv.slice(2);
 for(const [id,build]of [['nether',nether],['ancestors',ancestors],['void',voidRift],['sculk',sculk],['bloom',bloom]]){
   if(requested.length&&!requested.includes(id))continue;
   race=id;folder=new URL(id+'/',root);parts=[];bones={cluster:bloomRise};restRotations={};
   await call('create_project',{name:'JEM '+id+' R5 selected C-A',format:'free'});
   const pixels=await readFile(new URL('materials.png',folder));
   await call('create_texture',{name:'r5_materials',data:'data:image/png;base64,'+pixels.toString('base64'),width:48,height:48,uv_width:48,uv_height:48});
   if(['nether','void','sculk'].includes(id))await call('create_texture',{name:'r5_core_emissive',data:'data:image/png;base64,'+pixels.toString('base64'),width:48,height:48,uv_width:48,uv_height:48,render_mode:'emissive'});
   await call('create_texture',{name:'preview_stage',width:16,height:16,fill_color:'#727b70'});
   await group('preview_stage_OMIT_FROM_RUNTIME','root');
   await call('place_cube',{group:'preview_stage_OMIT_FROM_RUNTIME',texture:'preview_stage',elements:[{name:'neutral_ground_plinth',from:[-128,-64,-128],to:[128,-.3,128]}],faces:true});
   await group('cluster','root');await build();
   // Native rotation keys are offsets from the group's rest rotation. The
   // construction brief above specifies desired absolute local angles.
   const nativeBones=Object.fromEntries(Object.entries(bones).map(([bone,keys])=>[bone,keys.map(key=>key.rotation?{...key,rotation:key.rotation.map((v,i)=>v-(restRotations[bone]?.[i]??0))}:key)]));
   await call('create_animation',{name:id+'_signature_r5',animation_length:2.2,loop:false,bones:nativeBones});
   await call('set_mode',{mode_id:'animate'});await call('animation_timeline',{action:'set_fps',fps:20});
   await saveAndReview();
 }
}finally{await client.close();}
