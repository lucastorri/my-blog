//https://github.com/papers-we-love/papers-we-love/blob/master/caching/a-constant-algorithm-for-implementing-the-lfu-cache-eviction-scheme.pdf

import scala.collection.mutable

trait Identifier[Item, Id] {

  def apply(item: Item): Id

}

class HashCodeIdentifier[Item] extends Identifier[Item, Int] {

  override def apply(item: Item): Int = item.hashCode

}

class DoubleLinkedLFUCache[Item, Id](maxSize: Int)(implicit val identifier: Identifier[Item, Id]) {

  private val entries = mutable.HashMap.empty[Id, CachedItem]
  private val frequencyList = new FrequencyList(0)

  def insert(item: Item): Unit = {
    val id = identifier(item)
    if (entries.contains(id)) lookup(id)
    else append(item)
  }

  def lookup(itemId: Id): Option[Item] = {
    entries.get(itemId).map { cachedItem =>
      cachedItem.bump()
      cachedItem.item
    }
  }

  def removeLast(): Option[Item] = {
    if (entries.isEmpty) return None

    val last = frequencyList.next.removeLast().item
    entries.remove(identifier(last))
    Some(last)
  }

  private def append(item: Item): Unit = {
    if (entries.size == maxSize) removeLast()

    val cachedItem = new CachedItem(item)
    frequencyList.add(cachedItem)
    cachedItem.bump()
    entries.put(identifier(item), cachedItem)
  }

  class FrequencyList(val frequency: Int) extends DoubleLinked[FrequencyList] {

    var previous = this
    var next = this

    var items: CachedItem = null

    def bump(item: CachedItem): Unit = {
      val bumpedFrequency = frequency + 1
      val linked =
        if (next.frequency == bumpedFrequency) next
        else link(new FrequencyList(bumpedFrequency))

      remove(item)
      linked.add(item)
    }

    def link(link: FrequencyList): FrequencyList = {
      link.next = next
      link.previous = this
      next.previous = link
      next = link
      link
    }

    def unlink(link: FrequencyList): FrequencyList = {
      link.previous.next = link.next
      link.next.previous = link.previous
      link
    }

    def add(item: CachedItem): Unit = {
      item.list = this
      if (items == null) item.reset()
      else items.addBefore(item)
      items = item
    }

    def remove(item: CachedItem): Unit = {
      if (frequency == 0) items = null
      else if (item.isSingle) unlink(this)
      else item.remove()

      if (items == item) items = item.next
    }

    def removeLast(): CachedItem = {
      if (items.isSingle) unlink(this).items
      else items.last.remove()
    }

  }

  class CachedItem(val item: Item) extends DoubleLinked[CachedItem] {

    var list: FrequencyList = null

    var previous = this
    var next = this

    def isSingle: Boolean =
      next == this

    def addBefore(item: CachedItem): Unit = {
      item.previous = previous
      item.next = this
      previous.next = item
      previous = item
    }

    def remove(): CachedItem = {
      previous.next = next
      next.previous = previous
      this
    }

    def reset(): Unit = {
      previous = this
      next = this
    }

    def bump(): Unit = {
      list.bump(this)
    }

    def last: CachedItem =
      previous

  }

  trait DoubleLinked[Type <: DoubleLinked[Type]] { self: Type =>

    def next: Type
    def previous: Type

    def iterate(f: Type => Unit): Unit = {
      var tail = this
      do {
        f(tail)
        tail = tail.next
      } while (tail != this)
    }

  }

  override def toString: String = {
    val str = StringBuilder.newBuilder
    str.append("LFU")
    frequencyList.iterate { list =>
      str.append("\n |\n[").append(list.frequency).append("]")
      if (list.items != null) {
        list.items.iterate { cached =>
          str.append("-").append(identifier(cached.item))
        }
      }
    }
    str.toString()
  }

}

object Main extends App {

  implicit def hashCodeIdentifier[Item]: Identifier[Item, Int] = new HashCodeIdentifier


  val size = 10
  val lfu = new DoubleLinkedLFUCache[Int, Int](size)
  lookup(3)

  ---

  insert(5)
  lookup(5)

  ---

  insert(7)
  lookup(7)
  lookup(7)
  lookup(7)
  lookup(7)

  ---

  removeLast()
  removeLast()
  removeLast()

  ---

  (0 to size).foreach(insert)
  (0 to size).foreach(lookup)
  (0 to size).foreach(_ => removeLast())

  ---

  (0 to size).foreach { i =>
    insert(i)
    (0 until i).foreach(_ => lookup(i))
  }


  def print() =
    println(s"$lfu\n")

  def insert(i: Int) = {
    lfu.insert(i)
    println(s"insert($i)")
    print()
  }

  def lookup(i: Int) = {
    val item = lfu.lookup(i)
    println(s"lookup($i) = $item")
    print()
  }

  def removeLast() = {
    val last = lfu.removeLast()
    println(s"removeLast() = $last")
    print()
  }

  def --- = {
    println("--------")
    0
  }

}

