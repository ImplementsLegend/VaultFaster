package implementslegend.mod.vaultfaster.event

import iskallia.vault.core.event.CommonEvents
import iskallia.vault.core.event.Event
import iskallia.vault.core.random.ChunkRandom
import iskallia.vault.core.util.RegionPos
import iskallia.vault.core.vault.Vault
import iskallia.vault.core.world.template.PlacementSettings

data class TemplateEventData(val templateSettings:PlacementSettings, val vault:Vault, val random:ChunkRandom, val pos:RegionPos)

object TemplateConfigurationEvent: Event<TemplateConfigurationEvent, TemplateEventData>() {
    init {
        CommonEvents.REGISTRY.add(this)
    }
    override fun createChild() = this
}