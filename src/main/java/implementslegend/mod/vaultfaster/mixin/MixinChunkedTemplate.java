package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.AtomicallyIndexedConcurrentArrayCollection;
import implementslegend.mod.vaultfaster.VaultGenerationExecutorKt;
import iskallia.vault.core.world.data.entity.PartialEntity;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.core.world.template.PlacementSettings;
import iskallia.vault.core.world.template.StaticTemplate;
import iskallia.vault.core.world.template.Template;
import iskallia.vault.core.world.template.configured.ChunkedTemplate;
import iskallia.vault.core.world.template.configured.ConfiguredTemplate;
import kotlin.Unit;
import net.minecraft.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(ChunkedTemplate.class)
public abstract class MixinChunkedTemplate extends ConfiguredTemplate {

    @Mutable
    @Shadow @Final private Map<ChunkPos, StaticTemplate> cache;


    private final AtomicBoolean lockedAtomic = new AtomicBoolean(false);
    private final CompletableFuture<Unit> isFullyGenerated = new CompletableFuture<>();

    public MixinChunkedTemplate(Template parent, PlacementSettings settings) {
        super(parent, settings);
    }

    @Inject(method = "<init>",at = @At("TAIL"))
    private void concurrentCache(Template parent, PlacementSettings settings, CallbackInfo ci){

        cache=new ConcurrentHashMap<ChunkPos,StaticTemplate>();
    }

    @Redirect(method = {"lambda$onTile$0","lambda$onEntity$1"},at = @At(value = "NEW", target = "(Ljava/lang/Iterable;Ljava/lang/Iterable;)Liskallia/vault/core/world/template/StaticTemplate;"))
    private static StaticTemplate newTemplate(Iterable tiles, Iterable entities){
        return new StaticTemplate(new AtomicallyIndexedConcurrentArrayCollection(new PartialTile[32768]),new ArrayList<>(128));
    }


    @Redirect(method = "place",at = @At(value = "FIELD", target = "Liskallia/vault/core/world/template/configured/ChunkedTemplate;locked:Z",opcode = Opcodes.GETFIELD),remap = false)
    private boolean isLockedAtomic(ChunkedTemplate thiz){
        return lockedAtomic.getAndSet(true);
    }

    @Inject(method = "place",at = @At(value = "INVOKE", target = "Ljava/util/Collection;forEach(Ljava/util/function/Consumer;)V"),cancellable = true,remap = false)
    private void dontTrim(ServerLevelAccessor world, ChunkPos pos, CallbackInfo ci){
        isFullyGenerated.complete(Unit.INSTANCE);
        ci.cancel();
    }
    @Inject(method = "place",at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"),cancellable = true,remap = false)
    private void placeAsync(ServerLevelAccessor world, ChunkPos pos, CallbackInfo ci){
        try {
            isFullyGenerated.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        StaticTemplate child = this.cache.get(pos);
        if (child != null) {
            child.place(world, this.settings);
        }
        ci.cancel();
    }

}
