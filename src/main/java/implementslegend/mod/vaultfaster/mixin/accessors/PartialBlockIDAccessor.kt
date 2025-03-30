package implementslegend.mod.vaultfaster.mixin.accessors

import iskallia.vault.core.world.data.tile.PartialBlock
import net.minecraft.resources.ResourceLocation
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(PartialBlock::class)
interface PartialBlockIDAccessor {
    val id: ResourceLocation @Accessor get
}
