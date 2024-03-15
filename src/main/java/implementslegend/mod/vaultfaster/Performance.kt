package implementslegend.mod.vaultfaster

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class Performance {
    var count = AtomicInteger()
    var start = System.currentTimeMillis()
    var lastPrint = AtomicLong(start)
    var lastCount = 0


    fun record() {
        val c = count.incrementAndGet()
        lastPrint.updateAndGet {
            if(it+400<System.currentTimeMillis()){
                println("totalChunks: $c, time: ${System.currentTimeMillis()-start}, chunksPerS: ${(1000*c)/(System.currentTimeMillis()-start).toDouble()}, chunkDelta: ${c-lastCount}")
                lastCount=c
                it+400
            }else it
        }
    }
}