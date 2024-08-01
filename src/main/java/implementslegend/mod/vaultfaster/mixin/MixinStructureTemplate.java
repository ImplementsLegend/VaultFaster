package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.interfaces.ExtendedPlacementSettings;
import implementslegend.mod.vaultfaster.interfaces.IndexedBlock;
import implementslegend.mod.vaultfaster.interfaces.StreamedTemplate;
import implementslegend.mod.vaultfaster.interfaces.TileMapperContainer;
import iskallia.vault.core.util.iterator.MappingIterator;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.core.world.data.tile.TilePredicate;
import iskallia.vault.core.world.processor.Processor;
import iskallia.vault.core.world.processor.ProcessorContext;
import iskallia.vault.core.world.template.PlacementSettings;
import iskallia.vault.core.world.template.StructureTemplate;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/*
* applies tile mapper when processing all tiles except blacklisted
* */
@SuppressWarnings("ALL")
@Mixin(StructureTemplate.class)
public class MixinStructureTemplate implements StreamedTemplate {

    @Shadow private Map<TilePredicate, List<PartialTile>> tiles;

    private PartialTile tileMappingFunc(PartialTile tile, TilePredicate filter, PlacementSettings settings){
        tile = tile.copy();
        var regIndex = ((IndexedBlock) tile.getState().getBlock()).getRegistryIndex();
        if (tile == null || !filter.test(tile)) return null;
        for (Processor<PartialTile> processor : ((ExtendedPlacementSettings) settings).getUnmappedProcessors()) {

            if (tile == null || !filter.test(tile)) {
                tile = null;
                break;
            }
            ProcessorContext processorContext = settings.getProcessorContext();
            tile= processor.process(tile, processorContext);
        }

        tile=((TileMapperContainer) settings).getTileMapper().mapBlock(tile, settings.getProcessorContext());
        var ri = ((IndexedBlock) tile.getState().getBlock()).getRegistryIndex();
        return tile;
    }

    @NotNull
    @Override
    public Stream<PartialTile> getTileStream(@NotNull TilePredicate filter, @NotNull PlacementSettings settings) {
        return ((List<PartialTile>)this.tiles.get(filter)).parallelStream().map((tile) -> tileMappingFunc(tile,filter,settings));
    }

    @Overwrite(remap = false)
    public Iterator<PartialTile> getTiles(TilePredicate filter, PlacementSettings settings) {
        return new MappingIterator<PartialTile,PartialTile>(((List)this.tiles.get(filter)).iterator(), (PartialTile tile) -> tileMappingFunc(tile,filter,settings));
    }
}
