---
layout: post
title: "Useful UNIX Signals (SIGQUIT, SIGSTOP, SIGCONT)"
date: 2016-03-09 16:55:41 +0100
categories: update
---

It is noticeable that I've been spending a bit of time with Elixir and Erlang, and therefore using *iex* and Erlang's shell a lot.

On REPLs like this, I'm used to press `ctrl+d` to exit them (*bash*, *scala*, *python*, *irb*, etc), and I was a bit annoyed that this doesn't work on *iex* and *erl*. While playing with [elixir-mines](https://github.com/lucastorri/elixir-mines), a minesweeper game that you can play through telnet, I ended up learning another shortcut that can be used in these cases...

When using the telnet command, you can use `ctrl+]` to escape a session, taking you to telnet's prompt (which, BTW, also supports `ctrl+d`). By mistake, I tried to do the same thing on *iex*, but pressed `\` instead of `]`. Turns out `ctrl+\` sends a *SIGQUIT* UNIX signal, which makes both *iex* and Erlang to stop their process'.

I then wondered what would that make on other REPLs. This also works with *python* and *irb*, albeit returning a non-zero exit code. But on Scala's, something completely different happens. A *SIGQUIT*s causes the JVM to [generate a thread dump](https://access.redhat.com/solutions/18178).

Later on the week, while listening to a [podcast](http://blog.ipspace.net/2014/09/snabb-switch-deep-dive-on-software-gone.html), I also found out that [SIGHUP](https://en.wikipedia.org/wiki/SIGHUP), which originally was created to signal a process that a user physically hang up the modem (removing the phone from it, just so you know how old its origins are), is nowadays commonly used to signal a process that it should reload its configuration files.

Those learnings got me curious about how signals work at all. I found a nice overview on this [StackExchange article](http://unix.stackexchange.com/questions/80044/how-signals-work-internally/80052#80052) and [this post](https://major.io/2010/03/18/sigterm-vs-sigkill/). In a nutshell, *SIGINT* and *SIGKILL* will not reach the target process, but instead will be handled directly by the kernel. Other signals are sent to the target process, which in turn decides what to do with them (that means even ignoring them). BTW, you can get a brief overview of existing signals through `man signal`.

Digging a bit more, I found out about *SIGSTOP* and *SIGCONT*, and how they can be used to [pause and continue a process](https://major.io/2009/06/15/two-great-signals-sigstop-and-sigcont/). It might be useful for debugging purposes in the future. For instance, connect VisualVM to a running JVM, do a Heap dump, pause the process (so nothing changes in between), check what is going on, continue it.
