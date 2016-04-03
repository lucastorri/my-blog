---
layout: post
title: "Javascript Scrolling Header"
date: 2015-11-22 19:23:56 +0100
categories: update
---
Use Javascript to Hide Fixed Header when Scrolling

One of my past projects was to create a [CRUD](https://en.wikipedia.org/wiki/Create,_read,_update_and_delete) application for [Points of Interest](https://en.wikipedia.org/wiki/Point_of_interest).

It is an single page AngularJS application, where users can edit all sorts of informations about the POI: names in different languages, addresses, geolocation, images, contact information, etc. A place can have lots of data associated. Just out of curiosity, famous places, like the Eiffel Tower, can have a name for each existing language. In French it would be *Tour Eiffel*, *Eiffelturm* in German, or *Torre Eiffel* in Portuguese. Therefore, the more space available on the screen, the better it is to navigate through it.

As a small improvement, we decided to hide the fixed header, containing quick access to other functionality, whenever the user scrolled down, and show it once again when scrolling up. We ended up creating our own implementation with jQuery helping us get position of elements in the window.

The code was isolated on an AngularJS directive, who would then inject the element and the associated scope. I decided to extract it and modify it to be a regular function. The final result is not fancy, but is quite simple, so it might come handy some day. The code and result can be seen below, or through this [link](https://jsfiddle.net/mkfabw8p/3/).

<iframe width="100%" height="350" src="//jsfiddle.net/mkfabw8p/3/embedded/result,js,html,css" allowfullscreen="allowfullscreen" frameborder="0"></iframe>

Internally, it behaves as a state machine, where each state is a function responsible for determining the next state, or none, if it should remain on the same one. State changes are triggered whenever the page is scrolled.

The `heightFactor` variable is used to indicate how many times the element height needs to be scrolled down in order to start hiding it. The higher it is, the longer you'll need to scroll to it start disappearing

If your scroll up again while the header is hidden, partially or completely, the header will immediately start to show up again.

&nbsp;

**Update**: you can also get the file [here](/downloads/2015-11-22-javascript-scrolling-header/index.html).
