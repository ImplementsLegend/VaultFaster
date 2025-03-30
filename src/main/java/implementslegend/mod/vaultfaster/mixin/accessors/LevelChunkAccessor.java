package implementslegend.mod.vaultfaster.mixin.accessors;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelChunk.class)
public interface LevelChunkAccessor {
    @Invoker <T extends BlockEntity> void callUpdateBlockEntityTicker(T p_156407_);
}
