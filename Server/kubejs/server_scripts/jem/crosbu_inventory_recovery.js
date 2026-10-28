// One-time recovery for the complete GraveStone death record from 2026-10-28.
// The marker is written only after GraveStone reports a successful restore.
const CROSBU_RECOVERY_DEATH_ID = '73102475-c0f2-4530-a2c0-1b1288fe224b'
const CROSBU_RECOVERY_MARKER = 'jemCrosbuRecovery73102475'

PlayerEvents.loggedIn(event => {
  const player = event.player
  if (player.username !== 'Crosbu') return
  if (player.persistentData.getBoolean(CROSBU_RECOVERY_MARKER)) return

  const restored = event.server.runCommandSilent(
    `restore Crosbu ${CROSBU_RECOVERY_DEATH_ID} add`
  )

  if (restored !== 1) {
    console.error(`[JEM recovery] GraveStone restore failed for Crosbu: ${CROSBU_RECOVERY_DEATH_ID}`)
    return
  }

  player.persistentData.putBoolean(CROSBU_RECOVERY_MARKER, true)
  player.tell('Your lost items were restored. If your inventory was full, check the ground nearby.')
  console.info(`[JEM recovery] Restored Crosbu inventory from GraveStone death ${CROSBU_RECOVERY_DEATH_ID}`)
})
