package com.playmonumenta.plugins;

import com.playmonumenta.plugins.abilities.AbilityHotbar;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.BlueStrikeDaggerCraftingBoss;
import com.playmonumenta.plugins.bosses.bosses.sirius.SiriusNPCBoss;
import com.playmonumenta.plugins.bosses.spells.SpellDetectionCircle;
import com.playmonumenta.plugins.chunk.ChunkManager;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.commands.*;
import com.playmonumenta.plugins.commands.experiencinator.ExperiencinatorCommand;
import com.playmonumenta.plugins.cosmetics.CosmeticsCommand;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.cosmetics.VanityManager;
import com.playmonumenta.plugins.custominventories.CustomInventoryCommands;
import com.playmonumenta.plugins.delves.DelvesCommands;
import com.playmonumenta.plugins.delves.DelvesManager;
import com.playmonumenta.plugins.depths.DepthsCommand;
import com.playmonumenta.plugins.depths.DepthsListener;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.guis.DepthsGUICommands;
import com.playmonumenta.plugins.discoveries.DiscoveryManager;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.explosions.ExplosionManager;
import com.playmonumenta.plugins.fishing.FishingCombatManager;
import com.playmonumenta.plugins.fishing.FishingManager;
import com.playmonumenta.plugins.gallery.GalleryCommands;
import com.playmonumenta.plugins.gallery.GalleryManager;
import com.playmonumenta.plugins.infinitytower.TowerCommands;
import com.playmonumenta.plugins.infinitytower.TowerManager;
import com.playmonumenta.plugins.integrations.ChestSortIntegration;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.integrations.PlaceholderAPIIntegration;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.integrations.TABIntegration;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.integrations.luckperms.listeners.Lockdown;
import com.playmonumenta.plugins.integrations.monumentanetworkrelay.BroadcastedEvents;
import com.playmonumenta.plugins.inventories.AnvilFixInInventory;
import com.playmonumenta.plugins.inventories.CustomContainerItemManager;
import com.playmonumenta.plugins.inventories.LootChestsInInventory;
import com.playmonumenta.plugins.inventories.PlayerInventoryView;
import com.playmonumenta.plugins.inventories.ShulkerInventoryManager;
import com.playmonumenta.plugins.inventories.WalletManager;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.infusions.StatTrackManager;
import com.playmonumenta.plugins.itemupdater.ItemUpdateManager;
import com.playmonumenta.plugins.listeners.*;
import com.playmonumenta.plugins.managers.DataCollectionManager;
import com.playmonumenta.plugins.managers.LoadoutManager;
import com.playmonumenta.plugins.managers.LootboxManager;
import com.playmonumenta.plugins.managers.PlayerSkinManager;
import com.playmonumenta.plugins.managers.PlaylistManager;
import com.playmonumenta.plugins.managers.TimeWarpManager;
import com.playmonumenta.plugins.market.MarketCommands;
import com.playmonumenta.plugins.market.MarketListener;
import com.playmonumenta.plugins.market.MarketManager;
import com.playmonumenta.plugins.minigames.chess.ChessManager;
import com.playmonumenta.plugins.minigames.pzero.PzeroManager;
import com.playmonumenta.plugins.mmquest.commands.MMQuest;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.network.HttpManager;
import com.playmonumenta.plugins.nodeplanner.NodePlanner;
import com.playmonumenta.plugins.overrides.ItemOverrides;
import com.playmonumenta.plugins.parrots.ParrotManager;
import com.playmonumenta.plugins.player.PlayerSaturationTracker;
import com.playmonumenta.plugins.player.activity.ActivityManager;
import com.playmonumenta.plugins.plots.AnimalLimits;
import com.playmonumenta.plugins.plots.PlotManager;
import com.playmonumenta.plugins.plots.ShopManager;
import com.playmonumenta.plugins.poi.POICommands;
import com.playmonumenta.plugins.poi.POIManager;
import com.playmonumenta.plugins.portals.PortalManager;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.protocollib.ProtocolLibIntegration;
import com.playmonumenta.plugins.seasonalevents.SeasonalEventCommand;
import com.playmonumenta.plugins.seasonalevents.SeasonalEventListener;
import com.playmonumenta.plugins.seasonalevents.SeasonalEventManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.server.reset.DailyReset;
import com.playmonumenta.plugins.spawners.SpawnerVisualisation;
import com.playmonumenta.plugins.spawnzone.SpawnZoneManager;
import com.playmonumenta.plugins.timers.CooldownTimers;
import com.playmonumenta.plugins.timers.ProjectileEffectTimers;
import com.playmonumenta.plugins.timers.ShowMarkerTimer;
import com.playmonumenta.plugins.tracking.TrackingManager;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.Nullable;

public class Plugin extends JavaPlugin {
	public static final boolean IS_PLAY_SERVER;
	public static final boolean ENABLE_TIME_WARP;

	static {
		/*
		 * Reads the environment variable MONUMENTA_IS_PLAY to determine if this is the build or play server
		 * If environment variable is not set or 0, build server. If set and nonzero, play server.
		 * This is stored into the scoreboard '$IsPlay const' in the onEnable() event
		 */
		String envIsPlay = System.getenv("MONUMENTA_IS_PLAY");
		if (envIsPlay == null || envIsPlay.isEmpty()) {
			IS_PLAY_SERVER = false;
		} else {
			boolean val;
			try {
				val = Integer.parseInt(envIsPlay) != 0;
			} catch (Exception ex) {
				val = false;
			}
			IS_PLAY_SERVER = val;
		}

		/*
		 * Reads the environment variable MONUMENTA_ENABLE_TIME_WARP to determine if /timewarp is allowed
		 * If environment variable is not set or 0, disabled. If set and nonzero, enabled.
		 */
		String envEnableTimeWarp = System.getenv("MONUMENTA_ENABLE_TIME_WARP");
		if (envEnableTimeWarp == null || envEnableTimeWarp.isEmpty()) {
			ENABLE_TIME_WARP = false;
		} else {
			boolean val;
			try {
				val = Integer.parseInt(envEnableTimeWarp) != 0;
			} catch (Exception ex) {
				val = false;
			}
			ENABLE_TIME_WARP = val;
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
	public ActivityManager mActivityManager;
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
	public VanityManager mVanityManager;
	public LoadoutManager mLoadoutManager;
	public PlaylistManager mPlaylistManager;
	public DataCollectionManager mDataCollectionManager;
	public PzeroManager mPzeroManager;
	public ShulkerEquipmentListener mShulkerEquipmentListener;
	public PlayerListener mPlayerListener;
	public GrapplingListener mGrapplingListener;
	public @Nullable AuditListener mAuditListener = null;
	private @Nullable CustomLogger mLogger = null;
	public @Nullable ProtocolLibIntegration mProtocolLibIntegration = null;

	// INSTANCE is set if the plugin is properly enabled
	@SuppressWarnings({"initialization.static.field.uninitialized", "NullAway.Init"})
	private static Plugin INSTANCE;

	public static Plugin getInstance() {
		return INSTANCE;
	}

	// fields are set as long as the plugin is properly enabled
	@SuppressWarnings({"initialization.fields.uninitialized", "NullAway.Init"})
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
		AbsorptionCommand.register();
		AdminNotify.register();
		AttributeModifierCommand.register();
		AuditLogCommand.register();
		BlueStrikeDaggerCraftingBoss.register();
		BoatUtilsCommand.register();
		BossDebug.register();
		BossFight.register();
		ChangeLogLevel.register();
		ChargeUpBarCommand.register(this);
		CharmsCommand.register();
		ClaimRaffle.register(this);
		ClearPortals.register();
		CooldownsCommand.register(this);
		CoreProtectLogCommand.register();
		CosmeticsCommand.register(this);
		CustomEffect.register();
		CustomInventoryCommands.register(this);
		DateVersionCommand.register();
		DeathMsg.register();
		DebugInfo.register(this);
		DelvesCommands.register(this);
		DiscoveryCommand.register();
		DungeonAccessCommand.register();
		EffectFromPotionCommand.register(this);
		Effect.register(this);
		Eggify.register();
		EventCommand.register();
		ExperiencinatorCommand.register();
		ForceCastSpell.register();
		GenerateCharms.register();
		GenerateItems.register();
		GetScoreCommand.register();
		GiveSoulbound.register();
		GlowingCommand.register();
		GraveCommand.register();
		HasBadWordCommand.register();
		ItemStatCommands.registerAttrCommand();
		ItemStatCommands.registerCharmCommand();
		ItemStatCommands.registerColorCommand();
		ItemStatCommands.registerConsumeCommand();
		ItemStatCommands.registerDelveInfusionTypeCommand();
		ItemStatCommands.registerEnchCommand();
		ItemStatCommands.registerFishCommand();
		ItemStatCommands.registerInfoCommand();
		ItemStatCommands.registerLoreCommand();
		ItemStatCommands.registerNameCommand();
		ItemStatCommands.registerRemoveCommand();
		ItemStatCommands.registerCopyCommand();
		JingleBells.register();
		Launch.register();
		LoadoutManagerCommand.register();
		MarketCommands.register();
		MMQuest.register(this);
		MonumentaDebug.register(this);
		MonumentaReload.register(this);
		MonumentaTrigger.register();
		NameMCVerify.register(this);
		if (!IS_PLAY_SERVER) {
			NodePlanner.registerCommands();
		}
		POICommands.register();
		PartialParticleCommand.register();
		ParticleUtilsCommand.register();
		ParticlesCommand.register();
		PersistentDataCommand.register();
		PickLevelAfterAnvils.register();
		Ping.register();
		PlayerCommand.register(this);
		PlayerItemStatsGUICommand.register(this);
		Portal1.register();
		Portal2.register();
		PZeroCommand.register();
		RedeemVoteRewards.register(this);
		RefreshClass.register(this);
		RegisterTorch.register();
		RepairItemCommand.register();
		ReportCommand.register();
		RemoveTags.register();
		RenameItemCommand.register();
		ResetClass.register();
		RestartEmptyCommand.register(this);
		RocketJump.register();
		GUITextures.register();
		RunWithPlaceholdersCommand.register();
		ScanChests.register();
		SeasonalEventCommand.register();
		SetActivity.register(this);
		SetMasterwork.register();
		SkillDescription.register(this);
		SkillSummary.register(this);
		ShardSorterCommand.register();
		SimulateLoot.register(this);
		SpawnerCountCommand.register();
		Spawn.register();
		SpawnPointReset.register();
		SpellDetectionCircle.registerCommand(this);
		StatTrackItem.register();
		Stuck.register();
		TeleportAsync.register();
		TeleportByScore.register();
		TellMiniMessage.register();
		TimeWarpCommand.register();
		DataCollectionCommand.register();
		ToggleSwap.register();
		ToggleTrail.register();
		UnsignBook.register();
		UpTimeCommand.register();
		UpdateChestItems.register();
		UpdateHeldItem.register();
		UpdatePlainName.register();
		UpdateStrikeChests.register();
		ViewActivity.register();
		VirtualFirmament.register();
		WalletCommand.register();
		WeaponDash.register();
		WorldNameCommand.register();
		BlockDisplayCommand.register();
		AddSpawnerEffectMarkersCommand.register();
		SiriusNPCBoss.register();
		EffectListCommand.register();
		PlayerSkinManagerCommand.register();
		ScanMobsCommand.register();
		WhatTableCommand.register();
		StatTrackAdd.register();

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
		PlotManager.registerCommands();

		mJunkItemsListener = new JunkItemListener();
		mItemDropListener = new ItemDropListener();
		mBlockInteractionsListener = new BlockInteractionsListener();
	}

	//  Logic that is performed upon enabling the plugin.
	@Override
	public void onEnable() {
		INSTANCE = this;

		TimeWarpManager.load();

		/*
		 * Set the score '$IsPlay const' to indicate whether this is the build (0) or play (1) server
		 * This is sourced from the environment variable MONUMENTA_IS_PLAY in the static init section at load time
		 * This is used by mechanisms to test if this is the build server or the play server, like:
		 * /execute if score $IsPlay const matches 1 run ...
		 */
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		Objective obj = scoreboard.getObjective("const");
		if (obj == null) {
			obj = scoreboard.registerNewObjective("const", Criteria.DUMMY, Component.text("const"));
		}
		obj.getScore("$IsPlay").setScore(IS_PLAY_SERVER ? 1 : 0);
		getLogger().info("Setting $IsPlay const = " + (IS_PLAY_SERVER ? 1 : 0) + " (" + (IS_PLAY_SERVER ? "play" : "build") + " server)");

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
		SeasonalEventManager.enable();
		mActivityManager = new ActivityManager(this);
		mVanityManager = new VanityManager();
		mLoadoutManager = new LoadoutManager();
		mPlaylistManager = new PlaylistManager();
		mDataCollectionManager = new DataCollectionManager();
		mPzeroManager = new PzeroManager();
		mShulkerEquipmentListener = new ShulkerEquipmentListener(this);
		mPlayerListener = new PlayerListener(this);
		mGrapplingListener = new GrapplingListener();

		new ClientModHandler(this);
		mCharmManager = CharmManager.getInstance();
		new AbilityHotbar(this);

		PlayerSaturationTracker.startTracking(this);

		DailyReset.startTimer(this);

		SpawnerCommand.register();

		//  Load info.
		reloadMonumentaConfig(null);

		MarketManager.reloadConfig();

		// Chunk loading/unloading helper
		manager.registerEvents(new ChunkManager(this), this);

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
			mAuditListener = new AuditListener(getLogger());
			manager.registerEvents(mAuditListener, this);
		}
		if (!IS_PLAY_SERVER) {
			manager.registerEvents(NodePlanner.getInstance(), this);
		}
		manager.registerEvents(new AnimalLimits(), this);
		manager.registerEvents(new ExceptionListener(this), this);
		manager.registerEvents(mPlayerListener, this);
		manager.registerEvents(new MobListener(this), this);
		manager.registerEvents(new EntityListener(this, mAbilityManager), this);
		manager.registerEvents(new VehicleListener(this), this);
		manager.registerEvents(new WorldListener(this), this);
		manager.registerEvents(new ShulkerShortcutListener(this), this);
		manager.registerEvents(mShulkerEquipmentListener, this);
		manager.registerEvents(new ExplosionManager(), this);
		manager.registerEvents(new LootboxManager(this), this);
		manager.registerEvents(new PortableEnderListener(), this);
		manager.registerEvents(new PotionConsumeListener(this), this);
		manager.registerEvents(new ZoneListener(), this);
		manager.registerEvents(new TridentListener(), this);
		manager.registerEvents(new CrossbowListener(this), this);
		manager.registerEvents(new FishListener(), this);
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
		LootTableManager.INSTANCE.reload();
		manager.registerEvents(LootTableManager.INSTANCE, this);
		manager.registerEvents(new CharmListener(this), this);
		manager.registerEvents(new QuiverListener(), this);
		manager.registerEvents(new ToggleTrail(), this);
		manager.registerEvents(mVanityManager, this);
		manager.registerEvents(mLoadoutManager, this);
		manager.registerEvents(mPlaylistManager, this);
		manager.registerEvents(POIManager.getInstance(), this);
		manager.registerEvents(new BrokenEquipmentListener(), this);
		manager.registerEvents(PortalManager.getInstance(), this);
		manager.registerEvents(new LootingLimiter(), this);
		manager.registerEvents(new InventoryUpdateListener(), this);
		WalletManager.initialize(new Location(Bukkit.getWorlds().get(0), 0, 0, 0));
		manager.registerEvents(new WalletManager(), this);
		manager.registerEvents(new CustomContainerItemManager(), this);
		manager.registerEvents(StatTrackManager.getInstance(), this);
		manager.registerEvents(new PotionBarrelListener(), this);
		manager.registerEvents(TemporaryBlockChangeManager.INSTANCE, this);
		manager.registerEvents(new TorchListener(), this);
		manager.registerEvents(new MarketListener(), this);
		manager.registerEvents(mPzeroManager, this);
		manager.registerEvents(new Lockdown(), this);
		manager.registerEvents(new IchorListener(), this);
		manager.registerEvents(new DiscoveryManager(), this);
		manager.registerEvents(mGrapplingListener, this);
		manager.registerEvents(new CelestialGemListener(), this);
		new SpawnerVisualisation().register();

		if (ServerProperties.getDepthsEnabled()) {
			manager.registerEvents(new DepthsListener(), this);
		}

		if (ServerProperties.getShardName().contains("gallery")
			|| ServerProperties.getShardName().startsWith("dev")) {
			GalleryCommands.register();
			manager.registerEvents(new GalleryManager(this), this);
		}

		if (ServerProperties.getShardName().contains("ring")
			|| ServerProperties.getShardName().startsWith("dev")) {
			FishingCombatManager fishingCombatManager = new FishingCombatManager();
			manager.registerEvents(fishingCombatManager, this);
			manager.registerEvents(new FishingManager(fishingCombatManager), this);
		}

		//TODO Move the logic out of Plugin and into it's own class that derives off Runnable, a Timer class of some type.
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			int mTicks = 0;

			@Override
			public void run() {
				final boolean oneHertz = (mTicks % 20) == 0;
				final boolean twoHertz = (mTicks % 10) == 0;
				final boolean fourHertz = (mTicks % 5) == 0;

				// Every 5 ticks - 4 times a second
				if (fourHertz) {
					// Update ability cooldowns
					try {
						mTimers.updateCooldowns(Constants.QUARTER_TICKS_PER_SECOND);
					} catch (Exception e) {
						e.printStackTrace();
					}

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
				try {
					mTrackingManager.mPlayers.update(1);
				} catch (Exception e) {
					e.printStackTrace();
				}

				// Play particles at tracked projectiles
				try {
					mProjectileEffectTimers.update();
				} catch (Exception e) {
					e.printStackTrace();
				}

				// Show marker entities
				try {
					ShowMarkerTimer.update();
				} catch (Exception e) {
					e.printStackTrace();
				}

				// Update everything related to the PZero Minigame
				try {
					mPzeroManager.update(oneHertz, twoHertz, fourHertz);
				} catch (Exception e) {
					e.printStackTrace();
				}

				// Updates related to discoveries
				try {
					DiscoveryManager.update();
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
			manager.registerEvents(new MonumentaNetworkRelayIntegration(this.getLogger()), this);

			//relies on it, so only register if the plugin is available.
			BroadcastedEvents.registerCommand(this);
			manager.registerEvents(new BroadcastedEvents(), this);
			BroadcastedEvents.registerTask(this);
		}

		// Hook into Library of Souls for mob management if available
		if (Bukkit.getPluginManager().isPluginEnabled("LibraryOfSouls")) {
			BossTagCommand.register();
			LibraryOfSoulsIntegration.enable(this.getLogger());
		}

		// Enable Monumenta Network Chat integration
		if (Bukkit.getPluginManager().isPluginEnabled("MonumentaNetworkChat")) {
			MonumentaNetworkChatIntegration.onEnable(this.getLogger());
		}

		// Provide placeholder API replacements if it is present
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			new PlaceholderAPIIntegration(this).register();
		}

		// Log things in CoreProtect if it is present
		if (Bukkit.getPluginManager().isPluginEnabled("CoreProtect")) {
			CoreProtectIntegration.enable(this.getLogger());
		}

		// Register luckperms commands if LuckPerms is present
		if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
			manager.registerEvents(new LuckPermsIntegration(this), this);
		}

		// Hook into PremiumVanish if present
		if (Bukkit.getPluginManager().isPluginEnabled("PremiumVanish")) {
			PremiumVanishIntegration.enable(this.getLogger());
		}

		if (Bukkit.getPluginManager().isPluginEnabled("TAB")) {
			manager.registerEvents(new TABIntegration(), this);
		}

		// Hook into ProtocolLib if present
		if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
			// load skins before this
			new PlayerSkinManager();
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
			MonumentaClasses classes = new MonumentaClasses();
			FileUtils.writeJson(skillExportPath, classes.toJson());
		} catch (Exception e) {
			// Failed to export skills to json, non-critical error.
			getLogger().warning("Failed to export skills.");
		}

		/* If this is the depths shard, enable depths manager */
		if (ServerProperties.getDepthsEnabled()) {
			new DepthsManager(this, getLogger(), getDataFolder() + File.separator + "depths");
			DepthsCommand.register(this);
			DepthsGUICommands.register();
		}
	}

	//  Logic that is performed upon disabling the plugin.
	@Override
	@SuppressWarnings("NullAway") // we set INSTANCE to null to find bugs easier
	public void onDisable() {
		INSTANCE = null;
		getServer().getScheduler().cancelTasks(this);

		TimeWarpManager.unload();

		if (ServerProperties.getShardName().contains("gallery")) {
			GalleryManager.close(); //TODO - test this
		}

		for (World world : Bukkit.getWorlds()) {
			for (Entity entity : world.getEntities()) {
				for (String tag : entity.getScoreboardTags()) {
					if (tag.contains(DelvesManager.PHANTOM_NAME)) {
						entity.remove();
					}
				}
			}
		}

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
	public void reloadMonumentaConfig(@Nullable CommandSender sender) {
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
