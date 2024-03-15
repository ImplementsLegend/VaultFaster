package implementslegend.mod.vaultfaster.mixin;

import iskallia.vault.core.world.data.tile.PartialBlockProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.LinkedHashMap;
import java.util.Map;

/*
* replaces regular hash map with linked hash map for faster iteration
* */

@Mixin(PartialBlockProperties.class)
public class MixinPartBlockProperties  {



    @ModifyVariable(method = "<init>", at = @At(value = "HEAD"), argsOnly = true)
    private static Map useLinkedHashMap(Map values){
        return new LinkedHashMap<String,String>(values);
    }
}
