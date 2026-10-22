ServerEvents.recipes(event => {
  if (!Platform.isLoaded('jem_economy')) return
  // Record this recipe manager before returning, including the pre-world load.
  const economyEnabled = Java.loadClass('com.siirio.jemeconomy.api.EconomyApi').recipePolicyEnabled()
  if (!economyEnabled || !Platform.isLoaded('farm_and_charm')) return
  // One stone must not become two stones through the mincer and Create's fuel-free fan.
  // Keep the native mincer recipe/category; only remove its material duplication.
  event.remove({ id: 'farm_and_charm:mincer/cobblestone' })
  event.custom({
    type: 'farm_and_charm:mincer',
    ingredient: { item: 'minecraft:stone' },
    recipe_type: 'STONE',
    result: { item: 'minecraft:cobblestone', count: 1 }
  }).id('farm_and_charm:mincer/cobblestone')
  console.info('[JEM economy] Active world recipe guard: mincer stone -> cobblestone x1')
})
