package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.interfaces.ExtendedPlacementSettings;
import implementslegend.mod.vaultfaster.TileMapper;
import implementslegend.mod.vaultfaster.interfaces.TileMapperContainer;
import iskallia.vault.core.world.processor.Palette;
import iskallia.vault.core.world.processor.Processor;
import iskallia.vault.core.world.processor.tile.*;
import iskallia.vault.core.world.template.PlacementSettings;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;


/*
 * adds TileMapper; faster processing of tiles
 */


@Mixin(PlacementSettings.class)
public class MixinPlacementSettings implements ExtendedPlacementSettings {
    @Shadow
    protected List<TileProcessor> tileProcessors;
    private TileMapper mapper = new TileMapper();

    @NotNull
    @Override
    public TileMapper getTileMapper() {
        return mapper;
    }


    @Inject(method = "addProcessor(Liskallia/vault/core/world/processor/Processor;)Liskallia/vault/core/world/template/PlacementSettings;",at = @At("HEAD"),remap = false)
    private void registerProcessor(Processor processor, CallbackInfoReturnable<Palette> cir){
        if(processor instanceof TileProcessor) mapper.addProcessor((TileProcessor) processor);
    }
    @Inject(method = "copy",locals = LocalCapture.CAPTURE_FAILHARD,at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Liskallia/vault/core/world/template/PlacementSettings;flags:I"/*,target = "Liskallia/vault/core/world/template/PlacementSettings;<init>(Liskallia/vault/core/world/processor/ProcessorContext;)V"*/),remap = false)
    private void copyMapper(CallbackInfoReturnable<PlacementSettings> cir, PlacementSettings nw){

        var tileMapper = ((TileMapperContainer) nw).getTileMapper();
        tileProcessors.forEach(processor -> {
            tileMapper.addProcessor(processor);
        });

    }

    @Override
    public void addProcessorAtBegining(@NotNull TileProcessor tileProcessor) {
        tileProcessors.add(0,tileProcessor);
        mapper.addProcessor( tileProcessor, true);
    }
}
