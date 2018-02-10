# Description
Monumenta terrain reset process by Combustible

# Before reset
## Generate new dungeon instances - Build console
1. Stop the dungeon shard using mark2
1. Make a copy of the dungeon world folder
```
cp -a ~/project_epic/dungeon/Project_Epic-dungeon ~/tmp/Project_Epic-dungeon
```

1. Unpack the good copy of the dungeon template. This creates `~/tmp/Project_Epic-template`
```
cd ~/tmp
tar xzf ~/dungeon_template-keep-this-12-29-2017.tgz
```

1. Run the dungeon generation script. It takes around two hours.
```
~/MCEdit-And-Automation/utility_code/dungeon_instance_gen.py
```

1. Once it finishes, rename the dungeons-out folder to `POST_RESET`. Things get added to this folder throughout reset until it becomes the new `project_epic` folder.
```
mv dungeons-out POST_RESET
```

1. Remove the no-longer-needed processing directories
```
rm -rf ~/tmp/Project_Epic-dungeon ~/tmp/Project_Epic-template
```

## Purging old coreprotect data - In-game on play server
- Before reset while the servers are up go to each of the dungeon shards and purge coreprotect data older than 30 days.
- You must do this in-game! There is a note in the documentation about it not working from the console
- Go to `r1plots`, `region_1`, and `betaplots` and run:
```
/co purge t:30d
```

## Compiling and tagging the plugin - Your development system
- Tag the current version of the plugin and push that tag to github. This lets us go back later and figure out what version of the plugin was used for which terrain reset.
- You'll need to adapt the 2.16.0 number below for whatever the next version should be.
- Do this on your development system, there's no plugin code on the build server
- Also push the newly compiled plugin to the build server so it will be included in terrain reset.
```
cd Monumenta-Plugins
git fetch
git checkout master
git merge origin/master
git tag 2.16.0
git push origin 2.16.0
./clean.sh && ./compile.sh && ./upload.sh
```

# Prepping the reset bundle - Build Console
## Region 1 and bungee templates
1. Stop the `region_1` server and copy it to the TEMPLATE directory
```
mkdir -p ~/tmp/TEMPLATE/region_1
cp -a ~/project_epic/region_1/Project_Epic-region_1 ~/tmp/TEMPLATE/region_1/
cp -a ~/project_epic/bungee ~/tmp/TEMPLATE/
```

## Purgatory
1. Copy purgatory from the build server
```
cp -a ~/project_epic/purgatory ~/tmp/POST_RESET/
```

## Tutorial
1. Extract a known-good copy of the tutorial
```
mkdir ~/tmp/POST_RESET/tutorial
cd ~/tmp/POST_RESET/tutorial
tar xzf ~/Project_Epic-tutorial.good.jan-12-2018.tgz
```

## Server Config Template
1. Go make a commit in the server config's data directory
```
cd ~/project_epic/server_config/data
git add .
git commit -m "Changes for terrain reset on $(date +%Y_%m_%d)"
```

1. Copy the build server's config
```
cp -a ~/project_epic/server_config ~/tmp/POST_RESET/
```

1. Update these things in the `POST_RESET/server_config/server_config_template`:
```
vim ~/tmp/POST_RESET/server_config/server_config_template/server.properties
	difficulty=2
vim ~/tmp/POST_RESET/server_config/server_config_template/spigot.yml
	tab-complete: 9999
```

## Wrap it up and transfer to play server
```
cd ~/tmp
tar czf project_epic_build_template_pre_reset_$(date +%Y_%m_%d).tgz POST_RESET TEMPLATE
scp project_epic_build_template_pre_reset_$(date +%Y_%m_%d).tgz 'play:/home/rock/tmp/'
```

# For reset - Play Server Console

## Stop the play server
1. Attach to the bungee console
```
mark2 attach -n bungee
```

1. Add a 10 minute timer to stop the play server
```
~stop 10m;5m;3m;2m;1m;30s;10s
```

1. Once bungee shuts down, stop all the other shards:
```
mark2 sendall '~stop'
```

1. Make sure all the shards actually stopped (result should be empty)
```
mark2 list
```

## Backup the play server before reset
1. Tarball the whole `project_epic` folder once everything is stopped as a backup
```
cd ~
tar czf ~/1_ARCHIVE/project_epic_pre_reset_full_backup_$(date +%Y_%m_%d).tgz project_epic
```

1. Since the dungeon region files are giant and no longer needed, delete them and re-tarball to save space
```
cd ~/project_epic
for x in white orange magenta lightblue yellow r1bonus tutorial purgatory roguelike; do
	rm -r $x/Project_Epic-$x/region
	rm -rf $x/plugins/CoreProtect
done
rm -rf purgatory tutorial
```

1. Remove the giant spigot jars from the `server_config` directory
```
rm server_config/*.jar
rm server_config/plugins/*.jar
```

1. Re-tarball the project for archival purposes
```
cd ~
tar czf ~/1_ARCHIVE/project_epic_pre_reset_$(date +%Y_%m_%d).tgz project_epic
```

1. Move the `project_epic` to ~/tmp where it will be processed for reset
```
mv project_epic ~/tmp/PRE_RESET
```

### Make sure MCEdit-And-Automation is up to date
Make sure all the latest code is committed and pushed for the MCEdit-And-Automation repository

General steps on the build server:
```
cd ~/MCEdit-And-Automation
git fetch
git rebase origin/master
git status
git tag 2.16.0
git push origin 2.16.0
```

General steps on the play server:
```
cd ~/MCEdit-And-Automation
git status
git fetch
git checkout 2.16.0
git submodule update
```

## The actual reset
### Extract foundation
1. Change to the tmp directory. You may have to clean it out by moving old junk in that folder someplace (don't delete it, just in case..)
```
cd ~/tmp
```

1. Unpack the tarball (creates `POST_RESET` and `TEMPLATE` folders)
```
tar xzf project_epic_build_template_pre_reset_$(date +%Y_%m_%d).tgz
mv project_epic_build_template_pre_reset_$(date +%Y_%m_%d).tgz ~/1_ARCHIVE/
```

### Bungeecord
1. Hopefully no new shards have been added. If they have been... you'll have to edit the bungeecord config manually.  Otherwise you can probably just copy the bungee folder as-is
```
mv ~/tmp/PRE_RESET/bungee ~/tmp/POST_RESET/
```

1. Increment the version number in `~/tmp/POST_RESET/bungee/config.yml` TODO IMPROVE
```
motd: 'Monumenta : Beta 2.1   Version: 1.12.2'
```

### Server config
1. Update the server's `server_version` (and `daily_version` too if reset spans when the daily reset would be).
First open the `PRE_RESET` version to find what the version(s) are, then increment only the version number in the `POST_RESET` version.
<span style="color:red">DON'T SCREW THIS UP</span>
```
cat ~/tmp/PRE_RESET/region_1/plugins/Monumenta-Plugins/config.yml
	version: 109
	daily_version: 70
vim ~/tmp/POST_RESET/server_config/server_config_template/plugins/Monumenta-Plugins/config.yml
	version: 110
	daily_version: 70
```

1. Copy the luckperms settings from the beta server
```
rm -rf ~/tmp/POST_RESET/server_config/plugins/LuckPerms/yaml-storage
mv ~/tmp/PRE_RESET/server_config/plugins/LuckPerms/yaml-storage POST_RESET/server_config/plugins/LuckPerms/yaml-storage
```

1. Server config template complete - delete the old `server_config` directory
```
rm -rf ~/tmp/PRE_RESET/server_config
```

### Region 1, dungeons, betaplots, r1plots
1. Modify the terrain reset script to have correct paths:
```
vim ~/MCEdit-And-Automation/utility_code/terrain_reset.py
```

1. Run the terrain reset script
```
~/MCEdit-And-Automation/utility_code/terrain_reset.py
```

1. Copy the coreprotect databases
```bash
for x in betaplots r1plots region_1; do
	mkdir -p ~/tmp/POST_RESET/$x/plugins/CoreProtect
	mv ~/tmp/PRE_RESET/$x/plugins/CoreProtect/database.db ~/tmp/POST_RESET/$x/plugins/CoreProtect/database.db
	mkdir -p ~/tmp/POST_RESET/$x/plugins/EasyWarp
	mv ~/tmp/PRE_RESET/$x/plugins/EasyWarp/warps.yml ~/tmp/POST_RESET/$x/plugins/EasyWarp/warps.yml
done
```

### Build shard
1. Build shard is unaffected by reset
```
mv ~/tmp/PRE_RESET/build ~/tmp/POST_RESET/
```

### Whitelist / Opslist / Banlist
1. Copy the whitelist, opslist, and banlist from the play server. Overwrite if prompted
```bash
for x in betaplots lightblue magenta orange r1bonus r1plots region_1 roguelike tutorial white yellow; do
	cp PRE_RESET/region_1/whitelist.json POST_RESET/$x/
	cp PRE_RESET/region_1/banned-players.json POST_RESET/$x/
	cp PRE_RESET/region_1/ops.json POST_RESET/$x/
done
```

### Load appropriate configuration for each server
1. Run the configuration generator on each server shard in play server mode
```
cd POST_RESET
$HOME/MCEdit-And-Automation/utility_code/gen_server_config.py --play build betaplots lightblue magenta orange purgatory r1bonus r1plots region_1 roguelike tutorial white yellow
```

### Final steps
1. Make sure there are no broken symbolic links
```
find ~/tmp/POST_RESET -xtype l
```

1. Rename the reset directory
```
mv ~/tmp/POST_RESET ~/tmp/project_epic
```

1. Tar the directory as a backup
```
cd ~/tmp
tar czf ~/1_ARCHIVE/project_epic_post_reset_$(date +%Y_%m_%d).tgz project_epic
```

1. Move the reset folder to the correct location
```
mv ~/tmp/project_epic ~/
```

1. Remove the temporary directories
```
rm -r ~/tmp/PRE_RESET ~/tmp/TEMPLATE
```

1. Restart all the shards except bungee. These launch asynchronously, so you have to wait for them to stop spamming the console.
```bash
cd ~/project_epic

launch_server() (
	cd $x
	mark2 start &
)
for x in betaplots build lightblue magenta orange purgatory r1bonus r1plots region_1 roguelike tutorial white yellow
do
	launch_server $x
done
```

1. Use `mark2 attach` to verify that each and every server is up and running
1. Finally, start bungee
```
cd bungee
mark2 start
```

# Addendum
## Starting in ops-only mode
1. Optionally, you may want to launch the servers so that only operators can join.
1. The best way to do this currently is to rename the whitelist file on every server and re-start them. It's a horrible pain now that there are so many shards. If the whitelist file is empty, ops can still join.
