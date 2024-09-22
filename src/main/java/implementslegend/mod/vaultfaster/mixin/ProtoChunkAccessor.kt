package implementslegend.mod.vaultfaster.mixin

import net.minecraft.core.BlockPos
import net.minecraft.world.level.chunk.ProtoChunk
import net.minecraft.world.level.lighting.LevelLightEngine
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(ProtoChunk::class)
interface ProtoChunkAccessor {
    @get:Accessor
    val lights: MutableList<BlockPos>?
}
