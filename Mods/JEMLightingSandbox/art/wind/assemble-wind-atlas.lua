local source = app.params["source"]
local output = app.params["output"]
local icon = Image{fromFile=source.."/icon.png"}
local document = Sprite(512, 96, ColorMode.RGB)
document.layers[1].name = "Calm - Windy logo silhouette"
local stormLayer = document:newLayer()
stormLayer.name = "Strong - Windy original frames"
local calm = document.cels[1].image
local rgba = app.pixelColor
local limits = {11,14,20,26,32,39,48,56,56,56,56,56}
local starts = {8,8,8,8,8,8,8,8,13,20,29,37}
for frame=0,11 do
  for y=25,41 do
    for x=starts[frame+1],limits[frame+1] do
      local pixel = icon:getPixel(x,y)
      if rgba.rgbaR(pixel)>210 and rgba.rgbaG(pixel)>210 and rgba.rgbaB(pixel)>210 then
        calm:drawPixel((frame%8)*64+x, math.floor(frame/8)*24+y-20, pixel)
      end
    end
  end
end
local storm = Image(document.spec)
for frame=0,18 do
  local index = frame+12
  local baseX = (index%8)*64
  local baseY = math.floor(index/8)*24
  local original = Image{fromFile=source.."/textures/particle/strong_wind/strong_wind_"..frame..".png"}
  for y=24,39 do
    for x=0,63 do
      local pixel=original:getPixel(x,y)
      if rgba.rgbaA(pixel)>0 then
        storm:drawPixel(baseX+x,baseY+y-20,pixel)
      end
    end
  end
end
document:newCel(stormLayer,1,storm,Point(0,0))
document:saveAs(output.."/wind_gusts.aseprite")
document:saveCopyAs(output.."/wind_gusts.png")