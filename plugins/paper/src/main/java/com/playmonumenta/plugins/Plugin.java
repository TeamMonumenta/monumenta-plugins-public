package com.playmonumenta.plugins;

import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.spells.SpellDetectionCircle;
import com.playmonumenta.plugins.commands.BossFight;
import com.playmonumenta.plugins.commands.Bot;
import com.playmonumenta.plugins.commands.BroadcastCommand;
import com.playmonumenta.plugins.commands.CalculateReforge;
import com.playmonumenta.plugins.commands.ClaimRaffle;
import com.playmonumenta.plugins.commands.DeCluckifyHeldItem;
import com.playmonumenta.plugins.commands.DeathMsg;
import com.playmonumenta.plugins.commands.DebugInfo;
import com.playmonumenta.plugins.commands.Effect;
import com.playmonumenta.plugins.commands.FestiveHeldItem;
import com.playmonumenta.plugins.commands.GildifyHeldItem;
import com.playmonumenta.plugins.commands.GiveSoulbound;
import com.playmonumenta.plugins.commands.HopeifyHeldItem;
import com.playmonumenta.plugins.commands.InfuseHeldItem;
import com.playmonumenta.plugins.commands.MonumentaDebug;
import com.playmonumenta.plugins.commands.MonumentaReload;
import com.playmonumenta.plugins.commands.RedeemVoteRewards;
import com.playmonumenta.plugins.commands.ReforgeHeldItem;
import com.playmonumenta.plugins.commands.ReforgeInventory;
import com.playmonumenta.plugins.commands.RefreshClass;
import com.playmonumenta.plugins.commands.RemoveTags;
import com.playmonumenta.plugins.commands.RestartEmptyCommand;
import com.playmonumenta.plugins.commands.SkillDescription;
import com.playmonumenta.plugins.commands.SkillSummary;
import com.playmonumenta.plugins.commands.Spectate;
import com.playmonumenta.plugins.commands.SpectateBot;
import com.playmonumenta.plugins.commands.SpringCleanItems;
import com.playmonumenta.plugins.commands.SpringScores;
import com.playmonumenta.plugins.commands.TestNoScore;
import com.playmonumenta.plugins.cooking.CookingCommand;
import com.playmonumenta.plugins.cooking.CookingTableInventoryManager;
import com.playmonumenta.plugins.cooking.CookingTableListeners;
import com.playmonumenta.plugins.enchantments.EnchantmentManager;
import com.playmonumenta.plugins.integrations.PlaceholderAPIIntegration;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.inventories.ShulkerInventoryManager;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.listeners.CrossbowListener;
import com.playmonumenta.plugins.listeners.EntityListener;
import com.playmonumenta.plugins.listeners.ExceptionListener;
import com.playmonumenta.plugins.listeners.JunkItemListener;
import com.playmonumenta.plugins.listeners.MobListener;
import com.playmonumenta.plugins.listeners.PlayerListener;
import com.playmonumenta.plugins.listeners.PortableEnderListener;
import com.playmonumenta.plugins.listeners.PotionConsumeListener;
import com.playmonumenta.plugins.listeners.ServerTransferListener;
import com.playmonumenta.plugins.listeners.ShatteredEquipmentListener;
import com.playmonumenta.plugins.listeners.ShulkerEquipmentListener;
import com.playmonumenta.plugins.listeners.ShulkerShortcutListener;
import com.playmonumenta.plugins.listeners.TridentListener;
import com.playmonumenta.plugins.listeners.VehicleListener;
import com.playmonumenta.plugins.listeners.WorldListener;
import com.playmonumenta.plugins.listeners.ZonePropertyListener;
import com.playmonumenta.plugins.network.HttpManager;
import com.playmonumenta.plugins.network.SocketManager;
import com.playmonumenta.plugins.overrides.ItemOverrides;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.redis.RedisManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.server.reset.DailyReset;
import com.playmonumenta.plugins.spawnzone.SpawnZoneManager;
import com.playmonumenta.plugins.timers.CombatLoggingTimers;
import com.playmonumenta.plugins.timers.CooldownTimers;
import com.playmonumenta.plugins.timers.ProjectileEffectTimers;
import com.playmonumenta.plugins.tracking.TrackingManager;
import com.playmonumenta.plugins.utils.MetadataUtils;

public class Plugin extends JavaPlugin {
	public CooldownTimers mTimers = null;
	public ProjectileEffectTimers mProjectileEffectTimers = null;
	public CombatLoggingTimers mCombatLoggingTimers = null;
	public Random mRandom = null;
	int mPeriodicTimer = -1;

	public EnchantmentManager mEnchantmentManager;
	public JunkItemListener mJunkItemsListener;
	private HttpManager mHttpManager = null;
	public TrackingManager mTrackingManager;
	public PotionManager mPotionManager;
	public SpawnZoneManager mZoneManager;
	public AbilityManager mAbilityManager;
	public ShulkerInventoryManager mShulkerInventoryManager;
	public CookingTableInventoryManager mCookingTableInventoryManager;
	private BossManager mBossManager;

	private RedisManager mRedis;

	public SocketManager mSocketManager;

	public ItemOverrides mItemOverrides;

	public World mWorld;

	private static Plugin INSTANCE = null;

	public static Plugin getInstance() {
		return INSTANCE;
	}

	@Override
	public void onLoad() {
		/*
		 * CommandAPI commands which register directly and are usable in functions
		 *
		 * These need to register immediately on load to prevent function loading errors
		 */

		GiveSoulbound.register();
		HopeifyHeldItem.register();
		FestiveHeldItem.register();
		GildifyHeldItem.register();
		InfuseHeldItem.register();
		SpringCleanItems.register();
		SpringScores.register();
		ClaimRaffle.register(this);
		DeCluckifyHeldItem.register();
		CalculateReforge.register();
		ReforgeHeldItem.register();
		ReforgeInventory.register();
		DebugInfo.register(this);
		RefreshClass.register(this);
		Effect.register(this);
		RemoveTags.register();
		DeathMsg.register();
		MonumentaReload.register(this);
		MonumentaDebug.register(this);
		RestartEmptyCommand.register(this);
		RedeemVoteRewards.register(this);
		BossFight.register();
		SpellDetectionCircle.registerCommand(this);
		SkillDescription.register(this);
		SkillSummary.register(this);
		CookingCommand.register(this);

		try {
			mHttpManager = new HttpManager(this);
		} catch (IOException err) {
			getLogger().warning("HTTP manager failed to start");
			err.printStackTrace();
		}

		ServerProperties.load(this, null);

		mEnchantmentManager = new EnchantmentManager(this);
		mEnchantmentManager.load(ServerProperties.getForbiddenItemLore());

		mJunkItemsListener = new JunkItemListener();

		Bot.register(this);
		if (ServerProperties.getBroadcastCommandEnabled()) {
			BroadcastCommand.register(this);
		}
	}

	//  Logic that is performed upon enabling the plugin.
	@Override
	public void onEnable() {
		INSTANCE = this;
		PluginManager manager = getServer().getPluginManager();

		mHttpManager.start();

		try {
			mRedis = new RedisManager(getLogger());
		} catch (Exception ex) {
			/* TODO: This is probably a fatal exception! */
			getLogger().severe("Failed to instantiate redis manager: " + ex.getMessage());
			ex.printStackTrace();
		}

		try {
			mSocketManager = new SocketManager(this);
		} catch (Exception ex) {
			/* TODO: This is probably a fatal exception! */
			getLogger().severe("Failed to instantiate socket manager: " + ex.getMessage());
			ex.printStackTrace();
		}

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
		mShulkerInventoryManager = new ShulkerInventoryManager(this);
		mCookingTableInventoryManager = new CookingTableInventoryManager(this);
		mBossManager = new BossManager(this);

		DailyReset.startTimer(this);

		//  Load info.
		reloadMonumentaConfig(null);

		// These are both a command and an event listener
		manager.registerEvents(new Spectate(this), this);
		manager.registerEvents(new SpectateBot(this), this);

		if (ServerProperties.getAuditMessagesEnabled()) {
			manager.registerEvents(new AuditListener(), this);
		}
		manager.registerEvents(new ServerTransferListener(this.getLogger()), this);
		manager.registerEvents(new ExceptionListener(this), this);
		manager.registerEvents(new PlayerListener(this, mWorld, mRandom), this);
		manager.registerEvents(new MobListener(this), this);
		manager.registerEvents(new EntityListener(this, mWorld, mAbilityManager), this);
		manager.registerEvents(new VehicleListener(this), this);
		manager.registerEvents(new WorldListener(this, mWorld), this);
		manager.registerEvents(new ShulkerShortcutListener(this), this);
		manager.registerEvents(new ShulkerEquipmentListener(this), this);
		manager.registerEvents(new PortableEnderListener(), this);
		manager.registerEvents(new ShatteredEquipmentListener(this), this);
		manager.registerEvents(new PotionConsumeListener(this), this);
		manager.registerEvents(new ZonePropertyListener(), this);
		manager.registerEvents(new TridentListener(), this);
		manager.registerEvents(new CrossbowListener(this), this);
		manager.registerEvents(mEnchantmentManager, this);
		manager.registerEvents(mJunkItemsListener, this);
		manager.registerEvents(mBossManager, this);
		manager.registerEvents(new CookingTableListeners(this), this);

		// The last remaining Spigot-style command...
		this.getCommand("testNoScore").setExecutor(new TestNoScore());

		//  Move the logic out of Plugin and into it's own class that derives off Runnable, a Timer class of some type.
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			int mTicks = 0;

			@Override
			public void run() {
				final boolean oneHertz = (mTicks % 20) == 0;
				final boolean twoHertz = (mTicks % 10) == 0;
				final boolean fourHertz = (mTicks % 5) == 0;
				final boolean twentyHertz = true;

				// NOW IT'S TWICE A SECOND MOTHAFUCKAAAASSSSSSSSS!!!!!!!!!!
				// FREQUENCY ANARCHY HAPPENING UP IN HERE

				if (twoHertz) {
					//  Update cooldowns.
					try {
						mTimers.updateCooldowns(Constants.HALF_TICKS_PER_SECOND);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				if (fourHertz) {
					for (Player player : mTrackingManager.mPlayers.getPlayers()) {
						try {
							mAbilityManager.periodicTrigger(player, fourHertz, twoHertz, oneHertz, mTicks);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
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

				mTicks = (mTicks + 1) % Constants.TICKS_PER_SECOND;
			}
		}, 0L, 1L);

		// Provide placeholder API replacements if it is present
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			new PlaceholderAPIIntegration(this).register();
		}

		// Register luckperms commands if LuckPerms is present
		if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
			new LuckPermsIntegration(this);
		}
	}

	//  Logic that is performed upon disabling the plugin.
	@Override
	public void onDisable() {
		mRedis.closePool();
		INSTANCE = null;
		getServer().getScheduler().cancelTasks(this);

		mTrackingManager.unloadTrackedEntities();
		mHttpManager.stop();
		mSocketManager.stop();
		mBossManager.unloadAll(true);
		MetadataUtils.removeAllMetadata(this);
	}

	public Player getPlayer(UUID playerID) {
		return getServer().getPlayer(playerID);
	}

	/* Sender will be sent debugging info if non-null */
	public void reloadMonumentaConfig(CommandSender sender) {
		ServerProperties.load(this, sender);
		mEnchantmentManager.load(ServerProperties.getForbiddenItemLore());
	}
}
