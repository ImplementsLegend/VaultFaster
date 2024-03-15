package implementslegend.mod.vaultfaster.mixin;


import implementslegend.mod.vaultfaster.IndexedBlock;
import iskallia.vault.core.world.data.tile.PartialBlock;
import iskallia.vault.core.world.data.tile.PartialBlockState;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
/*
* comparison using registry indices
* */
@Mixin(PartialBlockState.class)
public class MixinPartialBlockState {

    @Shadow private PartialBlock block;


    @Overwrite(remap = false)
    public boolean is(Block b){
        return ((IndexedBlock)b).getRegistryIndex() == ((IndexedBlock)block).getRegistryIndex();
    }
}
