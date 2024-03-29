package implementslegend.mod.vaultfaster

import iskallia.vault.init.ModBlocks
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraftforge.registries.ForgeRegistries
import java.util.*

object TileMapperBlacklist {

    val blacklist by lazy {
        val spawner = ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryParse("ispawner:spawner"))?.let { arrayOf(it) }?: arrayOf()


        BitSet(BLOCKS.size()).apply {
            val blocks = arrayOf<BlockBehaviour>(
                *spawner,
                ModBlocks.GOD_ALTAR,
                Blocks.WHITE_CONCRETE,
                Blocks.LIME_GLAZED_TERRACOTTA,
                Blocks.GRAY_GLAZED_TERRACOTTA,
                Blocks.COMMAND_BLOCK,
                ModBlocks.PLACEHOLDER,
                )
            blocks.forEach {
                set((it as IndexedBlock).registryIndex)
            }
        }
    }

    fun isBlacklisted(block:Int):Boolean = if(block<0)false else blacklist.get(block)

}