local source = app.params.source
local output = app.params.output

if not source or not output then
  error("source and output parameters are required")
end

local sprite = app.open(source)
if not sprite then
  error("Unable to open background source: " .. source)
end

sprite:saveCopyAs(output)
sprite:close()
