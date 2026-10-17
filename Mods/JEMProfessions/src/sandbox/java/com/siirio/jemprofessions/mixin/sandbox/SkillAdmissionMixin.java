package com.siirio.jemprofessions.mixin.sandbox;

import com.siirio.jemprofessions.api.ProfessionApi;
import com.siirio.jemprofessions.sandbox.SandboxProfessionSupport;
import com.siirio.jemprofessions.sandbox.SandboxProfessionPolicy;
import com.siirio.jemprofessions.sandbox.SandboxSkillAdmission;
import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.tree.ProfessionCatalog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.SkillsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native graph clicks receive the same catalog admission as the JEM screen.
 * Privileged Skill.unlock used by owner-only free claims remains a separate explicit route. */
@Mixin(value=SkillsMod.class,remap=false)
public abstract class SkillAdmissionMixin {
    @Inject(method="tryUnlockSkill",at=@At("HEAD"),cancellable=true,remap=false)
    private void jem$admit(ServerPlayer player,ResourceLocation category,String nodeId,boolean force,CallbackInfo callback){
        if(!category.getNamespace().equals("jem_professions"))return;
        ProfessionId id=ProfessionId.parse(category.getPath()).orElse(null);
        var node=id==null?null:ProfessionCatalog.node(id,nodeId);
        if(!SandboxProfessionSupport.allowed(player)||node==null||!SandboxProfessionPolicy.available(nodeId)) {callback.cancel();return;}
        if(SandboxSkillAdmission.bypass(player,category,id,nodeId)) return;
        if(ProfessionApi.level(player,id)<node.level()) {
            SandboxProfessionSupport.message(player,"Нужен уровень профессии "+node.level()+".","Profession level "+node.level()+" is required.");callback.cancel();
        } else if(!node.parentId().isBlank()&&!ProfessionApi.enabled(player,id,node.parentId())) {
            SandboxProfessionSupport.message(player,"Сначала изучи предыдущий навык этой ветки.","Learn the preceding skill in this branch first.");callback.cancel();
        }
    }
}
