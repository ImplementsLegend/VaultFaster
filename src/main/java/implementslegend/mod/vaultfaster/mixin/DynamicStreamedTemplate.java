package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.DbgKt;
import implementslegend.mod.vaultfaster.StreamedTemplate;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.core.world.data.tile.TilePredicate;
import iskallia.vault.core.world.processor.Processor;
import iskallia.vault.core.world.template.DynamicTemplate;
import iskallia.vault.core.world.template.PlacementSettings;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Mixin(DynamicTemplate.class)
public class DynamicStreamedTemplate implements StreamedTemplate {


    @Shadow @Final private List<PartialTile> tiles;

    @NotNull
    @Override
    public Stream<PartialTile> getTileStream(@NotNull TilePredicate filter, @NotNull PlacementSettings settings) {
        return tiles.parallelStream().map(tile -> {
            if (!filter.test(tile)) {
                return null;
            } else {
                tile = tile.copy();

                for (Processor<PartialTile> processor : settings.getTileProcessors()) {
                    if (tile == null || !filter.test(tile)) {
                        tile = null;
                        break;
                    }

                    tile = processor.process(tile, settings.getProcessorContext());
                }

                DbgKt.breakpoint();
                return tile;
            }
        });
    }
}
