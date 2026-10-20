// Structured Blockbench MCP geometry authoring. No eval and no raster operations.
import {Client} from 'file:///C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js';
import {StreamableHTTPClientTransport} from 'file:///C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin/node_modules/@modelcontextprotocol/sdk/dist/esm/client/streamableHttp.js';
import {writeFile,mkdir} from 'node:fs/promises';
import {dirname} from 'node:path';import{fileURLToPath}from'node:url';
const root=dirname(fileURLToPath(import.meta.url)).replaceAll('\\','/');
const workspace='C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/models/shipping-station-20261020';
await mkdir(workspace,{recursive:true});
const groups={crate:[],iron:[],brass:[],pans:[]};
function box(group,name,from,to){groups[group].push({name,from,to,origin:[8,8,8]});}
box('crate','floor',[1,0,1],[15,1.4,15]);
for(const x of [1,13.6])for(const z of [1,13.6])box('crate',`corner_${x}_${z}`,[x,0,z],[x+1.4,8.4,z+1.4]);
for(const[y,h]of [[1.4,1.8],[4,1.6],[6.7,1.7]]){
 box('crate',`front_board_${y}`,[2.4,y,1.2],[13.6,y+h,2.25]);
 box('crate',`back_board_${y}`,[2.4,y,13.75],[13.6,y+h,14.8]);
 box('crate',`left_board_${y}`,[1.2,y,2.4],[2.25,y+h,13.6]);
 box('crate',`right_board_${y}`,[13.75,y,2.4],[14.8,y+h,13.6]);
}
for(const x of [0.92,14.46])for(const z of [0.92,13.65])box('iron',`strap_${x}_${z}`,[x,0.7,z],[x+.62,7.9,z+1.43]);
box('crate','front_rail',[.8,8.1,.8],[15.2,8.8,2.45]);
box('crate','back_rail',[.8,8.1,13.55],[15.2,8.8,15.2]);
box('crate','left_rail',[.8,8.1,2.45],[2.45,8.8,13.55]);
box('crate','right_rail',[13.55,8.1,2.45],[15.2,8.8,13.55]);
box('iron','mast_floor_support',[7.35,1.4,11],[8.65,15.1,12.3]);
box('crate','mast_mount_board',[6.5,7.3,10.4],[9.5,8.2,13.75]);
box('brass','mast_foot',[6.8,8.2,10.65],[9.2,9,12.8]);
box('iron','balance_beam',[.8,14.85,10.95],[15.2,15.65,12.35]);
box('brass','center_pivot',[7,14.55,10.75],[9,16,12.55]);
for(const x of [1.2,13.8])box('brass',`beam_end_${x}`,[x,14.65,10.75],[x+1,15.85,12.55]);
for(const x of [1,10.5]){
 box('pans',`pan_floor_${x}`,[x,10.5,9.25],[x+4.5,10.85,13.75]);
 box('iron',`pan_front_${x}`,[x,10.85,9.25],[x+4.5,11.65,9.65]);
 box('iron',`pan_back_${x}`,[x,10.85,13.35],[x+4.5,11.65,13.75]);
 box('iron',`pan_left_${x}`,[x,10.85,9.65],[x+.4,11.65,13.35]);
 box('iron',`pan_right_${x}`,[x+4.1,10.85,9.65],[x+4.5,11.65,13.35]);
 for(const dx of [.4,3.8])box('iron',`pan_hanger_${x}_${dx}`,[x+dx,11.65,11.2],[x+dx+.3,14.85,11.5]);
}
const client=new Client({name:'jem-shipping-model-author',version:'1.0.0'});
await client.connect(new StreamableHTTPClientTransport(new URL('http://127.0.0.1:3000/bb-mcp')));
async function call(name,args){const r=await client.callTool({name,arguments:args});if(r.isError)throw Error(JSON.stringify(r.content));return r;}
try{
 await call('save_checkpoint',{name:'Empty shipping station before geometry'});
 for(const[name,elements]of Object.entries(groups)){await call('add_group',{name});await call('place_cube',{group:name,elements,faces:false});}
 const result=await call('export_model',{codec_id:'project',max_content_length:2000000});
 const t=result.content.filter(x=>x.type==='text').map(x=>x.text).join('\n');
 await writeFile(workspace+'/native-project-export-receipt.json',t);
 const meta=JSON.parse(t);const content=meta.content??meta.metadata?.content;
 if(typeof content!=='string')throw Error('Native project export did not return complete source content');
 await writeFile(workspace+'/station-blockout.bbmodel',content);
 await writeFile(root+'/geometry-spec.json',JSON.stringify({groups,target:'Java1.20.1, one16-unit block, north=-Z',cubeCount:Object.values(groups).flat().length},null,2));
 console.log(JSON.stringify({source:workspace+'/station-blockout.bbmodel',cubes:Object.values(groups).flat().length}));
}finally{await client.close();}
