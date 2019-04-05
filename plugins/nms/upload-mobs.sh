#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/target"

plugin="$(ls -r MonumentaNMS_*.jar | grep -v sources | head -n 1)"
if [[ -z "$plugin" ]]; then
	exit 1
fi

echo "Plugin version: $plugin"

scp -P 8822 $plugin epic@build.playmonumenta.com:/home/epic/mob_shard_plugins/
ssh -p 8822 epic@build.playmonumenta.com "cd /home/epic/mob_shard_plugins && rm -f MonumentaNMS.jar ; ln -s $plugin MonumentaNMS.jar"
