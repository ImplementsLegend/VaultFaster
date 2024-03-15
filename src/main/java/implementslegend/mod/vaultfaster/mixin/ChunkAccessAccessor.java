package implementslegend.mod.vaultfaster.mixin;

import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkAccess.class)
public interface ChunkAccessAccessor {
    @Accessor
    void setUnsaved(boolean value);
}
