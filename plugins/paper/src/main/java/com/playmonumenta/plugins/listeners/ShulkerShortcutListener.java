package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.inventories.ShulkerInventoryManager;
import com.playmonumenta.plugins.itemstats.enchantments.CurseOfEphemerality;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import javax.annotation.Nullable;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.permissions.Permission;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * These listeners work together with ShulkerInventoryManager and ShulkerInventory to
 * Allow players to access Shulker Boxes without being placed first.
 *
 * @see com.playmonumenta.plugins.inventories.ShulkerInventoryManager
 * @see com.playmonumenta.plugins.inventories.ShulkerInventory
 */
public class ShulkerShortcutListener implements Listener {
	private static final Permission PERMISSION = new Permission("monumenta.feature.shulkershortcut");
	private final Plugin mPlugin;

	public ShulkerShortcutListener(Plugin plugin) {
		mPlugin = plugin;
	}

	/**
	 * Event Handler for when a player performs any click inside any inventory.
	 * Used to prevent open Shulker Boxes from being removed from an inventory,
	 * as well as to open Shulker Boxes or deposit items when right-clicked.
	 *
	 * @see InventoryClickEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		ClickType click = event.getClick();
		InventoryAction action = event.getAction();
		ItemStack itemHeld = event.getCursor();
		ItemStack itemClicked = event.getCurrentItem();
		int slotClicked = event.getSlot();
		Inventory topInventory = event.getView().getTopInventory();
		Inventory clickedInventory = event.getClickedInventory();
		if (event.getWhoClicked() instanceof Player && clickedInventory != null) {
			// A player clicked inside an inventory window
			Player player = (Player)event.getWhoClicked();
			if (topInventory.getType() == InventoryType.SHULKER_BOX &&           // Player has Shulker open
			    ShulkerInventoryManager.playerHasShulkerOpen(player) &&          // Shulker was opened via shortcut
			    !mPlugin.mShulkerInventoryManager.updateShulker(player)) {       // Try to update Shulker if it still exists.
				// The currently open shulker no longer exists, cancel the click and close the inventory.
				event.setCancelled(true);
				player.sendMessage(ChatColor.RED + "Shulker no longer available");
				new BukkitRunnable() {
					@Override
					public void run() {
						player.closeInventory(InventoryCloseEvent.Reason.CANT_USE);
					}
				}.runTask(mPlugin);
			} else if (itemClicked != null &&
				click == ClickType.RIGHT &&
				isEnderExpansion(itemClicked) &&
				!clickedInventory.getType().equals(InventoryType.ENDER_CHEST)) {
				// Right clicked an Ender Chest Expansion shulker outside an ender chest
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
				player.sendMessage(ChatColor.RED + "This item only works in an ender chest");
				event.setCancelled(true);
			} else if (itemClicked != null &&
				click == ClickType.RIGHT &&
				(isPurpleTesseractContainer(itemClicked) || ChestUtils.isLootBox(itemClicked))) {
				// Right clicked a purple tesseract shulker or lootbox that can't be opened
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
				player.sendMessage(ChatColor.RED + "This container must be placed to access its items");
				event.setCancelled(true);
			} else if (itemClicked != null && ItemUtils.isShulkerBox(itemClicked.getType()) &&
			           !ShulkerEquipmentListener.isEquipmentBox(itemClicked) &&
			           !PortableEnderListener.isPortableEnder(itemClicked) &&
			           !ItemStatUtils.isShattered(itemClicked)) {
				// Player clicked a non-shattered non-equipment shulker box in an inventory.
				if (ShulkerInventoryManager.isShulkerInUse(itemClicked)) {
					// A currently open shulker box was clicked, cancel.
					player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
					player.sendMessage(ChatColor.RED + "That shulker is open");
					event.setCancelled(true);
				} else {
					// A shulker box that isn't currently open was clicked.
					if (player.hasPermission(PERMISSION)) {
						if (click == ClickType.RIGHT && action == InventoryAction.SWAP_WITH_CURSOR &&
							itemHeld != null && !ItemUtils.isShulkerBox(itemHeld.getType()) &&
							!CurseOfEphemerality.isEphemeral(itemHeld)) {

							// Player right-clicked shulker while holding an item on their cursor.
							event.setCancelled(true);
							int starting = itemHeld.getAmount();
							int remaining = mPlugin.mShulkerInventoryManager.addItemToShulker(player, clickedInventory, slotClicked, itemHeld);
							switch (remaining) {
								case -5:
									// Rate limited
									player.sendMessage(ChatColor.RED + "Too fast! Please try again");
									player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
									break;
								case -4:
									// Shulker blocked by zone property
									player.sendMessage(ChatColor.RED + "Shulkers can not be opened here");
									player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
									break;
								case -3:
									// Somehow that wasn't a shulker
									player.sendMessage(ChatColor.RED + "How did you...? That isn't a shulker. Please report this");
									player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
									break;
								case -2:
									// Shulker is locked
									player.sendMessage(ChatColor.RED + "That shulker is locked");
									player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
									break;
								case -1:
									// Shulker is already open
									player.sendMessage(ChatColor.RED + "That shulker is already open");
									player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
									break;
								case 0:
									// All items were inserted successfully.
									player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
									player.sendMessage(ChatColor.GOLD + "Item deposited into shulker.");
									event.getView().setCursor(null);
									break;
								default:
									if (remaining == starting) {
										// No items were placed, shulker is full.
										player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
										player.sendMessage(ChatColor.RED + "That shulker is full.");
									} else {
										// Items were inserted, but not all
										player.sendMessage(ChatColor.RED + "That shulker was too full to accept the full stack.");
										player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
									}
							}
						} else if (click == ClickType.RIGHT && action == InventoryAction.PICKUP_HALF) {
							// Player right-clicked shulker with an empty cursor.
							if (mPlugin.mShulkerInventoryManager.openShulker(player, clickedInventory, slotClicked)) {
								// Shulker was successfully opened
								player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
							} else {
								// Shulker couldn't be opened
								player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
							}
							event.setCancelled(true);
						} else if (ShulkerInventoryManager.playerIsShulkerRateLimited(player)) {
							player.sendMessage(ChatColor.RED + "Too fast! Please try again");
							player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
							event.setCancelled(true);
						}
					}
				}
			}
		}
	}

	/**
	 * Event Handler for when the player drags an ItemStack across one or more slots in an inventory.
	 * Used to update Shulker Boxes when items are dragged in their inventory.
	 *
	 * @see InventoryDragEvent
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void inventoryDragEvent(InventoryDragEvent event) {
		if (event.getWhoClicked() instanceof Player) {
			Player player = (Player)event.getWhoClicked();
			if (event.getView().getTopInventory().getType() == InventoryType.SHULKER_BOX && // Player has Shulker open
				ShulkerInventoryManager.playerHasShulkerOpen(player) && // Shulker was opened via shortcut
				!mPlugin.mShulkerInventoryManager.updateShulker(player)) { // Try to update Shulker if it still exists.
				// The currently open shulker no longer exists, cancel the click and close the inventory.
				event.setCancelled(true);
				player.sendMessage(ChatColor.RED + "Shulker no longer available");
				new BukkitRunnable() {
					@Override
					public void run() {
						player.closeInventory(InventoryCloseEvent.Reason.CANT_USE);
					}
				}.runTask(mPlugin);
			}
		}
	}

	/**
	 * Event Handler for whenever an inventory is closed.
	 * Used to update and unlock Shulker Boxes when closed.
	 *
	 * @see InventoryCloseEvent
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		if (event.getPlayer() instanceof Player) {
			Player player = (Player)event.getPlayer();
			mPlugin.mShulkerInventoryManager.closeShulker(player);
		}
	}

	/**
	 * Event Handler for whenever a dispenser is activated.
	 * Used to prevent placing open shulkers, and unlock shulkers with an invalid lock.
	 *
	 * @see BlockDispenseEvent
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void blockDispenseEvent(BlockDispenseEvent event) {
		if (ItemUtils.isShulkerBox(event.getItem().getType()) &&
			(ShulkerInventoryManager.isShulkerInUse(event.getItem()) ||
				isPurpleTesseractContainer(event.getItem()) ||
				isEnderExpansion(event.getItem()))) {
			event.getBlock().getWorld().playSound(event.getBlock().getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			event.setCancelled(true);
		}
	}

	/**
	 * Event Handler for whenever a block is placed.
	 * Used to prevent placing open shulkers, and unlock shulkers with an invalid lock.
	 *
	 * @see BlockPlaceEvent
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlockPlaced();
		if (ItemUtils.isShulkerBox(block.getType())) {
			if (ShulkerInventoryManager.isShulkerInUse(block)) {
				event.setCancelled(true);
				event.setBuild(false);
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
				player.sendMessage(ChatColor.RED + "That shulker is open");
			} else if (isPurpleTesseractContainer(event.getItemInHand())) {
				event.setCancelled(true);
				event.setBuild(false);

				ItemStack item = event.getItemInHand();
				ShulkerBox sbox = (ShulkerBox) ((BlockStateMeta) item.getItemMeta()).getBlockState();
				@Nullable ItemStack[] contents = sbox.getInventory().getContents();
				final String lockStr;
				if (sbox.isLocked()) {
					lockStr = sbox.getLock();
				} else {
					lockStr = null;
				}

				// Get the new chest and update that
				Bukkit.getScheduler().runTask(mPlugin, () -> {
					// Clears contents
					block.setType(Material.CHEST);

					if (block.getState() instanceof Chest chest) {
						if (lockStr != null) {
							chest.setLock(null);
							chest.customName(GsonComponentSerializer.gson().deserialize(lockStr));
						}
						chest.update();

						chest = (Chest)block.getState();
						chest.getInventory().setContents(contents);
					}
				});
				item.subtract();
			} else if (ChestUtils.isLootBox(event.getItemInHand())) {
				event.setCancelled(true);
				event.setBuild(false);

				ItemStack item = event.getItemInHand();
				@Nullable ItemStack[] contents = ChestUtils.removeOneLootshareFromLootbox(item);
				if (contents == null) {
					// LootBox is empty
					player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
					return;
				}

				// Get the new chest and update that
				Bukkit.getScheduler().runTask(mPlugin, () -> {
					// Clears contents
					block.setType(Material.CHEST);

					if (block.getState() instanceof Chest chest) {
						chest.update();

						chest = (Chest)block.getState();
						chest.getInventory().setContents(contents);
					}
				});
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		mPlugin.mShulkerInventoryManager.closeDepositShulker(event.getPlayer());
		mPlugin.mShulkerInventoryManager.closeShulker(event.getPlayer(), true);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerDeathEvent(PlayerDeathEvent event) {
		mPlugin.mShulkerInventoryManager.closeShulker(event.getEntity(), true);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerDropItemEvent(PlayerDropItemEvent event) {
		if (isEnderExpansion(event.getItemDrop().getItemStack())) {
			event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			if (event.getPlayer().getInventory().firstEmpty() >= 0) {
				event.setCancelled(true);
			}
		}
	}

	public static boolean isPurpleTesseractContainer(ItemStack item) {
		return item != null &&
		       ItemUtils.isShulkerBox(item.getType()) &&
			   item.hasItemMeta() &&
			   item.getItemMeta().hasDisplayName() &&
			   (ItemUtils.getPlainName(item).contains("Carrier of Emotion")
			    || ItemUtils.getPlainName(item).contains("Carrier of Festivity"));
	}

	public static boolean isEnderExpansion(ItemStack item) {
		return item != null &&
		       ItemUtils.isShulkerBox(item.getType()) &&
			   item.hasItemMeta() &&
			   item.getItemMeta().hasDisplayName() &&
			   ItemUtils.getPlainName(item).contains("Ender Chest Expansion");
	}
}
