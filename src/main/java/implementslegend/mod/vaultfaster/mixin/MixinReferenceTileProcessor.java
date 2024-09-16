package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.interfaces.CachedPaletteContainer;
import iskallia.vault.core.Version;
import iskallia.vault.core.util.WeightedList;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.VaultRegistry;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.core.world.processor.Palette;
import iskallia.vault.core.world.processor.ProcessorContext;
import iskallia.vault.core.world.processor.tile.ReferenceTileProcessor;
import iskallia.vault.core.world.processor.tile.TileProcessor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.ref.PhantomReference;
import java.util.Map;

/*
* caches some slow accesses
* */

@Mixin(ReferenceTileProcessor.class)
public class MixinReferenceTileProcessor implements CachedPaletteContainer {

    @Shadow @Final private WeightedList<ResourceLocation> pool;
    private Palette cachedPalette;
    private Version cachedVersion;
    private PhantomReference<Vault> lastVault = new PhantomReference(null,null);

    @Inject(method = "process(Liskallia/vault/core/world/data/tile/PartialTile;Liskallia/vault/core/world/processor/ProcessorContext;)Liskallia/vault/core/world/data/tile/PartialTile;",at = @At("HEAD"),remap = false,cancellable = true)
    public void processSimple(PartialTile value, ProcessorContext context, CallbackInfoReturnable<PartialTile> cir) {
        if(pool.isEmpty()){
            cir.setReturnValue(value);
        }else if(pool.size()==1){

            var palette = getCachedPalette(context);

            for(TileProcessor child : palette.getTileProcessors()) {
                value = child.process(value, context);
                if (value == null) {
                    cir.setReturnValue(null);
                    break;
                }
            }
            cir.setReturnValue( value);
        }
    }

    @NotNull
    @Override
    public Palette getCachedPalette(ProcessorContext context) {//this could break when running 2 vaults simultaneously
        var palette = cachedPalette;
        if (cachedVersion==null || cachedPalette==null || !lastVault.refersTo(context.getVault())) {
            lastVault = new PhantomReference<>(context.getVault(),null);
            Version version = context.getVault() == null ? Version.latest() : (Version)context.getVault().get(Vault.VERSION);
            if(version!=cachedVersion || palette==null){
                cachedVersion=version;
                palette = VaultRegistry.PALETTE.getKey((ResourceLocation) this.pool.entrySet().toArray(new Map.Entry[0])[0].getKey()).get(version);
                cachedPalette=palette;
            }
        }
        return palette;
    }

    @NotNull
    @Override
    public Palette getCachedPaletteForVersion(Version version) {//this could break when running 2 vaults simultaneously
        if(cachedPalette==null){
            cachedPalette = VaultRegistry.PALETTE.getKey((ResourceLocation) this.pool.entrySet().toArray(new Map.Entry[0])[0].getKey()).get(version);
        }
        return cachedPalette;
    }
}
