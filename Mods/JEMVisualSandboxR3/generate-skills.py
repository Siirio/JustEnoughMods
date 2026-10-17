"""Build a standalone sandbox adapter from canonical JEMRoles signature methods.

Gameplay algorithms remain owned by JEMRoles, not a second hand-maintained copy.
Sandbox-only differences: four earned stages, build-selected development hooks,
new authored emission and explicit source-owned standalone ability overrides.
"""
from pathlib import Path
import hashlib
import json
import re
import sys

HERE = Path(__file__).resolve().parent
OWNER = HERE.parent / 'JEMRoles/src/main/java/com/siirio/jemroles/trait'
BUILD = Path(sys.argv[1]) if len(sys.argv) > 1 else HERE / 'build'
OUT = BUILD / 'generated/sandbox/com/siirio/jemvisualsandbox/workbench'
OUT.mkdir(parents=True, exist_ok=True)
source = (OWNER / 'RaceAbilities.java').read_text(encoding='utf-8')


def method(name):
    match = re.search(r'^    (?:private|public) static [^\n]+\b' + name + r'\(', source, re.M)
    if not match:
        raise ValueError('Canonical method missing: ' + name)
    opening = source.index('{', match.start())
    depth = 1
    end = opening + 1
    while depth:
        if source[end] == '{': depth += 1
        if source[end] == '}': depth -= 1
        end += 1
    return source[match.start():end]


methods = ['lifeChorus', 'riftAmbush', 'verdantDominion', 'prepareSculk', 'sculkVeins',
           'infernalEruption', 'root', 'erupt', 'echoVision', 'serverTick', 'recruit',
           'target', 'nearestSculk', 'sculk', 'samplePath', 'fail', 'teleport']
imports = '\n'.join(line for line in source.splitlines() if line.startswith('import ') and not line.startswith('import com.siirio.'))
body = '\n\n'.join(method(name) for name in methods)
# Native R6 heads strike at .18/.50/.82 seconds. The gameplay is synchronized,
# rather than starting a separate old particle telegraph for every fissure.
body = body.replace('int delay = 8 + i * 3;', 'int delay = new int[]{4, 10, 16}[i];')
packet_source = (OWNER.parent / 'network/RaceVfxPacket.java').read_text(encoding='utf-8')
path_limit = re.search(r'MAX_PATH_POINTS\s*=\s*(\d+)', packet_source).group(1)
body = body.replace('RaceVfxPacket.MAX_PATH_POINTS', path_limit).replace('RaceEnvironment.', 'SandboxEnvironment.')
body = body.replace('target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 4));', 'target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 4));\n        SandboxHostileControl.bind(player, target);')
body = body.replace('if (tier >= 2) spawnVoidEcho(player, departure);', 'SandboxHostileControl.refreshTargets(player);')
body = body.replace('private static void echoVision(', 'public static void echoVision(')
body = body.replace('if (tier >= 3) for (LivingEntity nearby : player.serverLevel().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4), e -> e != player && e != target))',
                    'if (RacePowerStages.riftSlowTicks(tier) > 0) for (LivingEntity nearby : player.serverLevel().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(RacePowerStages.riftSlowRadius(tier)), e -> e != player && e != target))')
body = body.replace('nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));',
                    'nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, RacePowerStages.riftSlowTicks(tier), 2));')
# Echo's scan and the utility preflight use the same canonical stage radius.
body = body.replace(method('echoVision').replace('private static void echoVision(', 'public static void echoVision('),
                    method('echoVision').replace('private static void echoVision(', 'public static void echoVision(').replace('inflate(32)', 'inflate(RacePowerStages.echoRadius(RaceExperience.tier(player)))'))
head = '''
/** Generated from canonical signature source. Never edit generated output. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="jem_visual_sandbox")
public final class SandboxSkills {
    private static final CopyOnWriteArrayList<Zone> ZONES = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Eruption> ERUPTIONS = new CopyOnWriteArrayList<>();
    private enum RaceVfxId { LIFE_CHORUS, RIFT_DEPART, RIFT_ARRIVE, VERDANT_DOMINION, SCULK_VEINS, INFERNAL_TELEGRAPH, INFERNAL_IMPACT, VOLCANIC_ZONE, ECHO_VISION }
    public static void cast(ServerPlayer player, int race) { RaceExperience.cast(player); }
    private static void emit(ServerPlayer player, RaceVfxId id, Vec3 origin, Vec3 target, int sourceId, int targetId, int[] related, List<Vec3> path, int tier, int duration) {
        switch (id) {
            case LIFE_CHORUS -> SandboxNetwork.effect(player,20,0,origin,0,player.getId());
            case ECHO_VISION -> SandboxNetwork.effect(player,24,3,origin,0,player.getId());
            case RIFT_DEPART, RIFT_ARRIVE, VERDANT_DOMINION -> { } // Rift timeline / per-entity vine binding own these visuals.
            case SCULK_VEINS -> {
                SandboxNetwork.path(player,1,SurfacePlacement.sculkPath(player,path));
            }
            default -> { } // Nether's new eruption timeline owns its presentation.
        }
    }
    public static void selectionChanged(ServerPlayer player) {
        ZONES.removeIf(zone -> zone.owner.equals(player.getUUID()));
        ERUPTIONS.removeIf(eruption -> eruption.owner.equals(player.getUUID()));
    }
    public static LivingEntity riftTarget(ServerPlayer player) { return target(player,RacePowerStages.riftRange(RaceExperience.tier(player))); }
    public static boolean commitRift(ServerPlayer player, UUID id) {
        var entity=player.serverLevel().getEntity(id);
        if (!(entity instanceof LivingEntity target) || !target.isAlive() || player.distanceToSqr(target)>Math.pow(RacePowerStages.riftRange(RaceExperience.tier(player)),2)) { fail(player,"target_required");return false; }
        Vec3 behind=target.position().subtract(target.getLookAngle().normalize().scale(1.8));
        BlockPos safe=SandboxEnvironment.nearestSafe(player,player.serverLevel(),BlockPos.containing(behind),3,true).orElse(null);
        if(safe==null) { fail(player,"no_safe_destination");return false; }
        return riftAmbush(player,RaceExperience.tier(player),target,safe);
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.isDedicatedServer()) return;
        serverTick(server);
        for (ServerLevel level : server.getAllLevels()) {
            for (ArmorStand echo : level.getEntities(EntityType.ARMOR_STAND, e -> e.getPersistentData().contains("JemVoidEchoExpiry")))
                if (level.getGameTime() >= echo.getPersistentData().getLong("JemVoidEchoExpiry")) echo.discard();
        }
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) { ZONES.clear(); ERUPTIONS.clear(); }
'''
records = source[source.index('    private enum ZoneType'):source.rfind('}')]
(OUT / 'SandboxSkills.java').write_text('package com.siirio.jemvisualsandbox.workbench;\n' + imports + '\n' + head + body + '\n' + records + '}\n', encoding='utf-8')
environment = (OWNER / 'RaceEnvironment.java').read_text(encoding='utf-8').replace('package com.siirio.jemroles.trait;', 'package com.siirio.jemvisualsandbox.workbench;').replace('RaceEnvironment', 'SandboxEnvironment')
(OUT / 'SandboxEnvironment.java').write_text('// Generated from JEMRoles canonical environment source.\n' + environment, encoding='utf-8')
receipt = {'owner':'Mods/JEMRoles/src/main/java/com/siirio/jemroles/trait', 'methods':methods, 'raceAbilitiesSha256':hashlib.sha256((OWNER/'RaceAbilities.java').read_bytes()).hexdigest(), 'sandboxOverrides':['server-owned stages Base/1/2/3 at 0/10/20/30 successful signature or utility uses','normal persisted cooldowns','build-selected SANDBOX integrated-owner cooldown/selection hooks; PRODUCTION sealed hooks','Void: native End portal tear and tick14 safe teleport; remove old decoy redirection and refresh real target','Bloom: base6 damage once; upgrades1=90tick action lock,2=3 damage/second,3=Poison once; chunky roots retained','Sculk: radial8block sonic wave12damage; stage1knockback,2darkness100ticks plus mobdisorientation,3temporary radius16domain200ticks with owner size/attack1.15 inside; stage-scaled utility','Nether V: independently resolved +/-10Y eruptions, continuous ground scar and fire-wall VFX','Nether V/B stage damage 3.4/5.1/6.8/8.5, fire 1/2/4/6 seconds; B retains three cuts, timing, pulls and collision']}
(BUILD / 'generated-skills.json').write_text(json.dumps(receipt, indent=2)+'\n')

path_source = (OWNER / 'TemporaryPathSavedData.java').read_text(encoding='utf-8').replace('package com.siirio.jemroles.trait;', 'package com.siirio.jemvisualsandbox.workbench;').replace('TemporaryPathSavedData','SandboxPathData').replace('jem_race_paths','jem_sandbox_paths')
(OUT / 'SandboxPathData.java').write_text(path_source,encoding='utf-8')

rules_source = (OWNER.parent / 'race/RaceRules.java').read_text(encoding='utf-8').replace('package com.siirio.jemroles.race;', 'package com.siirio.jemvisualsandbox.workbench;').replace('RaceRules', 'SandboxRaceRules')
(OUT / 'SandboxRaceRules.java').write_text('// Generated canonical default cooldown/config contract.\n' + rules_source, encoding='utf-8')