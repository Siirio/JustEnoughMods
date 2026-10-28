package com.justenoughmods.achievementguide.integration.caves;
import java.util.ArrayDeque;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
public final class CaveQuestMenuDebit {
    public record Frame(ServerPlayer actor,ItemStack[] items,ItemStack carried,CaveQuestStation.Click click){}
    private static final class Calls { final ArrayDeque<Frame> frames=new ArrayDeque<>(); int overflow; }
    private static final ThreadLocal<Calls> CALLS=ThreadLocal.withInitial(Calls::new);
    private CaveQuestMenuDebit(){}
    public static void begin(AbstractContainerMenu menu,ServerPlayer actor){var calls=CALLS.get();if(calls.overflow>0 || calls.frames.size()>=32){calls.overflow++;return;}ItemStack[] items=new ItemStack[actor.getInventory().getContainerSize()];for(int i=0;i<items.length;i++)items[i]=actor.getInventory().getItem(i).copy();calls.frames.push(new Frame(actor,items,menu.getCarried().copy(),CaveQuestStation.capture(menu,actor)));}
    public static int before(ServerPlayer actor,ItemStack kind){var calls=CALLS.get();var frame=calls.frames.peek();if(calls.overflow>0 || frame==null || frame.actor()!=actor)return -1;int n=ItemStack.isSameItemSameTags(frame.carried(),kind)?frame.carried().getCount():0;for(var item:frame.items())if(ItemStack.isSameItemSameTags(item,kind))n+=item.getCount();return n;}
    public static void observe(AbstractContainerMenu menu,ServerPlayer actor,Runnable nativeCall){begin(menu,actor);boolean success=false;try{nativeCall.run();success=true;}finally{finish(success);}}
    private static void finish(boolean success){var calls=CALLS.get();if(calls.overflow>0){calls.overflow--;return;}if(calls.frames.isEmpty())return;try{if(success)CaveQuestStation.finish(calls.frames.peek().click());}finally{calls.frames.pop();if(calls.frames.isEmpty())CALLS.remove();}}
}
