-- Aseprite precision cleanup: retain each icon's connected silhouette.
-- Original imported pixels remain as a hidden native layer.
local root=app.params.root
local report=io.open(root .. '/manifests/' .. (app.params.only or 'icon') .. '-cleanup.csv','w')
report:write('job,asset,removed_disconnected_pixels\n')
for _,jobName in ipairs(app.params.only and {app.params.only} or {'smp-icons','town-icons'}) do
  local job=dofile(root .. '/manifests/' .. jobName .. '.lua')
  for _,name in ipairs(job.names) do
    local sprite=app.open(job.sourceDir .. '/' .. name .. '.aseprite')
    local original=sprite.layers[1]
    local image=Image(sprite.width,sprite.height,ColorMode.RGB)
    image:drawSprite(sprite,1)
    local visited,components={},{}
    local function opaque(x,y) return app.pixelColor.rgbaA(image:getPixel(x,y))>0 end
    for y=0,sprite.height-1 do for x=0,sprite.width-1 do
      local key=y*sprite.width+x
      if opaque(x,y) and not visited[key] then
        local queue={key};visited[key]=true
        local head=1
        while head<=#queue do
          local current=queue[head];head=head+1
          local cx,cy=current%sprite.width,math.floor(current/sprite.width)
          for dy=-1,1 do for dx=-1,1 do
            local nx,ny=cx+dx,cy+dy
            local nk=ny*sprite.width+nx
            if nx>=0 and ny>=0 and nx<sprite.width and ny<sprite.height and not visited[nk] and opaque(nx,ny) then
              visited[nk]=true;queue[#queue+1]=nk
            end
          end end
        end
        components[#components+1]=queue
      end
    end end
    table.sort(components,function(a,b) return #a>#b end)
    local removed=0
    for i=2,#components do
      -- Preserve meaningful disconnected objects; remove only tiny sampling flecks.
      if #components[i]<=4 then for _,key in ipairs(components[i]) do
        image:putPixel(key%sprite.width,math.floor(key/sprite.width),0);removed=removed+1
      end end
    end
    local clean=sprite:newLayer();clean.name='Pixel cleanup: isolated alpha flecks removed'
    sprite:newCel(clean,1,image,Point(0,0));original.isVisible=false
    sprite:saveAs(job.sourceDir .. '/' .. name .. '.aseprite')
    for _,destination in ipairs(job.destinations) do sprite:saveCopyAs(destination .. '/' .. name .. '.png') end
    report:write(jobName .. ',' .. name .. ',' .. removed .. '\n')
    sprite:close()
  end
end
report:close()
