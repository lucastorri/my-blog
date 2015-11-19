---
layout: post
title: "S3 Hosted Website and DNS Configuration"
date: 2015-11-19 00:57:07 +0100
categories: update
---
While trying to set DNS configurations for the new blog's domain, I tried, at first, to use the bare domain. That means, using <http://unstablebuild.com>, instead of using any other subdomain, like <http://www.unstablebuild.com>.

A bare domain, also know as naked domain, zone apex, and root domain, can only be done through an `A` record, while a subdomain requires only a `CNAME` entry. `A` stands for *Address*, and it maps a host to one or more IP addresses. `CNAME`'s, on the other hand, specifies that the name in question is an alias of another domain, subdomain, or IP address.

As explained [before]({% post_url 2015-11-15-automatically-build-and-deploy-the-blog %}), I am hosting this blog on S3, meaning that if I want to use the bare domain, I would need to create an `A` record that points to AWS' IP address. When you share an IP address with other sites, which is my case, the server has no idea what specific host you are trying to reach. In order to get the right result, the [Host header](http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.23) must be used. 

With that in mind, I tried a quick test: do a `GET` request to one of the S3's public IPs while using my bucket name as the host. Turns out it works:

{% highlight shell %}
$ telnet 54.231.136.101 80
Trying 54.231.136.101...
Connected to s3-website-eu-west-1.amazonaws.com.
Escape character is '^]'.
GET / HTTP/1.1
Host: www.unstablebuild.com

HTTP/1.1 200 OK
x-amz-id-2: qeKFDA+wUWq53dMDOvwfaLJsH03BbwZWsumsBpC2ecbm5lsdAsPDX3IALjA7PfFo/rdy+dZCfuw=
x-amz-request-id: E29B096F368EE417
Date: Thu, 19 Nov 2015 13:10:54 GMT
Last-Modified: Wed, 18 Nov 2015 16:46:53 GMT
ETag: "707adabd8f1c4d6d3a9d2cfc3a48ffd3"
Content-Type: text/html; charset=utf-8
Content-Length: 5973
Server: AmazonS3

...
  <title>Unstable Build</title>
...
{% endhighlight %}


Unfortunately, as explained on their [forum](https://forums.aws.amazon.com/thread.jspa?messageID=217487), that's not very reliable:

> [...] S3 by its nature is subject to expansion and change as Amazon and its customers needs expand. S3 has multiple IP addresses in any case.
> So you cannot rely on the IP address, only the domain name. 


The solution would be to then use [Route 53](http://aws.amazon.com/route53/), as explained [here](https://aws.amazon.com/blogs/aws/root-domain-website-hosting-for-amazon-s3/). The task isn't that difficult, just bothersome.

For simplicity, I decided to change my mind and use `www`. Consequently, all I needed to do was [redirect](https://en.wikipedia.org/wiki/HTTP_301) the bare domain to the `www` subdomain, and create a `CNAME` record pointing to my S3 bucket endpoint. Plus, both services are already offered by my registrar.

After changing DNS configurations, I can now check that everything looks as expected:

{% highlight shell %}
$ dig unstablebuild.com
...
unstablebuild.com.  1799    IN  A   162.255.119.251
...

$ whois 162.255.119.251
...
Namecheap, Inc.
...

$ curl -v http://unstablebuild.com
...
> GET / HTTP/1.1
> Host: unstablebuild.com
>
< HTTP/1.1 302 Moved Temporarily
< Location: http://www.unstablebuild.com/
<
...

$ dig www.unstablebuild.com
...
www.unstablebuild.com.  1799    IN  CNAME   www.unstablebuild.com.s3-website-eu-west-1.amazonaws.com.
www.unstablebuild.com.s3-website-eu-west-1.amazonaws.com. 60 IN CNAME s3-website-eu-west-1.amazonaws.com.
s3-website-eu-west-1.amazonaws.com. 5 IN A  54.231.136.101
...

$ whois 54.231.136.101
...
Amazon Technologies Inc.
...
{% endhighlight %}

As an alternative, there are free services, like [wwwizer](http://wwwizer.com/naked-domain-redirect), that redirect requests from your naked domain to your `www` subdomain. All you need to do is point through an `A` record your bare domain to their IP address. You can see how that works without even changing your DNS configuration, by directly telnet to `wwwizer`'s address:

{% highlight shell %}
telnet 174.129.25.170 80
Trying 174.129.25.170...
Connected to wwwizer.com.
Escape character is '^]'.
GET / HTTP/1.1
Host: unstablebuild.com

HTTP/1.1 301 Moved Permanently
Server: nginx/1.8.0
Date: Thu, 19 Nov 2015 12:49:52 GMT
Content-Type: text/html
Content-Length: 184
Connection: keep-alive
Location: http://www.unstablebuild.com/

<html>
<head><title>301 Moved Permanently</title></head>
<body bgcolor="white">
<center><h1>301 Moved Permanently</h1></center>
<hr><center>nginx/1.8.0</center>
</body>
</html>
Connection closed by foreign host.
{% endhighlight %}
