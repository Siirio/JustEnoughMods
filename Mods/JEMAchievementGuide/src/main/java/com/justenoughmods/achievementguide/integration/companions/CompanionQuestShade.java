package com.justenoughmods.achievementguide.integration.companions;
import dev.xylonity.companions.common.blockentity.AbstractShadeAltarBlockEntity;
import dev.xylonity.companions.common.entity.ShadeEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
public final class CompanionQuestShade {
 public static final String CHARGE="jemCompanionShadeChargeV4";private CompanionQuestShade(){}
 public static AbstractShadeAltarBlockEntity resolve(ServerPlayer actor,ItemStack bell){var tag=bell.getTag();if(tag==null || !actor.level().dimension().location().toString().equals(tag.getString("stored_dim")))return null;var pos=new net.minecraft.core.BlockPos(tag.getInt("stored_x"),tag.getInt("stored_y"),tag.getInt("stored_z"));return CompanionQuestProof.loaded(actor.serverLevel(),pos) && actor.level().getBlockEntity(pos) instanceof AbstractShadeAltarBlockEntity altar?altar:null;}
 public static boolean charge(AbstractShadeAltarBlockEntity altar,ServerPlayer actor,CompoundTag proof){return proof.hasUUID("actor") && proof.hasUUID("generation") && proof.getUUID("actor").equals(actor.getUUID()) && proof.getInt("charges")==altar.getCharges() && altar.getCharges()>0;}
 public static boolean matching(AbstractShadeAltarBlockEntity altar,ShadeEntity shade){return altar instanceof dev.xylonity.companions.common.blockentity.ShadeSwordAltarBlockEntity && shade instanceof dev.xylonity.companions.common.entity.companion.ShadeSwordEntity || altar instanceof dev.xylonity.companions.common.blockentity.ShadeBatAltarBlockEntity && shade instanceof dev.xylonity.companions.common.entity.companion.ShadeBatEntity || altar instanceof dev.xylonity.companions.common.blockentity.ShadeMawAltarBlockEntity && shade instanceof dev.xylonity.companions.common.entity.companion.ShadeMawEntity;}
}
