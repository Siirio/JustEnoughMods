// Export the actual native Blockbench mesh, preserving shared indices and UVs.
import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
import {createHash} from 'node:crypto';
const root=path.dirname(fileURLToPath(import.meta.url));
const sdk='C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin';
const {Client}=await import(pathToFileURL(sdk+'/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js'));
const {StreamableHTTPClientTransport}=await import(pathToFileURL(sdk+'/node_modules/@modelcontextprotocol/sdk/dist/esm/client/streamableHttp.js'));
const c=new Client({name:'guardian-indexed-export',version:'1'});
await c.connect(new StreamableHTTPClientTransport(new URL('http://127.0.0.1:3000/bb-mcp')));
try{
 const r=await c.callTool({name:'export_model',arguments:{codec_id:'project',max_content_length:2000000}});
 if(r.isError)throw Error(JSON.stringify(r));
 const value=r.structuredContent??JSON.parse(r.content.find(b=>b.type==='text').text);
 const native=JSON.parse(value.content);
 const mesh=native.elements.find(e=>e.name==='connected_hemisphere');
 if(!mesh || native.elements.length!==1 || mesh.type!=='mesh')throw Error('Wrong native mesh/tab');
 const keys=Object.keys(mesh.vertices),index=new Map(keys.map((k,i)=>[k,i]));
 const vertices=keys.map(k=>mesh.vertices[k].map(v=>v/16));
 const faces=Object.values(mesh.faces).map(f=>{
  const layer=f.vertices.every(k=>k.startsWith('outer_'))?'outer':f.vertices.every(k=>k.startsWith('inner_'))?'inner':'rim';
  return {layer,indices:f.vertices.map(k=>index.get(k)),uv:f.vertices.map(k=>[f.uv[k][0]/256,f.uv[k][1]/64]),normals:f.vertices.map(k=>{
   if(layer==='rim')return [0,-1,0];
   const p=mesh.vertices[k].map(v=>v/16);p[1]-=.1;
   const length=Math.hypot(...p),sign=layer==='inner'?-1:1;
   return p.map(v=>v/length*sign);
  })};
 });
 const file=root+'/guardian-dome.bbmodel';
 const output={format:1,radius:2.2,thickness:.04,baseY:.1,vertices,faces,sourceSha256:createHash('sha256').update(await fs.readFile(file)).digest('hex'),exporter:'Actual Blockbench native project mesh; shared vertex indices, per-corner UV and analytic smooth normals'};
 const target=path.resolve(root,'../../../../Mods/JEMVisualSandboxR3/src/main/resources/assets/jem_visual_sandbox/guardian_dome');
 await fs.mkdir(target,{recursive:true});
 await fs.writeFile(target+'/model.json',JSON.stringify(output));
 await fs.copyFile(root+'/gold-wave.png',target+'/gold-wave.png');
 await fs.writeFile(target+'/gold-wave.png.mcmeta',JSON.stringify({texture:{blur:false,clamp:false}}));
 await fs.writeFile(root+'/export.json',JSON.stringify({sourceSha256:output.sourceSha256,vertices:vertices.length,faces:faces.length,modelSha256:createHash('sha256').update(JSON.stringify(output)).digest('hex'),exporter:output.exporter},null,2));
 console.log('Exported native connected dome:',vertices.length,'vertices,',faces.length,'faces');
}finally{await c.close();}
