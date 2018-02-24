#!/usr/bin/env bash
read -p "Really deploy to maven central repository  (yes/no)? "
if ( [ "$REPLY" == "yes" ] ) then

mvn release:clean release:prepare release:perform -Prelease -X -e | tee maven-central-deploy.log

else
  echo 'Exit without deploy'
fi