ServerEvents.recipes(event => {
  if (Platform.isLoaded('handcrafted') && Platform.isLoaded('bbb'))
    event.remove({ output: 'handcrafted:hammer' })
  event.smelting('minecraft:leather', 'minecraft:rotten_flesh').id('jem:leather_from_rotten_flesh')
  if (Platform.isLoaded('farm_and_charm')) event.remove({ id: 'farm_and_charm:feeding_trough' })
  if (Platform.isLoaded('tide')) {
  event.replaceInput({}, 'minecraft:fishing_rod', '#jem:valid_fishing_rods')
  event.remove({ id: 'minecraft:carrot_on_a_stick' })
  event.remove({ id: 'minecraft:warped_fungus_on_a_stick' })
  event.shapeless('minecraft:carrot_on_a_stick', ['#jem:valid_fishing_rods', 'minecraft:carrot']).id('minecraft:carrot_on_a_stick')
  event.shapeless('minecraft:warped_fungus_on_a_stick', ['#jem:valid_fishing_rods', 'minecraft:warped_fungus']).id('minecraft:warped_fungus_on_a_stick')
  }
  if (Platform.isLoaded('alexsmobs') && Platform.isLoaded('primal')) event.replaceInput(
    { output: 'alexsmobs:crocodile_chestplate' },
    'alexsmobs:crocodile_scute',
    'primal:crocodile_scute'
  )
})
