package implementslegend.mod.vaultfaster.interfaces

import iskallia.vault.core.world.processor.entity.EntityProcessor
import iskallia.vault.core.world.processor.tile.TileProcessor

interface ExtendedPlacementSettings: TileMapperContainer {
    fun addProcessorAtBegining(tileProcessor: TileProcessor)

}