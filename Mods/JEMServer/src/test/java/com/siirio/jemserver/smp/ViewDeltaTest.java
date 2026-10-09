package com.siirio.jemserver.smp;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ViewDeltaTest {
    @Test void nestedRosterChangesDoNotLoseUnchangedMembers(){
        var before=new CompoundTag();var members=new CompoundTag();members.putString("alice","ready");members.putString("bob","waiting");before.put("members",members);before.putString("tab","parties");
        var after=before.copy();after.getCompound("members").putString("bob","ready");after.getCompound("members").putString("carol","waiting");
        var patch=ViewDelta.between(before,after);assertFalse(patch.contains("tab"));assertFalse(patch.getCompound("members").contains("alice"));assertEquals(after,ViewDelta.apply(before,patch));assertEquals("waiting",before.getCompound("members").getString("bob"));
    }
    @Test void removalAndReturnAreAppliedAgainstLatestFullBaseline(){
        var before=new CompoundTag();before.putString("state","open");before.putString("location","private");var after=before.copy();after.remove("location");assertEquals(after,ViewDelta.apply(before,ViewDelta.between(before,after)));var restored=ViewDelta.apply(after,ViewDelta.between(after,before));assertEquals(before,restored);
    }
    @Test void unchangedViewHasOnlyDeltaMarker(){var view=new CompoundTag();view.putString("search","keep");var patch=ViewDelta.between(view,view);assertEquals(1,patch.size());assertEquals(view,ViewDelta.apply(view,patch));}
}
