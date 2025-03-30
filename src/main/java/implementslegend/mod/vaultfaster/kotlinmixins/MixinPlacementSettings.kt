package implementslegend.mod.vaultfaster.kotlinmixins

import implementslegend.mod.vaultfaster.TileMapper
import implementslegend.mod.vaultfaster.interfaces.TileMapperContainer
import iskallia.vault.core.world.processor.tile.TileProcessor
import iskallia.vault.core.world.template.PlacementSettings
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean


class KMixinPlacementSettings(val tileProcessors: ()->MutableList<TileProcessor>) {

    var futureTileMapper: CompletableFuture<TileMapper> = CompletableFuture()
    private val tileMapperCreationStarted = AtomicBoolean()
    var hasTileMapperCreationStarted:Boolean
        get() = tileMapperCreationStarted.plain || tileMapperCreationStarted.get()
        set(value) {tileMapperCreationStarted.set(value)}

    fun getTileMapper(): TileMapper =
        if (!tileMapperCreationStarted.plain && tileMapperCreationStarted.compareAndSet(false, true))
            TileMapper().apply { tileProcessors().forEach(::addProcessor) }.also(futureTileMapper::complete)
        else try { futureTileMapper.get()!! } catch (e: InterruptedException) { throw RuntimeException(e) } catch (e: ExecutionException) { throw RuntimeException(e) }

    fun resetTileMapper() {
        if (hasTileMapperCreationStarted) {
            try { futureTileMapper.get() } catch (e: InterruptedException) { throw RuntimeException(e) } catch (e: ExecutionException) { throw RuntimeException(e) }

            futureTileMapper = CompletableFuture()
            hasTileMapperCreationStarted = false
        }
    }

    fun copyMapper(new: PlacementSettings) {
        if (hasTileMapperCreationStarted) {
            (new as TileMapperContainer).apply {
                futureTileMapper.complete(getTileMapper())
                hasTileMapperCreationStarted = true
            }
        }
    }

    fun addProcessorAtBegining(tileProcessor: TileProcessor) {
        resetTileMapper()
        tileProcessors().add(0, tileProcessor)
    }

}