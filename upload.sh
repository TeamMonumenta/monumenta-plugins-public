#!/bin/bash

monumenta_version="$(ls monumenta-plugins-*.jar | sed -e 's|^.*_\([0-9]*-[0-9]*\).jar|\1|g' | sort -n -r | head -n 1)"
if [[ -z "$monumenta_version" ]]; then
	exit 1
fi
monumenta="$(ls -r monumenta-plugins-*${monumenta_version}.jar | head -n 1)"

echo "Monumenta version: $monumenta"

scp -P 8822 $monumenta rock@beta.playmonumenta.com:/home/rock/project_epic/server_config/plugins/
ssh -p 8822 rock@beta.playmonumenta.com "cd /home/rock/project_epic/server_config/plugins && rm -f Monumenta-Plugins.jar ; ln -s $monumenta Monumenta-Plugins.jar"
