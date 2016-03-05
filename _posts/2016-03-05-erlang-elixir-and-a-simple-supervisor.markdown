---
layout: post
title: "Erlang, Elixir and a simple Supervisor"
date: 2016-03-05 19:51:42 +0100
categories: update
---

I'm learning more about [Elixir](http://elixir-lang.org/) and, consequently [Erlang](https://www.erlang.org/). So far, I found Elixir to be a great language, and it feels to me that Elixir is for the Erlang VM what Scala is for the JVM.

When experimenting with a new language, I always try to pick up some small problem in order to apply what I read. This time, I'm creating a [minesweeper game](https://github.com/lucastorri/elixir-mines) which, right now, you can only play it through *telnet*.

While trying to better understand how Supervisors work in Erlang, I created a small example that uses the Erlang API, rather than using the facilities provided by Elixir's `Supervisor` module.

I'm using the great [Learn You Some Erlang for Great Good!](http://learnyousomeerlang.com/supervisors) for reference. To make my example, I created two modules: `Sup` and `Wkr`, respectively representing supervisor and workers. You can copy and paste the following code on `iex`.

{% highlight elixir %}
{% raw %}
defmodule Sup do

  def start_link(types) do
    :supervisor.start_link(__MODULE__, types)
  end

  def init(types) do
    children =
      for {type, i} <- Enum.with_index(types) do
        {:"wkr_#{i}",
          {Wkr, :start_link, [i, type]}, :transient, 5000, :worker, [Wkr]}
      end

    {:ok, {{:one_for_one, 5, 60}, children}}
  end

end

defmodule Wkr do

  def start_link(id, arg) do
    IO.puts("Wkr #{id}: start #{arg}")
    pid = spawn_link(fn -> loop(id, arg) end)
    {:ok, pid}
  end

  defp loop(id, arg) do
    case arg do
      :unstable ->
        :timer.sleep(5000)
        IO.puts("Wkr #{id}: boom!")
        exit(:err)
      :stable ->
        IO.puts("Wkr #{id}: loop")
        :timer.sleep(10000)
        loop(id, arg)
    end
  end

end

{% endraw %}
{% endhighlight %}

`Sup` is started through the start_link function, which receives a list of type atoms. Each given type will become a new worker. The types are `stable` and `unstable`, meaning a worker that will always run without problems, and another that will crash.

It then calls the `start_link` function on the Erlang `supervisor` module, which in turn will call the `init` function of the given module. In this case, the given module is `Sup` itself through the `__MODULE__` keyword. `init` will build the spec that is used to describe the supervisor behavior and workers. More details about the parameters can be found [here](http://erlang.org/doc/man/supervisor.html#start_link-2).

For each worker, the function `Wkr.start_link` will be called, spawning their event loop.

For instance, you can start two workers, a good and a bad one, with the following code:

{% highlight elixir %}
{:ok, sup} = Sup.start_link([:unstable, :stable])
{% endhighlight %}

If you follow the printed messages, you'll see that the worker #0 will crash and be automatically restarted.

If you keep it running, eventually the whole supervisor will shutdown. That happens because I set the supervisor to "give up" in case there were 5 crashes on the last 60 seconds.

To prevent that, you can dynamically terminate the problematic child, and also delete its spec:

{% highlight elixir %}
:supervisor.terminate_child(sup, :wkr_0)
:supervisor.delete_child(sup, :wkr_0)
{% endhighlight %}
