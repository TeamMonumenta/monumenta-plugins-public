#!/bin/bash

bungee_version="$(ls monumenta-bungeecord-*.jar | sed -e 's|^.*_\([0-9]*-[0-9]*\).jar|\1|g' | sort -n -r | head -n 1)"
if [[ -z "$bungee_version" ]]; then
	exit 1
fi
bungee="$(ls -r monumenta-bungeecord-*${bungee_version}.jar | head -n 1)"

echo "Bungee version: $bungee"

scp -P 8822 $bungee rock@beta.playmonumenta.com:/home/rock/project_epic/server_config/plugins/
ssh -p 8822 rock@beta.playmonumenta.com "cd /home/rock/project_epic/server_config/plugins && rm -f Monumenta-Bungee-Plugins.jar ; ln -s $bungee Monumenta-Bungee-Plugins.jar"
