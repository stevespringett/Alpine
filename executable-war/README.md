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


Usage
-

Configure the Maven POM with the Alpine Embedded WAR dependency and configure the Maven WAR plugin.

```xml
<dependencies>
    <!-- Add a dependency on Alpine Embedded WAR -->
    <dependency>
        <groupId>us.springett</groupId>
        <artifactId>alpine-executable-war</artifactId>
        <version>1.2.1</version>
        <scope>provided</scope>
    </dependency>
</dependencies>

<plugins>
    <!-- Configure the Maven WAR plugin to overlay Alpine Embedded WAR -->
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-war-plugin</artifactId>
        <configuration>
            <warName>${project.build.finalName}</warName>
            <archive>
                <manifest>
                    <mainClass>alpine.embedded.EmbeddedJettyServer</mainClass>
                </manifest>
            </archive>
            <dependentWarExcludes>WEB-INF/lib/*log4j*.jar,WEB-INF/lib/*slf4j*.jar</dependentWarExcludes>
            <overlays>
                <overlay>
                    <groupId>us.springett</groupId>
                    <artifactId>alpine-executable-war</artifactId>
                    <type>jar</type>
                </overlay>
            </overlays>
        </configuration>
    </plugin>
</plugins>
```

Command-Line Arguments
-
The following command-line arguments can be passed to a compiled executable WAR when executing it:

| Argument | Default | Description |
|:---------|:--------|:------------|
| -context | /       | The application context to deploy to |
| -host    | 0.0.0.0 | The IP address to bind to |
| -port    | 8080    | The TCP port to listens on |


Copyright & License
-

Alpine is Copyright (c) Steve Springett. All Rights Reserved.

Permission to modify and redistribute is granted under the terms of the 
[Apache License 2.0](https://github.com/stevespringett/alpine/blob/master/LICENSE.txt)

Alpine makes use of several other open source libraries. Please see
the [NOTICE.txt](https://github.com/stevespringett/alpine/blob/master/NOTICE.txt) file for more information.


[license-image]: https://img.shields.io/badge/license-apache%20v2-brightgreen.svg
[license-url]: https://github.com/stevespringett/alpine/blob/master/LICENSE.txt
