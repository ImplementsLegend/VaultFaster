package implementslegend.mod.vaultfaster.mixin.concurrency;

import implementslegend.mod.vaultfaster.kotlinmixins.concurrency.KMixinConcurrentGridGenerator;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.world.generator.GridGenerator;
import iskallia.vault.core.world.generator.VaultGenerator;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GridGenerator.class)
public class ConcurrentGridGenerator {

    private KMixinConcurrentGridGenerator implementation = new KMixinConcurrentGridGenerator((VaultGenerator) (Object)this);

    @Overwrite(remap = false)
    public void generate(Vault vault, ServerLevelAccessor world, ChunkPos chunkPos) {
        implementation.generate(vault,world,chunkPos);
    }
}
