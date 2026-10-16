// USER_REJECTED negative evidence. Do not execute this archived art recipe.
throw new Error("Archived rejected candidate; choose current vanilla-grounded references before new authoring.");
// Original native-pixel design; images are painted by Blockbench, not generated/cropped externally.
import { Client } from 'file:///C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js';
import { StreamableHTTPClientTransport } from 'file:///C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/blockbench/mcp/blockbench-mcp-plugin/node_modules/@modelcontextprotocol/sdk/dist/esm/client/streamableHttp.js';
import { writeFile, mkdir } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
const directory = fileURLToPath(new URL('.', import.meta.url));
const palette = ['#102b26','#234c35','#397441','#649844','#a2c559','#e0dc8b','#fff0bb','#674b31','#a0753c','#d0ab58'];
const pixels = new Map();
let ox=0, oy=0;
function pixel(x,y,c) {
  x=Math.round(x); y=Math.round(y);
  if(x>=1&&x<=30&&y>=1&&y<=30) pixels.set(`${x+ox},${y+oy}`,c);
}
function line(x0,y0,x1,y1,c,width=1) {
  const n=Math.max(Math.abs(x1-x0),Math.abs(y1-y0));
  for(let i=0;i<=Math.ceil(n);i++) {
    const t=n?i/Math.ceil(n):0;
    for(let dx=0;dx<width;dx++)for(let dy=0;dy<width;dy++)pixel(x0+(x1-x0)*t+dx,y0+(y1-y0)*t+dy,c);
  }
}
function polygon(points,c) {
  for(let y=1;y<31;y++)for(let x=1;x<31;x++){
    let inside=false;
    for(let i=0,j=points.length-1;i<points.length;j=i++){
      const [a,b]=points[i], [d,e]=points[j];
      if((b>y)!==(e>y)&&x<(d-a)*(y-b)/(e-b)+a)inside=!inside;
    }
    if(inside)pixel(x,y,c);
  }
}
function leaf(x,y,tx,ty,width=3,fade=0) {
  const dx=tx-x,dy=ty-y,len=Math.hypot(dx,dy)||1;
  const px=-dy/len*width,py=dx/len*width;
  polygon([[x,y],[x+dx*.35+px,y+dy*.35+py],[tx,ty],[x+dx*.6-px,y+dy*.6-py]],fade?2:0);
  polygon([[x,y],[x+dx*.4+px*.7,y+dy*.4+py*.7],[tx,ty],[x+dx*.6-px*.65,y+dy*.6-py*.65]],fade?4:2);
  line(x,y,tx,ty,fade?5:4);
  if(!fade)line(x+dx*.4,y+dy*.4,x+dx*.6+px*.5,y+dy*.6+py*.5,3);
}
function branch(points,light=true) {
  for(let i=1;i<points.length;i++)line(...points[i-1],...points[i],0,3);
  for(let i=1;i<points.length;i++)line(points[i-1][0],points[i-1][1],points[i][0],points[i][1],7,2);
  for(let i=1;i<points.length;i++)line(points[i-1][0],points[i-1][1],points[i][0],points[i][1],light?8:2);
}
for(let frame=0;frame<8;frame++) {
  ox=frame*32;oy=0;
  const growth=[.12,.3,.52,.75,1,1,1,1][frame];
  const paths=[[[15,28],[12,23],[10,17],[6,13],[5,7]],[[16,28],[17,22],[20,18],[23,12],[22,5]],[[15,28],[13,20],[15,15],[14,9],[17,4]],[[16,28],[22,25],[26,20],[26,15]]];
  paths.forEach((path,index)=>{
    const count=Math.max(2,Math.ceil(path.length*growth));
    const active=path.slice(0,count).map(([x,y])=>[15+(x-15)*growth,28+(y-28)*growth]);
    if(frame===7&&index%2===0)return;
    branch(active,frame<6);
    if(frame>=2&&frame<7){const [x,y]=active.at(-1);leaf(x,y,x+(index%2?5:-5),y-4,2,frame===6);}
    if(frame>=3&&frame<6){const [x,y]=active[Math.max(0,active.length-2)];leaf(x,y,x+(index%2?-5:5),y-3,2);}
  });
  if(frame<7){polygon([[11,29],[13,26],[18,26],[21,29]],1);line(13,27,17,27,4);}
  if(frame>=5){[[4,8],[25,6],[9,17],[22,20]].forEach(([x,y],i)=>{pixel(x+(frame-5)*(i%2?1:-1),y-(frame-5)*2,5);});}
  oy=32;
  const radius=[2,4,7,11,13,12,11,9][frame];
  for(let p=0;p<7;p++){
    const angle=p*Math.PI*2/7-.2;
    const dx=Math.cos(angle),dy=Math.sin(angle);
    const base=frame>=5?4+(frame-5)*2:1;
    const bx=16+dx*base,by=16+dy*base;
    if(frame===7&&p%2===0)continue;
    leaf(bx,by,16+dx*radius,16+dy*radius,frame<3?2:3,frame>=5);
    if(frame>=2&&frame<=4)line(bx,by,16+dx*(radius-2),16+dy*(radius-2),5);
  }
  if(frame<6){polygon([[13,14],[16,12],[19,15],[17,19],[13,18]],8);polygon([[14,14],[17,14],[18,16],[15,18]],5);pixel(15,15,6);}
  if(frame>=4)for(let p=0;p<5;p++){const a=p*1.8;pixel(16+Math.cos(a)*14,16+Math.sin(a)*14,5);}
  oy=64;
  const h=[3,7,12,17,22,22,19,12][frame];
  if(frame<7){branch([[16,29],[15,29-h*.45],[17,29-h]],false);line(16,28,17,29-h,4);}
  for(let j=0;j<3;j++){
    if(h<6+j*5)continue;
    const y=27-j*6;
    leaf(16,y,5+j*2-(frame>5?1:0),y-5-j,3,frame>=6);
    if(frame<7)leaf(16,y-3,26-j,y-9,3,frame>=6);
  }
  if(frame>=3&&frame<=5){polygon([[15,7],[17,4],[19,7],[17,11]],5);pixel(17,6,6);if(frame===5){leaf(17,8,11,3,2,true);leaf(17,8,24,4,2,true);}}
  oy=96;
  const widths=[4,3,1,2,4,3,2,1];
  const xshift=[0,1,2,2,1,0,-1,-2][frame];
  leaf(11+xshift,25,22+xshift,7,widths[frame],frame>=6);
  if(frame<6){line(10+xshift,27,13+xshift,23,8);pixel(20+xshift,10,5);}
  if(frame>=4){pixel(7+frame,7-frame*.3,5);pixel(26,20-frame,4);}
}
const client=new Client({name:'jem-bloom-authoring',version:'2.0.0'});
await client.connect(new StreamableHTTPClientTransport(new URL('http://127.0.0.1:3000/bb-mcp')));
async function call(name,args){const r=await client.callTool({name,arguments:args},undefined,{timeout:180000});if(r.isError)throw new Error(JSON.stringify(r.content));return r;}
try {
  await call('set_mode',{mode_id:'paint'});
  // The named atlas was explicitly created for this candidate. Do not paint arbitrary projects.
  const listing=await call('list_textures',{});
  if(!listing.content.some(c=>c.type==='text'&&c.text.includes('bloom_growth_r2')))throw new Error('Open the R2 source project first.');
  for(let c=0;c<palette.length;c++){
    const coordinates=[...pixels].filter(([,color])=>color===c).map(([key])=>{const [x,y]=key.split(',').map(Number);return{x,y};});
    if(coordinates.length)await call('paint_with_brush',{texture_id:'bloom_growth_r2',coordinates,connect_strokes:false,brush_settings:{size:1,softness:0,opacity:255,shape:'square',blend_mode:'default',color:palette[c]}});
    console.log(`Painted ${palette[c]}: ${coordinates.length} pixels`);
  }
  await mkdir(directory,{recursive:true});
  const texture=await call('get_texture',{texture:'bloom_growth_r2'});
  const img=texture.content.find(c=>c.type==='image');
  if(!img)throw new Error('Native texture export returned no image.');
  await writeFile(directory+'/bloom_growth_r2.png',Buffer.from(img.data,'base64'));
  const exported=await call('export_model',{codec_id:'project',max_content_length:2000000});
  const meta=exported.structuredContent??JSON.parse(exported.content.find(c=>c.type==='text').text);
  if(meta.truncated||typeof meta.content!=='string')throw new Error('Incomplete project export.');
  await writeFile(directory+'/born_in_bloom_r2.bbmodel',meta.content);
  console.log('Saved native texture export and editable project.');
} finally {await client.close();}
