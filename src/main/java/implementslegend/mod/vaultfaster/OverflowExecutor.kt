package implementslegend.mod.vaultfaster

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

fun overflowExecutor(coreThreads:Int=Runtime.getRuntime().availableProcessors(), overflowThreads:Int=4,poolTaskCapacity:Int=32,factory:ThreadFactory = Executors.defaultThreadFactory()):ExecutorService {

    val overflowPool = Executors.newFixedThreadPool(overflowThreads,factory)
    var incrementPoolSize = {}

    val draining = AtomicBoolean(false)
    val queue = LinkedBlockingQueue<Runnable>(poolTaskCapacity)

    val corePool = ThreadPoolExecutor(2,2,10,TimeUnit.MINUTES,queue,factory){
        task,_->

        overflowPool.submit(task)
        if(draining.compareAndSet(false,true)) {
            incrementPoolSize()
            while (true) {
                overflowPool.submit(queue.poll(0, TimeUnit.NANOSECONDS) ?: break)
            }
            draining.set(false)
        }

    }
    incrementPoolSize={
        if(coreThreads>corePool.maximumPoolSize && Random.Default.nextFloat()<1f/(corePool.maximumPoolSize*corePool.maximumPoolSize)){
            corePool.maximumPoolSize++
        }
    }

    return corePool
}
