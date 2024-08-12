package implementslegend.mod.vaultfaster.batchsetblocks

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.chunk.ProtoChunk
import java.util.*
sealed class BlocksToSectionTask<T:ChunkAccess>(val chunk:T) {
    fun setBlocks(sectionIdx: Int, blocks: Iterable<Pair<BlockPos, BlockState>>, skipHeightmaps:Boolean = true):List<BlockPos>{

        val result = ArrayList<BlockPos>()
        val sectionHeightRange = chunk.minBuildHeight + 16 * sectionIdx rangeUntilWidth 16
        val sectionXRange = chunk.pos.x*16 rangeUntilWidth 16
        val sectionZRange = chunk.pos.z*16 rangeUntilWidth 16
        if(sectionIdx>4)
            Unit
        val section = chunk.getSection(sectionIdx)
        val tasks = ArrayList<()->Unit>(if(chunk is LevelChunk)4096 else 0)
        try {
            section.states.acquire()
            blocks.forEach { (position, newState) ->
                if (
                    position.x !in sectionXRange ||
                    position.z !in sectionZRange ||
                    position.y !in sectionHeightRange
                ) return@forEach

                if (newState.getLightEmission(chunk, position) > 0 && chunk is ProtoChunk) {
                    result.add(
                        position
                    )
                }

                val blockstate: BlockState = section.setBlockState(
                    position.x and 15,
                    position.y and 15,
                    position.z and 15,
                    newState,
                    false
                )

                if (!newState.hasBlockEntity() && blockstate.hasBlockEntity()) {
                    chunk.removeBlockEntity(position)
                }
                typeSpecificStuff(skipHeightmaps,newState, blockstate, position,section,tasks)

            }
        }finally {
            section.release()
            tasks.forEach{
                it()
            }
        }
        return result
    }

    abstract fun typeSpecificStuff(
        skipHeightmaps: Boolean,
        newState: BlockState,
        oldState: BlockState,
        position:BlockPos,
        section:LevelChunkSection,
        tasks:MutableList<()->Unit>
    )
}

