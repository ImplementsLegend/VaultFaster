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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;


/*
 * adds TileMapper; faster processing of tiles
 */


@Mixin(PlacementSettings.class)
public class MixinPlacementSettings implements ExtendedPlacementSettings {
    @Shadow
    protected List<TileProcessor> tileProcessors;
    private TileMapper mapper = new TileMapper();

    CompletableFuture<TileMapper> futureTileMapper = new CompletableFuture();
    private AtomicBoolean tileMapperCreationStarted = new AtomicBoolean();

    @NotNull
    @Override
    public TileMapper getTileMapper() {
        TileMapper privateMapper;
        if(!tileMapperCreationStarted.getPlain() && tileMapperCreationStarted.compareAndSet(false,true)){
            privateMapper=new TileMapper();
            tileProcessors.forEach(processor -> {
                privateMapper.addProcessor(processor);
            });
            futureTileMapper.complete(privateMapper);
        }else {
            try {
                privateMapper=futureTileMapper.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        return privateMapper;
    }

    @Override
    public void resetTileMapper(){
        if(getHasTileMapperCreationStarted()){
            try {
                futureTileMapper.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            futureTileMapper=new CompletableFuture();
            setHasTileMapperCreationStarted(false);
        }
        /*
        var privateMapper = new TileMapper();
        tileProcessors.forEach(processor -> {
            privateMapper.addProcessor(processor);
        });
        mapper=privateMapper;*/

    }


    @Inject(method = "addProcessor(Liskallia/vault/core/world/processor/Processor;)Liskallia/vault/core/world/template/PlacementSettings;",at = @At("HEAD"),remap = false)
    private void registerProcessor(Processor processor, CallbackInfoReturnable<Palette> cir){
        if(processor instanceof TileProcessor) resetTileMapper();//mapper.addProcessor((TileProcessor) processor);
    }
    @Inject(method = "copy",locals = LocalCapture.CAPTURE_FAILHARD,at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Liskallia/vault/core/world/template/PlacementSettings;flags:I"/*,target = "Liskallia/vault/core/world/template/PlacementSettings;<init>(Liskallia/vault/core/world/processor/ProcessorContext;)V"*/),remap = false)
    private void copyMapper(CallbackInfoReturnable<PlacementSettings> cir, PlacementSettings nw){
        if (getHasTileMapperCreationStarted()) {
            ((TileMapperContainer) nw).getFutureTileMapper().complete(getTileMapper());
            ((TileMapperContainer) nw).setHasTileMapperCreationStarted(true);
        }
        /*
        var tileMapper = ((TileMapperContainer) nw).getTileMapper();
        tileProcessors.forEach(processor -> {
            tileMapper.addProcessor(processor);
        });*/

    }

    @Override
    public void addProcessorAtBegining(@NotNull TileProcessor tileProcessor) {
        resetTileMapper();
        tileProcessors.add(0,tileProcessor);
        //mapper.addProcessor( tileProcessor, true);*
    }

    @NotNull
    @Override
    public CompletableFuture<TileMapper> getFutureTileMapper() {
        return futureTileMapper;
    }

    @Override
    public boolean getHasTileMapperCreationStarted() {
        return tileMapperCreationStarted.getPlain() || tileMapperCreationStarted.get();
    }

    @Override
    public void setHasTileMapperCreationStarted(boolean b) {
        tileMapperCreationStarted.set(b);
    }
}
