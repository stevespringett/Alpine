#!/usr/bin/env bash
read -p "Really deploy to maven central repository  (yes/no)? "
if ( [ "$REPLY" == "yes" ] ) then

read -p "Specify release version number in Maven Central (i.e. 1.0.0): "

mvn versions:set -DnewVersion=$REPLY
mvn clean deploy -Prelease
mvn versions:revert
mvn release:clean release:prepare release:perform -Prelease -X -e | tee maven-central-deploy.log

else
  echo 'Exit without deploy'
fi