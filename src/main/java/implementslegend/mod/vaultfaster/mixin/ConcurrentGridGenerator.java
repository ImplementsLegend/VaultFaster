package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.ConcurrentGridCache;
import iskallia.vault.core.data.key.FieldKey;
import iskallia.vault.core.random.ChunkRandom;
import iskallia.vault.core.util.ObjectCache;
import iskallia.vault.core.util.RegionPos;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.world.generator.GridGenerator;
import iskallia.vault.core.world.generator.VaultGenerator;
import iskallia.vault.core.world.template.configured.ConfiguredTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GridGenerator.class)
public class ConcurrentGridGenerator {

    @Shadow @Final public static FieldKey<iskallia.vault.core.world.generator.layout.GridLayout> LAYOUT;
    @Shadow protected ObjectCache<RegionPos, ConfiguredTemplate> cache;
    private ConcurrentGridCache concurrentCache = new ConcurrentGridCache();


    @Redirect(method = "generate(Liskallia/vault/core/vault/Vault;Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/level/ChunkPos;)V",at = @At(value = "INVOKE", target = "Liskallia/vault/core/util/ObjectCache;has(Ljava/lang/Object;)Z"),remap = false)
    private <K> boolean alwaysCached(ObjectCache instance, K object){
        return true;
    }

    @Inject(method = "generate(Liskallia/vault/core/vault/Vault;Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/level/ChunkPos;)V",at = @At(value = "INVOKE", target = "Liskallia/vault/core/util/ObjectCache;get(Ljava/lang/Object;)Ljava/lang/Object;"),locals = LocalCapture.CAPTURE_FAILEXCEPTION,remap = false)
    private void concurrentCache(Vault vault, ServerLevelAccessor world, ChunkPos chunkPos, CallbackInfo ci, BlockPos pos1, BlockPos pos2, int offsetX, int offsetZ, int x, int z, RegionPos region, ChunkRandom random){
        cache.set(region,concurrentCache.getAt(region,((VaultGenerator)(Object)this).get(LAYOUT),vault,random));
    }
}
