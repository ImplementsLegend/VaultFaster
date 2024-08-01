package implementslegend.mod.vaultfaster

import iskallia.vault.init.ModBlocks
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraftforge.registries.ForgeRegistries
import java.util.*

@Deprecated(level = DeprecationLevel.ERROR, message = "this shouldn't be needed anymore")
object TileMapperBlacklist {
    private val spawnerBlock by lazy {
        ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryParse("ispawner:spawner"))
    }
    private val chest by lazy {
        ModBlocks.PLACEHOLDER
    }


    //found reason why all these blocks were broken so this is not in use anymore
    val blacklist by lazy {
        BitSet(BLOCKS.size()).apply {
            val spawner = spawnerBlock?.let { arrayOf(it) }?: arrayOf()
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

    fun isBlacklisted(block:Int):Boolean = false//(block == ((spawnerBlock as IndexedBlock?)?.registryIndex ?: Int.MAX_VALUE)) or (block == (chest as IndexedBlock).registryIndex) //if(block<0)false else blacklist.get(block)

}