#!/bin/bash

ant pmd build jar
retcode=$?
if [[ $retcode -ne 0 ]]; then
	exit $retcode
fi
