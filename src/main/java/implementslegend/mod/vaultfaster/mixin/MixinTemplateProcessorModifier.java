package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.event.TemplateConfigurationEvent;
import implementslegend.mod.vaultfaster.interfaces.SectionedTemplateContainer;
import iskallia.vault.core.event.common.TemplateGenerationEvent;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.modifier.modifier.TemplateProcessorModifier;
import iskallia.vault.core.vault.modifier.spi.ModifierContext;
import iskallia.vault.core.vault.modifier.spi.VaultModifier;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.core.world.processor.tile.TileProcessor;
import iskallia.vault.core.world.storage.VirtualWorld;
import iskallia.vault.core.world.template.configured.ChunkedTemplate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(TemplateProcessorModifier.class)
public abstract class MixinTemplateProcessorModifier extends VaultModifier<TemplateProcessorModifier.Properties> {
    public MixinTemplateProcessorModifier(ResourceLocation id, TemplateProcessorModifier.Properties properties, Display display) {
        super(id, properties, display);
    }

    /*@Inject(method = "lambda$onVaultAdd$2",at=@At("HEAD"),remap = false,cancellable = true)
        private void stopMultipleAdditions(TemplateGenerationEvent.Data data, CallbackInfo ci){
            if(data.getTemplate() instanceof ChunkedTemplate ct && ((SectionedTemplateContainer)ct).getSectionedTemplate().getStarted().get())ci.cancel();
        }*/
    @Inject(method = "onVaultAdd",at=@At("HEAD"),remap = false,cancellable = true)
    private void onTemplateConfigured(VirtualWorld world, Vault vault, ModifierContext context, CallbackInfo ci){
        TemplateConfigurationEvent.INSTANCE.register(vault,(data)->{
            if(data.getVault()==vault){
                data.getTemplateSettings().addProcessor(TileProcessor.of((tile, ctx) -> {
                    if (((TemplateProcessorModifierPropertiesAccessor)properties).getBlacklist().test(tile)) {
                        return tile;
                    } else if (ctx.getRandom(tile.getPos()).nextFloat() >= this.properties.getProbability()) {
                        return tile;
                    } else {
                        BlockState state = tile.getState().asWhole().orElse(null);
                        if (state == null) {
                            return tile;
                        } else {
                            List<TileProcessor> palette;
                            if (state.isCollisionShapeFullBlock(world, tile.getPos())) {
                                palette = ((TemplateProcessorModifierPropertiesAccessor)this.properties).getFullBlock();
                            } else {
                                palette = ((TemplateProcessorModifierPropertiesAccessor)this.properties).getPartialBlock();
                            }

                            for (TileProcessor processor : palette) {
                                tile = processor.process(tile, ctx);
                            }

                            return tile;
                        }
                    }
                }));
            }
        });
        ci.cancel();
    }
}
