---
layout: post
title: "Secure Configuration Management for Microservices"
date: 2017-04-14 10:41:11 +0100
categories: update
---

A few days ago I read about the decision that the Rails project is taking regarding [secrets management](http://weblog.rubyonrails.org/2017/2/23/Rails-5-1-beta1/#encrypted-secrets):

> You can setup a new encrypted secrets file with bin/rails secrets:setup. That’ll generate a master key you’ll store outside of the repository, but allow you to commit the actual production secrets to your revision control. They’re then decrypted in production either through an injected key file or through RAILS_MASTER_KEY in the ENV.

It was interesting to see this news, because it validates a very similar approach I helped to introduce a few months ago on my current job in relation to how we manage our services configurations. Not only that, but we expanded the concept to also enable unit testing these configurations, an idea based on a [paper published by Facebook](https://research.fb.com/publications/holistic-configuration-management-at-facebook/) on 2015.


In practical terms, for each service in our stack two files are created and kept on a Git repository. The first of the two is an YAML document containing the actual configuration keys and values for the different environments. For example, a service called `device-service` would have a file named `device-service.yaml`, and if we image that the service has a database and connects to some other downstream service, its content might have looked something like the following:


{% highlight yaml %}
db_user
  stg: stg_user
  prod: prod_user
db_password_ENC:
  stg: super_secret_password
  prod: even_more_secret_pass
db_url:
  stg: jdbc:postgresql://stg-host:port/database
  prod: jdbc:postgresql://prod-host:port/database
downstream_service:
  stg: http://stg-service:8080
  prod: https://prod-service
enable_transaction:
  stg: false
  prod: true
{% endhighlight %}

Here I'm displaying passwords in plain text for commodity, but by convention any key ending wih `_ENC`, like in `db_password_ENC`, will be encrypted using symmetric cryptography. Values for other keys are maintained as plain text and so are friendlier to the VCS, also meaning that only data that the team deems sensitive is encrypted (usually meaning passwords or public/private keys).

The second file is a python test definition that checks and validates the configuration values for its companion. An example for the previous service would be called `device-service_test.py` and ought to have the following content:

{% highlight python %}
class Test(BaseTest):

    def __init__(self, *args):
        super(Test, self).__init__(__file__, *args)

    def test(self):
        self.validate({
            'db_user': exists,
            'db_password': exists,
            'db_url': exists,
            'downstream_service': is_url,
            'enable_transaction': is_boolean
        })

        self.assert_encrypted('db_password')
{% endhighlight %}

The format is simple: we declare a map where the keys are the configuration keys, and the values are matchers. Matchers here are Python functions that will raise an exception if a certain condition wasn't fulfilled, and are used to check that a configuration value is valid for all defined environments. `Exists` is the most basic of these, only validating that values are present for the specified key. Other matches can check if the value is an URL (`is_url`) or is either true or false (`is_boolean`). You could as well use built-in Python functions, like `int`. We also have more complex matchers that can be used for checking ranges, port numbers, among others. At last `assert_encrypted` allows the developer to verify that a key is in fact encrypted.

Those files are managed by a custom command line tool, called `c.py`, that is kept in the same Git repository. Our tool allows us to execute these unit tests and, with that, whenever a developer makes a configuration change, we can quickly check that everything is OK. It also gives commands to get or set values, as well as to open the whole file (content decrypted) on your preferred text editor. On that case, whenever someone modifies a value, the passwords will be re-encrypted and the changes applied to the original file. However, If you print the file (i.e. `cat`), all you would see on the password field is a base64 encoded string of the encrypted value.

The encryption key is retrieved by the tool from an environment variable. An easy way to share this key is still an open problem. A [GPG](https://www.gnupg.org/) based approach might be the solution here. For now we are keeping it simple and using sharing between developers through secure channels.

We use [Consul](https://www.consul.io/) to expose those configuration values to our services. Given a valid Consul credential, our tool allows one to publish configurations there, a step that is performed automatically by our build pipeline on every push. You can also perform a dry run to validate the final result by printing the actions that would be taken, what we also do automatically for every branch of the repository.

Different mechanisms are used to feed the values into the services instances. For accessing those values, a service needs its own name, the environment it is running, and the appropriate credentials to access Consul. The last two are usually available as environment variables on each machine. Some use custom libraries to fetch them directly in the service, while others use command line tools to render configuration files with the missing pieces before running.

In sum, the flow of the configuration values, from user edited files to the according services, would look like the following:

<center>
  <img src="/assets/2017-04-14-secure-configuration-management-for-microservices/services_config_stream.jpeg" alt="Configuration values flow"></img>
</center>
<br/>

Even though this was a very quick project (we had a prototype in just a few hours), it has really facilitated our lives regarding configuration management. The tests allows us to validate changes quickly and catch broken configurations before they hit our services. Adding these files to Git enables us to track values and their changes. We can comments on the YAML documents to add reminders about configurations that can be removed later on, let's say, after a new version is deployed. Furthermore, by handling configurations this way, we can use well known software development techniques, i.e. code-reviews, in order to validate changes.
