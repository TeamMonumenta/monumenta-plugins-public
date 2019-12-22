#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

for x in nms main; do
	cd "$SCRIPT_DIR/$x"
	./compile.sh
	ret=$?
	if [[ $ret -ne 0 ]]; then
		echo "Upload Failed!" >&2
		exit $ret
	fi
done
