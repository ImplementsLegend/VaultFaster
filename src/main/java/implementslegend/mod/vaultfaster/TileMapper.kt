package implementslegend.mod.vaultfaster

import implementslegend.mod.vaultfaster.mixin.PredicateIdAccessor
import implementslegend.mod.vaultfaster.mixin.ProcessorPredicateAccessor
import implementslegend.mod.vaultfaster.mixin.TileGroupsAccessor
import iskallia.vault.core.world.data.entity.PartialCompoundNbt
import iskallia.vault.core.world.data.tile.*
import iskallia.vault.core.world.processor.ProcessorContext
import iskallia.vault.core.world.processor.tile.BernoulliWeightedTileProcessor
import iskallia.vault.core.world.processor.tile.LeveledTileProcessor
import iskallia.vault.core.world.processor.tile.ReferenceTileProcessor
import iskallia.vault.core.world.processor.tile.SpawnerElementTileProcessor
import iskallia.vault.core.world.processor.tile.SpawnerTileProcessor
import iskallia.vault.core.world.processor.tile.TileProcessor
import iskallia.vault.core.world.processor.tile.VaultLootTileProcessor
import iskallia.vault.core.world.processor.tile.WeightedTileProcessor
import iskallia.vault.init.ModBlocks
import iskallia.vault.init.ModConfigs
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraftforge.registries.ForgeRegistries
import java.util.concurrent.atomic.AtomicBoolean

/*
* Template processor multi hash map; maps block numerical id -> list of tile processors
* trying to apply every tile processor to every tile was just too taxing and way to reduce number of considered processors was required
* this is probably the most important optimisation in the entire mod
*
* */

class TileMapper {
    val couldContainReferences = AtomicBoolean(true)
    var couldContainReferencesBadSync = true //so that you don't have to check Atomic version every time


    //all processors for which numerical ids couldn't be determined
    val unconditional:ArrayList<TileProcessor> = arrayListOf()
    /*
     * main table for storing all the mappings
     * idx: numerical id of block
     * value: list of applicable tile processors
     */
    val mappings:Array<ArrayList< TileProcessor>> = Array(BLOCKS.size()){
        arrayListOf()
    }


    /*
    * applies tile processors to a tile
    * */
    fun mapBlock(tile: PartialTile?, ctx: ProcessorContext):PartialTile? {
        val idx = ((tile?:return null).state.block as IndexedBlock).registryIndex
        var newTile:PartialTile =tile
        if (couldContainReferencesBadSync && couldContainReferences.getAndSet(false)) {
            couldContainReferencesBadSync=false
            val newProcessors = arrayListOf<TileProcessor>()
            unconditional.removeIf {
                tryFlatten(it, newProcessors, ctx)
            }
            unconditional += newProcessors
        }
        newTile = mapUnconditional(idx, newTile, ctx)?:return null
        newTile = mapConditional(idx, newTile, ctx)?:return null
        return newTile

    }

    private fun mapUnconditional(idx:Int,tile:PartialTile,ctx:ProcessorContext):PartialTile?{
        var newTile:PartialTile? =tile

        for(tileProcessor in unconditional){
            newTile=tileProcessor.process(newTile?:return null,ctx)
            if(idx!=(tile.state.block as IndexedBlock).registryIndex) return mapBlock(newTile, ctx)
        }
        return newTile
    }
    private fun mapConditional(idx:Int,tile:PartialTile,ctx:ProcessorContext):PartialTile?{

        var newTile:PartialTile? =tile
        for(tileProcessor in mappings.getOrNull(idx)?: emptyList()){
            if(/*cond(tile)*/ true){
                newTile=tileProcessor.process(newTile?:return null,ctx)
                if(idx!=(tile.state.block as IndexedBlock).registryIndex) return mapBlock(newTile, ctx)
            }
        }
        return newTile
    }

    private fun tryFlatten(processor: TileProcessor, newProcessors: ArrayList<TileProcessor>, ctx: ProcessorContext): Boolean {
        if(processor !is ReferenceTileProcessor) return false

        ((processor as CachedPaletteContainer).getCachedPalette(ctx) as TileMapperContainer).tileMapper.let { mapper ->
            mapper.mappings.forEachIndexed{
                idx,processors->
                mappings[idx]+=processors
            }
            mapper.unconditional.forEach {
                if (!tryFlatten(it,newProcessors,ctx))newProcessors+=it
            }
        }

        return true

    }

    fun addProcessor(processor: TileProcessor){
        try {
            if (processor is SpawnerTileProcessor || processor is WeightedTileProcessor) {
                addProcessor((processor as ProcessorPredicateAccessor).predicate, processor)
            } else if (processor is BernoulliWeightedTileProcessor) {
                addProcessor(processor.target, processor)
            } else if (processor is VaultLootTileProcessor) {
                addProcessor(PartialBlock.of(ModBlocks.PLACEHOLDER), processor)
            } else if (processor is SpawnerElementTileProcessor) {
                addProcessor(PartialBlock.of(ForgeRegistries.BLOCKS.getValue(ResourceLocation("ispawner","spawner"))), processor)
            } else if (processor is LeveledTileProcessor) {
                addProcessor(LeveledPredicate(processor),processor)
            } else {
                addUnconditional(processor)
            }
        } catch (th: UnconditionalPredicate) {
            addUnconditional(processor)
        }

    }

    private fun addUnconditional(processor: TileProcessor) {
        if(processor is ReferenceTileProcessor) {
            couldContainReferences.set(true)
            couldContainReferencesBadSync = true
        }
        unconditional += processor
    }

    private fun addProcessor(predicate:TilePredicate, processor: TileProcessor){
        getIndices(predicate).takeIf { indices -> indices.none { it<0 } }?.forEach {
            mappings[it]+=processor
        }?: run {
            addUnconditional(processor)
        }
    }

    /**
     * determines which blocks can mach given predicate
     * @param pred predicate for which to determine blocks
     * @return sequence of numerical ids of blocks that can match the given predicate
     * @throws UnconditionalPredicate if predicate isn't restricted by block type
     * */
    private fun getIndices(pred:TilePredicate):Sequence<Int>{
        return when(pred){
            is PartialBlockTag->{
                val key = TagKey(Registry.BLOCK_REGISTRY,(pred as PredicateIdAccessor).id)
                ForgeRegistries.BLOCKS.tags()?.getTag(key)?.iterator()?.asSequence()?.map { (it as IndexedBlock).registryIndex }?: emptySequence()

            }
            is PartialBlockGroup->{
                val key = (pred as PredicateIdAccessor).id
                (ModConfigs.TILE_GROUPS as TileGroupsAccessor).groups[key]?.asSequence()?.flatMap ( ::getIndices )?: emptySequence()

            }
            is PartialTile->{
                sequenceOf((pred.state.block as IndexedBlock).registryIndex)
            }
            is PartialBlockState->{
                sequenceOf((pred.block as IndexedBlock).registryIndex)
            }
            is PartialBlock->{
                sequenceOf((pred as IndexedBlock).registryIndex)
            }
            is OrTilePredicate->{
                pred.children.asSequence().flatMap ( ::getIndices )
            }
            is LeveledPredicate->{
                pred.processor.levels.entries.asSequence().flatMap{
                    (_,processor)->

                    if (processor is SpawnerTileProcessor || processor is WeightedTileProcessor) {
                        getIndices((processor as ProcessorPredicateAccessor).predicate)
                    } else if (processor is BernoulliWeightedTileProcessor) {
                        getIndices(processor.target)
                    } else if (processor is VaultLootTileProcessor) {
                        getIndices(PartialBlock.of(ModBlocks.PLACEHOLDER))
                    } else if (processor is LeveledTileProcessor) {
                        getIndices(LeveledPredicate(processor))
                    } else {
                        throw UnconditionalPredicate()
                    }
                }
            }
            else -> throw UnconditionalPredicate()
        }.distinct()
    }
}

private class UnconditionalPredicate:Throwable()

private class LeveledPredicate(val processor:LeveledTileProcessor):TilePredicate {
    override fun test(p0: PartialBlockState?, p1: PartialCompoundNbt?) = true
}
