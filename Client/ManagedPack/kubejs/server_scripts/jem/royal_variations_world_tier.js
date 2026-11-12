const JEM_ROYAL_VARIATIONS_TIER_API = Java.loadClass('com.siirio.jemworldbosstiers.api.WorldTierApi')
const JEM_ROYAL_VARIATIONS_MOBS = new Set([
  'royalvariations:royal_zombie',
  'royalvariations:royal_skeleton',
  'royalvariations:royal_creeper',
  'royalvariations:royal_enderman'
])

EntityEvents.spawned(event => {
  const entityId = String(event.entity.type)
  if (!JEM_ROYAL_VARIATIONS_MOBS.has(entityId)) return
  if (JEM_ROYAL_VARIATIONS_TIER_API.currentTier(event.server) >= 3) return

  event.entity.discard()
})
