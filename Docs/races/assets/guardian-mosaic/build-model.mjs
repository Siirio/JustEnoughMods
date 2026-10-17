// Structured MCP geometry and animation only; no Blockbench painting/evaluation.
import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
const root=path.dirname(fileURLToPath(import.meta.url));
const sdk='C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin';
const {Client}=await import(pathToFileURL(sdk+'/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js'));
const {StdioClientTransport}=await import(pathToFileURL(sdk+'/node_modules/@modelcontextprotocol/sdk/dist/esm/client/stdio.js'));
const client=new Client({name:'guardian-cuboid-production',version:'1'});
await client.connect(new StdioClientTransport({command:process.execPath,args:[sdk+'/bin/blockbench-mcp-headless.mjs','--root','C:/MyProjects'],stderr:'inherit'}));
async function call(name,args){const r=await client.callTool({name,arguments:args});if(r.isError)throw Error(JSON.stringify(r));return r;}
try{
 const file=root+'/guardian-mosaic.bbmodel';
 await call('bbmodel_create',{file,name:'Ancestors guardian mosaic',format:'free',box_uv:false,resolution:{width:64,height:32}});
 const ops=JSON.parse(await fs.readFile(root+'/operations.json','utf8'));
 for(let i=0;i<ops.length;i+=400)await call('bbmodel_edit',{file,operations:ops.slice(i,i+400)});
 const info=await call('bbmodel_info',{file});await fs.writeFile(root+'/mcp-info.json',JSON.stringify(info,null,2));
 await call('blockbench_launch',{file,wait_for_mcp_ms:4000});
 console.log('Editable cuboid model opened in Blockbench:',file);
}finally{await client.close();}
