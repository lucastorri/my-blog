---
layout: post
title: "_drafts in Submodule, Tweet on Updates"
date: 2016-04-03 19:53:51 +0200
categories: update
---
On one of the first posts here in this blog, I detailed how [I write and publish new content]({% post_url 2015-11-15-automatically-build-and-deploy-the-blog %}).

One of the open points was how/where to keep drafts. I honestly don't want to make them public while they are WIP. Sometimes I just have empty files, using their names as a reminder of an idea I would like to expand or take note later. Others, they are just very random notes, that would not make any sense to someone other than me.

One of the ideas was to cypher these files and add to the repo like the rest of the files, similar to what I already do to the environment variables. However, I believe that would be a bit cumbersome.

Instead, I chose to keep them on a separate private git repo, and refer to it as a submodule on the main one. With that, the `_drafts` are isolated from the other files and maintain an independent git history of their own.

When I clone the main repo, all I need to do is a `git submodule init` followed by `git submodule update`.

&nbsp;

I also wanted to tweet new posts whenever there was an update. I looked for *Jekyll* plugins that might do that, but found none. Instead, I read somewhere about this great idea of using [IFTTT](https://ifttt.com/). With a new simple recipe, whenever a new feed item shows up on my [feed.xml](http://www.unstablebuild.com/feed.xml), then a new tweet will be done.

If everything goes well, you should see a new tweet with a link to this post on [my account](https://twitter.com/lucastorri).
