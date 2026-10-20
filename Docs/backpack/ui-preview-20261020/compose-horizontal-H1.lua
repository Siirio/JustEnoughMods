-- Editor composition only. Native texture pixels and source consumer coordinates.
-- Run with the locally installed Aseprite --batch --script compose.lua.
local root = 'C:/MyProjects/Minecraft/JustEnoughMods/Docs/backpack/ui-preview-20261020/'
local native = root .. 'native/'
local fontmap = dofile(root .. 'font-map.lua')
local cache = {}
local function asset(name)
  if not cache[name] then cache[name] = Image{fromFile=native .. name} end
  return cache[name]
end
local pc = app.pixelColor
local cream = pc.rgba(238,219,192,255)
local ink = pc.rgba(101,78,52,255)
local white = pc.rgba(241,234,218,255)
local function blit(dst,name,sx,sy,sw,sh,x,y,w,h)
  local src=asset(name);w=w or sw;h=h or sh
  for dy=0,h-1 do for dx=0,w-1 do
    local c=src:getPixel(sx+math.floor(dx*sw/w),sy+math.floor(dy*sh/h))
    if pc.rgbaA(c)>0 then dst:drawPixel(x+dx,y+dy,c) end
  end end
end
local function nine(dst,sx,sy,sw,sh,b,x,y,w,h)
  for dy=0,h-1 do for dx=0,w-1 do
    local u=dx<b and dx or dx>=w-b and sw-(w-dx) or b+math.floor((dx-b)*(sw-2*b)/(w-2*b))
    local v=dy<b and dy or dy>=h-b and sh-(h-dy) or b+math.floor((dy-b)*(sh-2*b)/(h-2*b))
    local c=asset('backpack_assets.png'):getPixel(sx+u,sy+v)
    if pc.rgbaA(c)>0 then dst:drawPixel(x+dx,y+dy,c) end
  end end
end
local function text(dst,str,x,y,color)
  for _,cp in utf8.codes(str) do
    if cp==32 then x=x+4 else
      local g=fontmap[cp]
      if g then
        local src=asset(g[1]);local cw=src.width/16
        local ch=8;local max=-1
        for gy=0,ch-1 do for gx=0,cw-1 do
          local p=src:getPixel(g[2]*cw+gx,g[3]*ch+gy)
          if pc.rgbaA(p)>0 then dst:drawPixel(x+gx,y+gy,color);max=math.max(max,gx) end
        end end
        x=x+max+2
      end
    end
  end
  return x
end
local function slots(dst,x,y,cols,rows)
  for row=0,rows-1 do for col=0,cols-1 do blit(dst,'backpack_assets.png',238,128,18,18,x+col*18,y+row*18) end end
end
local function workbenchIcon(dst,x,y)
 local rows={'............','.ssssssssss.','.sllsllslls.','.ssssssssss.','.sllsllslls.','.ssssssssss.','.sllsllslls.','.ssssssssss.','..ll....ll..','..ll....ll..','..ll....ll..','..ss....ss..'}
 for yy,row in ipairs(rows) do for xx=1,12 do local c=row:sub(xx,xx);if c~='.' then dst:drawPixel(x+xx-1,y+yy-1,c=='l' and cream or pc.rgba(152,134,107,255)) end end end
end
local function item(dst,name,x,y) blit(dst,name,0,0,16,16,x,y) end
local function bay(dst,x,y,which,on)
 nine(dst,20,56,20,20,6,x,y,20,20)
 if which==1 then workbenchIcon(dst,x+4,y+4)
 elseif which==3 then blit(dst,'apple.png',0,0,16,16,x+4,y+4,12,12)
 else
  -- Native furnace inventory model is rendered by Minecraft, not available offline.
  -- Leave its icon absent rather than introduce an inaccurate replacement.
 end
 blit(dst,'backpack_assets.png',on and 202 or 212,138,10,10,x+21,y)
 blit(dst,'backpack_assets.png',202,168,10,10,x+21,y+10)
end
local function make(name,work,furnace,feed,lit,filled,picker,sampleRows)
 local rows=sampleRows or 3;local canvasHeight=rows>3 and 370 or 330
 local s=Sprite(300,canvasHeight,ColorMode.RGB)
 s.layers[1].name='Editor background; not game scenery'
 local bg=Image(300,canvasHeight,ColorMode.RGB);bg:clear(pc.rgba(31,33,35,255));s:newCel(s.layers[1],1,bg)
 local function layer(name) local l=s:newLayer();l.name=name;return l,Image(300,canvasHeight,ColorMode.RGB) end
 local l,im=layer('Native Backpacked surfaces and source-sized slots')
 local x,y,w=66,25,184;local nativeTop=44+rows*18;local storageH=25+rows*18
 local trunc=function(v) return math.modf(v) end
 local bayTop=16+trunc((storageH-70)/2);local actionsTop=16+trunc((storageH-68)/2)+5
 local rowTop=(work and furnace) and math.max(nativeTop,bayTop+70+5+4,actionsTop+58+5+4) or nativeTop
 local top=y+rowTop;local left=x+(w-((work and furnace) and 228 or 112))/2
 local inserted=(work or furnace) and (80+rowTop-nativeTop) or 0
 nine(im,128,0,128,128,11,x,y+16,w,25+rows*18)
 slots(im,x+11,y+27,9,rows)
 -- Native title label: 110px, with rename control in its native group.
 nine(im,74,14,15,15,4,x+37,y+5,110,16)
 blit(im,'backpack_assets.png',182,128,10,10,x+134,y+8)
 -- Three unlocked augment bays in a sample native 9x3 backpack.
 local bayX=x-31-(storageH<80 and 8 or 2)
 nine(im,74,14,15,15,4,bayX-5,y+bayTop-5,41,80)
 for i=1,3 do bay(im,bayX,y+bayTop+(i-1)*25,i,i==1 and work or i==2 and furnace or i==3 and feed) end
 -- Native right-hand action strip (sample ordinary default visibility).
 local actionsX=x+w+2+(68>storageH-10 and 6 or 0)
 nine(im,74,14,15,15,4,actionsX-5,y+actionsTop-5,20,68)
 for i,uv in ipairs({{172,128},{202,128},{212,158},{212,128},{192,128}}) do blit(im,'backpack_assets.png',uv[1],uv[2],10,10,actionsX,y+actionsTop+(i-1)*12) end
 if work then
  nine(im,128,0,128,128,11,left,top,112,80)
  slots(im,left+11,top+11,3,3);slots(im,left+84,top+29,1,1)
  blit(im,'backpack_assets.png',29,0,4,6,left+72,top+34,8,8)
 end
 if furnace then
  local left=left+(work and 116 or 0);local fy=top
  nine(im,128,0,128,128,11,left,fy,112,80)
  slots(im,left+21,fy+11,1,1);slots(im,left+21,fy+47,1,1);slots(im,left+81,fy+29,1,1)
  blit(im,'furnace.png',79,34,24,17,left+49,fy+31)
  if lit then
   local flame=10
   blit(im,'furnace.png',176,12-flame,14,flame,left+23,fy+45-flame)
   blit(im,'furnace.png',176,14,12,16,left+49,fy+31)
  end
 end
 local iy=y+44+rows*18+inserted
 if inserted>0 then
  nine(im,64,49,64,64,5,x+4,iy,176,101)
  slots(im,x+11,iy+18,9,3);slots(im,x+11,iy+76,9,1)
 else
  nine(im,66,31,15,15,4,x+4,iy,176,101)
  for row=0,2 do for col=0,8 do blit(im,'backpack_assets.png',81,31,18,18,x+11+col*18,iy+18+row*18) end end
  for col=0,8 do blit(im,'backpack_assets.png',81,31,18,18,x+11+col*18,iy+76) end
 end
 s:newCel(l,1,im)
 local il,ii=layer('Sample item data; not live backpack contents')
 if filled then
  item(ii,'apple.png',x+12,y+28);item(ii,'coal.png',x+30,y+28);item(ii,'raw_iron.png',x+48,y+28)
  if work then
   for col=0,2 do item(ii,'iron_ingot.png',left+12+col*18,top+12) end
   item(ii,'stick.png',left+30,top+30);item(ii,'stick.png',left+30,top+48);item(ii,'iron_pickaxe.png',left+85,top+30)
  end
  if furnace then local left=left+(work and 116 or 0);local fy=top;item(ii,'raw_iron.png',left+22,fy+12);item(ii,'coal.png',left+22,fy+48);item(ii,'iron_ingot.png',left+82,fy+30) end
 end
 s:newCel(il,1,ii)
 local tl,ti=layer('Native font; editor evidence caption outside the menu')
 text(ti,'Рюкзак',x+69,y+9,ink);text(ti,'Инвентарь',x+12,iy+7,pc.rgba(152,120,86,255))
 text(ti,'КОМПОЗИЦИЯ ИЗ ИСХОДНИКОВ',8,5,white)
 text(ti,'Не игровой снимок. Пример: 9x'..rows..'.',8,canvasHeight-16,white)
 s:newCel(tl,1,ti)
 if picker then
  local pl,pi=layer('Native augment picker; icon-only excerpt, source order native then JEM')
  nine(pi,64,49,64,64,5,78,144,146,88)
  text(pi,'Pick an Augment',86,153,ink)
  for i=0,11 do
   local bx=86+(i%6)*22;local by=169+math.floor(i/6)*22
   nine(pi,20,56,20,20,6,bx,by,20,20)
   blit(pi,'backpack_assets.png',12+i*12,244,12,12,bx+4,by+4)
  end
  for i=0,2 do local bx=86+i*22;local by=213;nine(pi,20,56,20,20,6,bx,by,20,20)
   if i==0 then item(pi,'apple.png',bx+2,by+2) elseif i==2 then workbenchIcon(pi,bx+4,by+4) end
  end
  s:newCel(pl,1,pi)
 end
 s:saveAs(root..'H1-'..name..'.aseprite')
 s:saveCopyAs(root..'H1-'..name..'-1x.png')
 s:close()
end
make('01-active',true,true,true,true,true,false)
make('02-idle',true,true,false,false,false,false)
make('03-furnace-only',false,true,true,true,true,false)
make('06-workbench-only',true,false,true,false,true,false)
make('04-all-disabled',false,false,false,false,false,false)
make('07-small-9x1',true,true,true,true,true,false,1)
make('08-configured-9x5',true,true,true,true,true,false,5)
-- Selector evidence is separate: sort changes with locale/config/installed augments.
local s=Sprite(300,190,ColorMode.RGB)
s.layers[1].name='Editor background and evidence captions'
local im=Image(300,190,ColorMode.RGB);im:clear(pc.rgba(31,33,35,255))
text(im,'СЕЛЕКТОР: ПРИМЕР ENGLISH SORT',8,6,white)
text(im,'Не игровой снимок. Порядок зависит',8,153,white)
text(im,'от языка, настроек и улучшений.',8,164,white)
text(im,'3D-иконка Furnace не воспроизведена.',8,178,white)
s:newCel(s.layers[1],1,im)
local l=s:newLayer();l.name='Native six-column picker; original native icons; selected entries disabled'
im=Image(300,190,ColorMode.RGB)
nine(im,64,49,64,64,5,77,30,146,110)
text(im,'Pick an Augment',85,39,ink)
local icons={0,'apple',12,24,'furnace',36,48,60,72,84,96,108,120,132,144,'workbench'}
for i,icon in ipairs(icons) do
 local bx=85+((i-1)%6)*22;local by=55+math.floor((i-1)/6)*22
 local installed=type(icon)=='string'
 nine(im,installed and 0 or 20,56,20,20,6,bx,by,20,20)
 if icon=='apple' then item(im,'apple.png',bx+2,by+2)
 elseif icon=='workbench' then workbenchIcon(im,bx+4,by+4)
 elseif type(icon)=='number' then blit(im,'backpack_assets.png',icon,244,12,12,bx+4,by+4) end
end
s:newCel(l,1,im)
s:saveAs(root..'H1-05-selector.aseprite');s:saveCopyAs(root..'H1-05-selector-1x.png');s:close()
