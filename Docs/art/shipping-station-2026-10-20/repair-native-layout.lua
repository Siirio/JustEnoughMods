local root="C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/shipping-station-2026-10-20/native/"
local sale=Sprite{fromFile=root.."shipping-ui.aseprite"}
local saleImage=Image(sale.spec);saleImage:drawSprite(sale,1)
local exchange=Sprite{fromFile=root.."shipping-exchange.aseprite"}
exchange.layers[1].isVisible=true;exchange.layers[2].isVisible=false;exchange.layers[3].isVisible=false
local quiet=exchange.layers[4]
local prior=quiet:cel(1);if prior then exchange:deleteCel(prior) end
local surface=Image(exchange.spec)
local function fill(x,y,w,h,r,g,b)
 local color=app.pixelColor.rgba(r,g,b,255)
 for yy=y,y+h-1 do for xx=x,x+w-1 do surface:drawPixel(xx,yy,color) end end
end
fill(179,2,176,16,135,96,58);fill(10,25,169,148,78,56,43);fill(187,25,172,148,78,56,43)
exchange:newCel(quiet,1,surface,Point(0,0))
local inventory=exchange:newLayer();inventory.name="05 shared native inventory from shipping-ui"
local image=Image(368,94,ColorMode.RGB)
for y=0,93 do for x=0,367 do image:drawPixel(x,y,saleImage:getPixel(x,y+178)) end end
exchange:newCel(inventory,1,image,Point(0,178))
exchange:saveAs(root.."shipping-exchange.aseprite")
local final=Image(exchange.spec);final:drawSprite(exchange,1);final:saveAs(root.."shipping-exchange.png")
exchange:close();sale:close()
