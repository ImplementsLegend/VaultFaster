package implementslegend.mod.vaultfaster.mixin.accessors

import iskallia.vault.core.world.data.tile.TilePredicate
import iskallia.vault.core.world.processor.tile.TargetTileProcessor
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(TargetTileProcessor::class)
interface ProcessorPredicateAccessor {
    val predicate:TilePredicate @Accessor get
}