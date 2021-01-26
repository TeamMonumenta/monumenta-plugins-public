package com.playmonumenta.plugins;

import java.io.IOException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.spells.SpellDetectionCircle;
import com.playmonumenta.plugins.commands.BarkifyHeldItem;
import com.playmonumenta.plugins.commands.BossFight;
import com.playmonumenta.plugins.commands.CalculateReforge;
import com.playmonumenta.plugins.commands.ClaimRaffle;
import com.playmonumenta.plugins.commands.ClearPortals;
import com.playmonumenta.plugins.commands.ColossalifyHeldItem;
import com.playmonumenta.plugins.commands.DeBarkifyHeldItem;
import com.playmonumenta.plugins.commands.DeCluckifyHeldItem;
import com.playmonumenta.plugins.commands.DeathMsg;
import com.playmonumenta.plugins.commands.DebugInfo;
import com.playmonumenta.plugins.commands.Effect;
import com.playmonumenta.plugins.commands.FestiveHeldItem;
import com.playmonumenta.plugins.commands.GetDepthPoints;
import com.playmonumenta.plugins.commands.GildifyHeldItem;
import com.playmonumenta.plugins.commands.GiveSoulbound;
import com.playmonumenta.plugins.commands.HopeifyHeldItem;
import com.playmonumenta.plugins.commands.InfuseHeldItem;
import com.playmonumenta.plugins.commands.Launch;
import com.playmonumenta.plugins.commands.Magnetize;
import com.playmonumenta.plugins.commands.MonumentaDebug;
import com.playmonumenta.plugins.commands.MonumentaReload;
import com.playmonumenta.plugins.commands.OpenDelveModifierSelectionGUI;
import com.playmonumenta.plugins.commands.Portal1;
import com.playmonumenta.plugins.commands.Portal2;
import com.playmonumenta.plugins.commands.RedeemVoteRewards;
import com.playmonumenta.plugins.commands.ReforgeHeldItem;
import com.playmonumenta.plugins.commands.ReforgeInventory;
import com.playmonumenta.plugins.commands.RefreshClass;
import com.playmonumenta.plugins.commands.RemoveTags;
import com.playmonumenta.plugins.commands.RestartEmptyCommand;
import com.playmonumenta.plugins.commands.ShatterHeldItem;
import com.playmonumenta.plugins.commands.SkillDescription;
import com.playmonumenta.plugins.commands.SkillSummary;
import com.playmonumenta.plugins.commands.Spectate;
import com.playmonumenta.plugins.commands.SpectateBot;
import com.playmonumenta.plugins.commands.TeleportAsync;
import com.playmonumenta.plugins.commands.TeleportByScore;
import com.playmonumenta.plugins.cooking.CookingCommand;
import com.playmonumenta.plugins.cooking.CookingTableInventoryManager;
import com.playmonumenta.plugins.cooking.CookingTableListeners;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.enchantments.AttributeManager;
import com.playmonumenta.plugins.enchantments.EnchantmentManager;
import com.playmonumenta.plugins.guis.SinglePageGUIManager;
import com.playmonumenta.plugins.integrations.ChestSortIntegration;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.integrations.PlaceholderAPIIntegration;
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
import com.playmonumenta.plugins.listeners.ArrowListener;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.listeners.CrossbowListener;
import com.playmonumenta.plugins.listeners.DeathItemListener;
import com.playmonumenta.plugins.listeners.DelvesListener;
import com.playmonumenta.plugins.listeners.EntityListener;
import com.playmonumenta.plugins.listeners.ExceptionListener;
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
import com.playmonumenta.plugins.network.HttpManager;
import com.playmonumenta.plugins.overrides.ItemOverrides;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.server.reset.DailyReset;
import com.playmonumenta.plugins.spawnzone.SpawnZoneManager;
import com.playmonumenta.plugins.timers.CooldownTimers;
import com.playmonumenta.plugins.timers.ProjectileEffectTimers;
import com.playmonumenta.plugins.tracking.TrackingManager;
import com.playmonumenta.plugins.utils.MetadataUtils;

public class Plugin extends JavaPlugin {
	public CooldownTimers mTimers = null;
	public ProjectileEffectTimers mProjectileEffectTimers = null;
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
	public CookingTableInventoryManager mCookingTableInventoryManager;
	private BossManager mBossManager;
	public ItemManager mItemManager;
	public IndexInventoryManager mIndexInventoryManager;
	public EffectManager mEffectManager;

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
		RedeemVoteRewards.register(this.getLogger());
		BossFight.register();
		SpellDetectionCircle.registerCommand(this);
		SkillDescription.register(this);
		SkillSummary.register(this);
		CookingCommand.register(this);
		ItemIndexCommand.register();
		TeleportAsync.register();
		TeleportByScore.register();
		Portal1.register();
		Portal2.register();
		ClearPortals.register();
		Launch.register();
		OpenDelveModifierSelectionGUI.register();
		GetDepthPoints.register();
		Magnetize.register();

		try {
			mHttpManager = new HttpManager(this);
		} catch (IOException err) {
			getLogger().warning("HTTP manager failed to start");
			err.printStackTrace();
		}

		ServerProperties.load(this, null);

		mEnchantmentManager = new EnchantmentManager(this);
		mEnchantmentManager.load(ServerProperties.getForbiddenItemLore());

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

		mProjectileEffectTimers = new ProjectileEffectTimers(this);

		mItemManager = new ItemManager();
		mIndexInventoryManager = new IndexInventoryManager();
		mPotionManager = new PotionManager();
		mTrackingManager = new TrackingManager(this);
		mZoneManager = new SpawnZoneManager(this);
		mAbilityManager = new AbilityManager(this);
		mShulkerInventoryManager = new ShulkerInventoryManager(this);
		mCookingTableInventoryManager = new CookingTableInventoryManager(this);
		mBossManager = new BossManager(this);
		mEffectManager = new EffectManager(this);

		DailyReset.startTimer(this);

		//  Load info.
		reloadMonumentaConfig(null);

		// These are both a command and an event listener
		manager.registerEvents(new Spectate(this), this);
		manager.registerEvents(new SpectateBot(this), this);

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
		manager.registerEvents(new DeathItemListener(this), this);
		manager.registerEvents(new ZonePropertyListener(), this);
		manager.registerEvents(new TridentListener(), this);
		manager.registerEvents(new CrossbowListener(this), this);
		manager.registerEvents(mEnchantmentManager, this);
		manager.registerEvents(mJunkItemsListener, this);
		manager.registerEvents(mBossManager, this);
		manager.registerEvents(mEffectManager, this);
		manager.registerEvents(new CookingTableListeners(this), this);
		manager.registerEvents(new IndexInventoryListeners(), this);
		manager.registerEvents(new DelvesListener(), this);
		manager.registerEvents(new SpawnerListener(this), this);
		manager.registerEvents(new PlayerInventoryView(), this);
		manager.registerEvents(new AnvilFixInInventory(this), this);
		manager.registerEvents(new ShatterCoinInInventory(this), this);
		manager.registerEvents(new LootChestsInInventory(), this);
		manager.registerEvents(new ArrowListener(this), this);
		manager.registerEvents(new SinglePageGUIManager(), this);

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
						mTrackingManager.update(Constants.QUARTER_TICKS_PER_SECOND);
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

		// Hook into JeffChestSort for custom chest sorting if present
		if (Bukkit.getPluginManager().isPluginEnabled("ChestSort")) {
			manager.registerEvents(new ChestSortIntegration(this.getLogger()), this);
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

		// Register the explosion repair mechanism if BKCommonLib is present
		if (Bukkit.getPluginManager().isPluginEnabled("BKCommonLib")) {
			manager.registerEvents(new RepairExplosionsListener(this), this);
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
		mEnchantmentManager.load(ServerProperties.getForbiddenItemLore());
		mItemManager.load();
	}
}
