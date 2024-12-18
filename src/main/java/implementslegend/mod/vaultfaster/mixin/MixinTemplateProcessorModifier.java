package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.event.TemplateConfigurationEvent;
import implementslegend.mod.vaultfaster.interfaces.SectionedTemplateContainer;
import iskallia.vault.core.event.common.TemplateGenerationEvent;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.modifier.modifier.TemplateProcessorModifier;
import iskallia.vault.core.vault.modifier.spi.ModifierContext;
import iskallia.vault.core.vault.modifier.spi.VaultModifier;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.core.world.processor.ProcessorContext;
import iskallia.vault.core.world.processor.tile.TileProcessor;
import iskallia.vault.core.world.storage.VirtualWorld;
import iskallia.vault.core.world.template.configured.ChunkedTemplate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(TemplateProcessorModifier.class)
public abstract class MixinTemplateProcessorModifier extends VaultModifier<TemplateProcessorModifier.Properties> {

    @Shadow protected abstract PartialTile lambda$onVaultAdd$1(TemplateGenerationEvent.Data par1, PartialTile par2, ProcessorContext par3);

    public MixinTemplateProcessorModifier(ResourceLocation id, TemplateProcessorModifier.Properties properties, Display display) {
        super(id, properties, display);
    }

    @Inject(method = "onVaultAdd",at=@At("HEAD"),remap = false,cancellable = true)
    private void onTemplateConfigured(VirtualWorld world, Vault vault, ModifierContext context, CallbackInfo ci){
        TemplateConfigurationEvent.INSTANCE.register(vault,(data)->{
            if(data.getVault()==vault){
                var data1=new TemplateGenerationEvent.Data(world,null,null,null,null,null);
                data.getTemplateSettings().addProcessor(TileProcessor.of((tile, ctx) -> lambda$onVaultAdd$1(data1,tile,ctx)));
            }
        });
        ci.cancel();
    }
}
