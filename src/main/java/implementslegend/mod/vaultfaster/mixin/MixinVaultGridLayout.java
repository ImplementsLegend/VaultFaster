package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.*;
import implementslegend.mod.vaultfaster.batchsetblocks.BlocksToProtoSectionTask;
import implementslegend.mod.vaultfaster.event.ObjectiveTemplateData;
import implementslegend.mod.vaultfaster.event.ObjectiveTemplateEvent;
import iskallia.vault.core.data.DataObject;
import iskallia.vault.core.data.key.FieldKey;
import iskallia.vault.core.event.Event;
import iskallia.vault.core.event.common.NoiseGenerationEvent;
import iskallia.vault.core.random.RandomSource;
import iskallia.vault.core.util.RegionPos;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.world.generator.layout.VaultGridLayout;
import iskallia.vault.core.world.generator.layout.VaultLayout;
import iskallia.vault.core.world.template.JigsawTemplate;
import iskallia.vault.core.world.template.PlacementSettings;
import iskallia.vault.core.world.template.Template;
import iskallia.vault.init.ModBlocks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@Mixin(VaultGridLayout.class)
public abstract class MixinVaultGridLayout {

    @Shadow @Final public static FieldKey<Void> FILL_AIR;

    /*faster way to fill the world with bedrock, could be even faster if you just replaced palette entry*/
    @Redirect(method = "initServer",at = @At(value = "INVOKE", target = "Liskallia/vault/core/event/common/NoiseGenerationEvent;register(Ljava/lang/Object;Ljava/util/function/Consumer;)Liskallia/vault/core/event/Event;"),remap = false)
    private Event batchFill(NoiseGenerationEvent instance, Object o, Consumer consumer){

        return instance.register(o,((DataObject)(Object)this).has(FILL_AIR)?(obj)->{}:(NoiseGenerationEvent.Data obj)->{
            List.of(0,1,2,3).stream().map((section)->
                SectionedTemplateKt.getGENERATOR_EXECUTOR().submit(()->{
                    (new BlocksToProtoSectionTask(obj.getChunk()))
                            .setBlocks(
                                    section,
                                    BedrockFillKt.blocksToFill(obj.getChunk().getPos().getWorldPosition().atY(16 * section - obj.getGenRegion().getMinBuildHeight())),
                                    true
                            );
                })
            ).forEach((it)-> {
                try {
                    it.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
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
    boolean deleteOriginalObjectiveAdd(List instance, Object e){
        return true;
    }

    @Redirect(method = "getAt",at = @At(value = "INVOKE", target = "Liskallia/vault/core/random/RandomSource;nextFloat()F"),remap = false)
    float deleteOriginalObjectiveCondition(RandomSource instance){
        return 1f;
    }

    @Inject(method = "getAt",at = @At(value = "INVOKE", target = "Liskallia/vault/core/world/template/JigsawTemplate;getChildren()Ljava/util/List;",ordinal = 2),locals = LocalCapture.CAPTURE_FAILHARD,remap = false)
    private void invokeObjectiveFixEvent(Vault vault, RegionPos region, RandomSource random, PlacementSettings settings, CallbackInfoReturnable<Template> cir, VaultLayout.PieceType type, Template jigsawUncast, JigsawTemplate jigsaw, Iterator iterator, JigsawTemplate target, double probability){
        if(random.nextFloat() < probability && target!=null) {
            var data = new ObjectiveTemplateData(target, vault,random);
            ObjectiveTemplateEvent.INSTANCE.invoke(data);
            jigsaw.getChildren().add(data.getTemplate());
        }
    }
}
