package implementslegend.mod.vaultfaster

import iskallia.vault.core.world.processor.Palette
import iskallia.vault.core.world.processor.ProcessorContext
import org.spongepowered.asm.mixin.gen.Accessor

interface CachedPaletteContainer {
    fun getCachedPalette(ctx:ProcessorContext):Palette

}