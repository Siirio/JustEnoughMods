-- Native 64x32 hand-directed material atlas, painted and exported only in Aseprite.
local out=app.params.output
local sprite=Sprite(64,32,ColorMode.RGB)
local function c(hex)return app.pixelColor.rgba(tonumber(hex:sub(1,2),16),tonumber(hex:sub(3,4),16),tonumber(hex:sub(5,6),16),255)end
local layers={sprite.layers[1],sprite:newLayer(),sprite:newLayer()}
layers[1].name='01 material masses';layers[2].name='02 directional form and grain';layers[3].name='03 contact highlights and hardware'
local ims={Image(sprite.spec),Image(sprite.spec),Image(sprite.spec)}
local function r(l,x,y,w,h,col)for py=y,y+h-1 do for px=x,x+w-1 do ims[l]:drawPixel(px,py,c(col))end end end
-- Warm oak side: broad fibre changes and one connected knot, no random noise.
r(1,0,0,16,16,'936B3E');r(2,0,0,16,2,'C59C60');r(2,0,2,16,1,'B58950');r(2,0,14,16,2,'64482F')
r(2,1,5,8,1,'AD824B');r(2,2,6,5,1,'A27842');r(2,10,4,6,1,'735231');r(2,11,5,4,1,'735231')
r(2,0,10,5,1,'735231');r(2,2,11,8,1,'735231');r(2,6,12,10,1,'AE8249');r(3,11,6,3,2,'67462D');r(3,10,6,1,1,'C29659');r(3,12,8,3,1,'AF8146')
-- Cut end/cross grain for rails and floor, intentionally different from sides.
r(1,16,0,16,16,'AA814A');r(2,16,0,16,2,'D0A766');r(2,16,14,16,2,'775535');r(2,17,4,12,1,'906435');r(2,18,5,9,1,'906435');r(2,20,8,11,1,'BF9556');r(2,17,11,8,1,'906435');r(3,23,6,4,2,'765331');r(3,24,6,2,1,'D1AA69')
-- Quiet shaded interior.
r(1,32,0,16,16,'59402C');r(2,32,0,16,2,'79603D');r(2,32,10,11,1,'684C30');r(2,39,6,9,1,'4C3728');r(2,32,15,16,1,'3A2D25')
-- Pan rim iron: blue-grey edge, wide quiet mid face, dark underside.
r(1,48,0,16,16,'515B60');r(2,48,0,16,2,'A7AFAB');r(2,48,2,16,2,'737E81');r(2,48,12,16,4,'30383C');r(3,49,4,5,1,'879194');r(3,58,5,6,1,'637174');r(3,48,14,10,1,'222B30')
-- Brass fittings have their own warm light/shadow family.
r(1,0,16,16,16,'B18A35');r(2,0,16,16,2,'F1D47C');r(2,0,18,16,2,'D2AC51');r(2,0,28,16,4,'785624');r(2,0,20,2,8,'D7B65F');r(2,13,20,3,8,'91702C');r(3,3,21,4,2,'E4C46A');r(3,9,26,4,1,'654A23')
-- Dark structural iron, not recoloured wood.
r(1,16,16,16,16,'343D42');r(2,16,16,16,2,'758285');r(2,16,18,16,1,'58666A');r(2,16,29,16,3,'1E292F');r(3,18,21,5,1,'46545A');r(3,26,25,6,1,'273139')
-- Strap/rivet face; bolt is pixels, not needless geometry.
r(1,32,16,16,16,'3C454A');r(2,32,16,16,1,'829091');r(2,32,30,16,2,'232E34');r(3,38,20,3,3,'1D292E');r(3,38,20,2,1,'B0B7B0');r(3,39,21,1,1,'7C898B');r(3,38,26,3,3,'1D292E');r(3,38,26,2,1,'9BA7A4')
-- Recessed pan interior; the brighter rim stays a separate UV island.
r(1,48,16,16,16,'333E43');r(2,48,16,16,2,'68787C');r(2,48,18,2,12,'53656A');r(2,50,28,14,4,'222E35');r(3,53,21,8,1,'3F5056');r(3,56,23,6,1,'3F5056')
for i=1,3 do sprite:newCel(layers[i],sprite.frames[1],ims[i],Point(0,0))end
sprite:saveAs(out..'/shipping-station-atlas.aseprite');sprite:saveCopyAs(out..'/shipping-station-atlas.png');sprite:close()
