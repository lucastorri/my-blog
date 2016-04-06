---
layout: post
title: "O(1) LFU Cache in Scala"
date: 2016-04-06 10:16:36 +0100
categories: update
---

Browsing the papers available on the [Papers We Love](https://github.com/papers-we-love/papers-we-love) community, looking for nothing in particular, I found an interesting [one](https://github.com/papers-we-love/papers-we-love/blob/master/caching/a-constant-algorithm-for-implementing-the-lfu-cache-eviction-scheme.pdf) entitled "An O(1) algorithm for implementing the LFU cache eviction scheme".

Contrary to Least Recently Used (LRU), a Least Frequently Used (LFU) keeps track of how many times a given entry was used, instead of just saying what item has the oldest use timestamp. I liked how simple the algorithm is, using a map and a pair of linked lists to maintain the entries' frequencies. It becomes very evident how those structures are employed once you see the figures available on the paper.

As an exercise, I decided to implement it using Scala as a mutable data structure.

I made it a generic class, where `Item` is the type being cached, and `Id` the type used as key for that item type. The ids are provided through an implementation of the `Identifier` interface. It allow you to create custom identification mechanisms. Most commonly though, you might just use a hash code of the item, but if you might be caching web content, it might make sense to use the resource's URL as the id.

You also specify the maximum number of items you are willing to accept. As an improvement, this could also be done more dynamic, by passing it an object that should tell if an item should be dropped or not. With that, you could decide to accept new items as long as you have free memory.

Furthermore, it could be changed to also provide a standard Scala API, like `Map`, for instance.

The class signature looks like the following:

{% highlight scala %}
class DoubleLinkedLFUCache[Item, Id](maxSize: Int)(implicit val identifier: Identifier[Item, Id]) {

  private val entries = mutable.HashMap.empty[Id, CachedItem]
  private val frequencyList = new FrequencyList(0)

  def insert(item: Item): Unit = //...

  def lookup(itemId: Id): Option[Item] = //...

  def removeLast(): Option[Item] = //...

  class FrequencyList(val frequency: Int) extends DoubleLinked[FrequencyList] { /*...*/ }

  class CachedItem(val item: Item) extends DoubleLinked[CachedItem] { /*...*/ }

}
{% endhighlight %}

As described on the paper, the collection supports 3 operation: insertions, lookups, and deletions. On the code, they map to the methods on the code above, following the aforementioned order. Inserting or looking up an item increases it frequency count. When you first insert an item, as expected, its frequency is equal to 1.

Internally, also based on the paper, items are kept on a linked list with all items that share the same frequency as that one (`CachedItem`). Those linked lists are kept on a second linked list (`FrequencyList`), where each entry is a list of items of frequency *n*. Finally, a map is used to associate a key to their respective link on the inner linked list (`entries`).

An example using a cache of integers would be:

{% highlight scala %}
val lfu = new DoubleLinkedLFUCache[Int, Int](size)

lfu.insert(1)
lfu.insert(2)
lfu.insert(3)

lfu.lookup(1)

lfu.lookup(2)

lfu.lookup(3)
lfu.lookup(3)
lfu.lookup(3)
lfu.lookup(3)

println(lfu)
/*
 * LFU
 *  |
 * [0]
 *  |
 * [2]-2-1
 *  |
 * [5]-3
 */
{% endhighlight %}

The full code can be found on [this gist](https://gist.github.com/lucastorri/138acfdd5f9903e5cf8e3cc1e7cbb8e7) or downloaded [here](/downloads/2016-04-06-o1-lfu-cache-in-scala/DoubleLinkedLFUCache.scala).
