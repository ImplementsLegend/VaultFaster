package implementslegend.mod.vaultfaster.event

import implementslegend.mod.vaultfaster.mixin.JigsawRootAccessor
import iskallia.vault.block.HeraldControllerBlock
import iskallia.vault.block.ObeliskBlock
import iskallia.vault.core.event.CommonEvents
import iskallia.vault.core.event.Event
import iskallia.vault.core.vault.Vault
import iskallia.vault.core.vault.objective.*
import iskallia.vault.core.world.data.entity.PartialCompoundNbt
import iskallia.vault.core.world.data.tile.PartialBlockState
import iskallia.vault.core.world.data.tile.PartialTile
import iskallia.vault.core.world.template.DynamicTemplate
import iskallia.vault.core.world.template.JigsawTemplate
import iskallia.vault.init.ModBlocks
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf

data class ObjectiveTemplateData(var template:JigsawTemplate,val vault: Vault)

/*
* objective POIs used block set event; That was slow and it was disabled by "fast set blocks" (batchSetBlocks.kt)
* this is a replacement
* it works by changing type of template used for placing objectives
*   herald boss room probably doesn't use template, just the actual block, so it is still broken
*
* */


object ObjectiveTemplateEvent:Event<ObjectiveTemplateEvent, ObjectiveTemplateData>(){
    init {
        CommonEvents.REGISTRY.add(this)
    }
    private val SCAVENGER_OBJECTIVE_TEMPLATE =
            dynamicTemplateOf(
                BlockPos(0,0,0) to ModBlocks.SCAVENGER_ALTAR.defaultBlockState()
            )
    private val LODESTONE_OBJECTIVE_TEMPLATE =
            dynamicTemplateOf(
                BlockPos(0,1,0) to ModBlocks.LODESTONE.defaultBlockState()
            )
    private val MONOLITH_OBJECTIVE_TEMPLATE =
        /*try {
            dynamicTemplateOf(
                BlockPos(0,0,0) to ModBlocks.MONOLITH.defaultBlockState().setValue(MonolithBlock.HALF, DoubleBlockHalf.LOWER),
                BlockPos(0,1,0) to ModBlocks.MONOLITH.defaultBlockState().setValue(MonolithBlock.HALF, DoubleBlockHalf.UPPER),
            )
        }catch (e:NoSuchFieldError){//3.13 made monolith single half; trying to access MonolithBlock.HALF will cause NoSuchFieldError*/
            dynamicTemplateOf(
                BlockPos(0,0,0) to ModBlocks.MONOLITH.defaultBlockState()
            )
        //}
    private val CRAKE_OBJECTIVE_TEMPLATE =
            dynamicTemplateOf(
                BlockPos(0,0,0) to ModBlocks.CRAKE_COLUMN.defaultBlockState(),
                BlockPos(0,1,0) to ModBlocks.CRAKE_PEDESTAL.defaultBlockState(),
            )
    private val OBELISK_OBJECTIVE_TEMPLATE =
            dynamicTemplateOf(
                BlockPos(0,0,0) to ModBlocks.OBELISK.defaultBlockState().setValue(ObeliskBlock.HALF, DoubleBlockHalf.LOWER),
                BlockPos(0,1,0) to ModBlocks.OBELISK.defaultBlockState().setValue(ObeliskBlock.HALF, DoubleBlockHalf.UPPER),
            )
    private val HERALD_OBJECTIVE_TEMPLATE =
        dynamicTemplateOf(
            BlockPos(0,0,0) to ModBlocks.HERALD_CONTROLLER.defaultBlockState().setValue(HeraldControllerBlock.HALF, DoubleBlockHalf.LOWER),
            BlockPos(0,1,0) to ModBlocks.HERALD_CONTROLLER.defaultBlockState().setValue(HeraldControllerBlock.HALF, DoubleBlockHalf.UPPER),
        )
    private val GRID_GATEWAY_OBJECTIVE_TEMPLATE =
        dynamicTemplateOf(
            BlockPos(0,0,0) to ModBlocks.GRID_GATEWAY.defaultBlockState()
        )
    private val OFFERING_BOSS_OBJECTIVE_TEMPLATE =
        dynamicTemplateOf(
            BlockPos(0,0,0) to ModBlocks.OFFERING_PILLAR.defaultBlockState()
        )

    override fun createChild() = this

    /*registers event for given objective*/
    @JvmOverloads
    fun registerObjectiveTemplate(objective:Objective,vault:Vault, newTemplate:DynamicTemplate? = getTemplateForObjective(objective)){
        register(objective){
            (template,vault2)->
            if(vault2!==vault)return@register
            (template as? JigsawRootAccessor)?.let {
                if (newTemplate != null) it.root = newTemplate
            }
        }
    }
    private fun getTemplateForObjective(objective: Objective) =
        when(objective){
            is ScavengerObjective -> SCAVENGER_OBJECTIVE_TEMPLATE
            is MonolithObjective -> MONOLITH_OBJECTIVE_TEMPLATE
            is ObeliskObjective, is LegacyObeliskObjective -> OBELISK_OBJECTIVE_TEMPLATE
            is CrakePedestalObjective -> CRAKE_OBJECTIVE_TEMPLATE
            is LodestoneObjective -> LODESTONE_OBJECTIVE_TEMPLATE
            is GridGatewayObjective -> GRID_GATEWAY_OBJECTIVE_TEMPLATE
            is OfferingBossObjective -> OFFERING_BOSS_OBJECTIVE_TEMPLATE
            is HeraldObjective -> HERALD_OBJECTIVE_TEMPLATE//does not work for herald
            else -> null
        }

}


@JvmName("dynamicTemplateOfTiles")
private fun dynamicTemplateOf(vararg tiles:PartialTile) = DynamicTemplate().apply {
    tiles.forEach (this::add)
}
private fun dynamicTemplateOf(vararg tiles:Pair<BlockPos,BlockState>) = dynamicTemplateOf(*tiles.map { (pos,state)->PartialTile.of(PartialBlockState.of(state), PartialCompoundNbt.empty(),pos) }.toTypedArray())
