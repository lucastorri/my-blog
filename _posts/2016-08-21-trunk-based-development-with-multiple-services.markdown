---
layout: post
title: "Trunk Based Development with Multiple Services"
date: 2016-08-21 18:53:55 +0200
categories: update
---

On a previous [post]({% post_url 2016-08-20-problems-with-branches-per-environment %}) I was detailing how we organize branches on my current job's projects and what are the problems we face with having a branch matching each of our environments (staging and production).

While researching alternatives to it, the one that really caught my interest is [Trunk Based Development](http://paulhammant.com/2013/04/05/what-is-trunk-based-development/), or TBD, in special because it shines where Branch per Environment seems to fall short. On TBD

> all developers [...] commit to one shared branch under source-control. [...]. Devs may, on their own dev workstations, do some multi-branch development (say with Git), but when they are “done” with a change or a bug fix, it should go back to the shared trunk.

As cited, TBD can still accommodate short lived branches, therefore allowing things like pull requests and code reviews to happen, which I regard as a very important practice. The post suggests one day, but I guess experimentation would drive the best trade off between code reviews and continuous integration. Additionally, something like [Branch by Abstraction](http://martinfowler.com/bliki/BranchByAbstraction.html) would fit quite well too.

The part where TBD is a bit more dangerous is on the fact that it allows developers to push broken code to the main branch. But that's the sort of thing that is solved by having a better process in place, including proper test automation, and requires full trust on your developers to achieve greater quality before checking in their changes.


With that in mind, some key ideas I would like to have in my projects would be:

* Each project has one single long lived branch, i.e. *master*;
* Code changes can be branched from *master*, but should be merged back frequently. They should contain unit tests, feature flags, and all other tools that might be required to meet quality standards;
* On each commit to master, a new artifact is built and a tag is created on the VCS, both using the CI server job number as the version;
* This code is deployed to the staging environment automatically;
* System tests are run and if the tests pass, the artifact is considered ready for deployment, a task that could be done with a single click;
* Code reviews can still be done on short lived branches, as described previously;
* If an urgent fix is necessary, a new temporary branch can be created from the VCS tag, the build pipeline changed to use it, and afterwards reverted to the original configurations;
* Branch by abstraction can be used in order to implements changes that are not conflicting (new code) and don't need to be merged so often.

In order to enable the previous points, here are a few assumptions and requirements I would count on:

* A bit more of automation is required;
* More trust on your test automation;
* Developers are able to run the service under development and its dependencies (on their machines or some other environment) in order to allow them to test and gain confidence on their changes;
* When using something like our *System tests*, code changes must be merged simultaneously to their corresponding tests.


## Using It With Multiple Services

When considering multiple services, though, it is important to notice that a given version of the system is the composition of individual services versions, and therefore, a version of the system can only be stable when this set of versions works correctly together. Consequently, when deploying multiple services the question becomes "how can I end up with a version set that works well together"?

One idea I was considering is akin to [`Gemfile.locks`](http://bundler.io/v1.5/gemfile.html) used in Ruby. A `Gemfile.lock` holds a snapshot of all of the gems and versions that were installed in a given project. With that, you are able to know the exact versions of the gems that were used and that for sure allow the application to work correctly. This same idea could be expanded to services.

Imagine having two deployment environments: staging and production, and where we have the capacity of listing the versions of all services installed on them. Let's also consider that code changes pushed to *master* are automatically deployed to staging, and whenever that happens, our end-to-end test suite is executed. If the tests are green after applying the updates, we can use that as a set of service versions that form a potentially deployable version of the system. A release to production would mean comparing the actual versions to this set, and update services that introduced newer versions. This set of versions also contains the version of the tests used, and a new release also means bringing these specific test suite to production. If one of the released components fails (startup, a basic health check, or the system tests), the release is rolled back and this set can be invalidated.

To give an example, consider the step-by-step scenario below, where each segment represents changes pushed to each service (columns) and result on new build numbers. I'm using two environments to demonstrate the idea, staging and production (rows). On this approach, *System tests* are just another deliverable that also goes through the pipeline and is promoted together with the rest of the services:

![System Versions Step-by-Step](/assets/2016-08-21-trunk-based-development-with-multiple-services/step_by_step_services_version_lock.png){: width="65%" style="margin: 0 auto; display: block; float: right;"}

1. Production and staging have the exact same versions of our services;
2. Someone pushes changes to `Service A` *master* without updating the *System tests* yet. That might break the build;
3. The changes containing the new expected behavior are applied to the *System tests*;
4. A refactoring of the code is made on `Service A`. It is pushed and as expected it doesn't break anything. We now have a valid version, but it doesn't mean that we need to put it in production;
5. Changes weren't tested properly and are added to `Service C`, breaking the build;
6. Changes on `Service C` are reverted (or quickly fixed), making the system stable once again;
7.  The current set of versions is pushed into the production, updating only the services that were changed. The System test are also pushed. If they work, the newer versions are promoted. Otherwise we rollback.

Moreover, the last good set of versions could be tracked, so even if staging is broken, we can use the latest known good set.

On my [next post]({% post_url 2016-08-23-services-version-lock-with-docker-and-jenkins %}) I'll demonstrate how to apply this idea by using Docker containers.


### Other References

* <https://www.thoughtworks.com/insights/blog/enabling-trunk-based-development-deployment-pipelines>
* <http://www.alwaysagileconsulting.com/articles/organisation-pattern-trunk-based-development/>
* <http://www.alwaysagileconsulting.com/articles/organisation-antipattern-integration-feature-branching/>
* <https://barro.github.io/2016/02/a-succesful-git-branching-model-considered-harmful/>
* <http://endoflineblog.com/gitflow-considered-harmful>
