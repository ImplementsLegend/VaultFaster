package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.FixatedBlockIDsKt;
import implementslegend.mod.vaultfaster.interfaces.IndexedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import org.spongepowered.asm.mixin.Mixin;

/*
* adds registry indices to all blocks
* */

@Mixin(BlockBehaviour.class)
public class MixinBlockRegistryIndex implements IndexedBlock {

    private int registryIndex = -1;
    @Override
    public int getRegistryIndex() {
        /*
        if(registryIndex<0){
            registryIndex= ((ForgeRegistry)ForgeRegistries.BLOCKS).getID((BlockBehaviour) (Object)this);
        }
        return registryIndex;
        */
        return FixatedBlockIDsKt.getIdForBlock((Block)(Object) this);
    }

    @Override
    public void copyRegistryIndex(int newIndex) {}
}
