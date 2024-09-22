package implementslegend.mod.vaultfaster.mixin;

import com.google.common.collect.Lists;
import implementslegend.mod.vaultfaster.interfaces.ExtendedPlacementSettings;
import iskallia.vault.core.Version;
import iskallia.vault.core.data.key.PaletteKey;
import iskallia.vault.core.world.processor.entity.EntityProcessor;
import iskallia.vault.core.world.processor.tile.TileProcessor;
import iskallia.vault.core.world.template.JigsawTemplate;
import iskallia.vault.core.world.template.PlacementSettings;
import iskallia.vault.core.world.template.data.TemplateEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/*
* Correctly modifies PlacementSettings
*
* Original could crash the game
*
* */

@Mixin(JigsawTemplate.class)
public class MixinJigsawCorrectAddProcessors {

    @Inject(method = "lambda$computeChildren$3",at = @At(value = "HEAD"),cancellable = true)
    private static  void addCorrectly(Rotation rotation, BlockPos target, BlockPos offset, TemplateEntry entry, Version version, PlacementSettings settings, CallbackInfo ci){

        for(PaletteKey palette : Lists.reverse(Lists.newArrayList(entry.getPalettes()))) {
            for(TileProcessor tileProcessor :Lists.reverse( (palette.get(version)).getTileProcessors())) {
                ((ExtendedPlacementSettings)settings).addProcessorAtBegining(tileProcessor);
            }

            for(EntityProcessor var15x : Lists.reverse((palette.get(version)).getEntityProcessors())) {
                settings.getEntityProcessors().add(0, var15x);
            }
        }
        ((ExtendedPlacementSettings)settings).addProcessorAtBegining(TileProcessor.ofJigsaw());
        ((ExtendedPlacementSettings)settings).addProcessorAtBegining( TileProcessor.translate(offset));
        ((ExtendedPlacementSettings)settings).addProcessorAtBegining( TileProcessor.rotate(rotation, target, true));
        settings.getEntityProcessors().add(0, EntityProcessor.translate(offset));
        settings.getEntityProcessors().add(0, EntityProcessor.rotate(rotation, target, true));

        ci.cancel();

    }
}
