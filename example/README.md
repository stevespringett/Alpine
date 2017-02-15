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
