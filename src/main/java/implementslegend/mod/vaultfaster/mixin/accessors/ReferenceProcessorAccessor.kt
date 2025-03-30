package implementslegend.mod.vaultfaster.mixin.accessors

import iskallia.vault.core.util.WeightedList
import iskallia.vault.core.world.processor.tile.ReferenceTileProcessor
import net.minecraft.resources.ResourceLocation
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(ReferenceTileProcessor::class)
interface ReferenceProcessorAccessor {
    val pool:WeightedList<ResourceLocation> @Accessor get
}
