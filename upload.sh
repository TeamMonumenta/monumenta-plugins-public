#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/target"

plugin="$(ls -r Monumenta-Plugins_*.jar | grep -v sources | head -n 1)"
if [[ -z "$plugin" ]]; then
	exit 1
fi

echo "Plugin version: $plugin"

scp -P 8822 $plugin rock@beta.playmonumenta.com:/home/rock/project_epic/server_config/plugins-1.13/
ssh -p 8822 rock@beta.playmonumenta.com "cd /home/rock/project_epic/server_config/plugins-1.13 && rm -f Monumenta-Plugins.jar ; ln -s $plugin Monumenta-Plugins.jar"
