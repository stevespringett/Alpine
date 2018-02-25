[![Build Status](https://travis-ci.org/stevespringett/Alpine.svg?branch=master)](https://travis-ci.org/stevespringett/Alpine)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/cefa2866cbc24deeb7fbc83b8f71ad60)](https://www.codacy.com/app/stevespringett/Alpine?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=stevespringett/Alpine&amp;utm_campaign=Badge_Grade)
[![License][license-image]][license-url]
<img src="http://stevespringett.github.io/alpine/images/Alpine.svg" width="300" align="right">

Example Alpine Application
=========

This application highlights and demonstrates some of the features of 
Alpine by letting you run an example Alpine application in your local environment.

Getting Started
-

Compile and execute the application
```bash 
mvn clean package -Pembedded-jetty
cd target
java -jar example.war
```

Finally, open your web browser to http://localhost:8080

Copyright & License
-

Alpine is Copyright (c) Steve Springett. All Rights Reserved.

Permission to modify and redistribute is granted under the terms of the 
[Apache License 2.0](https://github.com/stevespringett/alpine/blob/master/LICENSE.txt)

Alpine makes use of several other open source libraries. Please see
the [NOTICE.txt](https://github.com/stevespringett/alpine/blob/master/NOTICE.txt) file for more information.


[license-image]: https://img.shields.io/badge/license-apache%20v2-brightgreen.svg
[license-url]: https://github.com/stevespringett/alpine/blob/master/LICENSE.txt

