// MCP-only geometry/rig/keyframe authoring. Texture PNGs are painted externally.
import fs from 'node:fs/promises';
import path from 'node:path';
import {pathToFileURL,fileURLToPath} from 'node:url';
const here=path.dirname(fileURLToPath(import.meta.url));
const art=path.resolve(here,'../../Docs/races/assets/elemental-vfx');
const home='C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench';
const sdk=home+'/mcp/blockbench-mcp-plugin';
const {Client}=await import(pathToFileURL(sdk+'/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js'));
const {StdioClientTransport}=await import(pathToFileURL(sdk+'/node_modules/@modelcontextprotocol/sdk/dist/esm/client/stdio.js'));
const c=new Client({name:'elemental-authored-models',version:'1'});
await c.connect(new StdioClientTransport({command:process.execPath,args:[sdk+'/bin/blockbench-mcp-headless.mjs','--root','C:/MyProjects','--node',home+'/mcp/runtime/node-v24.22.0-win-x64/node.exe','--render-home',home+'/mcp/runtime/render'],stderr:'inherit'}));
async function call(name,args){const r=await c.callTool({name,arguments:args});if(r.isError)throw Error(JSON.stringify(r));return r.structuredContent??r;}
const entries=[];
const faces=texture=>Object.fromEntries(['north','south','east','west','up','down'].map((f,i)=>[f,{uv:i===4?[0,0,16,16]:[i%2*8,8,i%2*8+16,32],texture}]));
function thermalFaces(piece,texture,height){
 const top=Math.max(0,31-piece.to[1]/height*31),bottom=Math.min(31,31-piece.from[1]/height*31);
 return Object.fromEntries(['north','south','east','west','up','down'].map((f,i)=>[f,{texture,uv:f==='up'?[0,top,16,Math.min(32,top+1)]:f==='down'?[0,bottom,16,Math.min(32,bottom+1)]:[i%2*8,top,i%2*8+16,bottom]}]));
}
try {
 for(let variant=0;variant<(process.argv.includes('--vines-only')?0:3);variant++){
  const name='eruption'+variant,file=path.join(art,name+'.bbmodel');
  await call('bbmodel_create',{file,format:'free',box_uv:false,name,resolution:{width:32,height:32},overwrite:true});
  const ops=[];
  for(const material of ['rock','molten'])ops.push({op:'add_texture',name:material,source:'data:image/png;base64,'+(await fs.readFile(path.join(art,material+'.png'))).toString('base64'),width:32,height:32});
  ops.push({op:'add_group',name:'eruption',origin:[0,0,0]});
  // Fitted unequal angular slabs, seated side plates and a fractured hot seam.
  const pieces=[
   {name:'main fractured basalt',from:[-5,0,-4],to:[4,20+variant*3,4],origin:[0,0,0],rotation:[-8-variant*3,variant*13,10-variant*8]},
   {name:'left support plate',from:[-11,0,-4],to:[-3,11+variant*2,5],origin:[-3,0,0],rotation:[7,-12,19]},
   {name:'right sheared plate',from:[3,0,-5],to:[10,14+variant,3],origin:[3,0,0],rotation:[-11,18,-17]},
   {name:'front basalt ledge',from:[-6,1,-8],to:[5,6,0],origin:[0,0,0],rotation:[-12,variant*11,6]},
   {name:'back broken crown',from:[-2,9,0],to:[5,22+variant*3,6],origin:[0,4,0],rotation:[12,-19,-10]},
   {name:'exposed molten split',from:[-1,1,-4.4],to:[1.6,18+variant*3,-3.7],origin:[0,0,0],rotation:[-8-variant*3,variant*13,10-variant*8],hot:true},
   {name:'molten basal shelf',from:[-7,.2,-6],to:[7,2,6],origin:[0,0,0],rotation:[0,variant*15,0],hot:true}
  ];
  for(const p of pieces){const hot=p.hot;delete p.hot;ops.push({op:'add_cube',...p,parent:'eruption',faces:thermalFaces(p,hot?'molten':'rock',22+variant*3)});}
  ops.push({op:'add_animation',name:'break_ground',length:1.2,loop:'once',snapping:20});
  for(const [time,value] of [[0,[.05,.02,.05]],[.10,[.8,.28,.8]],[.25,[1,1.08,1]],[.40,[1,1,1]],[.7,[1,1,1]],[1,[.8,.3,.8]],[1.2,[.01,.01,.01]]])ops.push({op:'set_keyframe',animation:'break_ground',bone:'eruption',channel:'scale',time,value,interpolation:'linear'});
  for(const [time,value] of [[0,[0,-3,0]],[.25,[0,0,0]],[.40,[0,0,0]],[1.2,[0,-5,0]]])ops.push({op:'set_keyframe',animation:'break_ground',bone:'eruption',channel:'position',time,value,interpolation:'linear'});
  await call('bbmodel_edit',{file,operations:ops});entries.push([name,file]);
 }
 if(!process.argv.includes('--rocks-only')) {
 const file=path.join(art,'entity_vines.bbmodel');
 await call('bbmodel_create',{file,format:'free',name:'entity_vines',box_uv:false,resolution:{width:32,height:32},overwrite:true});
 const ops=[{op:'add_texture',name:'root',source:'data:image/png;base64,'+(await fs.readFile(path.join(art,'root.png'))).toString('base64'),width:32,height:32},{op:'add_texture',name:'leaf',source:'data:image/png;base64,'+(await fs.readFile(path.join(art,'leaf.png'))).toString('base64'),width:32,height:32},{op:'add_group',name:'wrap',origin:[0,0,0]}];
 // Unequal climbing paths over a unit body; the runtime scales to the real box.
 // Short overlapping chords follow deliberately unequal rounded climbing paths.
 const controls=[[[0,0,-10],[-5,1,-9],[-8.5,3,-5],[-9,5,1],[-7.5,7,6],[0,10,9],[7,13,7],[9,16,0],[7,19,-7],[0,22,-9],[-7,25,-7],[-8.5,28,0],[-5,30,7]],[[7,0,8],[9,2,3],[8.5,4,-3],[4,6,-8],[-3,9,-9],[-8,12,-5],[-9,15,2],[-5,18,8],[2,21,9],[8,24,5],[9,27,-2],[5,29,-8],[-2,31,-9]]];
 const paths=controls; // Keep angular bends; do not subdivide into smoother tubing.
 const tracks=[];
 for(let p=0;p<paths.length;p++){
  const group='climbing_vine_'+p;ops.push({op:'add_group',name:group,origin:[0,0,0],parent:'wrap'});
  for(let i=0;i<paths[p].length-1;i++){
   const a=paths[p][i],b=paths[p][i+1],mid=a.map((v,k)=>(v+b[k])/2),dx=b[0]-a[0],dy=b[1]-a[1],dz=b[2]-a[2],len=Math.hypot(dx,dy,dz);
   const thickness=2.1-(i/(paths[p].length-2))*1.65;
   const bone='wrapping_tip_'+p+'_'+i,start=p*.08+i*.05,finish=start+.05;
   ops.push({op:'add_group',name:bone,parent:group,origin:a,rotation:[Math.atan2(dz,Math.hypot(dx,dy))*180/Math.PI,0,-Math.atan2(dx,dy)*180/Math.PI]});
   ops.push({op:'add_cube',name:'brown_tapered_root_'+p+'_'+i,from:[a[0]-thickness,a[1],a[2]-thickness],to:[a[0]+thickness,a[1]+len+.22,a[2]+thickness],origin:a,parent:bone,faces:faces('root')});
   tracks.push({bone,start,finish,leaf:false,branch:p,segment:i});
   if(i%2===1){
    const leafBone='leaf_unfold_'+p+'_'+i;
    ops.push({op:'add_group',name:leafBone,parent:group,origin:b});
    ops.push({op:'add_cube',name:'pointed_green_leaf_'+p+'_'+i,from:[b[0]-.8,b[1],b[2]-.15],to:[b[0]+3.5,b[1]+5,b[2]+.15],origin:b,rotation:[p?25:-20,45,i%3*20-25],parent:leafBone,faces:Object.fromEntries(['north','south','east','west','up','down'].map(f=>[f,{uv:[0,0,32,32],texture:['north','south'].includes(f)?'leaf':null}]))});
    tracks.push({bone:leafBone,start:finish,finish:finish+.065,leaf:true,branch:p,segment:i});
   }
  }
 }
 ops.push({op:'add_animation',name:'bind_and_release',length:1.2,loop:'once',snapping:20});
 // Each branch grows around the body from segment to segment, with independent
 // timing, curl/settle and leaf unfolding. Never scale the whole coil as one prop.
 for(const t of tracks){
  const tiny=[.0001,.0001,.0001],release=1.05-t.segment*.004-t.branch*.015;
  for(const [time,value] of [[0,tiny],...(t.start>0?[[t.start,tiny]]:[]),[t.finish,[1,1,1]],[release,[1,1,1]],[release+.16,tiny],[1.2,tiny]])ops.push({op:'set_keyframe',animation:'bind_and_release',bone:t.bone,channel:'scale',time,value,interpolation:'linear'});
  const curl=(t.branch?-1:1)*(t.leaf?42:18+Math.min(10,t.segment*.5));
  for(const [time,value] of [[0,[0,0,curl]],[t.finish,[0,0,0]],[release,[0,0,0]],[1.2,[0,0,-curl/2]]])ops.push({op:'set_keyframe',animation:'bind_and_release',bone:t.bone,channel:'rotation',time,value,interpolation:'linear'});
 }
 for(let offset=0;offset<ops.length;offset+=300)await call('bbmodel_edit',{file,operations:ops.slice(offset,offset+300)});entries.push(['entity_vines',file]);
 }
 await fs.writeFile(path.join(art,'model-entries.json'),JSON.stringify(entries,null,2)+'\n');
 console.log(JSON.stringify({models:entries.map(e=>e[0]),source:'MCP headless geometry, UVs and native keyframes; external ORA textures'},null,2));
} finally {await c.close();}
