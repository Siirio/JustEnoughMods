package com.siirio.jemvisualsandbox.client.races;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.RenderGuiEvent;

/** One HUD: native Iron spell presentation when installed, compact own controls otherwise. */
public final class RaceAbilityHud {
    private static final String[] POWERS={"Life Chorus","Rift Ambush","Verdant Dominion","Sonic Wave","Infernal Eruption"};
    private RaceAbilityHud() {}
    public static void render(RenderGuiEvent.Post event){
        var c=Minecraft.getInstance();var s=RaceExperienceClient.status();
        if(c.player==null||c.screen!=null||c.options.hideGui||s==null||!s.selected())return;
        var g=event.getGuiGraphics();int x=10,y=c.getWindow().getGuiScaledHeight()-48;
        if(!OptionalRacePresentation.nativeHud()){
            var icon=switch(s.race()){case 0->Items.TOTEM_OF_UNDYING;case 1->Items.ENDER_PEARL;case 2->Items.FLOWERING_AZALEA;case 3->Items.ECHO_SHARD;default->Items.FIRE_CHARGE;};
            g.renderItem(new ItemStack(icon),x,y);
            long remaining=RaceExperienceClient.remaining(s.signatureRemaining());
            g.drawString(c.font,POWERS[s.race()]+" · "+(remaining>0?String.format(java.util.Locale.ROOT,"%.1fs",remaining/20.):"V"),x+21,y+3,remaining>0?0xb9b1a7:0xffedbd,false);
        }
        long utility=RaceExperienceClient.remaining(s.secondaryRemaining());
        String label=switch(s.race()){case 0->"Guardian";case 1->"Pearl / Shift+B gravity";case 2->"Leaf Bridge";case 3->"Echo / Resonance";default->"Fiery Slash";};
        g.drawString(c.font,"B · "+label+(utility>0?String.format(java.util.Locale.ROOT," · %.1fs",utility/20.):""),x,y+20,0xd1c9bb,false);
        if(s.developerMode())g.drawString(c.font,"DEV · normal progression preserved",x,y-13,0xffb26b,false);
    }
}
