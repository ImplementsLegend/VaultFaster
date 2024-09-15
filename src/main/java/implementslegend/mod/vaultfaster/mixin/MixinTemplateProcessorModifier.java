package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.interfaces.SectionedTemplateContainer;
import iskallia.vault.core.event.common.TemplateGenerationEvent;
import iskallia.vault.core.vault.modifier.modifier.TemplateProcessorModifier;
import iskallia.vault.core.world.template.configured.ChunkedTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TemplateProcessorModifier.class)
public class MixinTemplateProcessorModifier {
    @Inject(method = "lambda$onVaultAdd$2",at=@At("HEAD"),remap = false,cancellable = true)
    private void stopMultipleAdditions(TemplateGenerationEvent.Data data, CallbackInfo ci){
        if(data.getTemplate() instanceof ChunkedTemplate ct && ((SectionedTemplateContainer)ct).getSectionedTemplate().getStarted().get())ci.cancel();
    }
}
