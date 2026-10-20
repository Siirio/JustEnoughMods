// Open the fresh preview in Blockbench first. Captures native nearest-textured
// views; this script never paints, evaluates editor code, or launches Minecraft.
import {Client} from 'file:///C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js';
import {StreamableHTTPClientTransport} from 'file:///C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin/node_modules/@modelcontextprotocol/sdk/dist/esm/client/streamableHttp.js';
import {writeFile} from 'node:fs/promises';
import {dirname,join} from 'node:path';import {fileURLToPath} from 'node:url';
const out=process.argv[2]??dirname(fileURLToPath(import.meta.url));
const client=new Client({name:'collector-native-material-review',version:'2.0.0'});
const createdViews=[];
try{
 await client.connect(new StreamableHTTPClientTransport(new URL('http://127.0.0.1:3000/bb-mcp')));
 for(const[name,position,locked]of [
  ['three-quarter',[-28,25,-32],null],['front',[0,8,-40],'north'],
  ['back',[0,8,40],'south'],['top',[0,44,0],'top']]){
  const id='collector_r2_'+name.replaceAll('-','_')+'_'+Date.now();
  const create=await client.callTool({name:'create_offscreen_view',arguments:{id,width:900,height:900,antialias:false,copy_view:'none'}});
  if(create.isError)throw Error(JSON.stringify(create.content));
  createdViews.push(id);
  const args={view:id,position,target:[0,8,0],projection:'orthographic',zoom:0.85};
  if(locked)args.locked_angle=locked;
  const camera=await client.callTool({name:'set_camera_angle',arguments:args});
  if(camera.isError)throw Error(JSON.stringify(camera.content));
  const r=await client.callTool({name:'capture_screenshot',arguments:{view:id,max_size:900,format:'png'}});
  const im=r.content.find(x=>x.type==='image');if(!im)throw Error('No image for '+name);
  await writeFile(join(out,'view-'+name+'.png'),Buffer.from(im.data,'base64'));
  console.log('Saved native Blockbench '+name);
 }
}finally{
 for(const view of createdViews)await client.callTool({name:'delete_offscreen_view',arguments:{view}});
 await client.close();
}
