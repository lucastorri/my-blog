---
layout: post
title: "Regex Escape Sequences"
date: 2016-03-20 22:52:37 +0100
categories: update
---
Sometimes, when writing code derived from some documentation, I end up copying a table or list from the document and trying to create classes, objects, etc, that reflect/mimic those properties.

While trying to implement [CoAP](https://tools.ietf.org/html/rfc7252#section-12.2) in Elixir,
it recently happened to me [once again](https://github.com/lucastorri/coapex). On this case, I wanted to transform the numbers and names on the following table to a map of integers to atoms.

{% highlight text %}
+--------+------------------+-----------+
| Number | Name             | Reference |
+--------+------------------+-----------+
|      0 | (Reserved)       | [RFC7252] |
|      1 | If-Match         | [RFC7252] |
|      3 | Uri-Host         | [RFC7252] |
|      4 | ETag             | [RFC7252] |
|      5 | If-None-Match    | [RFC7252] |
...
+--------+------------------+-----------+
{% endhighlight %}

Instead of manually editing entry by entry, I copy-and-past that to *Sublime Text*, and use multiple cursors and find-and-replace to modify it into the required code. And for that, regexes are of great help.

A while ago, when doing a similar task, I leaned about [escape sequences](http://perldoc.perl.org/perlreref.html#ESCAPE-SEQUENCES), which allows transformations of captured groups, like case conversion. Since I don't use them very often and tend to forget how to use them, I decided to post them here...

Some of the available sequences are:

{% highlight text %}
   \l  Lowercase next character
   \u  Titlecase next character
   \L  Lowercase until \E
   \U  Uppercase until \E
   \E  End modification
{% endhighlight %}

Here are some of the transformations you can achieve using it (instead of '\1' you might need to use '$1'):

| Text        |        Find       |    Replace    | Result      |
|-------------|:-----------------:|:-------------:|-------------|
| hElLo woRlD |      ([a-z]+)     |      \L\1     | hello world |
| hElLo woRlD |      ([a-z]+)     |      \U\1     | HELLO WORLD |
| hElLo woRlD |      ([a-z]+)     |     \L\u\1    | Hello World |
| hElLo woRlD | ([a-z]+) ([a-z]+) | \L\u\1 \U\l\2 | Hello wORLD |
| hElLo woRlD | ([a-z]+) ([a-z]+) |  \L\u\1 \E\2  | Hello woRlD |
| hElLo woRlD |  ([a-z]+)([a-z])  |    \L\1\U\2   | hellO worlD |


&nbsp;


For the given table example, the steps I did were:

1. Remove unnecessary lines (`⌘+X` with no selection);
2. `Ctrl+Shift-{Up/Down}` or `Alt+Click-drag` to select all the lines, remove unnecessary columns, and put `=>` and `:` between values;
3. Replace `-` with `_`;
4. Convert characters to lowercase with `([A-Z])` to `\L\1`;
5. Ident and add Map syntax (`%{}`).

{% highlight text %}
## 1
|      0 | (Reserved)       | [RFC7252] |
|      1 | If-Match         | [RFC7252] |
|      3 | Uri-Host         | [RFC7252] |
|      4 | ETag             | [RFC7252] |
|      5 | If-None-Match    | [RFC7252] |

## 2
1 => :If-Match,
3 => :Uri-Host,
4 => :ETag,
5 => :If-None-Match

## 3
1 => :If_Match,
3 => :Uri_Host,
4 => :ETag,
5 => :If_None_Match

## 4
1 => :if_match,
3 => :uri_host,
4 => :etag,
5 => :if_none_match

## 5
%{
  1 => :if_match,
  3 => :uri_host,
  4 => :etag,
  5 => :if_none_match
}
{% endhighlight %}

This can probably be done with fewer steps, i.e. using a single regex to transform the lines. However, I prefer to use small steps because it is easier to rationalize about them and see the ongoing progress.
