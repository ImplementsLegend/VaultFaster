package implementslegend.mod.vaultfaster.mixin

import iskallia.vault.config.TileGroupsConfig
import iskallia.vault.core.world.data.tile.TilePredicate
import net.minecraft.resources.ResourceLocation
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(TileGroupsConfig::class)
interface TileGroupsAccessor {
    val groups:Map<ResourceLocation,Set<TilePredicate>> @Accessor get
}