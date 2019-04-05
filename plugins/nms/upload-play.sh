#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/target"

plugin="$(ls -r MonumentaNMS_*.jar | grep -v sources | head -n 1)"
if [[ -z "$plugin" ]]; then
	exit 1
fi

echo "Plugin version: $plugin"

scp -P 9922 $plugin play:/home/rock/project_epic/server_config/plugins/
ssh -p 9922 play "cd /home/rock/project_epic/server_config/plugins && rm -f MonumentaNMS.jar ; ln -s $plugin MonumentaNMS.jar"
