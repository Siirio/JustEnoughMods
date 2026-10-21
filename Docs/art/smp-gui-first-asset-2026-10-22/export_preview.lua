-- Assemble existing vanilla glyphs for review only. This script paints no art,
-- constructs no shapes, and never rescales the authored card.
local function read(path)
  local f = assert(io.open(path, 'rb'))
  local data = f:read('*a')
  f:close()
  return data
end
local source = assert(app.params.source)
local output = assert(app.params.output)
local providers = json.decode(read(assert(app.params.fonts)))
local glyphs = {}
for _, provider in ipairs(providers) do
  local atlas = Image { fromFile = provider['local'] }
  local columns = 0
  for _, chars in ipairs(provider.chars) do columns = math.max(columns, utf8.len(chars)) end
  local cw, ch = atlas.width / columns, atlas.height / #provider.chars
  for row, chars in ipairs(provider.chars) do
    local col = 0
    for _, cp in utf8.codes(chars) do
      if cp ~= 0 and not glyphs[cp] then
        local width = 0
        for y = 0, ch - 1 do
          for x = 0, cw - 1 do
            if app.pixelColor.rgbaA(atlas:getPixel(col * cw + x, (row - 1) * ch + y)) > 0 then width = math.max(width, x + 1) end
          end
        end
        glyphs[cp] = { atlas = atlas, x = col * cw, y = (row - 1) * ch, w = width, h = ch, offset = 7 - provider.ascent }
      end
      col = col + 1
    end
  end
end
local sprite = app.open(source)
local layer = sprite.layers[5]
local image = Image(sprite.spec)
local labels = {
  { 'Кровавая луна', 149, { 54, 43, 49 } },
  { 'Сражение с волнами', 170, { 75, 62, 55 } },
  { 'Соло или с группой', 181, { 92, 75, 64 } },
  { 'Подробнее', 197, { 251, 237, 209 } }
}
for _, label in ipairs(labels) do
  local width = 0
  for _, cp in utf8.codes(label[1]) do width = width + (cp == 32 and 4 or glyphs[cp].w + 1) end
  local x = math.floor((sprite.width - width + 1) / 2)
  local color = app.pixelColor.rgba(label[3][1], label[3][2], label[3][3], 255)
  for _, cp in utf8.codes(label[1]) do
    if cp ~= 32 then
      local g = assert(glyphs[cp])
      for py = 0, g.h - 1 do
        for px = 0, g.w - 1 do
          if app.pixelColor.rgbaA(g.atlas:getPixel(g.x + px, g.y + py)) > 0 then image:drawPixel(x + px, label[2] + py + g.offset, color) end
        end
      end
    end
    x = x + (cp == 32 and 4 or glyphs[cp].w + 1)
  end
end
sprite:newCel(layer, sprite.frames[1], image, Point(0, 0))
layer.isVisible = true
sprite:saveAs(source)
sprite:saveCopyAs(output)
sprite:close()
