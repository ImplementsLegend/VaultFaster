package implementslegend.mod.vaultfaster

import iskallia.vault.init.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.core.BlockPos.MutableBlockPos
import net.minecraft.world.level.block.state.BlockState

/*
* sequence for filling entire section with bedrock
* */

fun blocksToFill(offset:BlockPos):Iterable<Pair<BlockPos,BlockState>> {

    val mutBlockPos = MutableBlockPos()
    mutBlockPos.set(offset)
    mutBlockPos.x--
    mutBlockPos.y+=15
    val state = ModBlocks.VAULT_BEDROCK.defaultBlockState()
    return generateSequence<Pair<BlockPos,BlockState>>{
        mutBlockPos.x-=offset.x
        mutBlockPos.y-=offset.y
        mutBlockPos.z-=offset.z
        mutBlockPos.x++
        while(mutBlockPos.x>15) {
            mutBlockPos.x%=16
            mutBlockPos.z++
            while(mutBlockPos.z>15) {
                mutBlockPos.z%=16
                mutBlockPos.y--
                if(mutBlockPos.y<0) return@generateSequence null
            }
        }
        mutBlockPos.x+=offset.x
        mutBlockPos.y+=offset.y
        mutBlockPos.z+=offset.z
        (mutBlockPos to state)


    }.asIterable()

}