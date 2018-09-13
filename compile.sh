#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Update version number
VERSION=$(git describe --tags --always --dirty)
perl -p -i -e "s|<version>dev</version>|<version>$VERSION</version>|g" "$SCRIPT_DIR/pom.xml"
perl -p -i -e "s|^version: .*$|version: $VERSION|g" "$SCRIPT_DIR/src/main/resources/plugin.yml"

mvn clean install
retcode=$?
if [[ $retcode -ne 0 ]]; then
	exit $retcode
fi
