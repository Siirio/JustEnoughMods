package com.siirio.jemguisandbox.mirror.jemeconomy.network;
import net.minecraft.resources.ResourceLocation;
public final class EconomyNetwork {
    public static void marketQuery(String search,String category,int page,boolean open) {
        com.siirio.jemguisandbox.Demo.marketQuery(search,category,page);
    }
    public static void marketAction(MarketActionPacket.Action action,ResourceLocation item,int count,long revision,String search,String category,int page) {
        com.siirio.jemguisandbox.Demo.marketAction(action,item,count,search,category,page);
    }
}
