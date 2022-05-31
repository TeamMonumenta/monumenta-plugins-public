package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.inventories.ShulkerInventoryManager;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

/**
 * Handles quivers, which are shulker boxes for arrows.
 * Arrows are taken from them to shoot, and arrows being picked up are put in there before the inventory.
 */
public class QuiverListener implements Listener {

	// Refill if the used arrow stack has less than this many arrows left.
	private static final int REFILL_LOWER_THAN = 16;

	// Refill arrows up to this amount. This is less than max stack size to prevent using an infinity crossbow (or multiple in a row) starting a new stack.
	private static final int REFILL_UP_TO = 48;

	public enum ArrowTransformMode {
		NONE("disabled", null),
		NORMAL("Normal Arrows", new ItemStack(Material.ARROW)),
		SPECTRAL("Spectral Arrows", new ItemStack(Material.SPECTRAL_ARROW)),
		WEAKNESS("Arrows of Weakness", makeTippedArrowStack(PotionType.WEAKNESS)),
		SLOWNESS("Arrows of Slowness", makeTippedArrowStack(PotionType.SLOWNESS)),
		POISON("Arrows of Poison", makeTippedArrowStack(PotionType.POISON)),
		;

		private final String mArrowName;

		private final @Nullable ItemStack mItemStack;

		ArrowTransformMode(String arrowName, @Nullable ItemStack itemStack) {
			mArrowName = arrowName;
			mItemStack = itemStack;
		}

		public String getArrowName() {
			return mArrowName;
		}
	}

	private static ItemStack makeTippedArrowStack(PotionType potionType) {
		ItemStack result = new ItemStack(Material.TIPPED_ARROW);
		PotionMeta meta = ((PotionMeta) result.getItemMeta());
		meta.setBasePotionData(new PotionData(potionType));
		result.setItemMeta(meta);
		return result;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityShootBowEvent(EntityShootBowEvent event) {
		if (event.getEntity() instanceof Player player) {
			refillInventoryDelayed(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityLoadCrossbowEvent(EntityLoadCrossbowEvent event) {
		if (event.getEntity() instanceof Player player) {
			refillInventoryDelayed(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerInteractEvent(PlayerInteractEvent event) {
		// When right-clicking with a bow or crossbow while having no arrows in the inventory, take some out of a quiver is available
		Player player = event.getPlayer();
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
			    && (ItemUtils.isSomeBow(mainHand) || ItemUtils.isSomeBow(player.getInventory().getItemInOffHand()))
			    && Arrays.stream(player.getInventory().getContents()).noneMatch(ItemUtils::isArrow)) {
			refillInventoryImmediately(player);
		} else if ((event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR)
			           && ItemStatUtils.isArrowTransformingQuiver(mainHand)
			           && MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, "QuiverSwapModeThisTick")) {
			ArrowTransformMode mode = ItemStatUtils.getArrowTransformMode(mainHand);
			ArrowTransformMode[] allModes = ArrowTransformMode.values();
			ArrowTransformMode newMode = allModes[(mode.ordinal() + 1) % allModes.length];
			ItemStack newStack = ItemStatUtils.setArrowTransformMode(mainHand, newMode);
			ItemStatUtils.generateItemStats(newStack);
			player.getInventory().setItemInMainHand(newStack);
			player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.BLOCKS, 1, 1);
			if (newMode == ArrowTransformMode.NONE) {
				player.sendActionBar(Component.text("Quiver has been set to not transform arrows.", NamedTextColor.WHITE));
			} else {
				player.sendActionBar(Component.text("Quiver has been set to transform arrows to ", NamedTextColor.WHITE)
					.append(Component.text(newMode.getArrowName(), NamedTextColor.GOLD)));
			}
		}
	}

	// Refill delayed to execute after the event for bow shot/crossbow load (top execute after the arrow has been used)
	private void refillInventoryDelayed(Player player) {
		if (player.getGameMode() == GameMode.CREATIVE) {
			return;
		}
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			refillInventoryImmediately(player);
		});
	}

	// Refill immediately if the event allows
	private void refillInventoryImmediately(Player player) {
		if (player.getGameMode() == GameMode.CREATIVE) {
			return;
		}
		PlayerInventory inventory = player.getInventory();
		for (ItemStack arrow : inventory) {
			// Look for the first arrow stack in the player's inventory
			if (!ItemUtils.isArrow(arrow)) {
				continue;
			}
			// If that stack still has enough arrows, stop
			if (arrow.getAmount() >= REFILL_LOWER_THAN) {
				return;
			}
			// Search for quivers in the inventory and use them to restock that stack
			for (ItemStack quiver : inventory) {
				if (!ItemStatUtils.isQuiver(quiver)
					    || !(quiver.getItemMeta() instanceof BlockStateMeta blockStateMeta)
					    || !(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)) {
					continue;
				}
				boolean modified = false;
				// Move matching arrows until the stack to refill is almost full
				for (ItemStack quiverArrow : shulkerBox.getInventory()) {
					if (arrow.isSimilar(quiverArrow)) {
						int transferred = Math.min(REFILL_UP_TO - arrow.getAmount(), quiverArrow.getAmount());
						quiverArrow.subtract(transferred);
						arrow.add(transferred);
						modified = true;
						if (arrow.getAmount() >= REFILL_UP_TO) {
							break;
						}
					}
				}
				if (modified) {
					blockStateMeta.setBlockState(shulkerBox);
					quiver.setItemMeta(blockStateMeta);
				}
				if (arrow.getAmount() >= REFILL_UP_TO) {
					return;
				}
			}
			// no quiver found, or not enough arrows for a full stack - stop here.
			return;
		}

		// No arrows found in the inventory - the last arrow of its type was used up.
		// Search for a quiver and take out the first stack of arrows.
		if (!InventoryUtils.isFull(inventory)) {
			for (ItemStack quiver : inventory) {
				if (!ItemStatUtils.isQuiver(quiver)
					    || !(quiver.getItemMeta() instanceof BlockStateMeta blockStateMeta)
					    || !(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)) {
					continue;
				}
				for (ItemStack arrow : shulkerBox.getInventory()) {
					if (!ItemUtils.isArrow(arrow)) {
						continue;
					}
					ItemStack moved = arrow.clone();
					moved.setAmount(Math.min(arrow.getAmount(), REFILL_UP_TO));
					inventory.addItem(moved);
					arrow.subtract(moved.getAmount());
					blockStateMeta.setBlockState(shulkerBox);
					quiver.setItemMeta(blockStateMeta);
					return; // can directly return from here
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerAttemptPickupItemEvent(PlayerAttemptPickupItemEvent event) {
		handlePickupEvent(event, event.getItem(), event.getPlayer());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerPickupArrowEvent(PlayerPickupArrowEvent event) {
		handlePickupEvent(event, event.getItem(), event.getPlayer());
	}

	private void handlePickupEvent(Cancellable event, Item item, Player player) {
		if (player.getGameMode() == GameMode.CREATIVE || !item.isValid()) {
			return;
		}
		// If an arrow is picked up, put it into a quiver if space is available
		ItemStack itemStack = item.getItemStack();
		if (!ItemUtils.isArrow(itemStack)) {
			return;
		}
		for (ItemStack quiver : player.getInventory()) {
			if (!ItemStatUtils.isQuiver(quiver)
				    || !(quiver.getItemMeta() instanceof BlockStateMeta blockStateMeta)
				    || !(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)
				    || ShulkerInventoryManager.isShulkerInUse(quiver)) {
				continue;
			}
			ItemStack transformedItemStack = getTransformedArrowStack(quiver, itemStack);
			int oldAmount = transformedItemStack.getAmount();
			InventoryUtils.insertItemIntoLimitedInventory(shulkerBox.getInventory(), ItemStatUtils.getShulkerSlots(quiver), transformedItemStack);
			if (oldAmount != transformedItemStack.getAmount()) {
				blockStateMeta.setBlockState(shulkerBox);
				quiver.setItemMeta(blockStateMeta);
				if (transformedItemStack.getAmount() == 0) {
					event.setCancelled(true);
					player.playPickupItemAnimation(item);
					item.remove();
					return;
				} else {
					itemStack.setAmount(transformedItemStack.getAmount());
					item.setItemStack(itemStack);
				}
			}
		}
	}

	public static ItemStack getTransformedArrowStack(ItemStack quiver, ItemStack arrows) {
		if (Arrays.stream(ArrowTransformMode.values()).noneMatch(m -> arrows.isSimilar(m.mItemStack))) {
			return arrows;
		}
		ArrowTransformMode mode = ItemStatUtils.getArrowTransformMode(quiver);
		if (mode.mItemStack == null) { // i.e. not transformed
			return arrows;
		}
		ItemStack transformed = mode.mItemStack.clone();
		transformed.setAmount(arrows.getAmount());
		return transformed;
	}

}
