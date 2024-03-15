package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.Performance;
import iskallia.vault.core.world.generator.DummyChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(DummyChunkGenerator.class)
public abstract class MeasurePerformance extends ChunkGenerator {
    private Performance performance = new Performance();

    public MeasurePerformance(Registry<StructureSet> p_207960_, Optional<HolderSet<StructureSet>> p_207961_, BiomeSource p_207962_) {
        super(p_207960_, p_207961_, p_207962_);
    }

    @Inject(method = "buildSurface", at = @At("TAIL"))
    private void chunkDone(WorldGenRegion genRegion, StructureFeatureManager structureFeatureManager, ChunkAccess chunk, CallbackInfo ci){
        performance.record();
    }

    @Override
    public void addDebugScreenInfo(List<String> p_208054_, BlockPos p_208055_) {
        p_208054_.add("generated chunks: "+performance.getCount());
    }
}
