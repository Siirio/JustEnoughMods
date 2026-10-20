-- Aseprite-only file-backed transport for the installed MCP operation engine.
-- Windows rejects its large JSON argument; original engine remains unmodified.
local function read(path)
  local file=assert(io.open(path,"rb")); local data=file:read("*a"); file:close(); return data
end
local enginePath="C:/Users/user/.codex/tools/aseprite/mcp/src/scripts/operations.lua"
local engine=read(enginePath)
local dispatch=assert(engine:find("\nlocal operation = app.params.operation",1,true))
local native=engine:sub(1,dispatch-1)..[[
local file=assert(io.open(app.params.requests,"rb"))
local requests=json.decode(file:read("*a"));file:close()
for _,request in ipairs(requests) do
  if request.name ~= "render_sprite" then
    local operation=assert(operations[request.name],request.name)
    local ok,err=pcall(operation,request.arguments)
    assert(ok,err)
  end
end
]]
assert(load(native,"@aseprite-native-file-backed-paint"))()
