package implementslegend.mod.vaultfaster.mixin.template;

import implementslegend.mod.vaultfaster.interfaces.StreamedTemplate;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.core.world.data.tile.TilePredicate;
import iskallia.vault.core.world.template.EmptyTemplate;
import iskallia.vault.core.world.template.PlacementSettings;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

import java.util.stream.Stream;

@Mixin(EmptyTemplate.class)
public class EmptyStreamedTemplate implements StreamedTemplate {
    @NotNull
    @Override
    public Stream<PartialTile> getTileStream(@NotNull TilePredicate filter, @NotNull PlacementSettings settings) {
        return Stream.empty();
    }
}
