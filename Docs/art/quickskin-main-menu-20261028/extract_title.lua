local source = app.params.source
local outputPng = app.params.output_png
local outputAseprite = app.params.output_aseprite

if not source or not outputPng or not outputAseprite then
  error("source, output_png and output_aseprite parameters are required")
end

local sourceSprite = app.open(source)
if not sourceSprite then
  error("Unable to open title source: " .. source)
end

local sourceImage = sourceSprite.cels[1].image
local width = sourceImage.width
local height = sourceImage.height
local extracted = Image(width, height, ColorMode.RGB)
extracted:clear()

local minimumX = width
local minimumY = height
local maximumX = -1
local maximumY = -1

local function clamp(value, lower, upper)
  if value < lower then return lower end
  if value > upper then return upper end
  return value
end

local function isInsideLetterRegion(x, y)
  local topLine = x >= 80 and x <= width - 80 and y >= 70 and y <= 760
  local bottomLine = x >= 390 and x <= width - 390 and y >= 500 and y <= height - 55
  return topLine or bottomLine
end

for pixel in sourceImage:pixels() do
  local x = pixel.x
  local y = pixel.y
  local value = pixel()
  local red = app.pixelColor.rgbaR(value)
  local green = app.pixelColor.rgbaG(value)
  local blue = app.pixelColor.rgbaB(value)
  local minimum = math.min(red, green, blue)
  local maximum = math.max(red, green, blue)
  local chroma = maximum - minimum
  local whiteScore = minimum - chroma * 0.72
  local alpha = clamp(math.floor((whiteScore - 145) * 4.25 + 0.5), 0, 255)

  if alpha > 0 and isInsideLetterRegion(x, y) then
    extracted:drawPixel(x, y, app.pixelColor.rgba(255, 255, 255, alpha))
    minimumX = math.min(minimumX, x)
    minimumY = math.min(minimumY, y)
    maximumX = math.max(maximumX, x)
    maximumY = math.max(maximumY, y)
  end
end

sourceSprite:close()

if maximumX < minimumX or maximumY < minimumY then
  error("Title extraction produced no visible pixels")
end

local margin = 24
minimumX = math.max(0, minimumX - margin)
minimumY = math.max(0, minimumY - margin)
maximumX = math.min(width - 1, maximumX + margin)
maximumY = math.min(height - 1, maximumY + margin)

local croppedWidth = maximumX - minimumX + 1
local croppedHeight = maximumY - minimumY + 1
local titleSprite = Sprite(croppedWidth, croppedHeight, ColorMode.RGB)
titleSprite.filename = outputAseprite
titleSprite.layers[1].name = "Extracted white title"
titleSprite.cels[1].image:drawImage(extracted, Point(-minimumX, -minimumY))
titleSprite:saveAs(outputAseprite)
titleSprite:saveCopyAs(outputPng)
titleSprite:close()
