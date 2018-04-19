#!/bin/bash

ant clean
retcode=$?
if [[ $retcode -ne 0 ]]; then
	exit $retcode
fi
