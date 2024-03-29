package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.CachedPaletteContainer;
import implementslegend.mod.vaultfaster.TileMapperContainer;
import iskallia.vault.core.Version;
import iskallia.vault.core.data.key.PaletteKey;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.VaultRegistry;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.core.world.processor.Palette;
import iskallia.vault.core.world.processor.ProcessorContext;
import iskallia.vault.core.world.processor.tile.ReferenceTileProcessor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.lang.ref.PhantomReference;

/*
* caches some slow accesses
* */

@Mixin(ReferenceTileProcessor.class)
public class MixinReferenceTileProcessor implements CachedPaletteContainer {

    @Shadow @Final private ResourceLocation id;
    private Palette cachedPalette;
    private Version cachedVersion;
    private PhantomReference<Vault> lastVault = new PhantomReference(null,null);

    @Overwrite(remap = false)
    public PartialTile process(PartialTile value, ProcessorContext context) {
        value=((TileMapperContainer)getCachedPalette(context)).getTileMapper().mapBlock(value, context);
        return value;
    }

    @NotNull
    @Override
    public Palette getCachedPalette(ProcessorContext context) {//this could break when running 2 vaults simultaneously
        var palette = cachedPalette;
        if (cachedVersion==null || cachedPalette==null || !lastVault.refersTo(context.getVault())) {
            lastVault = new PhantomReference<>(context.getVault(),null);
            Version version = context.getVault() == null ? Version.latest() : (Version)context.getVault().get(Vault.VERSION);
            if(version!=cachedVersion){
                cachedVersion=version;
                palette = (Palette)((PaletteKey) VaultRegistry.PALETTE.getKey(this.id)).get(version);
                cachedPalette=palette;
            }
        }
        return palette;
    }
}
