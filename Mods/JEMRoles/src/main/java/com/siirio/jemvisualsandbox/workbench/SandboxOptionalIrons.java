package com.siirio.jemvisualsandbox.workbench;

import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

/** No optional Iron's class appears in this common loader's type signatures. */
public final class SandboxOptionalIrons {
    private static Method sync;
    private SandboxOptionalIrons() {}
    public static boolean available(){
        return ModList.get().getModContainerById("irons_spellbooks")
                .map(c->c.getModInfo().getVersion().toString())
                .map(v->v.equals("1.20.1-3.15.6")||v.equals("3.15.6")).orElse(false);
    }
    public static void register(IEventBus bus){
        if(!available())return;
        try {
            Class<?> bridge=Class.forName("com.siirio.jemvisualsandbox.compat.irons.SandboxIronsBridge");
            bridge.getMethod("register",IEventBus.class).invoke(null,bus);sync=bridge.getMethod("sync",ServerPlayer.class);
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Iron's Spells adapter could not initialize",e);}
    }
    public static void sync(ServerPlayer p){if(sync!=null)try{sync.invoke(null,p);}catch(ReflectiveOperationException e){throw new IllegalStateException("Iron's Spells cooldown projection failed",e);}}
}
