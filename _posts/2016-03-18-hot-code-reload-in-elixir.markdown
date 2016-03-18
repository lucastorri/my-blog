---
layout: post
title: "Hot Code Reload in Elixir"
date: 2016-03-18 17:44:55 +0100
categories: update
---

One of the nice features of Erlang is the possibility of [replacing](http://learnyousomeerlang.com/relups) a piece of code with a newer version while the application is still running. This means you can release new versions of your software without any downtime.

Bellow is a simple example created to help me understand it. It is a simple OTP GenServer that just increments a counter. The commented parts are going to be our "upgrade", modifying the module version (`@vsn`) and its behavior to increment from 1 by 1 to, 2 by 2.

{% highlight elixir %}
defmodule Inc do
  use GenServer

  @vsn "1"
  # @vsn "2"

  def start_link do
    GenServer.start_link(__MODULE__, [])
  end

  def init(_) do
    IO.puts("started")
    {:ok, 0}
  end

  def handle_call(:inc, _from, total) do
   new_total = total + 1
   # new_total = total + 2
   IO.puts("[#{@vsn}] new total: #{total} => #{new_total}")
   {:reply, new_total, new_total}
  end

  def handle_call(:total, _from, total) do
    IO.puts("total: #{inspect total}")
    {:reply, total, total}
  end

  def code_change(old_vsn, state, extra) do
    IO.puts "New version. Moving out of #{old_vsn} #{inspect extra}"
    {:ok, state}
  end

  def inc(i) do
    GenServer.call(i, :inc)
  end

  def get(i) do
    GenServer.call(i, :total)
  end

end
{% endhighlight %}

Let's suppose the code above is saved to a file called `inc.ex`, and you open *iex* on the same directory the file is, here are the steps taken to create an initial version, do our changes, and perform an upgrade:

{% highlight elixir %}
c "inc.ex"
# [Inc]
{:ok, pid} = Inc.start_link
# started
# {:ok, #PID<0.65.0>}
Inc.inc(pid)
# [1] new total: 0 => 1
# 1

### Do code changes

:sys.suspend(pid)
# :ok
c "inc.ex"
# inc.ex:1: warning: redefining module Inc
# [Inc]
:sys.change_code(pid, Inc, "1", [:hello])
# New version. Moving out of 1 [:hello]
# :ok
:sys.resume(pid)
# :ok
Inc.inc(pid)
# [1] new total: 1 => 3
# 3
{% endhighlight %}

As you can see, the value is now changing in increments of 2.

A better option to perform a hot upgrade, specially when deploying to production, might be to use [exrm](https://hexdocs.pm/exrm/extra-upgrades-and-downgrades.html).
