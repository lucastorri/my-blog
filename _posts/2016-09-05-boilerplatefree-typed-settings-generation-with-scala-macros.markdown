---
layout: post
title: "Boilerplate-free Typed Settings generation with Scala Macros"
date: 2016-09-05 20:33:40 +0200
categories: update
---

Java and Scala have plenty of configuration libraries available, from the standard basic [Properties](https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html), to more advanced ones like [Commons Configuration](https://commons.apache.org/proper/commons-configuration/) or Typesafe's [Config](https://github.com/typesafehub/config).

In general, though, I prefer to have an interface to define settings for different parts of my system, and have these libraries do their work behind the scenes.  For instance, if I have a class that acts as a client to another system (HTTP), I might have one settings class that can return the base URL, timeout, number of retries, etc. This helps on refactoring my code, finding usages of the values with an IDE, but also decouples details about how my settings files are organized. Unfortunately, there is a cost involved, even though small, of writing these classes.

Inspired on a Java library called [*owner*](https://github.com/lviggiano/owner) and as my first attempt with macros in Scala, I've decided to automate the step of writing those settings class. The result is now published as a library called [`settler`](https://github.com/lucastorri/settler) and is available on Sonatype's public repository.

With `settler`, given a configuration file like this:

{% highlight text %}
{
  str: "hello world",
  dbs: [
    { key: "k1", value: 123 },
    { key: "k2", value: 321 }
  ]
}
{% endhighlight %}

You can do the following:

{% highlight scala %}
import com.unstablebuild.settler._

trait AppSettings {
  def str: String
  def dbs: Set[DBSettings]
  def opt: Option[String]
  def optOrElse: String = opt.getOrElse("default")
}

trait DBSettings {
  def key: String
  def value: Int
}

// Pass you settings and trait to settler
val settings = Settler.settings[AppSettings](ConfigFactory.parseFile(myFile))

settings.str
// "hello world"

settings.dbs.map(db => s"${db.key}: ${db.value}")
// Seq("k1: 123", "k2: 321")

settings.opt
// None

settings.optOrElse
// "default"
{% endhighlight %}

With the defined traits, `settler` will generate the implementation for them, even for nested ones, handle type conversions and optional values.

It can be used out-of-the-box with [Typesafe's Config](https://github.com/typesafehub/config) or [Java Properties](https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html). Additionally, you can implement your own `ConfigProvider` and use whatever source you prefer (custom file formats, databases, Redis, HTTP calls, etc).

For further details, please take a look on the project's documentation [here](https://github.com/lucastorri/settler).
