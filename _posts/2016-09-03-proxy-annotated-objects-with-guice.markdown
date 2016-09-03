---
layout: post
title: "Proxy annotated objects with Guice"
date: 2016-09-03 14:40:32 +0200
categories: update
---

If you read my [previous post]({% post_url 2016-09-01-circuit-breakers-and-retries-in-scala-with-autobreaker %}), you might have learned about [`autobreaker`](https://github.com/lucastorri/autobreaker), a library that allows easily integrating circuit breakers in your code.

In some of my projects I have been using the [Play framework](https://www.playframework.com/), and so I was looking how to smoothly introduce `autobreaker` in those projects. Play uses [Guice](https://github.com/google/guice) as its default dependency injection framework. With the help of custom annotations, I've created a Guice extension to wrap objects with `autobreaker`. The result allows you to do the following:

{% highlight scala %}
trait MyService { /*...*/ }

@WithCircuitBreaker
class MyServiceImplementation extends MyService { /*...*/ }

class MyModule extends AutoBreakerModule {
  override def setup(): Unit = {
    bind(classOf[MyService]).to(classOf[MyServiceImplementation])
  }
}
{% endhighlight %}


To make it clear, the concrete implementation of `MyService`, `MyServiceImplementation`, will be `autobreaker`-enabled™ once injected in another class.

In order to accomplish this, I'm using Guice's service provider interface ([SPI](https://github.com/google/guice/wiki/InspectingModules)). Guice does allows methods to be intercepted through [AOP](https://github.com/google/guice/wiki/AOP). Implementations of `MethodInterceptors` can be defined, and are executed whenever a matching method is invoked. According to the documentation

> Since interceptors may be applied to many methods and will receive many calls, their implementation should be efficient and unintrusive.

Unfortunately, I wasn't interested on individual methods, but whole objects, where each would have some shared logic. In this case, a common circuit breaker for the full instance. Besides that, `autobreaker` is a generic framework already using [Proxy](https://docs.oracle.com/javase/8/docs/api/java/lang/reflect/Proxy.html), and I didn't want to reinvent the library in order to make it work with Guice.

Therefore, I decided to provide conveniences to transform modules on demand. Given an existing module, the helper code can iterate over the existing bindings and transform them case necessary. If the binding is not annotated, it simply adds it to the resulting module. Otherwise, we replace it with another binding that uses a provider. The provider, in turn, receives the injector, so it can get the originally binded instance, and return it wrapped on `autobreaker`. In a high level, the code looks like the following:

{% highlight scala %}
def prepare(module: Module): Module = {

  val elements = Elements.getElements(module).flatMap {

    case linked: LinkedKeyBinding[_] if needsCircuitBreaker(linked.getLinkedKey.getTypeLiteral.getRawType) =>
      overwrite(linked, new LinkedProvider(
        linked.getLinkedKey,
        annotationIn(linked.getLinkedKey.getTypeLiteral.getRawType)))

    // ...

    case other =>
      Seq(other)

  }

  Elements.getModule(elements)
}
{% endhighlight %}

The full implementation can be found [on GitHub](https://github.com/lucastorri/autobreaker/blob/master/guice/src/main/scala/com/unstablebuild/autobreaker/guice/AutoBreakerGuice.scala), and works with linked key bindings and concrete instances. Furthermore, I've created an extension to allow wrapping bindings more easily. The result is the code introduced on the beginning of the post.

Nonetheless, if one doesn't want to use `AutoBreakerModule`, the helpers can be used directly by doing this:

{% highlight scala %}
class MyModule extends AbstractModule {
  override def configure(): Unit = {
    install(AutoBreakerGuice.prepare(new BindingsModule))
  }
}

class BindingsModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[MyService]).to(classOf[MyServiceImplementation])
  }
}
{% endhighlight %}
