---
layout: post
title: "Option in Elixir"
date: 2016-03-07 00:05:13 +0100
categories: update
---
While trying to understand Elixir's [Protocols](http://elixir-lang.org/getting-started/protocols.html), I mimicked Scala's Option on a struct:

{% highlight elixir %}
defmodule Option do

  defstruct v: nil, set: false

  def some(v) do
    %Option{v: v, set: true}
  end

  def none do
    %Option{}
  end

  defimpl Inspect do
    import Inspect.Algebra

    def inspect(opt, opts) do
      if opt.set do
        concat ["#Some{", to_doc(opt.v, opts), "}"]
      else
        "#None"
      end
    end

  end

  defimpl Collectable do

    def into(original) do
      {original, fn
        opt, {:cont, v} -> Option.some(v)
        opt, :done -> opt
        _, :halt -> :ok
      end}
    end

  end

  defimpl Enumerable do

    alias Enumerable.List

    def reduce(opt, acc, fun), do: List.reduce(opt.set && [opt.v] || [], acc, fun)

    def member?(opt, val), do: {:ok, opt.set and opt.v === val}

    def count(opt), do: {:ok, opt.set && 1 || 0}

  end

end
{% endhighlight %}


That's what you get when using it:

{% highlight elixir %}
plus_1 = &(&1 + 1)

some_3 = Option.some(3)
# #Some{3}
none = Option.none
# #None

some_3 |> Enum.map(plus_1)
# [4]
none |> Enum.map(plus_1)
# []

some_3 |> Enum.count
# 1
none |> Enum.count
# 0

some_3 |> Enum.member?(1)
# false
some_3 |> Enum.member?(3)
# true
none |> Enum.member?(nil)
# false
none |> Enum.member?(1)
# false

for v <- Option.some(3), into: Option.none, do: v + 2
# #Some{5}
{% endhighlight %}

With that, the struct is converted to a string using the defined representation, can perform methods exposed by the Enum module, and can be used to collect values in for comprehensions.
