package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.listeners.LootTableManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;

public class ChestUtils {

	private static final double[] BONUS_ITEMS = {
			0, // Dummy value, this is a player count indexed array
			0.5,
			1.7,
			2.6,
			3.3,
			3.8,
			4.2,
			4.4,
			4.5
	};

	public static void generateContainerLootWithScaling(Player player, Block block) {
		BlockState blockState = block.getState();
		if (blockState instanceof Container) {
			Inventory inventory = ((Container) blockState).getInventory();
			if (inventory instanceof DoubleChestInventory) {
				generateContainerLootWithScaling(player, ((DoubleChestInventory) inventory).getLeftSide());
				generateContainerLootWithScaling(player, ((DoubleChestInventory) inventory).getRightSide());
			} else {
				generateContainerLootWithScaling(player, inventory);
			}
		}
	}

	private static void generateContainerLootWithScaling(Player player, Inventory inventory) {
		if (inventory.getHolder() instanceof Lootable lootable) {

			LootTable lootTable = lootable.getLootTable();
			if (lootTable != null) {
				/* Figure out what the luck level should be and which players contribute to scaling */
				int luckAmount; // The amount of luck the loot table should be rolled with
				List<Player> otherPlayers; // Other players not including the rolling player that may receive loot slices

				LootTableManager.LootTableEntry lootEntry = LootTableManager.getLootTableEntry(lootTable.getKey());
				if (lootEntry == null) {
					// This loot table doesn't exist, likely an error
					MMLog.severe("Player '" + player.getName() + " opened loot chest '" + lootTable.getKey().toString() + "' which wasn't loaded by LootTableManager");
					luckAmount = 0;
					otherPlayers = Collections.emptyList();
				} else if (ScoreboardUtils.getScoreboardValue(player, "ChestLuckToggle").orElse(0) <= 0) {
					// Loot scaling is disabled (dungeon loot rooms)
					luckAmount = 0;
					otherPlayers = Collections.emptyList();
				} else if (lootEntry != null && !lootEntry.hasBonusRolls()) {
					// This chest doesn't have bonus rolls, don't apply luck or distribute the chest results
					MMLog.fine("Player '" + player.getName() + " opened loot chest '" + lootTable.getKey().toString() + "' which did not have scaling/lootbox enabled");
					luckAmount = 0;
					otherPlayers = Collections.emptyList();
				} else {
					// Loot scaling is enabled
					MMLog.fine("Player '" + player.getName() + " opened loot chest '" + lootTable.getKey().toString() + "' which was scaled & distributed");

					// Get all other players in range, excluding the source player
					otherPlayers = PlayerUtils.playersInRange(player.getLocation(), ServerProperties.getLootScalingRadius(), true);
					otherPlayers.remove(player);

					double bonusItems = BONUS_ITEMS[Math.min(BONUS_ITEMS.length - 1, otherPlayers.size() + 1)];
					luckAmount = (int) bonusItems;

					// Account for fractions of extra items with random roll
					if (FastUtils.RANDOM.nextDouble() < bonusItems - luckAmount) {
						luckAmount++;
					}
				}

				// Actually roll the loot into a collection of items
				LootContext.Builder builder = new LootContext.Builder(player.getLocation());
				builder.luck(luckAmount);
				LootContext context = builder.build();
				Collection<ItemStack> popLoot = lootTable.populateLoot(FastUtils.RANDOM, context);
				// Clear the original chest (vanilla behavior, loot table overrides whatever is in the chest, doesn't add to it
				inventory.clear();

				// Divide the items up into fractional buckets for all the players
				List<List<ItemStack>> itemBuckets = distributeLootToBuckets(new ArrayList<>(popLoot), otherPlayers.size() + 1);

				// The orig container starts with the first bucket of items
				// Need to make a copy here because the buckets lists are not modifyable
				List<ItemStack> itemsForOrigContainer = new ArrayList<ItemStack>(itemBuckets.get(0));

				int bucketIdx = 1; // Start at 1, first one was already taken by source player
				int numOtherLootBoxes = 0;
				for (Player other : otherPlayers) {
					if (giveLootBoxSliceToPlayer(itemBuckets.get(bucketIdx), other)) {
						numOtherLootBoxes++;
					} else {
						// Failed to give this slice to the other player (no lootbox or it is full)
						// Put all these items in the orig container
						itemsForOrigContainer.addAll(itemBuckets.get(bucketIdx));
					}
					bucketIdx++;
				}

				if (numOtherLootBoxes > 0) {
					// Sound to indicate loot was split
					// /playsound minecraft:block.note_block.bass player @s ~ ~ ~ 0.2 1.4
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 0.2f, 1.4f);
					MessagingUtils.sendActionBarMessage(player, "Loot distributed to " + numOtherLootBoxes + " other nearby player" + (numOtherLootBoxes == 1 ? "" : "s"));
				}

				// Put the remainder of the loot in the original container
				ChestUtils.generateLootInventory(itemsForOrigContainer, inventory, player);
			}
		}
	}

	/**
	 * Distribute a collection of items into a number of distinct buckets.
	 *
	 * Biggest challenge here is to get things sort of evenly distributed. A lot of different tuning is possible here.
	 *
	 * TODO: Someday it would be nice to have many invocations of this function somehow spread out rares evenly among players
	 * This would require some state keeping about who has gotten what. Maybe just counters based on tiers? Tricky...
	 *
	 * This is right now the most fair shuffle I can come up with
	 *
	 * Caller should take care not to actually change the returned lists, they are sublists of the original input list
	 */
	private static List<List<ItemStack>> distributeLootToBuckets(List<ItemStack> loot, int numBuckets) {
		// Output buckets
		List<List<ItemStack>> buckets = new ArrayList<>(numBuckets);

		// No reason to shuffle things around if numBuckets is 1
		if (numBuckets == 1) {
			buckets.add(loot);
			return buckets;
		}

		// TODO: Check if items in the input are stacked or not, if they are, unstack them

		// Shuffle the input items so there is no bias to the first player getting the first item every time
		Collections.shuffle(loot, FastUtils.RANDOM);

		// Compute the min items per bucket
		int minItemsPerBucket = (loot.size() / numBuckets);
		// Compute how many buckets need 1 additional item
		int numBucketsWithExtra = (loot.size() % numBuckets);

		int itemsMoved = 0;
		for (int i = 0; i < numBuckets; i++) {
			final int itemsToMove;
			if (i < numBucketsWithExtra) {
				itemsToMove = minItemsPerBucket + 1;
			} else {
				itemsToMove = minItemsPerBucket;
			}

			// Pull off that many items from the input list and put them in a bucket
			buckets.add(loot.subList(itemsMoved, itemsMoved + itemsToMove));

			itemsMoved += itemsToMove;
		}

		// Shuffle the buckets themselves so the 1st player isn't more likely to get +1 item
		Collections.shuffle(buckets, FastUtils.RANDOM);

		return buckets;
	}

	/**
	 * Puts a player's fractional split of loot in a LOOTBOX in their inventory if present.
	 *
	 * Returns whether or not this was possible. If false the loot should be shared back into the generating chest.
	 */
	private static boolean giveLootBoxSliceToPlayer(List<ItemStack> loot, Player player) {
		ItemStack lootBox = getNextLootboxWithSpace(player);
		// No lootbox space. Indicate to the caller that the items should be distributed in the original chest
		if (lootBox == null) {
			return false;
		}

		// Create a new Loot Share container and add the loot to it
		ItemStack lootShare = new ItemStack(Material.CHEST);
		if (lootShare.getItemMeta() instanceof BlockStateMeta blockMeta && blockMeta.getBlockState() instanceof Chest chestMeta) {
			blockMeta.displayName(Component.text("Loot Share").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.LIGHT_PURPLE));
			ChestUtils.generateLootInventory(loot, chestMeta.getInventory(), player);
			blockMeta.setBlockState(chestMeta);
			lootShare.setItemMeta(blockMeta);
		}
		ItemUtils.setPlainTag(lootShare);

		// Add the item to the lootbox
		// Note that if we got here, that lootbox always has space available
		if (lootBox.getItemMeta() instanceof BlockStateMeta blockMeta && blockMeta.getBlockState() instanceof ShulkerBox shulkerMeta) {
			// Update the lootbox's inventory with the added item
			shulkerMeta.getInventory().addItem(lootShare);
			blockMeta.setBlockState(shulkerMeta);
			lootBox.setItemMeta(blockMeta);

			// Update the lore text with the new count
			int emptySpaces = countEmptySpaces(shulkerMeta.getInventory());
			if (ItemStatUtils.getLore(lootBox).size() >= 3) {
				ItemStatUtils.removeLore(lootBox, 2);
			}
			ItemStatUtils.addLore(lootBox, 2, Component.text((27 - emptySpaces) + "/27 shares").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE));
			ItemStatUtils.generateItemStats(lootBox);
		}

		return true;
	}

	public static @Nullable ItemStack[] removeOneLootshareFromLootbox(ItemStack lootBox) {
		@Nullable ItemStack[] returnContents = null;

		if (lootBox.getItemMeta() instanceof BlockStateMeta blockMeta && blockMeta.getBlockState() instanceof ShulkerBox shulkerMeta) {
			@Nullable ItemStack[] lootBoxContents = shulkerMeta.getInventory().getContents();

			// Find the first lootshare item in that lootbox
			@Nullable ItemStack lootShare = null;
			for (ItemStack lootBoxItem : lootBoxContents) {
				if (lootBoxItem != null && lootBoxItem.getType().equals(Material.CHEST)) {
					lootShare = lootBoxItem;
					break;
				}
			}

			if (lootShare == null) {
				// LootBox is empty
				return null;
			}

			if (lootShare.getItemMeta() instanceof BlockStateMeta shareBlockMeta && shareBlockMeta.getBlockState() instanceof Chest chestMeta) {
				returnContents = chestMeta.getInventory().getContents();
			}

			// Remove this lootshare item from the lootbox
			lootShare.subtract();

			// Update the lootbox's inventory with the removed item
			blockMeta.setBlockState(shulkerMeta);
			lootBox.setItemMeta(blockMeta);

			// Update the lore text with the new count
			int emptySpaces = countEmptySpaces(shulkerMeta.getInventory());
			if (ItemStatUtils.getLore(lootBox).size() >= 3) {
				ItemStatUtils.removeLore(lootBox, 2);
			}
			ItemStatUtils.addLore(lootBox, 2, Component.text((27 - emptySpaces) + "/27 shares").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE));
			ItemStatUtils.generateItemStats(lootBox);
		}

		return returnContents;
	}

	private static int countEmptySpaces(Inventory inventory) {
		int empty = 0;
		for (ItemStack subitem : inventory.getContents()) {
			if (subitem == null || subitem.getType().isAir()) {
				empty++;
			}
		}
		return empty;
	}

	public static @Nullable ItemStack getNextLootboxWithSpace(Player player) {
		ItemStack lootBox = null;
		int numAvailSpaces = 0;
		boolean foundLootBox = false;

		for (ItemStack item : player.getInventory().getContents()) {
			if (isLootBox(item)) {
				foundLootBox = true;

				if (item.getItemMeta() instanceof BlockStateMeta blockmeta) {
					if (blockmeta.getBlockState() instanceof ShulkerBox shulkerMeta) {
						int availSpaces = countEmptySpaces(shulkerMeta.getInventory());
						// Will return the first available box
						if (availSpaces > 0 && lootBox == null) {
							lootBox = item;
						}
						numAvailSpaces += availSpaces;
					}
				}

			}
		}

		if (foundLootBox) {
			if (numAvailSpaces > 6) {
				// Plenty of space
				// /playsound minecraft:block.note_block.chime player @s ~ ~ ~ 0.8 1.2
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.8f, 1.2f);
				MessagingUtils.sendActionBarMessage(player, "LOOTBOX chest added", NamedTextColor.GREEN);
			} else if (numAvailSpaces > 0) {
				// Only a few spaces left
				// /playsound minecraft:block.note_block.pling player @s ~ ~ ~ 0.5 1.5
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 0.5f, 1.5f);
				MessagingUtils.sendActionBarMessage(player, "LOOTBOX chest added, " + (numAvailSpaces - 1) + " spaces left", NamedTextColor.YELLOW);
			} else {
				// No space left
				// /playsound minecraft:block.beacon.deactivate player @s ~ ~ ~ 0.8 1.8
				player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.8f, 1.8f);
				MessagingUtils.sendActionBarMessage(player, "LOOTBOX is full", NamedTextColor.RED);
			}
		}

		return lootBox;
	}

	public static boolean isLootBox(ItemStack item) {
		return item != null &&
			   ItemUtils.isShulkerBox(item.getType()) &&
			   item.hasItemMeta() &&
			   item.getItemMeta().hasDisplayName() &&
			   ItemUtils.getPlainName(item).startsWith("LOOTBOX");
	}

	public static boolean isLootShare(ItemStack item) {
		return item != null &&
			   ItemUtils.isShulkerBox(item.getType()) &&
			   item.hasItemMeta() &&
			   item.getItemMeta().hasDisplayName() &&
			   ItemUtils.getPlainName(item).equals("Loot Share");
	}

	public static void generateLootInventory(Collection<ItemStack> populatedLoot, Inventory inventory, Player player) {
		ArrayList<ItemStack> lootList = new ArrayList<>();
		for (ItemStack i : populatedLoot) {
			if (i == null) {
				i = new ItemStack(Material.AIR);
			}
			lootList.add(i);
		}

		List<Integer> freeSlots = new ArrayList<>(27);
		for (int i = 0; i < 27; i++) {
			freeSlots.add(i);
		}
		Collections.shuffle(freeSlots);

		ArrayDeque<Integer> slotsWithMultipleItems = new ArrayDeque<>();
		for (ItemStack lootItem : lootList) {
			if (freeSlots.size() == 0) {
				Plugin.getInstance().getLogger().severe("Tried to overfill container for player " + player.getName() + " at inventory " + inventory.getType().toString() + " at location " + player.getLocation().toString());
				player.sendMessage("Tried to overfill this container! Please report this");
				break;
			}
			int slot = freeSlots.remove(0);
			inventory.setItem(slot, lootItem);
			if (lootItem.getAmount() > 1) {
				slotsWithMultipleItems.add(slot);
			}
		}

		while (freeSlots.size() > 1 && slotsWithMultipleItems.size() > 0) {
			int splitslot = slotsWithMultipleItems.getFirst();
			int slot = freeSlots.remove(0);

			ItemStack toSplitItem = inventory.getItem(splitslot);
			ItemStack splitItem = toSplitItem.clone();
			int amountToSplit = toSplitItem.getAmount() / 2;

			toSplitItem.setAmount(toSplitItem.getAmount() - amountToSplit);
			splitItem.setAmount(amountToSplit);
			inventory.setItem(slot, splitItem);

			if (amountToSplit > 1) {
				slotsWithMultipleItems.add(slot);
		    }
		}
	}

	public static boolean isEmpty(Block block) {
		return block.getState() instanceof Chest && isEmpty((Chest)block.getState());
	}

	public static boolean isEmpty(Chest chest) {
		for (ItemStack slot : chest.getInventory()) {
			if (slot != null) {
				return false;
			}
		}
		return true;
	}


}
