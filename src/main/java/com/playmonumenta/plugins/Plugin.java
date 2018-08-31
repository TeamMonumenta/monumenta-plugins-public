package com.playmonumenta.plugins;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.Iterator;

import com.playmonumenta.plugins.command.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import fr.rhaz.socketapi.SocketAPI.Client.SocketClient;
import com.playmonumenta.plugins.classes.AlchemistClass;
import com.playmonumenta.plugins.classes.BaseClass;
import com.playmonumenta.plugins.classes.ClericClass;
import com.playmonumenta.plugins.classes.MageClass;
import com.playmonumenta.plugins.classes.RogueClass;
import com.playmonumenta.plugins.classes.ScoutClass;
import com.playmonumenta.plugins.classes.WarlockClass;
import com.playmonumenta.plugins.classes.WarriorClass;
import com.playmonumenta.plugins.integrations.PlaceholderAPIIntegration;
import com.playmonumenta.plugins.integrations.VotifierIntegration;
import com.playmonumenta.plugins.items.ItemOverrides;
import com.playmonumenta.plugins.listeners.EntityListener;
import com.playmonumenta.plugins.listeners.MobListener;
import com.playmonumenta.plugins.listeners.PlayerListener;
import com.playmonumenta.plugins.listeners.SocketListener;
import com.playmonumenta.plugins.listeners.VehicleListener;
import com.playmonumenta.plugins.listeners.WorldListener;
import com.playmonumenta.plugins.managers.ZoneManager;
import com.playmonumenta.plugins.managers.potion.PotionManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.specializations.AssassinSpecialization;
import com.playmonumenta.plugins.specializations.BaseSpecialization;
import com.playmonumenta.plugins.specializations.ClassSpecialization;
import com.playmonumenta.plugins.specializations.CyromancerSpecialization;
import com.playmonumenta.plugins.specializations.PyromancerSpecialization;
import com.playmonumenta.plugins.specializations.ReaperSpecialization;
import com.playmonumenta.plugins.specializations.SniperSpecialization;
import com.playmonumenta.plugins.specializations.SwordsageSpecialization;
import com.playmonumenta.plugins.specializations.TenebristSpecialization;
import com.playmonumenta.plugins.timers.CombatLoggingTimers;
import com.playmonumenta.plugins.timers.CooldownTimers;
import com.playmonumenta.plugins.timers.ProjectileEffectTimers;
import com.playmonumenta.plugins.timers.PulseEffectTimers;
import com.playmonumenta.plugins.tracking.TrackingManager;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class Plugin extends JavaPlugin {
	//  TODO: Remove all Class related information out of Plugin and into it's own class "ClassManager" maybe?
	public enum Classes {
		NONE(0),
		MAGE(1),
		WARRIOR(2),
		CLERIC(3),
		ROGUE(4),
		ALCHEMIST(5),
		SCOUT(6),
		WARLOCK(7),

		COUNT(7);   //  Please update when new classes are added!

		private int value;
		private Classes(int value)  {
			this.value = value;
		}
		public int getValue()       {
			return this.value;
		}
	}

	public enum Times {
		ONE(1),
		TWO(2),
		FOURTY(40),
		SIXTY(60),
		ONE_TWENTY(120);

		private int value;
		private Times(int value)    {
			this.value = value;
		}
		public int getValue()       {
			return this.value;
		}
	}

	public HashMap<Integer, BaseClass> mClassMap = new HashMap<Integer, BaseClass>();
	public HashMap<Integer, BaseSpecialization> mSpecializationMap = new HashMap<Integer, BaseSpecialization>();
	public CooldownTimers mTimers = null;
	public ProjectileEffectTimers mProjectileEffectTimers = null;
	public PulseEffectTimers mPulseEffectTimers = null;
	public CombatLoggingTimers mCombatLoggingTimers = null;
	public Random mRandom = null;
	int mPeriodicTimer = -1;

	public ServerProperties mServerProperties = new ServerProperties();
	private FileConfiguration mConfig;
	private File mConfigFile;
	public int mDailyQuestVersion = 0;

	public TrackingManager mTrackingManager;
	public PotionManager mPotionManager;
	public ZoneManager mZoneManager;

	public SocketClient mSocketClient;

	public ItemOverrides mItemOverrides;

	public World mWorld;

	//  Logic that is performed upon enabling the plugin.
	@Override
	public void onEnable() {
		PluginManager manager = getServer().getPluginManager();

		mItemOverrides = new ItemOverrides();

		//  Initialize Variables.
		mRandom = new Random();
		mTimers = new CooldownTimers(this);
		mPulseEffectTimers = new PulseEffectTimers(this);
		mCombatLoggingTimers = new CombatLoggingTimers();

		mWorld = Bukkit.getWorlds().get(0);
		mProjectileEffectTimers = new ProjectileEffectTimers(mWorld);

		mPotionManager = new PotionManager(this);
		mTrackingManager = new TrackingManager(this, mWorld);
		mZoneManager = new ZoneManager(this);

		//  Load info.
		_loadConfig();
		mServerProperties.load(this);

		//  TODO: Move this out of here and into it's own ClassManager class.
		//  Initialize Classes.
		mClassMap.put(Classes.NONE.getValue(), new BaseClass(this, mRandom));
		mClassMap.put(Classes.MAGE.getValue(), new MageClass(this, mRandom, mWorld));
		mClassMap.put(Classes.WARRIOR.getValue(), new WarriorClass(this, mRandom));
		mClassMap.put(Classes.CLERIC.getValue(), new ClericClass(this, mRandom));
		mClassMap.put(Classes.ROGUE.getValue(), new RogueClass(this, mRandom));
		mClassMap.put(Classes.ALCHEMIST.getValue(), new AlchemistClass(this, mRandom));
		mClassMap.put(Classes.SCOUT.getValue(), new ScoutClass(this, mRandom));
		mClassMap.put(Classes.WARLOCK.getValue(), new WarlockClass(this, mRandom));

		mSpecializationMap.put(ClassSpecialization.NONE.getId(), new SwordsageSpecialization(this, mRandom));
		mSpecializationMap.put(ClassSpecialization.SWORDSAGE.getId(), new SwordsageSpecialization(this, mRandom));
		mSpecializationMap.put(ClassSpecialization.ASSASSIN.getId(), new AssassinSpecialization(this, mRandom));
		mSpecializationMap.put(ClassSpecialization.PYROMANCER.getId(), new PyromancerSpecialization(this, mRandom));
		mSpecializationMap.put(ClassSpecialization.CYROMANCER.getId(), new CyromancerSpecialization(this, mRandom));
		mSpecializationMap.put(ClassSpecialization.REAPER.getId(), new ReaperSpecialization(this, mRandom));
		mSpecializationMap.put(ClassSpecialization.TENEBRIST.getId(), new TenebristSpecialization(this, mRandom));
		mSpecializationMap.put(ClassSpecialization.SNIPER.getId(), new SniperSpecialization(this, mRandom));

		//  TODO: Move this out of here and into it's own EventManager class.
		manager.registerEvents(new SocketListener(this), this);
		manager.registerEvents(new PlayerListener(this, mWorld, mRandom), this);
		manager.registerEvents(new MobListener(this), this);
		manager.registerEvents(new EntityListener(this, mWorld), this);
		manager.registerEvents(new VehicleListener(this), this);
		manager.registerEvents(new WorldListener(this, mWorld), this);

		CommandFactory.createCommands(this, mServerProperties, mWorld, mPotionManager);

		//  Move the logic out of Plugin and into it's own class that derives off Runnable, a Timer class of some type.
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			int ticks = 0;

			@Override
			public void run() {
				final boolean twoHertz = (ticks % 10) == 0;
				final boolean fourHertz = (ticks % 5) == 0;
				final boolean twentyHertz = true;

				// NOW IT'S TWICE A SECOND MOTHAFUCKAAAASSSSSSSSS!!!!!!!!!!
				// FREQUENCY ANARCHY HAPPENING UP IN HERE

				if (twoHertz) {
					//  Update cooldowns.
					mTimers.UpdateCooldowns(Constants.HALF_TICKS_PER_SECOND);

					//  Update periodic timers.
					mPeriodicTimer++;

					final boolean one = (ticks % 20 == 0); //(mPeriodicTimer % Times.ONE.getValue()) == 0;
					final boolean two = (mPeriodicTimer % Times.TWO.getValue()) == 0;
					final boolean fourty = (mPeriodicTimer % Times.FOURTY.getValue()) == 0;
					final boolean sixty = (mPeriodicTimer % Times.SIXTY.getValue()) == 0;

					for (Player player : mTrackingManager.mPlayers.getPlayers()) {
						BaseClass pClass = Plugin.this.getClass(player);
						pClass.PeriodicTrigger(player, twoHertz, one, two, fourty, sixty, mPeriodicTimer);
					}

					mPeriodicTimer %= Times.ONE_TWENTY.getValue();
				}

				//  4 times a second.
				if (fourHertz) {
					mTrackingManager.update(mWorld, Constants.QUARTER_TICKS_PER_SECOND);
					mCombatLoggingTimers.update(mWorld, Constants.QUARTER_TICKS_PER_SECOND);
					mPulseEffectTimers.Update(Constants.QUARTER_TICKS_PER_SECOND);
				}

				//  Every tick.
				if (twentyHertz) {
					//  Update cooldowns.
					mProjectileEffectTimers.update();
				}

				ticks = (ticks + 1) % Constants.TICKS_PER_SECOND;
			}
		}, 0L, 1L);

		// Provide placeholder API replacements if it is present
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			new PlaceholderAPIIntegration(this).register();
		}

		// Get voting events if Votifier is present
		if (Bukkit.getPluginManager().isPluginEnabled("Votifier")) {
			manager.registerEvents(new VotifierIntegration(this), this);
		}
	}

	//  Logic that is performed upon disabling the plugin.
	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);

		mTrackingManager.unloadTrackedEntities();

		MetadataUtils.removeAllMetadata(this);
	}

	public Player getPlayer(UUID playerID) {
		return getServer().getPlayer(playerID);
	}

	//  TODO: Hmmm. I feel we may be able to transition all class related activites to static functions, investigate.
	public BaseClass getClass(Player player) {
		if (Constants.CLASSES_ENABLED) {
			int playerClass = ScoreboardUtils.getScoreboardValue(player, "Class");
			if (playerClass >= 0 && playerClass <= Classes.COUNT.getValue()) {
				return mClassMap.get(playerClass);
			}
		}

		//  We Seem to be missing a class.
		return mClassMap.get(Classes.NONE.getValue());
	}

	public BaseSpecialization getSpecialization(Player player) {
		if (Constants.SPECIALIZATIONS_ENABLED) {
			int playerClass = ScoreboardUtils.getScoreboardValue(player, "Specialization");
			if (playerClass >= 0 && playerClass <= ClassSpecialization.values().length) {
				return mSpecializationMap.get(playerClass);
			}
		}

		//  We Seem to be missing a class.
		return mSpecializationMap.get(0);
	}

	public void incrementDailyVersion() {
		if (mServerProperties.getDailyResetEnabled()) {
			mDailyQuestVersion++;
			_saveConfig();
		}
	}

	private void _loadConfig() {
		if (mConfigFile == null) {
			mConfigFile = new File(getDataFolder(), "config.yml");
		}

		mConfig = YamlConfiguration.loadConfiguration(mConfigFile);

		mDailyQuestVersion = mConfig.getInt("daily_version");
	}

	private void _saveConfig() {
		mConfig.set("daily_version", mDailyQuestVersion);

		try {
			mConfig.save(mConfigFile);
		} catch (IOException ex) {
			getLogger().log(Level.SEVERE, "Could not save config to " + mConfigFile, ex);
		}
	}
}
