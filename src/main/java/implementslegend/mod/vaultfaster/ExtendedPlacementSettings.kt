package implementslegend.mod.vaultfaster

import iskallia.vault.core.world.processor.tile.TileProcessor

interface ExtendedPlacementSettings:TileMapperContainer {
    fun addProcessorAtBegining(tileProcessor: TileProcessor)

    val unmappedProcessors:MutableList<TileProcessor>
}