ServerEvents.recipes(event => {
  const removedTransportItems = [
    'immersive_aircraft:airship',
    'immersive_aircraft:bamboo_hopper',
    'immersive_aircraft:bomb_bay',
    'immersive_aircraft:cargo_airship',
    'immersive_aircraft:eco_engine',
    'immersive_aircraft:enhanced_propeller',
    'immersive_aircraft:gyrodyne',
    'immersive_aircraft:gyroscope',
    'immersive_aircraft:gyroscope_dials',
    'immersive_aircraft:gyroscope_hud',
    'immersive_aircraft:heavy_crossbow',
    'immersive_aircraft:hull_reinforcement',
    'immersive_aircraft:improved_landing_gear',
    'immersive_aircraft:industrial_gears',
    'immersive_aircraft:nether_engine',
    'immersive_aircraft:quadrocopter',
    'immersive_aircraft:rotary_cannon',
    'immersive_aircraft:steel_boiler',
    'immersive_aircraft:sturdy_pipes',
    'immersive_aircraft:telescope',
    'immersive_aircraft:warship'
  ]
  removedTransportItems.forEach(item => event.remove({ output: item }))
  event.shaped('immersive_aircraft:sail', [
    'CCS',
    'CCS',
    'CCS'
  ], {
    C: 'minecraft:white_carpet',
    S: 'minecraft:string'
  }).id('immersive_aircraft:sail')
  event.smelting('minecraft:leather', 'minecraft:rotten_flesh').id('jem:leather_from_rotten_flesh')
  event.shapeless('4x jemcompat:aircraft_fuel', [
    'minecraft:coal_block',
    'minecraft:blaze_powder',
    'minecraft:honey_bottle'
  ]).id('jemcompat:aircraft_fuel')
  event.remove({ id: 'farm_and_charm:feeding_trough' })
  event.replaceInput({}, 'minecraft:fishing_rod', '#jem:valid_fishing_rods')
  event.remove({ id: 'minecraft:carrot_on_a_stick' })
  event.remove({ id: 'minecraft:warped_fungus_on_a_stick' })
  event.shapeless('minecraft:carrot_on_a_stick', ['#jem:valid_fishing_rods', 'minecraft:carrot']).id('minecraft:carrot_on_a_stick')
  event.shapeless('minecraft:warped_fungus_on_a_stick', ['#jem:valid_fishing_rods', 'minecraft:warped_fungus']).id('minecraft:warped_fungus_on_a_stick')
  event.replaceInput(
    { output: 'alexsmobs:crocodile_chestplate' },
    'alexsmobs:crocodile_scute',
    'primal:crocodile_scute'
  )
})
