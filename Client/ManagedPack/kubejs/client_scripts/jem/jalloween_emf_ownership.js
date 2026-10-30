// priority: 10

const JEM_JALLOWEEN_RESOURCE = Java.loadClass('net.minecraft.resources.ResourceLocation')
const JEM_JALLOWEEN_MINECRAFT = Java.loadClass('net.minecraft.client.Minecraft')
const JEM_JALLOWEEN_EMF_API = Java.loadClass('traben.entity_model_features.EMFAnimationApi')
const JEM_JALLOWEEN_SYSTEM = Java.loadClass('java.lang.System')

const JEM_JALLOWEEN_VANILLA_MODEL_TYPES = {
  'minecraft:bee': true,
  'minecraft:chicken': true,
  'minecraft:evoker': true,
  'minecraft:pillager': true,
  'minecraft:slime': true,
  'minecraft:vindicator': true
}

const JEM_JALLOWEEN_MARKER = new JEM_JALLOWEEN_RESOURCE('jem_jalloween', 'enabled.marker')
let jemJalloweenLastResourceCheck = 0
let jemJalloweenActive = false

function isJemJalloweenActive() {
  const now = JEM_JALLOWEEN_SYSTEM.currentTimeMillis()
  if (now - jemJalloweenLastResourceCheck >= 1000) {
    const resourceManager = JEM_JALLOWEEN_MINECRAFT.getInstance().getResourceManager()
    jemJalloweenActive = resourceManager.getResource(JEM_JALLOWEEN_MARKER).isPresent()
    jemJalloweenLastResourceCheck = now
  }
  return jemJalloweenActive
}

JEM_JALLOWEEN_EMF_API.registerVanillaModelCondition(entity => {
  return isJemJalloweenActive()
    && JEM_JALLOWEEN_VANILLA_MODEL_TYPES[entity['emf$getTypeString']()] === true
})
