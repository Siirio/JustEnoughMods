-- Native Aseprite artwork. Original polygons/planes, not reference tracing or image imports.
local ROOT='C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/aseprite/Projects/JEM-1.20.1/Studies/Smp-trading-cards-2026-10-21'
app.fs.makeAllDirectories(ROOT)
local function rgb(h)h=h:gsub('#','');return app.pixelColor.rgba(tonumber(h:sub(1,2),16),tonumber(h:sub(3,4),16),tonumber(h:sub(5,6),16),255)end
local target;local ox,oy=0,0
local function pixel(x,y,c) x=math.floor(x+ox);y=math.floor(y+oy);if x>=0 and y>=0 and x<target.width and y<target.height then target:drawPixel(x,y,rgb(c))end end
local function box(x,y,w,h,c)for yy=y,y+h-1 do for xx=x,x+w-1 do pixel(xx,yy,c)end end end
local function poly(points,c)
 local minY,maxY=100000,-100000;for _,p in ipairs(points)do minY=math.min(minY,p[2]);maxY=math.max(maxY,p[2])end
 for y=math.floor(minY),math.ceil(maxY)do local crossings={};local scan=y+0.5
  for i=1,#points do local a,b=points[i],points[i%#points+1];if(a[2]<=scan and b[2]>scan)or(b[2]<=scan and a[2]>scan)then crossings[#crossings+1]=a[1]+(scan-a[2])*(b[1]-a[1])/(b[2]-a[2])end end
  table.sort(crossings);for j=1,#crossings-1,2 do for x=math.ceil(crossings[j]),math.floor(crossings[j+1])do pixel(x,y,c)end end
 end
end
local function line(x1,y1,x2,y2,c,w)
 local n=math.max(math.abs(x2-x1),math.abs(y2-y1));for i=0,math.max(n,1)do local t=n==0 and 0 or i/n;box(math.floor(x1+(x2-x1)*t),math.floor(y1+(y2-y1)*t),w or 1,w or 1,c)end
end
local function path(points,c,w)for i=1,#points-1 do line(points[i][1],points[i][2],points[i+1][1],points[i+1][2],c,w)end end
local function oval(cx,cy,rx,ry,c)
 for y=-ry,ry do local x=math.floor(rx*math.sqrt(math.max(0,1-y*y/(ry*ry))));box(cx-x,cy+y,2*x+1,1,c)end
end
local function at(x,y,fn)local a,b=ox,oy;ox,oy=a+x,b+y;fn();ox,oy=a,b end
local D='#171B20';local W0='#38251F';local W1='#68442D';local W2='#94643A';local W3='#BC8E50';local W4='#E1B879'
local metal0='#253238';local metal1='#4D6063';local metal2='#899B97';local metal3='#D0D9BC'
local function bolt(x,y)
 at(x,y,function()poly({{2,0},{6,0},{8,2},{8,6},{6,8},{2,8},{0,6},{0,2}},D);poly({{2,1},{6,1},{7,2},{7,5},{5,7},{2,7},{1,5},{1,2}},metal1);path({{2,2},{5,2},{6,3}},metal3);line(2,5,5,3,metal0);end)
end
local function woodRibbon(x,y,w,h)
 at(x,y,function()
 poly({{0,8},{12,3},{64,4},{70,0},{w-19,2},{w-5,7},{w,19},{w-3,h-7},{w-15,h-2},{32,h},{5,h-5}},D)
 poly({{3,9},{14,6},{64,7},{71,3},{w-20,5},{w-8,9},{w-4,20},{w-6,h-9},{w-17,h-5},{33,h-3},{8,h-8}},W1)
 poly({{6,10},{17,8},{65,9},{72,6},{w-22,8},{w-10,11},{w-8,18},{w-13,h-12},{30,h-10},{10,h-12}},W2)
 path({{8,11},{22,9},{66,10},{73,7},{w-24,9},{w-12,12}},W4,2)
 path({{12,h-10},{33,h-7},{w-21,h-8},{w-11,h-12}},W0,2)
 path({{42,17},{71,15},{96,17},{122,16},{143,19}},'#AE7C42',2)
 path({{48,21},{66,20},{80,22}},'#6F492C',2)
 path({{w-124,h-18},{w-107,h-20},{w-83,h-18},{w-49,h-20}},'#BA8B4C',2)
 path({{w-52,10},{w-57,18},{w-53,23}},W0,2)
 end)
end
local function crest(x,y)
 at(x,y,function()
 poly({{16,0},{34,1},{49,13},{50,32},{37,47},{17,48},{1,34},{0,16}},D)
 poly({{17,3},{33,4},{45,15},{46,31},{35,43},{18,44},{5,32},{4,17}},metal1)
 poly({{18,6},{32,7},{42,17},{43,30},{34,40},{19,41},{8,30},{7,18}},'#34545C')
 path({{7,19},{11,12},{20,6},{32,7},{41,15}},metal2,2)
 poly({{25,9},{28,21},{39,25},{28,29},{24,40},{21,29},{11,24},{21,21}},'#D2AC65')
 poly({{25,10},{25,25},{12,24},{21,21}},'#F0D39A');poly({{25,25},{39,25},{28,29},{24,40}},'#97713F')
 oval(25,25,4,4,'#182D35');oval(24,24,2,2,'#A9C6BC')
 end)
end
-- Semantic icons painted at their consumer-native 36x36 size.
local icons={}
local function sword(x,y,flip)
 local a=flip and -1 or 1
 local function pt(xx,yy)return{x+a*xx,y+yy}end
 poly({pt(0,0),pt(5,0),pt(22,19),pt(18,23)},D)
 poly({pt(2,1),pt(4,2),pt(20,19),pt(17,19)},'#C3D3C6')
 poly({pt(2,3),pt(17,21),pt(18,18)},'#6F8E91')
 line(x+a*14,y+23,x+a*23,y+15,'#BD8B43',3);line(x+a*19,y+23,x+a*25,y+29,'#6E442E',3);box(x+a*25,y+28,3,3,'#D2A964')
end
local function face(x,y,skin,hair)
 poly({{x+1,y},{x+11,y},{x+14,y+3},{x+14,y+15},{x+10,y+18},{x+2,y+16},{x,y+12}},D)
 poly({{x+2,y+2},{x+10,y+2},{x+12,y+4},{x+12,y+14},{x+9,y+16},{x+3,y+14}},skin)
 poly({{x+1,y+1},{x+11,y+1},{x+13,y+4},{x+10,y+6},{x+6,y+5},{x+3,y+7},{x+1,y+5}},hair)
 box(x+3,y+8,3,2,'#EAE2C5');box(x+9,y+8,2,2,'#EFE6C9');box(x+5,y+8,1,2,'#466B70');box(x+10,y+8,1,2,'#466B70')
 path({{x+7,y+10},{x+7,y+12},{x+10,y+12}},'#986A52');line(x+5,y+14,x+8,y+14,'#754738')
end
local function makeIcon(id,draw)
 local sp=Sprite(36,36,ColorMode.RGB);local im=Image(sp.spec);target=im;ox,oy=0,0;draw();sp.layers[1].name='Native painted planes';sp:newCel(sp.layers[1],sp.frames[1],im,Point(0,0));
 local highlights=sp:newLayer();highlights.name='Material clusters and sculpted highlights';local detail=Image(sp.spec);target=detail
 if id=='profile'then
  poly({{12,4},{16,2},{21,3},{23,5},{18,4},{16,6},{13,6}},'#87603D');path({{13,10},{15,10},{15,13}},'#E8BC87');box(20,12,2,3,'#C18A64');box(15,18,5,2,'#AC7959');poly({{12,24},{16,23},{18,26},{15,28},{12,26}},'#91B6AA');poly({{22,24},{26,27},{28,32},{24,31},{23,28},{20,28}},'#315A68');box(18,26,2,2,'#F1D695')
 elseif id=='players'then
  path({{5,8},{8,7},{10,8}},'#937156');box(6,16,2,2,'#D49F72');poly({{4,27},{8,25},{11,27},{8,29},{4,29}},'#9A9CB0');poly({{22,5},{26,4},{30,6},{29,8},{26,7},{23,8}},'#7D4B35');box(23,12,2,3,'#F2C28B');poly({{20,27},{24,25},{27,28},{24,29},{20,30}},'#7EB19A');path({{29,28},{31,31},{31,33}},'#3B6B68',2)
 elseif id=='party'then
  poly({{13,6},{19,7},{18,13},{13,16},{13,19},{11,20},{11,7}},'#5B93A5');poly({{24,7},{26,7},{24,13},{26,16},{22,16},{22,12}},'#315573');path({{7,4},{9,4},{9,29}},'#B58D4A');box(6,4,1,27,'#E4C684');path({{20,23},{23,22},{26,23}},'#9A603F');box(21,29,2,2,'#E9BE84');poly({{19,32},{25,34},{24,35},{19,35}},'#386D7B')
 elseif id=='events'then
  poly({{10,6},{15,4},{20,5},{20,7},{17,9},{13,9},{12,12},{7,13}},'#F5A975');poly({{13,14},{17,12},{19,16},{16,19},{12,18}},'#D67352');poly({{23,8},{26,11},{27,17},{24,19},{24,15},{21,13}},'#B04E43');poly({{21,22},{23,21},{23,25},{19,27},{15,25},{17,23}},'#953B3D');box(9,8,2,3,'#FFCEA0');path({{6,18},{7,22},{10,24}},'#8F3C40')
 elseif id=='market'then
  poly({{4,8},{14,5},{20,5},{30,8},{29,10},{18,8},{5,10}},'#D5A445');path({{5,8},{16,6},{29,9}},'#FFE0A0');line(17,10,17,28,'#D5B76F');box(18,11,1,17,'#927140');poly({{1,22},{10,22},{8,25},{3,25}},'#C69442');line(2,22,9,22,'#FFE3A6');poly({{23,22},{33,22},{30,25},{26,25}},'#CCA14C');line(24,22,32,22,'#FFE4A5');line(12,29,24,29,'#E4C481');line(12,31,25,31,'#986A34')
 elseif id=='settlement'then
  path({{5,17},{16,7},{20,9}},'#E49A67',2);path({{11,14},{15,15},{14,17},{10,17}},'#8A4236');poly({{10,22},{15,18},{17,18},{17,22},{13,24},{10,24}},'#EED49A');box(10,29,6,1,'#B08B52');box(20,25,2,5,'#89623A');box(23,27,1,1,'#EDD18C');box(25,5,1,5,'#C1A470');line(9,32,27,32,'#D2B77B')
 elseif id=='rewards'then
  poly({{10,10},{23,10},{26,13},{10,13}},'#D9AA58');path({{11,16},{18,15},{22,17}},'#D6A656');path({{11,26},{17,25},{21,27}},'#B5793C');box(11,28,9,1,'#704529');box(7,13,2,2,'#D2D6B4');box(7,27,2,2,'#B5C2AF');box(23,13,2,2,'#D9DDC0');box(23,27,2,2,'#A7B6A7');poly({{13,3},{17,3},{19,6},{17,8},{13,7}},'#FFE2A0');poly({{23,4},{26,5},{28,8},{23,9},{21,7}},'#9CE0D0');box(17,20,2,2,'#FFF1B6');line(29,17,29,27,'#89643D')
 elseif id=='races'then
  poly({{8,6},{16,6},{16,10},{12,10},{10,8}},'#F3DB8E');poly({{7,11},{9,12},{11,17},{10,21},{8,19}},'#B49E60');poly({{12,19},{16,20},{17,24},{15,25},{12,23}},'#E1C27C');path({{21,7},{25,7},{26,10}},'#91A889');poly({{22,20},{26,18},{24,25},{21,28},{20,25}},'#405C58');box(17,7,2,1,'#D2F3DC');poly({{16,27},{18,28},{21,27},{20,29},{17,30}},'#8C7540')
 elseif id=='professions'then
  path({{12,4},{18,5},{25,8}},'#E2E8CE');poly({{12,6},{19,7},{23,10},{19,9},{15,9}},'#6F9297');path({{27,10},{30,14},{30,17}},'#B3C6B9');line(9,26,15,20,'#E3BD72');line(19,15,24,10,'#CBA15C');path({{13,18},{18,23},{22,25}},'#E5BA72');box(5,11,4,1,'#EEF0D0');box(5,13,2,2,'#5A8188');poly({{27,29},{29,31},{27,33},{25,32}},'#E1B265')
 end
 sp:newCel(highlights,sp.frames[1],detail,Point(0,0));local merged=Image(sp.spec);merged:drawImage(im);merged:drawImage(detail);icons[id]=merged
 sp:saveCopyAs(ROOT..'/icon-'..id..'.aseprite');sp:saveCopyAs(ROOT..'/icon-'..id..'.png');sp:close()
end
makeIcon('profile',function()
 poly({{4,34},{5,26},{12,20},{24,20},{31,26},{33,34}},D);poly({{7,33},{8,27},{14,23},{22,23},{28,27},{30,33}},'#3A717A');poly({{8,27},{14,23},{18,28},{12,29},{11,33},{7,33}},'#719D9B');face(11,2,'#D3A173','#52392D');box(17,25,3,5,'#D8C79B')
end)
makeIcon('players',function()
 poly({{1,34},{2,25},{9,21},{17,22},{21,34}},D);poly({{3,33},{4,26},{10,23},{16,25},{18,33}},'#6C758D');face(3,5,'#BB855F','#53443B')
 poly({{15,34},{17,23},{24,21},{33,25},{35,34}},D);poly({{18,33},{19,25},{26,23},{32,27},{33,33}},'#578D7F');face(20,2,'#DBAE82','#4E2D29')
end)
makeIcon('party',function()
 line(7,3,7,32,'#3A2C22',3);line(7,3,7,31,'#D1AB68');poly({{10,4},{29,5},{27,14},{31,19},{13,18},{10,22}},'#192A30');poly({{11,5},{27,6},{25,13},{28,17},{13,16},{11,19}},'#456F89');path({{12,6},{25,7},{24,10}},'#8EB9B5');
 poly({{16,8},{22,9},{22,12},{19,15},{16,12}},'#D9BF7C');box(4,32,10,2,'#89663F');face(18,20,'#D4A275','#4C332A')
end)
makeIcon('events',function()
 oval(18,16,14,14,D);oval(18,15,12,12,'#6D3035');oval(17,14,11,11,'#B34E43');poly({{9,7},{19,4},{26,8},{23,11},{15,10},{12,16},{6,16}},'#E78C61');poly({{19,16},{27,13},{27,21},{21,26},{16,22}},'#913C3B');box(11,9,4,3,'#F6BA82');
 poly({{1,34},{3,26},{7,29},{10,23},{12,33},{18,29},{23,33},{27,23},{29,28},{33,24},{35,34}},'#1D3033')
end)
makeIcon('market',function()
 path({{3,9},{17,6},{31,9}},D,3);path({{4,9},{17,7},{30,9}},'#D7B067',2);line(17,5,17,30,metal0,3);line(17,5,17,29,metal2)
 for _,x in ipairs({6,28})do line(x,10,x-4,21,'#748987');line(x,10,x+4,21,'#CCD2AE');poly({{x-6,22},{x+6,22},{x+3,26},{x-3,26}},D);poly({{x-5,22},{x+5,22},{x+2,24},{x-2,24}},'#D7B36C')end
 poly({{8,32},{11,28},{25,28},{28,32}},D);box(10,30,16,2,'#B38A52');oval(19,5,3,3,'#C29D58');box(19,3,2,2,'#E5CE92')
end)
makeIcon('settlement',function()
 poly({{1,18},{17,3},{34,16},{30,20},{17,10},{5,22}},D);poly({{3,17},{17,5},{31,16},{29,18},{17,8},{5,20}},'#9F4C3B');poly({{4,17},{17,7},{17,11},{7,21}},'#CB7951')
 poly({{7,20},{17,12},{29,21},{29,33},{7,33}},'#46392C');poly({{9,21},{17,15},{27,22},{27,31},{9,31}},'#D1B47C');box(18,23,6,9,'#59432F');box(11,22,4,5,'#40636B');box(11,22,2,2,'#95BDB1');box(24,5,4,8,'#9B825D');box(23,4,6,2,'#DEC795');line(8,32,28,32,'#8F7649')
end)
makeIcon('rewards',function()
 poly({{3,11},{9,7},{29,7},{34,12},{32,32},{5,34},{2,29}},D);poly({{5,13},{11,9},{28,9},{31,13},{30,30},{6,31}},'#8B562F');poly({{5,13},{28,13},{28,21},{5,21}},'#C6954D');poly({{28,13},{31,11},{31,28},{28,31}},'#62412D');line(5,14,26,14,'#E0BA74',2);line(5,22,27,22,'#402F25',2);box(7,12,3,19,metal1);box(23,12,3,19,metal1);box(8,13,1,17,metal2);box(24,13,1,17,metal2);box(15,18,6,9,'#2B3030');box(16,19,4,7,'#EDCA7F');box(17,22,2,2,'#7B5E35');
 poly({{11,8},{12,2},{18,1},{21,5},{18,10}},'#D9AE4E');poly({{19,9},{23,2},{28,4},{30,9}},'#77B8B1');box(14,3,3,2,'#FFE1A0')
end)
makeIcon('races',function()
 poly({{7,2},{29,2},{32,8},{30,23},{23,33},{14,33},{5,23},{3,8}},D);poly({{8,4},{27,4},{29,9},{27,23},{22,30},{15,30},{8,22},{6,9}},'#9C864B');poly({{8,5},{17,5},{17,29},{14,27},{10,21}},'#D6BD72');poly({{19,5},{27,5},{28,9},{25,23},{21,28},{19,19}},'#647F6E');
 poly({{9,11},{15,13},{15,17},{10,15}},'#263D42');poly({{20,13},{26,11},{25,16},{20,17}},'#20383C');box(16,7,4,3,'#9BCAC0');poly({{16,20},{20,20},{22,24},{17,27},{14,23}},'#413C2B');line(9,6,15,6,'#F2D990')
end)
makeIcon('professions',function()
 line(5,30,26,8,'#2B2B29',5);line(7,30,28,8,'#8F6337',3);line(8,29,27,9,'#D3A467')
 poly({{10,3},{18,2},{29,7},{34,14},{32,20},{28,14},{23,11},{12,8},{7,9}},D);poly({{11,4},{18,4},{28,9},{32,15},{31,16},{27,12},{22,9},{12,6},{8,8}},'#97B0B0');path({{12,4},{19,5},{27,9}},'#DBE5C7',2)
 poly({{4,11},{10,12},{13,16},{30,31},{26,34},{9,19},{5,18}},'#312E2B');poly({{6,13},{9,13},{12,17},{28,31},{26,32},{10,18},{6,17}},'#BD8950');box(4,10,6,6,metal1);box(5,11,4,2,metal3)
end)

-- Native 202x198 card illustrations, composed as orthographic voxel forms.
local art={}
local function tree(x,y,h,c)
 box(x-2,y,4,h,c);poly({{x,y-7},{x-8,y+8},{x-3,y+7},{x-13,y+21},{x-5,y+20},{x-18,y+36},{x-8,y+34},{x-21,y+49},{x+22,y+49},{x+8,y+34},{x+18,y+36},{x+5,y+20},{x+13,y+21},{x+3,y+7},{x+8,y+8}},c)
end
local function cuboid(x,y,w,h,dx,dy,front,top,side)
 poly({{x,y},{x+w,y},{x+w,y+h},{x,y+h}},front)
 poly({{x,y},{x+dx,y+dy},{x+w+dx,y+dy},{x+w,y}},top)
 poly({{x+w,y},{x+w+dx,y+dy},{x+w+dx,y+h+dy},{x+w,y+h}},side)
end
local function cardArt(id,draw)
 local sp=Sprite(202,198,ColorMode.RGB);local im=Image(sp.spec);target=im;ox,oy=0,0;draw();sp.layers[1].name='Authored scene and connected voxel planes';sp:newCel(sp.layers[1],sp.frames[1],im,Point(0,0));local l=sp:newLayer();l.name='Local refinements';sp:newCel(l,sp.frames[1],Image(sp.spec),Point(0,0));sp:saveCopyAs(ROOT..'/art-'..id..'.aseprite');sp:saveCopyAs(ROOT..'/art-'..id..'.png');art[id]=im;sp:close()
end
cardArt('blood-moon',function()
 box(0,0,202,198,'#242131');poly({{0,87},{36,76},{72,83},{117,70},{160,84},{202,80},{202,198},{0,198}},'#3D2A39')
 oval(61,54,46,46,'#67323E');oval(59,51,41,41,'#AE4F4C');oval(55,47,35,35,'#D77F61')
 poly({{31,23},{53,15},{77,21},{85,34},{74,40},{67,34},{46,37},{39,49},{21,45},{24,30}},'#F0B47E');poly({{53,47},{69,39},{88,48},{90,66},{75,80},{57,80},{51,71},{62,63}},'#B85C52');poly({{31,55},{45,50},{52,60},{43,69},{31,65}},'#E1976B')
 poly({{0,43},{22,40},{31,44},{51,41},{69,46},{77,53},{41,53},{30,49},{8,51},{0,49}},'#342634')
 poly({{91,70},{112,65},{134,68},{148,64},{177,68},{202,65},{202,76},{154,77},{131,73},{107,77}},'#4E303D')
 for _,t in ipairs({{14,74,79},{39,92,67},{174,71,100},{192,104,76}})do tree(t[1],t[2],t[3],'#252933')end
 poly({{0,160},{38,147},{72,158},{114,145},{160,154},{202,140},{202,198},{0,198}},'#18242B')
 -- Zombie: coherent block head/neck/torso and both forward-reaching arms.
 cuboid(106,83,43,43,16,-13,'#70866B','#A1A277','#3D6157')
 box(107,84,41,5,'#89916E');box(112,98,12,5,'#283F3C');box(135,96,10,5,'#243E3A');box(124,105,9,8,'#4A6854');box(114,117,26,5,'#3B5548')
 poly({{108,86},{119,86},{119,91},{131,91},{131,86},{145,86},{145,93},{137,93},{137,95},{111,95}},'#5E7A63')
 cuboid(110,132,38,48,13,-10,'#31545C','#567A7C','#233D4C')
 cuboid(82,119,28,16,13,-10,'#6F886B','#9BA27B','#3C6053');cuboid(59,114,24,17,11,-9,'#6A8469','#A6A67B','#3D5E50')
 cuboid(144,119,31,16,12,-10,'#60826A','#92A17B','#365F52');cuboid(174,113,21,16,7,-8,'#66886C','#91A27A','#3C6254')
 cuboid(111,181,16,23,8,-6,'#44445D','#5B5871','#2A3549');cuboid(132,178,16,26,9,-7,'#393E57','#51516A','#263247')
 -- Purposeful face-local texture clusters, following the plane instead of generic noise.
 poly({{110,89},{116,89},{116,92},{122,92},{122,95},{109,95}},'#94A177');poly({{129,86},{139,86},{139,91},{135,91},{135,94},{128,94}},'#7E906C')
 box(114,107,5,5,'#5B785D');box(136,107,7,4,'#82916B');box(121,120,8,3,'#8C966F');box(144,112,4,9,'#4A6C59')
 poly({{152,78},{160,71},{160,79},{155,85},{155,94},{151,97}},'#537765');box(154,102,4,8,'#2E524E')
 path({{114,135},{121,133},{127,134}},'#76938A',2);poly({{127,139},{134,137},{139,144},{136,151},{131,151}},'#406975');poly({{115,163},{123,163},{122,171},{128,177},{115,177}},'#294551')
 path({{150,133},{155,129},{158,132},{158,141}},'#33596A',2);box(116,187,4,8,'#616276');box(138,184,4,8,'#4D5269')
 box(65,117,5,3,'#B2B086');box(77,120,4,5,'#4E705D');box(91,123,7,3,'#92A17A');box(177,115,6,4,'#A3AC81')
 path({{115,139},{114,151},{122,157}},'#548180',2);box(139,158,7,13,'#233D4C');poly({{0,192},{13,179},{17,184},{22,169},{28,185},{38,176},{40,195},{57,184},{62,198},{0,198}},'#0E1D24')
 path({{20,172},{35,167},{48,169}},'#6C4948',2);path({{151,192},{166,180},{180,184}},'#856052',2)
end)
cardArt('boss-raid',function()
 box(0,0,202,198,'#172932');poly({{0,0},{65,0},{70,75},{50,92},{0,99}},'#203D46');poly({{158,0},{202,0},{202,198},{162,155}},'#10252E')
 -- Ancient masonry in a consistent frontal projection with top-right return planes.
 for _,b in ipairs({{0,19,48,28},{0,50,48,31},{0,84,48,27},{0,114,48,34},{0,151,48,30},{155,4,47,30},{155,37,47,32},{155,72,47,28},{155,103,47,35}})do
  cuboid(b[1],b[2],b[3],b[4],6,-4,'#29434B','#4C6570','#142E38')
 end
 box(48,18,8,148,'#37545B');path({{52,20},{52,89},{58,110},{58,158}},'#5D7C7E',2)
 poly({{0,183},{44,164},{119,158},{202,175},{202,198},{0,198}},'#283E46');poly({{37,198},{68,177},{148,169},{202,187},{202,198}},'#36515A');path({{77,188},{126,182},{153,185}},'#607A7D',2)
 -- Warden-like guardian illustration: broad connected body, bony horn silhouettes.
 cuboid(72,70,57,51,19,-15,'#183D48','#32606A','#102C38');box(78,76,44,5,'#245765')
 path({{76,90},{68,88},{62,76},{63,62},{57,56}},'#76B6AD',5);path({{72,88},{65,84},{59,72},{59,65}},'#C7D7B5',2)
 path({{125,80},{140,78},{146,66},{145,51},{152,44}},'#65A99F',5);path({{137,75},{143,64},{142,53},{148,46}},'#BDD4B3',2)
 box(88,95,30,5,'#0D2833');poly({{78,101},{84,100},{84,106},{115,106},{115,101},{122,101},{122,114},{80,114}},'#527D7E')
 cuboid(67,125,67,60,18,-14,'#183C48','#2C5964','#102C38');poly({{79,133},{91,129},{116,131},{123,143},{115,166},{91,172},{80,161}},'#4A6F70');poly({{88,138},{97,135},{113,138},{117,146},{109,160},{95,162},{88,155}},'#80B7A4');path({{92,140},{104,140},{112,147},{105,156},{95,154}},'#D6D9A8',3)
 cuboid(41,120,24,60,10,-8,'#183C48','#416C70','#0F2935');cuboid(138,109,25,65,12,-9,'#1B414C','#427174','#102C38')
 cuboid(72,187,24,22,7,-6,'#142E39','#2E505B','#0B232E');cuboid(112,184,24,25,7,-6,'#183440','#345662','#0D2530')
 -- Sculk marks and bone-rib contact are authored on actual body planes.
 poly({{76,77},{82,77},{82,81},{89,81},{89,85},{83,87},{76,84}},'#377682');box(113,74,8,3,'#367481');box(122,82,4,8,'#285B68');box(80,88,5,4,'#2A6574')
 path({{81,142},{85,136},{91,133}},'#B6CCAB',3);path({{80,153},{87,149},{91,149}},'#83AAA0',3);path({{86,163},{91,158},{94,159}},'#A4C0A5',3)
 path({{116,135},{122,139},{124,145}},'#91B8A7',3);path({{116,150},{122,152},{120,157}},'#C7D0AB',3)
 poly({{70,167},{75,169},{75,176},{81,176},{82,182},{70,180}},'#24505E');box(126,173,5,8,'#285360')
 box(43,125,5,9,'#2D6671');box(48,137,5,4,'#3A7B7E');box(43,153,4,10,'#285A68');box(56,169,4,5,'#467A7B');box(145,115,6,4,'#478586');box(147,135,4,9,'#315F6C');box(153,156,5,4,'#3D7B7F')
 for _,q in ipairs({{13,157},{23,136},{36,171},{167,145},{182,157},{178,127}})do poly({{q[1],q[2]},{q[1]+3,q[2]-5},{q[1]+6,q[2]},{q[1]+3,q[2]+4}},'#569B98')end
end)
cardArt('resource-rush',function()
 box(0,0,202,198,'#39342F');poly({{0,0},{64,0},{45,56},{57,80},{23,113},{0,108}},'#6B5840');poly({{117,0},{202,0},{202,198},{166,183},{168,91},{143,62}},'#282D2C')
 poly({{0,113},{47,97},{78,105},{126,89},{170,107},{202,102},{202,198},{0,198}},'#4C4536');poly({{0,151},{61,129},{104,140},{157,122},{202,138},{202,198},{0,198}},'#715B3E')
 -- Mine support and coherent timber overlaps.
 cuboid(14,19,14,129,7,-5,'#775235','#AC8250','#4B392A');cuboid(28,17,137,13,6,-5,'#8D643C','#C19B60','#58402D');cuboid(163,26,13,122,6,-5,'#67482F','#9F7547','#473628')
 path({{18,25},{19,58},{17,89},{19,120}},'#B99055',2);path({{52,22},{87,21},{108,23},{144,22}},'#5C402B',2)
 -- Faceted ore-bearing rock: cool planes, warm ground, no random pixel noise.
 poly({{47,73},{72,54},{113,49},{143,65},{157,101},{138,140},{92,153},{55,131},{36,103}},'#1A343B')
 poly({{47,73},{73,58},{108,53},{105,89},{76,100},{39,102}},'#66858A');poly({{108,53},{140,69},{152,100},{125,108},{105,89}},'#3A636E');poly({{39,104},{77,102},{94,123},{90,147},{56,129}},'#3F6069');poly({{78,102},{105,90},{126,110},{136,137},{92,149},{95,122}},'#284A58');poly({{128,111},{153,103},{137,136}},'#183B49')
 for _,q in ipairs({{55,81},{96,68},{115,112}})do local x,y=q[1],q[2];poly({{x,y},{x+10,y-5},{x+18,y},{x+13,y+15},{x+5,y+17}},'#183A47');poly({{x+2,y},{x+10,y-3},{x+16,y+1},{x+12,y+12},{x+6,y+14}},'#3CAFB1');poly({{x+3,y+1},{x+10,y-2},{x+9,y+10},{x+6,y+13}},'#A5E1CD');line(x+11,y,x+14,y+2,'#DCF0D3')end
 path({{75,65},{83,63},{91,65},{89,73}},'#90A39F',2);path({{44,113},{54,118},{59,124}},'#6D868A',2);poly({{88,108},{93,105},{99,112},{96,117}},'#537B83');poly({{131,86},{139,89},{144,97},{137,99}},'#4C7B82')
 -- Pickaxe spans rock with connected handle and upper-left-lit forged head.
 poly({{71,152},{79,158},{154,66},{147,60}},'#2C302C');poly({{73,152},{78,155},{152,65},{148,63}},'#9B713C');line(76,150,148,66,'#D5AC66',2)
 poly({{105,48},{127,42},{152,46},{171,61},{181,84},{175,90},{164,73},{148,60},{124,54},{100,60}},'#182A30');poly({{106,49},{127,45},{151,49},{168,63},{176,82},{174,84},{163,69},{146,57},{124,51},{103,57}},'#819C9C');path({{110,49},{128,46},{150,50},{164,60}},'#CDD9BD',3);poly({{154,56},{168,66},{175,82},{169,78},{162,68}},'#41666F')
 -- Foreground ore cart front/top/side, tied metal bands.
 cuboid(16,154,61,31,13,-9,'#946039','#BB8C4D','#5B412E');line(18,165,74,165,'#513828',2);line(18,179,74,179,'#513828',2);box(23,155,5,28,metal1);box(63,155,5,28,metal1);line(24,155,24,181,metal2);line(64,155,64,181,metal2);oval(30,190,9,8,'#242D2F');oval(30,190,5,5,'#6F7770');oval(69,190,9,8,'#242D2F');oval(69,190,5,5,'#747D73');
 poly({{22,150},{34,139},{47,148},{53,138},{67,146},{78,147},{75,154},{24,154}},'#B89042');poly({{31,146},{35,141},{42,147},{36,150}},'#E4C16D');poly({{55,145},{59,140},{65,145},{60,150}},'#D5B869')
end)

-- Minecraft bitmap font is read-only; all text pixels are still painted in Aseprite.
local function read(p)local f=assert(io.open(p,'rb'));local s=f:read('*a');f:close();return s end
local providers=json.decode(read('C:/MyProjects/Minecraft/JustEnoughMods/Server/local/settlements-art-20261020/native/fonts.json'));local glyphs={}
for _,provider in ipairs(providers)do local atlas=Image{fromFile=provider['local']};local columns=0;for _,chars in ipairs(provider.chars)do columns=math.max(columns,utf8.len(chars))end;local cw,ch=atlas.width/columns,atlas.height/#provider.chars
 for row,chars in ipairs(provider.chars)do local col=0;for _,cp in utf8.codes(chars)do if cp~=0 and not glyphs[cp]then local width=0;for y=0,ch-1 do for x=0,cw-1 do if app.pixelColor.rgbaA(atlas:getPixel(col*cw+x,(row-1)*ch+y))>0 then width=math.max(width,x+1)end end end;glyphs[cp]={im=atlas,x=col*cw,y=(row-1)*ch,w=width,h=ch,off=7-provider.ascent}end;col=col+1 end end
end
local function width(s,k)local n=0;for _,cp in utf8.codes(s)do local g=glyphs[cp]or glyphs[63];n=n+(cp==32 and 4 or g.w+1)*(k or 1)end;return n end
local function text(s,x,y,col,k)k=k or 1;for _,cp in utf8.codes(s)do local g=glyphs[cp]or glyphs[63];if cp~=32 then for dy=0,g.h-1 do for dx=0,g.w-1 do if app.pixelColor.rgbaA(g.im:getPixel(g.x+dx,g.y+dy))>0 then box(x+dx*k,y+(dy+g.off)*k,k,k,col)end end end end;x=x+(cp==32 and 4 or g.w+1)*k end end
local function centered(s,x,y,w,c,k)text(s,x+math.floor((w-width(s,k))/2),y,c,k)end
local page=Sprite(1024,640,ColorMode.RGB);local layers={};local ims={}
for i,name in ipairs({'01 Hand-shaped timber and iron','02 Sculpted navigation seats','03 Colored card shells and projecting ribbons','04 Original illustrations and semantic icons','05 Preview text - not game texture','06 Reserved local refinements'})do layers[i]=i==1 and page.layers[1]or page:newLayer();layers[i].name=name;ims[i]=Image(page.spec)end
target=ims[1];ox,oy=0,0
-- A shaped timber desk. No separator-rule decoration or flat full-canvas card grid.
poly({{25,24},{71,20},{81,14},{970,18},{990,30},{999,69},{991,81},{1002,599},{989,620},{724,623},{716,627},{34,623},{16,609},{20,92},{13,73},{16,38}},D)
poly({{29,29},{74,26},{83,20},{968,24},{983,34},{990,68},{985,82},{994,596},{983,612},{721,616},{711,619},{39,615},{24,604},{27,90},{20,70},{23,42}},W1)
woodRibbon(22,19,971,65);woodRibbon(29,594,962,26)
-- Broad wood-fibre clusters describe the long header face; none crosses a label.
poly({{405,31},{452,28},{483,30},{508,29},{544,33},{573,32},{597,36},{645,35},{654,38},{592,39},{562,37},{537,39},{501,35},{474,36},{447,33},{411,34}},'#A87842')
poly({{430,45},{467,40},{492,43},{516,42},{544,47},{577,46},{596,49},{559,51},{537,49},{513,48},{489,47},{466,45},{439,48}},'#7D512F')
poly({{455,52},{481,48},{506,50},{515,54},{498,58},{476,58},{461,56}},'#6E462B');poly({{464,53},{480,50},{501,52},{506,54},{496,56},{478,55}},'#C08D50');path({{473,52},{484,52},{492,54}},'#81502B',2)
poly({{537,63},{567,58},{590,60},{616,57},{645,59},{661,62},{630,62},{615,61},{591,64},{569,62},{548,66}},'#BC8950')
poly({{816,32},{851,29},{873,32},{911,31},{929,34},{913,37},{881,35},{850,34},{825,36}},'#A87842')
poly({{28,86},{44,84},{44,593},{31,599},{25,590}},W2);path({{31,94},{30,286},{32,443},{31,586}},W4,2)
poly({{976,84},{990,84},{994,587},{985,601},{976,592}},'#855B36');path({{980,89},{982,278},{980,463},{983,590}},'#BB8A4D',2)
poly({{48,88},{210,88},{215,592},{47,592}},'#302B27');poly({{222,89},{972,88},{973,592},{224,594}},'#292D2D')
-- A continuous carved spine with physical shoulders, not a thin dividing line.
poly({{208,89},{218,84},{231,89},{235,105},{232,573},{225,592},{212,590},{207,572}},D)
poly({{213,91},{218,89},{226,93},{229,106},{226,571},{222,582},{216,581},{212,571}},metal0);path({{216,97},{216,566},{220,577}},metal2,2);bolt(214,106);bolt(214,564)
crest(43,23);text('FOREVER SMP',110,39,'#F0D9AC',3);text('СОБЫТИЯ',692,47,'#DCBD82',2)
-- A real close-button silhouette and a carved back-arrow, not extra separator strokes.
poly({{938,35},{950,35},{956,41},{956,55},{950,61},{938,61},{932,55},{932,41}},'#342D25');poly({{939,38},{949,38},{953,42},{953,54},{949,58},{939,58},{935,54},{935,42}},metal0);path({{938,40},{949,40},{951,43}},metal2);line(940,44,948,52,'#E1D2A8',2);line(940,52,948,44,'#D5C69E',2)
for _,q in ipairs({{28,30},{969,31},{29,596},{973,596}})do bolt(q[1],q[2])end
-- Corner straps wrap and fasten to the wood instead of floating on it.
for _,q in ipairs({{30,82,1},{983,82,-1}})do local x,y,d=q[1],q[2],q[3];poly({{x,y},{x+d*27,y},{x+d*27,y+6},{x+d*9,y+6},{x+d*9,y+30},{x,y+30}},metal0);path({{x+d*2,y+2},{x+d*24,y+2}},metal2,2);bolt(x+(d==1 and 1 or -8),y+13)end
target=ims[2]
local nav={{'Профиль','profile'},{'Игроки','players'},{'Группы','party'},{'События','events'},{'Рынок','market'},{'Поселения','settlement'},{'Награды','rewards'},{'Расы','races'},{'Профессии','professions'}}
for i,n in ipairs(nav)do local y=109+(i-1)*47;local selected=i==4
 at(45,y,function()
  poly({{4,1},{146,0},{158,6},{159,32},{151,40},{4,39},{0,31},{0,8}},'#171E21')
  poly({{5,4},{144,3},{154,8},{155,30},{148,35},{6,35},{4,30},{3,9}},selected and'#6C4532'or'#42382D')
  path({{7,5},{142,4},{151,9}},selected and'#E0B16F'or'#896B47',2)
  path({{8,33},{147,33},{152,28}},'#2A2420',2)
  if selected then poly({{-10,8},{-1,3},{3,9},{1,31},{-8,36}},'#A74C3E');path({{-7,10},{-4,7},{-3,28}},'#ED9862',2);bolt(148,12)end
 end)
end
target=ims[4]
for i,n in ipairs(nav)do local y=109+(i-1)*47;target:drawImage(icons[n[2]],Point(52,y+2));end
target=ims[5]
for i,n in ipairs(nav)do local y=109+(i-1)*47;text(n[1],96,y+16,i==4 and'#F4DCA8'or'#DDD1B3',2)end
text('События сервера',252,110,'#F0DDB8',3);text('Выберите приключение для своей группы',253,146,'#B8BBAA',2)
local function card(x,id,title,desc,desc2,time,base,body,edgeColor,light)
 target=ims[3]
 at(x,185,function()
  -- Cast silhouette, clipped stepped corners, hooked crown and projecting lower feet.
  poly({{7,16},{22,2},{99,2},{105,10},{125,10},{131,2},{209,2},{223,16},{223,378},{211,393},{17,393},{5,381}},'#141B20')
  poly({{10,15},{24,5},{97,5},{103,14},{128,14},{134,5},{207,5},{220,17},{220,375},{208,389},{19,389},{9,378}},base)
  poly({{15,18},{27,10},{93,10},{100,19},{131,19},{138,10},{203,10},{215,21},{215,370},{205,382},{22,382},{14,373}},edgeColor)
  poly({{19,23},{29,15},{90,15},{97,24},{134,24},{141,15},{199,15},{210,25},{210,209},{18,209}},'#101F26')
  path({{14,29},{14,20},{25,9},{93,9},{100,17}},light,2);path({{137,10},{202,10},{215,23},{215,201}},light,2)
  -- Warm information panel, with a sculpted edge rather than a dividing rule.
  poly({{15,218},{209,218},{211,371},{201,381},{27,381},{15,371}},'#463B32')
  poly({{19,221},{205,221},{207,367},{199,375},{29,375},{20,366}},body)
  path({{22,225},{22,364},{30,371}},'#F0DCB3',2)
  -- Raised title plaque with physical folded returns and a body shadow.
  at(0,20,function()
   poly({{-4,217},{7,205},{215,205},{230,218},{221,243},{7,243}},'#141A1D')
   poly({{-3,216},{7,207},{7,232},{-3,225}},'#4E3330');poly({{217,207},{227,218},{220,236},{214,230}},'#382F2C')
   poly({{8,205},{214,205},{224,217},{216,235},{8,235},{0,217}},base)
   poly({{10,208},{212,208},{220,218},{213,230},{10,230},{4,217}},edgeColor)
   path({{11,208},{211,208},{218,217}},light,2);path({{12,232},{214,232},{222,218}},'#18252A',2)
  end)
  -- Shaped button with stepped cheeks, inset center and offset lower plane.
  at(0,26,function()
   poly({{31,311},{39,303},{187,303},{196,311},{194,331},{187,337},{38,337},{30,331}},'#2E2925')
   poly({{34,312},{41,307},{185,307},{191,312},{190,328},{185,332},{40,332},{34,328}},base)
   path({{38,313},{43,310},{182,310},{187,314}},light,2);path({{40,329},{184,329},{188,325}},'#26302B',2)
  end)
 end)
 target=ims[4];target:drawImage(art[id],Point(x+13,185+20))
 -- Image is beneath the raised title, never a stamped icon floating in empty space.
 target=ims[5];centered(title,x+3,418,218,'#F1E0B9',2);centered(desc,x+19,451,189,'#514332',2);centered(desc2,x+19,470,189,'#514332',2);centered(time,x+19,496,189,'#69563E',2);centered('Подробнее',x+34,527,158,'#F3DEAF',2)
end
card(249,'blood-moon','Кровавая луна','Выстоять против','ночных волн','0,25 дня','#773A35','#D5C19A','#A75A45','#EBA570')
card(491,'boss-raid','Рейд на босса','Объединиться','против босса','Группа','#28545D','#CDD0B1','#4C8490','#ACD0C0')
card(733,'resource-rush','Гонка ресурсов','Добыча','наперегонки','Добыча','#775732','#D5C399','#AC8547','#E3C780')
-- One carved back control; no extra fake home or service UI.
target=ims[2];woodRibbon(46,556,157,34);target=ims[5];centered('Назад',51,567,143,'#E8D4AA',2)
for i=1,#layers do page:newCel(layers[i],page.frames[1],ims[i],Point(0,0))end
layers[5].isVisible=false;page:saveCopyAs(ROOT..'/smp-cards-surface.png');layers[5].isVisible=true;page:saveCopyAs(ROOT..'/smp-cards.aseprite');page:saveCopyAs(ROOT..'/smp-cards-1x.png');page:close()
-- Icon comparison sheet, also painted and exported natively inside Aseprite.
local sheet=Sprite(276,204,ColorMode.RGB);target=Image(sheet.spec);ox,oy=0,0;box(0,0,276,204,'#242C2F')
for i,n in ipairs(nav)do local x=12+((i-1)%3)*90;local y=6+math.floor((i-1)/3)*66;target:drawImage(icons[n[2]],Point(x+20,y+2));centered(n[1],x,y+44,82,'#D9C8A5',1)end
sheet:newCel(sheet.layers[1],sheet.frames[1],target,Point(0,0));sheet.layers[1].name='Native icon selection';sheet:saveCopyAs(ROOT..'/icons-sheet.aseprite');sheet:saveCopyAs(ROOT..'/icons-sheet-1x.png');sheet:close()
