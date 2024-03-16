package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.IndexedBlock;
import implementslegend.mod.vaultfaster.TileMapperContainer;
import iskallia.vault.core.util.iterator.MappingIterator;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.core.world.data.tile.TilePredicate;
import iskallia.vault.core.world.processor.Processor;
import iskallia.vault.core.world.processor.ProcessorContext;
import iskallia.vault.core.world.processor.tile.*;
import iskallia.vault.core.world.template.PlacementSettings;
import iskallia.vault.core.world.template.StructureTemplate;
import iskallia.vault.init.ModBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
/*
* applies tile mapper when processing all tiles except blacklisted
* */
@SuppressWarnings("ALL")
@Mixin(StructureTemplate.class)
public class MixinStructureTemplate {


    @Shadow private Map<TilePredicate, List<PartialTile>> tiles;

    private int spawnerId = -1;

    private int getSpawnerId(){
        if(spawnerId<0){
            spawnerId = ((IndexedBlock)ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryParse("ispawner:spawner"))).getRegistryIndex();
        }
        return spawnerId;
    }

    @Overwrite(remap = false)
    public Iterator<PartialTile> getTiles(TilePredicate filter, PlacementSettings settings) {

        return new MappingIterator<PartialTile,PartialTile>(((List)this.tiles.get(filter)).iterator(), (PartialTile tile) -> {
            tile = tile.copy();

            if (tile == null || !filter.test(tile)) {
                tile = null;
                return tile;
            }
            var tileID = ((IndexedBlock)tile.getState().getBlock()).getRegistryIndex();
            //some blocks would not be processed correctly
            var dontUseTileMapper = tileID==getSpawnerId()/*order would be messed up and mobs would not spawn*/ ||
                    tileID == ((IndexedBlock)ModBlocks.GOD_ALTAR).getRegistryIndex() /*you wold get template challange*/ ||
                    tileID==((IndexedBlock) Blocks.WHITE_CONCRETE).getRegistryIndex() || //some room-specific stuff seems to be broken
                    tileID==((IndexedBlock) Blocks.LIME_GLAZED_TERRACOTTA).getRegistryIndex() ||
                    tileID==((IndexedBlock) Blocks.GRAY_GLAZED_TERRACOTTA).getRegistryIndex() ||
                    tileID==((IndexedBlock) Blocks.COMMAND_BLOCK).getRegistryIndex();

            for(Processor<PartialTile> processor : settings.getTileProcessors()) {

                if (tile == null || !filter.test(tile)) {
                    tile = null;
                    break;
                }
                if((processor instanceof TargetTileProcessor<?> ||
                        processor instanceof VaultLootTileProcessor ||
                        processor instanceof ReferenceTileProcessor ||
                        processor instanceof LeveledTileProcessor) && !dontUseTileMapper
                ); else
                    processNotTargetted(processor,tile,settings.getProcessorContext());
            }

            if(!dontUseTileMapper)tile=((TileMapperContainer)settings).getTileMapper().mapBlock(tile,settings.getProcessorContext());
            /*
            for(Processor<PartialTile> processor : settings.getTileProcessors()) {

                if (tile == null || !filter.test(tile)) {
                    tile = null;
                    break;
                }
                tile=(processor instanceof TargetTileProcessor)? processTargeted(processor,tile,settings.getProcessorContext()):processNotTargetted(processor,tile,settings.getProcessorContext());
            }*/

            return tile;
        });
    }

    /*for profiling*/
    private PartialTile processNotTargetted(Processor<PartialTile> processor, PartialTile tile, ProcessorContext processorContext) {
        return processor.process(tile, processorContext);
    }

    private PartialTile processTargeted(Processor<PartialTile> processor, PartialTile tile, ProcessorContext processorContext) {
        return processor.process(tile, processorContext);
    }
}
