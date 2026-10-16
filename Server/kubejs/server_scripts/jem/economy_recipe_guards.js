// The shared-server economy must not change an integrated singleplayer game.
ServerEvents.recipes(event => {
  // Installed KubeJS exposes this physical-side check as a supported binding.
  // FMLEnvironment is deliberately denied by its Java class filter.
  if (Platform.isClientEnvironment()) return
  // One stone must not become two stones through the mincer and Create's fuel-free fan.
  // Keep the native mincer recipe/category; only remove its material duplication.
  event.remove({ id: 'farm_and_charm:mincer/cobblestone' })
  event.custom({
    type: 'farm_and_charm:mincer',
    ingredient: { item: 'minecraft:stone' },
    recipe_type: 'STONE',
    result: { item: 'minecraft:cobblestone', count: 1 }
  }).id('farm_and_charm:mincer/cobblestone')
  console.info('[JEM economy] Dedicated recipe guard registered: mincer stone -> cobblestone x1 (physicalClient=false)')
})
