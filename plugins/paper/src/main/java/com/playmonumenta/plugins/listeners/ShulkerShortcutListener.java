package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.inventories.ShulkerInventory;
import com.playmonumenta.plugins.inventories.ShulkerInventoryManager;
import com.playmonumenta.plugins.itemstats.enchantments.CurseOfEphemerality;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.List;
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
		if (!(event.getWhoClicked() instanceof Player player) || clickedInventory == null) {
			return;
		}
		// A player clicked inside an inventory window

		// check if a shulker inventory is open
		ShulkerInventory shulkerInventory = ShulkerInventoryManager.getOpenShulkerInventory(player, topInventory);
		if (shulkerInventory != null // Shulker was opened via shortcut
			    && !mPlugin.mShulkerInventoryManager.updateShulker(player)) { // Try to update Shulker if it still exists.
			// The currently open shulker no longer exists, cancel the click and close the inventory.
			event.setCancelled(true);
			player.sendMessage(ChatColor.RED + "Shulker no longer available");
			new BukkitRunnable() {
				@Override
				public void run() {
					player.closeInventory(InventoryCloseEvent.Reason.CANT_USE);
				}
			}.runTask(mPlugin);
			return;
		}
		if (shulkerInventory != null
			    && event.getClickedInventory() == shulkerInventory.getInventory()
			    && event.getSlot() >= shulkerInventory.getSlots()) {
			// clicked on a reserved slot of a reduced-size shulker box
			event.setCancelled(true);
			return;
		}
		if (shulkerInventory != null) {
			boolean quiver = ItemStatUtils.isQuiver(shulkerInventory.getShulkerItem());
			if (quiver || shulkerInventory.getInventory().getType() != InventoryType.SHULKER_BOX) {
				// Disallow sorting partial inventories to prevent it duping the filler items and moving items into slots where they shouldn't be
				if (shulkerInventory.getSlots() % 9 != 0
					    && event.getClick().isRightClick()
					    && ItemUtils.isNullOrAir(event.getCursor())
					    && ItemUtils.isNullOrAir(event.getCurrentItem())) {
					event.setCancelled(true);
					return;
				}
				// prevent picking up the filler items if the player for some reason has some
				if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR
					    && ShulkerInventory.FILLER.isSimilar(event.getCursor())) {
					event.setCancelled(true);
					return;
				}
				// Quiver or modified shulker inventory is involved
				// For quivers, make sure only arrows can be put in it
				// For modified shulker inventories, prevent putting shulkers in
				ItemStack deposited = null;
				switch (event.getAction()) {
					case PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR -> {
						if (event.getClickedInventory() == shulkerInventory.getInventory()) {
							deposited = event.getCursor();
						}
					}
					case MOVE_TO_OTHER_INVENTORY -> {
						if (event.getClickedInventory() != shulkerInventory.getInventory()) {
							deposited = event.getCurrentItem();
						}
					}
					case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> {
						if (event.getClickedInventory() == shulkerInventory.getInventory()) {
							deposited = event.getClick() == ClickType.SWAP_OFFHAND ? player.getInventory().getItemInOffHand() : player.getInventory().getItem(event.getHotbarButton());
						}
					}
					case UNKNOWN -> { // unknown click (modded?): disallow
						event.setCancelled(true);
						return;
					}
					default -> {
						// nothing deposited for other actions
					}
				}
				if (deposited != null && deposited.getType() != Material.AIR) {
					if (ItemUtils.isShulkerBox(deposited.getType())) {
						event.setCancelled(true);
						return;
					}
					if (quiver && !ItemUtils.isArrow(deposited)) {
						player.sendMessage(ChatColor.RED + "Only arrows can be put into a quiver");
						player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						event.setCancelled(true);
						return;
					}
				}
			}
		}
		if (itemClicked != null
			    && click == ClickType.RIGHT
			    && isEnderExpansion(itemClicked)
			    && !clickedInventory.getType().equals(InventoryType.ENDER_CHEST)) {
			// Right clicked an Ender Chest Expansion shulker outside an ender chest
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			player.sendMessage(ChatColor.RED + "This item only works in an ender chest");
			event.setCancelled(true);
		} else if (itemClicked != null
			           && click == ClickType.RIGHT
			           && isPurpleTesseractContainer(itemClicked)) {
			// Right clicked a purple tesseract shulker that can't be opened
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			player.sendMessage(ChatColor.RED + "This container must be placed to access its items");
			event.setCancelled(true);
		} else if (itemClicked != null
			           && click == ClickType.RIGHT
			           && ChestUtils.isLootBox(itemClicked)) {
			// Right clicked a lootbox - dump contents into player's inventory
			List<ItemStack> items = ChestUtils.removeOneLootshareFromLootbox(itemClicked);
			if (items == null) {
				// Lootbox empty
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			} else {
				// Non-empty, got some items, drop them on the player
				// /playsound minecraft:block.chest.open player @s ~ ~ ~ 0.6 1
				player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.PLAYERS, 0.6f, 1f);
				for (ItemStack item : items) {
					if (item != null && !item.getType().isAir()) {
						InventoryUtils.dropTempOwnedItem(item, player.getLocation(), player);
					}
				}
			}
			event.setCancelled(true);
		} else if (itemClicked != null && ItemUtils.isShulkerBox(itemClicked.getType())
			           && !ShulkerEquipmentListener.isEquipmentBox(itemClicked)
			           && !PortableEnderListener.isPortableEnder(itemClicked)
			           && !ItemStatUtils.isShattered(itemClicked)) {
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
						if (remaining < 0) {
							// An error occurred (error message already sent to the player)
							player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (remaining == 0) {
							// All items were inserted successfully.
							player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
							player.sendMessage(ChatColor.GOLD + "Item deposited into shulker.");
							event.getView().setCursor(null);
						} else if (remaining == starting) {
							// No items were placed, shulker is full.
							player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
							player.sendMessage(ChatColor.RED + "That shulker is full.");
						} else {
							// Items were inserted, but not all
							player.sendMessage(ChatColor.RED + "That shulker was too full to accept the full stack.");
							player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
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

	/**
	 * Event Handler for when the player drags an ItemStack across one or more slots in an inventory.
	 * Used to update Shulker Boxes when items are dragged in their inventory.
	 *
	 * @see InventoryDragEvent
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void inventoryDragEvent(InventoryDragEvent event) {
		if (event.getWhoClicked() instanceof Player player) {
			ShulkerInventory shulkerInventory = ShulkerInventoryManager.getOpenShulkerInventory(player, event.getInventory());
			if (shulkerInventory != null) { // Shulker was opened via shortcut
				if (!acceptsItem(shulkerInventory, event.getOldCursor())
					    && event.getRawSlots().stream().anyMatch(slot -> event.getView().getInventory(slot) == shulkerInventory.getInventory())) {
					player.sendMessage(ChatColor.RED + "Only arrows can be put into a quiver");
					player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
					event.setCancelled(true);
					return;
				}
				if (!mPlugin.mShulkerInventoryManager.updateShulker(player)) { // Try to update Shulker if it still exists.
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
	}

	/**
	 * Event Handler for whenever an inventory is closed.
	 * Used to update and unlock Shulker Boxes when closed.
	 *
	 * @see InventoryCloseEvent
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		if (event.getPlayer() instanceof Player player) {
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
		ItemStack item = event.getItem();
		if (ItemUtils.isShulkerBox(item.getType())
			    && (ShulkerInventoryManager.isShulkerInUse(item)
				        || isPurpleTesseractContainer(item)
				        || isEnderExpansion(item)
				        || ItemStatUtils.getTier(item) != ItemStatUtils.Tier.SHULKER_BOX)) {
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
				@Nullable List<ItemStack> contents = ChestUtils.removeOneLootshareFromLootbox(item);
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
						ChestUtils.generateLootInventory(contents, chest.getInventory(), player, true);
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

	private static boolean acceptsItem(ShulkerInventory shulkerInventory, ItemStack item) {
		return !ItemStatUtils.isQuiver(shulkerInventory.getShulkerItem()) || ItemUtils.isArrow(item);
	}
}
