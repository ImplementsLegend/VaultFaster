package implementslegend.mod.vaultfaster.batchsetblocks

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.chunk.ProtoChunk
import net.minecraft.world.level.levelgen.Heightmap
import java.util.*

class BlocksToProtoSectionTask(chunk: ProtoChunk) : BlocksToSectionTask<ProtoChunk>(chunk){

    override fun typeSpecificStuff(
        skipHeightmaps: Boolean,
        newState: BlockState,
        oldState: BlockState,
        position: BlockPos,
        section: LevelChunkSection,
        tasks:MutableList<()->Unit>
    ) {


        if (newState.hasBlockEntity()) {
            val compoundtag = CompoundTag()
            compoundtag.putInt("x", position.x)
            compoundtag.putInt("y", position.y)
            compoundtag.putInt("z", position.z)
            compoundtag.putString("id", "DUMMY")
            chunk.setBlockEntityNbt(compoundtag)
        }

        if(!skipHeightmaps) {

            val enumset = chunk.status.heightmapsAfter()
            var enumset1: EnumSet<Heightmap.Types>? = null

            enumset.forEach { heightmapType ->
                val heightmap = chunk.heightmaps.singleOrNull { it.key == heightmapType }
                if (heightmap == null) {
                    val es = enumset1 ?: EnumSet.noneOf(Heightmap.Types::class.java).also { enumset1 = it }
                    es.add(heightmapType)
                }
            }
            enumset1?.let { Heightmap.primeHeightmaps(chunk, it) }

            enumset.forEach { heightmapType -> chunk.heightmaps.singleOrNull { it.key == heightmapType }?.value?.update(position.x and 15, position.y, position.z and 15, oldState) }
        }
    }
}