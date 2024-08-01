package implementslegend.mod.vaultfaster.mixin;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import iskallia.vault.core.world.storage.VirtualWorld;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.server.level.*;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.IntFunction;
import java.util.function.Supplier;
/*
*
* mojang be like: lets generate world with multithreading; 1 thread at a time
*
* */
@Mixin(ChunkMap.class)
public abstract class VaultBypassMailbox {

    private Executor mainWorker;

    @Shadow @Final private ServerLevel level;

    @Shadow protected abstract ChunkStatus getDependencyStatus(ChunkStatus p_140263_, int p_140264_);

    @Shadow protected abstract CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> getChunkRangeFuture(ChunkPos p_140211_, int p_140212_, IntFunction<ChunkStatus> p_140213_);

    @Shadow protected abstract void releaseLightTicket(ChunkPos p_140376_);

    @Shadow @Final private BlockableEventLoop<Runnable> mainThreadExecutor;

    @Shadow @Final private ChunkProgressListener progressListener;

    @Shadow protected abstract CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> protoChunkToFullChunk(ChunkHolder p_140384_);


    @Shadow private ChunkGenerator generator;

    @Shadow @Final private StructureManager structureManager;

    @Shadow @Final private ThreadedLevelLightEngine lightEngine;

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void captureMainWroket(ServerLevel p_143040_, LevelStorageSource.LevelStorageAccess p_143041_, DataFixer p_143042_, StructureManager p_143043_, Executor p_143044_, BlockableEventLoop p_143045_, LightChunkGetter p_143046_, ChunkGenerator p_143047_, ChunkProgressListener p_143048_, ChunkStatusUpdateListener p_143049_, Supplier p_143050_, int p_143051_, boolean p_143052_, CallbackInfo ci){
        mainWorker=p_143044_;
    }

    @Inject(method = "scheduleChunkGeneration",at = @At("HEAD"),cancellable = true)
    private void scheduleVaultChunkGeneration(ChunkHolder p_140361_, ChunkStatus p_140362_, CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> cir){
        if(level instanceof VirtualWorld w){

            ChunkPos chunkpos = p_140361_.getPos();
            CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> completablefuture = this.getChunkRangeFuture(chunkpos, p_140362_.getRange(), (p_203072_) -> {
                return this.getDependencyStatus(p_140362_, p_203072_);
            });
            this.level.getProfiler().incrementCounter(() -> "chunkGenerate " + p_140362_.getName());
            var future = completablefuture.thenComposeAsync((p_203016_) -> {
                return p_203016_.map((p_203022_) -> {
                    try {
                        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture1 = p_140362_.generate(mainWorker, this.level, this.generator, this.structureManager, this.lightEngine, (p_203062_) -> {
                            return this.protoChunkToFullChunk(p_140361_);
                        }, p_203022_, false);
                        this.progressListener.onStatusChange(chunkpos, p_140362_);
                        return completablefuture1;
                    } catch (Exception exception) {
                        exception.getStackTrace();
                        CrashReport crashreport = CrashReport.forThrowable(exception, "Exception generating new chunk");
                        CrashReportCategory crashreportcategory = crashreport.addCategory("Chunk to be generated");
                        crashreportcategory.setDetail("Location", String.format("%d,%d", chunkpos.x, chunkpos.z));
                        crashreportcategory.setDetail("Position hash", ChunkPos.asLong(chunkpos.x, chunkpos.z));
                        crashreportcategory.setDetail("Generator", this.generator);
                        this.mainThreadExecutor.execute(() -> {
                            throw new ReportedException(crashreport);
                        });
                        throw new ReportedException(crashreport);
                    }
                }, (p_203010_) -> {
                    this.releaseLightTicket(chunkpos);
                    return CompletableFuture.completedFuture(Either.right(p_203010_));
                });
            }, mainWorker);

            cir.setReturnValue(future);
        }
    }

}
