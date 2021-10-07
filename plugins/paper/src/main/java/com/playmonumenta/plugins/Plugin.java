package com.playmonumenta.plugins;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.attributes.AttributeManager;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.spells.SpellDetectionCircle;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.commands.AdminNotify;
import com.playmonumenta.plugins.commands.BarkifyHeldItem;
import com.playmonumenta.plugins.commands.BossDebug;
import com.playmonumenta.plugins.commands.BossFight;
import com.playmonumenta.plugins.commands.CalculateReforge;
import com.playmonumenta.plugins.commands.ClaimRaffle;
import com.playmonumenta.plugins.commands.ClearPortals;
import com.playmonumenta.plugins.commands.ColossalifyHeldItem;
import com.playmonumenta.plugins.commands.DeBarkifyHeldItem;
import com.playmonumenta.plugins.commands.DeCluckifyHeldItem;
import com.playmonumenta.plugins.commands.DeathMsg;
import com.playmonumenta.plugins.commands.DebugInfo;
import com.playmonumenta.plugins.commands.DelveInfuseHeldItem;
import com.playmonumenta.plugins.commands.Effect;
import com.playmonumenta.plugins.commands.FestiveHeldItem;
import com.playmonumenta.plugins.commands.GetDepthPoints;
import com.playmonumenta.plugins.commands.GetScoreCommand;
import com.playmonumenta.plugins.commands.GildifyHeldItem;
import com.playmonumenta.plugins.commands.GiveSoulbound;
import com.playmonumenta.plugins.commands.Grave;
import com.playmonumenta.plugins.commands.HopeifyHeldItem;
import com.playmonumenta.plugins.commands.InfuseHeldItem;
import com.playmonumenta.plugins.commands.Launch;
import com.playmonumenta.plugins.commands.LockedHeldItem;
import com.playmonumenta.plugins.commands.Magnetize;
import com.playmonumenta.plugins.commands.MonumentaDebug;
import com.playmonumenta.plugins.commands.MonumentaReload;
import com.playmonumenta.plugins.commands.OpenDelveModifierSelectionGUI;
import com.playmonumenta.plugins.commands.PhylacteryifyHeldItem;
import com.playmonumenta.plugins.commands.Portal1;
import com.playmonumenta.plugins.commands.Portal2;
import com.playmonumenta.plugins.commands.RedeemVoteRewards;
import com.playmonumenta.plugins.commands.ReforgeHeldItem;
import com.playmonumenta.plugins.commands.ReforgeInventory;
import com.playmonumenta.plugins.commands.RefreshClass;
import com.playmonumenta.plugins.commands.RemoveTags;
import com.playmonumenta.plugins.commands.RestartEmptyCommand;
import com.playmonumenta.plugins.commands.RunRegion;
import com.playmonumenta.plugins.commands.SetViewDistance;
import com.playmonumenta.plugins.commands.ShatterHeldItem;
import com.playmonumenta.plugins.commands.SkillDescription;
import com.playmonumenta.plugins.commands.SkillSummary;
import com.playmonumenta.plugins.commands.Spectate;
import com.playmonumenta.plugins.commands.SpectateBot;
import com.playmonumenta.plugins.commands.StasisCommand;
import com.playmonumenta.plugins.commands.StatTrackItem;
import com.playmonumenta.plugins.commands.TeleportAsync;
import com.playmonumenta.plugins.commands.TeleportByScore;
import com.playmonumenta.plugins.commands.ToggleSwap;
import com.playmonumenta.plugins.commands.UnlockHeldItem;
import com.playmonumenta.plugins.commands.UnsignBook;
import com.playmonumenta.plugins.commands.UpdateHeldItem;
import com.playmonumenta.plugins.custominventories.CustomInventoryCommands;
import com.playmonumenta.plugins.depths.DepthsCommand;
import com.playmonumenta.plugins.depths.DepthsGUICommands;
import com.playmonumenta.plugins.depths.DepthsListener;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.enchantments.EnchantmentManager;
import com.playmonumenta.plugins.guis.SinglePageGUIManager;
import com.playmonumenta.plugins.integrations.ChestSortIntegration;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.integrations.PlaceholderAPIIntegration;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.integrations.ProtocolLibIntegration;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.inventories.AnvilFixInInventory;
import com.playmonumenta.plugins.inventories.LootChestsInInventory;
import com.playmonumenta.plugins.inventories.PlayerInventoryView;
import com.playmonumenta.plugins.inventories.ShatterCoinInInventory;
import com.playmonumenta.plugins.inventories.ShulkerInventoryManager;
import com.playmonumenta.plugins.itemindex.IndexInventoryListeners;
import com.playmonumenta.plugins.itemindex.IndexInventoryManager;
import com.playmonumenta.plugins.itemindex.ItemIndexCommand;
import com.playmonumenta.plugins.itemindex.ItemManager;
import com.playmonumenta.plugins.itemupdater.ItemUpdateManager;
import com.playmonumenta.plugins.listeners.ArrowListener;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.listeners.BrewingListener;
import com.playmonumenta.plugins.listeners.CrossbowListener;
import com.playmonumenta.plugins.listeners.DeathItemListener;
import com.playmonumenta.plugins.listeners.DelvesListener;
import com.playmonumenta.plugins.listeners.EntityListener;
import com.playmonumenta.plugins.listeners.ExceptionListener;
import com.playmonumenta.plugins.listeners.GraveListener;
import com.playmonumenta.plugins.listeners.JunkItemListener;
import com.playmonumenta.plugins.listeners.MobListener;
import com.playmonumenta.plugins.listeners.PlayerListener;
import com.playmonumenta.plugins.listeners.PortableEnderListener;
import com.playmonumenta.plugins.listeners.PotionConsumeListener;
import com.playmonumenta.plugins.listeners.RepairExplosionsListener;
import com.playmonumenta.plugins.listeners.ShatteredEquipmentListener;
import com.playmonumenta.plugins.listeners.ShulkerEquipmentListener;
import com.playmonumenta.plugins.listeners.ShulkerShortcutListener;
import com.playmonumenta.plugins.listeners.SpawnerListener;
import com.playmonumenta.plugins.listeners.TridentListener;
import com.playmonumenta.plugins.listeners.VehicleListener;
import com.playmonumenta.plugins.listeners.WorldListener;
import com.playmonumenta.plugins.listeners.ZonePropertyListener;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.network.HttpManager;
import com.playmonumenta.plugins.overrides.ItemOverrides;
import com.playmonumenta.plugins.parrots.ParrotManager;
import com.playmonumenta.plugins.plots.PlotAccessManager;
import com.playmonumenta.plugins.plots.ShopManager;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.server.reset.DailyReset;
import com.playmonumenta.plugins.spawnzone.SpawnZoneManager;
import com.playmonumenta.plugins.timers.CombatLoggingTimers;
import com.playmonumenta.plugins.timers.CooldownTimers;
import com.playmonumenta.plugins.timers.ProjectileEffectTimers;
import com.playmonumenta.plugins.tracking.TrackingManager;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;

public class Plugin extends JavaPlugin {
	public CooldownTimers mTimers = null;
	public ProjectileEffectTimers mProjectileEffectTimers = null;
	public CombatLoggingTimers mCombatLoggingTimers = null;
	int mPeriodicTimer = -1;

	public EnchantmentManager mEnchantmentManager;
	public AttributeManager mAttributeManager;
	public JunkItemListener mJunkItemsListener;
	private HttpManager mHttpManager = null;
	public TrackingManager mTrackingManager;
	public PotionManager mPotionManager;
	public SpawnZoneManager mZoneManager;
	public AbilityManager mAbilityManager;
	public ShulkerInventoryManager mShulkerInventoryManager;
	private BossManager mBossManager;
	public ItemManager mItemManager;
	public IndexInventoryManager mIndexInventoryManager;
	public EffectManager mEffectManager;
	public ParrotManager mParrotManager;

	public DeathItemListener mDeathItemListener;

	public ItemOverrides mItemOverrides;

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
		StasisCommand.register();
		GiveSoulbound.register();
		HopeifyHeldItem.register();
		ColossalifyHeldItem.register();
		FestiveHeldItem.register();
		GildifyHeldItem.register();
		InfuseHeldItem.register();
		ClaimRaffle.register(this);
		BarkifyHeldItem.register();
		DeCluckifyHeldItem.register();
		ShatterHeldItem.register();
		DeBarkifyHeldItem.register();
		PhylacteryifyHeldItem.register();
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
		RedeemVoteRewards.register(this.getLogger());
		BossFight.register();
		SpellDetectionCircle.registerCommand(this);
		RunRegion.register();
		SkillDescription.register(this);
		SkillSummary.register(this);
		ItemIndexCommand.register();
		TeleportAsync.register();
		TeleportByScore.register();
		UpdateHeldItem.register();
		Portal1.register();
		Portal2.register();
		ClearPortals.register();
		Launch.register();
		OpenDelveModifierSelectionGUI.register();
		GetDepthPoints.register();
		Magnetize.register();
		SetViewDistance.register();
		UnsignBook.register();
		GetScoreCommand.register(this);
		Grave.register();
		StatTrackItem.register();
		LockedHeldItem.register();
		UnlockHeldItem.register();
		ToggleSwap.register(this);
		DelveInfuseHeldItem.register();
		CustomInventoryCommands.register(this);
		AdminNotify.register();


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
			new PlotAccessManager(getLogger(), getDataFolder() + File.separator + "plot_access.json");
			ShopManager.registerCommands();
		}

		mEnchantmentManager = new EnchantmentManager(this);
		mEnchantmentManager.load();

		mAttributeManager = new AttributeManager();

		mJunkItemsListener = new JunkItemListener();
	}

	//  Logic that is performed upon enabling the plugin.
	@Override
	public void onEnable() {
		INSTANCE = this;
		PluginManager manager = getServer().getPluginManager();

		mHttpManager.start();

		mItemOverrides = new ItemOverrides();

		//  Initialize Variables.
		mTimers = new CooldownTimers(this);
		mCombatLoggingTimers = new CombatLoggingTimers();

		mProjectileEffectTimers = new ProjectileEffectTimers(this);

		mItemManager = new ItemManager();
		mIndexInventoryManager = new IndexInventoryManager();
		mPotionManager = new PotionManager();
		mTrackingManager = new TrackingManager(this);
		mZoneManager = new SpawnZoneManager(this);
		mAbilityManager = new AbilityManager(this);
		mShulkerInventoryManager = new ShulkerInventoryManager(this);
		mBossManager = new BossManager(this);
		mEffectManager = new EffectManager(this);
		mDeathItemListener = new DeathItemListener(this);
		mParrotManager = new ParrotManager(this);

		new ClientModHandler(this);

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
		manager.registerEvents(new ShatteredEquipmentListener(this), this);
		manager.registerEvents(new PotionConsumeListener(this), this);
		manager.registerEvents(mDeathItemListener, this);
		manager.registerEvents(new ZonePropertyListener(), this);
		manager.registerEvents(new TridentListener(), this);
		manager.registerEvents(new CrossbowListener(this), this);
		manager.registerEvents(mEnchantmentManager, this);
		manager.registerEvents(mJunkItemsListener, this);
		manager.registerEvents(mBossManager, this);
		manager.registerEvents(mEffectManager, this);
		manager.registerEvents(mParrotManager, this);
		manager.registerEvents(new IndexInventoryListeners(), this);
		manager.registerEvents(new DelvesListener(), this);
		manager.registerEvents(new SpawnerListener(this), this);
		manager.registerEvents(new PlayerInventoryView(), this);
		manager.registerEvents(new AnvilFixInInventory(this), this);
		manager.registerEvents(new ShatterCoinInInventory(this), this);
		manager.registerEvents(new LootChestsInInventory(), this);
		manager.registerEvents(new ArrowListener(this), this);
		manager.registerEvents(new SinglePageGUIManager(), this);
		manager.registerEvents(new GraveListener(this), this);
		manager.registerEvents(new BrewingListener(), this);
		manager.registerEvents(new ItemUpdateManager(this), this);

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

					try {
						mCombatLoggingTimers.update(Constants.QUARTER_TICKS_PER_SECOND);
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
			manager.registerEvents(new MonumentaNetworkRelayIntegration(this.getLogger()), this);
		}

		// Hook into Library of Souls for mob management if available
		if (Bukkit.getPluginManager().isPluginEnabled("LibraryOfSouls")) {
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
			new ProtocolLibIntegration(this);
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

		mTrackingManager.unloadTrackedEntities();
		mHttpManager.stop();
		mBossManager.unloadAll(true);
		MetadataUtils.removeAllMetadata(this);
	}

	public Player getPlayer(UUID playerID) {
		return getServer().getPlayer(playerID);
	}

	/* Sender will be sent debugging info if non-null */
	public void reloadMonumentaConfig(CommandSender sender) {
		ServerProperties.load(this, sender);
		mItemManager.load();
	}
}
