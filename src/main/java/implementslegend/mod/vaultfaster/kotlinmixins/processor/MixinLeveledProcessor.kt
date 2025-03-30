package implementslegend.mod.vaultfaster.kotlinmixins.processor

import implementslegend.mod.vaultfaster.CachedValue
import implementslegend.mod.vaultfaster.cacheLoadOrUpdate
import iskallia.vault.core.vault.Vault
import iskallia.vault.core.vault.VaultLevel
import iskallia.vault.core.world.data.tile.PartialTile
import iskallia.vault.core.world.processor.ProcessorContext
import iskallia.vault.core.world.processor.tile.TileProcessor
import iskallia.vault.init.ModConfigs
import java.lang.ref.PhantomReference

class KMixinLeveledProcessor(val levels: () -> Map<Int, TileProcessor>) {

    val table by lazy {
        val map = levels().toSortedMap()
        Array(ModConfigs.LEVELS_META.maxLevel + 1){map.headMap(it).run { try{get(lastKey())}catch(e:java.util.NoSuchElementException){null} }}
    }

    val cached = CachedValue(-1 to PhantomReference<Vault>(null,null))

    fun cachedLevel(context: ProcessorContext): Int =
        cached.cacheLoadOrUpdate({
            it.first>0 && it.second.refersTo(context.vault)
        },{
            (if (context.vault === null) -1 else (context.vault.get(Vault.LEVEL) as VaultLevel).get())to PhantomReference(context.vault,null)
        }).first

    fun process(tile: PartialTile?, context: ProcessorContext): PartialTile? {
        var level: Int = cachedLevel(context)

        if (level < 0) level = 0
        if (level >= table.size) level = table.size - 1

        return table.getOrNull(level)?.process(tile, context)?:tile
    }
}