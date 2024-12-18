package implementslegend.mod.vaultfaster.mixin;

import iskallia.vault.core.vault.modifier.modifier.TemplateProcessorModifier;
import iskallia.vault.core.world.data.tile.TilePredicate;
import iskallia.vault.core.world.processor.tile.TileProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(TemplateProcessorModifier.Properties.class)
public interface TemplateProcessorModifierPropertiesAccessor {
    @Accessor(remap = false)
    TilePredicate getBlacklist();

    @Accessor(remap = false)
    List<TileProcessor> getFullBlock();

    @Accessor(remap = false)
    List<TileProcessor> getPartialBlock();

   @Accessor(remap = false) TilePredicate getWhitelist();
}
