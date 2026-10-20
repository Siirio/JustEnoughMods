-- Original native-resolution artwork and native Minecraft-font previews in Aseprite.
-- No external rasterization, generated images, image conversion or resized export.
local function read(path)local f=assert(io.open(path,"rb"));local s=f:read("*a");f:close();return s end
local spec=json.decode(read(app.params.spec))
local fonts=json.decode(read(spec.fonts))
local function progress(s)local f=assert(io.open(spec.fonts..'.progress','a'));f:write(s..'\n');f:close()end
local glyphs={}
for _,provider in ipairs(fonts) do
 local atlas=Image{fromFile=provider['local']}
 -- Aseprite JSON decodes the all-NUL first ASCII row as an empty string.
 -- Derive the column count from populated rows, never divide by that row.
 local columns=0;for _,chars in ipairs(provider.chars)do columns=math.max(columns,utf8.len(chars))end
 local cellW=atlas.width/columns;local cellH=atlas.height/#provider.chars
 progress(provider['local']..' '..atlas.width..'x'..atlas.height..' '..cellW..'/'..cellH)
 for row,chars in ipairs(provider.chars) do
  local col=0
  for _,cp in utf8.codes(chars) do
   if cp~=0 and not glyphs[cp] then
    local width=0
    for y=0,cellH-1 do for x=0,cellW-1 do if app.pixelColor.rgbaA(atlas:getPixel(col*cellW+x,(row-1)*cellH+y))>0 then width=math.max(width,x+1) end end end
    glyphs[cp]={image=atlas,x=col*cellW,y=(row-1)*cellH,width=width,height=cellH,offset=7-provider.ascent,advance=width+1}
   end
   col=col+1
  end
 end
end
local function rgba(s)local h=s:gsub('#','');return app.pixelColor.rgba(tonumber(h:sub(1,2),16),tonumber(h:sub(3,4),16),tonumber(h:sub(5,6),16),tonumber(h:sub(7,8),16) or 255)end
for _,icon in ipairs(spec.authoredIcons)do
 local sprite=Sprite(16,16,ColorMode.RGB);local image=Image(sprite.spec)
 for _,r in ipairs(icon.shapes)do for y=r[2],r[2]+r[4]-1 do for x=r[1],r[1]+r[3]-1 do image:drawPixel(x,y,rgba(r[5]))end end end
 sprite:newCel(sprite.layers[1],sprite.frames[1],image,Point(0,0));sprite:saveCopyAs(icon.source);sprite:saveCopyAs(icon.output);sprite:close()
end
local function width(s)local n=0;for _,cp in utf8.codes(s) do local g=glyphs[cp] or glyphs[63];n=n+(cp==32 and 4 or g.advance) end;return n>0 and n-1 or 0 end
local function clipped(s,w)
 if width(s)<=w then return s end
 local r='';for _,cp in utf8.codes(s) do local next=r..utf8.char(cp);if width(next..'...')>w then break end;r=next end;return r..'...'
end
local function wrapped(s,w)
 local lines={}
 for paragraph in (s..'\n'):gmatch('(.-)\n') do
  local line=''
  for word in paragraph:gmatch('[^ \t\r\n]+') do
   local next=line=='' and word or line..' '..word
   if width(next)>w and line~='' then table.insert(lines,line);line=word else line=next end
  end
  table.insert(lines,line)
 end
 return lines
end
local images={}
progress('fonts done')
for _,page in ipairs(spec.pages) do
 progress(page.id)
 local sprite=Sprite(page.width,page.height,ColorMode.RGB)
 local layers={sprite.layers[1],sprite:newLayer(),sprite:newLayer()}
 layers[1].name='01 painted materials';layers[2].name='02 controls and fixed bounds';layers[3].name='03 preview data - not game texture'
 local canvases={Image(sprite.spec),Image(sprite.spec),Image(sprite.spec)}
 for i=1,3 do sprite:newCel(layers[i],sprite.frames[1],canvases[i],Point(0,0)) end
 local function rect(im,x,y,w,h,color)for py=math.max(0,y),math.min(page.height-1,y+h-1)do for px=math.max(0,x),math.min(page.width-1,x+w-1)do im:drawPixel(px,py,color)end end end
 local function text(im,c)
  local lines=c.wrap and wrapped(c.text,c.w) or {clipped(c.text,c.w or page.width)}
  local color=rgba(c.color or '#EFE4C8')
  for line,s in ipairs(lines)do
   local y=c.y+(line-1)*(c.lineHeight or 11)
   if y+8>c.y+(c.h or 8)then break end
   local x=c.x;if c.align=='center'then x=x+math.floor((c.w-width(s))/2)elseif c.align=='right'then x=x+c.w-width(s)end
   for _,cp in utf8.codes(s)do
    local g=glyphs[cp] or glyphs[63]
    if cp~=32 then for dy=0,g.height-1 do for dx=0,g.width-1 do
     local py=y+dy+g.offset
     if app.pixelColor.rgbaA(g.image:getPixel(g.x+dx,g.y+dy))>0 and x+dx>=c.x and x+dx<c.x+(c.w or page.width) and py>=0 and py<page.height then im:drawPixel(x+dx,py,color)end
    end end end
    x=x+(cp==32 and 4 or g.advance)
   end
  end
 end
 for _,c in ipairs(page.commands)do
  local im=canvases[c.layer or 1]
  if c.type=='rect'then rect(im,c.x,c.y,c.w,c.h,rgba(c.color))
  elseif c.type=='text'then text(im,c)
  elseif c.type=='image'then
   if not images[c.path]then images[c.path]=Image{fromFile=c.path}end
   im:drawImage(images[c.path],Point(c.x,c.y))
  end
 end
 -- newCel copies the image; commit the completed native-size surfaces explicitly.
 for i=1,3 do sprite.cels[i].image=canvases[i] end
 layers[3].isVisible=false;sprite:saveCopyAs(page.background)
 layers[3].isVisible=true;sprite:saveCopyAs(page.source);sprite:saveCopyAs(page.preview)
 sprite:close();print(page.id)
end
-- Native-pixel contact boards are review aids; game exports above remain unchanged.
local reviewPages={};for _,p in ipairs(spec.pages)do if p.id~='shipping-exchange'then table.insert(reviewPages,p)end end
for first=1,#reviewPages,9 do
 local sheet=Sprite(1536,960,ColorMode.RGB);local canvas=Image(sheet.spec)
 for i=first,math.min(first+8,#reviewPages)do
  local j=i-first;canvas:drawImage(Image{fromFile=reviewPages[i].preview},Point((j%3)*512,math.floor(j/3)*320))
 end
 sheet:newCel(sheet.layers[1],sheet.frames[1],canvas,Point(0,0))
 local output=app.fs.filePath(spec.pages[1].preview)..'/../contact-'..math.floor((first-1)/9+1)..'.png'
 sheet:saveCopyAs(output);sheet:close()
end
