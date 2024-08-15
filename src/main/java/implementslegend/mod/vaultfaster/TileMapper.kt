package implementslegend.mod.vaultfaster

import implementslegend.mod.vaultfaster.interfaces.CachedPaletteContainer
import implementslegend.mod.vaultfaster.interfaces.IndexedBlock
import implementslegend.mod.vaultfaster.interfaces.TileMapperContainer
import implementslegend.mod.vaultfaster.mixin.PredicateIdAccessor
import implementslegend.mod.vaultfaster.mixin.ProcessorPredicateAccessor
import implementslegend.mod.vaultfaster.mixin.TileGroupsAccessor
import iskallia.vault.core.Version
import iskallia.vault.core.world.data.entity.PartialCompoundNbt
import iskallia.vault.core.world.data.tile.*
import iskallia.vault.core.world.processor.ProcessorContext
import iskallia.vault.core.world.processor.tile.BernoulliWeightedTileProcessor
import iskallia.vault.core.world.processor.tile.LeveledTileProcessor
import iskallia.vault.core.world.processor.tile.ReferenceTileProcessor
import iskallia.vault.core.world.processor.tile.SpawnerElementTileProcessor
import iskallia.vault.core.world.processor.tile.SpawnerTileProcessor
import iskallia.vault.core.world.processor.tile.TemplateStackSpawnerProcessor
import iskallia.vault.core.world.processor.tile.TemplateStackTileProcessor
import iskallia.vault.core.world.processor.tile.TileProcessor
import iskallia.vault.core.world.processor.tile.VaultLootTileProcessor
import iskallia.vault.core.world.processor.tile.WeightedTileProcessor
import iskallia.vault.init.ModBlocks
import iskallia.vault.init.ModConfigs
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraftforge.registries.ForgeRegistries
import java.util.concurrent.atomic.AtomicReferenceArray

/*
* Template processor multi hash map; maps block numerical id -> list of tile processors
* trying to apply every tile processor to every tile was just too taxing and way to reduce number of considered processors was required
* this is probably the most important optimisation in the entire mod
*
* */

val AtomicReferenceArray<*>.indices get() = 0 until this.length()

fun <T> AtomicReferenceArray<T>.forEachIndexed(fn:(index:Int,element:T)->Unit){
    for(i in indices) {
        fn(i, get(i))
    }
}


private fun <E> AtomicReferenceArray<E>.getOrNull(i: Int): E? = if(i !in indices) null else get(i)

class TileMapper() {

    //all processors for which numerical ids couldn't be determined
    val unconditional:ArrayList<TileProcessor> = arrayListOf()
    /*
     * main table for storing all the mappings
     * idx: numerical id of block
     * value: list of applicable tile processors
     *
     * nuw multi-tier to preserve a bit of memory
     */
    val mappingsTiered = AtomicReferenceArray(Array<Array<ArrayList<TileProcessor>>?>((BLOCKS.size() shr 8)+1){
        null
    })


    /*
    * applies tile processors to a tile
    * */
    fun mapBlock(tile: PartialTile?, ctx: ProcessorContext):PartialTile? {
        val idx = ((tile?:return null).state.block as IndexedBlock).registryIndex
        var newTile:PartialTile =tile
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
        for(tileProcessor in mappingsTiered.getOrNull(idx shr 8)?.getOrNull(idx and 0xff)?: emptyList()){
            newTile=tileProcessor.process(newTile?:return null,ctx)
            if(idx!=(tile.state.block as IndexedBlock).registryIndex) return mapBlock(newTile, ctx)
        }
        return newTile
    }

    private fun getOrCreateTier(idx: Int): Array<ArrayList<TileProcessor>> {
        if(idx !in mappingsTiered.indices) return emptyArray()
        return this.mappingsTiered.updateAndGet(idx){
            old->
            if(old===null) Array(256){
                arrayListOf()
            } else old
        }!!
            /*?:Array<ArrayList<TileProcessor>>(256){
            arrayListOf()
        }.also { mappingsTiered[idx]= it }*/
    }

    @JvmOverloads
    fun addProcessor(processor: TileProcessor,start:Boolean=false){
        try {
            if (processor is SpawnerTileProcessor || processor is WeightedTileProcessor || processor is TemplateStackTileProcessor || processor is TemplateStackSpawnerProcessor) {
                addProcessor((processor as ProcessorPredicateAccessor).predicate, processor,start=start)
            } else if (processor is BernoulliWeightedTileProcessor) {
                addProcessor(processor.target, processor,start=start)
            } else if (processor is VaultLootTileProcessor) {
                addProcessor(PartialBlock.of(ModBlocks.PLACEHOLDER), processor,start=start)
            } else if (processor is SpawnerElementTileProcessor) {
                addProcessor(PartialBlock.of(ForgeRegistries.BLOCKS.getValue(ResourceLocation("ispawner","spawner"))), processor,start=start)
            } else if (processor is LeveledTileProcessor) {
                addProcessor(LeveledPredicate(processor),processor,start=start)
            } else if(processor is ReferenceTileProcessor){
                addFlattening(processor, start=start)
            }else{
                addUnconditional(processor,start=start)
            }
        } catch (th: UnconditionalPredicate) {
            addUnconditional(processor,start=start)
        }

    }

    private fun addFlattening(processor:ReferenceTileProcessor, start: Boolean = false){
        (processor as CachedPaletteContainer).getCachedPaletteForVersion(Version.v1_20).tileProcessors.let {
            if (start)it.reversed() else it
        }.forEach {
            this.addProcessor(it,start)
        }
        /*
        val otherTileMapper = ((processor as CachedPaletteContainer).getCachedPaletteForVersion(Version.v1_20) as TileMapperContainer).tileMapper
        otherTileMapper.mappingsTiered.forEachIndexed{
                idx,tier->
            if(tier===null)return@forEachIndexed
            val thisTier = this.getOrCreateTier(idx)
            thisTier.forEachIndexed {
                    idx, processors->
                if(start)processors.addAll(0,tier[idx]) else processors+=tier[idx]
            }
        }

        unconditional.let {
                list->
            if(start)list.addAll(0,otherTileMapper.unconditional)
            else list+=otherTileMapper.unconditional
        }*/
    }

    private fun addUnconditional(processor: TileProcessor,start:Boolean=false) {
        unconditional.let {
                list->
            if(start)list.add(0,processor)
            else list+=processor
        }
    }

    private fun addProcessor(predicate:TilePredicate, processor: TileProcessor,start:Boolean=false){

        getIndices(predicate).takeIf { indices -> indices.none { it<0 } }?.forEach {
            getOrCreateTier(it shr 8).let {
                tier->
                if(tier.isEmpty()){
                    println(it shr 8)
                    println(BLOCKS.size() shr 8)
                    println(mappingsTiered.length())
                }
                val list = tier[it and 0xff]
                if(start)list.add(0,processor)
                else list+=processor
            }
        }?: run {
            addUnconditional(processor,start)
        }
    }


}


/**
 * determines which blocks can mach given predicate
 * @param pred predicate for which to determine blocks
 * @return sequence of numerical ids of blocks that can match the given predicate
 * @throws UnconditionalPredicate if predicate isn't restricted by block type
 * */
fun getIndices(pred:TilePredicate):Sequence<Int>{
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

private class UnconditionalPredicate:Throwable()

private class LeveledPredicate(val processor:LeveledTileProcessor):TilePredicate {
    override fun test(p0: PartialBlockState?, p1: PartialCompoundNbt?) = true
}
