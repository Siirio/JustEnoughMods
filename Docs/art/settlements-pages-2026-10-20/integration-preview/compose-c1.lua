-- C1 editor compositions from the current SettlementScreen coordinates.
-- Existing native materials/fonts/items only; no production pixel repaint or game execution.
local root=app.params.project
local out=app.params.output
assert(root and out,'project/output paths required')
local function read(path)local f=assert(io.open(path,'rb'));local s=f:read('*a');f:close();return s end
local fonts=json.decode(read(root..'/Server/local/settlements-art-20261020/native/fonts.json'))
local lang=json.decode(read(root..'/Mods/JEMSettlements/src/main/resources/assets/jem_settlements/lang/ru_ru.json'))
local function tr(k)return assert(lang['jem.settlements.gui.'..k],k)end
local function format(s,...)for _,v in ipairs({...})do s=s:gsub('%%s',function()return tostring(v)end,1)end;return s end
local glyphs={}
for _,provider in ipairs(fonts)do
 local atlas=Image{fromFile=provider['local']};local columns=0
 for _,chars in ipairs(provider.chars)do columns=math.max(columns,utf8.len(chars))end
 local cw,ch=atlas.width/columns,atlas.height/#provider.chars
 for row,chars in ipairs(provider.chars)do local col=0
  for _,cp in utf8.codes(chars)do
   if cp~=0 and not glyphs[cp]then
    local w=0;for y=0,ch-1 do for x=0,cw-1 do if app.pixelColor.rgbaA(atlas:getPixel(col*cw+x,(row-1)*ch+y))>0 then w=math.max(w,x+1)end end end
    glyphs[cp]={image=atlas,x=col*cw,y=(row-1)*ch,w=w,h=ch,offset=7-provider.ascent,advance=w+1}
   end
   col=col+1
  end
 end
end
local function color(h)h=h:gsub('#','');return app.pixelColor.rgba(tonumber(h:sub(1,2),16),tonumber(h:sub(3,4),16),tonumber(h:sub(5,6),16),tonumber(h:sub(7,8),16)or 255)end
local INK,MUTED,PALE='#342B26','#6C6257','#FFF1CB'
local function width(s)local w=0;for _,cp in utf8.codes(s)do local g=glyphs[cp]or glyphs[63];w=w+(cp==32 and 4 or g.advance)end;return math.max(0,w-1)end
local function clip(s,w)local result='';for _,cp in utf8.codes(s)do local next=result..utf8.char(cp);if width(next)>w then break end;result=next end;return result end
local cache={}
local function image(path)if not cache[path]then cache[path]=Image{fromFile=path}end;return cache[path]end
local assets=root..'/Mods/JEMSettlements/src/main/resources/assets/jem_settlements/textures/gui/'
local materials=image(assets..'pages/materials.png')
local function dashboard(name)return image(assets..'dashboard/'..name..'.png')end
local function rect(im,x,y,w,h,hue)
 local c=color(hue);for py=math.max(0,y),math.min(im.height-1,y+h-1)do for px=math.max(0,x),math.min(im.width-1,x+w-1)do im:drawPixel(px,py,c)end end
end
local function text(im,s,x,y,w,hue)
 s=clip(s,w);local c=color(hue)
 for _,cp in utf8.codes(s)do local g=glyphs[cp]or glyphs[63]
  if cp~=32 then for dy=0,g.h-1 do for dx=0,g.w-1 do local py=y+dy+g.offset
   if app.pixelColor.rgbaA(g.image:getPixel(g.x+dx,g.y+dy))>0 and x+dx>=0 and x+dx<im.width and py>=0 and py<im.height then im:drawPixel(x+dx,py,c)end
  end end end
  x=x+(cp==32 and 4 or g.advance)
 end
end
local function repeatRegion(im,src,x,y,w,h,u,v,sw,sh)
 if w<=0 or h<=0 then return end
 for dy=0,h-1,sh do for dx=0,w-1,sw do
  im:drawImage(Image(src,Rectangle(u,v,math.min(sw,w-dx),math.min(sh,h-dy))),Point(x+dx,y+dy))
 end end
end
local function rim(im,name,x,y,w,h)
 local src=dashboard(name)
 for row=0,2 do for col=0,2 do if row~=1 or col~=1 then
  local dx=col==0 and x or col==1 and x+5 or x+w-5;local dy=row==0 and y or row==1 and y+5 or y+h-5
  repeatRegion(im,src,dx,dy,col==1 and w-10 or 5,row==1 and h-10 or 5,col==0 and 0 or col==1 and 5 or 19,row==0 and 0 or row==1 and 5 or 19,col==1 and 14 or 5,row==1 and 14 or 5)
 end end end
end
local function patch(im,x,y,w,h,u,v,sw,sh,c)
 for row=0,2 do for col=0,2 do
  local dx=col==0 and 0 or col==1 and c or w-c;local dy=row==0 and 0 or row==1 and c or h-c
  repeatRegion(im,materials,x+dx,y+dy,col==1 and w-2*c or c,row==1 and h-2*c or c,u+(col==0 and 0 or col==1 and c or sw-c),v+(row==0 and 0 or row==1 and c or sh-c),col==1 and sw-2*c or c,row==1 and sh-2*c or c)
 end end
end
local function panel(im,x,y,w,h)
 repeatRegion(im,materials,x+3,y+3,w-6,h-6,314,114,64,48);rim(im,'paper_frame',x,y,w,h)
end
local function shell(im,x,y,w,h)
 rect(im,x+3,y+5,w,h,'#00000090');repeatRegion(im,materials,x+4,y+4,w-8,h-8,6,215,98,23);rim(im,'stone_frame',x,y,w,h)
 panel(im,x+8,y+7,w-15,h-14);rect(im,x+5,y+7,2,h-14,'#191C23');rect(im,x+7,y+7,1,h-14,'#777987')
end
local function header(im,x,y,w)
 patch(im,x+3,y+3,w-6,38,5,4,390,29,5);rim(im,'wood_frame',x,y,w,44);panel(im,x+51,y+5,w-102,34)
 im:drawImage(dashboard('banner'),Point(x+30,y+6));im:drawImage(dashboard('banner'),Point(x+w-46,y+6))
 im:drawImage(dashboard('lantern'),Point(x+9,y+8));im:drawImage(dashboard('lantern'),Point(x+w-25,y+8))
 im:drawImage(dashboard('vine'),Point(x-5,y-16));im:drawImage(dashboard('vine'),Point(x+w-33,y-16))
end
local function button(surfaces,labels,s,x,y,w,enabled,icon)
 patch(surfaces,x,y,w,20,319,249,80,20,3)
 if not enabled then rect(surfaces,x+3,y+3,w-6,14,'#17191B70');rect(surfaces,x+2,y+2,w-4,16,'#0F110E78')end
 local reserve=icon and 18 or 0
 if icon then labels:drawImage(image(root..'/Server/local/settlements-art-20261020/native/'..icon..'.png'),Point(x+3,y+2))end
 s=clip(s,math.max(1,w-reserve-8));text(labels,s,x+reserve+math.floor((w-reserve-width(s))/2),y+6,w-reserve-8,enabled and PALE or '#AAA38C')
end
local function title(labels,s,x,y,w)
 s=clip(s,math.floor(w/1.4));local glyphImage=Image(math.max(1,width(s)),12,ColorMode.RGB)
 text(glyphImage,s,0,0,glyphImage.width,INK)
 -- The source title itself uses1.4x; this remains a labelled editor approximation of GPU text.
 glyphImage:resize{width=math.ceil(glyphImage.width*1.4),height=17,method='nearest'}
 labels:drawImage(glyphImage,Point(x,y))
end
local function card(im,x,y,w)
 repeatRegion(im,dashboard('card'),x+5,y+5,w-10,30,5,5,14,14);rim(im,'card',x,y,w,40)
 rect(im,x+5,y+4,w-10,1,'#BFA67C');rect(im,x+4,y+5,1,4,'#BFA67C');rect(im,x+w-5,y+5,1,4,'#BFA67C')
end
local function save(id,pw,ph,heading,draw)
 local margin=20;local sprite=Sprite(pw+40,ph+40,ColorMode.RGB)
 local layers={sprite.layers[1],sprite:newLayer(),sprite:newLayer()}
 layers[1].name='01 existing native materials';layers[2].name='02 source control geometry';layers[3].name='03 sample dynamic data - EDITOR ONLY'
 local images={Image(sprite.spec),Image(sprite.spec),Image(sprite.spec)}
 shell(images[1],margin,margin+44,pw,ph-44);header(images[1],margin,margin,pw)
 title(images[3],heading,margin+57,margin+17,pw-120)
 draw(images[2],images[3],margin,pw,ph)
 for i=1,3 do sprite:newCel(layers[i],sprite.frames[1],images[i],Point(0,0))end
 sprite:saveCopyAs(out..'/'..id..'.aseprite');sprite:saveCopyAs(out..'/'..id..'.png')
 -- Enlargement is review-only; never exported to a game resource.
 app.command.SpriteSize{width=sprite.width*2,height=sprite.height*2,method='nearest'}
 sprite:saveCopyAs(out..'/'..id..'-2x.png');sprite:close()
end
save('01-directory-c1',460,148+3*43,tr('title'),function(surface,labels,m,w,h)
 local cx,cy,cw,bottom=m+8,m+60,w-16,m+h-22
 rect(surface,cx,cy,cw,20,'#A0A0A0');rect(surface,cx+1,cy+1,cw-2,18,'#000000')
 text(labels,tr('search'),cx+4,cy+6,cw-8,'#707070')
 local rows={{'Речная долина',12,84},{'Сосновый берег',8,70},{'Тихий луг',5,36}}
 for i,row in ipairs(rows)do local y=cy+26+(i-1)*43;card(surface,cx,y,cw-4)
  local bell=Image(image(root..'/Server/local/settlements-art-20261020/native/bell.png'));bell:resize{width=20,height=20,method='nearest'}
  labels:drawImage(bell,Point(cx+7,y+9));text(labels,row[1],cx+35,y+6,cw-46,INK)
  text(labels,format(tr('town_row'),'Обычный мир',row[2],row[3]),cx+35,y+18,cw-46,MUTED)
 end
 button(surface,labels,tr('create'),cx+cw-100,bottom-24,100,true)
 text(labels,format(tr('directory_count'),3),cx,bottom-18,cw-110,MUTED)
end)
save('11-chest-content-c1',364,168+18*6,'Речная долина',function(surface,labels,m,w,h)
 local cx,cy,cw,bottom=m+10,m+80,w-20,m+h-22
 button(surface,labels,tr('section.chests'),m+10,m+53,w-20,true)
 button(surface,labels,tr('back'),cx,cy,52,true)
 text(labels,format(lang['jem.settlements.storage_name'],1),cx+58,cy+5,cw-58,INK)
 text(labels,format(tr('coordinates'),'128, 64, −207'),cx,cy+25,cw,MUTED)
 local columns=9;local capacity=columns*math.floor((bottom-52-(cy+40))/18)
 local gx,gy=cx+math.floor((cw-columns*18)/2),cy+40
 for i=0,capacity-1 do local x,y=gx+(i%columns)*18,gy+math.floor(i/columns)*18
  rect(surface,x,y,18,18,'#72614C');rect(surface,x+1,y+1,16,16,'#211B16')
 end
 button(surface,labels,'<',cx,bottom-44,24,false);button(surface,labels,'>',cx+28,bottom-44,24,true)
 text(labels,format(tr('chest_slots'),1,capacity,54),cx+58,bottom-38,cw-58,MUTED)
 button(surface,labels,tr('chest_feed_all'),cx,bottom-20,cw,true,'bread')
end)
