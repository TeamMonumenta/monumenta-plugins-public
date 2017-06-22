package pe.project;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import pe.project.commands.ChatRangeCommand;
import pe.project.commands.GetServerVersionCommand;
import pe.project.commands.IsShittyCommand;
import pe.project.commands.PlayTimeStats;
import pe.project.commands.ProfilingCommand;
import pe.project.commands.RefreshPOITimerCommand;
import pe.project.commands.SetGuildPrefix;
import pe.project.commands.SetPlayerName;
import pe.project.commands.SetServerVersionCommand;
import pe.project.listeners.EntityListener;
import pe.project.listeners.ItemListener;
import pe.project.listeners.MobListener;
import pe.project.listeners.PlayerListener;
import pe.project.managers.POIManager;
import pe.project.managers.QuestManager;
import pe.project.tracking.TrackingManager;

public class Main extends JavaPlugin {
	private FileConfiguration mConfig;
	private File mConfigFile;
	public int mServerVersion = 0;
	
	public QuestManager mQuestManager;
	public TrackingManager mTrackingManager;
	public POIManager mPOIManager;
	
	//	Logic that is performed upon enabling the plugin.
	@Override
	public void onEnable() {
		PluginManager manager = getServer().getPluginManager();
		
		World world = Bukkit.getWorlds().get(0);
		
		manager.registerEvents(new PlayerListener(this), this);
		manager.registerEvents(new MobListener(this), this);
		manager.registerEvents(new EntityListener(this, world), this);
		manager.registerEvents(new ItemListener(this), this);
		
		//	Add some slash commands
		getCommand("setServerVersion").setExecutor(new SetServerVersionCommand(this));
		getCommand("getServerVersion").setExecutor(new GetServerVersionCommand(this));
		getCommand("playTimeStats").setExecutor(new PlayTimeStats(this, world));
		getCommand("chatRange").setExecutor(new ChatRangeCommand());
		getCommand("refreshPOITimer").setExecutor(new RefreshPOITimerCommand(this));
		getCommand("isShitty").setExecutor(new IsShittyCommand());
		getCommand("profiling").setExecutor(new ProfilingCommand(this));
		getCommand("setGuildPrefix").setExecutor(new SetGuildPrefix());
		getCommand("setPlayerName").setExecutor(new SetPlayerName());
		
		mQuestManager = new QuestManager(this, world);
		mTrackingManager = new TrackingManager(this, world);
		mPOIManager = new POIManager(this);
		
		//	Load info.
		_loadConfig();
		mPOIManager.loadAllPOIs();
		
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			int ticks = 0;
			
			@Override
			public void run() {
				//	Once a second.
				if (ticks == 0) {
					
				}
				
				mTrackingManager.update(world);
				mPOIManager.updatePOIs(Constants.QUARTER_TICKS_PER_SECOND);

				ticks = (ticks + 1) % 4;
			}
		}, 0L, Constants.QUARTER_TICKS_PER_SECOND);
	}
	
	//	Logic that is performed upon disabling the plugin.
	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
		
		//	Save info.
		mPOIManager.saveAllPOIs();
	}
	
	public void updateVersion(int version) {
		mServerVersion = version;
		mConfig.set("version", version);
		_saveConfig();
	}
	
	private void _loadConfig() {
		if (mConfigFile == null) {
			mConfigFile = new File(getDataFolder(), "config.yml");
		}
		
		mConfig = YamlConfiguration.loadConfiguration(mConfigFile);
		
		mServerVersion = mConfig.getInt("version");
	}
	
	private void _saveConfig() {
		try {
			mConfig.save(mConfigFile);
		} catch (IOException ex) {
			getLogger().log(Level.SEVERE, "Could not save config to " + mConfigFile, ex);
		}
	}
}
