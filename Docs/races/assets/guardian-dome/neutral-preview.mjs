// Metadata-only editor preview; pixel source and runtime additive material unchanged.
import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
const root=path.dirname(fileURLToPath(import.meta.url));
const sdk='C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin';
const {Client}=await import(pathToFileURL(sdk+'/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js'));
const {StreamableHTTPClientTransport}=await import(pathToFileURL(sdk+'/node_modules/@modelcontextprotocol/sdk/dist/esm/client/streamableHttp.js'));
const {StdioClientTransport}=await import(pathToFileURL(sdk+'/node_modules/@modelcontextprotocol/sdk/dist/esm/client/stdio.js'));
const d=new Client({name:'guardian-neutral-preview',version:'1'}),h=new Client({name:'guardian-texture-metadata',version:'1'});
async function call(c,name,args){const r=await c.callTool({name,arguments:args});if(r.isError)throw Error(JSON.stringify(r));return r;}
await d.connect(new StreamableHTTPClientTransport(new URL('http://127.0.0.1:3000/bb-mcp')));
await h.connect(new StdioClientTransport({command:process.execPath,args:[sdk+'/bin/blockbench-mcp-headless.mjs','--root','C:/MyProjects'],stderr:'inherit'}));
try{
 const r=await call(d,'export_model',{codec_id:'project',max_content_length:2000000});
 const v=r.structuredContent??JSON.parse(r.content.find(b=>b.type==='text').text);
 if(!JSON.parse(v.content).elements.some(e=>e.name==='connected_hemisphere'))throw Error('Unrelated active project; refusing to close');
 await call(d,'trigger_action',{action:'close_project'});
 const file=root+'/guardian-dome.bbmodel';
 await call(h,'bbmodel_edit',{file,operations:[{op:'update_texture',target:0,render_mode:'default'}]});
 await call(h,'blockbench_launch',{file,wait_for_mcp_ms:4000});
}finally{await d.close();await h.close();}
