// One layout/asset owner. All raster operations and native-font painting run in Aseprite.
import{readFile,writeFile,mkdir}from'node:fs/promises';import{dirname,resolve}from'node:path';import{fileURLToPath}from'node:url';
const root=dirname(fileURLToPath(import.meta.url)).replaceAll('\\','/');
const project=resolve(root,'../../..').replaceAll('\\','/');
const native=project+'/Server/local/settlements-art-20261020/native';
const currency=project+'/Mods/JEMCurrencyCore/src/main/resources/assets/jem_economy/textures/item';
const lang=JSON.parse(await readFile(project+'/Mods/JEMSettlements/src/main/resources/assets/jem_settlements/lang/ru_ru.json','utf8'));
await mkdir(root+'/native',{recursive:true});await mkdir(root+'/pages',{recursive:true});
const P={edge:'#25272B',slate:'#343C43',steel:'#6F7B80',shine:'#ABB1A8',wood:'#88623D',darkWood:'#4E382B',woodHi:'#C29A60',paper:'#CEC4A7',paperHi:'#E5DABC',ink:'#332D2B',muted:'#71644F',white:'#F1E6CF',gold:'#E1BF68',blue:'#83B0B0',green:'#799F59',red:'#B85E51',well:'#4A483F'};
const authoredNames=new Set(['chest','red_bed','shield','oak_stairs','happiness','satiety','housing','safety','jobs','community','comfort','residents','unhappy']);
const authoredIcons=[
 {name:'chest',shapes:[[1,5,14,10,'#30251F'],[2,6,12,8,'#956637'],[2,6,12,2,'#C49B58'],[2,9,12,1,'#4C3325'],[7,8,2,4,'#D8C691'],[1,5,1,10,'#5D4E3E'],[14,5,1,10,'#5D4E3E'],[3,3,10,2,'#B78548']]},
 {name:'red_bed',shapes:[[1,8,14,5,'#603E2C'],[2,6,12,5,'#C9B79E'],[5,6,9,4,'#A53D36'],[5,6,9,1,'#D96D53'],[2,6,3,4,'#E2DCC2'],[1,12,2,3,'#70492F'],[13,12,2,3,'#70492F']]},
 {name:'shield',shapes:[[3,1,10,11,'#252D33'],[4,2,8,10,'#A9AFA6'],[5,3,6,9,'#9D7149'],[5,3,1,9,'#C6A06C'],[4,11,8,2,'#78858A'],[5,13,6,1,'#78858A'],[6,14,4,1,'#39464C'],[7,5,2,5,'#5D4533']]},
 {name:'oak_stairs',shapes:[[1,11,14,4,'#513B2C'],[2,8,13,6,'#9B7447'],[6,5,9,9,'#9B7447'],[10,2,5,12,'#9B7447'],[1,10,5,2,'#C4A16C'],[5,7,5,2,'#C4A16C'],[9,4,6,2,'#C4A16C'],[10,1,5,2,'#C4A16C'],[14,2,1,12,'#694B32']]}
];
// Semantic 16px glyphs: distinct silhouettes and value clusters, not item replacements.
const outline='#302B2C', light='#F3D696', shade='#A66B38', green='#76974E';
authoredIcons.push(
 {name:'happiness',shapes:[[4,1,8,1,outline],[2,2,12,2,outline],[1,4,14,8,outline],[2,12,12,2,outline],[4,14,8,1,outline],[4,2,8,12,shade],[2,4,12,8,shade],[4,3,8,9,'#E0B958'],[3,4,10,7,'#E0B958'],[4,3,6,2,light],[4,6,2,2,outline],[10,6,2,2,outline],[4,10,2,1,outline],[6,11,4,1,outline],[10,10,2,1,outline]]},
 {name:'unhappy',shapes:[[4,1,8,14,outline],[2,3,12,10,outline],[4,2,8,12,'#B8795C'],[3,4,10,8,'#CE9F70'],[4,6,2,2,outline],[10,6,2,2,outline],[6,10,4,1,outline],[4,11,2,1,outline],[10,11,2,1,outline]]},
 {name:'satiety',shapes:[[2,4,11,2,outline],[1,6,14,7,outline],[3,3,7,1,outline],[3,5,10,6,'#B77A38'],[2,7,11,4,'#D5A452'],[3,6,9,2,light],[2,12,12,1,shade],[5,5,1,3,shade],[9,5,1,3,shade],[13,8,1,4,shade]]},
 {name:'housing',shapes:[[6,1,4,1,outline],[4,2,8,2,outline],[2,4,12,2,outline],[1,6,14,2,outline],[3,8,10,7,outline],[6,3,4,2,'#C76F4B'],[4,5,8,2,'#A34F38'],[4,8,8,6,'#D2B17A'],[5,8,3,2,light],[8,10,3,4,'#694A34'],[5,11,2,2,'#4D6468']]},
 {name:'safety',shapes:[[3,1,10,11,outline],[4,2,8,10,'#AEBBB6'],[5,3,6,9,'#56787B'],[4,11,8,2,'#768C8D'],[5,13,6,1,outline],[6,14,4,1,outline],[7,4,2,7,light],[5,6,6,2,light]]},
 {name:'jobs',shapes:[[7,2,3,12,outline],[8,4,1,10,'#B28B59'],[2,1,10,5,outline],[3,2,8,3,'#A1AAA4'],[3,2,7,1,'#D0D6BF'],[11,3,3,4,outline],[11,3,2,3,'#73858C'],[3,12,3,3,outline],[2,13,3,2,'#B38F55']]},
 {name:'community',shapes:[[2,2,5,5,outline],[9,3,5,5,outline],[3,3,3,3,'#D4A97B'],[10,4,3,3,'#A6B895'],[1,8,7,7,outline],[8,9,7,6,outline],[2,9,5,5,'#8A5F8D'],[9,10,5,4,'#5F8990'],[3,9,3,2,'#C494B0'],[10,10,3,1,'#91BCB5']]},
 {name:'residents',shapes:[[5,1,6,7,outline],[6,2,4,5,'#D4AC7A'],[4,2,8,2,'#876B43'],[2,9,12,6,outline],[3,10,10,4,'#708B56'],[6,10,4,2,'#C3C58D'],[7,6,3,1,'#8A5B3B']]},
 {name:'comfort',shapes:[[1,12,14,3,outline],[3,13,10,1,'#8B6A43'],[6,3,4,10,outline],[7,4,2,9,green],[2,6,5,4,outline],[3,7,4,2,'#9BB85D'],[9,1,5,6,outline],[10,2,3,4,'#A7BB60'],[10,2,2,1,'#DAE6A3']]}
);
const pages=[];let p;
function page(id,title,section='settlement',route='',size=[512,320]){
 p={id,title,section,route,width:size[0],height:size[1],source:root+'/native/'+id+'.aseprite',background:root+'/native/'+id+'.png',preview:root+'/pages/'+id+'.png',commands:[],controls:[]};pages.push(p);return p;
}
function rect(x,y,w,h,color,layer=1){p.commands.push({type:'rect',x,y,w,h,color,layer});}
function text(s,x,y,w=180,color=P.ink,h=8,wrap=false,align){s=s.replaceAll('“','"').replaceAll('”','"').replaceAll('«','"').replaceAll('»','"');p.commands.push({type:'text',text:s,x,y,w,h,wrap,color,align,layer:3});}
function icon(name,x,y){p.commands.push({type:'image',path:authoredNames.has(name)?root+'/native/icon-'+name+'.png':native+'/'+name+'.png',x,y,layer:3});}
function coin(name,x,y){p.commands.push({type:'image',path:currency+'/'+name+'_coin_front.png',x,y,layer:3});}
function frame(x,y,w,h,light=false){
 rect(x+3,y,w-6,h,P.edge);rect(x,y+3,w,h-6,P.edge);rect(x+2,y+2,w-4,h-4,light?P.paper:P.darkWood);
 rect(x+4,y+2,w-8,1,light?P.paperHi:P.woodHi);rect(x+2,y+4,1,h-8,light?P.paperHi:P.wood);rect(x+3,y+h-3,w-6,1,light?'#9A8865':'#302C2A');
 // Folded corners and interrupted inner seams, kept outside the content inset.
 rect(x+3,y+3,3,1,light?'#DCCDAC':P.wood);rect(x+w-6,y+h-4,3,1,light?'#AF9B76':P.wood);
}
function button(label,x,y,w=100,state='normal',id=label){
 rect(x,y,w,20,P.edge,2);rect(x+1,y+1,w-2,18,P.slate,2);rect(x+2,y+2,w-4,15,state==='disabled'?'#6C6B60':P.wood,2);
 rect(x+3,y+2,w-6,1,state==='hover'?P.paperHi:P.woodHi,2);rect(x+3,y+17,w-6,1,P.darkWood,2);
 rect(x+2,y+4,2,11,'#5A4535',2);rect(x+w-4,y+4,2,11,'#5A4535',2);
 rect(x+6,y+15,Math.max(1,w-12),1,'#765331',2);rect(x+w-12,y+4,5,1,'#AA804D',2);
 if(state==='focus'){rect(x+4,y+1,w-8,1,P.gold,2);rect(x+4,y+18,w-8,1,P.gold,2);}
 text(label,x+5,y+6,w-10,state==='disabled'?P.muted:P.white,8,false,'center');p.controls.push({id,x,y,w,h:20,state});
}
function field(label,value,x,y,w){text(label,x,y,w,P.muted);frame(x,y+12,w,21,true);text(value,x+6,y+18,w-12);p.controls.push({id:label,x,y:y+12,w,h:21,kind:'EditBox'});}
function money(label,groups,x,y,w=170){
 text(label,x,y,w,P.muted);let cursor=x;
 for(const[n,count]of groups){coin(n,cursor,y+11);text(String(count),cursor+20,y+16,24,P.ink);cursor+=count>=10?48:40;}
}
const metricIcons={'Счастье':'happiness','Сытость':'satiety','Жильё':'housing','Питание':'satiety','Безопасность':'safety','Работа':'jobs','Сообщество':'community','Обстановка':'comfort','Уют':'comfort'};
function meter(label,val,max,x,y,w,color=P.green){const name=metricIcons[label];if(name)icon(name,x,y-3);const tx=x+(name?21:0);text(label,tx,y,w-50-(tx-x),P.ink);text(val+' / '+max,x+w-52,y,52,P.muted,8,false,'right');rect(tx,y+12,w-(tx-x),7,P.edge,2);rect(tx+1,y+13,w-(tx-x)-2,5,P.well,2);rect(tx+1,y+13,Math.floor((w-(tx-x)-2)*Math.max(0,Math.min(max,val))/max),5,color,3);}
function slot(x,y){rect(x,y,18,18,'#7C7667',2);rect(x,y,17,17,'#35352F',2);rect(x+1,y+1,16,16,P.well,2);rect(x+1,y+17,17,1,P.paperHi,2);}
function rule(y,x=122,w=376){rect(x,y,w,1,'#AD9E7F',2);}
function settlementRelief(x,y){
 // Original small carved village emblem; no world map or live-town geometry claim.
 const c='#493B2D',hi='#C5A16D',roof='#5D6252',wall='#B99665';
 rect(x,y+20,92,2,c);rect(x+6,y+19,75,1,hi);
 rect(x+8,y+5,3,15,c);rect(x+4,y+4,12,7,'#69744A');rect(x+6,y+2,8,4,'#8A9155');rect(x+5,y+10,10,3,'#53653E');
 rect(x+27,y+10,23,10,c);rect(x+29,y+10,19,9,wall);rect(x+31,y+12,4,4,roof);rect(x+39,y+12,5,7,c);
 rect(x+25,y+8,27,3,c);rect(x+28,y+5,21,3,roof);rect(x+32,y+2,13,3,roof);rect(x+30,y+5,17,1,'#899078');rect(x+35,y+2,7,1,'#A4A282');
 rect(x+58,y+7,17,13,c);rect(x+60,y+8,13,11,wall);rect(x+62,y+10,3,4,roof);rect(x+68,y+14,3,5,c);rect(x+55,y+5,23,3,c);rect(x+59,y+2,15,3,roof);rect(x+62,y,9,2,roof);rect(x+60,y+3,12,1,'#9C9E80');
 rect(x+83,y+11,2,9,c);rect(x+79,y+8,9,6,'#5C7044');rect(x+81,y+6,6,3,'#87914F');
}
function shell(section=p.section){
 frame(1,3,p.width-2,p.height-5);rect(4,34,102,p.height-40,'#303A3E');
 // GUI+05: dark masonry, chipped contour and selective moss at material joints.
 for(let y=37,i=0;y<p.height-9;y+=27,i++){
  rect(6,y,98,24,i%2?'#3E484A':'#374144');rect(7,y,94,1,'#586164');rect(6,y+23,98,1,'#202B30');
  const seam=i%2?39:72;rect(seam,y+1,2,22,'#293235');rect(seam+2,y+2,1,18,'#4A5555');
 }
 rect(4,42,3,9,'#738456');rect(5,51,2,12,'#4E6949');rect(100,281,4,7,'#677C4E');
 // GUI+06: layered, deliberately shaped boards with broad grain clusters.
 rect(8,1,p.width-22,3,P.edge);rect(4,5,p.width-8,28,P.edge);
 rect(9,4,p.width-22,10,'#9A7547');rect(5,14,p.width-10,15,'#805A37');rect(11,29,p.width-28,4,'#503B2D');
 rect(13,5,p.width-32,2,'#C6A16A');rect(8,16,p.width-18,1,'#AE8552');rect(16,28,p.width-40,1,'#4B372B');
 rect(55,8,57,1,'#AF8956');rect(118,10,31,1,'#765331');rect(p.width-146,22,57,1,'#AC814A');rect(p.width-76,25,41,1,'#5F422E');
 rect(p.width-111,5,2,8,'#624730');rect(p.width-110,11,11,1,'#624730');rect(204,18,16,1,'#654630');rect(204,18,2,6,'#654630');
 settlementRelief(p.width-112,7);
 frame(111,37,p.width-116,p.height-42,true);
 for(const[x,y]of[[3,3],[p.width-9,3],[3,p.height-9],[p.width-9,p.height-9]]){rect(x,y,6,6,P.steel);rect(x,y,5,1,P.shine);rect(x+1,y+2,2,2,P.edge);}
 icon('bell',12,11);text('Речная долина',36,14,310,P.white);text(p.title,122,46,p.width-140,P.ink);
 const labels=[['settlement','Поселение','oak_door'],['residents','Жители','bell'],['happiness','Счастье','sunflower'],['chests','Сундуки','chest']];
 labels.forEach(([id,label,n],i)=>{const y=58+i*39;if(id===section){rect(9,y,91,30,'#302D2C',2);rect(11,y+1,87,27,'#834D3E',2);rect(12,y+2,84,1,'#BE8A58',2);rect(13,y+4,1,22,'#C39C64',2);rect(17,y+28,39,3,'#623D34',2);rect(58,y+28,37,2,'#623D34',2);rect(95,y+2,2,24,'#50342F',2);}icon(n,14,y+8);text(label,35,y+12,63,P.white);p.controls.push({id:'nav.'+id,x:8,y,w:94,h:32});});
 button('Справка',8,p.height-53,94);button('Поселения',8,p.height-28,94);
}
function row(name,sub,n,x,y,w,selected=false){
 if(selected){rect(x,y,w,32,'#B9B598',2);rect(x,y,2,32,P.blue,2);}icon(n,x+6,y+7);text(name,x+29,y+5,w-36);text(sub,x+29,y+19,w-36,P.muted);rule(y+32,x,w);
}
function overview(){
 shell();const labels=['Жители','Счастье','Уют','Казна'];const vals=['12 / 12','84 / 100','75 / 100',''];
 labels.forEach((s,i)=>{const x=122+i*94;frame(x,63,88,49,true);text(s,x+6,69,76,P.muted);if(i<3){icon(['residents','happiness','comfort'][i],x+6,84);text(vals[i],x+28,88,54);}else{coin('gold',x+5,84);text('12',x+25,89,48);}});
 frame(122,119,376,92,true);money('Баланс казны',[['aether',1],['gold',2]],130,126,170);text('Доход за Minecraft-день',320,126,170,P.muted);text('Жители: 68    Уют: 12',320,140,170);text('Всего: 80 медных',320,152,170);
 button('Налог: 5%',130,177,85);button('Монета',223,177,65);frame(293,177,55,20,true);text('10',300,183,40);button('Внести',354,177,65);button('Вывести',425,177,65);
 text('Обустройство',130,220,360);['Дорожки  20/25','Животные  25/25','Свет  15/25','Зелень  15/25'].forEach((s,i)=>text(s,130+(i%2)*184,235+Math.floor(i/2)*15,174,P.muted));
 frame(122,272,230,22,true);text('Речная долина',129,279,215);button('Название',358,273,67);button('Удалить',431,273,67);
}
page('01-directory','Выбор поселения','', 'mode=selector');shell('');field('Поиск','Речная',122,66,376);
[['Речная долина','Обычный мир · 12 жителей · 84 счастья'],['Сосновый берег','Обычный мир · 8 жителей · 70 счастья'],['Тихий луг','Обычный мир · 5 жителей · 76 счастья']].forEach(([n,s],i)=>row(n,s,'bell',122,115+i*42,376,i===0));button('Основать поселение',330,274,168);text('Всего поселений: 3',130,280,180,P.muted);
page('02-founding','Основание поселения','', 'mode=create / TownFoundingScreen');shell('');field('Название поселения','Речная долина',122,68,376);icon('bell',132,119);text('Колокол найден',156,121,316);text('Обычный мир · 124, 64, -208',132,143,350,P.muted);text('Начальная территория: 97 × 97 блоков',132,161,350);money('Стоимость основания',[['gold',3]],132,191);button('Основать',122,274,182);button('Назад',312,274,186);
page('03-overview','Обзор поселения','settlement','section=settlement / cityWidgets');overview();
page('04-residents','Жители поселения','residents','section=residents / residentsDashboard');shell();
field('Поиск','',122,65,376);
frame(122,108,177,190,true);frame(307,108,191,190,true);
[['Иван','Фермер · 84/100','wheat'],['Мария','Библиотекарь · 92/100','book'],['Степан','Кузнец · 73/100','iron_pickaxe'],['Андрей','Рекрут · 80/100','shield']].forEach(([n,s,i],k)=>row(n,s,i,128,116+k*40,165,k===0));
icon('wheat',319,119);text('Иван',343,121,145);text('Фермер',319,141,167,P.muted);meter('Счастье',84,100,319,164,167);meter('Сытость',90,100,319,191,167);text('Нет жалоб',319,219,167,P.green);text('Желает: хлеб',319,235,167);button('Покормить',319,249,80);button('Мой дом',407,249,79);button('Исключить',319,274,167);
function detail(kind){shell('residents');const isRecruit=kind==='recruit',golem=kind==='golem';icon(golem?'shield':isRecruit?'iron_sword':'wheat',132,73);text(golem?'Железный голем':isRecruit?'Андрей':'Иван',156,75,300);text(golem?'Защитник поселения':isRecruit?'Рекрут · родное управление':'Фермер · обычный житель',132,96,352,P.muted);
 if(golem){text('Здоровье: 100 / 100',132,124,352);text('Обычный мир · 132, 64, -212',132,147,352,P.muted);text('Голем учитывается в безопасности.',132,176,352,P.ink,24,true);button('Координаты',122,274,182);button('Назад',312,274,186);return;}
 meter('Счастье',84,100,132,124,350);meter('Сытость',90,100,132,153,350);
 if(isRecruit){money('Жалованье за 7 Minecraft-дней',[['silver',2]],132,187);button('Оплатить долг',312,204,174,'disabled');text('Долгов по наступившим выплатам нет',132,239,350,P.muted);button('Покормить',122,274,182);button('Управлять',312,274,186);}
 else{text('Желает: хлеб',132,187,350);text('Дом: 128, 64, -210',132,207,350,P.muted);text('Рабочее место: 130, 64, -211',132,223,350,P.muted);text('Сегодня: хлеб ×1',132,244,350);button('Покормить',122,274,120);button('Мой дом',250,274,120);button('Исключить',378,274,120);}}
page('05-villager','Карточка жителя','residents','residentPane / residentDetail');detail('villager');
page('06-recruit','Карточка рекрута','residents','residentPane / isRecruit');detail('recruit');
page('07-golem','Карточка защитника','residents','residentPane / guard without needs');detail('golem');
page('08-happiness','Счастье поселения','happiness','happinessRows top');shell();
[['Счастье','84/100','happiness'],['Сытость','90/100','satiety'],['Недовольны','1/12','unhappy'],['Уют','75/100','comfort']].forEach(([s,v,n],i)=>{const x=122+i*94;frame(x,64,88,43,true);text(s,x+6,71,76,P.muted);icon(n,x+6,85);text(v,x+28,89,54);});
[['Жильё',18,20],['Питание',18,20],['Безопасность',9,10],['Работа',9,10],['Сообщество',14,15],['Обстановка',12,15]].forEach(([n,v,m],i)=>meter(n,v,m,130,118+i*26,357));text('PatPat: личный вклад до 10',130,282,265,P.muted);text('Налоговый штраф: -3',130,295,265,P.muted);
page('09-happiness-details','Жильё и распределение','happiness','happinessRows lower scroll');shell();text('Жилищные жалобы',130,69,350);
[['Нет кровати','1 житель','red_bed'],['Нет крыши','0 жителей','oak_stairs'],['Теснота','0 жителей','oak_door'],['Темнота','2 жителя','lantern']].forEach(([a,b,c],i)=>row(a,b,c,122,89+i*38,376));
text('Распределение жителей',130,255,350);rect(130,273,355,8,P.edge,2);rect(131,274,29,6,P.red,3);rect(160,274,118,6,P.gold,3);rect(278,274,206,6,P.green,3);text('Недовольны 1    Нейтральны 4    Счастливы 7',130,291,357);
page('10-chests','Общие сундуки','chests','section=chests / chestRows');shell();field('Поиск','',122,65,376);
[['Сундук №1','128, 64, -207 · 54 слота'],['Сундук №2','140, 64, -205 · 27 слотов'],['Сундук №3','Запас неизвестен: чанк не загружен']].forEach(([n,s],i)=>row(n,s,'chest',122,116+i*45,376,i===0));text('Контейнеры помечаются колоколом.',130,279,350,P.muted,24,true);
page('11-chest-content','Сундук №1','chests','chestContents / InventoryKnown');shell();icon('chest',130,68);text('128, 64, -207',154,71,330,P.muted);
for(let r=0;r<6;r++)for(let c=0;c<9;c++)slot(220+c*18,100+r*18);
['bread','wheat','carrot','cod','salmon'].forEach((n,i)=>icon(n,221+i*18,101));text('Слоты 1–54 из 54',221,219,170,P.muted);button('Покормить всех',122,274,376);
function dialog(id,title,body,confirm=false){page(id,title,'residents','confirmationWidgets / '+title);shell();frame(167,102,286,145,true);text(title,179,115,262);text(body,179,139,262,P.ink,58,true);button(confirm?'Отмена':'Закрыть',179,214,confirm?125:262);if(confirm)button('Подтвердить',316,214,125);}
dialog('12-home-location','Мой дом','Иван\nДом: 128, 64, -210\nОбычный мир');
dialog('13-delete-confirm','Удалить поселение?','Речная долина\nУдаление не отменяет задолженность рекрутам.',true);
dialog('14-resident-remove','Исключить жителя?','Иван будет исключён из поселения. Его настоящая профессия принадлежит Minecraft.',true);
dialog('15-transfer-confirm','Подтверждение перевода','Внести 10 золотых монет в казну.\nИтог: 1 000 медных эквивалентов.',true);
dialog('16-coordinates','Координаты защитника','Железный голем\n132, 64, -212\nОбычный мир');
const chapters=['founding','territory','feeding','housing','food_satisfaction','safety','jobs','community','decoration','chest_delivery','payroll','rebellion','treasury','tax'];
chapters.forEach((key,i)=>{
 const raw=lang['jem.settlements.guide.'+key];const [title,...body]=raw.split('\n');page('guide-'+String(i+1).padStart(2,'0')+'-'+key,title,'','guideWidgets / chapter='+key);shell('');
 button('Назад',122,65,67);button('<',197,65,26);button('>',231,65,26);text('Глава '+(i+1)+' / 14',270,71,220,P.muted);
 frame(122,94,376,181,true);text(body.join('\n').replaceAll('игровой день','Minecraft-день').replaceAll('игровых дней','Minecraft-дней').replaceAll('игрового дня','Minecraft-дня'),132,106,356,P.ink,156,true);button('Предыдущая',122,282,182);button('Следующая',312,282,186);
});
for(const[state,title,body]of [['loading','Загрузка','Ожидается снимок поселения.'],['unsupported','Нет поддержки','Сервер не поддерживает интерфейс поселений.'],['pending','Операция выполняется','Ожидается соответствующий ответ сервера.'],['uncertain','Результат неизвестен','Не отправляйте повторный перевод. Требуется сверить результат отправленной операции.'],['empty-residents','Нет жителей','Зарегистрированные жители появятся в этом списке.'],['chest-unloaded','Сундук недоступен','Чанк не загружен. Запас контейнера сейчас неизвестен.']]){
 page('state-'+state,title,state.includes('chest')?'chests':'settlement','runtime state: '+state);shell();frame(132,109,356,120,true);icon(state.includes('chest')?'chest':'book',145,122);text(title,170,125,300);text(body,145,155,326,P.muted,55,true);if(state==='uncertain')button('Сверить результат',145,198,326);
}
// Compact uses the same real content in one column, not a scaled-down bitmap.
page('compact-resident','Карточка жителя','residents','residentDetail / compact',[320,320]);
frame(0,0,320,320);rect(5,5,310,30,P.wood);icon('wheat',13,11);text('Иван · фермер',37,14,260,P.white);frame(8,42,304,268,true);meter('Счастье',84,100,20,62,280);meter('Сытость',90,100,20,96,280);text('Желает: хлеб',20,137,280);text('Дом: 128, 64, -210',20,163,280,P.muted);text('Нет жалоб',20,190,280,P.green);button('Покормить',20,242,132);button('Мой дом',160,242,140);button('Назад',20,276,280);
const settlementPages=[...pages];
const shipping=project+'/Docs/art/shipping-station-2026-10-20';
page('shipping-exchange','Обмен монет','','ShippingBoxScreen / proposed second tab',[368,272]);
p.source=shipping+'/native/shipping-exchange.aseprite';p.background=shipping+'/native/shipping-exchange.png';p.preview=shipping+'/native/shipping-exchange-preview-1x.png';
p.commands.push({type:'image',path:shipping+'/native/shipping-ui.png',x:0,y:0,layer:1});
frame(7,23,174,150);frame(183,23,177,150);
for(const[x,w,active]of [[179,78,false],[263,91,true]]){rect(x,3,w,14,P.edge,2);rect(x+1,4,w-2,12,active?'#65442D':'#3D2C26',2);rect(x+2,4,w-4,1,active?P.gold:P.steel,2);}
p.commands.push({type:'image',path:shipping+'/native/collector-icon.png',x:13,y:1,layer:3});text('Сборщик',35,7,130,P.white);text('Продажа',195,7,66,P.muted);text('Обмен монет',276,7,76,P.white);text('Инвентарь',13,183,156,P.white);
text('Размен',15,29,152,P.white);button('Серебро',15,43,72);button('Золото',96,43,76);
coin('silver',21,66);text('10',43,70,32,P.white);text('>',88,70,16,P.gold);coin('gold',115,66);text('1',138,70,24,P.white);
button('-',15,88,25);frame(47,88,84,20,true);text('10',53,94,72,P.ink,8,false,'center');button('+',138,88,34);
button('Обменять',15,121,157);text('10 серебра = 1 золото',15,153,157,P.white);
text('Получить монеты',192,29,157,P.white);text('Кошелёк',192,47,157,P.white);coin('gold',192,59);text('2',212,63,23,P.white);coin('silver',240,59);text('4',260,63,23,P.white);
text('Снять с кошелька',192,85,157,P.white);frame(192,98,157,20,true);coin('gold',200,100);text('1',223,104,110);button('Получить монеты',192,126,157);text('В ваш инвентарь',192,155,157,P.white);
const manifest={fonts:native+'/fonts.json',authoredIcons:authoredIcons.map(x=>({...x,source:root+'/native/icon-'+x.name+'.aseprite',output:root+'/native/icon-'+x.name+'.png'})),pages};
await writeFile(root+'/pages-spec.json',JSON.stringify(manifest));
await writeFile(root+'/layout-manifest.json',JSON.stringify({canvas:'native pixels',pages:pages.map(({commands,...x})=>x),source:'paint-pages.mjs; matching render/hit rectangles',data:'illustrative examples; no server snapshot'},null,2));
const cards=settlementPages.map(x=>`<a href="pages/${x.id}.png" target="_blank"><img loading="lazy" src="pages/${x.id}.png" alt="${x.title}"><span>${x.title}</span><small>${x.route}</small></a>`).join('\n');
await writeFile(root+'/index.html',`<!doctype html><html lang="ru"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>Settlements — все страницы</title><style>body{background:#17191c;color:#eee;font:16px system-ui;margin:24px;max-width:1500px}h1{font-size:26px}p{color:#bbb}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(480px,1fr));gap:20px}a{display:block;color:#eee;text-decoration:none;border:1px solid #47423c;padding:12px;background:#24272b}img{width:100%;height:auto;image-rendering:pixelated}span,small{display:block;margin-top:8px}small{color:#aeb0ad;font-size:12px}@media(max-width:520px){main{display:block}a{margin-bottom:20px}}</style><h1>Settlements — ${settlementPages.length} страниц и состояний</h1><p>Нарисовано в Aseprite в целевом PNG-размере. Макеты с данными-примерами, в игру не установлены. Нажми на страницу, чтобы открыть оригинал.</p><main>${cards}</main></html>`);
console.log(JSON.stringify({pages:pages.length,spec:root+'/pages-spec.json',gallery:root+'/index.html'}));
