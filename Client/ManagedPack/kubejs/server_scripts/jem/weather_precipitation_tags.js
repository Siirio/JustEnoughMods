ServerEvents.tags('worldgen/biome', event => {
  if (!Platform.isLoaded('sereneseasons')) return

  event.remove('sereneseasons:tropical_biomes', [
    'minecraft:desert',
    'minecraft:badlands',
    'minecraft:eroded_badlands',
    'minecraft:wooded_badlands'
  ])
})
