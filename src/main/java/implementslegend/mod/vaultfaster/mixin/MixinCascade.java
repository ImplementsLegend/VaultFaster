package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.interfaces.CascadeApplicableIndices;
import implementslegend.mod.vaultfaster.interfaces.IndexedBlock;
import iskallia.vault.core.vault.modifier.modifier.DecoratorCascadeModifier;
import iskallia.vault.core.vault.modifier.spi.VaultModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(DecoratorCascadeModifier.class)
public abstract class MixinCascade extends VaultModifier<DecoratorCascadeModifier.Properties> {

    public MixinCascade(ResourceLocation id, DecoratorCascadeModifier.Properties properties, Display display) {
        super(id, properties, display);
    }

    @Inject(method = "lambda$onGenerate$3",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"),cancellable = true,locals = LocalCapture.CAPTURE_FAILHARD)
    private void earlyCheck(ServerLevelAccessor world, ChunkAccess access, String phase, List tiles, List pending, BlockPos pos, CallbackInfo ci, BlockState state){
        var filter = ((CascadeApplicableIndices)this.properties).getApplicableIndices();
        if(filter!=null){
            if(!filter.get(((IndexedBlock)state.getBlock()).getRegistryIndex())){
                ci.cancel();
            }
        }
    }


}
