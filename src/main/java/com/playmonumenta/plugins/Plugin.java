package com.playmonumenta.plugins;

import java.io.File;
import java.io.IOException;
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

import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.commands.Bot;
import com.playmonumenta.plugins.commands.BroadcastCommand;
import com.playmonumenta.plugins.commands.CreateGuild;
import com.playmonumenta.plugins.commands.DeathMsg;
import com.playmonumenta.plugins.commands.DebugInfo;
import com.playmonumenta.plugins.commands.Effect;
import com.playmonumenta.plugins.commands.FestiveHeldItem;
import com.playmonumenta.plugins.commands.GildifyHeldItem;
import com.playmonumenta.plugins.commands.GiveSoulbound;
import com.playmonumenta.plugins.commands.HopeifyHeldItem;
import com.playmonumenta.plugins.commands.IncrementDaily;
import com.playmonumenta.plugins.commands.RefreshClass;
import com.playmonumenta.plugins.commands.RemoveTags;
import com.playmonumenta.plugins.commands.TestNoScore;
import com.playmonumenta.plugins.commands.TransferScores;
import com.playmonumenta.plugins.commands.TransferServer;
import com.playmonumenta.plugins.commands.UpdateApartments;
import com.playmonumenta.plugins.integrations.PlaceholderAPIIntegration;
import com.playmonumenta.plugins.integrations.VotifierIntegration;
import com.playmonumenta.plugins.listeners.EntityListener;
import com.playmonumenta.plugins.listeners.MobListener;
import com.playmonumenta.plugins.listeners.PlayerListener;
import com.playmonumenta.plugins.listeners.SocketListener;
import com.playmonumenta.plugins.listeners.VehicleListener;
import com.playmonumenta.plugins.listeners.WorldListener;
import com.playmonumenta.plugins.overrides.ItemOverrides;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.spawnzone.SpawnZoneManager;
import com.playmonumenta.plugins.timers.CombatLoggingTimers;
import com.playmonumenta.plugins.timers.CooldownTimers;
import com.playmonumenta.plugins.timers.ProjectileEffectTimers;
import com.playmonumenta.plugins.tracking.TrackingManager;
import com.playmonumenta.plugins.utils.MetadataUtils;

import fr.rhaz.socketapi.SocketAPI.Client.SocketClient;

public class Plugin extends JavaPlugin {
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
	public SpawnZoneManager mZoneManager;
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
		FestiveHeldItem.register();
		GildifyHeldItem.register();
		DebugInfo.register(this);
		RefreshClass.register(this);
		Effect.register(this);
		RemoveTags.register();
		DeathMsg.register();
		UpdateApartments.register();
		IncrementDaily.register(this);
		TransferScores.register(this);
		CreateGuild.register(this);

		mServerProperties.load(this);
		Bot.register(this);
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

		mPotionManager = new PotionManager();
		mTrackingManager = new TrackingManager(this, mWorld);
		mZoneManager = new SpawnZoneManager(this);
		mAbilityManager = new AbilityManager(this, mWorld, mRandom);

		//  Load info.
		_loadConfig();
		mServerProperties.load(this);

		//  TODO: Move this out of here and into it's own EventManager class.
		manager.registerEvents(new SocketListener(this), this);
		manager.registerEvents(new PlayerListener(this, mWorld, mRandom), this);
		manager.registerEvents(new MobListener(this), this);
		manager.registerEvents(new EntityListener(this, mWorld), this);
		manager.registerEvents(new VehicleListener(this), this);
		manager.registerEvents(new WorldListener(this, mWorld), this);

		// The last remaining Spigot-style command...
		plugin.getCommand("testNoScore").setExecutor(new TestNoScore());

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
