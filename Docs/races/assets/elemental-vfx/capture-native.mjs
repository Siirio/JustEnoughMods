// Capture the actual open native vine project. Never synthesize model poses.
import fs from 'node:fs/promises';
import path from 'node:path';
import {pathToFileURL,fileURLToPath} from 'node:url';
const here=path.dirname(fileURLToPath(import.meta.url));
const sdk='C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin';
const {Client}=await import(pathToFileURL(sdk+'/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js'));
const {StreamableHTTPClientTransport}=await import(pathToFileURL(sdk+'/node_modules/@modelcontextprotocol/sdk/dist/esm/client/streamableHttp.js'));
const c=new Client({name:'native-root-review',version:'1'});
await c.connect(new StreamableHTTPClientTransport(new URL('http://127.0.0.1:3000/bb-mcp')));
async function call(name,args){const r=await c.callTool({name,arguments:args});if(r.isError)throw Error(JSON.stringify(r));return r;}
try {
 const live=await call('export_model',{codec_id:'project',max_content_length:2000000});
 const payload=live.structuredContent??JSON.parse(live.content.find(v=>v.type==='text').text);
 const model=JSON.parse(payload.content);
 const source=JSON.parse(await fs.readFile(path.join(here,'entity_vines.bbmodel'),'utf8'));
 if(model.elements[0].uuid!==source.elements[0].uuid)throw Error('Open the current entity_vines source in Blockbench first');
 await call('select_all_of_type',{type:'mesh',add_to_selection:false});
 await call('set_camera_angle',{position:[45,28,65],target:[0,15,0],projection:'orthographic',zoom:.30});
 await fs.mkdir(path.join(here,'native-frames'),{recursive:true});
 for(let i=0;i<25;i++){
  await call('animation_timeline',{action:'set_time',animation_id:'bind_and_release',time:i/20});
  const shot=await call('capture_screenshot',{format:'png',max_size:900});
  const image=shot.content.find(v=>v.type==='image');if(!image)throw Error('No native frame');
  await fs.writeFile(path.join(here,'native-frames',String(i).padStart(2,'0')+'.png'),Buffer.from(image.data,'base64'));
 }
 await call('animation_timeline',{action:'set_time',animation_id:'bind_and_release',time:.5});
 console.log('25 actual Blockbench viewport frames saved. Editor evidence only.');
} finally {await c.close();}
