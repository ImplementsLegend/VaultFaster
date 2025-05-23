package implementslegend.mod.vaultfaster.mixin;

import iskallia.vault.core.world.data.tile.PartialBlockProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/*
* replaces regular hash map with linked hash map for faster iteration
* */

@Mixin(PartialBlockProperties.class)
public abstract class MixinPartBlockProperties  {

    @Shadow @Final private Map<String, String> properties;

    @Shadow public abstract Map<String, String> getProperties();


    private static final HashMap ANTIALLOCATION = new HashMap();

    @Redirect(method = "empty", at = @At(value = "NEW", target = "()Ljava/util/HashMap;"), remap = false)
    private static HashMap useLinkedHashMap(){
        return ANTIALLOCATION;
    }
    @Redirect(method = "of(Lnet/minecraft/world/level/block/state/BlockState;)Liskallia/vault/core/world/data/tile/PartialBlockProperties;", at = @At(value = "NEW", target = "()Ljava/util/HashMap;"), remap = false)
    private static HashMap useLinkedHashMap2(){
        return ANTIALLOCATION;
    }


    @Inject(method = "copy()Liskallia/vault/core/world/data/tile/PartialBlockProperties;", at = @At("HEAD"), remap = false,cancellable = true)
    private void useLinkedHashMap3(CallbackInfoReturnable<PartialBlockProperties> cir){
        cir.setReturnValue(PartialBlockProperties.of(new LinkedHashMap<>(properties)));
    }


    @ModifyVariable(method = "<init>", at = @At(value = "HEAD"), argsOnly = true)
    private static Map useLinkedHashMap(Map values){
        return (values instanceof LinkedHashMap<?,?>)?values:new LinkedHashMap<String,String>(values);
    }
    @ModifyVariable(method = "of(Lnet/minecraft/world/level/block/state/BlockState;)Liskallia/vault/core/world/data/tile/PartialBlockProperties;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getProperties()Ljava/util/Collection;"), argsOnly = false)
    private static Map useLinkedHashMap2(Map values){
        return new LinkedHashMap<String,String>(values);
    }
}
