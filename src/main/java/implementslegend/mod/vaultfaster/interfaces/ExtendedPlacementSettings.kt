package implementslegend.mod.vaultfaster.interfaces

import iskallia.vault.core.world.processor.tile.TileProcessor

interface ExtendedPlacementSettings: TileMapperContainer {
    fun addProcessorAtBegining(tileProcessor: TileProcessor)

    val unmappedProcessors:MutableList<TileProcessor>
}