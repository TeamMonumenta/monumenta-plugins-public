package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Constants.Colors;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.ToggleSwap;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.itemstats.enchantments.CurseOfEphemerality;
import com.playmonumenta.plugins.itemstats.infusions.Phylactery;
import com.playmonumenta.plugins.itemstats.infusions.StatTrackManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.point.Point;
import com.playmonumenta.plugins.portals.PortalManager;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.protocollib.VirtualFirmamentReplacer;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.server.reset.DailyReset;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import com.playmonumenta.scriptedquests.managers.TranslationsManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedMainHandEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class PlayerListener implements Listener {

	private static final String PLAYERS_TEAM_NAME = "players";

	private final Plugin mPlugin;

	public PlayerListener(Plugin plugin) {
		mPlugin = plugin;

		// Delete the players team on startup to clear any lingering entries (and recreate it later with proper settings)
		Team playersTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(PLAYERS_TEAM_NAME);
		if (playersTeam != null) {
			playersTeam.unregister();
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		if (ServerProperties.getJoinMessagesEnabled() == false) {
			event.setJoinMessage("");
		}

		/* This needs to stick around basically forever to remove this no-longer-needed tag */
		player.removeScoreboardTag("MidTransfer");

		mPlugin.mTrackingManager.addEntity(player);
		mPlugin.mAbilityManager.playerJoinEvent(player, event);

		DailyReset.handle(mPlugin, player);
		//This checks to make sure that when you login you aren't stuck in blocks, just in case the lag that causes you to fall also kicks you. You don't want to be stuck in dirt forever, right?
		Location loc = player.getLocation();
		runTeleportRunnable(player, loc);

		// add player to the players team (and create the team if it doesn't exist already)
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
			Team playersTeam = scoreboard.getTeam(PLAYERS_TEAM_NAME);
			if (playersTeam == null) {
				playersTeam = scoreboard.registerNewTeam(PLAYERS_TEAM_NAME);
				playersTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
				playersTeam.setCanSeeFriendlyInvisibles(false);
			}
			if (!playersTeam.hasEntry(player.getName())) {
				playersTeam.addEntry(player.getName());
			}
		}, 1);

		// transform old NO_SELF_PARTICLES tag into new partial particle option
		// can be removed after a while
		if (ScoreboardUtils.checkTag(player, Constants.Tags.NO_SELF_PARTICLES)) {
			player.removeScoreboardTag(Constants.Tags.NO_SELF_PARTICLES);
			ScoreboardUtils.setScoreboardValue(player, ParticleCategory.OWN_PASSIVE.mObjectiveName, 0);
		}

	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerChannelEvent(PlayerRegisterChannelEvent event) {
		if (ClientModHandler.CHANNEL_ID.equals(event.getChannel())) {
			ClientModHandler.updateAbilities(event.getPlayer());
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		if (!ServerProperties.getJoinMessagesEnabled()) {
			event.setQuitMessage("");
		}

		Player player = event.getPlayer();

		/* Remove portals from player */
		PortalManager.clearPortal(player, 1);
		PortalManager.clearPortal(player, 2);

		/* Remove ephemeral items on logout */
		InventoryUtils.removeSpecialItems(player, true);

		mPlugin.mAbilityManager.playerQuitEvent(player, event);

		mPlugin.mTrackingManager.removeEntity(player);

		for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), 20)) {
			if (!mob.isPersistent()) {
				mPlugin.mCombatLoggingTimers.addTimer(mob.getUniqueId(), Constants.TEN_MINUTES);
				mob.setRemoveWhenFarAway(false);
			}
		}

		Team playersTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(PLAYERS_TEAM_NAME);
		if (playersTeam != null) {
			playersTeam.removeEntry(player.getName());
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerSaveEvent(PlayerSaveEvent event) {
		Player player = event.getPlayer();
		mPlugin.mAbilityManager.playerSaveEvent(player, event);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void playerInteractEvent(PlayerInteractEvent event) {
		if (event.useInteractedBlock() == Event.Result.DENY && event.useItemInHand() == Event.Result.DENY) {
			return;
		}
		Action action = event.getAction();
		Player player = event.getPlayer();
		ItemStack item = event.getItem();
		Block block = event.getClickedBlock();
		Material mat = (block != null) ? block.getType() : Material.AIR;

		// Plot Security: If block is in a plot but the player is in adventure, cancel.
		if (block != null && player != null && player.getGameMode() == GameMode.ADVENTURE && ZoneUtils.isInPlot(block.getLocation())) {
			event.setCancelled(true);
			return;
		}

		mPlugin.mAbilityManager.playerInteractEvent(player, action, mat);
		mPlugin.mItemStatManager.onPlayerInteract(mPlugin, player, event);

		// Overrides
		// TODO: Rewrite overrides system to handle item/block interactions separately
		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			if (item != null && !mPlugin.mItemOverrides.leftClickInteraction(mPlugin, player, action, item, block)) {
				event.setCancelled(true);
				return;
			}
		} else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			if (!mPlugin.mItemOverrides.rightClickInteraction(mPlugin, player, action, item, block, event)) {
				event.setCancelled(true);
				return;
			}
		}

		// Item Interactions
		if (event.useItemInHand() != Event.Result.DENY) {
			if (item != null && ItemUtils.isArmor(item)) {
				InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);
			}
		}

		// Block Interactions
		if (event.useInteractedBlock() != Event.Result.DENY) {
			if (block != null) {
				Location location = block.getLocation();
				if (player.getGameMode() == GameMode.ADVENTURE
					    && ZoneUtils.isInPlot(location)) {
					event.setUseInteractedBlock(Event.Result.DENY);
					return;
				}
			}
		}

		// Immediately load crossbows when the right mouse button is held down for long enough instead of only when released.
		// This prevents "ghost" loading crossbows, where the client thinks the button was held for long enough, but the server disagrees.
		if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
			    && event.useItemInHand() != Event.Result.DENY
			    && item != null
			    && item.getType() == Material.CROSSBOW
			    && item.getItemMeta() instanceof CrossbowMeta meta
			    && meta.getChargedProjectiles().isEmpty()) {
			int slot = player.getInventory().getHeldItemSlot();
			new BukkitRunnable() {
				@Override
				public void run() {
					if (slot != player.getInventory().getHeldItemSlot()
						    || !item.equals(player.getActiveItem())
						    || !player.isHandRaised()) {
						this.cancel();
						return;
					}
					if (player.getItemUseRemainingTime() <= 0) {
						this.cancel();
						NmsUtils.getVersionAdapter().releaseActiveItem(player, false);
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		ItemStack item = event.getItemInHand();
		Player player = event.getPlayer();
		Block block = event.getBlock();

		if (!mPlugin.mItemOverrides.blockPlaceInteraction(mPlugin, player, item, event)) {
			event.setCancelled(true);
			return;
		}

		//Prevent players in survival mode breaking blocks in adventure mode zones not including plots
		if (!ZoneUtils.playerCanInteractWithBlock(player, block)) {
			event.setCancelled(true);
			return;
		}
	}

	// Player interacts with an entity (not triggered on armor stands for some reason)
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
		Player player = event.getPlayer();

		/* Don't let the player do this when in a restricted zone */
		if (ZoneUtils.hasZoneProperty(player, ZoneProperty.RESTRICTED) && player.getGameMode() != GameMode.CREATIVE) {
			event.setCancelled(true);
			return;
		}

		Entity clickedEntity = event.getRightClicked();
		ItemStack itemInHand = player.getEquipment().getItemInMainHand();
		if (event.getHand().equals(EquipmentSlot.OFF_HAND)) {
			itemInHand = player.getEquipment().getItemInOffHand();
		}

		//Prevent feeding special item lore items to animals (specifically horses)
		if (clickedEntity instanceof Animals && itemInHand != null && itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasLore()) {
			event.setCancelled(true);
			return;
		} else if (clickedEntity instanceof ItemFrame) {
			ItemFrame frame = (ItemFrame) clickedEntity;

			// Plot Security: If item frame is in a plot but the player is in adventure, cancel.
			if (player.getGameMode() == GameMode.ADVENTURE && ZoneUtils.isInPlot(frame)) {
				event.setCancelled(true);
				return;
			}

			if (frame.isInvulnerable()) {
				if (player.getGameMode().equals(GameMode.CREATIVE)) {
					player.sendMessage(Component.text("This item frame is invulnerable / creative only", NamedTextColor.GOLD));
				} else {
					event.setCancelled(true);
				}

				ItemStack frameItem = frame.getItem();
				if (frameItem != null && frameItem.getType().equals(Material.FILLED_MAP)) {
					if (player.getGameMode().equals(GameMode.ADVENTURE)) {
						ItemStack giveMap = frameItem.clone();
						ItemMeta mapMeta;

						if (giveMap.hasItemMeta()) {
							mapMeta = giveMap.getItemMeta();
						} else {
							mapMeta = Bukkit.getServer().getItemFactory().getItemMeta(Material.FILLED_MAP);
						}

						List<Component> mapLore = mapMeta.lore();
						if (mapLore == null) {
							mapLore = new ArrayList<>(1);
						}
						mapLore.add(Component.text("* Official Map *", NamedTextColor.GOLD));
						mapMeta.lore(mapLore);
						giveMap.setItemMeta(mapMeta);
						ItemUtils.setPlainLore(giveMap);
						InventoryUtils.giveItem(player, giveMap);
					}
				}
			}
		}

		if (!mPlugin.mItemOverrides.rightClickEntityInteraction(mPlugin, player, clickedEntity, itemInHand)) {
			event.setCancelled(true);
		}
	}

	// Player interacts with an armor stand
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerArmorStandManipulateEvent(PlayerArmorStandManipulateEvent event) {
		Player player = event.getPlayer();
		ArmorStand armorStand = event.getRightClicked();

		/* Don't let the player do this in a restricted zone */
		if (ZoneUtils.hasZoneProperty(player, ZoneProperty.RESTRICTED) && player.getGameMode() != GameMode.CREATIVE) {
			event.setCancelled(true);
			return;
		}

		// Plot Security: If armor stand is in a plot but the player is in adventure, cancel.
		if (player.getGameMode() == GameMode.ADVENTURE && ZoneUtils.isInPlot(armorStand)) {
			event.setCancelled(true);
			return;
		}
	}

	// The Player swapped their current selected item.
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerItemHeldEvent(PlayerItemHeldEvent event) {
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer(), event);
	}

	// The player dropped an item.
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerDropItemEvent(PlayerDropItemEvent event) {
		Player player = event.getPlayer();

		/* Don't let the player do this when in a restricted zone */
		if (ZoneUtils.hasZoneProperty(player, ZoneProperty.RESTRICTED) && player.getGameMode() != GameMode.CREATIVE) {
			event.setCancelled(true);
			return;
		}
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer(), event);
	}

	// An entity picked up an item
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void entityPickupItemEvent(EntityPickupItemEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();

			/* Don't let the player do this when in a restricted zone */
			if (ZoneUtils.hasZoneProperty(player, ZoneProperty.RESTRICTED) && player.getGameMode() != GameMode.CREATIVE) {
				event.setCancelled(true);
				return;
			}

			/* Don't let players pick up items that have already been processed for shattering */
			if (event.getItem().getScoreboardTags().contains("ShatteringNoPickup")) {
				event.setCancelled(true);
				return;
			}

			// If the item contains lore, schedule an equipment update
			if (event.getItem().getItemStack().getItemMeta().hasLore()) {
				InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);
			}

			/* Mark the item so it won't get shattered later */
			event.getItem().addScoreboardTag("ShatterProcessed");
		} else {
			/* Non-player entities don't get to pick up items */
			event.setCancelled(true);
		}
	}

	// An item on the player breaks.
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerItemBreakEvent(PlayerItemBreakEvent event) {
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer(), event);
	}

	// If an inventory interaction happened.
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}

		// If item contains curse of ephemerality, prevent from putting in other inventories
		// Checks for player inventory unless it's a shift click
		if (
			// Prevent shift-clicking an ephemeral item from your inventory to something else
			(
				(event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT)
					&& event.getCurrentItem() != null
					&& CurseOfEphemerality.isEphemeral(event.getCurrentItem())
					&& event.getClickedInventory() instanceof PlayerInventory
			)
				// Prevent clicking an ephemeral item from your cursor down into something else
				|| (
				event.getClick() != ClickType.SHIFT_LEFT
					&& event.getClick() != ClickType.SHIFT_RIGHT
					&& event.getCursor() != null
					&& CurseOfEphemerality.isEphemeral(event.getCursor())
			)) {
			event.setCancelled(true);
			return;
		}

		if (!mPlugin.mItemOverrides.inventoryClickInteraction(mPlugin, player, event)) {
			event.setCancelled(true);
		}
	}

	// If an item is being dragged in an inventory
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void inventoryDragEvent(InventoryDragEvent event) {
		if (event.getWhoClicked() instanceof Player) {
			Player player = (Player) event.getWhoClicked();
			InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);
		}
		//If item contains curse of ephemerality, prevent from putting in other inventories
		if (event.getWhoClicked() instanceof Player && event.getCursor() != null && CurseOfEphemerality.isEphemeral(event.getCursor())) {
			event.setCancelled(true);
		} else if (event.getNewItems() != null) {
			for (Map.Entry<Integer, ItemStack> iter : event.getNewItems().entrySet()) {
				if (CurseOfEphemerality.isEphemeral(iter.getValue())) {
					event.setCancelled(true);
					return;
				}
			}
		}
	}

	// The player opened an inventory
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void inventoryOpenEvent(InventoryOpenEvent event) {
		if (event.getPlayer() instanceof Player) {
			Player player = (Player) event.getPlayer();

			/* Don't let the player do this when in a restricted zone */
			if (ZoneUtils.hasZoneProperty(player, ZoneProperty.RESTRICTED)
			    && player.getGameMode() != GameMode.CREATIVE
			    && player.getGameMode() != GameMode.SPECTATOR) {
				event.setCancelled(true);
				return;
			}
		}
	}

	// ...Because there's a known bug with the stupid Item Property stuff and the InventoryClickEvent stuff...
	// The player inventory is closed
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		Inventory inventory = event.getInventory();
		InventoryHolder holder = inventory.getHolder();
		if (holder != null && holder instanceof Chest chest) {
			// Break empty graves or halloween creeper chests in safe zones automatically when closed
			if (ChestUtils.isEmpty(chest) && (chest.getCustomName() != null && chest.getCustomName().contains("Creeperween Chest"))) {
				chest.getBlock().breakNaturally();
			}
		}
		if (event.getPlayer() instanceof Player player) {
			InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);

			// Workaround for 1.17 clients not having proper inventory state after cancelling click events
			// (when opening a new inventory after a click event is canceled, a "ghost" item is on the cursor)
			Bukkit.getScheduler().runTaskLater(mPlugin, player::updateInventory, 1);
		}
	}

	// Something interacts with an inventory
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void inventoryInteractEvent(InventoryInteractEvent event) {
		if (event.getWhoClicked() instanceof Player) {
			Player player = (Player) event.getWhoClicked();

			/* Don't let the player do this when in a restricted zone */
			if (ZoneUtils.hasZoneProperty(player, ZoneProperty.RESTRICTED)
			    && player.getGameMode() != GameMode.CREATIVE
			    && player.getGameMode() != GameMode.SPECTATOR) {
				event.setCancelled(true);
				return;
			}
			InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);
		}
	}

	// Player ran a command
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerCommandPreprocessEvent(PlayerCommandPreprocessEvent event) {
		final String NBT_EDITOR_OPTIONAL_PREFIX = "nbteditor:";

		Player player = event.getPlayer();
		String command = event.getMessage().substring(1);

		if (command.startsWith(NBT_EDITOR_OPTIONAL_PREFIX)) {
			command = command.substring(NBT_EDITOR_OPTIONAL_PREFIX.length());
		}

		if (command.startsWith("nbte")
		    || command.startsWith("nbti")
		    || command.startsWith("nbtp")) {
			new BukkitRunnable() {
				@Override
				public void run() {
					ItemStack item = player.getEquipment().getItemInMainHand();
					if (item != null && item.getAmount() > 0) {
						ItemUtils.setPlainTag(item);
						player.getEquipment().setItemInMainHand(item, true);
					}
				}
			}.runTaskLater(mPlugin, 0);
		}
	}

	// Player changed hand items
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerChangedMainHandEvent(PlayerChangedMainHandEvent event) {
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer(), event);
	}

	// Player swapped hand items
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		mPlugin.mAbilityManager.playerSwapHandItemsEvent(event.getPlayer(), event);
		if (event.getPlayer().getScoreboardTags().contains(ToggleSwap.SWAP_TAG)) {
			event.setCancelled(true);
		}
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer(), event);
	}

	// The player has died
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();

		if (player.getHealth() > 0) {
			return;
		}

		// Prevent an inescapable death loop by overriding KeepInventory if your Max Health is 0
		if (event.getKeepInventory()) {
			AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
			if (maxHealth != null && maxHealth.getValue() <= 0) {
				event.setKeepInventory(false);
			}
		} else {
			//Only activate on death effects for non-safe deaths
			mPlugin.mItemStatManager.onDeath(mPlugin, player, event);
		}

		// Give the player a NewDeath score of 1 so the city guides will give items again
		ScoreboardUtils.setScoreboardValue(player, "NewDeath", 1);

		if (event.deathMessage() != null && ScoreboardUtils.getScoreboardValue(player, Constants.SCOREBOARD_DEATH_MESSAGE).orElse(0) != 0) {
			player.sendMessage(event.deathMessage());
			player.sendMessage(Component.text("Only you saw this message. Change this with /deathmsg", NamedTextColor.AQUA));
			event.deathMessage(null);
		}

		// Clear effects
		mPlugin.mPotionManager.clearAllPotions(player);
		mPlugin.mAbilityManager.updatePlayerAbilities(player, true);
	}

	// The player has respawned.
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerRespawnEvent(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		final String name = player.getName();

		player.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, new Runnable() {
			@Override
			public void run() {
				Player player = Bukkit.getPlayer(name);
				if (player != null) {
					mPlugin.mPotionManager.clearAllPotions(player);
					mPlugin.mAbilityManager.updatePlayerAbilities(player, true);

					InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);
				}
			}
		}, 0);

		Phylactery.applyStoredEffects(mPlugin, player);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerFishEvent(PlayerFishEvent event) {
		Player player = event.getPlayer();
		if (event.getState() == State.FISHING) {
			mPlugin.mTrackingManager.mFishingHook.addEntity(player, event.getHook());
		} else if (event.getState() == State.CAUGHT_ENTITY || event.getState() == State.CAUGHT_FISH) {
			mPlugin.mTrackingManager.mFishingHook.removeEntity(player);

			if (event.getState() == State.CAUGHT_ENTITY && !EntityUtils.isHostileMob(event.getCaught())) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerItemConsumeEvent(PlayerItemConsumeEvent event) {
		Player player = event.getPlayer();

		/* Don't let players consume shattered items */
		if (ItemStatUtils.isShattered(event.getItem())) {
			event.setCancelled(true);
			return;
		}

		if (event.getItem().getItemMeta() instanceof PotionMeta) {
			if (PotionUtils.isLuckPotion((PotionMeta) event.getItem().getItemMeta())) {
				Location loc = player.getLocation();
				loc.getWorld().playSound(loc, Sound.ENTITY_HORSE_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
				player.sendMessage(Component.text("Luck potions can no longer be consumed", NamedTextColor.RED, TextDecoration.BOLD));
				event.setCancelled(true);
				return;
			}
		}

		if (!mPlugin.mItemOverrides.playerItemConsume(mPlugin, player, event)) {
			event.setCancelled(true);
			return;
		}

		mPlugin.mAbilityManager.playerItemConsumeEvent(player, event);

		if (event.getItem().containsEnchantment(Enchantment.ARROW_INFINITE)) {
			event.setReplacement(event.getItem());
			//Stat tracker for consuming infinity items
			StatTrackManager.incrementStat(event.getItem(), player, InfusionType.STAT_TRACK_CONSUMED, 1);
		}

		mPlugin.mItemStatManager.onConsume(mPlugin, player, event);

		for (PotionEffect effect : PotionUtils.getEffects(event.getItem())) {
			// Kill the player if they drink a potion with instant damage 10+
			if (effect.getType() != null
			    && effect.getType().equals(PotionEffectType.HARM)
				&& effect.getAmplifier() >= 9) {

				player.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, new Runnable() {
					@Override
					public void run() {
						player.setHealth(0);
					}
				}, 0);
			} else if (effect.getType() != null &&
					effect.getType().equals(PotionEffectType.SLOW_FALLING)) {
				//Remove Slow Falling effects
				player.sendMessage(Component.text("You cannot apply slow falling potion effects, other effects were still applied.", NamedTextColor.RED));
				player.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, new Runnable() {
					@Override
					public void run() {
						player.removePotionEffect(PotionEffectType.SLOW_FALLING);
					}
				}, 1);
			}

			mPlugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, effect);
		}
	}

	// An item has taken damage.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerItemDamageEvent(PlayerItemDamageEvent event) {
		Player player = event.getPlayer();
		ItemStack item = event.getItem();

		mPlugin.mAbilityManager.playerItemDamageEvent(player, event);

		int oldDamage = event.getDamage();
		int newDamage = oldDamage;

		if (ItemUtils.isArmor(item)) {
			// Players that get resistance from safezones don't take armor damage
			if (oldDamage < 0 || ZoneUtils.hasZoneProperty(player, ZoneProperty.NO_EQUIPMENT_DAMAGE)) {
				newDamage = 0;
			}
		}

		//Tridents do not take damage in safezones
		if (item.getType() == Material.TRIDENT && ZoneUtils.hasZoneProperty(player, ZoneProperty.NO_EQUIPMENT_DAMAGE)) {
			newDamage = 0;
		}

		// Vanilla hoes and axes take 2 durability, we only want them to take 1
		if ((ItemUtils.isHoe(item) || ItemUtils.isAxe(item)) && oldDamage > 1) {
			newDamage = oldDamage / 2;
		}

		event.setDamage(newDamage);
		mPlugin.mItemStatManager.onItemDamage(mPlugin, player, event);

		// Low durability notification code
		Material mat = item.getType();
		if (item.getItemMeta() instanceof Damageable dMeta) {
			int itemDamage = dMeta.getDamage();
			if (itemDamage < mat.getMaxDurability() * 0.9 && itemDamage + newDamage >= mat.getMaxDurability() * 0.9) {
				World world = player.getWorld();
				Location loc = player.getLocation();
				world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.45f);
				world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.25f);
				BlockData fallingDustData = Material.ANVIL.createBlockData();
				world.spawnParticle(Particle.FALLING_DUST, loc.add(0, 1, 0), 20,
					1.1, 0.6, 1.1, fallingDustData);
				Component itemName = item.hasItemMeta() ? item.getItemMeta().displayName() : null;
				if (itemName == null) {
					itemName = Component.translatable(mat.getTranslationKey());
				} else {
					itemName = itemName.decoration(TextDecoration.UNDERLINED, false);
				}
				String translatedMessage = TranslationsManager.translate(player, "Your %s is about to break!");
				Component message = Component.text(translatedMessage).color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
					.replaceText(TextReplacementConfig.builder().matchLiteral("%s").replacement(itemName).build());
				player.sendMessage(message);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerRiptideEvent(PlayerRiptideEvent event) {
		Player player = event.getPlayer();
		Location loc = player.getLocation();

		//Manually forces the player in place during the riptide if they use it out of water (in rain)
		if (StasisListener.isInStasis(player) || !mPlugin.mItemOverrides.playerRiptide(mPlugin, player, event)) {
			player.teleport(loc);
			player.setCooldown(Material.TRIDENT, 15*20);
			new BukkitRunnable() {
				@Override
				public void run() {
					if (!player.isRiptiding()) {
						this.cancel();
						return;
					}
					player.setVelocity(player.getVelocity().multiply(0));
				}
			}.runTaskTimer(mPlugin, 0, 2);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerExpChangeEvent(PlayerExpChangeEvent event) {
		Player player = event.getPlayer();

		mPlugin.mItemStatManager.onExpChange(mPlugin, player, event);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerTeleportEvent(PlayerTeleportEvent event) {
		// Cancel teleports caused by forbidden sources
		TeleportCause cause = event.getCause();
		if (cause.equals(TeleportCause.CHORUS_FRUIT)
			|| cause.equals(TeleportCause.END_GATEWAY)
			|| cause.equals(TeleportCause.END_PORTAL)
			|| (cause.equals(TeleportCause.ENDER_PEARL)
			&& (ZoneUtils.hasZoneProperty(event.getFrom(), ZoneProperty.ADVENTURE_MODE)
			|| ZoneUtils.hasZoneProperty(event.getTo(), ZoneProperty.ADVENTURE_MODE)))
			|| cause.equals(TeleportCause.NETHER_PORTAL)) {
			event.setCancelled(true);
			return;
		}

		Player player = event.getPlayer();
		Location loc = event.getTo();

		if (!cause.equals(TeleportCause.UNKNOWN) && !cause.equals(TeleportCause.ENDER_PEARL) && !player.getGameMode().equals(GameMode.SPECTATOR) && !cause.equals(TeleportCause.SPECTATE)) {
			runTeleportRunnable(player, loc);
		}
	}

	public void runTeleportRunnable(Player player, Location loc) {
		// Runnable to make sure that players don't get stuck in the floor too often. That if statement is a testament to never trying to edit how teleporting works, its so easy to break it
		// Made it only work if you are stuck exactly 2 blocks into the ground or less to prevent exploits, if it ends up being a problem, adding one for 3 blocks shouldn't be too hard
		new BukkitRunnable() {
			@Override
			public void run() {
				Block block = player.getLocation().getBlock();
				Block blockEye = player.getEyeLocation().getBlock();
				if ((!block.getType().equals(Material.AIR) || !blockEye.getType().equals(Material.AIR))) {
					if (player.getLocation().add(0, 1, 0).getBlock().getType().equals(Material.AIR) && player.getEyeLocation().add(0, 1, 0).getBlock().getType().equals(Material.AIR)) {
						player.teleport(loc.add(0, 1, 0));
					} else if (player.getLocation().add(0, 2, 0).getBlock().getType().equals(Material.AIR) && player.getEyeLocation().add(0, 2, 0).getBlock().getType().equals(Material.AIR)) {
						player.teleport(loc.add(0, 2, 0));
					}
					this.cancel();
				} else if ((block.getType().equals(Material.AIR) || blockEye.getType().equals(Material.AIR)) || player.isOnline() || player.isDead() || !player.isValid()) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerBedEnterEvent(PlayerBedEnterEvent event) {
		Player player = event.getPlayer();
		Block bed = event.getBed();
		Location loc = bed.getLocation();
		World world = loc.getWorld();

		/* Prevent entering beds designed to glitch through blocks */
		Material aboveMat = loc.add(0, 1, 0).getBlock().getType();
		if (aboveMat.equals(Material.BEDROCK) || aboveMat.equals(Material.BARRIER) || aboveMat.equals(Material.OBSIDIAN)) {
			new BukkitRunnable() {
				float mFreq = 0;

				@Override
				public void run() {
					player.playSound(player.getLocation(), Sound.ENTITY_HORSE_DEATH, SoundCategory.MASTER, 1, mFreq);
					mFreq += 0.05f;
					if (mFreq > 1.5) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 10);

			event.setCancelled(true);
			return;
		}

		/* Implements bed teleporters.
		 *
		 * When a player sleeps in a bed, the 10 blocks under that bed are checked for
		 * a regular command block.
		 *
		 * If a command block is found, its command string is used for coordinates to teleport
		 * the player to after a short delay
		 */
		for (double y = 10; y > 0; y--) {
			loc = loc.subtract(0, 1, 0);
			Block testblock = loc.getBlock();
			BlockState state = testblock.getState();

			if (testblock.getType().equals(Material.COMMAND_BLOCK)
			    && state instanceof CommandBlock) {

				String str = ((CommandBlock) state).getCommand();

				String[] split = str.split(" ");
				if (split.length != 5) {
					player.sendMessage(Component.text("Command block should be of the format 'x y z pitch yaw'", NamedTextColor.RED));
					player.sendMessage(Component.text("Relative and absolute coordinates accepted", NamedTextColor.RED));
					continue;
				}

				try {
					// Coordinates are relative to the head of the bed
					Point pt = Point.fromString(player, split[0], split[1], split[2], false, bed.getLocation());

					float yaw = (float) CommandUtils.parseDoubleFromString(player, split[3]);
					float pitch = (float) CommandUtils.parseDoubleFromString(player, split[4]);

					// Adjust for player teleports being off-center
					Location teleLoc = new Location(world, pt.mX - 0.5, pt.mY, pt.mZ - 0.5, yaw, pitch);

					//Records player's previous spawnpoint - either their bed spawn point if it exists (and is not this bed), or the world spawn otherwise
					Location tempSpawnLoc = player.getBedSpawnLocation();
					if (tempSpawnLoc == null || tempSpawnLoc.equals(loc)) {
						tempSpawnLoc = world.getSpawnLocation();
					}
					final Location playerSpawn = tempSpawnLoc;

					// Create a deferred task to eject player and teleport them after a short sleep
					new BukkitRunnable() {
						int mTicks = 0;

						@Override
						public void run() {
							GameMode mode;
							final int BED_TELE_TIME = 20 * 3;

							if (mTicks == 0) {
								//Set player's spawnpoint back to whatever it was
								String cmd = String.format("spawnpoint %s %d %d %d", player.getName(), playerSpawn.getBlockX(), playerSpawn.getBlockY(), playerSpawn.getBlockZ());
								Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd);
							}


							if (++mTicks == BED_TELE_TIME) {
								// Get player's current gamemode
								mode = player.getGameMode();

								// Set player's gamemode to survival so they can be damaged
								player.setGameMode(GameMode.SURVIVAL);

								// Poke the player to eject them from the bed
								player.damage(0.001);

								// Set player's gamemode back to whatever it was
								player.setGameMode(mode);

							} else if (mTicks >= BED_TELE_TIME + 1) {
								player.teleport(teleLoc);

								world.playSound(teleLoc, Sound.ENTITY_ELDER_GUARDIAN_DEATH, 1.0f, 1.3f);

								this.cancel();
							} else if (!player.isSleeping()) {
								// Abort, player got out of bed early
								this.cancel();
								return;
							}
						}
					}.runTaskTimer(mPlugin, 0, 1);

					return;
				} catch (Exception e) {
					player.sendMessage("Failed to parse teleport coordinates");
				}
			}
		}
	}

	private static final String PLAYER_LEFT_BED_TICK_METAKEY = "PlayerLeftBedTick";

	/*
	 * Prevent players from passing through 1-thick barriers using beds
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerBedLeaveEvent(PlayerBedLeaveEvent event) {
		Player player = event.getPlayer();
		if (MetadataUtils.checkOnceThisTick(mPlugin, player, PLAYER_LEFT_BED_TICK_METAKEY)) {
			Block bed = event.getBed();
			Location loc = bed.getLocation();
			player.teleport(loc.add(0.5, 1, 0.5));
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerToggleSneakEvent(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();

		if (event.isSneaking()) {
			/*
			 * Player started sneaking
			 *
			 * Create a task that will fire in 2s and attach that taskId to the player via metadata
			 *
			 * If that task fires, trigger the extended sneak event and remove the metadata
			 */
			int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(mPlugin,
					() -> {
						mPlugin.mAbilityManager.playerExtendedSneakEvent(player);
						player.removeMetadata(Constants.PLAYER_SNEAKING_TASK_METAKEY, mPlugin);
					}, 20);

			player.setMetadata(Constants.PLAYER_SNEAKING_TASK_METAKEY, new FixedMetadataValue(mPlugin, taskId));
		} else {
			/*
			 * Player stopped sneaking
			 *
			 * Check if they still have the metadata (they haven't been sneaking for 2s)
			 * If so, remove it and cancel the task
			 */
			if (player.hasMetadata(Constants.PLAYER_SNEAKING_TASK_METAKEY)) {
				int taskId = player.getMetadata(Constants.PLAYER_SNEAKING_TASK_METAKEY).get(0).asInt();
				Bukkit.getScheduler().cancelTask(taskId);
				player.removeMetadata(Constants.PLAYER_SNEAKING_TASK_METAKEY, mPlugin);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void abilityCastEvent(AbilityCastEvent event) {
		Player player = event.getCaster();
		mPlugin.mAbilityManager.abilityCastEvent(player, event);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlock();

		if (!mPlugin.mItemOverrides.blockBreakInteraction(mPlugin, event.getPlayer(), block, event)) {
			event.setCancelled(true);
			return;
		}

		//Prevent players in survival mode breaking blocks in adventure mode zones not including plots
		if (!ZoneUtils.playerCanInteractWithBlock(player, block)) {
			event.setCancelled(true);
			return;
		}

		mPlugin.mItemStatManager.onBlockBreak(mPlugin, player, event);
	}

	@EventHandler(ignoreCancelled = true)
	public void playerAnimationEvent(PlayerAnimationEvent event) {
		Player player = event.getPlayer();
		if (event.getAnimationType().equals(PlayerAnimationType.ARM_SWING)) {
			mPlugin.mAbilityManager.playerAnimationEvent(player, event);
		}
	}

	//Player is healed.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onRegain(EntityRegainHealthEvent event) {
		if (event.getEntity() instanceof Player player) {
			mPlugin.mItemStatManager.playerRegainHealthEvent(mPlugin, player, event);
			mPlugin.mAbilityManager.playerRegainHealthEvent(player, event);
		}
	}

	/*
	 * Pick survival or adventure as appropriate for zone properties when
	 * switching into either gamemode.
	 */
	@EventHandler(ignoreCancelled = true)
	public void gamemodeCorrection(PlayerGameModeChangeEvent event) {
		GameMode newGameMode = event.getNewGameMode();
		Player player = event.getPlayer();
		boolean shouldBeAdventure = (
			ZoneUtils.hasZoneProperty(player, ZoneProperty.ADVENTURE_MODE)
				&& !ZoneUtils.isInPlot(player)
		);

		//NOTE Once we update Paper version more,
		// replace the event's generic failure message via
		// event.cancelMessage(),
		// instead of doing player.sendMessage() separately
		if (GameMode.SURVIVAL.equals(newGameMode) && shouldBeAdventure) {
			event.setCancelled(true);
			player.setGameMode(GameMode.ADVENTURE);
			// Op check since listener also catches mechanisms putting players
			// into the wrong game mode for their zone, such as Frost Giant,
			// and in that case we don't want to notify normal players who
			// didn't choose to enter that game mode
			// /who won't see vanilla "Set own game mode" messages.
			// This check also won't be needed in the future once we're just
			// replacing event.cancelMessage().
			if (player.isOp()) {
				player.sendMessage(
					Component
						.text("Set own game mode to ", Colors.GREENISH_BLUE)
						.append(
							Component.text("Adventure Mode ", Colors.GREENISH_BLUE_DARK)
						)
						.append(
							Component.text(
								"instead of Survival due to zone.",
								Colors.GREENISH_BLUE
							)
						)
				);
			}
		} else if (GameMode.ADVENTURE.equals(newGameMode) && !shouldBeAdventure) {
			event.setCancelled(true);
			player.setGameMode(GameMode.SURVIVAL);
			if (player.isOp()) {
				player.sendMessage(
					Component
						.text("Set own game mode to ", Colors.GREENISH_BLUE)
						.append(
							Component.text("Survival Mode ", Colors.GREENISH_BLUE_DARK)
						)
						.append(
							Component.text(
								"instead of Adventure due to zone.",
								Colors.GREENISH_BLUE
							)
						)
				);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void gamemodeChangeEvent(PlayerGameModeChangeEvent event) {
		// When switching to creative, update the inventory to update any virtual Firmaments back into normal Firmaments to prevent breaking them
		if (event.getNewGameMode() == GameMode.CREATIVE) {
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> event.getPlayer().updateInventory(), 1);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void inventoryCreativeEvent(InventoryCreativeEvent event) {
		// The inventory update initiated above takes a while, during which the Firmament can be broken anyway, so need to watch for these events as well
		if ((event.getCurrentItem() != null && VirtualFirmamentReplacer.isVirtualFirmament(event.getCurrentItem()))
			    || VirtualFirmamentReplacer.isVirtualFirmament(event.getCursor())) {
			event.setCancelled(true);
		}
	}

	/*
	 * Refresh class abilities after event (end of tick)
	 * when switching to/from spectator.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void spectatorAbilityRefresh(PlayerGameModeChangeEvent event) {
		Player player = event.getPlayer();
		GameMode oldGameMode = player.getGameMode();
		GameMode newGameMode = event.getNewGameMode();

		if (
			GameMode.SPECTATOR.equals(oldGameMode)
				|| GameMode.SPECTATOR.equals(newGameMode)
		) {
			Plugin plugin = Plugin.getInstance();
			if (plugin != null) {
				new BukkitRunnable() {
					@Override
					public void run() {
						plugin.mAbilityManager.updatePlayerAbilities(player, true);
					}
				}.runTaskLater(plugin, 0);
			}
		}
	}

	/*
	 * Prevent crafting with custom items
	 */
	@EventHandler(ignoreCancelled = true)
	public void craftItemEvent(CraftItemEvent event) {
		ItemStack result = event.getCurrentItem();
		if (result == null) {
			return;
		}
		Material resultMat = result.getType();
		String resultMatStr = resultMat.getKey().toString();

		boolean cancel = false;

		boolean gotDye = false;
		boolean resultIsDyed = false;
		if (resultMat.equals(Material.FIREWORK_STAR)
			|| resultMatStr.startsWith("minecraft:leather_")
			|| resultMatStr.endsWith("_banner")
			|| resultMatStr.endsWith("_shulker_box")) {
			resultIsDyed = true;
		}

		boolean gotBanner = false;
		boolean gotShield = resultMat.equals(Material.SHIELD);

		for (ItemStack item : event.getInventory().getMatrix()) {
			if (item != null) {
				Material mat = item.getType();
				String matStr = mat.getKey().toString();

				if (item.hasItemMeta()) {
					ItemMeta meta = item.getItemMeta();
					if (meta.hasLore()) {
						List<String> lore = ItemUtils.getPlainLore(item);
						if (lore.contains("Material")) {
							if (matStr.endsWith("_dye")) {
								gotDye = true;
							}
							if (matStr.endsWith("_banner")) {
								gotBanner = true;
							}
						} else {
							cancel = true;
						}
					} else { // Has no lore
						if (matStr.endsWith("_dye")) {
							gotDye = true;
						}
						if (matStr.endsWith("_banner")) {
							gotBanner = true;
						}
					}
				} else { // Has no meta
					if (matStr.endsWith("_dye")) {
						gotDye = true;
					}
					if (matStr.endsWith("_banner")) {
						gotBanner = true;
					}
				}
			}
		}

		if (gotDye && resultIsDyed) {
			cancel = false;
		}
		if (gotBanner && gotShield) {
			cancel = false;
		}
		if (resultMat.equals(Material.WRITTEN_BOOK)) {
			cancel = false;
		}
		if (resultMat.equals(Material.TIPPED_ARROW)) {
			cancel = false;
		}
		event.setCancelled(cancel);
	}

	private static final Set<DamageCause> DISABLE_KNOCKBACK_DAMAGE_CAUSES = Set.of(
			DamageCause.CONTACT,
			DamageCause.FALL,
			DamageCause.FIRE,
			DamageCause.FIRE_TICK,
			DamageCause.LAVA,
			DamageCause.VOID,
			DamageCause.STARVATION,
			DamageCause.POISON,
			DamageCause.WITHER,
			DamageCause.HOT_FLOOR);

	private final Set<UUID> mIgnoreKnockbackThisTick = new HashSet<>();
	private final Set<UUID> mIgnoreKnockbackNextTick = new HashSet<>();
	private int mKnockbackTaskId = -1;

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityDamageEvent(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player player
			    && player.getNoDamageTicks() <= player.getMaximumNoDamageTicks() / 2.0f // can only take knockback again after half the iframes are over
			    && DISABLE_KNOCKBACK_DAMAGE_CAUSES.contains(event.getCause())) {
			mIgnoreKnockbackNextTick.add(player.getUniqueId());

			// NB: The two sets are required because this task runs after the damage event, but before the velocity change event.
			if (mKnockbackTaskId < 0 || !Bukkit.getScheduler().isQueued(mKnockbackTaskId)) {
				mKnockbackTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(mPlugin, () -> {
					mIgnoreKnockbackThisTick.clear();
					mIgnoreKnockbackThisTick.addAll(mIgnoreKnockbackNextTick);
					mIgnoreKnockbackNextTick.clear();
					if (mIgnoreKnockbackThisTick.isEmpty()) {
						Bukkit.getScheduler().cancelTask(mKnockbackTaskId);
						mKnockbackTaskId = -1;
					}
				}, 0, 1);
			}
		}
	}

	// Handles cancelled events to properly remove the player from the mIgnoreNextKnockback set even if the event has already been cancelled by another listener
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerVelocityEvent(PlayerVelocityEvent event) {
		if (mIgnoreKnockbackThisTick.remove(event.getPlayer().getUniqueId())) {
			event.setCancelled(true);
		}
	}

}
