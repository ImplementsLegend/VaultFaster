package implementslegend.mod.vaultfaster.mixin.accessors

import iskallia.vault.core.world.data.tile.PartialBlockGroup
import iskallia.vault.core.world.data.tile.PartialBlockTag
import net.minecraft.resources.ResourceLocation
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(PartialBlockTag::class)
interface PredicateIdAccessor {
    val id:ResourceLocation @Accessor get
}