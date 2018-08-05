#!/bin/bash

./mvnw clean install
retcode=$?
if [[ $retcode -ne 0 ]]; then
	exit $retcode
fi
