#!/bin/bash

# Update version number
perl -p -i -e "s|<version>dev</version>|<version>$(git describe --tags --always --dirty)</version>|g" pom.xml

mvn clean install
retcode=$?
if [[ $retcode -ne 0 ]]; then
	exit $retcode
fi
