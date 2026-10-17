// Native posed OBJ export only. External painted textures remain byte-identical.
import fs from 'node:fs/promises';
import path from 'node:path';
import {pathToFileURL,fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
const here=path.dirname(fileURLToPath(import.meta.url));
const art='C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench';
const sdk=art+'/mcp/blockbench-mcp-plugin';
const {Client}=await import(pathToFileURL(sdk+'/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js'));
const {StdioClientTransport}=await import(pathToFileURL(sdk+'/node_modules/@modelcontextprotocol/sdk/dist/esm/client/stdio.js'));
const {StreamableHTTPClientTransport}=await import(pathToFileURL(sdk+'/node_modules/@modelcontextprotocol/sdk/dist/esm/client/streamableHttp.js'));
const output=path.join(here,'src/main/resources/assets/jem_visual_sandbox/workbench');
await fs.mkdir(output,{recursive:true});
const proof=art+'/models/proofs/Art-engine-20261017';
const entries=process.argv[2]?JSON.parse(await fs.readFile(process.argv[2],'utf8')):[['shield',proof+'/ancestor-shield/ancestor-shield-r7.bbmodel'],['coffer',proof+'/bloom-coffer/bloom-coffer-r4.bbmodel'],['sculk',proof+'/sculk-crawl/sculk-crawl-r4.bbmodel'],...['void','nether'].map(id=>[id,path.resolve(here,'../../Docs/races/assets/race_vfx_r6/'+id+'/signature.bbmodel')])];
const d=new Client({name:'sandbox-workbench-bake',version:'1'}),h=new Client({name:'sandbox-workbench-files',version:'1'});
const receipt=[];
async function call(c,name,args){const r=await c.callTool({name,arguments:args});if(r.isError)throw Error(JSON.stringify(r));let v=r.structuredContent;if(!v){const t=r.content?.find(c=>c.type==='text');if(t){try{v=JSON.parse(t.text)}catch{v={text:t.text}}}else v={};}return v;}
function obj(text){let object='',material='';const v=[],uv=[],faces=[];for(const line of text.split('\n')){const[k,...a]=line.trim().split(/\s+/);if(k==='o')object=a.join(' ');if(k==='usemtl')material=a[0];if(k==='v')v.push(a.map(Number));if(k==='vt')uv.push(a.map(Number));if(k==='f'&&!/preview_stage|neutral_ground_plinth/.test(object)){if(a.length!==4)throw Error('Non-quad');const p=a.map(x=>x.split('/').map(Number));faces.push({object,material,uv:p.flatMap(i=>[uv[i[1]-1][0],1-uv[i[1]-1][1]]),positions:p.flatMap(i=>v[i[0]-1])});}}return faces;}
await h.connect(new StdioClientTransport({command:process.execPath,args:[sdk+'/bin/blockbench-mcp-headless.mjs','--root','C:/MyProjects','--node',art+'/mcp/runtime/node-v24.22.0-win-x64/node.exe','--render-home',art+'/mcp/runtime/render'],stderr:'inherit'}));
await d.connect(new StreamableHTTPClientTransport(new URL('http://127.0.0.1:3000/bb-mcp')));
try{
 for(const[id,file]of entries){
  const source=await fs.readFile(file);const model=JSON.parse(source);
  const info=await call(h,'bbmodel_info',{file});
  await call(h,'blockbench_launch',{file,wait_for_mcp_ms:4000});
  let ready=false;
  for(let n=0;n<25;n++){const live=await call(d,'export_model',{codec_id:'project',max_content_length:2000000});const current=JSON.parse(live.content);if(current.elements?.[0]?.uuid===model.elements[0].uuid){ready=true;break;}
   // A regenerated task file may still have its previous saved tab open.
   // Close only the known same-named task model, then reopen its canonical file.
   if(n===2 && current.name && current.name===model.name){await call(d,'trigger_action',{action:'close_project'});await call(h,'blockbench_launch',{file,wait_for_mcp_ms:4000});}
   await new Promise(r=>setTimeout(r,200));}
  if(!ready)throw Error('Wrong native tab '+id);
  await call(d,'set_mode',{mode_id:'animate'});
  const clip=model.animations[0],count=Math.round(clip.length*20)+1,frames=[];let faces;
  const materials=model.textures.filter(t=>t.name!=='preview_stage');
  for(let i=0;i<count;i++){
   await call(d,'animation_timeline',{action:'set_time',animation_id:clip.name,time:i/20});
   const exp=await call(d,'export_model',{codec_id:'obj',max_content_length:2000000,options:{scale:16}});
   if(exp.truncated)throw Error('Truncated OBJ');const posed=obj(exp.content);
   if(!faces)faces=posed;
   if(posed.length!==faces.length||posed.some((f,n)=>f.object!==faces[n].object||f.material!==faces[n].material||JSON.stringify(f.uv)!==JSON.stringify(faces[n].uv)))throw Error('Animated topology changed');
   frames.push(posed.flatMap(f=>f.positions));
  }
  const bytes=Buffer.alloc(16+faces.length*33+frames.length*faces.length*48);let p=0;
  const int=v=>{bytes.writeInt32BE(v,p);p+=4;},float=v=>{if(!Number.isFinite(v))throw Error('Non-finite');bytes.writeFloatBE(v,p);p+=4;};
  int(0x4a523301);int(count);int(faces.length);float(.05);
  for(const f of faces){const m=materials.findIndex(t=>'m_'+t.uuid===f.material);if(m<0)throw Error('Unknown material '+f.material);bytes[p++]=m;f.uv.forEach(float);}frames.forEach(f=>f.forEach(float));
  await fs.writeFile(path.join(output,id+'.mesh'),bytes);
  for(let i=0;i<materials.length;i++){const t=materials[i];if(!t.source?.startsWith('data:image/png;base64,'))throw Error('Texture not embedded');await fs.writeFile(path.join(output,id+'-'+i+'.png'),Buffer.from(t.source.split(',')[1],'base64'));}
  // Capture actual posed native viewport for handoff, not a substitute renderer.
  await call(d,'animation_timeline',{action:'set_time',animation_id:clip.name,time:.55});
  const shot=await d.callTool({name:'capture_screenshot',arguments:{format:'png'}});
  await fs.mkdir(path.join(here,'build/workbench-evidence'),{recursive:true});
  for(const c of shot.content??[])if(c.type==='image')await fs.writeFile(path.join(here,'build/workbench-evidence',id+'.png'),Buffer.from(c.data,'base64'));
  const entry={id,source:file,sourceSha256:createHash('sha256').update(source).digest('hex'),meshSha256:createHash('sha256').update(bytes).digest('hex'),frames:count,quads:faces.length,materials:materials.length,seconds:clip.length,exporter:'Blockbench native OBJ scale16; 20Hz sampled poses',headlessInspected:!!info};receipt.push(entry);console.log(JSON.stringify(entry));
 }
 const old=JSON.parse(await fs.readFile(path.join(output,'provenance.json'),'utf8').catch(()=> '[]'));
 await fs.writeFile(path.join(output,'provenance.json'),JSON.stringify([...old.filter(e=>!receipt.some(n=>n.id===e.id)),...receipt],null,2)+'\n');
}finally{await d.close();await h.close();}
