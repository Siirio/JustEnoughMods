-- Read native Minecraft PNG alpha in Aseprite. No painting, conversion or resampling.
local input=app.params.input
local result={}
for _,name in ipairs({'diamond','cod','salmon'}) do
  local spr=app.open(input..'/'..name..'.png')
  local im=Image(spr.spec)
  im:drawSprite(spr,1)
  local rows={}
  for y=0,15 do
    local row=''
    for x=0,15 do row=row..(app.pixelColor.rgbaA(im:getPixel(x,y))>0 and '1' or '0') end
    rows[#rows+1]=row
  end
  result[name]=rows
  spr:close()
end
local f=io.open(app.params.output,'w');f:write(json.encode(result));f:close()
