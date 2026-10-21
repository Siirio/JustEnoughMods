package com.siirio.jemsettlements.mixin.xaero;

import java.util.ArrayList;
import com.siirio.jemsettlements.compat.xaero.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

@Pseudo
@Mixin(targets="xaero.map.gui.dropdown.rightclick.GuiRightClickMenu",remap=false)
public abstract class SettlementRightClickMenuMixin implements SettlementMenuAccess {
    @Shadow private ArrayList<RightClickOption> actionOptions;
    @Override public SettlementExpansionOption jem$expansionOption() {
        return actionOptions!=null && actionOptions.size()==1 && actionOptions.get(0) instanceof SettlementExpansionOption option ? option : null;
    }
}
