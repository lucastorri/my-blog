---
layout: post
title: "Erlang Link, Monitor, and Process Exit"
date: 2016-03-05 20:40:32 +0100
categories: update
---

There are two commons ways to follow up what is going on with a Erlang process: [links and monitors](http://learnyousomeerlang.com/errors-and-processes). The first one causes linked processes to be shutdown together if one of the linked processes terminates abruptly. The second, only notifies what happened to the other process.

To see that happening, let's create a new process (*outer*) that spawns a third one (*inner*). The last process can either exit forcefully when the explode flag is true, or normally otherwise. In both cases, this will only happen after 5 seconds.

{% highlight elixir %}
inner = fn explode ->
  IO.puts("inner start #{inspect self}")
  :timer.sleep(5000)
  if explode, do: exit(:err)
  IO.puts("inner stop")
end

outer = fn explode ->
  IO.puts("outer start #{inspect self}")
  pid = spawn_link(fn -> inner.(explode) end)
  :erlang.monitor(:process, pid)
  IO.puts("outer wait")
  receive do
    msg -> IO.puts("outer message #{inspect msg}")
  end
  IO.puts("outer stop")
end
{% endhighlight %}

While spawning a new process in the shell, lets also monitor it, so we can receive status messages.

When spawning the *outer* process with the flag as true, you can see that the *inner* process is also being spawned. After 5 seconds, they are not running anymore, as showed by the message that the shell received:

{% highlight elixir %}
{pid, ref} = spawn_monitor(fn -> outer.(true) end)
# outer start #PID<0.169.0>
# outer wait
# inner start #PID<0.170.0>
# {#PID<0.169.0>, #Reference<0.0.4.320>}
:erlang.process_info(pid)
# [current_function: ...]

# ... wait 5 seconds

flush
# {:DOWN, #Reference<0.0.4.320>, :process, #PID<0.169.0>, :err}
# :ok
:erlang.process_info(pid)
# :undefined
{% endhighlight %}

It is good to notice that when two processes are linked, and one of the exits normally, the other one is not brought down. That becomes evident when using the flag as false. The processes will be started in the same way as before, but *outer*, who is also monitoring *inner*, receives a message notifying that *inner* went down, and therefore gets out of the receive block and also exits normally.

{% highlight elixir %}
{pid, ref} = spawn_monitor(fn -> outer.(false) end)
# outer start #PID<0.175.0>
# outer wait
# inner start #PID<0.176.0>
# {#PID<0.175.0>, #Reference<0.0.4.339>}
:erlang.process_info(pid)
# [current_function: ...]
# inner stop
# outer message {:DOWN, #Reference<0.0.4.342>, :process, #PID<0.176.0>, :normal}
# outer stop

# ... wait 5 seconds

flush
# {:DOWN, #Reference<0.0.4.339>, :process, #PID<0.175.0>, :normal}
# :ok
{% endhighlight %}


By setting `trap_exit` to true, you can change the link behavior. With that, when using [:erlang.process_flag](http://erlang.org/doc/man/erlang.html#process_flag-2), exit signals arriving to a process are converted to messages. To see that in action, let's do a small change to the previous example. *inner* will always exit abruptly, and *outer* is not monitoring *inner* anymore, but instead trapping the exit signal.

{% highlight elixir %}
inner = fn ->
  IO.puts("inner start #{inspect self}")
  :timer.sleep(5000)
  exit(:err)
  IO.puts("inner stop")
end

outer = fn i ->
  IO.puts("outer start #{inspect self}")
  :erlang.link(i)
  :erlang.process_flag(:trap_exit, true)
  IO.puts("outer wait")
  receive do
    msg -> IO.puts("outer message #{inspect msg}")
  end
  IO.puts("outer stop")
end

{i, ref} = spawn_monitor(inner)
# inner start #PID<0.185.0>
# {#PID<0.185.0>, #Reference<0.0.4.302>}
{o, ref} = spawn_monitor(fn -> outer.(i) end)
# outer start #PID<0.187.0>
# outer wait
# {#PID<0.187.0>, #Reference<0.0.1.2>}
# outer message {:EXIT, #PID<0.185.0>, :err}
# outer stop
# flush
# {:DOWN, #Reference<0.0.4.302>, :process, #PID<0.185.0>, :err}
# {:DOWN, #Reference<0.0.1.2>, :process, #PID<0.187.0>, :normal}
# :ok
{% endhighlight %}

As you can see, the exit signal was converted to a message starting with the `:EXIT` atom.

One last thing is that you can force a process to be killed by sending the `:kill` atom to it:

{% highlight elixir %}
{i, ref} = spawn_monitor(inner)
# inner start #PID<0.198.0>
# {#PID<0.198.0>, #Reference<0.0.1.44>}
{o, ref} = spawn_monitor(fn -> outer.(i) end)
# outer start #PID<0.200.0>
# outer wait
# {#PID<0.200.0>, #Reference<0.0.1.49>}
:erlang.exit(o, :kill)
# true
flush
# {:DOWN, #Reference<0.0.1.49>, :process, #PID<0.200.0>, :killed}
# {:DOWN, #Reference<0.0.1.44>, :process, #PID<0.198.0>, :killed}
# :ok
{% endhighlight %}
