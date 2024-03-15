package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.BatchSetBlockKt;
import implementslegend.mod.vaultfaster.ObjectiveTemplateData;
import implementslegend.mod.vaultfaster.ObjectiveTemplateEvent;
import implementslegend.mod.vaultfaster.SectionBlocksKt;
import iskallia.vault.core.data.DataObject;
import iskallia.vault.core.data.key.FieldKey;
import iskallia.vault.core.event.Event;
import iskallia.vault.core.event.common.NoiseGenerationEvent;
import iskallia.vault.core.random.RandomSource;
import iskallia.vault.core.util.RegionPos;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.world.generator.layout.VaultLayout;
import iskallia.vault.core.world.template.JigsawTemplate;
import iskallia.vault.core.world.template.PlacementSettings;
import iskallia.vault.core.world.template.Template;
import iskallia.vault.init.ModBlocks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Consumer;

@Mixin(VaultLayout.class)
public abstract class MixinVaultLayout {

    @Shadow @Final public static FieldKey<Void> FILL_AIR;

    /*faster way to fill the world with bedrock, could be even faster if you just replaced palette entry*/
    @Redirect(method = "initServer",at = @At(value = "INVOKE", target = "Liskallia/vault/core/event/common/NoiseGenerationEvent;register(Ljava/lang/Object;Ljava/util/function/Consumer;)Liskallia/vault/core/event/Event;"),remap = false)
    private Event batchFill(NoiseGenerationEvent instance, Object o, Consumer consumer){

        return instance.register(o,((DataObject)(Object)this).has(FILL_AIR)?(obj)->{}:(NoiseGenerationEvent.Data obj)->{
            for(var section = 3;section>=0;section--) {

                BatchSetBlockKt.setBlocks(obj.getChunk(), section, SectionBlocksKt.blocksToFill(obj.getChunk().getPos().getWorldPosition().atY(16*section-obj.getGenRegion().getMinBuildHeight())),true);
            }
            obj.getChunk().getHeightmaps().forEach((entry)->{//batchSetBlocks skips heightmap updates; must be done manually
                for(var x = 0;x<16;x++) {
                    for(var z = 0;z<16;z++) {
                        entry.getValue().update(x, 63, z, ModBlocks.VAULT_BEDROCK.defaultBlockState());
                    }
                }
            });
        });
    }


    /*calls event to change template for objective placeholder since the old method is removed*/
    @Redirect(method = "getAt",at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z",ordinal = 0),remap = false)
    boolean invokeObjectiveFixEvent(List instance, Object e){
        var data = new ObjectiveTemplateData((JigsawTemplate) e);
        ObjectiveTemplateEvent.INSTANCE.invoke(data);
        return instance.add(data.getTemplate());
    }
}
