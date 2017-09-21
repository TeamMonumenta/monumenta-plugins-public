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

scp -P 9922 $bungee rock@158.69.117.57:/home/rock/project_epic/server_config/plugins/
scp -P 9922 $monumenta rock@158.69.117.57:/home/rock/project_epic/server_config/plugins/
ssh -p 9922 rock@158.69.117.57 "cd /home/rock/project_epic/server_config/plugins && rm -f Monumenta-Bungee-Plugins.jar ; ln -s $bungee Monumenta-Bungee-Plugins.jar"
ssh -p 9922 rock@158.69.117.57 "cd /home/rock/project_epic/server_config/plugins && rm -f Monumenta-Plugins.jar ; ln -s $monumenta Monumenta-Plugins.jar"


