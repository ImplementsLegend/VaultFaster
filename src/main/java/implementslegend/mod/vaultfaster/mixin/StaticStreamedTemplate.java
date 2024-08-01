package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.interfaces.StreamedTemplate;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.core.world.data.tile.TilePredicate;
import iskallia.vault.core.world.template.PlacementSettings;
import iskallia.vault.core.world.template.StaticTemplate;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Mixin(StaticTemplate.class)
public class StaticStreamedTemplate implements StreamedTemplate {
    @Shadow @Final private Iterable<PartialTile> tiles;

    @NotNull
    @Override
    public Stream<PartialTile> getTileStream(@NotNull TilePredicate filter, @NotNull PlacementSettings settings) {
        if(this.tiles instanceof Collection<PartialTile> c){
            return c.parallelStream();
        }
        return StreamSupport.stream(tiles.spliterator(), true);
    }
}
