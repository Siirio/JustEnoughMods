-- Remove unmistakable bright red matte residue on non-red-semantic artwork.
local root=app.params.root
local report=io.open(root .. '/manifests/' .. (app.params.only or 'icon') .. '-edge-refinement.csv','w')
report:write('job,asset,removed_bright_red_boundary_pixels\n')
local protected={blood_moon=true,mood_sad=true,roles=true,calendar=true,events=true,warning=true}
for _,jobName in ipairs(app.params.only and {app.params.only} or {'smp-icons','town-icons'}) do
  local job=dofile(root .. '/manifests/' .. jobName .. '.lua')
  for _,name in ipairs(job.names) do
    local sprite=app.open(job.sourceDir .. '/' .. name .. '.aseprite')
    local layer
    for _,candidate in ipairs(sprite.layers) do if candidate.name=='Pixel cleanup: isolated alpha flecks removed' then layer=candidate end end
    assert(layer,'Run initial icon cleanup first')
    local image=Image(layer:cel(1).image)
    local removed=0
    if not protected[name] then
      local targets={}
      for y=0,image.height-1 do for x=0,image.width-1 do
        local p=image:getPixel(x,y)
        local r,g,b,a=app.pixelColor.rgbaR(p),app.pixelColor.rgbaG(p),app.pixelColor.rgbaB(p),app.pixelColor.rgbaA(p)
        if a>0 and r>=220 and g<=65 and b<=65 then
          local boundary=false
          for dy=-1,1 do for dx=-1,1 do
            local nx,ny=x+dx,y+dy
            if nx<0 or ny<0 or nx>=image.width or ny>=image.height or app.pixelColor.rgbaA(image:getPixel(nx,ny))==0 then boundary=true end
          end end
          if boundary then targets[#targets+1]={x,y} end
        end
      end end
      for _,point in ipairs(targets) do image:putPixel(point[1],point[2],0);removed=removed+1 end
    end
    layer:cel(1).image=image
    sprite:saveAs(job.sourceDir .. '/' .. name .. '.aseprite')
    for _,destination in ipairs(job.destinations) do sprite:saveCopyAs(destination .. '/' .. name .. '.png') end
    report:write(jobName .. ',' .. name .. ',' .. removed .. '\n')
    sprite:close()
  end
end
report:close()
