package implementslegend.mod.vaultfaster.interfaces

import implementslegend.mod.vaultfaster.TileMapper

/*
* for flattening purposes
* */
interface TileMapperContainer {
    val tileMapper: TileMapper
    fun resetTileMapper()
}