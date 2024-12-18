package implementslegend.mod.vaultfaster.interfaces

import implementslegend.mod.vaultfaster.TileMapper
import java.util.concurrent.CompletableFuture

/*
* for flattening purposes
* */
interface TileMapperContainer {
    val tileMapper: TileMapper
    val futureTileMapper: CompletableFuture<TileMapper>
    var hasTileMapperCreationStarted: Boolean
    fun resetTileMapper()
}