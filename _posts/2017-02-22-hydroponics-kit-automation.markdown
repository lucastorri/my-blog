---
layout: post
title: "Hydroponics Kit Automation"
date: 2017-02-22 18:52:56 +0100
categories: update
---

A few weeks ago, I was visiting IKEA and came across a hydroponics set they sell, called [Växer](http://www.ikea.com/gb/en/products/indoor-gardening/). I always liked the thought of getting my hands dirt (ha!) with hydroponics, and although I assume there are better kits out there, it just happens that I had all that was necessary right in front of me: lights, nursery, cultivation box, seeds, fertilizer, etc.

I was looking at the pieces on the store, wondering if I should or not spend money on them, when I had an idea. No, it wasn't planting marijuana, as friends suggested, but instead I've decided that it would be cool to automate it. For instance, being smart about when to turn the lights on/off, add more water to the system, and so on. I had some hardware kits (Arduino, [BananaPi](http://www.banana-pi.org/), sensors, etc) laying around at home, and finally agreed on giving it a go.


To make it more interesting, though, I set some goals for this project:

- Try to use as many of the hardware kits I had (and finally find use for them);
- Connect it to the Internet (because, you know, IoT);
- Try to learn a new language or improve on some least used one;
- Focus more on the software than on the hardware (the later is not a strong point of mine);


Now that I have a somehow stable version running for the last week, I chose to share the details here. Right away I'll only give some basic information about the solution and over the following posts I'll describe it in more details, breaking down into each of the composing parts. I aim to soon put the code in GitHub. Nevertheless, the elements on the system are:

<center>
  <img src="/assets/2017-02-22-hydroponics-kit-automation/hydroponics-diagram.svg" alt="System diagram"></img>
</center>
<br/>

I've used an Arduino to plug sensors and actuators. It is just so easy to find libraries for almost any peripheral that in the end it was a no-brainer choice. In theory, I could have implemented the whole thing into it, but I wanted it to have a more powerful platform on top, in favor of flexibility. Why make it simple, when you can complicate, right?! There are just a couple of actuators plugged at the moment:

- Temperature/Humidity sensor (DHT11)
- Relay for controlling the lights

The control logic is running on a BananaPi with Debian. It was written in [Go](https://golang.org/) after I failed to do it in [Rust](https://www.rust-lang.org/en-US/). I did learn Rust in the process and had a nice time with it, but in the end I was spending too much time fighting the compiler. Nonetheless, this application allows different clients to connect to it through *connectors*. Right now there are two: HTTP/WebSockets and [MQTT](https://en.wikipedia.org/wiki/MQTT). It also communicates with the sensors/actuators (transducers) on the Arduino board through a serial port and a homemade protocol called [Simple Serial Slave-Master Protocol (S3MP)](https://github.com/lucastorri/s3mp).

![Hardware](/assets/2017-02-22-hydroponics-kit-automation/hardware.jpg)

S3MP allows the master, in this case the BananaPi, to send commands to the slaves, while requiring a small number of bytes and maintaining reliability of the transmitted information. Transducers are identified by names (i.e. `temperature-sensor`), but messages are sent to single byte addresses instead, akin to DNS names and IP addresses. The master can send commands like `GET` or `SET`, but also subscribe to sensors and receive notifications on changes. For example a temperature sensor can notify the master whenever the temperature varies, instead of the master needing to poll it.

![Kit](/assets/2017-02-22-hydroponics-kit-automation/kit.jpg)
