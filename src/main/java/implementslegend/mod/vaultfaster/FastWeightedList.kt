package implementslegend.mod.vaultfaster

import iskallia.vault.core.random.RandomSource
import iskallia.vault.util.data.WeightedList
import java.util.random.RandomGenerator
import kotlin.random.Random

class FastWeightedList<T> private constructor(capacity:Int) {

    var totalWeight:Float = 0f

    var weights = FloatArray(capacity)
    var values = ArrayList<T>(capacity)

    val entries = weights.zip(values).filter { it.first!=0f }

    constructor(w:iskallia.vault.core.util.WeightedList<T>):this(w.entries.size){
        w.entries.sortedBy { it.value }.forEach {
            mutableEntry ->
            totalWeight+=mutableEntry.value.toFloat()
            values+=mutableEntry.key
            weights[values.size-1]=mutableEntry.value.toFloat()
        }
    }


    constructor(w:WeightedList<T>):this(w.size){
        w.sortedBy { it.weight }.forEachIndexed {
                index, mutableEntry ->
            totalWeight+=mutableEntry.weight.toFloat()
            weights[index]=mutableEntry.weight.toFloat()
            values[index]=mutableEntry.value
        }
    }


    fun random(rng:RandomGenerator) = random(rng::nextFloat)
    fun random(rng:RandomSource) = random(rng::nextFloat)
    fun random(rng:Random) = random(rng::nextFloat)
    fun random(rng:()->Float):T?{
        if(totalWeight<=0f)return null
        weights.foldIndexed(rng()*totalWeight){
            idx,acc,weight->
            if(acc<weight)return@random values[idx]
            acc-weight
        }
        return values.last()
    }


}