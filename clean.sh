#!/bin/bash

./mvnw clean
retcode=$?
if [[ $retcode -ne 0 ]]; then
	exit $retcode
fi
