package pe.project;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import pe.project.classes.*;
import pe.project.commands.*;
import pe.project.listeners.EntityListener;
import pe.project.listeners.ItemListener;
import pe.project.listeners.MobListener;
import pe.project.listeners.PlayerListener;
import pe.project.listeners.PluginListener;
import pe.project.listeners.WorldListener;
import pe.project.managers.POIManager;
import pe.project.managers.QuestManager;
import pe.project.managers.potion.PotionManager;
import pe.project.timers.CooldownTimers;
import pe.project.timers.CombatLoggingTimers;
import pe.project.timers.ProjectileEffectTimers;
import pe.project.timers.PulseEffectTimers;
import pe.project.tracking.TrackingManager;
import pe.project.utils.ScoreboardUtils;

public class Main extends JavaPlugin {
	public enum Classes {
		NONE(0),
		MAGE(1),
		WARRIOR(2),
		CLERIC(3),
		ROGUE(4),
		ALCHEMIST(5),
		SCOUT(6),
		
		COUNT (6);	//	Please update when new classes are added!
		
		private int value;
		private Classes(int value)	{	this.value = value;	}
		public int getValue()		{	return this.value;	}
	}
	
	public enum Times {
		ONE(1),
		TWO(2),
		FOURTY(40),
		SIXTY(60),
		ONE_TWENTY(120);
		
		private int value;
		private Times(int value)	{	this.value = value;	}
		public int getValue()		{	return this.value;	}
	}
	
	public HashMap<Integer, BaseClass> mClassMap = new HashMap<Integer, BaseClass>();
	public CooldownTimers mTimers = null;
	public ProjectileEffectTimers mProjectileEffectTimers = null;
	public PulseEffectTimers mPulseEffectTimers = null;
	public CombatLoggingTimers mCombatLoggingTimers = null;
	private Random mRandom = null;
	int mPeriodicTimer = -1;
	
	private FileConfiguration mConfig;
	private File mConfigFile;
	public int mServerVersion = 0;
	public int mDailyQuestVersion = 0;
	
	public QuestManager mQuestManager;
	public TrackingManager mTrackingManager;
	public POIManager mPOIManager;
	public PotionManager mPotionManager;
	
	//	Logic that is performed upon enabling the plugin.
	@Override
	public void onEnable() {
		PluginManager manager = getServer().getPluginManager();
		
		//	Initialize Variables.
		mRandom = new Random();
		mTimers = new CooldownTimers(this);
		mPulseEffectTimers = new PulseEffectTimers(this);
		mCombatLoggingTimers = new CombatLoggingTimers();
			
		World world = Bukkit.getWorlds().get(0);
		mProjectileEffectTimers = new ProjectileEffectTimers(world);
			
		//	Initialize Classes.
		mClassMap.put(Classes.NONE.getValue(), new BaseClass(this, mRandom));
		mClassMap.put(Classes.MAGE.getValue(), new MageClass(this, mRandom));
		mClassMap.put(Classes.WARRIOR.getValue(), new WarriorClass(this, mRandom));
		mClassMap.put(Classes.CLERIC.getValue(), new ClericClass(this, mRandom));
		mClassMap.put(Classes.ROGUE.getValue(), new RogueClass(this, mRandom));
		mClassMap.put(Classes.ALCHEMIST.getValue(), new AlchemistClass(this, mRandom));
		mClassMap.put(Classes.SCOUT.getValue(), new ScoutClass(this, mRandom));
		
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new PluginListener(this));
		
		manager.registerEvents(new PlayerListener(this, world), this);
		manager.registerEvents(new MobListener(this), this);
		manager.registerEvents(new EntityListener(this, world), this);
		manager.registerEvents(new ItemListener(this), this);
		manager.registerEvents(new WorldListener(this, world), this);
		
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
		getCommand("transferScores").setExecutor(new TransferScores());
		getCommand("getScore").setExecutor(new GetScore());
		getCommand("refreshClassEffects").setExecutor(new RefreshClassEffects(this, world));
		getCommand("transferServer").setExecutor(new TransferServer(this));
		getCommand("incrementDaily").setExecutor(new IncrementDaily(this));
		
		mPotionManager = new PotionManager(this);
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
				boolean oneHertz = ticks == 0;
				boolean fourHertz = (ticks % 5) == 0;
				boolean twentyHertz = true;
				
				//	Once a second.
				if (oneHertz) {
					//	Update cooldowns.
					mTimers.UpdateCooldowns(Constants.TICKS_PER_SECOND);
					mPulseEffectTimers.Update(Constants.TICKS_PER_SECOND);
					
					//	Update periodic timers.
					mPeriodicTimer++;

					for(Player player : mTrackingManager.mPlayers.getPlayers()) {
						BaseClass pClass = Main.this.getClass(player);
							
						boolean two = (mPeriodicTimer % Times.TWO.getValue()) == 0;
						boolean fourty = (mPeriodicTimer % Times.FOURTY.getValue()) == 0;
						boolean sixty = (mPeriodicTimer % Times.SIXTY.getValue()) == 0;
						pClass.PeriodicTrigger(player, two, fourty, sixty, mPeriodicTimer);
					}
						
					mPeriodicTimer %= Times.ONE_TWENTY.getValue();
				}
				
				//	4 times a second.
				if (fourHertz) {
					mTrackingManager.update(world, Constants.QUARTER_TICKS_PER_SECOND);
					mPOIManager.updatePOIs(Constants.QUARTER_TICKS_PER_SECOND);
					mCombatLoggingTimers.update(world, Constants.QUARTER_TICKS_PER_SECOND);
				}
				
				//	Every tick.
				if (twentyHertz) {
					//	Update cooldowns.
					mProjectileEffectTimers.update();
				}
				
				ticks = (ticks + 1) % Constants.TICKS_PER_SECOND;
			}
		}, 0L, 1L);
	}
	
	//	Logic that is performed upon disabling the plugin.
	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
		
		mTrackingManager.unloadTrackedEntities();
		
		//	Save info.
		mPOIManager.saveAllPOIs();
	}
	
	public Player getPlayer(UUID playerID) {
		return getServer().getPlayer(playerID);
	}
	
	public BaseClass getClass(Player player) {
		int playerClass = ScoreboardUtils.getScoreboardValue(player, "Class");
		if (playerClass >= 0 && playerClass <= Classes.COUNT.getValue()) {
			return mClassMap.get(playerClass);
		}
		
		//	We Seem to be missing a class.
		return mClassMap.get(Classes.NONE.getValue());
	}
	
	public void updateVersion(int version) {
		mServerVersion = version;
		_saveConfig();
	}
	
	public void incrementDailyVersion() {
		mDailyQuestVersion++;
		_saveConfig();
	}
	
	private void _loadConfig() {
		if (mConfigFile == null) {
			mConfigFile = new File(getDataFolder(), "config.yml");
		}
		
		mConfig = YamlConfiguration.loadConfiguration(mConfigFile);
		
		mServerVersion = mConfig.getInt("version");
		mDailyQuestVersion = mConfig.getInt("daily_version");
	}
	
	private void _saveConfig() {
		mConfig.set("version", mServerVersion);
		mConfig.set("daily_version", mDailyQuestVersion);
		
		try {
			mConfig.save(mConfigFile);
		} catch (IOException ex) {
			getLogger().log(Level.SEVERE, "Could not save config to " + mConfigFile, ex);
		}
	}
}
