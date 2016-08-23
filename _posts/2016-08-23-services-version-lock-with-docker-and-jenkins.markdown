---
layout: post
title: "Services Version Lock with Docker and Jenkins"
date: 2016-08-23 19:04:26 +0200
categories: update
---

On my [previous post]({% post_url 2016-08-21-trunk-based-development-with-multiple-services %}), I was introducing the idea of having some sort of file that describes a set of versions of your services that works well together. It borrows the solution from Ruby's `Gemfile.lock`s and applies them to services, micro-services, or whatever SOA-like approach you might be using. It relies on a series of automated end to end tests that allow you to quickly and confidently verify the quality of your system. This version set locks can subsequently be used when releasing a new version of your system.

In order to demonstrate how it could be used in practice, I've implemented a small proof of concept of the idea. I'm using Docker images as the deliverables of my services, plus Jenkins and some Python scripts to orchestrate deployment and validation of images. To keep it simple, I've kept the number of components to a minimum. In the end, I have only 3 git repositories, each one representing one of the components required:

  * `service-a`: a [Play](https://www.playframework.com/) Scala application containing only the original generated template. A request to the service will return a 200 status code and a "welcome" message;
  * `e2e-tests`: a Python script that will try to connect to the `service-a` and validate that the correct status code is returned and the response contains the welcome message;
  * `deploy`: contains scripts that deploy my services, runs the `e2e-tests` against them, and saves the service version set if everything works correctly.

Also, for the experiment, I've decided to have all the parts running on my machine. So I have my own Docker registry, git server, Jenkins, etc. For instance, you can spawn yours components with the following commands:

{% highlight bash %}
# Jenkins
docker run -p 8080:8080 -p 50000:50000 jenkins

# Docker Registry
docker run -p 5000:5000 --name local-registry registry:2

# Git Server
git daemon --reuseaddr --base-path=. --export-all --enable=receive-pack --verbose

# Version Server
docker run -it -p 0.0.0.0:8151:8151 -v "$PWD":/usr/src/myapp -w /usr/src/myapp python:2.7.12-alpine ./main.py
{% endhighlight %}

Before we dig into details of how things are put together, let's take a look on the following image and see how parts interact with each other:

![Components Interaction](/assets/2016-08-23-services-version-lock-with-docker-and-jenkins/components_interaction.jpg)

As you can see, `service-a` and `e2e-tests` have their own Jenkins pipelines and are publishing images to the local Docker registry. Whenever a developer pushes any changes to the master branch of each git repository, Jenkins will pick those and run them through the respective pipeline. If they run successfully, they also trigger a third Jenkins job, which is responsible for deploying newer versions of each individual service and validating if they work correctly together.

I'm using [Multibranch Pipelines](https://jenkins.io/blog/2015/12/03/pipeline-as-code-with-multibranch-workflows-in-jenkins/) on Jenkins, which also allow me to define the steps on each build through [`Jenkinsfile`s](https://jenkins.io/doc/pipeline/jenkinsfile/). To give you an example, here is the `Jenkinsfile` defined for the `e2e-tests`:

{% highlight groovy %}
node {

  try {

    stage 'Checkout'

      checkout scm

    stage 'Tag'

      sh "git tag -a 'build-${env.BRANCH_NAME}-${env.BUILD_NUMBER}' -m 'Build #${env.BUILD_NUMBER} ${env.BRANCH_NAME}'"
      sh 'git push origin --tags'

    stage 'Prepare'

      sh 'rm -rf out'
      sh 'mkdir out'
      sh 'cp main.py requirements.txt Dockerfile out'

    stage 'Docker'

      String imageName
      if (env.BRANCH_NAME == 'master') {
        imageName = "192.168.50.1:5000/e2e-tests:${env.BUILD_NUMBER}"
      } else {
        imageName = "192.168.50.1:5000/e2e-tests:${env.BRANCH_NAME}-latest"
      }

      sh "docker build -t '${imageName}' out"

      if (env.BRANCH_NAME == 'master') {
        sh "docker tag ${imageName} 192.168.50.1:5000/e2e-tests:latest"
      }

    stage 'Staging'

      if (env.BRANCH_NAME == 'master') {
        build 'deploy/master'
      }

  } catch (err) {
    currentBuild.result = 'FAILURE'
    throw err
  }

}
{% endhighlight %}

Each stage specifies commands that are performed on each step of the build. For example, you can see that on the *Docker* stage, we create an image using the build number and tag it as *latest*, that is, in case we are building the master branch. Because Multibranch Pipelines automatically create new pipelines whenever you create a new branch, Jenkins will consequently test those branches and also create docker images from them. When building a branch other than master, we tag it as `${env.BRANCH_NAME}-latest`, so we can differentiate them and use it for test purposes.

The `Jenkinsfile` for `service-a` looks very similar to the one present above, with the exception that the unit tests are executed before trying to create a tag on the VCS or create a new docker image with [sbt](http://www.scala-sbt.org/sbt-native-packager). Therefore, they are our first line of defense against broken services, given that if they don't pass, no artifacts will be created.

On the next block you are able to see an example of the resulting images on my registry. For the sake of readability, I'm keeping only the first 4 digits of the image ids. You might also notice that there are no images for `service-a` with the tag numbers 19 and 20. Since the unit tests on those specific build numbers failed, nothing was published.

{% highlight bash %}
» docker images --no-trunc
REPOSITORY                    TAG      IMAGE ID      CREATED           SIZE
...
192.168.50.1:5000/service-a   21       sha256:4151   13 minutes ago    736.8 MB
192.168.50.1:5000/service-a   latest   sha256:4151   13 minutes ago    736.8 MB
192.168.50.1:5000/service-a   14       sha256:285f   11 days ago       736.8 MB
192.168.50.1:5000/service-a   15       sha256:285f   11 days ago       736.8 MB
192.168.50.1:5000/service-a   16       sha256:285f   11 days ago       736.8 MB
192.168.50.1:5000/service-a   17       sha256:285f   11 days ago       736.8 MB
192.168.50.1:5000/service-a   18       sha256:285f   11 days ago       736.8 MB
192.168.50.1:5000/service-a   10       sha256:3e93   11 days ago       736.8 MB
192.168.50.1:5000/service-a   11       sha256:3e93   11 days ago       736.8 MB
192.168.50.1:5000/service-a   12       sha256:3e93   11 days ago       736.8 MB
192.168.50.1:5000/service-a   13       sha256:3e93   11 days ago       736.8 MB
192.168.50.1:5000/service-a   9        sha256:3e93   11 days ago       736.8 MB
...
192.168.50.1:5000/e2e-tests   7        sha256:0b61   5 minutes ago     78.93 MB
192.168.50.1:5000/e2e-tests   latest   sha256:0b61   5 minutes ago     78.93 MB
192.168.50.1:5000/e2e-tests   6        sha256:30b2   23 minutes ago    78.93 MB
192.168.50.1:5000/e2e-tests   5        sha256:e95e   9 days ago        78.93 MB
...
{% endhighlight %}

![Failed 19 and 20](/assets/2016-08-23-services-version-lock-with-docker-and-jenkins/service_a_failed_19_and_20.png)

Finally, on the *Staging* stage, each pipeline will trigger the deployment pipeline. `deploy` is a single Python 3 script, using [docker-py](https://github.com/docker/docker-py/) to work with Docker. On this case, my test environment is simply my machine. The complete script is available [here](/downloads/2016-08-23-services-version-lock-with-docker-and-jenkins/deploy/main.py), but I'll highlight the main parts of it in order to explain the process:

{% highlight python %}
test_container = '192.168.50.1:5000/e2e-tests:latest'

services = [
  '192.168.50.1:5000/service-a:latest'
]

port_mappings = {
  '192.168.50.1:5000/service-a:latest': { 9000: ('0.0.0.0', 9091) }
}

#...

if __name__ == '__main__':
  new_versions = latest_versions()
  redeploy(select_updatable(deployed_versions(), new_versions))
  test_version = test()
  new_versions.update({ test_container: test_version })
  save_versions(new_versions)
{% endhighlight %}

At first, the script defines a list of our services, meaning their docker image names and tags. You can optionally define a set of port mappings that your image requires. Since the `e2e-tests` is a bit special, it is declared on a separate variable.

Once the script starts, it first tries to figure out what are the latest versions of the declared services (`docker images`). It then gets the versions of the services currently running (`docker ps`), finally comparing them and selecting the ones that can be updated. This set of updatable services is redeployed (`docker kill` followed by `docker run`) and the latest `e2e-tests` is executed. If the tests pass, the version of the test image is added to the set and it is uploaded to the Version Server. If not, the build fails and the current set is ignored.

![Failed end to end tests](/assets/2016-08-23-services-version-lock-with-docker-and-jenkins/failed_e2e_tests.png)

The [Version Server](/downloads/2016-08-23-services-version-lock-with-docker-and-jenkins/version-server/main.py) is very boring. It contains endpoints to allow publishing version sets, retrieving the latest, or the full history of version sets.

For instance, <http://localhost:8151/> will return the latest valid version set, like this:

{% highlight json %}
{
  "192.168.50.1:5000/service-a:latest": "sha256:4151",
  "192.168.50.1:5000/e2e-tests:latest": "sha256:0b61"
}
{% endhighlight %}

<http://localhost:8151/history>, on the other hand, returns the history of valid version sets, being the last element the newest one:

{% highlight json %}
[
  ...
  {
    "192.168.50.1:5000/service-a:latest": "sha256:285f",
    "192.168.50.1:5000/e2e-tests:latest": "sha256:e95e"
  },
  {
    "192.168.50.1:5000/service-a:latest": "sha256:285f",
    "192.168.50.1:5000/e2e-tests:latest": "sha256:30b2"
  },
  {
    "192.168.50.1:5000/service-a:latest": "sha256:4151",
    "192.168.50.1:5000/e2e-tests:latest": "sha256:30b2"
  },
  {
    "192.168.50.1:5000/service-a:latest": "sha256:4151",
    "192.168.50.1:5000/e2e-tests:latest": "sha256:0b61"
  }
]
{% endhighlight %}

&nbsp;

And there you have. If now you decide to release a new version of your system, you already have a set of service versions as reference. You know that they can be started and that the composing services and their versions will work together. You might think of it as some sort of bill of materials for your system. You can even use that to generate [docker compose](https://docs.docker.com/compose/overview/) definitions, or other specification that can be used to describe your deployment.

Once in production, the `e2e-tests` can be executed constantly, let's say every 10 minutes, in order to detect other issues that might come up during the system's life cycle.
