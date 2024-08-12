package implementslegend.mod.vaultfaster.batchsetblocks

import iskallia.vault.core.world.data.tile.PartialTile
import iskallia.vault.core.world.template.Template
import net.minecraft.core.BlockPos
import java.util.*

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