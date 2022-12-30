package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.Nullable;

public class PotionBarrelListener implements Listener {

	public static final String POTION_BARREL_NAME = "Potion Barrel";
	public static final NamespacedKey POTION_BARREL_LOOT_TABLE = NamespacedKeyUtils.fromString("epic:items/potion_barrel");

	// inventory handling

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		Inventory barrelInventory = event.getInventory();
		if (!(barrelInventory.getType() == InventoryType.BARREL
				&& barrelInventory.getHolder() instanceof BlockInventoryHolder blockInventoryHolder
				&& isPotionBarrel(blockInventoryHolder.getBlock())
				&& event.getWhoClicked() instanceof Player player)) {
			return;
		}
		if (event.getClick() == ClickType.UNKNOWN) {
			// disable all unknown/modded clicks
			event.setCancelled(true);
			return;
		}
		ItemStack cursorItem = event.getCursor();
		ItemStack clickedItem = event.getCurrentItem();
		if (event.getClickedInventory() != barrelInventory) {
			// Player inventory click: only need to handle shift clicks
			switch (event.getClick()) {
				case SHIFT_LEFT -> {
					// deposit potion
					event.setCancelled(true);
					if (clickedItem != null && clickedItem.getType() != Material.AIR) {
						if (ItemUtils.isSomePotion(clickedItem)) {
							ItemStack barrelPotion = getBarrelPotion(barrelInventory);
							if (barrelPotion == null || barrelPotion.isSimilar(clickedItem)) {
								if (!addToBarrel(barrelInventory, clickedItem)) {
									errorSound(player);
								}
							} else {
								errorSound(player);
							}
						} else {
							errorSound(player);
						}
					}
				}
				case SHIFT_RIGHT -> {
					// deposit all similar potions
					event.setCancelled(true);
					if (clickedItem != null && clickedItem.getType() != Material.AIR) {
						if (ItemUtils.isSomePotion(clickedItem)) {
							ItemStack barrelPotion = getBarrelPotion(barrelInventory);
							if (barrelPotion == null || barrelPotion.isSimilar(clickedItem)) {
								if (addToBarrel(barrelInventory, clickedItem)) {
									for (ItemStack playerItem : player.getInventory()) {
										if (playerItem != null && playerItem.isSimilar(barrelPotion)) {
											addToBarrel(barrelInventory, playerItem);
										}
									}
								} else {
									errorSound(player);
								}
							} else {
								errorSound(player);
							}
						} else {
							errorSound(player);
						}
					}
				}
				case RIGHT -> {
					// prevent quick drinking potions
					if (ItemUtils.isSomePotion(clickedItem)) {
						event.setCancelled(true);
					}
				}
				default -> {
					return;
				}
			}
		} else {
			switch (event.getClick()) {
				case LEFT, RIGHT -> {
					// Take a potion onto the cursor, or deposit one from the cursor
					event.setCancelled(true);
					if (cursorItem != null && cursorItem.getType() != Material.AIR) {
						if (ItemUtils.isSomePotion(cursorItem)) {
							ItemStack barrelPotion = getBarrelPotion(barrelInventory);
							if (barrelPotion == null || barrelPotion.isSimilar(cursorItem)) {
								if (!addToBarrel(barrelInventory, cursorItem)) {
									errorSound(player);
								}
							} else {
								errorSound(player);
							}
						} else {
							errorSound(player);
						}
					} else {
						if (clickedItem != null && clickedItem.getType() != Material.AIR) {
							ItemStack clone = ItemUtils.clone(clickedItem);
							clone.setAmount(1);
							removeOne(barrelInventory, clickedItem);
							player.setItemOnCursor(clone);
						}
					}
				}
				case SHIFT_LEFT -> {
					// Take one potion
					event.setCancelled(true);
					if (!ItemUtils.isNullOrAir(clickedItem) && InventoryUtils.numEmptySlots(player.getInventory()) > 0) {
						ItemStack clone = ItemUtils.clone(clickedItem);
						clone.setAmount(1);
						removeOne(barrelInventory, clickedItem);
						player.getInventory().addItem(clone);
					}
				}
				case SHIFT_RIGHT -> {
					// Fill inventory with potions
					event.setCancelled(true);
					if (!ItemUtils.isNullOrAir(clickedItem)) {
						takeAll(barrelInventory, player);
					}
				}
				case NUMBER_KEY -> {
					// Move potion to hotbar, or deposit potion from hotbar
					event.setCancelled(true);
					ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
					if (hotbarItem != null && hotbarItem.getType() != Material.AIR) {
						if (ItemUtils.isSomePotion(hotbarItem)) {
							ItemStack barrelPotion = getBarrelPotion(barrelInventory);
							if (barrelPotion == null || barrelPotion.isSimilar(hotbarItem)) {
								if (!addToBarrel(barrelInventory, hotbarItem)) {
									errorSound(player);
								}
							} else {
								errorSound(player);
							}
						} else {
							errorSound(player);
						}
					} else {
						if (clickedItem != null && clickedItem.getType() != Material.AIR) {
							ItemStack clone = ItemUtils.clone(clickedItem);
							clone.setAmount(1);
							removeOne(barrelInventory, clickedItem);
							player.getInventory().setItem(event.getHotbarButton(), clone);
						}
					}
				}
				case WINDOW_BORDER_LEFT, WINDOW_BORDER_RIGHT -> {
					// Perform vanilla function
				}
				case DROP, CONTROL_DROP -> {
					// Drop one, even if holding control, to prevent creating entity lag
					event.setCancelled(true);
					if (!ItemUtils.isNullOrAir(clickedItem)) {
						ItemStack clone = ItemUtils.clone(clickedItem);
						clone.setAmount(1);
						removeOne(barrelInventory, clickedItem);
						player.getWorld().dropItem(player.getEyeLocation(), clone, item -> item.setVelocity(player.getLocation().getDirection().multiply(0.3)));
					}
				}
				default -> {
					event.setCancelled(true);
				}
			}
		}
		// NB: No Core Protect logging required: It listens on MONITOR to all inventory clicks already, even cancelled ones.
	}


	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void inventoryDragEvent(InventoryDragEvent event) {
		Inventory barrelInventory = event.getInventory();
		if (!(barrelInventory.getType() == InventoryType.BARREL
				&& barrelInventory.getHolder() instanceof BlockInventoryHolder blockInventoryHolder
				&& isPotionBarrel(blockInventoryHolder.getBlock())
				&& event.getWhoClicked() instanceof Player player)) {
			return;
		}
		if (event.getRawSlots().stream().allMatch(slot -> event.getView().getInventory(slot) == player.getInventory())) {
			// Dragging around the player inventory only is allowed.
			return;
		}
		if (event.getRawSlots().stream().allMatch(slot -> event.getView().getInventory(slot) == barrelInventory)) {
			// If dragging only over the barrel inventory, just deposit all dragged potions.
			// This can happen when a click turns into a tiny drag for example.
			// Modifying the event is not possible, and when cancelling the event the cursor item is reset
			// to what it was before the event, thus need to run everything a bit delayed.
			event.setCancelled(true);
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				ItemStack cursor = player.getItemOnCursor();
				if (ItemUtils.isSomePotion(cursor)) {
					ItemStack barrelPotion = getBarrelPotion(barrelInventory);
					if (barrelPotion == null || barrelPotion.isSimilar(cursor)) {
						if (addToBarrel(barrelInventory, cursor)) {
							player.setItemOnCursor(cursor);
						} else {
							errorSound(player);
						}
					} else {
						errorSound(player);
					}
				} else {
					errorSound(player);
				}
			});
		}
		// if dragging over multiple inventories, just prevent the event entirely.
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void playerInteractEvent(PlayerInteractEvent event) {
		Block clickedBlock = event.getClickedBlock();
		if (event.getAction() == Action.LEFT_CLICK_BLOCK
				&& clickedBlock != null
				&& isPotionBarrel(clickedBlock)) {
			Barrel barrel = (Barrel) clickedBlock.getState(false);
			Player player = event.getPlayer();
			if (player.isSneaking()) {
				// fill inventory
				CoreProtectIntegration.logContainerTransaction(player, clickedBlock);
				takeAll(barrel.getInventory(), player);
			} else {
				// deposit all matching potions
				ItemStack barrelPotion = getBarrelPotion(barrel.getInventory());
				if (barrelPotion != null) {
					CoreProtectIntegration.logContainerTransaction(player, clickedBlock);
					int added = 0;
					for (ItemStack playerItem : player.getInventory()) {
						if (playerItem != null && playerItem.isSimilar(barrelPotion)) {
							if (addToBarrel(barrel.getInventory(), playerItem)) {
								added++;
							}
						}
					}
					if (added > 0) {
						player.sendMessage(Component.text("Deposited " + added + " " + ItemUtils.getPlainName(barrelPotion) + ".", NamedTextColor.GRAY));
					}
				}
			}
		}
	}

	private @Nullable ItemStack getBarrelPotion(Inventory barrelInventory) {
		for (ItemStack item : barrelInventory) {
			if (!ItemUtils.isNullOrAir(item)) {
				return item;
			}
		}
		return null;
	}

	private boolean addToBarrel(Inventory barrelInventory, ItemStack potion) {
		for (int i = 0; i < barrelInventory.getSize(); i++) {
			ItemStack item = barrelInventory.getItem(i);
			if (ItemUtils.isNullOrAir(item)) {
				barrelInventory.setItem(i, ItemUtils.clone(potion));
				potion.setAmount(0);
			} else if (item.isSimilar(potion)) {
				int remainingSpace = 64 - item.getAmount();
				int added = Math.min(remainingSpace, potion.getAmount());
				if (added > 0) {
					item.setAmount(item.getAmount() + added);
					potion.setAmount(potion.getAmount() - added);
				}
			}
		}
		return potion.getAmount() == 0;
	}

	private void takeAll(Inventory barrelInventory, Player player) {
		int remainingSpace = InventoryUtils.numEmptySlots(player.getInventory());
		if (remainingSpace == 0) {
			return;
		}
		for (int i = barrelInventory.getSize() - 1; i >= 0; i--) {
			ItemStack item = barrelInventory.getItem(i);
			if (!ItemUtils.isNullOrAir(item)) {
				while (item.getAmount() > 0) {
					ItemStack clone = ItemUtils.clone(item);
					clone.setAmount(1);
					item.setAmount(item.getAmount() - 1);
					player.getInventory().addItem(clone);
					remainingSpace--;
					if (remainingSpace == 0) {
						return;
					}
				}
			}
		}
	}

	private void removeOne(Inventory barrelInventory, ItemStack item) {
		for (int i = barrelInventory.getSize() - 1; i >= 0; i--) {
			ItemStack barrelItem = barrelInventory.getItem(i);
			if (barrelItem != null && barrelItem.isSimilar(item)) {
				barrelItem.setAmount(barrelItem.getAmount() - 1);
				return;
			}
		}
	}

	private static void errorSound(Player player) {
		player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1, 1);
	}

	// block handling

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void blockExplodeEvent(BlockExplodeEvent event) {
		event.blockList().removeIf(PotionBarrelListener::isPotionBarrel);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		event.blockList().removeIf(PotionBarrelListener::isPotionBarrel);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		if (isPotionBarrel(event.getBlock())
				&& !((Barrel) event.getBlock().getState()).getInventory().isEmpty()) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(Component.text("You cannot break a filled " + POTION_BARREL_NAME + "! Empty it first.", NamedTextColor.RED));
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void plockPlaceEvent(BlockPlaceEvent event) {
		if (isPotionBarrel(event.getItemInHand())
				&& !isValidLocation(event.getBlock().getLocation())) {
			event.setCancelled(true);
		}
	}

	// Replace drop on breaking the barrel with the item from the loot table (with description text)
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void blockDropItemEvent(BlockDropItemEvent event) {
		if (event.getBlockState() instanceof Barrel barrel
				&& POTION_BARREL_NAME.equals(MessagingUtils.plainText(barrel.customName()))) {
			ItemStack potionBarrel = InventoryUtils.getItemFromLootTable(event.getBlock().getLocation(), POTION_BARREL_LOOT_TABLE);
			if (potionBarrel != null) {
				event.getItems().removeIf(item -> item.getItemStack().getType() == Material.BARREL);
				event.getItems().add(event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().add(0.5, 0, 0.5), potionBarrel));
			}
		}
	}

	private static boolean isPotionBarrel(ItemStack item) {
		return item.getType() == Material.BARREL
				&& item.getItemMeta() instanceof BlockStateMeta blockStateMeta
				&& blockStateMeta.getBlockState() instanceof Barrel barrel
				&& POTION_BARREL_NAME.equals(MessagingUtils.plainText(barrel.customName()));
	}

	private static boolean isPotionBarrel(Block block) {
		return block.getType() == Material.BARREL
				&& block.getState() instanceof Barrel barrel
				&& POTION_BARREL_NAME.equals(MessagingUtils.plainText(barrel.customName()));
	}

	private static boolean isValidLocation(Location location) {
		return ServerProperties.getShardName().equals("playerplots")
			       || ServerProperties.getShardName().startsWith("dev")
			       || (ServerProperties.getShardName().equals("plots") && !ZoneUtils.hasZoneProperty(location, ZoneUtils.ZoneProperty.SHOPS_POSSIBLE));
	}

}
