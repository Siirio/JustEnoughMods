// Actual headless Blockbench MCP authoring, then open the single native mesh.
import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
const root=path.dirname(fileURLToPath(import.meta.url));
const sdk='C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin';
const {Client}=await import(pathToFileURL(sdk+'/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js'));
const {StdioClientTransport}=await import(pathToFileURL(sdk+'/node_modules/@modelcontextprotocol/sdk/dist/esm/client/stdio.js'));
const c=new Client({name:'connected-guardian-dome',version:'1'});
await c.connect(new StdioClientTransport({command:process.execPath,args:[sdk+'/bin/blockbench-mcp-headless.mjs','--root','C:/MyProjects'],stderr:'inherit'}));
async function call(name,args){const r=await c.callTool({name,arguments:args});if(r.isError)throw Error(JSON.stringify(r));return r;}
try{
 const file=root+'/guardian-dome.bbmodel';
 await call('bbmodel_create',{file,name:'Ancestors connected guardian dome',format:'free',box_uv:false,resolution:{width:256,height:64}});
 await call('bbmodel_edit',{file,operations:JSON.parse(await fs.readFile(root+'/operations.json','utf8'))});
 await call('blockbench_launch',{file,wait_for_mcp_ms:4000});
 console.log('Opened connected native dome:',file);
}finally{await c.close();}
