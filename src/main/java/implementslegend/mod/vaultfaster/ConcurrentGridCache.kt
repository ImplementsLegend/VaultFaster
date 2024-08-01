package implementslegend.mod.vaultfaster

import iskallia.vault.core.random.ChunkRandom
import iskallia.vault.core.util.RegionPos
import iskallia.vault.core.vault.Vault
import iskallia.vault.core.world.generator.GridGenerator
import iskallia.vault.core.world.generator.layout.GridLayout
import iskallia.vault.core.world.processor.ProcessorContext
import iskallia.vault.core.world.template.PlacementSettings
import iskallia.vault.core.world.template.Template
import iskallia.vault.core.world.template.configured.ChunkedTemplate
import iskallia.vault.core.world.template.configured.ConfiguredTemplate
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

class ConcurrentGridCache {

    val map = ConcurrentHashMap<RegionPos, WeakReference<ConfiguredTemplate>>()

    fun getAt(
        pos: RegionPos,
        layout: GridLayout,
        vault: Vault,
        random: ChunkRandom ): ConfiguredTemplate {
        var newValue = null as ConfiguredTemplate?
        map.compute(pos) { pos,currentWeak ->
            if(currentWeak==null || currentWeak.get().also { newValue=it }===null) {
                val settings = PlacementSettings(ProcessorContext(vault, random)).setFlags(3)
                WeakReference(layout.getAt(vault, pos, random, settings)
                    .configure(
                        { parent: Template?, settings: PlacementSettings? ->
                            ChunkedTemplate(
                                parent,
                                settings
                            )
                        }, settings
                    ).also { newValue = it })
            } else currentWeak
        }
        return newValue!!

    }
}