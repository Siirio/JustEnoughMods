// Authored native pixel decisions; this script prepares Aseprite MCP commands.
// It never rasterizes a PNG itself, resizes a texture, or reads AI artwork.
import {writeFile, mkdir} from 'node:fs/promises';
import {dirname, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
const root=dirname(fileURLToPath(import.meta.url)).replaceAll('\\','/');
const out=root+'/native'; await mkdir(out,{recursive:true});
const operations=[];
const op=(name,args)=>operations.push({name,arguments:args});
function canvas(w,h){
  const rows=Array.from({length:h},()=>Array(w).fill('.'));
  const colors={};
  const color=c=>{let k=Object.keys(colors).find(k=>colors[k]===c);if(!k){k=String.fromCharCode(65+Object.keys(colors).length);colors[k]=c;}return k;};
  const pixel=(x,y,c)=>{if(x>=0&&y>=0&&x<w&&y<h)rows[y][x]=color(c);};
  const rect=(x,y,rw,rh,c)=>{for(let j=y;j<y+rh;j++)for(let i=x;i<x+rw;i++)pixel(i,j,c);};
  const line=(x,y,n,c)=>rect(x,y,n,1,c);
  const clear=(x,y,rw,rh)=>{for(let j=y;j<y+rh;j++)for(let i=x;i<x+rw;i++)rows[j][i]='.';};
  return {w,h,pixel,rect,line,clear,grid:()=>({rows:rows.map(r=>r.join('')),legend:colors})};
}
const P={ink:'#201E20',deep:'#30292A',wood0:'#3D2C26',wood1:'#65442D',wood2:'#87603A',wood3:'#AE8452',wood4:'#D3AA6C',iron0:'#22282D',iron1:'#414A50',iron2:'#6B7377',iron3:'#9DA7A8',iron4:'#CCD0C5',well0:'#393831',well1:'#4C4A40',well2:'#6F6B5C',well3:'#AAA28A',paper:'#CFCAAF',text:'#F3E6CD',mute:'#BAAE98',gold:'#E5C46F',blue:'#8EAFAC'};
function panel(c,x,y,w,h){
 c.rect(x+2,y,w-4,h,P.ink);c.rect(x,y+2,w,h-4,P.ink);
 c.rect(x+2,y+1,w-4,h-3,P.wood1);c.rect(x+1,y+3,w-2,h-6,P.wood2);
 c.line(x+3,y+2,w-6,P.wood4);c.rect(x+2,y+4,1,h-8,P.wood3);
 c.line(x+3,y+h-3,w-6,P.wood0);c.rect(x+w-3,y+4,1,h-8,P.wood0);
}
function iron(c,x,y,w,h){c.rect(x,y,w,h,P.iron0);c.rect(x+1,y+1,w-2,h-2,P.iron1);c.line(x+1,y+1,w-2,P.iron3);c.line(x+2,y+2,w-4,P.iron2);c.line(x+1,y+h-2,w-2,P.ink);}
function rivet(c,x,y){c.rect(x,y,3,3,P.iron0);c.line(x,y,2,P.iron3);c.pixel(x+1,y+1,P.iron2);}
function slot(c,x,y,payout=false){c.rect(x,y,18,18,P.well0);c.rect(x+1,y+1,16,16,payout?'#4E4532':P.well1);c.line(x+1,y+1,16,P.ink);c.rect(x+1,y+1,1,16,P.ink);c.line(x+1,y+17,17,P.well3);c.rect(x+17,y+1,1,17,P.well2);}
function button(c,x,y,w,state='normal'){
 iron(c,x,y,w,22);c.rect(x+2,y+2,w-4,17,state==='disabled'?P.well2:P.wood2);
 c.line(x+3,y+3,w-6,state==='hover'?P.wood4:P.wood3);c.line(x+3,y+18,w-6,P.wood0);
 if(state==='pressed'){c.line(x+3,y+3,w-6,P.wood0);c.line(x+3,y+17,w-6,P.wood3);}
 if(state==='focus'){c.line(x+5,y+2,w-10,P.gold);c.line(x+5,y+19,w-10,P.gold);}
 rivet(c,x+3,y+8);rivet(c,x+w-6,y+8);
}
const ui=canvas(304,272);panel(ui,0,0,304,272);
// Structural wood beams; intentional quiet horizontal grain, no random noise.
for(const y of [18,174]){ui.rect(4,y,296,5,P.wood0);ui.line(5,y,294,P.wood3);ui.line(8,y+3,287,P.wood1);}
for(const [x,y,n]of [[39,5,28],[78,11,19],[210,7,24],[253,12,15],[19,268,24],[119,265,17]])ui.line(x,y,n,P.wood1);
ui.rect(7,23,174,148,P.wood0);ui.rect(9,25,170,144,P.wood1);
ui.rect(7,179,174,87,P.wood0);ui.rect(9,181,170,83,P.wood1);
iron(ui,183,23,113,150);ui.rect(186,26,107,143,P.wood0);
ui.line(190,52,99,P.wood2);ui.line(190,78,99,P.wood1);ui.line(190,105,99,P.wood1);
button(ui,189,127,103);
// Corner brackets have a shaped silhouette and one fastener per arm.
for(const [x,y,sx,sy] of [[2,2,1,1],[301,2,-1,1],[2,269,1,-1],[301,269,-1,-1]]){
 for(let a=0;a<14;a++)for(let b=0;b<4;b++){ui.pixel(x+sx*a,y+sy*b,P.iron1);ui.pixel(x+sx*b,y+sy*a,P.iron1);}
 for(let a=1;a<13;a++){ui.pixel(x+sx*a,y,P.iron3);ui.pixel(x,y+sy*a,P.iron2);}
 const rx=sx===1?x+1:x-3,ry=sy===1?y+1:y-3;rivet(ui,rx,ry);
}
// The player inventory extends down only under the real slots; no empty right slab.
ui.clear(184,176,120,96);
ui.rect(181,180,3,90,P.iron0);ui.rect(181,181,1,87,P.iron2);
ui.line(8,270,174,P.iron0);ui.line(8,269,174,P.iron2);rivet(ui,179,267);
const wells=canvas(304,272);
for(let row=0;row<6;row++)for(let col=0;col<9;col++)slot(wells,13+18*col,29+18*row);
for(let col=0;col<9;col++)slot(wells,13+18*col,153,true);
for(let row=0;row<3;row++)for(let col=0;col<9;col++)slot(wells,13+18*col,194+18*row);
for(let col=0;col<9;col++)slot(wells,13+18*col,251);

// Original 16px weighing-station item/action silhouette, not an AI crop.
const icon=canvas(16,16);
icon.rect(7,1,2,8,P.iron0);icon.rect(8,2,1,6,P.iron2);
icon.rect(1,2,14,2,P.iron0);icon.line(2,2,12,P.iron3);icon.rect(7,1,2,3,P.wood4);
for(const x of [2,12]){icon.rect(x,4,1,3,P.iron2);icon.rect(x-2,7,5,2,P.iron0);icon.line(x-1,7,3,P.iron3);}
icon.rect(1,10,14,6,P.wood0);icon.rect(2,10,12,5,P.wood2);icon.line(2,10,12,P.wood4);icon.line(2,13,12,P.wood1);
icon.rect(1,10,2,6,P.iron1);icon.rect(13,10,2,6,P.iron1);icon.pixel(1,11,P.iron3);icon.pixel(14,11,P.iron3);
icon.rect(4,9,3,2,'#9B7236');icon.pixel(5,8,'#E3BA53');icon.rect(10,9,2,2,'#68727B');

const fonts={
'А':['01110','10001','10001','11111','10001','10001','10001'], 'Б':['11111','10000','10000','11110','10001','10001','11110'],
'В':['11110','10001','10001','11110','10001','10001','11110'], 'Г':['11111','10000','10000','10000','10000','10000','10000'],
'Д':['00110','01010','01010','01010','01010','11111','10001'], 'Е':['11111','10000','10000','11110','10000','10000','11111'],
'Ж':['10101','10101','01110','00100','01110','10101','10101'], 'З':['01110','10001','00001','00110','00001','10001','01110'],
'И':['10001','10011','10011','10101','11001','11001','10001'], 'Й':['01010','00100','10001','10011','10101','11001','10001'],
'К':['10001','10010','10100','11000','10100','10010','10001'], 'Л':['00111','01001','01001','01001','01001','10001','10001'],
'М':['10001','11011','10101','10101','10001','10001','10001'], 'Н':['10001','10001','10001','11111','10001','10001','10001'],
'О':['01110','10001','10001','10001','10001','10001','01110'], 'П':['11111','10001','10001','10001','10001','10001','10001'],
'Р':['11110','10001','10001','11110','10000','10000','10000'], 'С':['01111','10000','10000','10000','10000','10000','01111'],
'Т':['11111','00100','00100','00100','00100','00100','00100'], 'У':['10001','10001','01010','00100','00100','01000','10000'],
'Ф':['00100','01110','10101','10101','01110','00100','00100'], 'Х':['10001','10001','01010','00100','01010','10001','10001'],
'Ц':['10010','10010','10010','10010','10010','11111','00001'], 'Ч':['10001','10001','10001','01111','00001','00001','00001'],
'Ш':['10101','10101','10101','10101','10101','10101','11111'], 'Щ':['10101','10101','10101','10101','10101','11111','00001'],
'Ы':['10001','10001','10001','11101','10011','10011','11101'], 'Ь':['10000','10000','10000','11110','10001','10001','11110'],
'Э':['11110','00001','00001','01111','00001','00001','11110'], 'Ю':['10110','11001','11001','11101','11001','11001','10110'],
'Я':['01111','10001','10001','01111','00101','01001','10001'],
'0':['01110','10001','10011','10101','11001','10001','01110'], '1':['00100','01100','00100','00100','00100','00100','01110'],
'2':['01110','10001','00001','00010','00100','01000','11111'], '3':['11110','00001','00001','01110','00001','00001','11110'],
'4':['00010','00110','01010','10010','11111','00010','00010'], '8':['01110','10001','10001','01110','10001','10001','01110']};
function text(c,s,x,y,color=P.text){for(const a of s.toUpperCase()){const g=fonts[a];if(g)g.forEach((r,dy)=>[...r].forEach((p,dx)=>{if(p==='1')c.pixel(x+dx,y+dy,color);}));x+=a===' '?4:6;}}
const demo=canvas(304,272);
text(demo,'Сборщик',35,7);text(demo,'Товары',13,21);text(demo,'Оплата',13,143);text(demo,'Инвентарь',13,183);
text(demo,'К выплате',192,34,P.gold);text(demo,'Ваш лимит',192,60,P.mute);text(demo,'Бюджет рынка',192,86,P.mute);
text(demo,'Забрать товар',202,135);text(demo,'Готово',192,158,P.blue);
text(demo,'1',213,45);text(demo,'2',246,45);text(demo,'8',278,45);
text(demo,'2',213,71);text(demo,'4',246,71);
text(demo,'1',213,97);text(demo,'2',246,97);

const source=out+'/shipping-ui.aseprite';op('create_sprite',{outputPath:source,width:304,height:272});
op('set_layer_properties',{inputPath:source,layerIndex:1,name:'01 wood and iron'});
op('draw_grid',{inputPath:source,layerIndex:1,...ui.grid()});
op('add_layer',{inputPath:source,name:'02 native slots'});op('draw_grid',{inputPath:source,layerIndex:2,...wells.grid()});
// The production PNG has neither text nor demonstration content.
op('export_png',{inputPath:source,outputPath:out+'/shipping-ui.png'});
op('add_layer',{inputPath:source,name:'03 preview labels only'});op('draw_grid',{inputPath:source,layerIndex:3,...demo.grid()});
op('add_layer',{inputPath:source,name:'04 preview native items only'});
const currency=resolve(root,'../../../Mods/JEMCurrencyCore/src/main/resources/assets/jem_economy/textures/item').replaceAll('\\','/');
for(const [names,y]of [[['gold','silver','copper'],40],[['aether','gold'],66],[['arcane','aether'],92]])for(const[i,name]of names.entries())op('draw_image',{inputPath:source,layerIndex:4,imagePath:currency+'/'+name+'_coin_front.png',x:192+i*32,y});
const nativeItems=resolve(root,'../../../Server/local/shipping-art-20261020/native-reference-items').replaceAll('\\','/');
for(const[i,name]of ['wheat','iron_ingot','carrot'].entries())op('draw_image',{inputPath:source,layerIndex:4,imagePath:nativeItems+'/'+name+'.png',x:14+18*i,y:30});
const iconSource=out+'/collector-icon.aseprite';op('create_sprite',{outputPath:iconSource,width:16,height:16});op('draw_grid',{inputPath:iconSource,...icon.grid()});op('export_png',{inputPath:iconSource,outputPath:out+'/collector-icon.png'});
op('draw_image',{inputPath:source,layerIndex:4,imagePath:out+'/collector-icon.png',x:13,y:3});
op('export_png',{inputPath:source,outputPath:out+'/shipping-ui-preview-1x.png'});
op('render_sprite',{inputPath:source,scale:4,background:'#1B1B1D',outputPath:root+'/shipping-ui-preview-4x.png'});

const controls=canvas(128,128);for(const [i,state]of ['normal','hover','focus','pressed','disabled'].entries())button(controls,23,i*25,103,state);
slot(controls,0,0);slot(controls,0,22,true);const kit=out+'/controls.aseprite';op('create_sprite',{outputPath:kit,width:128,height:128});op('draw_grid',{inputPath:kit,...controls.grid()});op('export_png',{inputPath:kit,outputPath:out+'/controls.png'});
op('render_sprite',{inputPath:kit,scale:4,background:'#1B1B1D',outputPath:root+'/controls-preview-4x.png'});

for(const face of ['front','side','top']){
 const c=canvas(16,16);c.rect(0,0,16,16,P.wood0);c.rect(1,1,14,14,P.wood2);
 if(face==='top'){
  c.rect(2,2,12,12,P.deep);c.rect(2,2,12,2,P.wood1);c.rect(2,2,2,12,P.wood1);
  c.line(1,0,14,P.wood4);c.line(1,15,14,P.wood1);
 }else{
  for(const y of [1,5,9,13]){c.line(1,y,14,P.wood3);c.line(1,y+3,14,P.wood1);}
  c.rect(0,0,2,16,P.iron1);c.rect(14,0,2,16,P.iron1);c.pixel(0,2,P.iron3);c.pixel(15,2,P.iron3);c.pixel(0,13,P.iron2);c.pixel(15,13,P.iron2);
  if(face==='front'){c.rect(5,7,6,3,P.wood0);c.line(6,7,4,P.iron2);}
 }
 const f=out+'/block-'+face+'.aseprite';op('create_sprite',{outputPath:f,width:16,height:16});op('draw_grid',{inputPath:f,...c.grid()});op('export_png',{inputPath:f,outputPath:out+'/block-'+face+'.png'});
}
await writeFile(root+'/aseprite-operations.json',JSON.stringify(operations));
console.log(JSON.stringify({operations:operations.length,request:root+'/aseprite-operations.json',nativeSize:'304x272',source}));
