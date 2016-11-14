---
layout: post
title: "Integration testing for nginx Routes"
date: 2016-11-14 16:50:51 +0100
categories: update
---

If you are working with microservices, changes are that you might be using an [API gateway](http://microservices.io/patterns/apigateway.html). This pattern creates a single entry point for your services, where all client requests pass through and are then routed to the desired service.

In our case we are using [nginx](https://www.nginx.com/resources/wiki/), and its configuration looks similar to the following:

{% highlight text %}
{% raw %}
server {
  listen 7000;

  location / {
    proxy_pass {{ service_1 }};
  }
  location /something {
    proxy_pass {{ service_2 }};
  }
}
{% endraw %}
{% endhighlight %}

Parts that look like `{{ key }}` represent some string that will be replaced by a configuration management tool. That way we can dynamically configure some of the behavior, let's say, different DNS names for different environments (staging, production).

On the previous configuration file, we are opening the server on port 7000.

As described on its [documentation](http://nginx.org/en/docs/http/request_processing.html), nginx chooses a *location* to process a request, and it will match the most specific prefix given, regardless of the listed order. In the example above, any path starting with /something (i.e. `/something` or `/something/else`) would be proxied to `service_2`, any other request would fall back to `/`, or `service_1`.

Although nginx is definitely great at its job, it might be difficult to properly test the changes made in you configuration. As you increase the number of services that are behind your gateway and, therefore, increase the number of rules, how can you validate that your changes are working and/or not breaking other existing rules? Even if you have each downstream service running, it might be difficult to validate that your request were routed to the desired service.

As a small experiment, I create a project that automates the testing of your routes, and is able to inform if they are valid or not. It is available [on GitHub](https://github.com/lucastorri/nginx-route-testing).

It works by defining tests for each service, described using a simple YAML syntax:

{% highlight yaml %}
service: service_1
tests:
  - POST /
  - GET /hello
{% endhighlight %}

**service** defines the configuration key to be replaced on the nginx configuration, and **tests** is a list of requests that should hit the defined service.

Then the tests can be run with a python script, and the results are reported on the standard output:

{% highlight bash %}
python2 "$project_dir/test/test.py"
[  OK] GET / -> service_1
[  OK] POST /hello -> service_2
{% endhighlight %}

The python script performs the following actions:

1. parses all test files;
2. instantiates a fake HTTP server for each service;
3. renders the nginx configuration files in `src/servers` with the services described on these test files and their fakes;
4. starts nginx with the rendered configuration;
5. runs all declared tests (perform the described HTTP calls) and make sure that the correct one was hit for each request;
6. clean ups the environment and reports the results.


![nginx test tool overview](/assets/2016-11-14-integration-testing-for-nginx-routes/nginx_test_tool_overview.jpg)


A failure can be represented in two ways:

- Hitting the wrong service

{% highlight bash %}
[FAIL] POST /other -> service_2 [hit service_1 instead]
{% endhighlight %}

- Hitting none of the services

{% highlight bash %}
[FAIL] GET /gah -> service_1 [no hit]
{% endhighlight %}


This allows quicker and confident changes of your nginx routes, without waiting them to be deployed, or running a bunch of services and validating responses manually.

This, once again, is only an experiment, but demonstrates how this task could be automated without much coding at all (the current implementation is less than 250 code lines in python). Test requests are done sequentially, but could be made in parallel while passing a specific HTTP header to identify the test doing the request (i.e. `X-TEST-CODE`).
