package implementslegend.mod.vaultfaster.mixin.processor;

import implementslegend.mod.vaultfaster.FastWeightedList;
import iskallia.vault.core.random.RandomSource;
import iskallia.vault.core.util.WeightedList;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.core.world.processor.tile.WeightedTileProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(WeightedTileProcessor.class)
public class MixinWeightedProcessor {
    private FastWeightedList<PartialTile> cached;

    @Redirect(method = "process(Liskallia/vault/core/world/data/tile/PartialTile;Liskallia/vault/core/world/processor/ProcessorContext;)Liskallia/vault/core/world/data/tile/PartialTile;",at= @At(value = "INVOKE", target = "Liskallia/vault/core/util/WeightedList;getRandom(Liskallia/vault/core/random/RandomSource;)Ljava/util/Optional;"),remap = false)
    private Optional<PartialTile> fastRandom(WeightedList<PartialTile> instance, RandomSource random){
        var list = cached;
        if(list==null){
            list=new FastWeightedList<>(instance);
            cached=list;
        }
        return Optional.ofNullable(list.random(random));
    }
}
