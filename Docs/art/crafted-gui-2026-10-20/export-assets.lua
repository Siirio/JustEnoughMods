-- Aseprite batch production export. Parameter `job` points to an explicit Lua contract.
local job = dofile(app.params.job)
local incoming = app.open(job.input)
assert(incoming, 'Cannot open production artwork: ' .. job.input)
local source = Image(incoming.width, incoming.height, ColorMode.RGB)
source:drawSprite(incoming, 1)
local sw, sh = incoming.width, incoming.height
incoming:close()
local records = io.open(job.report, 'w')
records:write('asset,source_x,source_y,source_width,source_height,export_width,export_height\n')
for i, name in ipairs(job.names) do
  local x, y, w, h
  if job.rectangles then
    local bounds = job.rectangles[i]
    x,y,w,h = bounds[1],bounds[2],bounds[3],bounds[4]
  elseif job.columns then
    local column, row = (i-1) % job.columns, math.floor((i-1) / job.columns)
    x, y = math.floor(column*sw/job.columns), math.floor(row*sh/job.rows)
    w = math.floor((column+1)*sw/job.columns)-x
    h = math.floor((row+1)*sh/job.rows)-y
  else
    w, h = sw, sh
    if w/h > job.width/job.height then w = math.floor(h*job.width/job.height)
    else h = math.floor(w*job.height/job.width) end
    x, y = math.floor((sw-w)/2), math.floor((sh-h)/2)
  end
  local cropWidth, cropHeight = w,h
  local cropped = Image(w, h, ColorMode.RGB)
  cropped:drawImage(source, Point(-x, -y))
  if job.sourceCorner then
    local assembled = Image(job.width, job.height, ColorMode.RGB)
    local function map(p, target, total, sourceInset, targetInset)
      if p < targetInset then return math.floor(p*sourceInset/targetInset) end
      if p >= target-targetInset then return total-sourceInset+math.floor((p-target+targetInset)*sourceInset/targetInset) end
      return sourceInset+math.floor((p-targetInset)*(total-2*sourceInset)/(target-2*targetInset))
    end
    for py=0,job.height-1 do for px=0,job.width-1 do
      assembled:putPixel(px,py,cropped:getPixel(map(px,job.width,w,job.sourceCorner,job.corner),map(py,job.height,h,job.sourceCorner,job.corner)))
    end end
    cropped=assembled; w=job.width; h=job.height
  end
  local sprite = Sprite(w, h, ColorMode.RGB)
  sprite.layers[1].name = 'Generated production artwork'
  sprite:newCel(sprite.layers[1], 1, cropped, Point(0, 0))
  app.activeSprite = sprite
  app.command.SpriteSize{ui=false, width=job.width, height=job.height, method='nearest'}
  local guide = sprite:newLayer()
  guide.name = 'Protected geometry guide (not exported)'
  local geometry = Image(job.width, job.height, ColorMode.RGB)
  local line = app.pixelColor.rgba(255, 0, 255, 150)
  local inset = job.corner or 0
  for py=0,job.height-1 do
    geometry:putPixel(0,py,line); geometry:putPixel(job.width-1,py,line)
    if inset>0 then geometry:putPixel(inset,py,line); geometry:putPixel(job.width-inset-1,py,line) end
  end
  for px=0,job.width-1 do
    geometry:putPixel(px,0,line); geometry:putPixel(px,job.height-1,line)
    if inset>0 then geometry:putPixel(px,inset,line); geometry:putPixel(px,job.height-inset-1,line) end
  end
  sprite:newCel(guide,1,geometry,Point(0,0))
  guide.isVisible = false
  guide.isEditable = false
  sprite:saveAs(job.sourceDir .. '/' .. name .. '.aseprite')
  for _, destination in ipairs(job.destinations) do sprite:saveCopyAs(destination .. '/' .. name .. '.png') end
  records:write(string.format('%s,%d,%d,%d,%d,%d,%d\n',name,x,y,cropWidth,cropHeight,job.width,job.height))
  sprite:close()
end
records:close()
