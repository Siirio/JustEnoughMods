package com.siirio.jemprofessions.sandbox;

import java.util.List;
import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Explicit sandbox artifact plus exact optional native owner; ordinary dedicated servers apply none. */
public final class SandboxMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig(){return null;}
    @Override public boolean shouldApplyMixin(String target,String mixin){
        if(FMLEnvironment.dist!=Dist.CLIENT||FMLLoader.getLoadingModList().getModFileById("jem_professions_sandbox")==null)return false;
        String required=mixin.contains(".Epic")?"epicfight":mixin.contains(".Revive")?"playerrevive":mixin.contains(".IronCast")?"irons_spellbooks":null;
        return required==null||FMLLoader.getLoadingModList().getModFileById(required)!=null;
    }
    @Override public void acceptTargets(Set<String> mine,Set<String> others){}
    @Override public List<String> getMixins(){return null;}
    @Override public void preApply(String target,ClassNode node,String mixin,IMixinInfo info){}
    @Override public void postApply(String target,ClassNode node,String mixin,IMixinInfo info){}
}
