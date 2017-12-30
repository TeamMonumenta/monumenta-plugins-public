#!/bin/bash

bungee_version="$(ls Monumenta-Bungee-Plugins*.jar | sed -e 's|^.*_\([0-9]*-[0-9]*\).jar|\1|g' | sort -n -r | head -n 1)"
if [[ -z "$bungee_version" ]]; then
	exit 1
fi
bungee="$(ls -r Monumenta-Bungee-Plugins*${bungee_version}.jar | head -n 1)"

monumenta_version="$(ls Monumenta-Plugins*.jar | sed -e 's|^.*_\([0-9]*-[0-9]*\).jar|\1|g' | sort -n -r | head -n 1)"
if [[ -z "$monumenta_version" ]]; then
	exit 1
fi
monumenta="$(ls -r Monumenta-Plugins*${monumenta_version}.jar | head -n 1)"

echo "Bungee version: $bungee"
echo "Monumenta version: $monumenta"

scp -P 8822 $bungee $monumenta rock@beta.playmonumenta.com:/home/rock/project_epic/server_config/plugins/
ssh -p 8822 rock@beta.playmonumenta.com "cd /home/rock/project_epic/server_config/plugins && rm -f Monumenta-Bungee-Plugins.jar Monumenta-Plugins.jar ; ln -s $bungee Monumenta-Bungee-Plugins.jar ; ln -s $monumenta Monumenta-Plugins.jar"


