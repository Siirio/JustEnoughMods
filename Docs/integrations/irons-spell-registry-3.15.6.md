# Iron's Spells 3.15.6 integration

Source of truth is the installed `irons_spellbooks-1.20.1-3.15.6.jar`. Optional JEM classes load only when mod id `irons_spellbooks` is present. Damage uses the native `SpellDamageEvent`; JEM changes its amount and never creates a second damage event.

Damage order is Iron base level/power, JEM World Tier factor (`0.65/0.80/0.95/1.10/1.20`, PvP `1.00`), Human direct-hit `+1`, then Iron school resistance and ordinary target mitigation. Professions observe native `SpellOnCastEvent` once per caster/tick/spell/source audit key.

Registered spell paths, grouped by native school:

- blood: `acupuncture`, `blood_needles`, `blood_slash`, `blood_step`, `devour`, `heartstop`, `raise_dead`, `ray_of_siphoning`, `sacrifice`, `wither_skull`
- eldritch: `abyssal_shroud`, `eldritch_blast`, `planar_sight`, `pocket_dimension`, `sculk_tentacles`, `sonic_boom`, `telekinesis`
- ender: `black_hole`, `counterspell`, `dragon_breath`, `echoing_strikes`, `evasion`, `magic_arrow`, `magic_missile`, `portal`, `recall`, `shadow_slash`, `starfall`, `summon_ender_chest`, `summon_swords`, `teleport`
- evocation: `arrow_volley`, `chain_creeper`, `fang_strike`, `fang_ward`, `firecracker`, `gust`, `invisibility`, `lob_creeper`, `shield`, `slow`, `spectral_hammer`, `summon_horse`, `summon_vex`, `throw`, `wololo`
- fire: `blaze_storm`, `burning_dash`, `fire_arrow`, `fire_breath`, `fireball`, `firebolt`, `flaming_barrage`, `flaming_strike`, `heat_surge`, `magma_bomb`, `raise_hell`, `scorch`, `wall_of_fire`
- holy: `angel_wing`, `blessing_of_life`, `cleanse`, `cloud_of_regeneration`, `divine_smite`, `fortify`, `greater_heal`, `guiding_bolt`, `haste`, `heal`, `healing_circle`, `sunbeam`, `wisp`
- ice: `cone_of_cold`, `frost_step`, `frostbite`, `frostwave`, `ice_block`, `ice_spikes`, `ice_tomb`, `icicle`, `ray_of_frost`, `snowball`, `summon_polar_bear`
- lightning: `ball_lightning`, `chain_lightning`, `charge`, `electrocute`, `lightning_bolt`, `lightning_lance`, `shockwave`, `thunder_step`, `thunderstorm`, `volt_strike`
- nature: `acid_orb`, `blight`, `earthquake`, `firefly_swarm`, `gluttony`, `oakskin`, `poison_arrow`, `poison_breath`, `poison_splash`, `root`, `spider_aspect`, `stomp`, `touch_dig`

Every ID uses namespace `irons_spellbooks`. Runtime catalog lookup rejects unknown IDs instead of guessing. Utility, movement, healing and summons remain native; only an actual native damage event is tier-scaled.

