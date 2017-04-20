---
layout: post
title: "Blockchains as Part of the IoT Architecture"
date: 2017-04-20 17:07:43 +0200
categories: update
---

Stating the obvious here, but [blockchains](https://en.wikipedia.org/wiki/Blockchain) are a hot topic these days. It is easy to forget, though, that they are in fact distributed databases, so their application goes further than cryptocurrencies. Working with IoT for the last year, I kept asking myself how this could be adopted as part of an IoT Architecture.

Luckily, an article on [Hacker News](https://news.ycombinator.com/) became popular a few months ago, where the author explained how they implemented a very [basic blockchain](https://medium.com/@lhartikk/a-blockchain-in-200-lines-of-code-963cc1cc0e54). The code is indeed very simple, and is available [here](https://github.com/lhartikk/naivechain). I won't explain its details here. If you haven't seen it yet, I encourage you to have a look on the original post or the code itself, both very approachable.

Anyhow, I saw there an opportunity to modify the original version and test/see how it could be used on devices on a network. The fork containing this proof of concept is also available [on my GitHub](https://github.com/lucastorri/iot-naivechain).

I modified the original code to be able to simulate different types of devices. This is controlled by passing a environment variable, for example `DEVICE=lights`.

I also modified it to be able to receive two types of block content: *readings* and *commands*. A *reading* is something a device sends, for instance a device reporting its temperature. A *command* is something that a device should do, like turning lights on. They are handled by two functions, respectively `sendReading` and `sendCommand`. `sendCommand` is exposed to the HTTP interface, enabling a user to send a command by calling any node on the chain. `sendReading` is not exposed, as that's something initiated by the device itself. Whenever a *reading* or *command* is added on a device, this gets propagated to the rest of the chain by the original implementation.

When you start a device type, two behaviors are defined: what will it do whenever a command is received, and what will it do while running. Here is a summary of the devices implemented and their behaviors:


|          Device |                  While Running                  |               On Command              |
|----------------:|:-----------------------------------------------:|:-------------------------------------:|
|     **gateway** |                       Idle                      |     Logs all blocks to the console    |
|      **lights** |                       Idle                      | On `on` or `off`, log the action done |
| **temperature** | Report its (random) temperature every 5 seconds |                Nothing                |


The **gateway** acts as some sort of entry point between the devices in the blockchain and external networks. For instance, it could be sending readings and receiving commands from a MQTT broker. **lights** is an example of an actuator, and **temperature** a sensor.

As said before, commands are sent through HTTP. Turning the lights on can be done with:

{% highlight bash %}
curl \
  -H 'Content-type: application/json' \
  -d '{
    "device": "lights",
    "name": "on"
  }' \
  http://localhost:3001/sendCommand
{% endhighlight %}


Here is the output for a quick run, where the lights were turned on and then off:

{% highlight bash %}
# gateway
Reading 32 published by temperature
Reading 32.74064654276291 published by temperature
Command on sent to lights
Reading 33.488606288057184 published by temperature
Reading 32.53708277418028 published by temperature
Reading 32.91726429757095 published by temperature
Command off sent to lights
Reading 33.33392823289213 published by temperature

# lights
lights on
lights off
{% endhighlight %}

Of course, this experiment raises more questions than answers. The first one is if there would be a device that is capable of storing the whole blockchain? One option is to keep only the last *N* blocks, specially on memory-constrained devices. Since all devices need to receive all blocks in order to check if a command if sent for them, a problem could emerge on busy blockchains where devices have limited CPU. The use of absolute commands (`on`/`off`, instead of `toggle`) might also be required. Commands that don't do that (imagine `toggle` for the lights) are a bit more difficult to define an expected result due to the asynchronous nature of the command handling. Also, what happens if there is a fork on the network? I believe a lot of these issues are being discussed right now, but I haven't been due paying to the latest breakthroughs.

On the other hand, one would be able to have the whole history of *commands* and *readings* as blocks, giving the option to replay and audit what happened on the chain. Furthermore, if your devices are connected through a [mesh network](https://en.wikipedia.org/wiki/Mesh_networking), this would give a reliable way to distribute those blocks (I'm not considering [split-brains](https://en.wikipedia.org/wiki/Split-brain_(computing)) though).
