package com.playmonumenta.plugins.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.CommandBlock;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Constants.Colors;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.attributes.AttributeProjectileDamage;
import com.playmonumenta.plugins.attributes.AttributeThrowRate;
import com.playmonumenta.plugins.commands.ToggleSwap;
import com.playmonumenta.plugins.enchantments.Bleeding;
import com.playmonumenta.plugins.enchantments.Decay;
import com.playmonumenta.plugins.enchantments.Duelist;
import com.playmonumenta.plugins.enchantments.Frost;
import com.playmonumenta.plugins.enchantments.HexEater;
import com.playmonumenta.plugins.enchantments.IceAspect;
import com.playmonumenta.plugins.enchantments.Inferno;
import com.playmonumenta.plugins.enchantments.PointBlank;
import com.playmonumenta.plugins.enchantments.Regicide;
import com.playmonumenta.plugins.enchantments.RegionScalingDamageDealt;
import com.playmonumenta.plugins.enchantments.Slayer;
import com.playmonumenta.plugins.enchantments.Sniper;
import com.playmonumenta.plugins.enchantments.Spark;
import com.playmonumenta.plugins.enchantments.StatTrack.StatTrackOptions;
import com.playmonumenta.plugins.enchantments.StatTrackManager;
import com.playmonumenta.plugins.enchantments.ThunderAspect;
import com.playmonumenta.plugins.enchantments.curses.CurseOfEphemerality;
import com.playmonumenta.plugins.enchantments.evasions.EvasionInfo;
import com.playmonumenta.plugins.enchantments.infusions.Focus;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.EvasionEvent;
import com.playmonumenta.plugins.point.Point;
import com.playmonumenta.plugins.portals.PortalManager;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.server.reset.DailyReset;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.GraveUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;



public class PlayerListener implements Listener {
	Plugin mPlugin = null;

	public PlayerListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		if (ServerProperties.getJoinMessagesEnabled() == false) {
			event.setJoinMessage("");
		}

		/* This needs to stick around basically forever to remove this no-longer-needed tag */
		player.removeScoreboardTag("MidTransfer");

		mPlugin.mTrackingManager.addEntity(player);
		DailyReset.handle(mPlugin, player);
		//This checks to make sure that when you login you aren't stuck in blocks, just in case the lag that causes you to fall also kicks you. You don't want to be stuck in dirt forever, right?
		Location loc = player.getLocation();
		runTeleportRunnable(player, loc);
	}

	@EventHandler(priority = EventPriority.LOW)
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

		mPlugin.mTrackingManager.removeEntity(player);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void playerInteractEvent(PlayerInteractEvent event) {
		Action action = event.getAction();
		Player player = event.getPlayer();
		ItemStack item = event.getItem();
		Block block = event.getClickedBlock();

		Material mat = (block != null) ? block.getType() : Material.AIR;
		mPlugin.mAbilityManager.playerInteractEvent(player, action, item, mat);
		mPlugin.mTrackingManager.mPlayers.onPlayerInteract(mPlugin, player, event);

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
				    && ZoneUtils.inPlot(location, ServerProperties.getIsTownWorld())) {
					event.setUseInteractedBlock(Event.Result.DENY);
					return;
				}
				if (GraveUtils.isGrave(block)
				    && player.getGameMode() != GameMode.CREATIVE
				    && player.getGameMode() != GameMode.SPECTATOR) {
					Chest grave = (Chest) block.getState();
					if (GraveUtils.canPlayerOpenGrave(block, player)) {
						// Player has permission to access this grave. Move as much of the grave's contents as possible into the player's inventory.
						Inventory graveInventory = grave.getInventory();
						PlayerInventory playerInventory = player.getInventory();
						int itemsMoved = 0;
						int itemsLeftBehind = 0;
						for (int i = 0; i < graveInventory.getSize(); i++) {
							if (graveInventory.getItem(i) != null) {
								if (playerInventory.firstEmpty() != -1) {
									// Player has a space in their inventory. Move the item
									playerInventory.setItem(playerInventory.firstEmpty(), graveInventory.getItem(i));
									graveInventory.setItem(i, null);
									itemsMoved++;
								} else {
									// Player doesn't have a space in their inventory. Don't move anything
									itemsLeftBehind++;
								}
							}
						}
						player.sendActionBar(Component.text(String.format("Retrieved %d items from the grave. %d items remain.", itemsMoved, itemsLeftBehind)));
						if (itemsLeftBehind == 0) {
							block.setType(Material.AIR);
						}
						event.setUseInteractedBlock(Event.Result.DENY);
					} else {
						// Player does not have permission to access this grave.
						player.sendActionBar(Component.text("You cannot open ").append(grave.customName()));
						event.setUseInteractedBlock(Event.Result.DENY);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		if (event.isCancelled()) {
			return;
		}
		ItemStack item = event.getItemInHand();
		Player player = event.getPlayer();

		if (!mPlugin.mItemOverrides.blockPlaceInteraction(mPlugin, player, item, event)) {
			event.setCancelled(true);
		}
	}

	// Player interacts with an entity (not triggered on armor stands for some reason
	@EventHandler(priority = EventPriority.NORMAL)
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
			if (player.getGameMode() == GameMode.ADVENTURE
			    && ZoneUtils.inPlot(frame, ServerProperties.getIsTownWorld())) {
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
						List<Component> mapLore;

						if (giveMap.hasItemMeta()) {
							mapMeta = giveMap.getItemMeta();
						} else {
							mapMeta = Bukkit.getServer().getItemFactory().getItemMeta(Material.FILLED_MAP);
						}

						if (mapMeta.hasLore()) {
							mapLore = mapMeta.lore();
						} else {
							mapLore = new ArrayList<Component>(1);
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
	@EventHandler(priority = EventPriority.LOW)
	public void playerArmorStandManipulateEvent(PlayerArmorStandManipulateEvent event) {
		if (event.isCancelled()) {
			return;
		}

		Player player = event.getPlayer();
		ArmorStand armorStand = event.getRightClicked();

		/* Don't let the player do this in a restricted zone */
		if (ZoneUtils.hasZoneProperty(player, ZoneProperty.RESTRICTED) && player.getGameMode() != GameMode.CREATIVE) {
			event.setCancelled(true);
			return;
		}

		// Plot Security: If armor stand is in a plot but the player is in adventure, cancel.
		if (player.getGameMode() == GameMode.ADVENTURE
		    && ZoneUtils.inPlot(armorStand, ServerProperties.getIsTownWorld())) {
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void entityShootBowEvent(EntityShootBowEvent event) {
		if (event.getEntity() instanceof Player && event.getArrowItem() != null && event.getArrowItem().hasItemMeta() && event.getArrowItem().getItemMeta().hasLore()) {
			mPlugin.mAttributeManager.mAttributeTrie.resetArrow((Player) event.getEntity());
			mPlugin.mAttributeManager.updateAttributeArrowTrie(mPlugin, (Player) event.getEntity(), event.getArrowItem());
		}
	}

	// The Player swapped their current selected item.
	@EventHandler(priority = EventPriority.HIGH)
	public void playerItemHeldEvent(PlayerItemHeldEvent event) {
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer(), event);
	}

	// The player dropped an item.
	@EventHandler(priority = EventPriority.HIGH)
	public void playerDropItemEvent(PlayerDropItemEvent event) {
		if (event.isCancelled()) {
			return;
		}

		Player player = event.getPlayer();

		/* Don't let the player do this when in a restricted zone */
		if (ZoneUtils.hasZoneProperty(player, ZoneProperty.RESTRICTED) && player.getGameMode() != GameMode.CREATIVE) {
			event.setCancelled(true);
			return;
		}
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer(), event);
	}

	// An entity picked up an item
	@EventHandler(priority = EventPriority.HIGH)
	public void entityPickupItemEvent(EntityPickupItemEvent event) {
		if (event.isCancelled()) {
			return;
		}

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
		}
	}

	// An item on the player breaks.
	@EventHandler(priority = EventPriority.HIGH)
	public void playerItemBreakEvent(PlayerItemBreakEvent event) {
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer(), event);
	}

	// If an inventory interaction happened.
	@EventHandler(priority = EventPriority.NORMAL)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (event.isCancelled()) {
			return;
		}
		// If item contains curse of ephemerality, prevent from putting in other inventories
		// Checks for player inevntory unless it's a shift click
		if (event.getWhoClicked() instanceof Player && (event.getCursor() != null
			&& CurseOfEphemerality.isEphemeral(event.getCursor())
			&& !(event.getClickedInventory() instanceof PlayerInventory)
			|| event.getCurrentItem() != null && CurseOfEphemerality.isEphemeral(event.getCurrentItem())
			&& (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT))) {
			event.setCancelled(true);
		}
	}

	// If an item is being dragged in an inventory
	@EventHandler(priority = EventPriority.HIGH)
	public void inventoryDragEvent(InventoryDragEvent event) {
		if (event.isCancelled()) {
			return;
		}
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
	@EventHandler(priority = EventPriority.HIGH)
	public void inventoryOpenEvent(InventoryOpenEvent event) {
		if (event.isCancelled()) {
			return;
		}

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
	@EventHandler(priority = EventPriority.HIGH)
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		Inventory inventory = event.getInventory();
		InventoryHolder holder = inventory.getHolder();
		if (holder != null && holder instanceof Player) {
			Player player = (Player) holder;

			ItemStack mainHand = player.getInventory().getItemInMainHand();
			ItemStack offHand = player.getInventory().getItemInOffHand();

			mPlugin.mAbilityManager.playerItemHeldEvent(player, mainHand, offHand);
		} else if (holder instanceof Chest) {
			Chest chest = (Chest) holder;
			// Break empty graves or halloween creeper chests in safe zones automatically when closed
			if (ChestUtils.isEmpty(chest)
			    && (GraveUtils.isGrave(chest)
			        || (chest.getCustomName() != null && chest.getCustomName().contains("Creeperween Chest")))) {
				chest.getBlock().breakNaturally();
			}
		}
		if (event.getPlayer() != null && event.getPlayer() instanceof Player) {
			InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, (Player) event.getPlayer(), event);
		}
	}

	// Something interacts with an inventory
	@EventHandler(priority = EventPriority.HIGH)
	public void inventoryInteractEvent(InventoryInteractEvent event) {
		if (event.isCancelled()) {
			return;
		}

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
	@EventHandler(priority = EventPriority.LOW)
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
	@EventHandler(priority = EventPriority.HIGH)
	public void playerChangedMainHandEvent(PlayerChangedMainHandEvent event) {
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer(), event);
	}

	// Player swapped hand items
	@EventHandler(priority = EventPriority.HIGH)
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (event.isCancelled()) {
			return;
		}
		mPlugin.mAbilityManager.playerSwapHandItemsEvent(event.getPlayer(), event);
		if (event.getPlayer().getScoreboardTags().contains(ToggleSwap.SWAP_TAG)) {
			event.setCancelled(true);
		}
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer(), event);
	}

	// The player has died
	@EventHandler(priority = EventPriority.HIGH)
	public void playerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();

		if (event.isCancelled()) {
			return;
		}

		mPlugin.mTrackingManager.mPlayers.onDeath(mPlugin, player, event);
		mPlugin.mAbilityManager.playerDeathEvent(player, event);

		if (player.getHealth() > 0) {
			return;
		}

		// Prevent an inescapable death loop by overriding KeepInventory if your Max Health is 0
		if (event.getKeepInventory()) {
			AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
			if (maxHealth != null && maxHealth.getValue() <= 0) {
				event.setKeepInventory(false);
			}
		}

		// Give the player a NewDeath score of 1 so the city guides will give items again
		ScoreboardUtils.setScoreboardValue(player, "NewDeath", 1);

		if (event.getDeathMessage() != null && ScoreboardUtils.getScoreboardValue(player, Constants.SCOREBOARD_DEATH_MESSAGE) != 0) {
			player.sendMessage(event.deathMessage());
			player.sendMessage(Component.text("Only you saw this message. Change this with /deathmsg", NamedTextColor.AQUA));
			event.setDeathMessage(null);
		}

		// Clear effects
		mPlugin.mPotionManager.clearAllPotions(player);
		mPlugin.mAbilityManager.updatePlayerAbilities(player);
	}

	// The player has respawned.
	@EventHandler(priority = EventPriority.HIGH)
	public void playerRespawnEvent(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		final String name = player.getName();

		player.getServer().getScheduler().scheduleSyncDelayedTask(mPlugin, new Runnable() {
			@Override
			public void run() {
				Player player = Bukkit.getPlayer(name);

				mPlugin.mPotionManager.clearAllPotions(player);
				mPlugin.mAbilityManager.updatePlayerAbilities(player);

				ItemStack mainHand = player.getInventory().getItemInMainHand();
				ItemStack offHand = player.getInventory().getItemInOffHand();

				mPlugin.mAbilityManager.playerItemHeldEvent(player, mainHand, offHand);
				InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);
			}
		}, 0);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerFishEvent(PlayerFishEvent event) {
		if (event.isCancelled()) {
			return;
		}

		if (event.getState() == State.FISHING) {
			mPlugin.mTrackingManager.mFishingHook.addEntity(event.getPlayer(), event.getHook());
		} else if (event.getState() == State.CAUGHT_ENTITY || event.getState() == State.CAUGHT_FISH) {
			mPlugin.mTrackingManager.mFishingHook.removeEntity(event.getPlayer());

			if (event.getState() == State.CAUGHT_ENTITY && !EntityUtils.isHostileMob(event.getCaught())) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerItemConsumeEvent(PlayerItemConsumeEvent event) {
		Player player = event.getPlayer();
		mPlugin.mAbilityManager.playerItemConsumeEvent(player, event);

		/* Don't let players consume shattered items */
		if (ItemUtils.isItemShattered(event.getItem())) {
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

		if (event.getItem().containsEnchantment(Enchantment.ARROW_INFINITE)) {
			event.setReplacement(event.getItem());
			//Stat tracker for consuming infinity items
			StatTrackManager.incrementStat(event.getItem(), player, StatTrackOptions.TIMES_CONSUMED, 1);
		}
		mPlugin.mTrackingManager.mPlayers.onConsume(mPlugin, player, event);

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
					effect.getType().equals(PotionEffectType.SLOW_FALLING) &&
					player.getGameMode().equals(GameMode.ADVENTURE)) {
				//Remove Slow Falling effects in Adventure mode areas (#947)
				player.sendMessage(Component.text("You cannot apply slow falling potion effects in adventure mode areas, other effects were still applied.", NamedTextColor.RED));
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
	@EventHandler(priority = EventPriority.LOW)
	public void playerItemDamageEvent(PlayerItemDamageEvent event) {
		if (event.isCancelled()) {
			return;
		}

		ItemStack item = event.getItem();
		mPlugin.mAbilityManager.playerItemDamageEvent(event.getPlayer(), event);

		int damage = event.getDamage();

		if (ItemUtils.isArmor(item)) {
			// Players that get resistance from safezones don't take armor damage
			if (damage < 0 || ZoneUtils.hasZoneProperty(event.getPlayer(), ZoneProperty.NO_EQUIPMENT_DAMAGE)) {
				damage = 0;
			}

			event.setDamage(damage);
		}

		//Tridents do not take damage in safezones
		if (event.getItem().getType() == Material.TRIDENT && ZoneUtils.hasZoneProperty(event.getPlayer(), ZoneProperty.NO_EQUIPMENT_DAMAGE)) {
			event.setDamage(0);
		}

		mPlugin.mTrackingManager.mPlayers.onItemDamage(mPlugin, event.getPlayer(), event);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerRiptideEvent(PlayerRiptideEvent event) {
		Player player = event.getPlayer();
		Location loc = player.getLocation();
		//Manually forces the player in place during the riptide if they use it out of water (in rain)
		if (!mPlugin.mItemOverrides.playerRiptide(mPlugin, player, event)) {
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

	@EventHandler(priority = EventPriority.LOW)
	public void playerExpChangeEvent(PlayerExpChangeEvent event) {
		Player player = event.getPlayer();

		mPlugin.mTrackingManager.mPlayers.onExpChange(mPlugin, player, event);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
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

	/** Implements bed teleporters.
	 *
	 * When a player sleeps in a bed, the 10 blocks under that bed are checked for
	 * a regular command block.
	 *
	 * If a command block is found, it's command string is used for coordinates to teleport
	 * the player to after a short delay
	 */
	@EventHandler(priority = EventPriority.LOW)
	public void playerBedEnterEvent(PlayerBedEnterEvent event) {
		if (event.isCancelled()) {
			return;
		}

		Player player = event.getPlayer();
		Block bed = event.getBed();
		Location loc = bed.getLocation();
		World world = loc.getWorld();

		//Records player's previous spawnpoint
		Location tempSpawnLoc = world.getSpawnLocation(); //Default spawn is za warudo spawn
		if (player.getBedSpawnLocation() != null && !(player.getBedSpawnLocation().equals(loc))) {
			tempSpawnLoc = player.getBedSpawnLocation(); //If the player has another bed in the world, that's their spawn
		}
		Location playerSpawn = tempSpawnLoc; //Weird scope error workaround

		/* Prevent entering beds designed to glitch through blocks */
		Material aboveMat = loc.add(0, 1, 0).getBlock().getType();
		if (aboveMat.equals(Material.BEDROCK) || aboveMat.equals(Material.BARRIER) || aboveMat.equals(Material.OBSIDIAN)) {
			new BukkitRunnable() {
				float mFreq = 0;

				@Override
				public void run() {
					player.playSound(player.getLocation(), Sound.ENTITY_HORSE_DEATH, SoundCategory.MASTER, 1, mFreq);
					mFreq += 0.05;
					if (mFreq > 1.5) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 10);

			event.setCancelled(true);
			return;
		}

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

					// Create a deferred task to eject player and teleport them after a short sleep
					new BukkitRunnable() {
						Integer mTicks = 0;
						@Override
						public void run() {
							GameMode mode;
							final int BED_TELE_TIME = 20 * 3;

							if (++mTicks == BED_TELE_TIME) {
								// Abort, player got out of bed early
								if (player.isSleeping() == false) {
									this.cancel();
									return;
								}

								// Get player's current gamemode
								mode = player.getGameMode();

								// Set player's gamemode to survival so they can be damaged
								player.setGameMode(GameMode.SURVIVAL);

								// Poke the player to eject them from the bed
								player.damage(0.001);

								// Set player's gamemode back to whatever it was
								player.setGameMode(mode);

								//Set player's spawnpoint back to whatever it was
								//I couldn't find a Bukkit method to do so, ran the spawnpoint command instead from console
								String cmd = String.format("spawnpoint %s %d %d %d", player.getName(), playerSpawn.getBlockX(), playerSpawn.getBlockY(), playerSpawn.getBlockZ());
								Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd);

							} else if (mTicks >= BED_TELE_TIME + 1) {
								player.teleport(teleLoc);

								world.playSound(teleLoc, Sound.ENTITY_ELDER_GUARDIAN_DEATH, 1.0f, 1.3f);

								this.cancel();
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
	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerBedLeaveEvent(PlayerBedLeaveEvent event) {
		Player player = event.getPlayer();
		if (MetadataUtils.checkOnceThisTick(mPlugin, player, PLAYER_LEFT_BED_TICK_METAKEY)) {
			Block bed = event.getBed();
			Location loc = bed.getLocation();
			player.teleport(loc.add(0.5, 1, 0.5));
		}
	}

	@EventHandler(priority = EventPriority.LOW)
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

	@EventHandler
	public void abilityCastEvent(AbilityCastEvent event) {
		Player player = event.getCaster();
		mPlugin.mAbilityManager.abilityCastEvent(player, event);
	}

	@EventHandler
	public void blockBreakEvent(BlockBreakEvent event) {
		if (event.isCancelled()) {
			return;
		}

		Player player = event.getPlayer();
		ItemStack item = player.getInventory().getItemInMainHand();
		if (!mPlugin.mItemOverrides.blockBreakInteraction(mPlugin, event.getPlayer(), event.getBlock(), event)) {
			event.setCancelled(true);
			return;
		}

		mPlugin.mTrackingManager.mPlayers.onBlockBreak(mPlugin, player, event, item);
	}

	@EventHandler
	public void evasionEvent(EvasionEvent event) {
		Player player = event.getPlayer();
		mPlugin.mTrackingManager.mPlayers.onEvade(mPlugin, player, event);
	}

	@EventHandler
	public void playerAnimationEvent(PlayerAnimationEvent event) {
		Player player = event.getPlayer();
		if (event.getAnimationType().equals(PlayerAnimationType.ARM_SWING)) {
			mPlugin.mAbilityManager.playerAnimationEvent(player, event);
		}
	}

	//Player is healed.
	@EventHandler(priority = EventPriority.LOW)
	public void onRegain(EntityRegainHealthEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			mPlugin.mTrackingManager.mPlayers.onRegain(mPlugin, player, event);
		}
	}

	/*
	 * Handles on damage enchants. Needs to be EventPriority.LOW to apply
	 * base damage modifiers like projectile attributes before any
	 * modifiers from custom effects (EventPriority.NORMAL) or abilities
	 * (EventPriority.HIGH) are applied.
	 */
	@EventHandler(priority = EventPriority.LOW)
	public void entityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		if (event.isCancelled()) {
			return;
		}

		Entity damagee = event.getEntity();
		Entity damager = event.getDamager();

		if (damagee instanceof Player) {
			Player player = (Player) damagee;

			mPlugin.mTrackingManager.mPlayers.onHurtByEntity(mPlugin, player, event);
			if (event.getDamage() > 0) {
				EvasionInfo.triggerEvasion(player, event);
			}
		} else if (damagee instanceof LivingEntity && !(damagee instanceof Villager)) {
			if (damager instanceof Projectile) {
				ProjectileSource source = ((Projectile) damager).getShooter();
				if (source instanceof Player) {
					Projectile proj = (Projectile) damager;
					LivingEntity le = (LivingEntity) damagee;

					AttributeProjectileDamage.onShootAttack(mPlugin, proj, le, event);

					Sniper.onShootAttack(mPlugin, proj, le, event);
					PointBlank.onShootAttack(mPlugin, proj, le, event);
					Frost.onShootAttack(mPlugin, proj, le, event);
					Inferno.onShootAttack(mPlugin, proj, le, event);
					Focus.onShootAttack(mPlugin, proj, le, event);
					Spark.onShootAttack(mPlugin, proj, le, event);
					Regicide.onShootAttack(mPlugin, proj, le, event);

					if (damager instanceof Trident) {
						IceAspect.onShootAttack(mPlugin, proj, le, event);
						ThunderAspect.onShootAttack(mPlugin, proj, le, event);
						Bleeding.onShootAttack(mPlugin, proj, le, event);
						Decay.onShootAttack(mPlugin, proj, le, event);
						HexEater.onShootAttack(mPlugin, proj, le, event);
						Slayer.onShootAttack(mPlugin, proj, le, event);
						Duelist.onShootAttack(mPlugin, proj, le, event);

						//Fire aspect trident implementation
						if (damager.hasMetadata(AttributeThrowRate.FIRE_ASPECT_META) && !EntityUtils.isFireResistant(le)) {
							le.setFireTicks(le.getFireTicks() + 4 * 20 * damager.getMetadata(AttributeThrowRate.FIRE_ASPECT_META).get(0).asInt());
						}

						Inferno.onShootAttack(mPlugin, proj, le, event);

						/*
						 * The trident damage from Smite, Bane, Impaling seems to be properly applied, even
						 * though AttributeProjectileDamage.onShootAttack(mPlugin, proj, le, event); does
						 * direct damage setting, so that's convenient
						 */
					}

					RegionScalingDamageDealt.onShootAttack(mPlugin, proj, le, event);
				}
			} else if (damager instanceof Player) {
				if (event.getCause() != DamageCause.THORNS) {
					Player player = (Player) damager;

					mPlugin.mTrackingManager.mPlayers.onDamage(mPlugin, player, (LivingEntity) damagee, event);

					if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
						mPlugin.mTrackingManager.mPlayers.onAttack(mPlugin, player, (LivingEntity) damagee, event);
					}
				}
			}
		}
	}

	/*
	 * Pick survival or adventure as appropriate for zone properties when
	 * switching into either gamemode.
	 */
	@EventHandler(ignoreCancelled = true)
	public void gamemodeCorrection(@NotNull PlayerGameModeChangeEvent event) {
		@NotNull GameMode newGameMode = event.getNewGameMode();
		@NotNull Player player = event.getPlayer();
		boolean shouldBeAdventure = (
			ZoneUtils.hasZoneProperty(player, ZoneProperty.ADVENTURE_MODE)
			&& !ZoneUtils.isInPlot(player)
		);

		//NOTE Once we update Paper version more,
		// replace the event's generic failure message via
		// event.cancelMessage(),
		// instead of doing player.sendMessage() separately
		if (GameMode.SURVIVAL.equals(newGameMode) && shouldBeAdventure) {
			// event.cancelMessage(
			// 	Component
			// 		.text("Set own game mode to ", Colors.GREENISH_BLUE)
			// 		.append(
			// 			Component.text("Adventure Mode ", Colors.GREENISH_BLUE_DARK)
			// 		)
			// 		.append(
			// 			Component.text(
			// 				"instead of Survival due to zone.",
			// 				Colors.GREENISH_BLUE
			// 			)
			// 		)
			// );
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
			// event.cancelMessage(
			// 	Component
			// 		.text("Set own game mode to ", Colors.GREENISH_BLUE)
			// 		.append(
			// 			Component.text("Survival Mode ", Colors.GREENISH_BLUE_DARK)
			// 		)
			// 		.append(
			// 			Component.text(
			// 				"instead of Adventure due to zone.",
			// 				Colors.GREENISH_BLUE
			// 			)
			// 		)
			// );
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

	/*
	 * Refresh class abilities after event (end of tick)
	 * when switching to/from spectator.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void spectatorAbilityRefresh(@NotNull PlayerGameModeChangeEvent event) {
		@NotNull Player player = event.getPlayer();
		@NotNull GameMode oldGameMode = player.getGameMode();
		@NotNull GameMode newGameMode = event.getNewGameMode();

		if (
			GameMode.SPECTATOR.equals(oldGameMode)
			|| GameMode.SPECTATOR.equals(newGameMode)
		) {
			@Nullable Plugin plugin = Plugin.getInstance();
			if (plugin != null) {
				new BukkitRunnable() {
					@Override
					public void run() {
						plugin.mAbilityManager.updatePlayerAbilities(player);
					}
				}.runTaskLater(plugin, 0);
			}
		}
	}

	/*
	 * Prevent crafting with custom items
	 */
	@EventHandler(ignoreCancelled = true)
	public void craftItemEvent(@NotNull CraftItemEvent event) {
		ItemStack result = event.getCurrentItem();
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
}