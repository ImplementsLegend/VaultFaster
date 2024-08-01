package implementslegend.mod.vaultfaster.interfaces

import iskallia.vault.core.Version
import iskallia.vault.core.world.processor.Palette
import iskallia.vault.core.world.processor.ProcessorContext

interface CachedPaletteContainer {
    fun getCachedPalette(ctx:ProcessorContext):Palette
    fun getCachedPaletteForVersion(ver:Version):Palette

}