-- Production asset contact sheets, assembled from the exact exported PNG files.
local root = app.params.root
local function sheet(jobName, columns, width, height, scale)
  local job=dofile(root .. '/manifests/' .. jobName .. '.lua')
  local rows=math.ceil(#job.names/columns)
  local sprite=Sprite(columns*width,rows*height,ColorMode.RGB)
  local image=Image(sprite.width,sprite.height,ColorMode.RGB)
  for i,name in ipairs(job.names) do
    local imported=app.open(job.destinations[1] .. '/' .. name .. '.png')
    local element=Image(imported.width,imported.height,ColorMode.RGB)
    element:drawSprite(imported,1)
    imported:close()
    image:drawImage(element,Point((i-1)%columns*width,math.floor((i-1)/columns)*height))
  end
  sprite:newCel(sprite.layers[1],1,image,Point(0,0))
  sprite.layers[1].name='Exact exported asset contact sheet'
  app.activeSprite=sprite
  app.command.SpriteSize{ui=false,width=sprite.width*scale,height=sprite.height*scale,method='nearest'}
  sprite:saveCopyAs(root .. '/reference/' .. jobName .. '-export-contact.png')
  sprite:close()
end
sheet('materials',3,64,64,4)
sheet('smp-icons',4,32,32,4)
sheet('town-icons',4,32,32,4)
