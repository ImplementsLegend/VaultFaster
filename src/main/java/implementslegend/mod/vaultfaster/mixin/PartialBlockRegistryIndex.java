package implementslegend.mod.vaultfaster.mixin;

import implementslegend.mod.vaultfaster.BlocksKt;
import implementslegend.mod.vaultfaster.IndexedBlock;
import iskallia.vault.core.world.data.tile.PartialBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;


/*
* replaces resource locations with registry indicies for faster comparisons
* */

@Mixin(PartialBlock.class)
public class PartialBlockRegistryIndex implements IndexedBlock {
    @Shadow protected ResourceLocation id;
    private int regIdx = -69420;

    @Override
    public int getRegistryIndex(){
        if(regIdx==-69420){
            regIdx= BlocksKt.getIdForBlock(((ForgeRegistry<Block>) ForgeRegistries.BLOCKS).getValue(this.id));
            if(regIdx==0 ) {
                var resName = ((PartialBlockIDAccessor)this).getId();
                if(!resName.getNamespace().equals("minecraft") && !resName.getPath().equals("air")) regIdx=-666;
            }
        }
        return regIdx;
    }
    @Override
    public void copyRegistryIndex(int newIndex){
        regIdx=newIndex;
    }



    @Overwrite(remap = false)
    public static PartialBlock of(Block block) {
        var partial = PartialBlock.of(block.getRegistryName());
        ((IndexedBlock)partial).copyRegistryIndex(((IndexedBlock)block).getRegistryIndex());
        return partial;
    }



    @Overwrite(remap = false)
    public static PartialBlock of(BlockState state) {
        return PartialBlock.of(state.getBlock());
    }

    @Overwrite(remap = false)
    public boolean isSubsetOf(PartialBlock other) {
        return this.id == null || (regIdx<0?this.id.equals(((PartialBlockIDAccessor)other).getId()):((IndexedBlock)other).getRegistryIndex()==getRegistryIndex());
    }

    @Overwrite(remap = false)
    public Optional<Block> asWhole() {
        return Optional.ofNullable(BlocksKt.getBlockByIDOrNull(getRegistryIndex()));
    }


    @Overwrite(remap = false)
    public PartialBlock copy() {
        var pb = PartialBlock.of(this.id);
        ((IndexedBlock)pb).copyRegistryIndex(getRegistryIndex());
        return pb;
    }


    @Inject(method = "fillInto(Liskallia/vault/core/world/data/tile/PartialBlock;)V",at = @At("HEAD"), remap = false)
    public void fillInto(PartialBlock other, CallbackInfo ci) {
        if (this.id != null) {
            ((IndexedBlock)other).copyRegistryIndex(getRegistryIndex());
        }
    }
}
