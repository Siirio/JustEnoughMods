-- Collector repaint r2: material pixels authored in Aseprite at final64x64.
-- Exact face islands come from geometry, never a whole16px tile squeezed onto a rail.
local file=io.open(app.params.layout,'r');local layout=json.decode(file:read('*a'));file:close()
local out=app.params.output
local spr=Sprite(64,64,ColorMode.RGB)
local names={'01 substance','02 connected grain and plane values','03 selective metal contact','UV guide (hidden)'}
local layers={spr.layers[1],spr:newLayer(),spr:newLayer(),spr:newLayer()}
local images={Image(spr.spec),Image(spr.spec),Image(spr.spec),Image(spr.spec)}
for i=1,4 do layers[i].name=names[i]end
local function col(hex)return app.pixelColor.rgba(tonumber(hex:sub(1,2),16),tonumber(hex:sub(3,4),16),tonumber(hex:sub(5,6),16),255)end
local function pixel(l,x,y,hex)images[l]:drawPixel(x,y,col(hex))end
local function rect(l,x,y,w,h,hex)for yy=y,y+h-1 do for xx=x,x+w-1 do pixel(l,xx,yy,hex)end end end

for _,i in ipairs(layout.islands)do
 local x,y,w,h,role,v=i.x,i.y,i.width,i.height,i.material,i.variant
 local wood=role:sub(1,4)=='wood'
 local brass=role:sub(1,5)=='brass'
 local base=wood and '936A39' or brass and 'B28B3D' or '3B4242'
 if role=='wood_under' then base='65492E' end
 if role=='wood_top' then base='AE8347' end
 if role=='wood_post' then base='805C34' end
 if role=='wood_end' then base='9E7844' end
 if role=='iron_under' then base='242B2D' end
 if role=='iron_top' or role=='iron_rim_top' then base='626B69' end
 if role=='iron_bowl' then base='343C3C' end
 if role=='brass_top' then base='D3AD58' end
 if role=='brass_under' then base='785A2C' end
 rect(1,x,y,w,h,base)
 if role=='wood_board' then
   -- Each long face has only its actual1–2 rows, with broad connected fibres.
   if h>1 then rect(2,x,y+h-1,w,1,'7C582F') end
   if w>=6 then
     local start=v==0 and 1 or v==1 and 4 or 7
     if start<w then rect(2,x+start,y,math.min(3,w-start),1,'AB8045')end
     if h>1 and w>=10 then rect(2,x+(v==2 and 2 or 7),y+h-1,2,1,'8A6233')end
   end
 elseif role=='wood_post' then
   if w>1 then rect(2,x+w-1,y,1,h,'956E3C') end
   if h>=6 then rect(2,x,y+3,1,3,'73502F')end
 elseif role=='wood_top' then
   if h>1 then rect(2,x,y,w,1,'BA9455')end
   if w>=8 then rect(2,x+3,y+h-1,3,1,'946B39')end
 elseif role=='wood_end' then
   if w>1 and h>1 then pixel(2,x+w-1,y+h-1,'805D35')end
 elseif role=='wood_floor' then
   -- Actual broad floor islands can carry three plank fields at native density.
   for line=4,w-1,5 do rect(2,x+line,y,1,h,'765431')end
   if w>=8 and h>=6 then rect(2,x+1,y+3,2,4,'A77C43');rect(2,x+7,y+1,2,3,'886137')end
 elseif role=='iron_structure' then
   -- Neutral dark forged iron, no chrome strip pattern on every face.
   if w<=2 and h>=5 then
     pixel(3,x,y+1,'697371');rect(2,x+w-1,y+3,1,math.min(4,h-3),'303738')
   elseif w>=7 then
     rect(2,x+1,y,3,1,'4D5654');rect(2,x+w-4,y+h-1,3,1,'303738')
   end
 elseif role=='iron_strap' then
   if h>=5 then pixel(3,x,y+1,'858D85');pixel(2,x,y+2,'2A3131');pixel(3,x,y+h-2,'69736D')end
 elseif role=='iron_rim' then
   if w>=4 then rect(2,x,y,2,1,'4F5957')end
 elseif role=='iron_rim_top' then
   if w>=4 then pixel(3,x+1,y,'939A91')end
 elseif role=='iron_bowl' then
   if w>=3 and h>=3 then rect(2,x+1,y+1,w-2,h-2,'2C3435');rect(3,x,y,math.min(2,w),1,'55625F')end
 elseif role=='iron_hanger' then
   if h>=3 then pixel(3,x,y,'727D77')end
 elseif role=='brass_body' then
   if h>=2 then rect(2,x,y+h-1,w,1,'8D6B30')end
   if w>=2 then pixel(3,x,y,'D6B460')end
 end
 -- Edge extension is only a gutter for atlas sampling, not extra on-model detail.
 for yy=y-1,y+h do for xx=x-1,x+w do
   if xx<x or xx>=x+w or yy<y or yy>=y+h then
     local sx=math.max(x,math.min(x+w-1,xx));local sy=math.max(y,math.min(y+h-1,yy))
     local value=images[1]:getPixel(sx,sy)
     for l=2,3 do local c=images[l]:getPixel(sx,sy);if app.pixelColor.rgbaA(c)>0 then value=c end end
     images[1]:drawPixel(xx,yy,value)
   end
 end end
 for xx=x,x+w-1 do pixel(4,xx,y,'D95BCD')end
 for yy=y,y+h-1 do pixel(4,x,yy,'D95BCD')end
end
for i=1,4 do spr:newCel(layers[i],spr.frames[1],images[i],Point(0,0))end
layers[4].isVisible=false
spr:saveAs(out..'/shipping-station-atlas.aseprite')
spr:saveCopyAs(out..'/shipping-station-atlas.png')
spr:close()
