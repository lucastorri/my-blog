---
layout: post
title: "Learning From My Mistakes: Building a Crawler"
date: 2015-12-02 23:25:23 +0100
categories: update
---
This post tells a story that I hope to use as a reminder for my future self. *TLDR*:

 * [KISS](https://en.wikipedia.org/wiki/KISS_principle);
 * Focus on the right things;
 * Don't be overconfident;
 * Avoid overengineering.

It goes like this...

About an year ago we started a new project. The goal was to extract places' information that were published on their websites. Let's say, what kind of food a given restaurant serves, or what is its phone number.

Naturally, we needed some data to start with, and so we built a quick and dirty crawler based on [crawler4j](https://github.com/yasserg/crawler4j). It was enough to download a couple hundred websites, allowing us to work on our problem, and quickly deliver a nice initial implementation.

But it was time to move on, and we needed to scale the crawler. We started to make plans, discussing about how to crawl millions of pages. Being overconfident from our recent success, we became greedy. At least I did...

We wanted a crawler that would do it all, plus prepare your morning coffee. It should make decisions online, re-crawl web pages every now and then, while avoiding unnecessary work, like downloading content twice, keeping versions of the downloaded URLs, respecting [politeness](https://en.wikipedia.org/wiki/Web_crawler#Politeness_policy), and have a bunch of configurable rules to decide what content to crawl based on different client needs.

Existing solutions like [Nutch](http://nutch.apache.org/) or [Heritrix](https://github.com/internetarchive/heritrix3) were tested, and we found them to be difficult to adapt, enough to make us think that the best option would be to write our own solution.

It might have been the case, but custom software ain't cheap, as we were reminded the hard way. The more complicated it is, the more resources it will take, not only to develop, but to test, deploy, maintain, and so on. Besides, it is very easy to be seduced by cool technology. Getting them to work isn't always so easy.

We then created a plan that seemed doable in paper, using trendy tech, and a few initial prototypes gave us positive feedback. But scaling that idea up on a reliable way proved to be a bit more complicated.

Although the problem is very interesting, we had to give up at some point. Not only some nasty issues started to show up, but our team was quite small (3 people), and we had less and less time to focus on the crawler. But more importantly, we forgot that our product wasn't that, but rather the systems that would process the fetched content.

Luckily, we realized the mistake in time, taking a step back and ending up with something even simpler than what we started with. The current solution, which can be found [here](https://github.com/rizsotto/Babywalk), uses plain *wget* with a Python wrapper. We still care about politeness, but less and less about the rest of the requirements. It is just too expensive to be smart.

Looking back, I would have given more chances to Nutch. It has been shown to scale well, and works on top of Hadoop, which can be used with [EMR](https://aws.amazon.com/elasticmapreduce/).

Nevertheless, I decided to register my learnings by writing my own crawler. You can have a look [on GitHub](https://github.com/lucastorri/moca). It was also the way I found to learn more about [Akka](http://akka.io/) cluster and persistence. It is written in Scala, and can be distributed across different machines. The current implementation uses PostgreSQL for saving state, and S3 for storing fetched files.
