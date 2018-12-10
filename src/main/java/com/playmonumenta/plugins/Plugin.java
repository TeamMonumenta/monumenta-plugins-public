package com.playmonumenta.plugins;

import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.command.*;
import com.playmonumenta.plugins.integrations.PlaceholderAPIIntegration;
import com.playmonumenta.plugins.integrations.VotifierIntegration;
import com.playmonumenta.plugins.items.ItemOverrides;
import com.playmonumenta.plugins.listeners.EntityListener;
import com.playmonumenta.plugins.listeners.MobListener;
import com.playmonumenta.plugins.listeners.PlayerListener;
import com.playmonumenta.plugins.listeners.SocketListener;
import com.playmonumenta.plugins.listeners.VehicleListener;
import com.playmonumenta.plugins.listeners.WorldListener;
import com.playmonumenta.plugins.managers.potion.PotionManager;
import com.playmonumenta.plugins.managers.ZoneManager;
import com.playmonumenta.plugins.rawcommands.BroadcastCommand;
import com.playmonumenta.plugins.rawcommands.DebugInfo;
import com.playmonumenta.plugins.rawcommands.Effect;
import com.playmonumenta.plugins.rawcommands.GildifyHeldItem;
import com.playmonumenta.plugins.rawcommands.GiveSoulbound;
import com.playmonumenta.plugins.rawcommands.HopeifyHeldItem;
import com.playmonumenta.plugins.rawcommands.RefreshClass;
import com.playmonumenta.plugins.rawcommands.RemoveTags;
import com.playmonumenta.plugins.rawcommands.TransferServer;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.specializations.*;
import com.playmonumenta.plugins.timers.CombatLoggingTimers;
import com.playmonumenta.plugins.timers.CooldownTimers;
import com.playmonumenta.plugins.timers.ProjectileEffectTimers;
import com.playmonumenta.plugins.tracking.TrackingManager;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import fr.rhaz.socketapi.SocketAPI.Client.SocketClient;

import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.World;

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

		public static Classes getClassById(int id) {
			for (Classes cl : Classes.values()) {
				if (cl != Classes.COUNT) {
					if (cl.getValue() == id) {
						return cl;
					}
				}
			}
			return NONE;
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

	public HashMap<Integer, BaseSpecialization> mSpecializationMap = new HashMap<Integer, BaseSpecialization>();
	public CooldownTimers mTimers = null;
	public ProjectileEffectTimers mProjectileEffectTimers = null;
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
	public AbilityManager mAbilityManager;

	public SocketClient mSocketClient;

	public ItemOverrides mItemOverrides;

	public World mWorld;

	private static Plugin plugin;

	public static Plugin getInstance() {
		return plugin;
	}

	@Override
	public void onLoad() {
		/*
		 * CommandAPI commands which register directly and are usable in functions
		 *
		 * These need to register immediately on load to prevent function loading errors
		 */

		TransferServer.register(this);
		GiveSoulbound.register();
		HopeifyHeldItem.register();
		GildifyHeldItem.register();
		DebugInfo.register(this);
		RefreshClass.register(this);
		Effect.register(this);
		RemoveTags.register();

		mServerProperties.load(this);
		if (mServerProperties.getBroadcastCommandEnabled()) {
			BroadcastCommand.register(this);
		}
	}

	//  Logic that is performed upon enabling the plugin.
	@Override
	public void onEnable() {
		plugin = this;
		PluginManager manager = getServer().getPluginManager();

		mItemOverrides = new ItemOverrides();

		//  Initialize Variables.
		mRandom = new Random();
		mTimers = new CooldownTimers(this);
		mCombatLoggingTimers = new CombatLoggingTimers();

		mWorld = Bukkit.getWorlds().get(0);
		mProjectileEffectTimers = new ProjectileEffectTimers(mWorld);

		mPotionManager = new PotionManager(this);
		mTrackingManager = new TrackingManager(this, mWorld);
		mZoneManager = new ZoneManager(this);
		mAbilityManager = new AbilityManager(this, mWorld, mRandom);

		//  Load info.
		_loadConfig();
		mServerProperties.load(this);

		//  Initialize Specializations
		mSpecializationMap.put(ClassSpecialization.NONE.getId(), new SwordsageSpecialization(this, mRandom, mWorld));
		mSpecializationMap.put(ClassSpecialization.SWORDSAGE.getId(), new SwordsageSpecialization(this, mRandom, mWorld));
		mSpecializationMap.put(ClassSpecialization.ASSASSIN.getId(), new AssassinSpecialization(this, mRandom, mWorld));
		mSpecializationMap.put(ClassSpecialization.PYROMANCER.getId(), new PyromancerSpecialization(this, mRandom, mWorld));
		mSpecializationMap.put(ClassSpecialization.CYROMANCER.getId(), new CyromancerSpecialization(this, mRandom, mWorld));
		mSpecializationMap.put(ClassSpecialization.REAPER.getId(), new ReaperSpecialization(this, mRandom, mWorld));
		mSpecializationMap.put(ClassSpecialization.TENEBRIST.getId(), new TenebristSpecialization(this, mRandom, mWorld));
		mSpecializationMap.put(ClassSpecialization.SNIPER.getId(), new SniperSpecialization(this, mRandom, mWorld));
		mSpecializationMap.put(ClassSpecialization.RANGER.getId(), new RangerSpecialization(this, mRandom, mWorld));
		mSpecializationMap.put(ClassSpecialization.PALADIN.getId(), new PaladinSpecialization(this, mRandom, mWorld));
		mSpecializationMap.put(ClassSpecialization.HIEROPHANT.getId(), new HierophantSpecialization(this, mRandom, mWorld));
		mSpecializationMap.put(ClassSpecialization.ELEMENTALIST.getId(), new ElementalistSpecialization(this, mRandom, mWorld));
		mSpecializationMap.put(ClassSpecialization.ARCANIST.getId(), new ArcanistSpecialization(this, mRandom, mWorld));

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
					try {
						mTimers.UpdateCooldowns(Constants.HALF_TICKS_PER_SECOND);
					} catch (Exception e) {
						e.printStackTrace();
					}

					//  Update periodic timers.
					mPeriodicTimer++;

					final boolean one = (ticks % 20 == 0); //(mPeriodicTimer % Times.ONE.getValue()) == 0;
					final boolean two = (mPeriodicTimer % Times.TWO.getValue()) == 0;
					final boolean fourty = (mPeriodicTimer % Times.FOURTY.getValue()) == 0;
					final boolean sixty = (mPeriodicTimer % Times.SIXTY.getValue()) == 0;

					for (Player player : mTrackingManager.mPlayers.getPlayers()) {
						try {
							BaseSpecialization pSpec = getSpecialization(player);
							pSpec.PeriodicTrigger(player, twoHertz, one, two, fourty, sixty, mPeriodicTimer);
						} catch (Exception e) {
							e.printStackTrace();
						}

						try {
							AbilityManager.getManager().PeriodicTrigger(player, twoHertz, one, two, fourty, sixty, mPeriodicTimer);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					mPeriodicTimer %= Times.ONE_TWENTY.getValue();
				}

				//  4 times a second.
				if (fourHertz) {
					try {
						mTrackingManager.update(mWorld, Constants.QUARTER_TICKS_PER_SECOND);
					} catch (Exception e) {
						e.printStackTrace();
					}

					try {
						mCombatLoggingTimers.update(mWorld, Constants.QUARTER_TICKS_PER_SECOND);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				//  Every tick.
				if (twentyHertz) {
					//  Update cooldowns.
					try {
						mProjectileEffectTimers.update();
					} catch (Exception e) {
						e.printStackTrace();
					}
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
