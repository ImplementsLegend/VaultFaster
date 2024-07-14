package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.ObjectiveTemplateEvent;
import iskallia.vault.core.event.Event;
import iskallia.vault.core.event.common.BlockSetEvent;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.objective.*;
import iskallia.vault.core.world.storage.VirtualWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;


/*
* removal of CommonEvents.BLOCK_SET_EVENT caused objectives to stop working; this is a fix (but herald still doesn't work)
*
* */
@Mixin({ScavengerObjective.class, LodestoneObjective.class, HeraldObjective.class, ObeliskObjective.class, MonolithObjective.class, CrakePedestalObjective.class})
public class FixObjectives {


    /*
    * replaces old block set event with new objective template event
    * */
    @Redirect(method = "initServer",at = @At(value = "INVOKE", target = "Liskallia/vault/core/event/common/BlockSetEvent;register(Ljava/lang/Object;Ljava/util/function/Consumer;)Liskallia/vault/core/event/Event;"),remap = false)
    private Event fixObjective(BlockSetEvent instance, Object o, Consumer consumer){
        /*var objEvent = ObjectiveTemplateEventKt.getOBJECTIVE_TEMPLATE_EVENT();
        objEvent.registerObjectiveTemplate((Objective)(Object)this);
        return objEvent;*/
        return null;
    }

    @Inject(method = "initServer",at = @At("HEAD"),remap = false)
    private void fixObjective2(VirtualWorld world, Vault vault, CallbackInfo ci){

        ObjectiveTemplateEvent.INSTANCE.registerObjectiveTemplate((Objective)(Object)this,vault);

    }

}
