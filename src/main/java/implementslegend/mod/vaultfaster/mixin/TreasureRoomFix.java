package implementslegend.mod.vaultfaster.mixin;

import iskallia.vault.block.TreasureDoorBlock;
import iskallia.vault.block.entity.TreasureDoorTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TreasureDoorTileEntity.class)
public class TreasureRoomFix {

    @Inject(method = "tick",at = @At(value = "INVOKE", target = "Liskallia/vault/block/entity/TreasureDoorTileEntity;fillTunnel(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Liskallia/vault/block/entity/TreasureDoorTileEntity;)V"),cancellable = true,remap = false)
    private static void disableInvalidDoor(Level level, BlockPos pos, BlockState state, TreasureDoorTileEntity tile, CallbackInfo ci){
        if(state.getValue(TreasureDoorBlock.OPEN))ci.cancel();

    }

}
