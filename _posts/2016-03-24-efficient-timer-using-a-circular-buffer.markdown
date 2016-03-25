---
layout: post
title: "Efficient Timer using a Circular Buffer"
date: 2016-03-24 00:09:03 +0100
categories: update
---

Over the week I read an interesting post about a timer that allows [*O(1)* scheduling](http://www.cubrid.org/blog/dev-platform/more-efficient-timer-implementation-using-timerwheel/), rather than the usual *O(log n)* complexity, i.e. of [`java.util.Timer`](https://docs.oracle.com/javase/7/docs/api/java/util/Timer.html):

> [...] it uses a binary heap to represent its task queue, so the cost to schedule a task is O(log n), where n is the number of concurrently scheduled tasks.

The original post explains the idea and motivation behind it, together with examples of how it was used.

As an exercise, I decided to implement it, but with a small twist: instead of using blocking operations (`synchronized`, `Lock`, etc), I tried to make it a [non-blocking algorithm](https://en.wikipedia.org/wiki/Non-blocking_algorithm). There are several [ways to accomplish](http://preshing.com/20120612/an-introduction-to-lock-free-programming/#compare-and-swap-loops) that, but I went for [compare-and-swap](https://en.wikipedia.org/wiki/Compare-and-swap) operations, or *CAS*.

The result can be found in [this gist](https://gist.github.com/lucastorri/fa4da12ab374a98618f2).

The general idea is that your timer is backed by an indexed sequence, for instance an Array. Scheduled task are mapped to different indexes based on their deadline, and each index represents a time frame, for instance tasks to be executed in 50 ms.

In my implementation, there are two methods that are used to make it work:

1. `add`, as the name states, allows scheduling tasks. Each task has a function to be executed and a long representing in how many milliseconds that function should be executed. This method is thread safe;
2. `submitCurrentTasks` runs on a separated thread, sleeping between intervals and running on a thread pool tasks that are due to execution.

{% highlight scala %}
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
{% endhighlight %}


When you add a task, the first step is calculating on which slot of the array should the task be placed. If the slot is the same as the last one executed, the new task is executed right away. If not, it is placed on the appropriate slot.

Each slot has a companion [*AtomicInteger*](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/atomic/AtomicInteger.html) that is used to keep track of how many tasks are on that slot. *AtomicInteger* uses underneath the [*Unsafe* class](http://mishadoff.com/blog/java-magic-part-4-sun-dot-misc-dot-unsafe/) for enabling CAS.

By incrementing the *AtomicInteger* of the chosen slot, we are, let's say, "signaling" the thread running `submitCurrentTasks` that a new task will be added to that slot. If the number returned is smaller than the defined maximum, we place the task on the appropriate index. Otherwise, we rollback and decrement the *AtomicInteger*.

On the thread running `submitCurrentTasks`, once we are awaken, we move our cursor to the next slot and copy the tasks on that slot to a secondary list. In order to know how many tasks there are, we use the same *AtomicInteger* that was used by `add`. As we copy the tasks, we check that they are really there through a *null* check, and set it to *null* to mark that it was copied. In the meanwhile, if no items were added to that slot, the number of tasks on the secondary list will be the same as the value on the *AtomicInteger*. If that's the case, the compare-and-set operation will be successful and we will be able to run the tasks. Otherwise, it will fail and we will repeat the steps until the values match.



Of course, this approach has a lot of drawbacks and only works for very particular cases, as explained on the original article. But this implementation has its own special behavior.

If you try to add a new task, and the cursor moves between the time you calculate the task's slot and the time you finally place the task on the slot, you might end up missing its right execution deadline, and instead will take a whole cycle to execute it. To give an example, let's say that the current cursor is 0, you add a task and calculates that it should go to slot 1, and before you place it in the slot, the scheduler thread moves the cursor to 1 and submits all tasks.

On the other hand, it won't cause a delay of execution. If you had a blocking algorithm, all the tasks on slot 1 would have their execution delayed until you finally placed the new task on its slot. Furthermore, (and I haven't benchmarked that yet), non-locking algorithms tend to be faster than their locking counterparts, as they cause less context switches.

But to be honest, parallelism is a tricky world which I know very little, and I'm not entirely sure if this implementation would always work... The question here is: given a task is added to its backing array, will the scheduler thread see those changes?

For instance, when you have a variable that is modified by different threads in Java, you might use the [*volatile* keyword](http://www.javamex.com/tutorials/synchronization_volatile.shtml) to let the compiler know about that. *volatile* makes reads and writes to that variable to pass through "main memory", instead of the thread cache, meaning that changes are always visible between threads.

As long as the [specification goes](https://docs.oracle.com/javase/specs/jls/se7/html/jls-8.html#jls-8.3.1.4), volatile works for fields only, and arrays are not treated any differently. Perhaps [*AtomicReferenceArray*](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/atomic/AtomicReferenceArray.html) would do the trick here...
