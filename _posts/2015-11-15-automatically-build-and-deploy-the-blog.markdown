---
layout: post
title: "Automatically Build and Deploy the Blog"
date: 2015-11-15 19:06:05 +0100
categories: update
---
In order to simplify my life while maintaining this blog, I was looking for the simplest way to let me focus just on the writing, and have all other steps (build, publish, ...) happen automatically.

As you might have noticed, I'm using [Jekyll](http://jekyllrb.com/) as my base platform, which generates an static website from my markdown files. Jekyll's website has sections on *Deployment methods* and *Continuous Integration*, both quite helpful. They made me opt for keeping sources on GitHub, while publishing the generated site to S3 using [s3_website](https://github.com/laurilehmijoki/s3_website) and [Travis CI](https://travis-ci.org/).

There are a few scripts I created that conduct the process, and that help me create new posts. They are also [available on GitHub](https://github.com/lucastorri/my-blog/tree/master/_scripts):

* `deploy`: has the commands that will be executed on Travis, building the site and publishing it to S3;
* `local-serve`: let me see locally how my changes will look like once they are published;
* `new-post`: creates a new post template on Jekyll based on the provided title. One of the interesting things it has is a bash function for slugifying the title.

In order to publish files to S3, *s3_website* expects, of course, valid AWS credentials. After creating a new IAM user, with access rights to S3 only, I modified the corresponding YAML file to look for two environment variables: `S3_KEY` and `S3_SECRET`. Since all my blog informations are in the open, I searched for a way to securely pass the credentials to the build process, and learned that Travis' command line client allows [encrypting](http://docs.travis-ci.com/user/encryption-keys/) variables using a public key created for your repository. You can do that as follows:

{% highlight bash %}
travis encrypt S3_KEY=blah --add
travis encrypt S3_SECRET=bleh --add
{% endhighlight %}


With that, the steps involved when writing a new post are:

1. I create a new file, and edit it using markdown;
2. I validate my changes locally;
3. When I decide that they are fine, I commit my changes, and push them to GitHub;
4. Travis CI starts a new build, compiling and publishing the pages to S3.


### TODO:

* I don't have a domain yet, and for now I just use the bucket endpoint;
* I allowed the new IAM user full S3 access. I want to check if I can limit it to the corresponding bucket only.
* I'm not sure what I'll do while working on a post draft. I might create a new branch, or use master, but somehow encrypt working files (I leave a lot of ideas open while writing, that I would not like to share).
