package implementslegend.mod.vaultfaster.batchsetblocks

import implementslegend.mod.vaultfaster.mixin.accessors.ChunkAccessAccessor
import implementslegend.mod.vaultfaster.mixin.accessors.LevelChunkAccessor
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.levelgen.Heightmap

class BlocksToLevelSectionTask(chunk: LevelChunk) : BlocksToSectionTask<LevelChunk>(chunk) {

    override fun typeSpecificStuff(
        skipHeightmaps: Boolean,
        newState: BlockState,
        oldState: BlockState,
        position: BlockPos,
        section: LevelChunkSection,
        tasks:MutableList<()->Unit>
    ) {

        if (newState.hasBlockEntity()) {
            val blockentity = (newState.getBlock() as EntityBlock).newBlockEntity(position, newState)
            if (blockentity != null) {
                chunk.setBlockEntity(blockentity)
            } else {
                chunk.removeBlockEntity(position)
            }
        }
        val oldLight = oldState.getLightEmission(chunk, position)
        val oldOpacity = oldState.getLightBlock(chunk, position)
        val inChunkX = position.x and 15
        val inChunkZ = position.z and 15
        val inChunkY = position.y and 15
        val block: Block = newState.block
        if (!skipHeightmaps) {
            chunk.heightmaps.singleOrNull { it.key == Heightmap.Types.MOTION_BLOCKING }?.value?.update(
                inChunkX,
                position.y,
                inChunkZ,
                newState
            )
            chunk.heightmaps.singleOrNull { it.key == Heightmap.Types.MOTION_BLOCKING_NO_LEAVES }?.value?.update(
                inChunkX,
                position.y,
                inChunkZ,
                newState
            )
            chunk.heightmaps.singleOrNull { it.key == Heightmap.Types.OCEAN_FLOOR }?.value?.update(
                inChunkX,
                position.y,
                inChunkZ,
                newState
            )
            chunk.heightmaps.singleOrNull { it.key == Heightmap.Types.WORLD_SURFACE }?.value?.update(
                inChunkX,
                position.y,
                inChunkZ,
                newState
            )
        }
        val flag2: Boolean = oldState.hasBlockEntity()
        if (!chunk.level.isClientSide) {
            oldState.onRemove(chunk.level, position, newState, false)
        } else if ((!oldState.`is`(block) || !newState.hasBlockEntity()) && flag2) {
            chunk.removeBlockEntity(position)
        }


        tasks += {
            val blockstate1 = chunk.getBlockState(position)
            if (blockstate1 !== oldState && (blockstate1.getLightBlock(
                    chunk,
                    position
                ) != oldOpacity || blockstate1.getLightEmission(
                    chunk,
                    position
                ) != oldLight || blockstate1.useShapeForLightOcclusion() || oldState.useShapeForLightOcclusion())
            ) {
                chunk.level.profiler.push("queueCheckLight")
                chunk.level.chunkSource.lightEngine.checkBlock(position)
                chunk.level.profiler.pop()
            }
            if (section.getBlockState(inChunkX, inChunkY, inChunkZ).`is`(block)) {
                /*if (!level.isClientSide && !level.captureBlockSnapshots) {
                    newState.onPlace(level, position, blockstate, false)
                }*/
                if (newState.hasBlockEntity()) {
                    var blockentity = chunk.getBlockEntity(position, LevelChunk.EntityCreationType.CHECK)
                    if (blockentity == null) {
                        blockentity = (block as EntityBlock).newBlockEntity(position, newState)
                        if (blockentity != null) {
                            chunk.addAndRegisterBlockEntity(blockentity)
                        }
                    } else {
                        blockentity.blockState = newState
                        (this.chunk as LevelChunkAccessor).callUpdateBlockEntityTicker(blockentity)
                    }
                }
                (this.chunk as ChunkAccessAccessor).setUnsaved(true)
            }
        }
    }
}