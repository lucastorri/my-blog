---
layout: post
title: "Using xargs to Parallelize Tasks"
date: 2016-04-01 20:24:29 +0100
categories: update
---

I recently wrote a specialized crawler that was able to navigate a unknown page structure and look for "interesting" content using [SlimerJS](https://slimerjs.org/). The structure is unknown because we crawl different websites, none of them with common *id* or *class* attributes.

As a side note, I chose to use that instead of [PhantomJS](http://phantomjs.org/), because with the later I wasn't able to capture AJAX content.

When started, the crawler receives through the command line a few arguments, i.e. the website URL, together with other information that will help guiding it to the content we are interested on.

Since working with new windows in SlimerJS is a bit tricky, I decided that it would be better to spawn multiple copies of the *node* process for each of the crawled sites. At the same time, the list of websites we have can be a bit large (+1000), and spawning all those processes at once might be killer. It's good to remember that SlimerJS works as a regular browser, having to run Javascript code, render CSS, etc.

It was quite good to learn that you can use **xargs**, a command available in any UNIX system, to parallelize execution of a given command. On its manual you can find the following:

>     -P maxprocs
>             Parallel mode: run at most maxprocs invocations of utility at once.

To get it all together, I created a bash script similar to the following to run on a given moment at most `$parallelism_level` instances of the crawler:

{% highlight bash %}
#!/bin/bash

readonly parallelism_level=8
readonly input_file="$1"

crawl_site() {
  local url="$1"
  local arg_2="$2"
  #...

  #crawl
  npm run main -- \
    --homepage=$url #...
}

export -f crawl_site
cat "$input_file" | xargs -I % -P $parallelism_level bash -c 'crawl_site %'
{% endhighlight %}

The script expects an input file to be received, where each line contains all the information required to run the crawler. The function `crawl_site` is responsible for preparing and starting this new instance, passing to it any necessary arguments.

After exporting the function, we *cat* the input file and pipe it to *xargs* with the necessary flags. *xargs* then launches a new bash process that call our function and the current line content as argument. Since the passed line is not quoted, if the contents of your line are space separated, they will already become separated arguments on the receiving function.

As soon as one of the running crawlers exits, *xargs* will automatically launch a new one, till all the lines of the file are finally processed.
