#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/target"

plugin="$(ls -r monumenta-plugins-*.jar | grep -v sources | head -n 1)"
if [[ -z "$plugin" ]]; then
	exit 1
fi

echo "Plugin version: $plugin"

scp -P 8822 $plugin epic@beta.playmonumenta.com:/home/epic/mob_shard_plugins/
ssh -p 8822 epic@beta.playmonumenta.com "cd /home/epic/mob_shard_plugins && rm -f Monumenta-Plugins.jar ; ln -s $plugin Monumenta-Plugins.jar"
