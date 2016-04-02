---
layout: post
title: "Parsing CoAP's Binary Messages in Elixir"
date: 2016-04-02 10:05:54 +0200
categories: update
---

I have been implementing the [Constrained Application Protocol](https://en.wikipedia.org/wiki/Constrained_Application_Protocol) in Elixir,
I called the project [*coapex*](https://github.com/lucastorri/coapex). It has been a nice exercise to learn more Elixir and existing IoT protocols.

CoAP is a HTTP-like protocol intended to be used in devices with constrained memory and limited communication capabilities. Messages are transmitted using UDP instead of TCP, which allows the use of multicast. As in HTTP, it can work using the request-response model, but unlike it, messages can be sent without receiving back a confirmation/response, or receiving responses anytime a given path is updated (Observe mode). The message format is binary and defined to be very light. To give an example, that's the basic format of CoAP's header defined on the [protocol's RFC](https://tools.ietf.org/html/rfc7252):

{% highlight text %}
     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |Ver| T |  TKL  |      Code     |          Message ID           |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |   Token (if any, TKL bytes) ...
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |   Options (if any) ...
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |1 1 1 1 1 1 1 1|    Payload (if any) ...
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                          Figure 7: Message Format
{% endhighlight %}

Options are equivalent to HTTP header fields, and just as in HTTP, can have a variable number.

One of the basic types in Elixir is *binary*, which is just a sequence of bytes. What is very nice about it is that the language allows you to do pattern matching with them. For instance, that is part of the [code](https://github.com/lucastorri/coapex/blob/master/lib/coap/parser.ex) responsible for parsing the CoAP fields of a *binary* (`header`) message in *coapex*:

{% highlight elixir %}
<<
  version :: unsigned-size(2),
  type :: unsigned-size(2),
  token_length :: unsigned-size(4),
  code_class :: unsigned-size(3),
  code_detail :: unsigned-size(5),
  message_id :: unsigned-size(16),
  token :: binary-size(token_length),
  rest :: binary
>> = header
{% endhighlight %}

For me that's very clean and intuitive. You can see the code and easily understand what is what in the received message. What is also interesting, is that you can use extracted fields on the following matches. In this case, we extract `token_length` at the beginning of the message, and use it later on to extract the received `token`.

Afterwards, in the parser code, the `rest` of the message is recursively parsed to extract any options available and, finally, its content.

The other way around, or serializing the message back to binary, is also very simple:

{% highlight elixir %}
<<
  hdr.version :: unsigned-size(2),
  hdr.type :: unsigned-size(2),
  byte_size(hdr.token) :: unsigned-integer-size(4),
  hdr.code_class :: unsigned-size(3),
  hdr.code_detail :: unsigned-size(5),
  hdr.message_id :: unsigned-size(16),
  hdr.token :: binary
>>
{% endhighlight %}

Here, `hdr` is a Struct containing all the attributes of the CoAP message. On the code, I'm declaring what will go in that position (i.e. `hdr.version`) and how it will be transformed (2 bits, unsigned, with `unsigned-size(2)`).

&nbsp;

*coapex* is functional and has been tested against other two libraries: [*node-coap*](https://github.com/mcollina/node-coap) and [*go-coap*](https://github.com/dustin/go-coap). The code can be found on [GitHub](https://github.com/lucastorri/coapex)

Apart from the unit tests, a Server/Client example can be found [here](https://github.com/lucastorri/coapex/blob/master/lib/coap/example.ex).
