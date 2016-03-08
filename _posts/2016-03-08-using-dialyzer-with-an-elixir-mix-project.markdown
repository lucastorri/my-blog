---
layout: post
title: "Using Dialyzer with an Elixir Mix Project"
date: 2016-03-08 15:47:53 +0100
categories: update
---

Starting with Elixir, and knowing that it is a dynamic language, I wondered why are there *[typespecs](http://elixir-lang.org/getting-started/typespecs-and-behaviours.html)*. Typespecs allow you to declare custom data types and typed function signatures. After reading [Programming Elixir](http://www.amazon.com/Programming-Elixir-Functional-Concurrent-Pragmatic/dp/1937785580) I learned that this information can be used by an Erlang tool called **Dialyzer**. By their own [definition](http://erlang.org/doc/apps/dialyzer/dialyzer_chapter.html):

> Dialyzer is a static analysis tool that identifies software discrepancies such as type errors, unreachable code, unnecessary tests, etc in single Erlang modules or entire (sets of) applications.

Before being able to use it, we need to build a [Persistent Lookup Table](http://erlang.org/doc/apps/dialyzer/dialyzer_chapter.html#id59082), or simply PLT. This file is used to store the result of an analysis, and serves as a cache of information about analyzed functions. You should build a PLT against the OTP applications you commonly use. Therefore, I'll build it using the elixir lib:

{% highlight bash %}
dialyzer --build_plt --apps erts /usr/local/Cellar/elixir/1.2.3/lib/elixir/ebin
{% endhighlight %}

I'm creating 3 modules to exemplify its use. `Blah` is the one with an typespec for the `blah` function. It expects to receive a number (integer or float), and returns a String. Bleh and Blih are users of that function. The former uses it wrongly, by passing a String instead of a number, and the later uses it correctly, passing the integer 1.

{% highlight elixir %}
defmodule Blah do

  @spec blah(number) :: String.t
  def blah(i) do
    to_string(i + 1)
  end

end

defmodule Bleh do

  def run do
    Blah.blah("bleh")
  end

end

defmodule Blih do

  def run do
    Blah.blah(1)
  end

end
{% endhighlight %}

After creating the files in your project, you can compile it and run the tool, checking then the reported errors.

{% highlight bash %}
mix compile
dialyzer _build/dev/lib/$project_name/ebin/
#   Checking whether the PLT /Users/lucastorri/.dialyzer_plt is up-to-date... yes
#   Proceeding with analysis...
# bleh.ex:3: Function run/0 has no local return
# bleh.ex:4: The call 'Elixir.Blah':blah(<<_:32>>) will never return since the success typing is (number()) -> any() and the contract is (number()) -> 'Elixir.String':t()
# Unknown types:
#   file:date_time/0
#   file:file_info/0
#  done in 0m0.28s
# done (warnings were emitted)
{% endhighlight %}

As expected, a warning was emitted for `Bleh`, but not for `Blih`.
