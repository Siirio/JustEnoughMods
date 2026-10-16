// Export exact posed geometry through the native Blockbench OBJ codec.
// Open the canonical verdant_grasp_r3.bbmodel in Blockbench before running.
import fs from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL, fileURLToPath } from 'node:url';
import { createHash } from 'node:crypto';
const here = path.dirname(fileURLToPath(import.meta.url));
const sdk = process.env.BLOCKBENCH_MCP_ROOT || 'C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin';
const { Client } = await import(pathToFileURL(path.join(sdk, 'node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js')));
const { StreamableHTTPClientTransport } = await import(pathToFileURL(path.join(sdk, 'node_modules/@modelcontextprotocol/sdk/dist/esm/client/streamableHttp.js')));
const source = path.resolve(here, '../../Docs/races/assets/bloom_r3_candidate');
const sourceBytes = await fs.readFile(path.join(source, 'verdant_grasp_r3.bbmodel'));
const model = JSON.parse(sourceBytes);
const output = path.join(here, 'src/main/resources/assets/jem_visual_sandbox/r3');
await fs.mkdir(output, { recursive: true });
const client = new Client({name:'jem-r3-native-bake',version:'1.0.0'});
await client.connect(new StreamableHTTPClientTransport(new URL('http://127.0.0.1:3000/bb-mcp')));
async function call(name, args) {
    const result = await client.callTool({name, arguments:args});
    if (result.isError) throw new Error(JSON.stringify(result));
    return result.content.filter(c=>c.type==='text').map(c=>c.text).join('\n');
}
function parseObj(text) {
    const vertices=[], uvs=[], faces=[];
    let object='', material='';
    for (const line of text.split('\n')) {
        const [kind,...values]=line.trim().split(/\s+/);
        if(kind==='o') object=values.join(' ');
        if(kind==='usemtl') material=values[0];
        if(kind==='v') vertices.push(values.map(Number));
        if(kind==='vt') uvs.push(values.map(Number));
        if(kind==='f' && object!=='neutral_ground_plinth') {
            if(values.length!==4) throw new Error('Only native cuboid quads are supported');
            const points=values.map(v=>v.split('/').map(Number));
            faces.push({object,material,uv:points.flatMap(p=>[uvs[p[1]-1][0],1-uvs[p[1]-1][1]]),positions:points.flatMap(p=>vertices[p[0]-1])});
        }
    }
    return faces;
}
try {
    const live=JSON.parse(await call('export_model',{codec_id:'project',max_content_length:1000000}));
    const current=JSON.parse(live.content);
    // Ignore editor-only state, but require identical authored geometry/materials/curves.
    for(const key of ['elements','outliner','animations']) {
        if(JSON.stringify(current[key])!==JSON.stringify(model[key])) throw new Error(`Active Blockbench ${key} differs from canonical R3 source`);
    }
    const names=['bark','foliage','bud'];
    const materialIds=names.map(name=>'m_'+model.textures.find(t=>t.name==='r3_'+name).uuid);
    const poses=[];
    let faces;
    for(let frame=0;frame<=44;frame++) {
        await call('animation_timeline',{action:'set_time',animation_id:model.animations[0].name,time:frame/20});
        const exported=JSON.parse(await call('export_model',{codec_id:'obj',max_content_length:200000,options:{scale:16}}));
        if(exported.truncated) throw new Error('Truncated OBJ');
        const posed=parseObj(exported.content);
        if(posed.length!==294 || new Set(posed.map(f=>f.object)).size!==49) throw new Error('Expected exactly 49 runtime cubes');
        if(!faces) faces=posed;
        if(posed.some((f,i)=>f.object!==faces[i].object || f.material!==faces[i].material || JSON.stringify(f.uv)!==JSON.stringify(faces[i].uv))) throw new Error('Animated topology changed');
        poses.push(posed.flatMap(f=>f.positions));
    }
    const bytes=Buffer.alloc(16+faces.length*33+poses.length*faces.length*48);
    let cursor=0;
    const int=v=>{bytes.writeInt32BE(v,cursor);cursor+=4;};
    const float=v=>{if(!Number.isFinite(v)) throw new Error('Non-finite geometry');bytes.writeFloatBE(v,cursor);cursor+=4;};
    int(0x4a523301); int(poses.length); int(faces.length); float(.05);
    for(const face of faces) {
        const index=materialIds.indexOf(face.material);
        if(index<0) throw new Error(`Unexpected material ${face.material}`);
        bytes[cursor++]=index;
        face.uv.forEach(float);
    }
    poses.forEach(p=>p.forEach(float));
    await fs.writeFile(path.join(output,'verdant_grasp.mesh'),bytes);
    for(const name of names) await fs.copyFile(path.join(source,name+'.png'),path.join(output,name+'.png'));
    const manifest={source:'Docs/races/assets/bloom_r3_candidate/verdant_grasp_r3.bbmodel',sourceSha256:createHash('sha256').update(sourceBytes).digest('hex'),exporter:'Blockbench 5.2.2 native OBJ, scale=16 (16 model units per block)',frames:45,seconds:2.2,cubes:49,quads:294,omitted:'preview_stage_OMIT_FROM_RUNTIME / neutral_ground_plinth',meshSha256:createHash('sha256').update(bytes).digest('hex'),bytes:bytes.length};
    await fs.writeFile(path.join(output,'provenance.json'),JSON.stringify(manifest,null,2)+'\n');
    console.log(JSON.stringify(manifest));
} finally {
    await call('animation_timeline',{action:'set_time',time:1.2});
    await client.close();
}
