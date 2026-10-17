import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
const root=path.dirname(fileURLToPath(import.meta.url));
const sdk='C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin';
const {Client}=await import(pathToFileURL(sdk+'/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js'));
const {StreamableHTTPClientTransport}=await import(pathToFileURL(sdk+'/node_modules/@modelcontextprotocol/sdk/dist/esm/client/streamableHttp.js'));
const c=new Client({name:'guardian-native-evidence',version:'1'});
await c.connect(new StreamableHTTPClientTransport(new URL('http://127.0.0.1:3000/bb-mcp')));
async function call(name,args){const r=await c.callTool({name,arguments:args});if(r.isError)throw Error(JSON.stringify(r));return r;}
try{
 await call('set_mode',{mode_id:'animate'});
 const views=await call('list_views',{});
 if(!JSON.stringify(views).includes('guardian_connected_review'))await call('create_offscreen_view',{id:'guardian_connected_review',width:768,height:640,antialias:false,copy_view:'none'});
 await call('set_camera_angle',{view:'guardian_connected_review',position:[115,85,135],target:[0,19,0],projection:'perspective',fov:40});
 await fs.mkdir(root+'/native-frames',{recursive:true});
 for(let i=0;i<=15;i++){
  await call('animation_timeline',{action:'set_time',animation_id:'guardian_shell',time:i*.1});
  const r=await call('capture_screenshot',{view:'guardian_connected_review',format:'png'});
  for(const block of r.content??[])if(block.type==='image')await fs.writeFile(root+'/native-frames/'+String(i).padStart(2,'0')+'.png',Buffer.from(block.data,'base64'));
 }
 await call('animation_timeline',{action:'set_time',animation_id:'guardian_shell',time:.55});
 await call('set_camera_angle',{view:'active',position:[115,85,135],target:[0,19,0],projection:'perspective',fov:40});
}finally{await c.close();}
