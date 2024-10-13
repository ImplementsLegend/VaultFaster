package implementslegend.mod.vaultfaster

import implementslegend.mod.vaultfaster.interfaces.IndexedBlock
import net.minecraft.core.IdMapper
import net.minecraft.world.level.block.Block
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.ForgeRegistry

//forge bad

/*
    external block <-> integer map
    forge registries use pretty slow way of mapping blocks to numbers
    also their mappings may change within single game instance
 */

val BLOCKS by lazy {
    val data = (ForgeRegistries.BLOCKS as ForgeRegistry<Block>).values

    val result = IdMapper<Block>(data.count()/*data.maxOf { (it as IndexedBlock).registryIndex }+1*/)
    data.forEachIndexed { index, it ->
        result.addMapping(it,index)
    }
    result
}

fun getBlockByIDOrNull(id:Int):Block? = BLOCKS.byId(id)
fun getIdForBlock(block:Block?) = block?.let { BLOCKS.getId(it) }?:-1