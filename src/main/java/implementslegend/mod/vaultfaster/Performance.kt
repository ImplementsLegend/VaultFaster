package implementslegend.mod.vaultfaster

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private val printInterval = System.getProperties().getProperty("implementslegend.vaultfaster.performanceprintinterval","-1").toInt()

class Performance {
    var count = AtomicInteger()
    var start = System.currentTimeMillis()
    var lastPrint = AtomicLong(start)
    var lastCount = 0

    val chunksPerSec get()= count.get()*1000f/(System.currentTimeMillis()-start)

    fun record() {
        val c = count.incrementAndGet()
        if(printInterval>=0)
        lastPrint.updateAndGet {
            if(it+ printInterval<System.currentTimeMillis()){
                println("totalChunks: $c, time: ${System.currentTimeMillis()-start}, chunksPerS: ${(1000*c)/(System.currentTimeMillis()-start).toDouble()}, chunkDelta: ${c-lastCount}")
                lastCount=c
                it+printInterval
            }else it
        }
    }
}