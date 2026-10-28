package com.justenoughmods.achievementguide.integration.worldritual;
import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.mcreator.luminousnether.entity.*;
public final class WorldRitualQuestEctoplasm {
    public static final String SOURCE="world_ritual_guzzle_ectoplasm";
    private static final String PROJECTILE="jem_quest_guzzle_paid";
    private static final ThreadLocal<Calls> CALLS=ThreadLocal.withInitial(Calls::new);
    private static final class Calls {final ArrayDeque<Receipt> frames=new ArrayDeque<>();int overflow;}
    private static final class Receipt {
        final Entity actor;final String kind;final Entity subject;final ItemStack food;final int count;final BlockPos pos;final BlockState state;
        final List<ItemEntity> drops=new ArrayList<>();ItemStack ammo=ItemStack.EMPTY;ItemStack ammoSnapshot=ItemStack.EMPTY;int ammoCount;EctoplasmShotEntity shot;FallingBlockEntity falling;
        Receipt(Entity actor,String kind,Entity subject,ItemStack food,BlockPos pos){this.actor=actor;this.kind=kind;this.subject=subject;this.food=food;count=food.getCount();this.pos=pos;state=pos!=null?subject.level().getBlockState(pos):null;}
    }
    private WorldRitualQuestEctoplasm(){}
    private static void push(Receipt receipt){Calls calls=CALLS.get();if(calls.overflow>0||calls.frames.size()>=16){calls.overflow++;return;}calls.frames.push(receipt);}
    private static Receipt current(){Calls calls=CALLS.get();return calls.overflow==0&&!calls.frames.isEmpty()?calls.frames.peek():null;}
    public static void feed(Entity creature,Entity actor){push(new Receipt(actor,"feed",creature,actor instanceof LivingEntity living?living.getMainHandItem():ItemStack.EMPTY,null));}
    public static void throwBegin(LivingEntity actor){push(new Receipt(actor,"throw",actor,ItemStack.EMPTY,null));}
    public static void ammo(ItemStack actual){Receipt r=current();if(r!=null&&r.kind.equals("throw")){r.ammo=actual;r.ammoSnapshot=actual.copy();r.ammoCount=actual.getCount();}}
    public static void hit(EctoplasmShotEntity projectile,BlockHitResult result){push(new Receipt(projectile.getOwner(),"hit",projectile,ItemStack.EMPTY,result.getBlockPos().immutable()));}
    public static void spawned(Entity entity,boolean accepted){
        Receipt r=current();if(r==null||!accepted||!WorldRitualQuestProof.real(r.actor)||entity.level()!=r.actor.level()||((ServerPlayer)r.actor).serverLevel().getEntity(entity.getUUID())!=entity)return;
        if(r.kind.equals("feed")&&entity instanceof ItemEntity drop&&WorldRitualQuestProof.item(drop.getItem()).equals("luminous_nether:ectoplasm_ball")&&r.drops.size()<16)r.drops.add(drop);
        if(r.kind.equals("throw")&&entity instanceof EctoplasmShotEntity shot&&shot.getOwner()==r.actor)r.shot=shot;
        if(r.kind.equals("hit")&&entity instanceof FallingBlockEntity falling&&r.pos.equals(falling.blockPosition())&&falling.getBlockState().equals(r.state))r.falling=falling;
    }
    public static void end(){
        Calls calls=CALLS.get();if(calls.overflow>0){calls.overflow--;return;}Receipt r=calls.frames.isEmpty()?null:calls.frames.pop();if(calls.frames.isEmpty())CALLS.remove();if(r==null||!WorldRitualQuestProof.real(r.actor))return;
        ServerPlayer actor=(ServerPlayer)r.actor;
        if(r.kind.equals("feed")&&r.subject instanceof GuzzleghastEntity ghast&&WorldRitualQuestProof.natural(ghast)&&r.count-r.food.getCount()==1&&!ghast.getEntityData().get(GuzzleghastEntity.DATA_IsHungry)){
            for(ItemEntity drop:r.drops)if(!drop.isRemoved()&&actor.serverLevel().getEntity(drop.getUUID())==drop)NativeFoodSources.get(actor.serverLevel()).certify(actor.serverLevel(),actor.getUUID(),SOURCE,drop.getItem(),drop.getItem().getCount());
        }
        if(r.kind.equals("throw")&&r.shot!=null&&!r.shot.isRemoved()&&r.ammoCount-r.ammo.getCount()==1&&WorldRitualQuestProof.item(r.ammoSnapshot).equals("luminous_nether:ectoplasm_ball")
            &&NativeFoodSources.get(actor.serverLevel()).consume(actor.getUUID(),r.ammoSnapshot,SOURCE,1))r.shot.getPersistentData().putUUID(PROJECTILE,actor.getUUID());
        if(r.kind.equals("hit")&&r.subject.getPersistentData().hasUUID(PROJECTILE)&&r.subject.getPersistentData().getUUID(PROJECTILE).equals(actor.getUUID())
            &&r.falling!=null&&!r.falling.isRemoved()&&!r.state.isAir()&&actor.level().getBlockState(r.pos).isAir())WorldRitualQuestProof.emit(actor,"v3_combat_guzzle_ectoplasm");
    }
    public static void clear(){CALLS.remove();}
}
