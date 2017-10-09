[![Build Status](https://travis-ci.org/stevespringett/Alpine.svg?branch=master)](https://travis-ci.org/stevespringett/Alpine)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/cefa2866cbc24deeb7fbc83b8f71ad60)](https://www.codacy.com/app/stevespringett/Alpine?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=stevespringett/Alpine&amp;utm_campaign=Badge_Grade)
[![Dependency Status](https://www.versioneye.com/user/projects/58a2a49a0f3d4f003ce97f07/badge.svg?style=flat)](https://www.versioneye.com/user/projects/58a2a49a0f3d4f003ce97f07)
[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/690/badge)](https://bestpractices.coreinfrastructure.org/projects/690)
[![License][license-image]][license-url]
[![Join the chat at https://gitter.im/java-alpine/Lobby](https://badges.gitter.im/java-alpine/Lobby.svg)](https://gitter.im/java-alpine/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
![Static Analysis][fortify-image]
[![Component Analysis][odc-image]][odc-url]
<img src="http://stevespringett.github.io/alpine/images/Alpine.svg" width="300" align="right">

Alpine
=========

[![Join the chat at https://gitter.im/java-alpine/Lobby](https://badges.gitter.im/java-alpine/Lobby.svg)](https://gitter.im/java-alpine/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

An opinionated scaffolding library that jumpstarts Java projects with an 
API-first design, secure defaults, and minimal dependencies. Alpine came 
about due to many commonalities between several of my personal and 
professional projects.

Alpine provides the basis for quickly developing a '**Thin Server Architecture**'.
This type of architecture shifts the role of webapps to being API providers
with little or no responsibility for server-side HTML rendering. This type
of architecture is perfect for client-side rendered webapps that rely heavily
on JSON, for Single Page Applications (SPA), and to power back-ends that drive
mobile applications.

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
Alpine can perform parallel processing of tasks using an asynchronous
pub/sub model. It can easily scale to consume as much or as little of 
available resources as necessary.

* **Secure By Default** - 
All REST resources are secure by default, requiring authentication to 
access them. Gone are the days of forgetting to protect a resource. 

* **Flexible Persistences** - 
Uses JDO, the most flexible Java persistence specification available. I 
never understood how JPA, a small subset of JDO, is viewed as progress.
It's not.

* **Minimal Dependencies** - 
Too many frameworks unnecessary increase the attack surface of applications 
built using them. Even a simple Hello World application is often susceptible 
to attack from the use of vulnerable components or poorly configured 
frameworks. Alpine includes what is necessary for a modern app, nothing more.

* **Control** - 
Frameworks often force developers higher up the stack, freeing them from
low-level details. While this is certainly a huge win for productivity, 
developers often don't understand how their app actually works. Alpine 
does not do this. It provides standards-based APIs in a pre-packaged 
library giving developers both full control over their app, as well as a
productivity jumpstart.

Application Features
-

The following features are free and require little or no coding just for using Alpine.
* Authentication for Internal (managed) and LDAP users
* Authentication via API keys
* Authentication via JWT
* Stateless API-first design
* Automatic generation of Swagger 2.0 definitions
* REST resources are locked down by default (requires authentication)
* Configurable enforcement of authentication and authorization
* Built-in support for BCrypt for the hashing and salting of passwords for managed users
* Built-in models for managed users, LDAP users, API keys, and groups (called teams in Alpine)
* Built-in and consistent support for pagination and ordering via REST
* Embedded database
* Flexible persistence supporting RDBMS and non-RDBMS datastores (via Datanucleus JDO)
* Separate application and audit logs
* Scheduled and on-demand execution of parallel tasks via asynchronous pub/sub event framework
* Extendable and centralized application configuration
* Built-in input validation (JSR 303 & 349) for all REST resources and default model classes
* Defensive security mechanisms for enabling:
  * Click-jacking protection (X-Frame-Options) (RFC-7034)
  * Content Security Policy (Level 1 and 2)
  * HTTP Public Key Pinning (HPKP) (RFC-7469)
  * HTTP Strict Transport Security (HSTS) (RFC-6797)

Build Features
-

These build-time features are inherited simply by using the Alpine pom
* Simplifies dependency management. Simply including Alpine as a dependency is all that's required
* Analysis of third-party components for known vulnerabilities via OWASP Dependency-Check & Retire.js
* Support for HPE Fortify Source Code Analyzer (SCA) (requires Fortify license to use)
* Alpine apps are automatically built as WARs
* Optional packaging as an executable WAR containing an embedded Jetty container

Compiling
-------------------

```bash
mvn clean install
````

Maven Usage
-------------------
Alpine is currently pre-release software but snapshot builds can be used and 
are available on the Maven Central Repository. These can be used without having
to compile Alpine yourself.

```xml
<!-- Place the parent right after the <project> root node
     to inherit all the goodies from alpine-parent pom -->
<parent>
    <groupId>us.springett</groupId>
    <artifactId>alpine-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>

<dependencies>
    <!-- Add the alpine dependency -->
    <dependency>
        <groupId>us.springett</groupId>
        <artifactId>alpine</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

Why 'Alpine'
-
Alt-right American politics has devised many insults against liberal thought
and the people who practice it. The term 'snowflake' is often applied to 
such people as a derogatory insult in the absence of their own critical 
thinking ability. Although a creative term, it's highly ineffective and
does nothing to address global concerns. So 'Alpine' is the public celebration
of being a 'snowflake', named after the blankets of snow that cover the French
Alps. Besides, 'Alpine' has a certain je ne sais quoi to it.

Open source is truly one of the great liberal ideals. So Alpine is released
with an equally liberal Apache 2.0 license.

Alpine is also the brand name of high-end car audio equipment, a hobby I was
passionate about in my teens and twenties when I built many systems and often
competed in various sound-offs in Illinois and Wisconsin. The name is nostalgic
and brings to me a certain joy.

Alpine is not affiliated with the Docker container image by the same name. It's
interesting that both projects have minimalism as a goal, but this is merely
coincidence.

Is Alpine For You
-
My opinions are just that, mine. They will evolve over time. However, my belief
in sticking with open standards and not having to conform to the conventions of
a specific framework will likely never change. Alpine is lightweight, 
standards-based, and framework-free. If this appeals to you, give Alpine a try. 

Projects Using Alpine
-
If your open source or commercial project is using Alpine, feel free to add its
name:

* [Hakbot Origin Controller](https://github.com/hakbot/hakbot-origin-controller)
* [OWASP Dependency-Track](https://www.owasp.org/index.php/OWASP_Dependency_Track_Project)

Copyright & License
-

Alpine is Copyright (c) Steve Springett. All Rights Reserved.

Permission to modify and redistribute is granted under the terms of the 
[Apache License 2.0] [license-url]

Alpine makes use of several other open source libraries. Please see
the [NOTICE.txt] [notice] file for more information.

  [alpine-image]: http://6000rpms.com/images/Alpine.svg
  [GitHub Wiki]: https://github.com/stevespringett/alpine/wiki
  [license-image]: https://img.shields.io/badge/license-apache%20v2-brightgreen.svg
  [license-url]: https://github.com/stevespringett/alpine/blob/master/LICENSE.txt
  [fortify-image]: https://img.shields.io/badge/static%20analysis-fortify%20sca-blue.svg
  [odc-image]: https://img.shields.io/badge/component%20analysis-owasp%20dependency--check-blue.svg
  [odc-url]: https://www.owasp.org/index.php/OWASP_Dependency_Check
  [notice]: https://github.com/stevespringett/alpine/blob/master/NOTICE.txt
