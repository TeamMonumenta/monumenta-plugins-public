#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/out"

version="$(ls monumenta-bossfights-*.jar | sed -e 's|^.*_\([0-9]*-[0-9]*\).jar|\1|g' | sort -n -r | head -n 1)"
if [[ -z "$version" ]]; then
	exit 1
fi
plugin="$(ls -r monumenta-bossfights-*${version}.jar | head -n 1)"

echo "Plugin version: $plugin"

scp -P 8822 $plugin rock@beta.playmonumenta.com:/home/rock/project_epic/server_config/plugins/
ssh -p 8822 rock@beta.playmonumenta.com "cd /home/rock/project_epic/server_config/plugins && rm -f Monumenta_BossFights.jar ; ln -s $plugin Monumenta_BossFights.jar"
