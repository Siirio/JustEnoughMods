package com.siirio.jemluxium.client;

import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class LuxiumOptionStorage implements OptionStorage<LuxiumOptionStorage.State> {
    private final State state = new State(LuxiumShaderController.isLuxiumActive());

    @Override
    public State getData() {
        return state;
    }

    @Override
    public void save() {
        LuxiumShaderController.setLuxiumEnabled(state.enabled);
    }

    static final class State {
        boolean enabled;

        private State(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
