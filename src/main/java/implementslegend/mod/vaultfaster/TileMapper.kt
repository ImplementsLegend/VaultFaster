package implementslegend.mod.vaultfaster

import implementslegend.mod.vaultfaster.interfaces.CachedPaletteContainer
import implementslegend.mod.vaultfaster.interfaces.IndexedBlock
import implementslegend.mod.vaultfaster.mixin.accessors.PartialBlockIDAccessor
import implementslegend.mod.vaultfaster.mixin.accessors.PredicateIdAccessor
import implementslegend.mod.vaultfaster.mixin.accessors.ProcessorPredicateAccessor
import implementslegend.mod.vaultfaster.mixin.accessors.ReferenceProcessorAccessor
import iskallia.vault.core.Version
import iskallia.vault.core.world.data.entity.PartialCompoundNbt
import iskallia.vault.core.world.data.tile.*
import iskallia.vault.core.world.processor.ProcessorContext
import iskallia.vault.core.world.processor.tile.*
import iskallia.vault.init.ModBlocks
import iskallia.vault.init.ModConfigs
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Blocks
import net.minecraftforge.registries.ForgeRegistries

/*
* Template processor multi hash map; maps block numerical id -> list of tile processors
* trying to apply every tile processor to every tile was just too taxing and way to reduce number of considered processors was required
* this is probably the most important optimisation in the entire mod
*
* */

typealias TileProcessors = ArrayList<IndexedValue<TileProcessor>>



class TileMapper() {

    //all processors for which numerical ids couldn't be determined
    val unconditional:TileProcessors = arrayListOf()
    var transform = transformIdentity()
    /*
     * main table for storing all the mappings
     * idx: numerical id of block
     * value: list of applicable tile processors
     *
     * now multi-tier to preserve a bit of memory
     */
    var tail = 0
    var head = 0

    val mappingsTiered = Array<Array<TileProcessors>?>((BLOCKS.size() shr 8)+1){
        null
    }



    /*
    * applies tile processors to a tile
    * */
    fun mapBlock(tile: PartialTile?, ctx: ProcessorContext, startIdx:Int=Int.MIN_VALUE):PartialTile? {
        if(tile===null)return null
        var newTile:PartialTile? =if(startIdx==Int.MIN_VALUE) transform.process(tile,ctx)  else  tile
        val idx = ((newTile?:return null).state.block as IndexedBlock).registryIndex

        val unconditionalMappings = unconditional
        val conditionalMappings = mappingsTiered.getOrNull(idx shr 8)?.getOrNull(idx and 0xff)?: emptyList()

        var i1 = 0
        var i2 = 0
        for (mp in unconditionalMappings)if(mp.index<startIdx)i1++ else break
        for (mp in conditionalMappings)if(mp.index<startIdx)i2++ else break
        while(i1 in unconditionalMappings.indices && i2 in conditionalMappings.indices){
            if(unconditionalMappings[i1].index>conditionalMappings[i2].index){

                newTile=conditionalMappings[i2].value.process(newTile,ctx)?:return null
                if(idx!=(newTile.state.block as IndexedBlock).registryIndex) return mapBlock(newTile, ctx,conditionalMappings[i2].index)
                i2++
            }else{

                newTile=unconditionalMappings[i1].value.process(newTile,ctx)?:return null
                if(idx!=(newTile.state.block as IndexedBlock).registryIndex) return mapBlock(newTile, ctx,unconditionalMappings[i1].index)
                i1++
            }
        }
        for (i2b in i2 until conditionalMappings.size){
            newTile=conditionalMappings[i2b].value.process(newTile,ctx)?:return null
            if(idx!=(newTile.state.block as IndexedBlock).registryIndex) return mapBlock(newTile, ctx,conditionalMappings[i2b].index)

        }

        for (i1b in i1 until unconditionalMappings.size){
            newTile=unconditionalMappings[i1b].value.process(newTile,ctx)?:return null
            if(idx!=(newTile.state.block as IndexedBlock).registryIndex) return mapBlock(newTile, ctx,unconditionalMappings[i1b].index)

        }
        return newTile

    }

    private fun getOrCreateTier(idx: Int): Array<TileProcessors> {
        if(idx !in mappingsTiered.indices) return emptyArray()
        return this.mappingsTiered.updateAndGet(idx){
            old->
            if(old===null) Array(256){
                arrayListOf()
            } else old
        }!!
    }

    fun transform(transformTileProcessor: TransformTileProcessor, start: Boolean) {
        transform=if(start) mergeTransforms(transformTileProcessor,transform) else mergeTransforms(transform,transformTileProcessor)
    }

    @JvmOverloads
    fun addProcessor(processor: TileProcessor,start:Boolean=false){
        if(processor is TranslateTileProcessor) return transform(processor.toTransformTileProcessor(), start)
        if(processor is MirrorTileProcessor) return transform(processor.toTransformTileProcessor(), start)
        if(processor is RotateTileProcessor) return transform(processor.toTransformTileProcessor(), start)

        try {
            if (processor is BernoulliWeightedTileProcessor) {
                addProcessor(processor.target, processor,start=start)
            } else if (processor is VaultLootTileProcessor) {
                addProcessor(PartialBlock.of(ModBlocks.PLACEHOLDER), processor,start=start)
            } else if (processor is SpawnerElementTileProcessor) {
                addProcessor(PartialBlock.of(ForgeRegistries.BLOCKS.getValue(ResourceLocation("ispawner","spawner"))), processor,start=start)
            } else if (processor is JigsawTileProcessor) {
                addProcessor(PartialBlock.of(Blocks.JIGSAW),processor,start=start)
            } else if (processor is StructureVoidTileProcessor) {
                addProcessor(PartialBlock.of(Blocks.STRUCTURE_VOID),processor,start=start)
            } else if (processor is TargetTileProcessor<*>) {
                addProcessor((processor as ProcessorPredicateAccessor).predicate, processor,start=start)
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
        val accessor = processor as ReferenceProcessorAccessor
        when(accessor.pool.size ){
            0->{/*do nothing*/}
            1->{
                /*flatten as before*/
                (processor as CachedPaletteContainer).getCachedPaletteForVersion(Version.latest()).tileProcessors.let {
                    if (start)it.reversed() else it
                }.forEach {
                    this.addProcessor(it,start)
                }
            }
            else->{
                /*good luck*/
                this.addUnconditional(processor)
            }

        }
    }

    private fun addUnconditional(processor: TileProcessor,start:Boolean=false) {
        unconditional.let {
                list->
            if(start)list.add(0,IndexedValue(--head,processor))
            else list+=IndexedValue(tail++,processor)
        }
    }

    private fun addProcessor(predicate:TilePredicate, processor: TileProcessor,start:Boolean=false){

        getIndices(predicate).takeIf { indices -> indices.none { it<0 } }?.forEach {
            getOrCreateTier(it shr 8).let {
                tier->
                val list = tier[it and 0xff]
                if(start)list.add(0,IndexedValue(--head,processor))
                else list+=IndexedValue(tail++,processor)
            }
        }?: run {
            addUnconditional(processor,start)
        }
    }


}

private fun <T> Array<T>.updateAndGet(idx: Int, update:(T)->T): T = update(this[idx]).also { this[idx]=it }


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
            val key = pred.id
            ModConfigs.TILE_GROUPS.groups[key]?.asSequence()?.flatMap ( ::getIndices )?: emptySequence()

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
