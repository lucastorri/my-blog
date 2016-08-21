---
layout: post
title: "Problems with Branches per Environment"
date: 2016-08-20 22:51:00 +0200
categories: update
---


At [my current job](https://www.relayr.io/), the source code of our projects are organized into two central git branches, namely *master* and *develop*. Whenever a developer starts working on a ticket, he or she will branch from *develop* using a short description of the changes as the branch name. Once changes are ready, a pull request is open (GitHub) and, if nothing bad pops up, merged into *develop*. From time to time, a new pull request is created to bring changes that are sitting in *develop* to *master*.

Each of these branches represents an environment. *Develop* contains the code that is deployed to our staging environment, and *master* the one in production. Once the code in *develop* is merged to *master*, a Jenkins job is manually triggered to deploy it into production.

Most of our services are using either Scala or Node.js. We also have a special project using Python 3, called *System tests*. This project provides (almost) end-to-end tests, but without touching the UI. Instead, they exercise our services through their HTTP APIs. Yet another project provides [Ansible](https://github.com/ansible/ansible) and other scripts that help deploying things. It also has both branches.

&nbsp;

I've been trying to research what are the advantages of this branching setup. In general, I've seen the name *Branch per Environment* (BpE) being used. The branching organization resembles [git flow](http://nvie.com/posts/a-successful-git-branching-model/), but mostly only on the name of the branches, since the original post doesn't mention using *develop* and *master* to directly reflect the staging and production environments.

This mechanism seems to be a straightforward solution when you have a small team, where changes don't happen too frequently, and it is easy to coordinate with all involved parts. But once you grow your number of services and developers touching these code bases, deployments become more challenging, as we have currently been experiencing.

Proponents of BpE declare that one of the technique's advantage is that bugs on production can be fixed directly on *master*. Although that's possible, I believe it results on more issues than actual benefits. For instance, who guarantees that the fix is good? Shouldn't it follow the process of being tested on a staging environment and then finally being submitted to production? If it is a major bug, why wasn't it found before on any other environment (which seems to point to a problem on the process mostly) and why not rollback the release?

Another statement is that it provides naming convention for branches. I consider that yet another poor argument. On any work flow defined by a team it might be necessary to define at least a standard for names used on the feature branches. Besides that, any version control system has a well known and defined main branch out-of-the-box (*master*, *trunk*, etc).

One issue that particularly annoys me is that, since each branch creates artifacts that go to different repositories, two identical pieces of code might end up having completely different versions. That's not only confusing, but also means that the code that will eventually run in production may not match 100% the code run during testing. This might happen, for instance, when you have your dependencies defined by major and minor versions, but not [patches](http://semver.org/#spec-item-6), or when working with compiled languages, where the compiler might end up emitting different outputs on different runs.

Furthermore, having those two branches requires coordinating multiple projects in order to make a release. Once changes for a given project are being merged from *develop* to *master*, it is also necessary to track and know what other projects (and depend or are a dependency of the first) have newer versions and need to be merged as well.

What I've been seeing while using BpE is that we end releasing big chunks of code, instead of smaller, more granular, releases. This increases the time necessary for releasing features, amounts to more coordination between releases, and therefore increases the cost of a release. The fact that you have your production code on a isolated somewhere on a different branch just provides a false feeling of security and stability, while in reality it means possible future nightmares just waiting to reach *master*. Furthermore, depending on how long your feature branches live, another issue might come up. Having long lived branches means postponing code integration and therefore delaying conflicts, increasing feedback loops and, complicating code refactoring.

The commit history is yet another victim of the technique (and of branching in general), as it becomes completely convoluted:

![Merge Branches Nightmare](/assets/2016-08-20-problems-with-branches-per-environment/branch_merge_nightmare.png)

The above image was generated with:

{% highlight bash %}
git log --graph --pretty=format:'%C(yellow)%h %Cblue%aN %Cgreen%ar %Creset%s'
{% endhighlight %}

&nbsp;

Enough with the bashing...

On a next post I will write about a possible alternative to *BpE*.
