package implementslegend.mod.vaultfaster

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

fun overflowExecutor(coreThreads:Int=Runtime.getRuntime().availableProcessors(), overflowThreads:Int=4,poolTaskCapacity:Int=32,factory:ThreadFactory = Executors.defaultThreadFactory()):ExecutorService {

    val overflowPool = Executors.newFixedThreadPool(overflowThreads,factory)
    var incrementPoolSize = {}

    val draining = AtomicBoolean(false)
    val queue = LinkedBlockingQueue<Runnable>(poolTaskCapacity)

    val sequence = AtomicInteger(0)
    val lastOverflowTime = AtomicLong(System.nanoTime())

    val corePool = ThreadPoolExecutor(1,1,10,TimeUnit.MINUTES,queue,factory){
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
        val t = System.nanoTime()
        if(t-lastOverflowTime.get()<5e8){
            sequence.getAndIncrement()
        }else sequence.set(0)
        lastOverflowTime.set(t)
        if(coreThreads>corePool.maximumPoolSize && sequence.get()>3){
            corePool.maximumPoolSize++
            sequence.set(0)
        }

    }

    return corePool
}
