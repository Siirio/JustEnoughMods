// Original materials and cuboid rig, authored through structured Blockbench MCP tools.
import {Client} from 'file:///C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js';
import {StreamableHTTPClientTransport} from 'file:///C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin/node_modules/@modelcontextprotocol/sdk/dist/esm/client/streamableHttp.js';
import {writeFile} from 'node:fs/promises';
const client=new Client({name:'jem-bloom-r3-author',version:'3.0.0'});
await client.connect(new StreamableHTTPClientTransport(new URL('http://127.0.0.1:3000/bb-mcp')));
async function call(name,args){const r=await client.callTool({name,arguments:args},undefined,{timeout:180000});if(r.isError)throw new Error(JSON.stringify(r.content));return r;}
const output=new URL('.',import.meta.url);
const palettes={bark:['#5c4b37','#756046','#8d7656','#a58b66'],foliage:['#354a2a','#4a6433','#65813f','#819650'],bud:['#733c5c','#a75d83','#cd91ac','#dec6ad'],stage:['#485440','#536149','#607052','#69795a']};
function material(name){
  const grid=Array.from({length:16},()=>Array(16).fill(name==='bud'?1:1));
  const rect=(x,y,w,h,c)=>{for(let j=y;j<y+h;j++)for(let i=x;i<x+w;i++)if(j>=0&&j<16&&i>=0&&i<16)grid[j][i]=c;};
  if(name==='bark'){
    [[1,0,2,6,0],[0,7,2,6,0],[5,3,2,8,0],[6,12,2,4,0],[11,0,2,9,0],[10,10,2,6,0],[3,1,2,5,2],[3,8,2,5,2],[8,4,2,7,2],[13,1,2,7,2],[12,11,2,4,2],[4,2,1,3,3],[9,6,1,4,3],[14,2,1,4,3]].forEach(r=>rect(...r));
  }else if(name==='foliage'){
    [[0,2,4,3,0],[1,11,4,3,0],[7,5,3,4,0],[12,11,4,4,0],[6,0,4,3,2],[2,6,4,3,2],[9,9,3,3,2],[12,2,4,4,2],[3,5,2,2,3],[10,1,2,2,3],[8,11,2,2,3],[4,13,2,2,2],[13,6,3,2,0]].forEach(r=>rect(...r));
  }else if(name==='bud'){
    [[0,0,4,16,0],[12,0,4,16,0],[4,0,8,3,0],[4,4,3,7,2],[8,4,3,5,2],[6,11,4,4,2],[6,6,4,3,3],[7,5,2,5,3]].forEach(r=>rect(...r));
  }else{
    [[0,1,4,3,0],[6,5,5,3,2],[2,10,4,4,2],[11,1,4,4,2],[12,11,4,4,0],[7,1,2,2,3],[1,7,3,2,0],[10,9,2,2,3]].forEach(r=>rect(...r));
  }
  return grid;
}
const animation={};
async function group(name,parent,origin,rotation=[0,0,0]){await call('add_group',{name,parent,origin,rotation});}
async function cubes(group,texture,elements){await call('place_cube',{group,texture,elements,faces:true});}
try{
  const info=await call('get_project_info',{});
  if(!JSON.stringify(info).includes('Born in Bloom R3'))throw new Error('Select the dedicated R3 candidate project first.');
  for(const [name,palette]of Object.entries(palettes)){
    await call('create_texture',{name:'r3_'+name,width:16,height:16,uv_width:16,uv_height:16,fill_color:palette[1]});
    await call('set_mode',{mode_id:'paint'});
    const grid=material(name);
    for(let color=0;color<palette.length;color++){
      const coordinates=[];for(let y=0;y<16;y++)for(let x=0;x<16;x++)if(grid[y][x]===color)coordinates.push({x,y});
      if(coordinates.length)await call('paint_with_brush',{texture_id:'r3_'+name,coordinates,connect_strokes:false,brush_settings:{size:1,opacity:255,softness:0,shape:'square',color:palette[color]}});
    }
    const image=(await call('get_texture',{texture:'r3_'+name})).content.find(c=>c.type==='image');
    await writeFile(new URL(name+'.png',output),Buffer.from(image.data,'base64'));
  }
  await call('set_mode',{mode_id:'edit'});
  await group('preview_stage_OMIT_FROM_RUNTIME','root',[0,0,0]);
  await cubes('preview_stage_OMIT_FROM_RUNTIME','r3_stage',[{name:'neutral_ground_plinth',from:[-16,-2,-16],to:[16,-.4,16]}]);
  const angles=[15,90,160,235,305];
  for(let i=0;i<5;i++){
    const yaw=angles[i],height=[1,1.12,.9,1.05,.86][i],delay=i*.045;
    await group('root_'+i,'root',[0,0,0],[0,yaw,0]);
    await group('rise_'+i,'root_'+i,[0,0,6],[12,0,0]);
    await cubes('rise_'+i,'r3_bark',[{name:'root_foot_'+i,from:[-1.8,-1,4.5],to:[1.8,5.4,8.0]},{name:'ground_spur_'+i,from:[-1.1,-.7,7],to:[1.1,1.3,11.5],rotation:[0,12,0],origin:[0,0,7]}]);
    await group('curl_'+i,'rise_'+i,[0,5,6],[-27,0,0]);
    await cubes('curl_'+i,'r3_bark',[{name:'root_shaft_'+i,from:[-1.35,4.5,5.1],to:[1.35,11.5*height,7.8]}]);
    await group('tip_'+i,'curl_'+i,[0,10*height,6],[-36,0,0]);
    await cubes('tip_'+i,'r3_bark',[{name:'tapered_tip_'+i,from:[-.9,10*height,5.4],to:[.9,14*height,7.2]},{name:'terminal_tip_'+i,from:[-.55,13.5*height,5.8],to:[.55,15.5*height,6.9],rotation:[-18,0,0],origin:[0,13.5*height,6]}]);
    await group('leaf_'+i,'curl_'+i,[0,8.4*height,6]);
    await cubes('leaf_'+i,'r3_foliage',[
      {name:'left_leaf_mass_'+i,from:[-4.4,8.2*height,4.9],to:[-.5,9.7*height,8.0],rotation:[0,0,19],origin:[-.5,8.4*height,6]},
      {name:'left_leaf_tip_'+i,from:[-5.4,8.7*height,5.5],to:[-3.1,9.7*height,7.5],rotation:[0,0,19],origin:[-.5,8.4*height,6]},
      {name:'right_leaf_mass_'+i,from:[.6,9.3*height,5.2],to:[4.2,10.7*height,7.8],rotation:[0,0,-22],origin:[.6,9.3*height,6]},
      {name:'right_leaf_tip_'+i,from:[3.1,9.7*height,5.6],to:[5.1,10.6*height,7.5],rotation:[0,0,-22],origin:[.6,9.3*height,6]}]);
    if(i===1||i===3){await group('bud_'+i,'tip_'+i,[0,13*height,6]);await cubes('bud_'+i,'r3_bud',[{name:'flower_bud_'+i,from:[-1.3,13*height,5.2],to:[1.3,15.2*height,7.6]},{name:'bud_crown_'+i,from:[-.8,15.2*height,5.6],to:[.8,16.2*height,7.2]}]);}
    animation['rise_'+i]=[
      {time:0,position:[0,-17,0],rotation:[30,0,0]},
      {time:.12+delay,position:[0,-15,0],rotation:[30,0,0]},
      {time:.45+delay,position:[0,.35,0],rotation:[9,0,0]},
      {time:.67+delay,position:[0,0,0],rotation:[12,0,0]},
      {time:1.58,position:[0,0,0],rotation:[12,0,0]},
      {time:1.83+delay*.3,position:[0,-2,0],rotation:[20,0,0]},
      {time:2.2,position:[0,-17,0],rotation:[30,0,0]}];
    animation['curl_'+i]=[{time:0,rotation:[18,0,0]},{time:.28+delay,rotation:[18,0,0]},{time:.67+delay,rotation:[-31,0,0]},{time:.85+delay*.5,rotation:[-27,0,0]},{time:1.6,rotation:[-27,0,0]},{time:2.2,rotation:[18,0,0]}];
    animation['tip_'+i]=[{time:0,rotation:[5,0,0]},{time:.48+delay,rotation:[5,0,0]},{time:.8+delay*.5,rotation:[-40,0,0]},{time:1.05,rotation:[-36,0,0]},{time:1.6,rotation:[-36,0,0]},{time:2.2,rotation:[5,0,0]}];
    animation['leaf_'+i]=[{time:0,scale:[.08,.08,.08]},{time:.45+delay,scale:[.08,.08,.08]},{time:.9+delay,scale:[1,1,1]},{time:1.55,scale:[1,1,1]},{time:1.95,scale:[.4,.4,.4]},{time:2.2,scale:[.08,.08,.08]}];
    if(i===1||i===3)animation['bud_'+i]=[{time:0,scale:[.05,.05,.05]},{time:.8,scale:[.05,.05,.05]},{time:1.15,scale:[1,1,1]},{time:1.6,scale:[1,1,1]},{time:2.05,scale:[.05,.05,.05]}];
  }
  await call('create_animation',{name:'verdant_grasp_r3',animation_length:2.2,loop:false,bones:animation});
  await call('animation_timeline',{action:'set_fps',fps:20});
  await call('animation_timeline',{action:'set_time',time:1.2});
  const exported=await call('export_model',{codec_id:'project',max_content_length:2000000});
  const data=exported.structuredContent??JSON.parse(exported.content.find(c=>c.type==='text').text);
  if(data.truncated)throw new Error('Project export truncated');
  await writeFile(new URL('verdant_grasp_r3.bbmodel',output),data.content);
  console.log('Authored original materials, five articulated roots, coherent 44-tick action and editable Blockbench source.');
}finally{await client.close();}
