ServerEvents.recipes(event => {
  const woods = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'bamboo', 'crimson', 'warped']
  const woodShapes = ['stairs', 'slab', 'fence', 'fence_gate', 'door', 'trapdoor', 'sign', 'button', 'pressure_plate']

  woods.forEach(wood => {
    const planks = `minecraft:${wood}_planks`
    const log = wood === 'bamboo'
      ? 'minecraft:bamboo_block'
      : wood === 'crimson' || wood === 'warped'
        ? `minecraft:${wood}_stem`
        : `minecraft:${wood}_log`
    event.stonecutting(`4x ${planks}`, log).id(`jem:woodcutting/${wood}_planks_from_log`)
    woodShapes.forEach(shape => {
      const count = shape === 'slab' ? 2 : 1
      const result = `minecraft:${wood}_${shape}`
      const output = count === 1 ? result : `${count}x ${result}`
      event.stonecutting(output, planks).id(`jem:woodcutting/${wood}_${shape}`)
    })
  })

  event.blasting('minecraft:stone', 'minecraft:cobblestone').id('jem:blasting/stone_from_cobblestone')
  event.blasting('minecraft:smooth_stone', 'minecraft:stone').id('jem:blasting/smooth_stone_from_stone')
  event.blasting('minecraft:brick', 'minecraft:clay_ball').id('jem:blasting/brick_from_clay_ball')
  event.blasting('minecraft:nether_brick', 'minecraft:netherrack').id('jem:blasting/nether_brick_from_netherrack')
  event.blasting('minecraft:deepslate', 'minecraft:cobbled_deepslate').id('jem:blasting/deepslate_from_cobbled_deepslate')
})
