package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.batchsetblocks.BatchSetBlockKt;
import implementslegend.mod.vaultfaster.interfaces.StreamedTemplate;
import iskallia.vault.core.world.data.tile.PartialTile;
import iskallia.vault.core.world.data.tile.TilePredicate;
import iskallia.vault.core.world.template.PlacementSettings;
import iskallia.vault.core.world.template.Template;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;


/*
* applies fast batchSetBlocks algorithm for faster placement of blocks
* */
@Mixin(Template.class)
public abstract class MixinTemplate {

    private static Constructor<?> resultConstructor = getResultConstructor();

    private static Constructor<?> getResultConstructor() {
        var result = Arrays.stream(Template.class.getDeclaredClasses()).filter((it)->it.getName().endsWith("TilePlacementResult")).toList().get(0).getConstructors()[0];
        result.trySetAccessible();
        return result;
    }

    @Shadow public abstract Iterator<PartialTile> getTiles(PlacementSettings settings);


    @Shadow @Final public static TilePredicate ALL_TILES;

    @Coerce
    @Inject(method = "placeTiles",at = @At(value = "INVOKE", target = "Liskallia/vault/core/world/template/Template;getTiles(Liskallia/vault/core/world/template/PlacementSettings;)Ljava/util/Iterator;"),cancellable = true, remap = false)
    private void fastPlaceTiles(ServerLevelAccessor world, PlacementSettings settings, CallbackInfoReturnable cir) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Object result = resultConstructor.newInstance(/*private classes are hard to access*/
                new ArrayList(1024), new ArrayList(settings.doKeepFluids() ? 4096 : 0), new ArrayList(settings.doKeepFluids() ? 4096 : 0)
        );
        var tiles = ((StreamedTemplate)this).getTileStream(ALL_TILES, settings);
        BatchSetBlockKt.placeTiles(world,tiles, result);

        cir.setReturnValue(result);
    }

}
