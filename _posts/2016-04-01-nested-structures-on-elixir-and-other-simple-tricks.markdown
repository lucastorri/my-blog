---
layout: post
title: "Nested Structures on Elixir, and Other Simple Tricks"
date: 2016-04-01 20:04:07 +0100
categories: update
---

Following are a few tricks that have been very useful while using Elixir...

The [first](https://dockyard.com/blog/2016/02/01/elixir-best-practices-deeply-nested-maps) is one of the most helpful ones. `update_in` and `put_in` are macros that allow you to update nested structures with just a given path and a function, case you want to transform the current value, or a new value:

{% highlight elixir %}
data = %{
  nested: %{
    value: 7
  }
}

put_in(data.nested.value, 13)
# %{nested: %{value: 13}}

update_in(data.nested.value, fn old -> old * 3 end)
# %{nested: %{value: 21}}

new_key = :hi
put_in(data.nested[new_key], 23)
# %{nested: %{hi: 23, value: 7}}
{% endhighlight %}

&nbsp;

Using `with`, instead of having several `case`s inside each other, you can pattern match multiple return values. It returns either the result of the `do` block, if everything happened as expected, or the first faulty return value. It is very useful when creating multiple resources, like chaining calls to `GenServer.start_link`:

{% highlight elixir %}
v1 = {:ok, 3}
v2 = {:ok, 5}

with {:ok, a} <- v1,
     {:ok, b} <- v2,
     do: {:ok, a + b}
# {:ok, 8}


v2 = {:error, 5}

with {:ok, a} <- v1,
     {:ok, b} <- v2,
     do: {:ok, a + b}
# {:error, 5}
{% endhighlight %}

&nbsp;

While looking at [Cauldron](https://github.com/meh/cauldron)'s source code, I learned how you can use pattern matching to get the last part of a string:

{% highlight elixir %}
"/a/b/" <> something = "/a/b/hello"
something == "hello"
# true
{% endhighlight %}

&nbsp;

When reviewing the [Getting Started](http://elixir-lang.org/getting-started/introduction.html) section, I remembered that you can check if an element belongs to a collection with:

{% highlight elixir %}
1 in [1, 2, 3]
{:a, 1} in %{ a: 1, b: 2, c: 3 }
{% endhighlight %}

I don't use this last very often, because usually pattern matching provides a better/cleaner solution.
