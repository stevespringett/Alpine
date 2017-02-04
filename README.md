[![Build Status](https://travis-ci.org/stevespringett/alpine.svg?branch=3.0-dev)](https://travis-ci.org/stevespringett/alpine)

Alpine
=========

An opinionated scaffolding library that jumpstarts Java projects with an 
API-first design, secure defaults, and minimal dependencies. Alpine came 
about due to many commonalities that many of my personal and professional
projects share. 

Design Features
-

* **API-First Design** - 
Alpine assumes an API-first design where REST (via JAX-RS) is
at its core. The servers resources are stateless and do not rely
on sessions. JSON Web Tokens (JWT) are used to maintain some state
and are signed with an HMAC.

* **API Documentation** - 
Swagger support is built-in, allowing you to document APIs and generate
Swagger 2.0 definitions with ease.

* **Authentications** - 
Alpine supports multiple types of principals including LDAP users and 
API keys, both of which can be integrated into teams for access control.

* **Simplified Event System** - 
Alpine can perform parallel processing of tasks using a pub/sub model.
It can easily scale to consume as much or as little of the hosts hardware.

* **Secure By Default** - 
All REST resources are secure by default, requiring authentication to 
access them. Gone are the days of forgetting to protect a resource. 

* **Flexible Persistences** - 
Uses JDO, the most flexible Java persistence specification available. I 
never understood how JPA, a small subset of JDO, is viewed as progress.
It's not.

* **Minimal Dependencies** - 
Too many frameworks unnecessary increase the attack surface of applications 
built using them. Even a simple Hello World application can be susceptible 
to attack from vulnerable or poorly configured frameworks. Alpine includes
what is necessary for a modern app, nothing more.

* **Control** - 
Frameworks often force developers higher up the stack, freeing them from
low-level details. While this is certainly a huge win for productivity, 
developers often don't understand how their app actually works. Alpine 
does not do this. It provides standards-based APIs in a pre-packaged 
library giving developers both full control over their app, as well as a
productivity jumpstart.


Copyright & License
-

Alpine is Copyright (c) Steve Springett. All Rights Reserved.

Permission to modify and redistribute is granted under the terms of the 
[Apache License 2.0] [license]

Alpine makes use of several other open source libraries. Please see
the [NOTICE.txt] [notice] file for more information.

  [GitHub Wiki]: https://github.com/stevespringett/alpine/wiki
  [license]: https://github.com/stevespringett/alpine/blob/master/LICENSE.txt
  [notice]: https://github.com/stevespringett/alpine/blob/master/NOTICE.txt
