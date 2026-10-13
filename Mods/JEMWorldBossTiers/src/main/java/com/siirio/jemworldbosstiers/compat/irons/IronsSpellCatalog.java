package com.siirio.jemworldbosstiers.compat.irons;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class IronsSpellCatalog {
    private IronsSpellCatalog() {
    }

    public static Optional<ResourceLocation> registeredId(AbstractSpell spell) {
        ResourceLocation id = SpellRegistry.REGISTRY.get().getKey(spell);
        return id != null && "irons_spellbooks".equals(id.getNamespace()) ? Optional.of(id) : Optional.empty();
    }
}
