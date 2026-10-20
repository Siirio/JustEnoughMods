-- All native art and Minecraft-font preview pixels are painted by Aseprite.
local function read(path)local f=assert(io.open(path,'rb'));local s=f:read('*a');f:close();return s end
local spec=json.decode(read(app.params.spec));local providers=json.decode(read(spec.fonts));local glyphs={}
for _,provider in ipairs(providers)do
 local atlas=Image{fromFile=provider['local']};local columns=0
 for _,chars in ipairs(provider.chars)do columns=math.max(columns,utf8.len(chars))end
 local cw,ch=atlas.width/columns,atlas.height/#provider.chars
 for row,chars in ipairs(provider.chars)do local col=0
  for _,cp in utf8.codes(chars)do
   if cp~=0 and not glyphs[cp]then local width=0
    for y=0,ch-1 do for x=0,cw-1 do if app.pixelColor.rgbaA(atlas:getPixel(col*cw+x,(row-1)*ch+y))>0 then width=math.max(width,x+1)end end end
    glyphs[cp]={image=atlas,x=col*cw,y=(row-1)*ch,w=width,h=ch,offset=7-provider.ascent}
   end;col=col+1
  end
 end
end
local function rgba(s)local h=s:gsub('#','');return app.pixelColor.rgba(tonumber(h:sub(1,2),16),tonumber(h:sub(3,4),16),tonumber(h:sub(5,6),16),255)end
local images={}
for _,page in ipairs(spec.pages)do
 local sprite=Sprite(page.width,page.height,ColorMode.RGB);local layers={};local canvases={}
 local names={'01 Structural materials','02 Recesses and control bounds','03 Joinery cloth and states','04 Authored vignettes and native icons','05 Preview text - dynamic in game'}
 for i=1,5 do layers[i]=i==1 and sprite.layers[1]or sprite:newLayer();layers[i].name=names[i];canvases[i]=Image(sprite.spec)end
 local function rect(im,x,y,w,h,col)for py=math.max(0,y),math.min(page.height-1,y+h-1)do for px=math.max(0,x),math.min(page.width-1,x+w-1)do im:drawPixel(px,py,col)end end end
 for _,c in ipairs(page.commands)do local im=canvases[c.layer or 1]
  if c.type=='rect'then rect(im,c.x,c.y,c.w,c.h,rgba(c.color))
  elseif c.type=='image'then if not images[c.path]then images[c.path]=Image{fromFile=c.path}end;im:drawImage(images[c.path],Point(c.x,c.y))
  elseif c.type=='text'then local x=c.x;local scale=c.scale or 1
   for _,cp in utf8.codes(c.text)do local g=glyphs[cp]or glyphs[63]
    if cp~=32 then for dy=0,g.h-1 do for dx=0,g.w-1 do
     if app.pixelColor.rgbaA(g.image:getPixel(g.x+dx,g.y+dy))>0 and x+dx*scale<c.x+c.w then rect(im,x+dx*scale,c.y+(dy+g.offset)*scale,scale,scale,rgba(c.color))end
    end end end
    x=x+(cp==32 and 4 or g.w+1)*scale
   end
  end
 end
 for i=1,5 do sprite:newCel(layers[i],sprite.frames[1],canvases[i],Point(0,0))end
 layers[5].isVisible=false;sprite:saveCopyAs(page.background)
 layers[5].isVisible=true;sprite:saveCopyAs(page.source);sprite:saveCopyAs(page.preview)
 sprite:close();print('Painted '..page.id)
end
