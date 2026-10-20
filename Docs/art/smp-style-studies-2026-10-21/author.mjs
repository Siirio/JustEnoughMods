// Native-size paint instructions only. Aseprite creates every raster pixel/export.
import {mkdir,readFile,writeFile} from 'node:fs/promises';
import {dirname} from 'node:path';import {fileURLToPath} from 'node:url';
const root=dirname(fileURLToPath(import.meta.url)).replaceAll('\\','/');
const project='C:/MyProjects/Minecraft/JustEnoughMods';
const native=project+'/Server/local/settlements-art-20261020/native';
const pages=[];let p;
function r(x,y,w,h,color,layer=1){p.commands.push({type:'rect',x,y,w,h,color,layer});}
function t(text,x,y,color='#F2E5CA',scale=1,w=500){p.commands.push({type:'text',text,x,y,color,scale,w,layer:5});}
function icon(name,x,y){
 if(['head','people','group'].includes(name)){
  const head=(hx,hy)=>{r(hx,hy,7,8,'#252629',4);r(hx+1,hy+1,5,6,'#C79568',4);r(hx+1,hy,5,2,'#63452D',4);r(hx+1,hy+3,1,1,'#E0DED0',4);r(hx+5,hy+3,1,1,'#E0DED0',4);r(hx+3,hy+5,2,1,'#80553D',4);};
  if(name==='head'){head(x+4,y+1);r(x+2,y+9,12,6,'#435F6B',4);r(x+3,y+10,4,2,'#76928F',4);}
  else{if(name==='group'){head(x+5,y);r(x+3,y+7,9,7,'#526B47',4);}head(x,y+4);head(x+9,y+4);r(x,y+12,8,4,'#84646D',4);r(x+9,y+12,7,4,'#547583',4);r(x+1,y+12,4,1,'#B79293',4);r(x+10,y+12,4,1,'#8DB0AD',4);}
  return;
 }
 p.commands.push({type:'image',path:native+'/'+name+'.png',x,y,layer:4});
}
const ink='#1A2022',dark='#2B2420',gold='#E3BB70';
function pin(x,y,c='#9CA9A8'){r(x,y,4,4,ink,3);r(x,y,3,1,'#D6D9C5',3);r(x+1,y+1,2,2,c,3);r(x+2,y+2,1,1,'#435255',3);}
function board(x,y,w,h,mode='oak'){
 const c=mode==='oak'?['#32271F','#65452E','#947044','#C69D61']:['#20282C','#3B484B','#57686A','#92A2A0'];
 // Broad cut face, upper plane, occlusion and asymmetric end joints.
 r(x+3,y,w-8,h,c[0]);r(x,y+3,w,h-7,c[0]);r(x+2,y+2,w-5,h-5,c[1]);r(x+3,y+3,w-7,h-8,c[2]);
 r(x+5,y+2,w-11,1,c[3]);r(x+2,y+5,1,h-12,c[3]);r(x+4,y+h-4,w-8,2,c[0]);
 r(x+14,y+5,19,1,c[1]);r(x+30,y+6,21,1,c[1]);r(x+w-76,y+h-9,24,1,c[1]);
 r(x+w-55,y+h-10,17,1,c[1]);r(x+w-9,y+4,2,6,c[0]);r(x+w-15,y+9,7,1,c[0]);
}
function edge(x,y,w,h,c='#657376',fill='#252B2D'){
 r(x+2,y,w-4,h,ink,2);r(x,y+2,w,h-4,ink,2);r(x+1,y+2,w-2,h-4,c,2);
 r(x+3,y+3,w-6,h-6,fill,2);r(x+3,y+2,w-6,1,'#A8ADA0',2);r(x+2,y+h-3,w-4,1,'#131A1D',2);
}
function bracket(x,y,flip=false){
 const d=flip?-1:1;
 for(let i=0;i<17;i++){r(x+d*i,y,1,4,'#3B474A',3);r(x,y+i,4,1,'#3B474A',3);}
 r(flip?x-15:x+1,y,15,1,'#9CA8A4',3);r(x,y+1,1,14,'#7C8988',3);pin(flip?x-3:x+2,y+3);
}
function leaf(x,y,flip=false){
 const d=flip?-1:1;
 for(const [dx,dy,w,h,c]of [[0,0,2,14,'#334A35'],[1,2,6,3,'#425C38'],[3,0,4,3,'#6C8548'],[-4,6,5,3,'#6C8548'],[-6,8,4,2,'#92A35B'],[1,10,5,3,'#526E3B'],[4,9,4,2,'#8B9D52']])r(x+d*dx,y+dy,w,h,c,3);
}
function cloth(x,y,w,h,c='#843D36',hi='#BB6350'){
 r(x,y,w,h-6,'#30252A',2);r(x+1,y+1,w-2,h-8,c,2);r(x+2,y+2,1,h-10,hi,2);r(x+w-4,y+2,2,h-10,'#5A2E30',2);
 for(let j=0;j<6;j++){r(x+j,y+h-7+j,w-j*2,1,'#30252A',2);if(j<4)r(x+2+j,y+h-7+j,w-4-j*2,1,c,2);}
}
function paper(x,y,w,h){
 r(x+2,y+2,w,h,'#45382B',2);r(x,y,w,h,'#A0875E',2);r(x+1,y+1,w-2,h-2,'#D4C39B',2);r(x+3,y+3,w-6,h-6,'#E9DABB',2);
 r(x+4,y+2,w-11,1,'#F6E9CA',2);r(x+1,y+8,2,h-19,'#B7A079',2);
 r(x+w-9,y,9,8,'#9F8661',2);r(x+w-9,y+1,7,5,'#F8E9C8',2);r(x+w-9,y+6,6,1,'#C1AB84',2);
 r(x+7,y+h-2,15,1,'#AF966B',2);r(x+23,y+h-3,7,1,'#C5AD82',2);
}
function button(label,x,y,w,theme,selected=false){
 const fill=theme===2?(selected?'#446C58':'#8E6940'):theme===3?(selected?'#35646B':'#455052'):(selected?'#87543A':'#6A4E35');
 edge(x,y,w,22,theme===2?'#8F774F':'#707B78',fill);
 r(x+4,y+4,w-8,1,selected?'#C7CC96':theme===2?'#B99A60':'#8D9A8F',3);
 if(selected){r(x+4,y+6,2,10,gold,3);r(x+7,y+10,2,3,gold,3);}
 t(label,x+Math.floor((w-label.length*6)/2),y+8,'#F4E8CE',1,w-12);
 p.controls.push({label,x,y,w,h:22,kind:'style-only'});
}
function moon(x,y){
 // Original 72x64 event vignette: stepped full moon, distant ridge and fir silhouettes.
 r(x,y,72,64,'#261F2A',4);r(x,y+35,72,29,'#3A2932',4);
 for(const [dx,dy,w,h,c]of [[26,5,22,2,'#8B3D42'],[21,7,32,4,'#B55350'],[18,11,38,7,'#CE6B58'],[16,18,42,18,'#D47A61'],[18,36,38,6,'#B55350'],[22,42,30,3,'#8F4144'],[28,45,18,2,'#60303B'],[24,13,12,4,'#E8A076'],[20,20,10,8,'#E6A279'],[40,17,9,5,'#A44B4B'],[37,31,13,5,'#B25A51'],[29,35,5,4,'#EFAC78'],[49,25,5,9,'#A14B4B']])r(x+dx,y+dy,w,h,c,4);
 for(const [dx,dy,w,h]of [[0,45,10,19],[8,41,10,23],[17,47,13,17],[28,43,14,21],[40,48,13,16],[51,39,10,25],[60,43,12,21]])r(x+dx,y+dy,w,h,'#1C2427',4);
 for(const [dx,dy]of [[4,29],[58,31],[65,26]]){
  r(x+dx,y+dy,2,30,'#111B20',4);r(x+dx-2,y+dy+5,6,3,'#111B20',4);r(x+dx-4,y+dy+11,10,4,'#111B20',4);r(x+dx-6,y+dy+18,14,4,'#111B20',4);
 }
 r(x+23,y+56,17,3,'#76544E',4);r(x+29,y+54,7,2,'#A16D5A',4);
}
function smallArt(type,x,y){
 if(type==='raid'){
  r(x,y,38,28,'#283235',4);r(x+5,y+10,28,18,'#526066',4);r(x+8,y+5,6,10,'#6A7978',4);r(x+24,y+5,6,10,'#6A7978',4);r(x+13,y+14,12,14,'#172326',4);r(x+9,y+10,4,3,'#A8B2A1',4);r(x+25,y+10,4,3,'#A8B2A1',4);r(x+17,y+19,4,9,'#B78043',4);
 }else{
  r(x,y,38,28,'#2F3A2D',4);r(x+3,y+19,32,9,'#60482E',4);r(x+3,y+18,32,2,'#95A65A',4);icon('diamond',x+4,y+2);icon('iron_pickaxe',x+19,y+8);
 }
}
const nav=[['Профиль','head'],['Игроки','people'],['Группы','group'],['События','iron_sword'],['Рынок','emerald'],['Поселения','bell'],['Награды','diamond'],['Расы','book'],['Профессии','iron_pickaxe']];
function make(id,theme){
 p={id,width:512,height:320,source:root+'/'+id+'.aseprite',preview:root+'/'+id+'-1x.png',background:root+'/'+id+'-surface.png',commands:[],controls:[]};pages.push(p);
 if(theme===1){
  r(9,11,494,300,'#181C1F');r(13,14,486,292,'#3B3025');
  board(5,5,501,38);board(6,302,500,12);board(5,40,12,264);board(493,40,13,264);
  for(const y of [46,98,151,203,256])board(16,y,111,51);
  r(132,46,359,251,'#24282B');r(134,46,2,251,'#6C7878');
  bracket(8,7);bracket(502,7,true);pin(8,299);pin(500,299);
  r(132,38,5,267,'#20282B');r(133,42,2,253,'#798681');
 }else if(theme===2){
  r(10,13,493,294,'#2B251F');r(13,14,486,290,'#6A4C31');
  for(const y of [11,49,88,129,169,210,250,290])board(y===88?5:8,y,497,y===290?22:39);
  board(9,8,492,34);r(126,44,4,255,'#30251E');r(127,45,1,250,'#BD9257');
  paper(138,45,352,250);leaf(491,6,true);leaf(8,276);leaf(116,306,true);
  bracket(10,9);bracket(496,9,true);
 }else{
  r(9,11,494,299,'#181E24');r(13,15,486,289,'#343F43');
  // Different joint offsets belong to stone courses, not a repeated crack stamp.
  for(const [x,y,w,h]of [[16,47,58,48],[76,47,50,48],[16,97,86,49],[104,97,23,49],[16,148,44,48],[62,148,65,48],[16,198,79,49],[97,198,30,49],[16,249,52,49],[70,249,57,49]]){
   r(x,y,w,h,'#232E33');r(x+1,y+1,w-3,h-3,'#3C494D');r(x+2,y+1,w-5,1,'#607070');
  }
  board(6,7,500,35);board(6,303,500,11);board(5,41,10,264);board(496,41,10,264);
  r(130,44,4,256,'#181E24');r(131,45,1,252,'#B18B56');r(137,46,353,250,'#202A30');
  bracket(9,9);bracket(502,9,true);cloth(455,8,25,43,'#35676C','#72A09A');
 }
 // One hand-placed knot and long grain transition, outside the title/control region.
 for(const [x,y,w,h,c]of [[268,15,27,2,'#785332'],[258,17,14,2,'#A57B44'],[274,17,19,1,'#BD9358'],[289,18,25,2,'#60432D'],[259,21,18,1,'#795331'],[278,22,20,2,'#AA814A'],[293,24,17,1,'#785332'],[406,12,34,1,'#B18B50'],[422,14,24,1,'#795331']])r(x,y,w,h,c,3);
 r(9,41,3,4,'#1A2022',3);r(500,43,4,3,'#1A2022',3);
 if(theme===1){
  for(const [x,y,w,h,c]of [[34,94,28,2,'#4D3727'],[29,96,14,2,'#B48B52'],[46,97,29,1,'#BD9259'],[69,98,27,2,'#60432D'],[54,145,20,2,'#65462C'],[74,146,22,1,'#BD965C'],[22,200,29,1,'#C7A16A'],[86,262,26,2,'#4E3828']])r(x,y,w,h,c,3);
  r(16,44,111,4,'#503B29',2);r(16,48,111,1,'#B28B53',2);
 }
 if(theme===2){leaf(489,112,true);leaf(133,213);}
 if(theme===3){r(38,151,1,12,'#1C282D',3);r(39,162,6,1,'#1C282D',3);r(45,163,1,5,'#1C282D',3);r(39,151,1,8,'#657575',3);}
 icon('map',19,17);t('FOREVER SMP',43,16,'#F6E6BD',2,240);t('СОБЫТИЯ',327,22,'#E5C68B');
 edge(477,15,19,19,'#7E897E','#3A322A');t('×',483,21,'#E6D5AE');
 for(let i=0;i<nav.length;i++){
  const y=51+i*24,selected=i===3;
  // Each carved label bed has a fixed quiet baseline; board joints never cross text.
  if(!selected){
   const base=theme===2?'#755435':theme===3?'#34434A':'#62472F';
   r(22,y+1,98,20,base,3);r(21,y+3,1,16,theme===3?'#202C32':'#463426',3);
   r(23,y+1,95,1,theme===2?'#B48D57':theme===3?'#617375':'#A38252',3);
   r(25,y+20,91,1,theme===3?'#26343B':'#483426',3);
  }
  if(selected){
   if(theme===2){cloth(19,y-2,104,30,'#42634C','#91A16A');r(22,y+3,2,14,gold,3);}
   else{edge(20,y-1,103,23,theme===3?'#B18B56':'#9A8562',theme===3?'#35616A':'#775238');r(22,y+3,2,14,gold,3);}
  }else if(theme!==2){r(24,y+21,94,1,theme===3?'#202A2D':'#4A392A',2);}
  icon(nav[i][1],27,y+2);t(nav[i][0],48,y+7,selected?'#F7E8BE':theme===2?'#F0DCB2':'#D7D6BE',1,70);
  p.controls.push({label:nav[i][0],x:20,y:y-1,w:103,h:23});
 }
 button('Назад',21,278,100,theme);
 const text=theme===2?'#3D332A':'#F3E4C9',muted=theme===2?'#78684D':'#A9B3A9';
 t('События сервера',151,57,text,1,220);t('Участвуйте вместе',151,72,muted,1,220);
 r(150,86,324,1,theme===2?'#B8A27B':'#586464',2);
 if(theme===1){edge(147,95,332,110,'#7F8C88','#302E2B');cloth(157,91,80,115);moon(161,101);pin(160,94);pin(227,94);}
 if(theme===2){paper(148,96,331,109);r(153,99,80,100,'#665541',2);moon(157,105);pin(155,97,'#9B865E');pin(226,97,'#9B865E');}
 if(theme===3){edge(147,95,332,110,'#A88858','#28363D');edge(154,101,80,78,'#6D807C','#1A252B');moon(158,105);r(158,180,72,17,'#5C343A',3);t('СОБЫТИЕ',169,185,'#EDB19B');}
 t('Кровавая луна',245,107,theme===2?'#793D35':'#F1B58E',1,220);
 t('Ночь. Волны. Общая награда.',245,124,text,1,222);
 t('До начала: 0,25 Minecraft-дня',245,142,muted,1,222);
 t('Группа участников',245,156,muted,1,220);
 button('Подробнее',245,174,133,theme,true);
 if(theme!==3){t('КРОВАВАЯ',165,175,theme===2?'#F0DDAD':'#EDB19B',1,70);t('ЛУНА',183,187,theme===2?'#F0DDAD':'#EDB19B',1,60);}
 t('Другие события',151,214,text,1,240);
 for(let i=0;i<2;i++){
  const x=148+i*169;
  if(theme===2)paper(x,230,162,56);else edge(x,230,162,56,theme===3?'#657C7C':'#78684E',theme===3?'#2C393D':'#3D352B');
  smallArt(i===0?'raid':'rush',x+6,237);
  t(i===0?'Рейд на босса':'Гонка ресурсов',x+49,239,text,1,106);
  t(i===0?'Соберите группу':'Добывайте вместе',x+49,253,muted,1,108);
  t('Открыть →',x+49,270,theme===2?'#785630':'#E0BC7C',1,106);
  p.controls.push({label:i===0?'Рейд на босса':'Гонка ресурсов',x,y:230,w:162,h:56});
 }
 // Sample-data disclosure belongs to the study caption, outside future game art.
 if(theme===1){pin(137,294);pin(483,294);}
}
make('01-trading-station',1);make('02-notice-board',2);make('03-stone-and-oak',3);
const fonts=JSON.parse(await readFile(native+'/fonts.json','utf8'));
await mkdir(root+'/inputs',{recursive:true});
// Copy only source-owned vanilla font/icon input bytes into the portable art study.
for(const f of fonts){const name=f.local.split('/').at(-1);await writeFile(root+'/inputs/'+name,await readFile(f.local));f.local=root+'/inputs/'+name;}
for(const n of [...new Set(pages.flatMap(p=>p.commands.filter(c=>c.type==='image').map(c=>c.path)))]){
 const dest=root+'/inputs/'+n.split('/').at(-1);await writeFile(dest,await readFile(n));
 for(const page of pages)for(const c of page.commands)if(c.path===n)c.path=dest;
}
await writeFile(root+'/fonts.json',JSON.stringify(fonts));
await writeFile(root+'/study.json',JSON.stringify({fonts:root+'/fonts.json',pages},null,2));
console.log(JSON.stringify({root,pages:pages.map(p=>({id:p.id,commands:p.commands.length}))}));
