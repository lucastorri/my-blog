---
layout: post
title: "Syntax Diagrams from Rules, with Javascript, Scala, and Scala.js"
date: 2016-04-14 17:44:09 +0200
categories: update
---

A colleague of mine is working with natural language parsing and came up with some simplified grammar syntax to define expected input structures. The expected words/tokens are placed on a text file, and, over the weekend, I decided it would be interesting to have a way to visualize that and also how to implement that. TLDR, the code can be found [here](https://github.com/lucastorri/draw-diagram).

The rules on this grammar are defined through a simple language. Tokens are just regular words, but they can be made optional or part of a choice group. Optional tokens are enclosed in brackets (`[]`), and choices are inside parentheses (`()`) and with pipes (`|`) in between.

Words enclosed by less-than and greater-than signs (`<>`) will be drawn inside square boxes. They might have a special meaning, like referencing to another rule.

For example, a valid rule would be:

```
[please] (what is|what's) your <name>
```

Luckily, when looking for libraries that did the drawing part (which for me would be the most complex one), I was able to find [railroad-diagrams](https://github.com/tabatkins/railroad-diagrams). It has a list of functions that allow you to define [Syntax diagrams](https://en.wikipedia.org/wiki/Syntax_diagram), and it will take of care of rendering them. And here I have to say, that's something really nice about Javascript: the amount of visualization libraries available these days.

Now, the missing part was to transform my colleague's format to the one used by *railroad-diagrams*...

Since the drawing library was in Javascript, I decided to use it as well. I sat down for a few minutes, trying to think how would I structure my script, but Scala's case classes kept coming to my mind, ending with me ranting about my dislikes about Javascript. But then I remembered that [Scala.js](https://www.scala-js.org/) exists and decided to give it a try. Scala.js is a transpiler from pure Scala code to Javascript, like you would have with Typescript, Coffeescript, and so on.

I was so convinced about using Scala that I decided to implement it first, and then try to make it *Scala.js* compatible.

The way I implemented it is this: I tokenize the rules, parse them into an intermediate Scala representation using case classes, and them translate them to the *railroad-diagrams* functions.

After I was able to get it running and see that I obtained the right output, I started to make it *Scala.js* ready. The process was quite simple. I added the necessary *sbt* plugin, and created an entry point that extended `scala.scalajs.js.JSApp`. There, I created a function, annotated with `@JSExport`, that would receive the user input, pass it through my parser, and return it's output.

Unfortunately, I had used Java's `StringTokenizer`, and learned that *Scala.js* can't convert that, which makes sense. I wrote a small version of it in Scala, and voila: I had my Javascript code ready.

Lastly, I created an HTML page with a small script that glued everything together. The page has a text area where you can place your rules, and once that's is done, it will create and append the generated diagrams to the page. So, for the rule I gave before, we would have the following diagram drawn:

![Example's Syntax Diagram](/assets/2016-04-14-syntax-diagrams-from-rules-with-javascript-scala-and-scalajs/example.png)

Once again, the project can be found [on GitHub](https://github.com/lucastorri/draw-diagram). More details are available on its *README*.
