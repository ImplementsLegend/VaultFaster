package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.*;
import iskallia.vault.core.util.iterator.MappingIterator;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.core.world.data.tile.TilePredicate;
import iskallia.vault.core.world.processor.Processor;
import iskallia.vault.core.world.processor.ProcessorContext;
import iskallia.vault.core.world.processor.tile.*;
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

        if (tile == null || !filter.test(tile)) {
            return null;
        }
        //some blocks would not be processed correctly
        var dontUseTileMapper = TileMapperBlacklist.INSTANCE.isBlacklisted(((IndexedBlock)tile.getState().getBlock()).getRegistryIndex());

        if(dontUseTileMapper){
            for(Processor<PartialTile> processor : settings.getTileProcessors()) {

                if (tile == null || !filter.test(tile)) {
                    tile = null;
                    break;
                }
                processBlacklisted(processor,tile,settings.getProcessorContext());
            }
            return tile;
        } else {

            for (Processor<PartialTile> processor : ((ExtendedPlacementSettings) settings).getUnmappedProcessors()) {

                if (tile == null || !filter.test(tile)) {
                    tile = null;
                    break;
                }
                processNotTargetted(processor, tile, settings.getProcessorContext());
            }

            return ((TileMapperContainer) settings).getTileMapper().mapBlock(tile, settings.getProcessorContext());
        }
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

    /*for profiling*/
    private PartialTile processNotTargetted(Processor<PartialTile> processor, PartialTile tile, ProcessorContext processorContext) {
        return processor.process(tile, processorContext);
    }

    private PartialTile processBlacklisted(Processor<PartialTile> processor, PartialTile tile, ProcessorContext processorContext) {
        return processor.process(tile, processorContext);
    }
}
