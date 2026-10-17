"""Build a standalone sandbox adapter from canonical JEMRoles signature methods.

Gameplay algorithms remain owned by JEMRoles, not a second hand-maintained copy.
Sandbox-only differences: tier 3, unlimited selection, no cooldown/account gates,
new authored emission, and the explicit Nether fiery-slash replacement.
"""
from pathlib import Path
import hashlib
import json
import re

HERE = Path(__file__).resolve().parent
OWNER = HERE.parent / 'JEMRoles/src/main/java/com/siirio/jemroles/trait'
OUT = HERE / 'build/generated/sandbox/com/siirio/jemvisualsandbox/workbench'
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
           'infernalEruption', 'root', 'erupt', 'spawnVoidEcho', 'serverTick', 'recruit',
           'target', 'nearestSculk', 'sculk', 'samplePath', 'fail', 'teleport']
imports = '\n'.join(line for line in source.splitlines() if line.startswith('import ') and not line.startswith('import com.siirio.'))
body = '\n\n'.join(method(name) for name in methods)
# Native R6 heads strike at .18/.50/.82 seconds. The gameplay is synchronized,
# rather than starting a separate old particle telegraph for every fissure.
body = body.replace('int delay = 8 + i * 3;', 'int delay = new int[]{4, 10, 16}[i];')
packet_source = (OWNER.parent / 'network/RaceVfxPacket.java').read_text(encoding='utf-8')
path_limit = re.search(r'MAX_PATH_POINTS\s*=\s*(\d+)', packet_source).group(1)
body = body.replace('RaceVfxPacket.MAX_PATH_POINTS', path_limit).replace('RaceEnvironment.', 'SandboxEnvironment.')
body = body.replace('target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 4));', 'target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 4));\n        SandboxNetwork.vine(player, target);')
head = '''
/** Generated from canonical signature source. Never edit generated output. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="jem_visual_sandbox")
public final class SandboxSkills {
    private static final CopyOnWriteArrayList<Zone> ZONES = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Eruption> ERUPTIONS = new CopyOnWriteArrayList<>();
    private enum RaceVfxId { LIFE_CHORUS, RIFT_DEPART, RIFT_ARRIVE, VERDANT_DOMINION, SCULK_VEINS, INFERNAL_TELEGRAPH, INFERNAL_IMPACT, VOLCANIC_ZONE }
    public static void cast(ServerPlayer player, int race) {
        if (player.server.isDedicatedServer() || !player.server.isSingleplayerOwner(player.getGameProfile()) || !player.isAlive() || player.isSpectator()) return;
        int tier = 3;
        switch (race) {
            case 0 -> lifeChorus(player, tier);
            case 1 -> {
                ElementalAbilities.rift(player);
            }
            case 2 -> { if (!verdantDominion(player, tier)) fail(player, "no_nearby_targets"); }
            case 3 -> { SignaturePlan plan = prepareSculk(player, tier); if (plan != null) sculkVeins(player, tier, plan); }
            case 4 -> {
                ElementalAbilities.eruption(player);
            }
            default -> throw new IllegalArgumentException("Unknown sandbox race");
        }
    }
    private static void emit(ServerPlayer player, RaceVfxId id, Vec3 origin, Vec3 target, int sourceId, int targetId, int[] related, List<Vec3> path, int tier, int duration) {
        switch (id) {
            case LIFE_CHORUS -> SandboxNetwork.visual(player, 0, origin, -player.getYRot());
            case RIFT_DEPART, RIFT_ARRIVE, VERDANT_DOMINION -> { } // Rift timeline / per-entity vine binding own these visuals.
            case SCULK_VEINS -> {
                int limit = Math.min(8, path.size());
                for (int i = 0; i < limit; i++) SandboxNetwork.effect(player,7,3,path.get(i * (path.size()-1) / Math.max(1,limit-1)).add(0,.02,0),player.getYRot(),-1);
            }
            default -> { } // Nether's new eruption timeline owns its presentation.
        }
    }
    public static LivingEntity riftTarget(ServerPlayer player) { return target(player,32); }
    public static boolean commitRift(ServerPlayer player, UUID id) {
        var entity=player.serverLevel().getEntity(id);
        if (!(entity instanceof LivingEntity target) || !target.isAlive() || player.distanceToSqr(target)>32*32) { fail(player,"target_required");return false; }
        Vec3 behind=target.position().subtract(target.getLookAngle().normalize().scale(1.8));
        BlockPos safe=SandboxEnvironment.nearestSafe(player,player.serverLevel(),BlockPos.containing(behind),3,true).orElse(null);
        if(safe==null) { fail(player,"no_safe_destination");return false; }
        return riftAmbush(player,3,target,safe);
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
receipt = {'owner':'Mods/JEMRoles/src/main/java/com/siirio/jemroles/trait', 'methods':methods, 'raceAbilitiesSha256':hashlib.sha256((OWNER/'RaceAbilities.java').read_bytes()).hexdigest(), 'sandboxOverrides':['tier 3','unlimited selection','no cooldown','Void: native End portal tear, revalidated teleport at tick14','Bloom: per-target adaptive brown Blockbench roots and pointed green leaves','Sculk: authored pixel VFX on canonical path','Nether V: five progressing eruptions using three authored models','Nether B (Russian И): approved FierySlash with unchanged assets, timing and pulls']}
(HERE / 'build/generated-skills.json').write_text(json.dumps(receipt, indent=2)+'\n')
