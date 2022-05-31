package com.playmonumenta.plugins;

import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.spells.SpellDetectionCircle;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.commands.*;
import com.playmonumenta.plugins.commands.experiencinator.ExperiencinatorCommand;
import com.playmonumenta.plugins.cosmetics.CosmeticsCommand;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.custominventories.CustomInventoryCommands;
import com.playmonumenta.plugins.delves.DelvesCommands;
import com.playmonumenta.plugins.delves.DelvesManager;
import com.playmonumenta.plugins.depths.DepthsCommand;
import com.playmonumenta.plugins.depths.DepthsGUICommands;
import com.playmonumenta.plugins.depths.DepthsListener;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.guis.SinglePageGUIManager;
import com.playmonumenta.plugins.infinitytower.TowerCommands;
import com.playmonumenta.plugins.infinitytower.TowerManager;
import com.playmonumenta.plugins.integrations.ChestSortIntegration;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.integrations.PlaceholderAPIIntegration;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.inventories.AnvilFixInInventory;
import com.playmonumenta.plugins.inventories.LootChestsInInventory;
import com.playmonumenta.plugins.inventories.PlayerInventoryView;
import com.playmonumenta.plugins.inventories.ShulkerInventoryManager;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemupdater.ItemUpdateManager;
import com.playmonumenta.plugins.listeners.*;
import com.playmonumenta.plugins.minigames.chess.ChessManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.network.HttpManager;
import com.playmonumenta.plugins.overrides.ItemOverrides;
import com.playmonumenta.plugins.parrots.ParrotManager;
import com.playmonumenta.plugins.plots.PlotManager;
import com.playmonumenta.plugins.plots.ShopManager;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.protocollib.ProtocolLibIntegration;
import com.playmonumenta.plugins.seasonalevents.SeasonalEventCommand;
import com.playmonumenta.plugins.seasonalevents.SeasonalEventListener;
import com.playmonumenta.plugins.seasonalevents.SeasonalEventManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.server.reset.DailyReset;
import com.playmonumenta.plugins.spawnzone.SpawnZoneManager;
import com.playmonumenta.plugins.timers.CooldownTimers;
import com.playmonumenta.plugins.timers.ProjectileEffectTimers;
import com.playmonumenta.plugins.tracking.TrackingManager;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class Plugin extends JavaPlugin {
	public static final boolean IS_PLAY_SERVER;

	static {
		/*
		 * Reads the environment variable MONUMENTA_IS_PLAY to determine if this is the build or play server
		 * If environment variable is not set or 0, build server. If set and nonzero, play server.
		 * This is stored into the scoreboard '$IsPlay const' in the onEnable() event
		 */
		String shardName = System.getenv("MONUMENTA_IS_PLAY");
		if (shardName == null || shardName.isEmpty()) {
			IS_PLAY_SERVER = false;
		} else {
			boolean val;
			try {
				val = Integer.parseInt(shardName) != 0;
			} catch (Exception ex) {
				val = false;
			}
			IS_PLAY_SERVER = val;
		}
	}

	public CooldownTimers mTimers;
	public ProjectileEffectTimers mProjectileEffectTimers;

	public JunkItemListener mJunkItemsListener;
	public ItemDropListener mItemDropListener;
	public BlockInteractionsListener mBlockInteractionsListener;
	private @Nullable HttpManager mHttpManager = null;
	public TrackingManager mTrackingManager;
	public PotionManager mPotionManager;
	public SpawnZoneManager mZoneManager;
	public AbilityManager mAbilityManager;
	public ShulkerInventoryManager mShulkerInventoryManager;
	public BossManager mBossManager;
	public EffectManager mEffectManager;
	public ParrotManager mParrotManager;
	public ItemStatManager mItemStatManager;
	public ChessManager mChessManager;
	public TowerManager mTowerManager;
	public SignUtils mSignUtils;
	public CharmManager mCharmManager;
	public ItemOverrides mItemOverrides;
	public CosmeticsManager mCosmeticsManager;
	public SeasonalEventManager mSeasonalEventManager;
	private @Nullable CustomLogger mLogger = null;
	public @Nullable ProtocolLibIntegration mProtocolLibIntegration = null;

	// INSTANCE is set if the plugin is properly enabled
	@SuppressWarnings("initialization.static.field.uninitialized")
	private static Plugin INSTANCE;

	public static Plugin getInstance() {
		return INSTANCE;
	}

	// fields are set as long as the plugin is properly enabled
	@SuppressWarnings("initialization.fields.uninitialized")
	public Plugin() {
	}

	@Override
	public void onLoad() {
		if (mLogger == null) {
			mLogger = new CustomLogger(super.getLogger(), Level.INFO);
		}

		NmsUtils.loadVersionAdapter(this.getServer().getClass(), getLogger());

		/*
		 * CommandAPI commands which register directly and are usable in functions
		 *
		 * These need to register immediately on load to prevent function loading errors
		 */
		ChangeLogLevel.register();
		GiveSoulbound.register();
		ClaimRaffle.register(this);
		DateVersionCommand.register();
		ShatterHeldItem.register();
		CalculateReforge.register();
		ReforgeHeldItem.register();
		ReforgeInventory.register();
		DebugInfo.register(this);
		BossDebug.register();
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
		TeleportAsync.register();
		TeleportByScore.register();
		UpdateHeldItem.register();
		UpTimeCommand.register();
		Portal1.register();
		Portal2.register();
		ClearPortals.register();
		Launch.register();
		Magnetize.register();
		UnsignBook.register();
		GetScoreCommand.register();
		GraveCommand.register();
		StatTrackItem.register();
		ToggleSwap.register(this);
		CustomInventoryCommands.register(this);
		DelvesCommands.register(this);
		AdminNotify.register();
		ItemStatUtils.registerInfoCommand();
		ItemStatUtils.registerLoreCommand();
		ItemStatUtils.registerNameCommand();
		ItemStatUtils.registerEnchCommand();
		ItemStatUtils.registerAttrCommand();
		ItemStatUtils.registerCharmCommand();
		ItemStatUtils.registerConsumeCommand();
		ItemStatUtils.registerRemoveCommand();
		PlayerItemStatsGUICommand.register(this);
		AuditLogCommand.register();
		PickLevelAfterAnvils.register();
		GenerateItems.register();
		GenerateCharms.register();
		JingleBells.register();
		Spawn.register();
		Stuck.register(this);
		GlowingCommand.register();
		VirtualFirmament.register();
		ExperiencinatorCommand.register();
		EventCommand.register();
		Eggify.register();
		SeasonalEventCommand.register(this);
		CosmeticsCommand.register(this);
		NameMCVerify.register(this);
		TellMiniMessage.register();
		RunWithPlaceholdersCommand.register();
		PartialParticleCommand.register();
		CustomEffect.register();
		EffectFromPotionCommand.register(this);
		CharmsCommand.register(this);
		WorldNameCommand.register();
		ToggleTrail.register();


		try {
			mHttpManager = new HttpManager(this);
		} catch (IOException err) {
			getLogger().warning("HTTP manager failed to start");
			err.printStackTrace();
		}

		ServerProperties.load(this, null);

		/* If this is the plots shard, register /plotaccess functions and enable functionality */
		if (ServerProperties.getShardName().equals("plots")
				|| ServerProperties.getShardName().equals("mobs")
				|| ServerProperties.getShardName().equals("dev1")
				|| ServerProperties.getShardName().equals("dev2")) {
			ShopManager.registerCommands();
		}

		/* Plot commands are valid on all shards */
		new PlotManager();

		mJunkItemsListener = new JunkItemListener();
		mItemDropListener = new ItemDropListener();
		mBlockInteractionsListener = new BlockInteractionsListener();
	}

	//  Logic that is performed upon enabling the plugin.
	@Override
	public void onEnable() {
		INSTANCE = this;

		/*
		 * Set the score '$IsPlay const' to indicate whether this is the build (0) or play (1) server
		 * This is sourced from the environment variable MONUMENTA_IS_PLAY in the static init section at load time
		 * This is used by mechanisms to test if this is the build server or the play server, like:
		 * /execute if score $IsPlay const matches 1 run ...
		 */
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		Objective obj = scoreboard.getObjective("const");
		if (obj == null) {
			obj = scoreboard.registerNewObjective("const", "dummy", Component.text("const"));
		}
		obj.getScore("$IsPlay").setScore(IS_PLAY_SERVER ? 1 : 0);
		getLogger().info("Setting $IsPlay const = " + Integer.toString(IS_PLAY_SERVER ? 1 : 0) + " (" + (IS_PLAY_SERVER ? "play" : "build") + " server)");

		PluginManager manager = getServer().getPluginManager();

		if (mHttpManager != null) {
			mHttpManager.start();
		}

		mItemOverrides = new ItemOverrides();

		//  Initialize Variables.
		mTimers = new CooldownTimers(this);

		mProjectileEffectTimers = new ProjectileEffectTimers(this);

		mPotionManager = new PotionManager();
		mTrackingManager = new TrackingManager(this);
		mZoneManager = new SpawnZoneManager(this);
		mAbilityManager = new AbilityManager(this);
		mShulkerInventoryManager = new ShulkerInventoryManager(this);
		mBossManager = new BossManager(this);
		mEffectManager = new EffectManager(this);
		mParrotManager = new ParrotManager(this);
		mItemStatManager = new ItemStatManager(this);
		mChessManager = new ChessManager(this);
		mSignUtils = new SignUtils(this);
		mTowerManager = new TowerManager(this);
		mCosmeticsManager = CosmeticsManager.getInstance();
		mSeasonalEventManager = new SeasonalEventManager();

		new ClientModHandler(this);
		mCharmManager = CharmManager.getInstance();

		DailyReset.startTimer(this);

		//  Load info.
		reloadMonumentaConfig(null);

		// These are both a command and an event listener
		manager.registerEvents(new Spectate(this), this);
		manager.registerEvents(new SpectateBot(this), this);

		/* If this is the plots shard, register /plotaccess functions and enable functionality */
		if (ServerProperties.getShardName().equals("plots")
			|| ServerProperties.getShardName().equals("mobs")
			|| ServerProperties.getShardName().equals("dev1")
			|| ServerProperties.getShardName().equals("dev2")) {
			manager.registerEvents(new ShopManager(), this);
		}

		if (ServerProperties.getShardName().contains("valley")
			|| ServerProperties.getShardName().contains("dev")) {

			//minigames can only be on devshard or valley
			TowerCommands.register(this);
			manager.registerEvents(mChessManager, this);
			manager.registerEvents(mTowerManager, this);
		}

		if (ServerProperties.getShardName().contains("mobs")) {
			TowerCommands.registerDesign(this);
		}

		if (ServerProperties.getAuditMessagesEnabled()) {
			manager.registerEvents(new AuditListener(getLogger()), this);
		}
		manager.registerEvents(new ExceptionListener(this), this);
		manager.registerEvents(new PlayerListener(this), this);
		manager.registerEvents(new MobListener(this), this);
		manager.registerEvents(new EntityListener(this, mAbilityManager), this);
		manager.registerEvents(new VehicleListener(this), this);
		manager.registerEvents(new WorldListener(this), this);
		manager.registerEvents(new ShulkerShortcutListener(this), this);
		manager.registerEvents(new ShulkerEquipmentListener(this), this);
		manager.registerEvents(new PortableEnderListener(), this);
		manager.registerEvents(new ShatteredEquipmentListener(), this);
		manager.registerEvents(new PotionConsumeListener(this), this);
		manager.registerEvents(new ZoneListener(), this);
		manager.registerEvents(new TridentListener(), this);
		manager.registerEvents(new CrossbowListener(this), this);
		manager.registerEvents(mJunkItemsListener, this);
		manager.registerEvents(mItemDropListener, this);
		manager.registerEvents(mBlockInteractionsListener, this);
		manager.registerEvents(mBossManager, this);
		manager.registerEvents(mEffectManager, this);
		manager.registerEvents(mParrotManager, this);
		manager.registerEvents(new DelvesManager(), this);
		manager.registerEvents(new SpawnerListener(this), this);
		manager.registerEvents(new PlayerInventoryView(), this);
		manager.registerEvents(new AnvilFixInInventory(this), this);
		manager.registerEvents(new LootChestsInInventory(), this);
		manager.registerEvents(new ArrowListener(this), this);
		manager.registerEvents(new SinglePageGUIManager(), this);
		manager.registerEvents(new GraveListener(this), this);
		manager.registerEvents(new BrewingListener(), this);
		manager.registerEvents(new ItemUpdateManager(this), this);
		manager.registerEvents(new DamageListener(this), this);
		manager.registerEvents(mItemStatManager, this);
		manager.registerEvents(new StasisListener(), this);
		manager.registerEvents(new TradeListener(), this);
		manager.registerEvents(new WitchListener(this), this);
		manager.registerEvents(new SeasonalEventListener(), this);
		manager.registerEvents(CosmeticsManager.getInstance(), this);
		manager.registerEvents(new LootTableManager(), this);
		manager.registerEvents(new CharmListener(this), this);
		manager.registerEvents(new QuiverListener(), this);
		manager.registerEvents(new ToggleTrail(), this);

		if (ServerProperties.getShardName().contains("depths")
				|| ServerProperties.getShardName().equals("mobs")
				|| ServerProperties.getShardName().startsWith("dev")) {
			manager.registerEvents(new DepthsListener(this), this);
		}

		//TODO Move the logic out of Plugin and into it's own class that derives off Runnable, a Timer class of some type.
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			int mTicks = 0;

			@Override
			public void run() {
				final boolean oneHertz = (mTicks % 20) == 0;
				final boolean twoHertz = (mTicks % 10) == 0;
				final boolean fourHertz = (mTicks % 5) == 0;

				// Every 10 ticks - 2 times a second
				if (twoHertz) {
					//  Update cooldowns.
					try {
						mTimers.updateCooldowns(Constants.HALF_TICKS_PER_SECOND);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				// Every 5 ticks - 4 times a second
				if (fourHertz) {
					for (Player player : mTrackingManager.mPlayers.getPlayers()) {
						try {
							mAbilityManager.periodicTrigger(player, twoHertz, oneHertz, mTicks);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					try {
						mTrackingManager.update(Constants.QUARTER_TICKS_PER_SECOND);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				// Every tick - 20 times a second
				// Update cooldowns
				try {
					mProjectileEffectTimers.update();
				} catch (Exception e) {
					e.printStackTrace();
				}

				mTicks = (mTicks + 1) % Constants.TICKS_PER_SECOND;
			}
		}, 0L, 1L);

		// Hook into JeffChestSort for custom chest sorting if present
		if (Bukkit.getPluginManager().isPluginEnabled("ChestSort")) {
			manager.registerEvents(new ChestSortIntegration(this), this);
		}

		// Hook into Monumenta Redis Sync for server transfers if available
		if (Bukkit.getPluginManager().isPluginEnabled("MonumentaRedisSync")) {
			manager.registerEvents(new MonumentaRedisSyncIntegration(this), this);
		}

		// Hook into Monumenta Network Relay for message brokering if available
		if (Bukkit.getPluginManager().isPluginEnabled("MonumentaNetworkRelay")) {
			new MonumentaNetworkRelayIntegration(this.getLogger());
		}

		// Hook into Library of Souls for mob management if available
		if (Bukkit.getPluginManager().isPluginEnabled("LibraryOfSouls")) {
			BossTagCommand.register();
			new LibraryOfSoulsIntegration(this.getLogger());
		}

		// Provide placeholder API replacements if it is present
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			new PlaceholderAPIIntegration(this).register();
		}

		// Log things in CoreProtect if it is present
		if (Bukkit.getPluginManager().isPluginEnabled("CoreProtect")) {
			new CoreProtectIntegration(this.getLogger());
		}

		// Register luckperms commands if LuckPerms is present
		if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
			new LuckPermsIntegration(this);
		}

		// Hook into PremiumVanish if present
		if (Bukkit.getPluginManager().isPluginEnabled("PremiumVanish")) {
			new PremiumVanishIntegration(this.getLogger());
		}

		// Hook into ProtocolLib if present
		if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
			mProtocolLibIntegration = new ProtocolLibIntegration(this);
			if (Bukkit.getPluginManager().isPluginEnabled("PrometheusExporter")) {
				PacketMonitoringCommand.register(this);
			}
		}

		// Register the explosion repair mechanism if BKCommonLib is present
		if (Bukkit.getPluginManager().isPluginEnabled("BKCommonLib")) {
			manager.registerEvents(new RepairExplosionsListener(this), this);
		}

		// Export class/skill info
		try {
			String skillExportPath = getDataFolder() + File.separator + "exported_skills.json";
			MonumentaClasses classes = new MonumentaClasses(this, (Player) null);
			FileUtils.writeJson(skillExportPath, classes.toJson());
		} catch (Exception e) {
			// Failed to export skills to json, non-critical error.
			getLogger().warning("Failed to export skills.");
		}

		/* If this is the depths shard, enable depths manager */
		if (ServerProperties.getShardName().contains("depths")
			|| ServerProperties.getShardName().equals("mobs")
			|| ServerProperties.getShardName().startsWith("dev")) {
			new DepthsManager(this, getLogger(), getDataFolder() + File.separator + "depths");
			DepthsCommand.register(this);
			DepthsGUICommands.register(this);
		}
	}

	//  Logic that is performed upon disabling the plugin.
	@Override
	public void onDisable() {
		INSTANCE = null;
		getServer().getScheduler().cancelTasks(this);

		TowerManager.unload();
		mChessManager.unloadAll();
		mTrackingManager.unloadTrackedEntities();
		if (mHttpManager != null) {
			mHttpManager.stop();
		}
		mBossManager.unloadAll(true);
		MetadataUtils.removeAllMetadata(this);
	}

	public @Nullable Player getPlayer(UUID playerID) {
		return getServer().getPlayer(playerID);
	}

	/* Sender will be sent debugging info if non-null */
	public void reloadMonumentaConfig(CommandSender sender) {
		ServerProperties.load(this, sender);
	}

	@Override
	public Logger getLogger() {
		if (mLogger == null) {
			mLogger = new CustomLogger(super.getLogger(), Level.INFO);
		}
		return mLogger;
	}
}
