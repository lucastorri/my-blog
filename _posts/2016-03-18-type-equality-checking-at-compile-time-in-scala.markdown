---
layout: post
title: "Type Equality Checking at Compile Time in Scala"
date: 2016-03-18 16:02:38 +0100
categories: update
---
Scala has two types that can be used to express types constraints between two types: `=:=` and `<:<`.
[The former](http://www.scala-lang.org/api/rc2/scala/Predef$$$less$colon$less.html) is used to check
equality between two types, and [the later](http://www.scala-lang.org/api/rc2/scala/Predef$$$eq$colon$eq.html)
that a type *A* is a subtype of *B*.

There is some interesting topic on StackOverflow demonstrating how these [can be used](http://stackoverflow.com/questions/22714609/using-scala-implicitly-for-type-equality/22717040#22717040).

Just as an example, here are a few things you might be able to do:

{% highlight scala %}
trait X
class XX extends X
class Y

class A[T](t: T) {
  def theInt(implicit equiv: T =:= Int): Int = t.asInstanceOf[Int]
  def onlyX(implicit equiv: T <:< X): Unit = {}
  def onlyType[P](implicit equiv: T <:< P): Unit = {}
}


val ai = new A(1)
val as = new A("oi")

ai.theInt

//// won't compile
// as.theInt


val ax = new A(new XX)
val ay = new A(new Y)

ax.onlyX
ax.onlyType[X]

//// wont' compile
// ay.onlyX
// ay.onlyType[X]

ay.onlyType[Y]
{% endhighlight %}

You can also check using [abstract types](http://docs.scala-lang.org/tutorials/tour/abstract-types):

{% highlight scala %}
trait Typed {
  type T
}

class StringTyped extends Typed {
  override type T = String
}

//// won't compile
// implicitly[StringTyped#T =:= Int]

implicitly[StringTyped#T =:= String]
{% endhighlight %}

Alternatively, when limiting the usage of a method for just a given(s) type, you might use the same mechanism used on Scala's collections, i.e. `List.sum`. `sum`'s signature is the following:

{% highlight scala %}
def sum[B >: A](implicit num: Numeric[B]): B
{% endhighlight %}

With that, a specific implicit `Numeric` implementation for the desired type is injected. If there is no implementation for a given type, it will cause a compilation error. On the other hand, whenever you want to support a new type, you just need to expose a new implicit implementation for it. For instance:

{% highlight scala %}
trait Stringer[T] {
  def apply(t: T): String
}
class A[T](t: T) {
  def asString(implicit convert: Stringer[T]): String = convert(t)
}

implicit object IntStringer extends Stringer[Int] {
  def apply(i: Int): String = i.toString
}

val ai = new A(1)
val af = new A(3.14)

//// won't compile
// af.asString

ai.asString

implicit object IntStringer extends Stringer[Double] {
  def apply(d: Double): String = d.toString
}

//// works now
af.asString

{% endhighlight %}
