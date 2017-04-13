---
layout: post
title: "Scala Projects and Integration Tests"
date: 2017-04-13 22:26:00 +0200
categories: update
---

Sometimes when a service you are developing uses an external system, i.e. a RDMS like PostgreSQL, it might be interesting to use integration tests to validate the integration points. Thankfully, things are way easier nowadays with Docker, where one can simply spawn images for whatever dependencies are needed, run the tests, and then tear down the instances that were created.

Whenever I implemented this sort of tests in a Scala project, I would usually write a bash script to orchestrate the required steps. Fortunately [@hcwilhelm](https://github.com/hcwilhelm) and [@petterarvidsson](https://github.com/petterarvidsson) showed me a cool trick where we can instead use SBT for that. Here is an example of a `build.sbt` file for a project that depends on S3:

{% highlight scala %}
lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(libraryDependencies ++= dependencies)

val startS3 = TaskKey[Unit]("start-s3", "Start a local FakeS3 instance")

(startS3 in IntegrationTest) := {
  "docker run -d -p 4567:4567 --name fakes3-instance ignicaodigital/fakes3" !
}

(test in IntegrationTest) := {
  (test in IntegrationTest).dependsOn(startS3 in IntegrationTest).andFinally {
    "docker rm -f fakes3-instance" !
  }
}.value
{% endhighlight %}


The steps here are simple. We are using an existing Docker image for [`fake-s3`](https://github.com/jubos/fake-s3) and declaring a task called `startS3` that, once executed, spawns a new instance of that image. Then we redefined the `test` task on the `IntegrationTest` configuration to depend on the `startS3` task and, on termination, to stop and clean up the instance.

Now, all you need to execute your integration tests is to run `sbt it:test`.
