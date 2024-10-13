package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.interfaces.LazyObject;
import iskallia.vault.core.world.data.entity.EntityPredicate;
import iskallia.vault.core.world.data.entity.PartialEntity;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.core.world.data.tile.TilePredicate;
import iskallia.vault.core.world.template.PlacementSettings;
import iskallia.vault.core.world.template.StructureTemplate;
import kotlin.Unit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(StructureTemplate.class)
public abstract class LazyStructureTemplate implements LazyObject {
    @Shadow private String path;

    @Shadow public abstract void deserializeNBT(CompoundTag nbt);

    @Shadow private Set<ResourceLocation> tags;
    private AtomicReference<CompletableFuture<Unit>> initializationTask = new AtomicReference<>(CompletableFuture.completedFuture(Unit.INSTANCE));

    private HashSet<ResourceLocation> preInitTags = new HashSet<>();

    private static ExecutorService LAZY_LOADING_EXECUTOR = Executors.newSingleThreadExecutor((runnable)->{
        var thread = new Thread(runnable);
        thread.setName("LazyLoadingThread");
        return thread;
    });

    @Override
    public void setUninitialized() {
        initializationTask.set(null);
    }

    @Redirect(method = "fromPath(Ljava/lang/String;Liskallia/vault/core/world/data/tile/TilePredicate;)Liskallia/vault/core/world/template/StructureTemplate;",at = @At(value = "INVOKE", target = "Liskallia/vault/core/world/template/StructureTemplate;deserializeNBT(Lnet/minecraft/nbt/CompoundTag;)V"),remap = false)
    private static void markForLazyInit(StructureTemplate instance, CompoundTag entityNBT){
        ((LazyObject)instance).setUninitialized();
    }

    @Unique
    private void initializeIfNot(){
        var intTask = new CompletableFuture<Unit>();
        if(initializationTask.compareAndSet(null,intTask)){

            intTask.completeAsync(()->{
                try {
                    deserializeNBT(NbtIo.readCompressed(new FileInputStream(path)));
                } catch (IOException var4) {
                    var4.printStackTrace();
                } catch (Exception e){
                    e.printStackTrace();
                }
                tags.addAll(preInitTags);
                return Unit.INSTANCE;
            }, LAZY_LOADING_EXECUTOR);
        }else intTask = initializationTask.get();
        try {
            intTask.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Inject(method={"getTiles"},at = @At("HEAD"),remap = false)
    private void invokeLazyInit0(TilePredicate filter, PlacementSettings settings, CallbackInfoReturnable<Iterator<PartialTile>> cir){initializeIfNot();}

    @Inject(method={"serializeNBT()Lnet/minecraft/nbt/CompoundTag;"},at = @At("HEAD"),remap = false)
    private void invokeLazyInit1(CallbackInfoReturnable<CompoundTag> cir){initializeIfNot();}

    @Inject(method={"getEntities"},at = @At("HEAD"),remap = false)
    private void invokeLazyInit2(EntityPredicate filter, PlacementSettings settings, CallbackInfoReturnable<Iterator<PartialEntity>> cir){initializeIfNot();}

    @Inject(method={"addTag"},at = @At("HEAD"),remap = false)
    private void invokeLazyInit3(ResourceLocation tag, CallbackInfo ci){preInitTags.add(tag);}

    @Inject(method={"hasTag"},at = @At("HEAD"),remap = false,cancellable = true)
    private void invokeLazyInit4(ResourceLocation tag, CallbackInfoReturnable<Boolean> cir){if(preInitTags.contains(tag))cir.setReturnValue(true); else initializeIfNot();}

    @Inject(method={"getTags"},at = @At("HEAD"),remap = false)
    private void invokeLazyInit5(CallbackInfoReturnable<Iterator<ResourceLocation>> cir){initializeIfNot();}
}
