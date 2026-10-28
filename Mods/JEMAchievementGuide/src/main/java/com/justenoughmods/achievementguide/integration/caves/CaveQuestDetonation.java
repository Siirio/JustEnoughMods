package com.justenoughmods.achievementguide.integration.caves;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
public final class CaveQuestDetonation {
    public record Run(ServerPlayer actor,GlobalPos target,List<Entity> primed){}
    private static final ThreadLocal<Run> CURRENT=new ThreadLocal<>();
    private static final ThreadLocal<Boolean> FIRING=new ThreadLocal<>();private CaveQuestDetonation(){}
    public static void begin(ServerPlayer actor,ItemStack stack){
        CURRENT.remove();var proof=CaveQuestStackProof.get(stack);var target=stack.hasTag()?com.github.alexmodguy.alexscaves.server.item.RemoteDetonatorItem.getBombPosition(stack.getTag()):null;
        if(actor!=null && target!=null && target.dimension().equals(actor.level().dimension()) && CaveQuestProof.permitted(actor,target.pos())
                && !proof.isEmpty() && proof.getString("kind").equals("detonator") && proof.getUUID("actor").equals(actor.getUUID()))CURRENT.set(new Run(actor,target,new ArrayList<>()));
    }
    public static void spawned(Entity entity,boolean accepted){var run=CURRENT.get();if(Boolean.TRUE.equals(FIRING.get()) && run!=null && accepted && run.primed().size()<8 && entity.level()==run.actor().level() && entity.blockPosition().equals(run.target().pos())
            && (entity instanceof net.minecraft.world.entity.item.PrimedTnt || entity instanceof com.github.alexmodguy.alexscaves.server.entity.item.NuclearBombEntity))run.primed().add(entity);}
    public static void fire(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.Level world,net.minecraft.core.BlockPos pos,net.minecraft.core.Direction side,net.minecraft.world.entity.LivingEntity actor){
        var run=CURRENT.get();boolean observed=run!=null && run.actor()==actor && world==actor.level() && run.target().pos().equals(pos);
        if(observed)FIRING.set(true);
        try{state.onCaughtFire(world,pos,side,actor);}catch(RuntimeException|Error error){CURRENT.remove();throw error;}finally{FIRING.remove();}
    }
    public static void end(){var run=CURRENT.get();CURRENT.remove();if(run==null || !run.actor().serverLevel().getBlockState(run.target().pos()).isAir())return;
        for(var entity:run.primed())if(!entity.isRemoved() && run.actor().serverLevel().getEntity(entity.getUUID())==entity){entity.getPersistentData().putUUID("jemCaveIgniterV4",run.actor().getUUID());CaveQuestProof.emit(run.actor(),"v3_cave_nuclear_controls");}}
}
