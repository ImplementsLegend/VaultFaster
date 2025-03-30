package implementslegend.mod.vaultfaster.kotlinmixins.concurrency

import implementslegend.mod.vaultfaster.ConcurrentGridCache
import iskallia.vault.core.Version
import iskallia.vault.core.event.CommonEvents
import iskallia.vault.core.event.common.TemplateGenerationEvent
import iskallia.vault.core.random.ChunkRandom
import iskallia.vault.core.util.RegionPos
import iskallia.vault.core.vault.Vault
import iskallia.vault.core.world.generator.GridGenerator
import iskallia.vault.core.world.generator.VaultGenerator
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.ServerLevelAccessor

class KMixinConcurrentGridGenerator(val thiz:VaultGenerator) {

    private val concurrentCache = ConcurrentGridCache()

    fun generate(vault: Vault, world: ServerLevelAccessor?, chunkPos: ChunkPos) {
        val min = BlockPos(chunkPos.x * 16, Int.MIN_VALUE, chunkPos.z * 16)
        val max = BlockPos(chunkPos.x * 16 + 15, Int.MAX_VALUE, chunkPos.z * 16 + 15)

        val cellX = thiz.get(GridGenerator.CELL_X)
        val cellZ = thiz.get(GridGenerator.CELL_Z)
        val versionFlag = vault.get(Vault.VERSION).isOlderThan(Version.v1_7)
        val layout = thiz.get(GridGenerator.LAYOUT)
        val seed = vault.get(Vault.SEED)
        for(x in (min.x - Math.floorMod(min.x, cellX))..max.x step cellX){
            for(z in (min.z - Math.floorMod(min.z, cellZ))..max.z step cellZ){
                val region = RegionPos.ofBlockPos(BlockPos(x, 0, z), cellX, cellZ)
                val random = ChunkRandom.any()
                if (versionFlag) random.setCarverSeed(seed, region.x, region.z) else random.setRegionSeed(seed, region.x, region.z, 1234567890L)

                concurrentCache.getAt(region, layout, vault, random)?.let {
                    val template = CommonEvents.TEMPLATE_GENERATION.invoke(world, it, region, chunkPos, random, TemplateGenerationEvent.Phase.PRE).template
                    template.place(world, chunkPos)
                    CommonEvents.TEMPLATE_GENERATION.invoke(world, template, region, chunkPos, random, TemplateGenerationEvent.Phase.POST)
                }
            }


        }
    }
}