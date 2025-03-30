package implementslegend.mod.vaultfaster.batchsetblocks

import implementslegend.mod.vaultfaster.GENERATOR_EXECUTOR
import implementslegend.mod.vaultfaster.mixin.accessors.ProtoChunkAccessor
import iskallia.vault.VaultMod
import iskallia.vault.core.world.data.tile.PartialTile
import iskallia.vault.init.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.WorldGenRegion
import net.minecraft.world.Clearable
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.CommandBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.ProtoChunk
import java.util.ArrayList
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Collector
import java.util.stream.Stream



infix fun Int.rangeUntilWidth(i: Int): IntRange = this until (this+i)

private val delayExecutor = try {
    CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS, GENERATOR_EXECUTOR)
} catch (e:Throwable){
    e.printStackTrace()
    throw Exception(e)
}

private typealias CollectionResult = Pair<ArrayList<Pair<BlockPos, BlockState>>, ArrayList<PartialTile>>

fun LevelAccessor.placeTiles(blocks_: Stream<PartialTile>, result_: Any?) {
    val collector = Collector.of<PartialTile, CollectionResult>(
        {
            ArrayList<Pair<BlockPos, BlockState>>(256) to ArrayList(256)
        },
        { collected, tile ->
            val (states, tiles) = collected
            val state = tile.state
                .asWhole()
                .orElseGet {
                    //VaultMod.LOGGER.error("Could not resolve tile '$tile' at (${tile.pos.x}, ${tile.pos.y}, ${tile.pos.z})")
                    ModBlocks.ERROR_BLOCK.defaultBlockState()
                }
            val entityTag = tile.entity.asWhole().filter { state.hasBlockEntity() && !it.isEmpty }
            states += tile.pos to state
            entityTag.ifPresent { tiles.add(tile) }
        },
        { collected1, collected2 ->
            val (states1, tiles1) = collected1
            val (states2, tiles2) = collected2
            states1.apply { addAll(states2) } to tiles1.apply { addAll(tiles2) }
        }
    )
    val result = result_?.let { TileResult(it) }
    val (states, tiles) = blocks_.filter { it !== null }.collect(collector)
    states
        .groupBy { SectionPos.of(it.first) }
        .entries.map { (sectionPos, pairs) ->
            val chunk = getChunk(sectionPos.x, sectionPos.z)
            val task = when (chunk) {
                is LevelChunk -> BlocksToLevelSectionTask(chunk)
                is ProtoChunk -> BlocksToProtoSectionTask(chunk)
                else -> null
            }
            chunk to (task?.setBlocks((sectionPos.y - (chunk.minBuildHeight shr 4)), pairs) ?: emptyList())
        }.also { data ->
            when(this) {
                is WorldGenRegion -> data
                    .forEach { (chunk, posList) ->
                        synchronized<Unit>(chunk) {
                            (chunk as? ProtoChunkAccessor)?.lights?.addAll(posList)
                        }
                    }
                is ServerLevel -> data
                    .map { it.first.pos }
                    .distinct()
                    .forEach { pos ->
                        delayExecutor.execute { //no delay might be enough on slow computers
                            players().forEach { player ->
                                val lc = getChunk(pos.x, pos.z) as LevelChunk
                                synchronized(lc) {
                                    (player as ServerPlayer).connection.send(
                                        ClientboundLevelChunkWithLightPacket(
                                            lc,
                                            lightEngine,
                                            null,
                                            null,
                                            false
                                        )
                                    )
                                }
                            }
                        }
                    }
            }
        }
    tiles.forEach { partial ->
        val pos = partial.pos
        val entityTag = partial.entity.asWhole()
        try {
            val blockEntity = getBlockEntity(pos)
            entityTag.ifPresent {
                Clearable.tryClear(blockEntity)
            }

            blockEntity?.load(entityTag.get())
            if (blockEntity is CommandBlockEntity) {
                scheduleTick(partial.pos, Blocks.COMMAND_BLOCK, 1)
            }
            result?.placedBlockEntities?.add(partial)

        } catch (e: Exception) {
            VaultMod.LOGGER.error("Failed to setup tile entity '$partial' at (${partial.pos.x}, ${partial.pos.y}, ${partial.pos.z})")
            e.printStackTrace()
            setBlock(partial.pos, ModBlocks.ERROR_BLOCK.defaultBlockState(), 0)
        }
    }
}
