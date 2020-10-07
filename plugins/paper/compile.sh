#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Backup the files with version numbers
cp "$SCRIPT_DIR/pom.xml" "$SCRIPT_DIR/pom.xml.compilebackup"
cp "$SCRIPT_DIR/src/main/resources/plugin.yml" "$SCRIPT_DIR/src/main/resources/plugin.yml.compilebackup"

# Update version number
VERSION=$(git describe --tags --always --dirty)
perl -p -i -e "s|<version>dev</version>|<version>$VERSION</version>|g" "$SCRIPT_DIR/pom.xml"
perl -p -i -e "s|^version: .*$|version: $VERSION|g" "$SCRIPT_DIR/src/main/resources/plugin.yml"

mvn clean install "$@"
retcode=$?

# Restore the original files with version numbers
mv -f "$SCRIPT_DIR/pom.xml.compilebackup" "$SCRIPT_DIR/pom.xml"
mv -f "$SCRIPT_DIR/src/main/resources/plugin.yml.compilebackup" "$SCRIPT_DIR/src/main/resources/plugin.yml"

# Return the appropriate error code from compilation
if [[ $retcode -ne 0 ]]; then
	exit $retcode
fi
