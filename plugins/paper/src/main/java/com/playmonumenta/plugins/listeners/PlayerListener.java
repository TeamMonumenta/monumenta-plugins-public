package com.playmonumenta.plugins.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
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
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
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
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.enchantments.CurseOfEphemerality;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.EvasionEvent;
import com.playmonumenta.plugins.integrations.JeffChestSortIntegration;
import com.playmonumenta.plugins.point.Point;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.server.reset.DailyReset;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.GraveUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ItemUtils.ItemDeathResult;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class PlayerListener implements Listener {
	Plugin mPlugin = null;
	World mWorld = null;
	Random mRandom = null;

	public PlayerListener(Plugin plugin, World world, Random random) {
		mPlugin = plugin;
		mWorld = world;
		mRandom = random;
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
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerQuitEvent(PlayerQuitEvent event) {
		if (ServerProperties.getJoinMessagesEnabled() == false) {
			event.setQuitMessage("");
		}

		Player player = event.getPlayer();

		/* Remove ephemeral items on logout */
		InventoryUtils.removeSpecialItems(player, true);

		mPlugin.mTrackingManager.removeEntity(player);

		for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), 20)) {
			mPlugin.mCombatLoggingTimers.addTimer(mob.getUniqueId(), Constants.TEN_MINUTES);
			mob.setRemoveWhenFarAway(false);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void playerInteractEvent(PlayerInteractEvent event) {
		Action action = event.getAction();
		Player player = event.getPlayer();
		ItemStack item = event.getItem();
		Block block = event.getClickedBlock();

		Material mat = (block != null) ? block.getType() : Material.AIR;
		AbilityManager.getManager().playerInteractEvent(player, action, item, mat);
		mPlugin.mTrackingManager.mPlayers.onPlayerInteract(mPlugin, player, event);

		// Overrides
		// TODO: Rewrite overrides system to handle item/block interactions separately
		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			if (!mPlugin.mItemOverrides.leftClickInteraction(mPlugin, player, action, item, block)) {
				event.setCancelled(true);
				return;
			}
		} else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			if (!mPlugin.mItemOverrides.rightClickInteraction(mPlugin, player, action, item, block)) {
				event.setCancelled(true);
				return;
			}
		}

		// Item Interactions
		if (event.useItemInHand() != Event.Result.DENY) {
			if (item != null && ItemUtils.isArmorItem(item.getType())) {
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
						MessagingUtils.sendActionBarMessage(mPlugin, player, String.format("Retrieved %d items from the grave. %d items remain.", itemsMoved, itemsLeftBehind));
						if (itemsLeftBehind == 0) {
							block.setType(Material.AIR);
						}
						event.setUseInteractedBlock(Event.Result.DENY);
					} else {
						// Player does not have permission to access this grave.
						MessagingUtils.sendActionBarMessage(mPlugin, player, "You cannot open " + ChatColor.stripColor(grave.getCustomName()));
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
		if (clickedEntity instanceof Animals && itemInHand != null && itemInHand.getLore() != null) {
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
					player.sendMessage(ChatColor.GOLD + "This item frame is invulnerable / creative only");
				} else {
					event.setCancelled(true);
				}

				ItemStack frameItem = frame.getItem();
				if (frameItem != null && frameItem.getType().equals(Material.FILLED_MAP)) {
					if (player.getGameMode().equals(GameMode.ADVENTURE)) {
						ItemStack giveMap = frameItem.clone();
						ItemMeta mapMeta;
						List<String> mapLore;

						if (giveMap.hasItemMeta()) {
							mapMeta = giveMap.getItemMeta();
						} else {
							mapMeta = Bukkit.getServer().getItemFactory().getItemMeta(Material.FILLED_MAP);
						}

						if (mapMeta.hasLore()) {
							mapLore = mapMeta.getLore();
						} else {
							mapLore = new ArrayList<String>(1);
						}
						mapLore.add(ChatColor.GOLD + "* Official Map *");
						mapMeta.setLore(mapLore);
						giveMap.setItemMeta(mapMeta);
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
			InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);
		}
	}

	// An item on the player breaks.
	@EventHandler(priority = EventPriority.HIGH)
	public void playerItemBreakEvent(PlayerItemBreakEvent event) {
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer(), event);

		// If an item breaks, attempt to shatter it
		ItemStack iStack = event.getBrokenItem();
		ItemUtils.shatterItem(iStack);
		if (ItemUtils.isItemShattered(iStack)) {
			// If the item shatters, drop it on the player with instant pickup, if inv full for some reason - grave the item
			Player player = event.getPlayer();
			Location location = player.getLocation();
			Item item = player.getWorld().dropItemNaturally(location, iStack);
			GraveUtils.setGraveScoreboard(item, player, location);
			item.setPickupDelay(0);
		}
	}

	// If an inventory interaction happened.
	@EventHandler(priority = EventPriority.HIGH)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (event.getWhoClicked() instanceof Player) {
			Player player = (Player) event.getWhoClicked();
			InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);
			if (JeffChestSortIntegration.isPresent()) {
				Inventory inventory = event.getClickedInventory();
				if (inventory == null) {
					return;
				} else if (inventory instanceof PlayerInventory) {
					// Don't sort player inventories until support is added
					// to prevent sorting hotbar / armor slots
				} else {
					if (event.getClick() != null
					    && event.getClick().equals(ClickType.RIGHT)
					    && inventory.getItem(event.getSlot()) == null
					    && event.getAction().equals(InventoryAction.NOTHING)) {

						// Player right clicked an empty space and nothing happened
						// Check if the last thing the player did was also the same thing.
						// If so, sort the chest
						if (player.hasMetadata(Constants.PLAYER_CHEST_SORT_CLICK_COUNT_METAKEY)) {
							JeffChestSortIntegration.sortInventory(inventory);
							player.updateInventory();
							player.removeMetadata(Constants.PLAYER_CHEST_SORT_CLICK_COUNT_METAKEY, mPlugin);

							// Just in case we sorted an item on top of where the player was clicking
							event.setCancelled(true);
						} else {
							// Mark the player as having right clicked an empty slot
							player.setMetadata(Constants.PLAYER_CHEST_SORT_CLICK_COUNT_METAKEY, new FixedMetadataValue(mPlugin, 1));
						}
					} else {
						// Player did something else with this inventory - clear the right click metadata if present
						if (player.hasMetadata(Constants.PLAYER_CHEST_SORT_CLICK_COUNT_METAKEY)) {
							player.removeMetadata(Constants.PLAYER_CHEST_SORT_CLICK_COUNT_METAKEY, mPlugin);
						}
					}
				}
			}
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

			// Clear the player's sorting flag if present
			if (player.hasMetadata(Constants.PLAYER_CHEST_SORT_CLICK_COUNT_METAKEY)) {
				player.removeMetadata(Constants.PLAYER_CHEST_SORT_CLICK_COUNT_METAKEY, mPlugin);
			}

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

			// Clear the player's sorting flag if present
			if (player.hasMetadata(Constants.PLAYER_CHEST_SORT_CLICK_COUNT_METAKEY)) {
				player.removeMetadata(Constants.PLAYER_CHEST_SORT_CLICK_COUNT_METAKEY, mPlugin);
			}

			ItemStack mainHand = player.getInventory().getItemInMainHand();
			ItemStack offHand = player.getInventory().getItemInOffHand();

			AbilityManager.getManager().playerItemHeldEvent(player, mainHand, offHand);
		} else if (holder instanceof Chest) {
			Chest chest = (Chest) holder;
			// Break empty graves or halloween creeper chests in safe zones automatically when closed
			if (ChestUtils.isEmpty(chest)
			    && (GraveUtils.isGrave(chest)
			        || (chest.getCustomName() != null && chest.getCustomName().contains("Creeperween Chest")))) {
				if (!ZoneUtils.hasZoneProperty(chest.getLocation(), ZoneProperty.ADVENTURE_MODE)) {
					chest.getBlock().breakNaturally();
				}
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
		InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, event.getPlayer(), event);
	}

	private static final List<Integer> KEEP_EQUIPPED_SLOTS =
	    Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 36, 37, 38, 39, 40);

	private static final double KEPT_ITEM_DURABILITY_DAMAGE_PERCENT = 0.1;

	// The player has died
	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();

		mPlugin.mTrackingManager.mPlayers.onDeath(mPlugin, player, event);
		AbilityManager.getManager().playerDeathEvent(player, event);

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

		if (!event.getKeepInventory()) {
			/* Monumenta-custom keep inventory
			 *
			 * Keep armor and hotbar items if they meet some conditions (_isKeptItemOnDeath)
			 *
			 * The player always gets keepinv set on them to prevent relog bugs - so items must
			 * be manually dropped here if they don't meet the conditions.
			 *
			 * Items dropped are invulnerable for a short while to prevent double-creepering
			 */
			event.setKeepInventory(true);
			event.setKeepLevel(false);

			List<Item> droppedItems = new ArrayList<Item>();

			PlayerInventory inv = player.getInventory();
			for (int slot = 0; slot <= 40; slot++) {
				ItemStack item = inv.getItem(slot);
				if (item == null) {
					continue;
				}
				ItemDeathResult result = ItemUtils.getItemDeathResult(item);
				if (result == ItemDeathResult.SHATTER_NOW) {
					// Item has Curse of Vanishing 1. It should be shattered
					ItemUtils.shatterItem(item);
					// Then treat it like a normal item
					dropAndMarkItem(player, droppedItems, inv, slot, item, result);
				} else if (result == ItemDeathResult.DESTROY) {
					// Item has Curse of Vanishing 2, destroy item
					inv.setItem(slot, null);
				} else if (result == ItemDeathResult.KEEP) {
					// Item is kept with no durability loss
					// This empty if statement is intentional so it's not included in "else".
				} else if (result == ItemDeathResult.KEEP_DAMAGED
				           || (result == ItemDeathResult.KEEP_EQUIPPED && KEEP_EQUIPPED_SLOTS.contains(slot))) {
					ItemUtils.damageItemPercent(item, KEPT_ITEM_DURABILITY_DAMAGE_PERCENT, false);
				} else {
					// Migrated normal item treatment to a method so Curse of Vanishing items can be treated the same way
					dropAndMarkItem(player, droppedItems, inv, slot, item, result);
				}
			}

			if (droppedItems.size() > 0) {
				player.sendMessage(ChatColor.RED + "Some of your items were dropped! See /deathhelp for info");
			}
		}

		// Give the player a NewDeath score of 1 so the city guides will give items again
		ScoreboardUtils.setScoreboardValue(player, "NewDeath", 1);

		if (ScoreboardUtils.getScoreboardValue(player, Constants.SCOREBOARD_DEATH_MESSAGE) != 0) {
			player.sendMessage(event.getDeathMessage());
			player.sendMessage(ChatColor.AQUA + "Only you saw this message. Change this with /deathmsg");
			event.setDeathMessage("");
		}

		// Clear effects
		mPlugin.mPotionManager.clearAllPotions(player);
		mPlugin.mAbilityManager.updatePlayerAbilities(player);
	}

	private void dropAndMarkItem(Player player, List<Item> droppedItems, PlayerInventory inv, int slot, ItemStack item,
			ItemDeathResult result) {
		// Item is dropped, decide what to do with it.
		Location location = player.getLocation();
		Item droppedItem = player.getWorld().dropItemNaturally(location, item);
		// Make sure items don't float away unless they're in water
		new BukkitRunnable() {
			int mNumTicks = 0;

			@Override
			public void run() {
				if (droppedItem != null && droppedItem.isValid()) {
					Location dLoc = droppedItem.getLocation();
					// Check the item's location and the next block down for water, just in case it bobs out of the water.
					// Should fix the weird bug with hoped items no longer floating?
					if (!(dLoc.getBlock().isLiquid() || dLoc.clone().subtract(0, 1, 0).getBlock().isLiquid())
							&& dLoc.getY() > location.getY() + 2) {
						droppedItem.teleport(location);
					}
				} else {
					this.cancel();
				}

				// Very infrequently check if the item is still actually there
				mNumTicks++;
				if (mNumTicks > 30) {
					mNumTicks = 0;
					if (!EntityUtils.isStillLoaded(droppedItem)) {
						this.cancel();
					}
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 1 * 20, 1 * 20);
		if (InventoryUtils.testForItemWithLore(item, ChatColor.GRAY + "Hope")) {
			droppedItem.setInvulnerable(true);
		} else {
			// Make item invulnerable to explosions for 5 seconds
			droppedItem.addScoreboardTag("ExplosionImmune");
			new BukkitRunnable() {
				@Override
				public void run() {
					if (droppedItem != null && droppedItem.isValid()) {
						droppedItem.removeScoreboardTag("ExplosionImmune");
					}
				}
			}.runTaskLater(Plugin.getInstance(), 5 * 20);
		}
		if ((result == ItemDeathResult.SAFE || result == ItemDeathResult.SHATTER || result == ItemDeathResult.SHATTER_NOW)
		    && !player.getScoreboardTags().contains("DisableGraves")) {
			GraveUtils.setGraveScoreboard(droppedItem, player, location);
		}
		droppedItems.add(droppedItem);
		inv.clear(slot);
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

				AbilityManager.getManager().playerItemHeldEvent(player, mainHand, offHand);
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

	@EventHandler(priority = EventPriority.LOW)
	public void playerItemConsumeEvent(PlayerItemConsumeEvent event) {
		if (event.isCancelled()) {
			return;
		}

		Player player = event.getPlayer();
		AbilityManager.getManager().playerItemConsumeEvent(player, event);

		/* Don't let players consume shattered items */
		if (ItemUtils.isItemShattered(event.getItem())) {
			event.setCancelled(true);
			return;
		}

		if (event.getItem().getItemMeta() instanceof PotionMeta) {
			if (PotionUtils.isLuckPotion((PotionMeta) event.getItem().getItemMeta())) {
				Location loc = player.getLocation();
				loc.getWorld().playSound(loc, Sound.ENTITY_HORSE_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
				player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Luck potions can no longer be consumed");
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
		AbilityManager.getManager().playerItemDamageEvent(event.getPlayer(), event);

		int damage = event.getDamage();

		if (ItemUtils.isArmorItem(item.getType())) {
			// Players that get resistance from safezones don't take armor damage
			if (damage < 0 || ZoneUtils.hasZoneProperty(event.getPlayer(), ZoneProperty.NO_EQUIPMENT_DAMAGE)) {
				damage = 0;
			}

			event.setDamage(damage);
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
					player.sendMessage(ChatColor.RED + "Command block should be of the format 'x y z pitch yaw'");
					player.sendMessage(ChatColor.RED + "Relative and absolute coordinates accepted");
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

	/*
	 * Prevent players from passing through 1-thick barriers using beds
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerBedLeaveEvent(PlayerBedLeaveEvent event) {
		Player player = event.getPlayer();
		Block bed = event.getBed();
		Location loc = bed.getLocation();
		player.teleport(loc.add(0.5, 1, 0.5));
	}

	public static Set<Material> POTION_TYPES = EnumSet.of(Material.POTION, Material.SPLASH_POTION,
	                                                      Material.LINGERING_POTION);

	@EventHandler(priority = EventPriority.LOW)
	public void brewEvent(BrewEvent event) {
		BrewerInventory inv = event.getContents();
		ItemStack ingred = inv.getIngredient();
		ItemStack[] cont = inv.getStorageContents();
		boolean malfunction = false;
		for (ItemStack i : cont) {
			if (i != null) {
				if (POTION_TYPES.contains(i.getType())) {
					if (ingred.getType() == Material.MAGMA_CREAM) {
						malfunction = true;
						break;
					} else if (ingred.getType() == Material.TURTLE_HELMET) {
						malfunction = true;
						break;
					} else if (ingred.getType() == Material.FERMENTED_SPIDER_EYE) {
						PotionMeta meta = (PotionMeta) i.getItemMeta();
						PotionData data = meta.getBasePotionData();
						if (data.getType() == PotionType.NIGHT_VISION) {
							malfunction = true;
							break;
						}
					}
				}
			}
		}
		if (malfunction) {
			Block block = event.getBlock();
			Location loc = block.getLocation().add(0.5, 0.5, 0.5);
			loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);
			loc.getWorld().spawnParticle(Particle.FLAME, loc, 100, 0, 0, 0, 0.25);
			loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.1);
			loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_DEATH, 1, 0);
			loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
			loc.getBlock().setType(Material.AIR);
			event.setCancelled(true);
			for (Entity e : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
				if (e instanceof Player) {
					Player player = (Player) e;
					Vector v = player.getLocation().toVector().subtract(loc.toVector()).normalize();
					// Create the vector.
					v.add(new Vector(0, 0.5, 0));
					player.setVelocity(v.multiply(1)); // Set the velocity.
				}
			}
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
						AbilityManager.getManager().playerExtendedSneakEvent(player);
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

	@EventHandler(priority = EventPriority.MONITOR)
	public void playerGameModeChangeEvent(PlayerGameModeChangeEvent event) {
		if (!event.isCancelled()) {
			Player player = event.getPlayer();

			if (player.getGameMode().equals(GameMode.SPECTATOR) || event.getNewGameMode().equals(GameMode.SPECTATOR)) {
				/* Refresh class abilities when switching to/from spectator */
				new BukkitRunnable() {
					@Override
					public void run() {
						AbilityManager.getManager().updatePlayerAbilities(player);
					}
				}.runTaskLater(mPlugin, 0);
			}
			boolean isTownWorld = ServerProperties.getIsTownWorld();
			if (event.getNewGameMode().equals(GameMode.SURVIVAL)
			    && ZoneUtils.hasZoneProperty(player, ZoneProperty.ADVENTURE_MODE)
			    && !ZoneUtils.inPlot(player, isTownWorld)) {
				event.setCancelled(true);
				player.setGameMode(GameMode.ADVENTURE);
			}
			if (event.getNewGameMode().equals(GameMode.ADVENTURE)
			    && (!ZoneUtils.hasZoneProperty(player, ZoneProperty.ADVENTURE_MODE)
			        || ZoneUtils.inPlot(player, isTownWorld))) {
				event.setCancelled(true);
				player.setGameMode(GameMode.SURVIVAL);
			}
		}
	}

	@EventHandler
	public void abilityCastEvent(AbilityCastEvent event) {
		Player player = event.getCaster();
		AbilityManager.getManager().abilityCastEvent(player, event);
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
			AbilityManager.getManager().playerAnimationEvent(player, event);
		}
	}
}
