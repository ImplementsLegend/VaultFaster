package implementslegend.mod.vaultfaster

import implementslegend.mod.vaultfaster.mixin.ChunkAccessAccessor
import implementslegend.mod.vaultfaster.mixin.LevelChunkAccessor
import implementslegend.mod.vaultfaster.mixin.ProtoChunkAccessor
import iskallia.vault.VaultMod
import iskallia.vault.core.world.data.tile.PartialTile
import iskallia.vault.core.world.template.PlacementSettings
import iskallia.vault.core.world.template.Template
import iskallia.vault.init.ModBlocks
import net.minecraft.Util
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Clearable
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.CommandBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.ChunkStatus
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.ProtoChunk
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.lighting.LevelLightEngine
import net.minecraftforge.fml.loading.FMLEnvironment
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import java.util.stream.StreamSupport
import kotlin.collections.LinkedHashSet
import kotlin.streams.toList


private infix fun Int.rangeUntilWidth(i: Int): IntRange = this until (this+i)

@JvmInline
value class TileResult(val result:Any){
    val placedBlockEntities get()= placedBlockEntitiesGetter(result) as MutableList<PartialTile>
    val flowingPositions get()= flowingPositionsGetter(result) as MutableList<BlockPos>
    val sourcePositions get()= sourcePositionsGetter(result) as MutableList<BlockPos>

    private companion object{

        private var clazz = Arrays.stream(Template::class.java.declaredClasses).filter { it: Class<*> -> it.name.endsWith("TilePlacementResult") }.toList()[0]
        val placedBlockEntitiesGetter = clazz.getField("placedBlockEntities")::get
        val flowingPositionsGetter = clazz.getField("flowingPositions")::get
        val sourcePositionsGetter = clazz.getField("sourcePositions")::get
    }
}


fun LevelAccessor.placeTiles(blocks: Iterator<PartialTile>, settings: PlacementSettings, result_:Any){
    val result = TileResult(result_)
    val nl = ArrayList<Pair<BlockPos,BlockState>>(4096)
    val tiles = ArrayList<Pair<BlockPos,CompoundTag>>(4096)
    blocks.forEachRemaining{
            tile->
        val state = tile.state
            .asWhole()
            .orElseGet {
                if (FMLEnvironment.production) {
                    VaultMod.LOGGER.error("Could not resolve tile '$tile' at (${tile.pos.x}, ${tile.pos.y}, ${tile.pos.z})")
                }
                ModBlocks.ERROR_BLOCK.defaultBlockState()
            }
        if (tile.entity.asWhole().isPresent) {
            val blockentity = getBlockEntity(tile.pos)
            Clearable.tryClear(blockentity)
        }
        nl+=tile.pos to state
        tile.entity.asWhole().ifPresent{
            result.placedBlockEntities.add(tile)
            tiles+=tile.pos to it
        }
    }
    setBlocks(nl)
    tiles.forEach {
            (pos,tile)->
        try {
            val blockEntity = getBlockEntity(pos)

            blockEntity?.load(tile)
            if (blockEntity is CommandBlockEntity) {
                scheduleTick(pos, Blocks.COMMAND_BLOCK, 1)
            }

        } catch (e:Exception){setBlock(pos,ModBlocks.ERROR_BLOCK.defaultBlockState(),0)}
    }
}


fun LevelAccessor.placeTiles(blocks_: Stream<PartialTile>, settings: PlacementSettings, result_:Any){
    val result = TileResult(result_)
    val (nl,tiles) = blocks_.collect({
        ArrayList<Pair<BlockPos,BlockState>>(256) to ArrayList<Pair<PartialTile,CompoundTag>>(256)
    },{
        (nl,tiles),tile->
        val state = tile.state
            .asWhole()
            .orElseGet {
                if (FMLEnvironment.production) {
                    VaultMod.LOGGER.error("Could not resolve tile '$tile' at (${tile.pos.x}, ${tile.pos.y}, ${tile.pos.z})")
                }
                ModBlocks.ERROR_BLOCK.defaultBlockState()
            }
        if (tile.entity.asWhole().isPresent) {
            val blockentity = getBlockEntity(tile.pos)
            Clearable.tryClear(blockentity)
        }
        nl+=tile.pos to state
        tile.entity.asWhole().ifPresent{
            tiles+=tile to it
        }
    },{(nl1,tiles1),(nl2,tiles2)->nl1.apply { addAll(nl2) } to tiles1.apply { addAll(tiles2) }})
    setBlocks(nl)
    tiles.forEach {
        (partial,data)->
        try {
            val blockEntity = getBlockEntity(partial.pos)

            blockEntity?.load(data)
            if (blockEntity is CommandBlockEntity) {
                scheduleTick(partial.pos, Blocks.COMMAND_BLOCK, 1)
            }
            result.placedBlockEntities.add(partial)

        } catch (e:Exception){setBlock(partial.pos,ModBlocks.ERROR_BLOCK.defaultBlockState(),0)}
    }
}

private val delayExecutor = CompletableFuture.delayedExecutor(500,TimeUnit.MILLISECONDS,Util.backgroundExecutor())

fun LevelAccessor.setBlocks(blocks: List<Pair<BlockPos, BlockState>>){
    val positions = LinkedHashSet<ChunkPos>()
    blocks.groupBy { SectionPos.of(it.first) }.forEach { (sectionPos, pairs) ->
        val chunk = getChunk(sectionPos.x, sectionPos.z)
        positions+=chunk.pos
        chunk.setBlocks((sectionPos.y-(chunk.minBuildHeight shr 4)),pairs)
    }
    if (this is ServerLevel) { //stuff was not getting updated correctly
        positions.forEach { pos ->
            delayExecutor.execute { //no delay might be enough on slow computers
                this.players().forEach { player ->
                    val lc: LevelChunk = getChunk(pos.x, pos.z)
                    player.connection.send(ClientboundLevelChunkWithLightPacket(lc, lightEngine, null, null, false))
                }
            }
        }
    }
}


/*
* the actual "fast set blocks" algorithm;
* differences:
*   only acquires lock once per section
*   can skip heightmap updates
*   no "block set" events
*   but light updates seem to be a bit broken for LevelChunk
* */

fun ChunkAccess.setBlocks(sectionIdx: Int, blocks: Iterable<Pair<BlockPos, BlockState>>, skipHeightmaps:Boolean=true){

    val sectionHeightRange = minBuildHeight + 16 * sectionIdx rangeUntilWidth 16
    val sectionXRange = pos.x*16 rangeUntilWidth 16
    val sectionZRange = pos.z*16 rangeUntilWidth 16
    if(sectionIdx>4)
        Unit
    val section = getSection(sectionIdx)
    val tasks = ArrayList<()->Unit>(4096)
    try {

        section.acquire()
        blocks.forEach { (position, newState) ->
            if (position.x !in sectionXRange || position.z !in sectionZRange || position.y !in sectionHeightRange) return@forEach

            if (newState.getLightEmission(this, position) > 0 && this is ProtoChunk) (this as ProtoChunkAccessor).lights.add(BlockPos((position.x and 15) + sectionXRange.first, position.y, (position.z and 15) + sectionZRange.first))
            val j: Int = position.x and 15
            val k: Int = position.y and 15
            val l: Int = position.z and 15
            val old = if(this is LevelChunk)getBlockState(position) else null
            val oldLight = old?.getLightEmission(this, position)
            val oldOpacity = old?.getLightBlock(this, position)
            val blockstate: BlockState = section.setBlockState(j, k, l, newState,false)



            if (!newState.hasBlockEntity()) {
                if (blockstate.hasBlockEntity()) {
                    removeBlockEntity(position)
                }
            }
            (this as? ProtoChunk)?.apply {


                if (newState.hasBlockEntity()) {
                    val compoundtag = CompoundTag()
                    compoundtag.putInt("x", position.x)
                    compoundtag.putInt("y", position.y)
                    compoundtag.putInt("z", position.z)
                    compoundtag.putString("id", "DUMMY")
                    setBlockEntityNbt(compoundtag)
                }

                if (status.isOrAfter(ChunkStatus.FEATURES) && newState !== blockstate && (newState.getLightBlock(this, position) != blockstate.getLightBlock(this, position) || newState.getLightEmission(this, position) != blockstate.getLightEmission(this, position) || newState.useShapeForLightOcclusion() || blockstate.useShapeForLightOcclusion())) {
                    (this as ProtoChunkAccessor).lightEngine?.checkBlock(position)
                }
                if(!skipHeightmaps) {

                    val enumset = status.heightmapsAfter()
                    var enumset1: EnumSet<Heightmap.Types>? = null

                    enumset.forEach { heightmapType ->
                        val heightmap = heightmaps.singleOrNull { it.key == heightmapType }
                        if (heightmap == null) {
                            val es = enumset1 ?: EnumSet.noneOf(Heightmap.Types::class.java).also { enumset1 = it }
                            es.add(heightmapType)
                        }
                    }
                    enumset1?.let { Heightmap.primeHeightmaps(this, it) }

                    enumset.forEach { heightmapType -> heightmaps.singleOrNull { it.key == heightmapType }?.value?.update(j, position.y, l, blockstate) }
                }
            }
            (this as? LevelChunk)?.apply {

                if (newState.hasBlockEntity()) {
                    val blockentity = (newState.getBlock() as EntityBlock).newBlockEntity(position, newState)
                    if (blockentity != null) {
                        setBlockEntity(blockentity)
                    } else {
                        removeBlockEntity(position)
                    }
                }

                val block: Block = newState.block
                if (!skipHeightmaps) {
                    heightmaps.singleOrNull { it.key == Heightmap.Types.MOTION_BLOCKING }?.value?.update(j, position.y, l, newState)
                    heightmaps.singleOrNull { it.key == Heightmap.Types.MOTION_BLOCKING_NO_LEAVES }?.value?.update(j, position.y, l, newState)
                    heightmaps.singleOrNull { it.key == Heightmap.Types.OCEAN_FLOOR }?.value?.update(j, position.y, l, newState)
                    heightmaps.singleOrNull { it.key == Heightmap.Types.WORLD_SURFACE }?.value?.update(j, position.y, l, newState)
                }
                val flag1: Boolean = section.hasOnlyAir()
                if (/*flag != flag1*/ true) {
                }

                val flag2: Boolean = blockstate.hasBlockEntity()
                if (!level.isClientSide) {
                    blockstate.onRemove(level, position, newState, false)
                } else if ((!blockstate.`is`(block) || !newState.hasBlockEntity()) && flag2) {
                    removeBlockEntity(position)
                }


                tasks += {
                    val blockstate1 = this.getBlockState(position)
                    if (blockstate1 !== old && (blockstate1.getLightBlock(this, position) != oldOpacity || blockstate1.getLightEmission(this, position) != oldLight || blockstate1.useShapeForLightOcclusion() || blockstate.useShapeForLightOcclusion())) {
                        level.profiler.push("queueCheckLight")
                        level.chunkSource.lightEngine.checkBlock(position)
                        level.profiler.pop()
                    }
                    if (section.getBlockState(j, k, l).`is`(block)) {
                        if (!level.isClientSide && !level.captureBlockSnapshots) {
                            newState.onPlace(level, position, blockstate, false)
                        }
                        if (newState.hasBlockEntity()) {
                            var blockentity = this.getBlockEntity(position, LevelChunk.EntityCreationType.CHECK)
                            if (blockentity == null) {
                                blockentity = (block as EntityBlock).newBlockEntity(position, newState)
                                if (blockentity != null) {
                                    addAndRegisterBlockEntity(blockentity)
                                }
                            } else {
                                blockentity.blockState = newState

                                (this as LevelChunkAccessor).callUpdateBlockEntityTicker(blockentity)
                                //(this as LevelChunkAccessor).callUpdateBlockEntityTicker(blockentity)
                            }
                        }
                        (this as ChunkAccessAccessor).setUnsaved(true)
                        //(this as LevelChunkAccessor).unsaved = true
                    }
                }
            }
        }
    }finally {
        section.release()
        tasks.forEach{
            it()
        }
    }
}