package implementslegend.mod.vaultfaster.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ProtoChunk.class)
public interface ProtoChunkAccessor {
    @Accessor
    LevelLightEngine getLightEngine();
    @Accessor
    List<BlockPos> getLights();
}
