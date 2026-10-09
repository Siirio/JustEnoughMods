ServerEvents.recipes(event => {
  [
    'endrem:old_eye',
    'endrem:black_eye',
    'endrem:magical_eye',
    'endrem:corrupted_eye',
    'endrem:cold_eye',
    'endrem:undead_eye',
    'endrem:nether_eye',
    'endrem:lost_eye',
    'endrem:guardian_eye',
    'endrem:exotic_eye',
    'endrem:cursed_eye',
    'endrem:wither_eye',
    'endrem:cryptic_eye',
    'endrem:evil_eye',
    'endrem:rogue_eye',
    'endrem:witch_eye'
  ].forEach(eye => event.remove({ output: eye }))
})
