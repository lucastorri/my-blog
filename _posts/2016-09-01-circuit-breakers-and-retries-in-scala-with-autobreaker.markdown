---
layout: post
title: "Circuit Breakers and retries in Scala with autobreaker"
date: 2016-09-01 22:40:57 +0200
categories: update
---

Most of my work these days has been around backend systems written in Scala. Whenever I'm working on a project, one of the abstractions I often end using are [services](https://lostechies.com/jimmybogard/2008/08/21/services-in-domain-driven-design/).

In many cases, the methods on my services return a [Future](http://www.scala-lang.org/api/current/#scala.concurrent.Future). A `Future` in Scala is a way of representing asynchronous computations. They are handled in a non-blocking way and act as a placeholders for values that may not yet exist. Consider, for instance, values obtained when communicating with downstream systems (HTTP calls, repositories, etc). On those same cases, though, a series of other concerns start to manifest. For example, should I retry a failed call? And if doing so, ain't I actually stressing an already unhealthy downstream system?

On the book [Release It!](https://www.amazon.com/Release-Production-Ready-Software-Pragmatic-Programmers/dp/0978739213), the author introduces the concept of [circuit breakers](http://martinfowler.com/bliki/CircuitBreaker.html) as a software pattern to handle these precise problems. The idea is simple: if I had too many failures in a short period while doing a specific operation, I start to prevent further calls to that operation to be made, breaking my circuit and make it *open*. New calls would then fail, with an exception for instance, instead of doing the original call. After a few moments, I then gradually start to attempt calling it, and if they now seem to be working correctly, I move back to the *closed* state and let all requests to be forwarded.

A great implementation of this pattern is [Hystrix](https://github.com/Netflix/Hystrix). [Akka](http://akka.io/) also happens to have [their own implementation](http://doc.akka.io/docs/akka/current/common/circuitbreaker.html).

While looking for a generic way to put those in use, my attempt was to do it on a way that would not pollute existing code with this logic. To be honest, I don't want my service implementation to worry so much about it, as their focus should be about [doing the things that they are supposed to do](https://en.wikipedia.org/wiki/Separation_of_concerns) (communicate with other systems, etc).

That made me into creating a new library: [`autobreaker`](https://github.com/lucastorri/autobreaker). `autobreaker` is a convenience library that wraps your objects and protects all methods returning a `Future` with a circuit breaker. It also allows you to define rules of when to retry calls. It is based on [atmos](https://github.com/zmanio/atmos) and Akka's circuit breaker, using [Java's Proxy](https://docs.oracle.com/javase/8/docs/api/java/lang/reflect/Proxy.html) to intercept method calls.

With `autobreaker`, given a service implementation, you can simply do the following in order to start using circuit breakers:

{% highlight scala %}
import com.unstablebuild.autobreaker._

val service: MyService = new MyDownstreamService
val serviceWithCircuitBreaker: MyService = AutoBreaker.proxy(service)

serviceWithCircuitBreaker.doSomething("hello")
{% endhighlight %}

The usage of your object doesn't change at all, but given enough failing calls, they would start to fail fast and avoid calling the original code. Instead, calls to a open circuit would return a custom exception and log the problem:

{% highlight scala %}
// [warn] e.c.CircuitBreakerProxy - Attempt 1 of operation interrupted: akka.pattern.CircuitBreakerOpenException: Circuit Breaker is open; calls are failing fast
// akka.pattern.CircuitBreakerOpenException: Circuit Breaker is open; calls are failing fast
{% endhighlight %}


`autobreaker` also allows you to decide what exception types should be considered failures. This becomes handy when you use exceptions to represent any sort of problem that deviates from your happy path, i.e. validation problems. This adapts quite well with `Future`s, since they provide several features to handle exceptions. It is also very nice to rescue from failures, or map failures to, let's say, HTTP status codes using pattern matching. For instance, in a lot of my projects I create a common interface to represent these failures, and make my errors extend them:

{% highlight scala %}
sealed trait ApplicationError extends Exception with NoStackTrace
case object InexistentId extends ApplicationError
case object InvalidName extends ApplicationError
case object UnallowedOperation extends ApplicationError

val statusCode = error match {
  case InexistentId | InvalidName => 400
  case UnallowedOperation => 403
}
{% endhighlight %}

Therefore, you could define that an `InvalidName` is not an error that should cause a circuit breaker to open.

If you liked the idea, I invite you to take a better look on the project's [documentation](https://github.com/lucastorri/autobreaker).
