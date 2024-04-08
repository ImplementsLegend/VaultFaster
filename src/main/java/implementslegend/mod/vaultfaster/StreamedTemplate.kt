package implementslegend.mod.vaultfaster

import iskallia.vault.core.world.data.tile.PartialTile
import iskallia.vault.core.world.data.tile.TilePredicate
import iskallia.vault.core.world.template.PlacementSettings
import java.util.stream.Stream

interface StreamedTemplate {

    fun getTileStream(filter: TilePredicate, settings: PlacementSettings):Stream<PartialTile>
}