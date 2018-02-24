[![Build Status](https://travis-ci.org/stevespringett/Alpine.svg?branch=master)](https://travis-ci.org/stevespringett/Alpine)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/cefa2866cbc24deeb7fbc83b8f71ad60)](https://www.codacy.com/app/stevespringett/Alpine?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=stevespringett/Alpine&amp;utm_campaign=Badge_Grade)
[![License][license-image]][license-url]
<img src="http://stevespringett.github.io/alpine/images/Alpine.svg" width="300" align="right">

Alpine Executable WAR
=========

An executable WAR provides a way to create a standalone WAR, with all dependencies, capable of being deployed
simply by calling `java -jar myapp.war`. Pre-configured servlet containers are not required. The project 
incorporates a modern embedded Jetty container capable of supporting the Servlet 3.1 specification along with
support for JSPs. The project requires Java 8 and is completely independent of the main Alpine project. In fact,
the Alpine Executable WAR project can be used for virtually all Java web applications.

Copyright & License
-

Alpine is Copyright (c) Steve Springett. All Rights Reserved.

Permission to modify and redistribute is granted under the terms of the 
[Apache License 2.0] [license-url]

Alpine makes use of several other open source libraries. Please see
the [NOTICE.txt] [notice] file for more information.


[license-image]: https://img.shields.io/badge/license-apache%20v2-brightgreen.svg
[license-url]: https://github.com/stevespringett/alpine/blob/master/LICENSE.txt
[notice]: https://github.com/stevespringett/alpine/blob/master/NOTICE.txt
