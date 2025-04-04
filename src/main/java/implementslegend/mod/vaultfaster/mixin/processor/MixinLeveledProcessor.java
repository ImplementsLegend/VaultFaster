package implementslegend.mod.vaultfaster.mixin.processor;

import implementslegend.mod.vaultfaster.kotlinmixins.processor.KMixinLeveledProcessor;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.core.world.processor.ProcessorContext;
import iskallia.vault.core.world.processor.tile.LeveledTileProcessor;
import iskallia.vault.core.world.processor.tile.TileProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

/*
* replaces map* of levels with table
* for some reason that map is hash map with linear search, but tree map would be better; doesn't matter since table is instant
* */
@Mixin(LeveledTileProcessor.class)
public class MixinLeveledProcessor {

    @Shadow public Map<Integer, TileProcessor> levels;

    private KMixinLeveledProcessor implementation = new KMixinLeveledProcessor(()->levels);

    @Overwrite(remap = false)
    public PartialTile process(PartialTile tile, ProcessorContext context) {
        return implementation.process(tile, context);
    }
}
