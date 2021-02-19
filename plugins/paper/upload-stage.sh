#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/target"

plugin="$(ls -r Monumenta_*.jar | grep -v sources | head -n 1)"
if [[ -z "$plugin" ]]; then
	exit 1
fi

echo "Plugin version: $plugin"

scp -P 9922 $plugin epic@admin.playmonumenta.com:/home/epic/stage/project_epic/server_config/plugins/
ssh -p 9922 epic@admin.playmonumenta.com "cd /home/epic/stage/project_epic/server_config/plugins && rm -f Monumenta.jar ; ln -s $plugin Monumenta.jar"
