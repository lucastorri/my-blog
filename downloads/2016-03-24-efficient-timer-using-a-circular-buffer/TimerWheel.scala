// http://www.cubrid.org/blog/dev-platform/more-efficient-timer-implementation-using-timerwheel/
// http://preshing.com/20120612/an-introduction-to-lock-free-programming/#compare-and-swap-loops
// https://en.wikipedia.org/wiki/Non-blocking_algorithm
// http://www.javamex.com/tutorials/synchronization_volatile.shtml
// https://gist.github.com/lucastorri/fa4da12ab374a98618f2

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.JavaConversions._

class TimerWheel(slots: Int, slotMillisInterval: Long, maxTasksPerSlot: Int) {

  private val wheel = Array.fill(slots)(Array.ofDim[Task](maxTasksPerSlot) -> new AtomicInteger(0))
  private val cursor = new AtomicInteger(0)
  private val pool = Executors.newCachedThreadPool()

  /* Constructors shouldn't start threads
   * http://www.javapractices.com/topic/TopicAction.do?Id=254
   */
  TimeoutThread.start()

  def add(task: Task): Boolean = {
    if (task == null) return false

    val currentCursor = cursor.get
    val slotNumber = (currentCursor + (task.afterMillis / slotMillisInterval)).toInt % slots
    if (slotNumber == currentCursor) {
      submit(task)
    } else {
      //TODO if cursor changed, it might have missed execution and will take a whole cycle to be executed
      // but on the other hand, it won't cause a delay on execution
      val (slot, size) = wheel(slotNumber)
      val index = size.getAndIncrement()
      val hasFreeSlot = index < maxTasksPerSlot
      if (hasFreeSlot) slot(index) = task
      else size.decrementAndGet()
      hasFreeSlot
    }
  }

  private object TimeoutThread extends Thread {

    @volatile
    var running = true

    override def run(): Unit = {
      var processingTime = 0L
      while (running) {
        Thread.sleep(Math.max(0, slotMillisInterval - processingTime))
        val started = System.currentTimeMillis()
        submitCurrentTasks()
        processingTime = started - started
      }
    }

    @inline
    private def submitCurrentTasks(): Unit = {
      val currentCursor = cursor.get
      cursor.set((currentCursor + 1) % slots)

      val (slot, size) = wheel(currentCursor)

      val tasks = new java.util.ArrayList[Task]()
      while (!size.compareAndSet(tasks.length, 0)) {
        (tasks.size until math.min(size.get, maxTasksPerSlot))
          .takeWhile(i => slot(i) != null)
          .foreach { i =>
            tasks.add(slot(i))
            slot(i) = null
          }
      }

      tasks.foreach(task => submit(task))
    }

  }

  private def submit(task: Task): Boolean = {
    pool.submit(new TaskRunner(task))
    true
  }

  private class TaskRunner(task: Task) extends Runnable {
    override def run(): Unit = task.f()
  }

  def stop(): Unit = {
    TimeoutThread.running = false
    pool.shutdown()
  }

}

case class Task(afterMillis: Long, f: () => Unit)

object Test extends App {

  val slots = 10
  val slotMillisInterval = 50
  val maxTasksPerSlot = 20
  val timer = new TimerWheel(slots, slotMillisInterval, maxTasksPerSlot)

  val adders = Runtime.getRuntime.availableProcessors() + 1
  val executor = Executors.newFixedThreadPool(adders)
  val nThreads = 10
  val threadRange = 0 until nThreads
  val runs = 3
  val nTasks = runs * nThreads * slots
  val timestamps = java.util.Collections.synchronizedList(new java.util.ArrayList[(Long, Long)]())

  for (_ <- threadRange) {
    executor.submit(AddThread)
  }

  while (timestamps.size != nTasks) {
    Thread.sleep(500)
    System.out.println(s"waiting... [${timestamps.size}/$nTasks]")
  }
  System.out.println("done!")
  executor.shutdown()
  timer.stop()
  timestamps.foreach(println)

  object AddThread extends Runnable {
    override def run(): Unit = {
      for (_ <- 0 until runs) {
        for (i <- 0 until slots) {
          val now = System.currentTimeMillis()
          val after = i * slotMillisInterval
          val added = timer.add(Task(after, () => {
            val executed = System.currentTimeMillis()
            val expected = now + after
            timestamps.add((executed, math.abs(expected - executed)))
          }))
          if (!added) timestamps.add((-1L, -1L))
        }
        Thread.sleep(100)
      }
    }
  }

}

