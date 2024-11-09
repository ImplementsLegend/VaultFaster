package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.interfaces.TileMapperContainer;
import iskallia.vault.block.entity.challenge.elite.EliteAnimation;
import iskallia.vault.block.entity.challenge.raid.RaidAnimation;
import iskallia.vault.block.entity.challenge.xmark.XMarkAnimation;
import iskallia.vault.core.world.template.PlacementSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

@Mixin({RaidAnimation.class, XMarkAnimation.class, EliteAnimation.class})
public class FixRaidAnimation {

    @Coerce
    @Redirect(method = "place",at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V",ordinal = 1),remap = false)
    private void copyBeforeAccept(Consumer instance, Object t){
        ((TileMapperContainer)t).resetTileMapper();
        instance.accept(t);
    }


}
