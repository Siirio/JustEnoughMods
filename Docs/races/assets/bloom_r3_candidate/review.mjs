import {Client} from 'file:///C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js';
import {StreamableHTTPClientTransport} from 'file:///C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin/node_modules/@modelcontextprotocol/sdk/dist/esm/client/streamableHttp.js';
import {writeFile,mkdir} from 'node:fs/promises';
const client=new Client({name:'jem-bloom-r3-review',version:'3.0.0'});
await client.connect(new StreamableHTTPClientTransport(new URL('http://127.0.0.1:3000/bb-mcp')));
async function call(name,args){const r=await client.callTool({name,arguments:args},undefined,{timeout:180000});if(r.isError)throw new Error(JSON.stringify(r.content));return r;}
const output=new URL('.',import.meta.url);
async function image(name,r){const image=r.content.find(c=>c.type==='image');if(!image)throw new Error('Missing native image');await writeFile(new URL(name,output),Buffer.from(image.data,'base64'));}
try{
  const info=await call('get_project_info',{});
  if(!JSON.stringify(info).includes('Born in Bloom R3'))throw new Error('Select R3 project');
  if(process.argv.includes('--revise')){
    await call('set_mode',{mode_id:'edit'});
    await call('modify_cube',{id:'neutral_ground_plinth',from:[-128,-40,-128],to:[128,-.4,128]});
    const groundPixels=[];for(let y=0;y<16;y++)for(let x=0;x<16;x++)groundPixels.push({x,y});
    await call('set_mode',{mode_id:'paint'});
    await call('paint_with_brush',{texture_id:'r3_stage',coordinates:groundPixels,connect_strokes:false,brush_settings:{size:1,opacity:255,softness:0,shape:'square',color:'#87917d'}});
    await call('set_mode',{mode_id:'edit'});
    for(let i=0;i<5;i++){
      const h=[1,1.12,.9,1.05,.86][i];
      await call('modify_cube',{id:'left_leaf_mass_'+i,from:[-4.2,8.0*h,4.8],to:[-.4,10.4*h,8.3]});
      await call('modify_cube',{id:'left_leaf_tip_'+i,from:[-5.3,8.5*h,5.4],to:[-3,10.0*h,7.7]});
      await call('modify_cube',{id:'right_leaf_mass_'+i,from:[.5,9*h,5.1],to:[4.0,11.1*h,8.1]});
      await call('modify_cube',{id:'right_leaf_tip_'+i,from:[3,9.6*h,5.6],to:[5,10.8*h,7.6]});
      // Deliberate UV samples at one material texel per model unit, rather than a flat corner of the tile.
      for(const [side,width,height,depth]of [['left',3.8,2.4*h,3.5],['right',3.5,2.1*h,3.0]]){
        const offset=side==='left'?[2,5]:[9,8];
        const rect=(w,h)=>[offset[0],offset[1],offset[0]+w,offset[1]+h];
        await call('set_cube_uv',{id:side+'_leaf_mass_'+i,box_uv:false,faces:{north:{uv:rect(width,height)},south:{uv:rect(width,height)},east:{uv:rect(depth,height)},west:{uv:rect(depth,height)},up:{uv:rect(width,depth)},down:{uv:rect(width,depth)}}});
      }
    }
  }
  await call('set_mode',{mode_id:'animate'});
  await call('animation_timeline',{action:'set_time',time:1.2});
  const exported=await call('export_model',{codec_id:'project',max_content_length:2000000});
  const data=exported.structuredContent??JSON.parse(exported.content.find(c=>c.type==='text').text);
  if(data.truncated)throw new Error('Incomplete native export');
  await writeFile(new URL('verdant_grasp_r3.bbmodel',output),data.content);
  for(const name of ['bark','foliage','bud','stage'])await image(name+'.png',await call('get_texture',{texture:'r3_'+name}));
  const views=await call('list_views',{});
  if(!JSON.stringify(views).includes('bloom_r3_review'))await call('create_offscreen_view',{id:'bloom_r3_review',width:600,height:600,antialias:false,copy_view:'none'});
  const cameras={hero:[42,26,46],front:[0,14,66],side:[66,14,0],back:[0,18,-66],eye:[42,29,46]};
  for(const[name,position]of Object.entries(cameras))await image('view-'+name+'.png',await call('set_camera_angle',{view:'bloom_r3_review',position,target:[0,5,0],projection:'perspective',fov:35}));
  await call('set_camera_angle',{view:'bloom_r3_review',position:cameras.hero,target:[0,5,0],projection:'perspective',fov:35});
  await mkdir(new URL('frames/',output),{recursive:true});
  for(let frame=0;frame<=44;frame++){
    await call('animation_timeline',{action:'set_time',time:frame/20});
    await image('frames/'+String(frame).padStart(3,'0')+'.png',await call('capture_screenshot',{view:'bloom_r3_review',format:'png'}));
  }
  await call('animation_timeline',{action:'set_time',time:1.2});
  // This pinned plugin's delete_offscreen_view currently throws "onContextMenu is not defined".
  // Do not repeat the failing disposal; the remaining private view is documented in the audit.
  console.log('Saved revised source, five native editor views and 45 fixed-camera animation frames.');
}finally{await client.close();}
