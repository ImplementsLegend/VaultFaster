package implementslegend.mod.vaultfaster

import java.util.concurrent.atomic.AtomicInteger


inline fun <reified T:Any> concurrentArrayCollection(capacity:Int) = AtomicallyIndexedConcurrentArrayCollection(arrayOfNulls<T>(capacity))
class AtomicallyIndexedConcurrentArrayCollection<T:Any>(val initialArray: Array<T?>):Collection<T> {


    val index = AtomicInteger(0)
    fun add(element:T){
        val position = index.incrementAndGet()
        initialArray[position]=element
    }

    override val size: Int
        get() = index.get()

    @Suppress("ReplaceSizeCheckWithIsNotEmpty")
    override fun isEmpty(): Boolean = size!=0

    override fun iterator(): Iterator<T> {
        var position = 0
        return object :Iterator<T>{
            override fun hasNext(): Boolean = position<size && initialArray[position+1]!==null

            override fun next(): T = initialArray[position++]!!

        }
    }

    fun toList() = List(index.get()){initialArray[it]}

    override fun containsAll(elements: Collection<T>): Boolean = elements.all(::contains)

    override fun contains(element: T): Boolean = this.any { element == it }


}