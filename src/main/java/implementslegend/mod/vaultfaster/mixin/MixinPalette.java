package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.TileMapper;
import implementslegend.mod.vaultfaster.TileMapperContainer;
import iskallia.vault.core.world.processor.Palette;
import iskallia.vault.core.world.processor.tile.TileProcessor;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

/*
 * adds TileMapper; faster processing of tiles
 */

@Mixin(Palette.class)
public class MixinPalette implements TileMapperContainer {
    @Shadow protected List<TileProcessor> tileProcessors;
    private TileMapper mapper = new TileMapper();

    @NotNull
    @Override
    public TileMapper getTileMapper() {
        return mapper;
    }

    @Inject(method = "processTile",at = @At("HEAD"),remap = false)
    private void registerProcessor(TileProcessor processor, CallbackInfoReturnable<Palette> cir){
        mapper.addProcessor(processor);
    }
    @Inject(method = "copy",locals = LocalCapture.CAPTURE_FAILHARD,at = @At(value = "NEW", shift = At.Shift.BY, by = 4, target = "()Liskallia/vault/core/world/processor/Palette;"),remap = false)
    private void copyMapper(CallbackInfoReturnable<Palette> cir, Palette nw){
        tileProcessors.forEach(((TileMapperContainer) nw).getTileMapper()::addProcessor);
    }

}
