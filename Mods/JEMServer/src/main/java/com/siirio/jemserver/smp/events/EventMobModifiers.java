package com.siirio.jemserver.smp.events;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid="jem_server",value=net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class EventMobModifiers {
    private static final String ARCHETYPE="jem:event_archetype";
    private static final String ELITE="jem:event_elite";
    private static final String ENRAGED="jem:event_enraged";
    private static final String PENETRATION="jem:event_penetration";
    private static final String BLOOD_MOON_ATTACK_PRESSURE="jem:blood_moon_attack_pressure";
    private static final String SET_PIECE_BOSS="jem:setpiece_damage_boss";
    private static final String SET_PIECE_WAVE="jem:setpiece_damage_wave";
    private static final String SET_PIECE_SIGNATURE="jem:setpiece_signature";
    private static final String ELITE_TYPES="eliteTypes";
    private static final String SPECIAL_EQUIPPED="specialEquipped";
    private static final String FULL_EQUIPPED="fullEquipped";
    private static final int MAX_SPECIAL_EQUIPPED=3;
    private static final int MAX_FULL_EQUIPPED=2;
    private static final double SPECIAL_EQUIPMENT_CHANCE=.18;
    private static final double FULL_EQUIPMENT_CHANCE=.25;
    private static final double BOSS_SIGNATURE_DAMAGE_JUMP=1.30;
    private static final double BLOOD_MOON_DAMAGE_STRENGTH=1.15;
    private static final double BLOOD_MOON_ARMOR_PENETRATION=.05;
    private static final double WAVE_THREE_BOSS_DAMAGE_CAP=.30;
    private static final double WAVE_FIVE_BOSS_DAMAGE_CAP=.35;
    private static final double WAVE_THREE_BOSS_HEALTH=3.00;
    private static final double WAVE_FIVE_BOSS_HEALTH=3.00;
    private static final double WAVE_THREE_BOSS_DAMAGE=1.80;
    private static final double WAVE_FIVE_BOSS_DAMAGE=2.10;
    private static final double ARENA_FIVE_BOSS_DAMAGE_MULTIPLIER=1.40;
    private static final double STRONGEST_WAVE_TWO_HIT=13;
    private static final double STRONGEST_WAVE_FOUR_HIT=14;
    private static final List<String> ARCHETYPES=List.of("BRUTE","HUNTER","ARMORED","BREAKER","DEFLECTOR","DUELIST","THROWER");

    public static void resetWave(CompoundTag row) {
        row.put(ELITE_TYPES,new CompoundTag());
        row.putInt(SPECIAL_EQUIPPED,0);
        row.putInt(FULL_EQUIPPED,0);
    }

    public static void applyBloodMoon(Mob mob,CompoundTag row,int tier,int wave) {
        applyTier(mob,tier);
        penetration(mob,BLOOD_MOON_ARMOR_PENETRATION);
        mob.getPersistentData().putDouble(BLOOD_MOON_ATTACK_PRESSURE,BloodMoonWaves.attackPressure(tier));
        multiply(mob,Attributes.MAX_HEALTH,"wave_health",BloodMoonWaves.pressure(wave)*BloodMoonWaves.healthStrength(wave));
        multiply(mob,Attributes.ATTACK_DAMAGE,"wave_damage",BloodMoonWaves.damagePressure(wave));
        multiply(mob,Attributes.MOVEMENT_SPEED,"wave_speed",BloodMoonWaves.movementSpeed(wave));
        if(mob.getRandom().nextDouble()<BloodMoonWaves.archetypeChance(wave)) applyArchetype(mob,ARCHETYPES.get(mob.getRandom().nextInt(ARCHETYPES.size())),tier);
        if(mob.getRandom().nextDouble()<BloodMoonWaves.eliteChance(tier,wave)&&claimElite(row,mob)) applyElite(mob,wave);
        equip(mob,row,wave);
        mob.setHealth(mob.getMaxHealth());
    }

    public static void applyBloodMoonSetPiece(Mob mob,int tier,int wave,boolean boss) {
        penetration(mob,BLOOD_MOON_ARMOR_PENETRATION);
        if(boss) {
            mob.getPersistentData().putBoolean(SET_PIECE_BOSS,true);
            mob.getPersistentData().putInt(SET_PIECE_WAVE,wave);
            setPieceResistance(mob);
            if(wave==3) {
                multiply(mob,Attributes.MAX_HEALTH,"wave_three_boss_health",WAVE_THREE_BOSS_HEALTH);
            } else if(wave==BloodMoonWaves.WAVES) {
                multiply(mob,Attributes.MAX_HEALTH,"wave_five_boss_health",WAVE_FIVE_BOSS_HEALTH);
            }
        }
        mob.setHealth(mob.getMaxHealth());
    }

    public static boolean isSetPieceBoss(Entity entity) {
        return entity.getPersistentData().getBoolean(SET_PIECE_BOSS);
    }

    public static void applyMovementProfile(Mob mob,float scale,boolean boss) {
        double factor=boss?Math.max(.36,Math.min(.82,1/Math.sqrt(Math.max(1,scale)))):Math.max(.70,Math.min(1,1/Math.sqrt(Math.max(1,scale))));
        multiply(mob,Attributes.MOVEMENT_SPEED,"encounter_scale_speed",factor);
    }

    public static void applyBloodMoonSummon(Mob mob) {
        multiply(mob,Attributes.MAX_HEALTH,"summon_health",.75);
        multiply(mob,Attributes.ATTACK_DAMAGE,"summon_damage",.65);
        mob.setHealth(mob.getMaxHealth());
    }

    public static float signatureDamage(int tier,int wave,float nativeDamage) {
        int previous=wave==3?2:wave==BloodMoonWaves.WAVES?4:wave;
        float damage=(float)Math.max(nativeDamage,benchmark(previous)*tierDamage(tier)*BloodMoonWaves.damagePressure(previous)*BOSS_SIGNATURE_DAMAGE_JUMP);
        return (float)(damage*bossDamage(wave));
    }

    public static void signature(Mob source,Runnable attack) {
        source.getPersistentData().putBoolean(SET_PIECE_SIGNATURE,true);
        try { attack.run(); }
        finally { source.getPersistentData().remove(SET_PIECE_SIGNATURE); }
    }

    public static void signature(Entity projectile) {
        projectile.getPersistentData().putBoolean(SET_PIECE_SIGNATURE,true);
    }

    private static double benchmark(int wave) {
        return wave==2?STRONGEST_WAVE_TWO_HIT:STRONGEST_WAVE_FOUR_HIT;
    }

    private static double tierDamage(int tier) {
        double damage=1;
        if(tier>=2) damage*=1.10;
        if(tier>=4) damage*=1.12;
        if(tier>=5) damage*=1.12;
        return damage;
    }

    public static void applyRaidMob(Mob mob,int tier,int players) {
        multiply(mob,Attributes.MAX_HEALTH,"raid_health",1.50);
        multiply(mob,Attributes.ATTACK_DAMAGE,"raid_damage",1.50);
        mob.getPersistentData().putDouble(PENETRATION,.20);
        applyTier(mob,tier);
        if(mob.getRandom().nextDouble()<.35) applyArchetype(mob,ARCHETYPES.get(mob.getRandom().nextInt(ARCHETYPES.size())),tier);
        double playerHealth=players<=1?1:1+.20*(Math.min(5,players)-1);
        double playerDamage=players<=1?1:Math.min(1.45,1+.05*(Math.min(10,players)-1));
        multiply(mob,Attributes.MAX_HEALTH,"raid_players_health",playerHealth);
        multiply(mob,Attributes.ATTACK_DAMAGE,"raid_players_damage",playerDamage);
        mob.setHealth(mob.getMaxHealth());
    }

    private static void applyTier(Mob mob,int tier) {
        double health=1,damage=1,speed=1;
        if(tier>=2) {health*=1.10;damage*=1.10;}
        if(tier>=3) speed*=1.08;
        if(tier>=4) {health*=1.12;damage*=1.12;mob.getPersistentData().putDouble(PENETRATION,.05);}
        if(tier>=5) {health*=1.12;damage*=1.12;speed*=1.05;}
        multiply(mob,Attributes.MAX_HEALTH,"tier_health",health);
        multiply(mob,Attributes.ATTACK_DAMAGE,"tier_damage",damage);
        multiply(mob,Attributes.MOVEMENT_SPEED,"tier_speed",speed);
    }

    private static void applyArchetype(Mob mob,String archetype,int tier) {
        CompoundTag data=mob.getPersistentData();
        data.putString(ARCHETYPE,archetype);
        switch(archetype) {
            case "BRUTE" -> {multiply(mob,Attributes.MAX_HEALTH,"brute_health",1.30);multiply(mob,Attributes.KNOCKBACK_RESISTANCE,"brute_resistance",1.15);multiply(mob,Attributes.MOVEMENT_SPEED,"brute_speed",.92);}
            case "HUNTER" -> {multiply(mob,Attributes.MOVEMENT_SPEED,"hunter_speed",1.12);multiply(mob,Attributes.FOLLOW_RANGE,"hunter_range",1.15);multiply(mob,Attributes.MAX_HEALTH,"hunter_health",.90);}
            case "BREAKER" -> data.putDouble(PENETRATION,Math.min(.30,.15+(tier>=4?.05:0)));
            case "DUELIST" -> multiply(mob,Attributes.MOVEMENT_SPEED,"duelist_speed",1.08);
            default -> { }
        }
    }

    private static void applyElite(Mob mob,int wave) {
        mob.getPersistentData().putBoolean(ELITE,true);
        mob.setCustomName(Component.translatable("jem.smp.elite_name",mob.getType().getDescription()));
        mob.setCustomNameVisible(true);
        multiply(mob,Attributes.MAX_HEALTH,"elite_health",1.40);
        multiply(mob,Attributes.ATTACK_DAMAGE,"elite_damage",1.15);
        multiply(mob,Attributes.MOVEMENT_SPEED,"elite_speed",1.10);
        float scale=.15F+mob.getRandom().nextFloat()*.10F;
        if(wave==BloodMoonWaves.WAVES) scale=Math.max(scale,.20F);
        EventMobScale.apply(mob,1+scale);
    }

    private static boolean claimElite(CompoundTag row,Mob mob) {
        String type=BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        CompoundTag types=row.getCompound(ELITE_TYPES);
        if(types.getBoolean(type)) return false;
        types.putBoolean(type,true);
        row.put(ELITE_TYPES,types);
        return true;
    }

    private static void equip(Mob mob,CompoundTag row,int wave) {
        if(wave!=1&&wave!=2&&wave!=4||!(mob instanceof Zombie||mob instanceof AbstractSkeleton||mob instanceof Pillager||mob instanceof Vindicator)) return;
        boolean special=row.getInt(SPECIAL_EQUIPPED)<MAX_SPECIAL_EQUIPPED
                && (mob.getPersistentData().getString(ARCHETYPE).equals("ARMORED")||mob.getRandom().nextDouble()<SPECIAL_EQUIPMENT_CHANCE);
        boolean full=special&&row.getInt(FULL_EQUIPPED)<MAX_FULL_EQUIPPED&&mob.getRandom().nextDouble()<FULL_EQUIPMENT_CHANCE;
        int armorPieces=full?4:special?2+mob.getRandom().nextInt(2):mob.getRandom().nextInt(2);
        var armor=armor(wave);
        List<EquipmentSlot> slots=new java.util.ArrayList<>(List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET));
        java.util.Collections.shuffle(slots,new java.util.Random(mob.getRandom().nextLong()));
        for(int index=0;index<armorPieces;index++) set(mob,slots.get(index),enchanted(mob,new ItemStack(armor.get(slots.get(index))),wave));
        Item weapon=weapon(mob,wave);
        if(weapon!=Items.AIR) set(mob,EquipmentSlot.MAINHAND,enchanted(mob,new ItemStack(weapon),wave));
        if(special) row.putInt(SPECIAL_EQUIPPED,row.getInt(SPECIAL_EQUIPPED)+1);
        if(full) row.putInt(FULL_EQUIPPED,row.getInt(FULL_EQUIPPED)+1);
    }

    private static Map<EquipmentSlot,Item> armor(int wave) {
        if(wave==2) return Map.of(EquipmentSlot.HEAD,Items.IRON_HELMET,EquipmentSlot.CHEST,Items.IRON_CHESTPLATE,EquipmentSlot.LEGS,Items.IRON_LEGGINGS,EquipmentSlot.FEET,Items.IRON_BOOTS);
        if(wave==4) return Map.of(EquipmentSlot.HEAD,Items.DIAMOND_HELMET,EquipmentSlot.CHEST,Items.DIAMOND_CHESTPLATE,EquipmentSlot.LEGS,Items.DIAMOND_LEGGINGS,EquipmentSlot.FEET,Items.DIAMOND_BOOTS);
        return Map.of(EquipmentSlot.HEAD,Items.LEATHER_HELMET,EquipmentSlot.CHEST,Items.LEATHER_CHESTPLATE,EquipmentSlot.LEGS,Items.LEATHER_LEGGINGS,EquipmentSlot.FEET,Items.LEATHER_BOOTS);
    }

    private static Item weapon(Mob mob,int wave) {
        if(mob instanceof AbstractSkeleton) return Items.BOW;
        if(mob instanceof Pillager) return Items.CROSSBOW;
        boolean axe=mob instanceof Vindicator;
        if(wave==2) return axe?Items.IRON_AXE:Items.IRON_SWORD;
        if(wave==4) return axe?Items.DIAMOND_AXE:Items.DIAMOND_SWORD;
        return registry(axe?"minecraft:copper_axe":"minecraft:copper_sword",axe?Items.STONE_AXE:Items.STONE_SWORD);
    }

    private static Item registry(String id,Item fallback) {
        Item item=BuiltInRegistries.ITEM.get(new ResourceLocation(id));
        return item==Items.AIR?fallback:item;
    }

    private static ItemStack enchanted(Mob mob,ItemStack stack,int wave) {
        double chance=wave==1?.12:wave==2?.28:.50;
        if(mob.getRandom().nextDouble()>=chance||!(mob.level() instanceof ServerLevel level)) return stack;
        return EnchantmentHelper.enchantItem(level.random,stack,wave==1?8:wave==2?16:26,false);
    }

    private static void set(Mob mob,EquipmentSlot slot,ItemStack stack) {
        mob.setItemSlot(slot,stack);
        mob.setDropChance(slot,.085F);
    }

    @SubscribeEvent(priority=EventPriority.HIGH)
    public static void damage(LivingDamageEvent event) {
        var victim=event.getEntity();
        Entity direct=event.getSource().getDirectEntity();
        var source=EventSession.owner(event.getSource().getEntity());
        if(source==null) source=EventSession.owner(direct);
        if(source instanceof Mob mob&&mob.getPersistentData().hasUUID(EventSession.SESSION)) {
            boolean signature=mob.getPersistentData().getBoolean(SET_PIECE_SIGNATURE)
                    || direct!=null&&direct.getPersistentData().getBoolean(SET_PIECE_SIGNATURE);
            if(mob.getPersistentData().hasUUID(BloodMoon.EVENT_ID) && !mob.getPersistentData().getBoolean(SET_PIECE_BOSS)) {
                float amount=event.getAmount();
                if(!signature&&!event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) amount=(float)(amount*mob.getPersistentData().getDouble(BLOOD_MOON_ATTACK_PRESSURE));
                if(!signature) amount*=BLOOD_MOON_DAMAGE_STRENGTH;
                event.setAmount(amount);
            }
            double penetration=mob.getPersistentData().getDouble(PENETRATION);
            if(penetration>0&&!event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) event.setAmount((float)(event.getAmount()/(1-Math.min(.30,penetration))));
            if(victim instanceof net.minecraft.server.level.ServerPlayer player&&!event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR))
                event.setAmount(event.getAmount()*BossDebuffController.penetrationMultiplier(player));
            if(mob.getPersistentData().getBoolean(SET_PIECE_BOSS)) {
                int wave=mob.getPersistentData().getInt(SET_PIECE_WAVE);
                if(!signature) event.setAmount((float)(event.getAmount()*bossDamage(wave)));
                if(wave==BloodMoonWaves.WAVES) event.setAmount((float)(event.getAmount()*ARENA_FIVE_BOSS_DAMAGE_MULTIPLIER));
                double cap=wave==BloodMoonWaves.WAVES
                        ?WAVE_FIVE_BOSS_DAMAGE_CAP*ARENA_FIVE_BOSS_DAMAGE_MULTIPLIER
                        :WAVE_THREE_BOSS_DAMAGE_CAP;
                event.setAmount((float)Math.min(event.getAmount(),victim.getMaxHealth()*cap));
            }
        }
        if(victim instanceof Mob mob&&mob.getPersistentData().hasUUID(EventSession.SESSION)) {
            String archetype=mob.getPersistentData().getString(ARCHETYPE);
            if(archetype.equals("DEFLECTOR")&&event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) event.setAmount(event.getAmount()*.65F);
            if(archetype.equals("ARMORED")&&event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) event.setAmount(event.getAmount()*.90F);
            if(archetype.equals("DUELIST")&&!event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)&&event.getSource().getDirectEntity()!=null) event.setAmount(event.getAmount()*.80F);
        }
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if(!(event.getEntity() instanceof Mob mob)||mob.level().isClientSide||!mob.getPersistentData().hasUUID(EventSession.SESSION)) return;
        if(mob.getPersistentData().hasUUID(BloodMoon.EVENT_ID)&&mob.level().isDay()&&mob.level().canSeeSky(mob.blockPosition())) mob.clearFire();
        if(mob.tickCount%10!=0||mob.getPersistentData().getBoolean(ENRAGED)||mob.getHealth()>=mob.getMaxHealth()*.30F) return;
        mob.getPersistentData().putBoolean(ENRAGED,true);
        multiply(mob,Attributes.MOVEMENT_SPEED,"enraged_speed",1.15);
        multiply(mob,Attributes.ATTACK_DAMAGE,"enraged_damage",1.10);
    }

    private static void multiply(Mob mob,Attribute attribute,String name,double factor) {
        if(factor==1) return;
        var instance=mob.getAttribute(attribute);
        if(instance==null) return;
        UUID id=UUID.nameUUIDFromBytes(("jem:event:"+name).getBytes(StandardCharsets.UTF_8));
        instance.removeModifier(id);
        instance.addPermanentModifier(new AttributeModifier(id,"JEM event "+name.toLowerCase(Locale.ROOT),factor-1,AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void setPieceResistance(Mob mob) {
        var instance=mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if(instance==null) return;
        UUID id=UUID.nameUUIDFromBytes("jem:event:set_piece_knockback".getBytes(StandardCharsets.UTF_8));
        instance.removeModifier(id);
        instance.addPermanentModifier(new AttributeModifier(id,"JEM set-piece knockback resistance",Math.max(0,.92-instance.getBaseValue()),AttributeModifier.Operation.ADDITION));
    }

    private static double bossDamage(int wave) {
        if(wave==3) return WAVE_THREE_BOSS_DAMAGE;
        return wave==BloodMoonWaves.WAVES?WAVE_FIVE_BOSS_DAMAGE:1;
    }

    private static void penetration(Mob mob,double value) {
        var data=mob.getPersistentData();
        data.putDouble(PENETRATION,Math.max(value,data.getDouble(PENETRATION)));
    }

    private EventMobModifiers() {}
}
