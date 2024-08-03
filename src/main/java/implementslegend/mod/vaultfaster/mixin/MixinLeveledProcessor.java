package implementslegend.mod.vaultfaster.mixin;

import iskallia.vault.core.Version;
import iskallia.vault.core.data.key.PaletteKey;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.VaultLevel;
import iskallia.vault.core.vault.VaultRegistry;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.core.world.processor.Palette;
import iskallia.vault.core.world.processor.ProcessorContext;
import iskallia.vault.core.world.processor.tile.LeveledTileProcessor;
import iskallia.vault.core.world.processor.tile.TileProcessor;
import iskallia.vault.init.ModConfigs;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraftforge.fml.config.ModConfig;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;

import java.lang.ref.PhantomReference;
import java.util.Map;


/*
* replaces map* of levels with table
* for some reason that map is hash map with linear search, but tree map would be better; doesn't matter since table is instant
* */
@Mixin(LeveledTileProcessor.class)
public class MixinLeveledProcessor {

    @Shadow public Map<Integer, TileProcessor> levels;
    private int cachedLevel = -1;

    private TileProcessor[] table;
    private PhantomReference<Vault> lastVault = new PhantomReference(null,null);
    public int getCachedLevel(ProcessorContext context) {//this could break when running 2 vaults simultaneously
        var lvl = cachedLevel;
        if (lvl<0 || !lastVault.refersTo(context.getVault())) {
            lastVault = new PhantomReference<>(context.getVault(),null);
            lvl= context.getVault() == null ? -1 : ((VaultLevel)context.getVault().get(Vault.LEVEL)).get();
            cachedLevel=lvl;
        }
        return lvl;
    }


    @Overwrite(remap = false)
    public PartialTile process(PartialTile tile, ProcessorContext context) {
        TileProcessor processor;
        int level = getCachedLevel(context);

        var table = getOrCreateTable();
        if(level<0)level=0;
        if(level>=table.length) level=table.length-1;
        processor=table[level];

        return processor == null ? tile : (PartialTile)processor.process(tile, context);
    }

    private  TileProcessor[] getOrCreateTable() {
        if(this.table==null) {
            var table = new TileProcessor[ModConfigs.LEVELS_META.getMaxLevel()+1];
            for (Map.Entry<Integer, TileProcessor> entry : this.levels.entrySet()) {
                for (int i = entry.getKey(); i < table.length; i++) {
                    table[i] = (TileProcessor) entry.getValue();
                }
            }
            this.table = table;
        }
        return table;
    }
}
