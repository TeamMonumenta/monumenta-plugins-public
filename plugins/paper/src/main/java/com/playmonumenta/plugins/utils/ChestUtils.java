package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.delves.DelveLootTableGroup;
import com.playmonumenta.plugins.listeners.LootTableManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTItem;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.jetbrains.annotations.Nullable;

public class ChestUtils {
	private static final int LOOTBOX_EPIC_MAX_SIZE = 100;
	private static final int LOOTBOX_WARN_FREE_SPACES = 6;

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
					MMLog.severe("Player '" + player.getName() + " opened loot chest '" + lootTable.getKey() + "' which wasn't loaded by LootTableManager");
					luckAmount = 0;
					otherPlayers = Collections.emptyList();
				} else if (ZoneUtils.hasZoneProperty(inventory.getLocation() != null ? inventory.getLocation() : player.getLocation(), ZoneUtils.ZoneProperty.LOOTROOM)) {
					// Loot scaling is disabled (dungeon loot rooms)
					luckAmount = 0;
					otherPlayers = Collections.emptyList();
				} else if (!lootEntry.hasBonusRolls()) {
					// This chest doesn't have bonus rolls, don't apply luck or distribute the chest results
					MMLog.fine("Player '" + player.getName() + " opened loot chest '" + lootTable.getKey() + "' which did not have scaling/lootbox enabled");
					luckAmount = 0;
					otherPlayers = Collections.emptyList();
				} else {
					// Loot scaling is enabled
					MMLog.fine("Player '" + player.getName() + " opened loot chest '" + lootTable.getKey() + "' which was scaled & distributed");

					// Get all other players in range, excluding the source player
					otherPlayers = PlayerUtils.playersInLootScalingRange(player, true);

					int otherPlayersMultiplier = otherPlayers.size() + 1;

					MMLog.fine("Lootable seed: " + lootable.getSeed());
					// Loot table seed set and use the seed for number of players
					if (lootable.getSeed() > 0 && lootable.getSeed() < 50) {
						otherPlayersMultiplier = (int)lootable.getSeed();
						MMLog.fine("Chest loot was already generated due to a spawner being broken with seed " + otherPlayersMultiplier);
					}

					double bonusItems = BONUS_ITEMS[Math.min(BONUS_ITEMS.length - 1, otherPlayersMultiplier)];
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
				// Need to make a copy here because the buckets lists are not modifiable
				List<ItemStack> itemsForOrigContainer = new ArrayList<>(itemBuckets.get(0));

				int bucketIdx = 1; // Start at 1, first one was already taken by source player
				Set<String> otherLootBoxPlayers = new TreeSet<>();
				Set<String> noSharePlayers = new TreeSet<>();
				for (Player other : otherPlayers) {
					if (ServerProperties.getLootBoxEnabled() && giveLootBoxSliceToPlayer(itemBuckets.get(bucketIdx), other)) {
						otherLootBoxPlayers.add(other.getName());
					} else {
						// Failed to give this slice to the other player (no lootbox or it is full or lootbox is disabled on this shard)
						// Put all these items in the orig container
						itemsForOrigContainer.addAll(itemBuckets.get(bucketIdx));
						noSharePlayers.add(other.getName());
					}
					bucketIdx++;
				}

				if (!otherLootBoxPlayers.isEmpty()) {
					// Sound to indicate loot was split
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 0.2f, 1.4f);

					StringJoiner otherPlayersJoiner = new StringJoiner(", ");
					for (String other : otherLootBoxPlayers) {
						otherPlayersJoiner.add(other);
					}
					Component lootboxPlayers = Component.text(otherLootBoxPlayers.size()
						+ " other nearby player" + (otherLootBoxPlayers.size() == 1 ? "" : "s"),
						NamedTextColor.GOLD)
						.hoverEvent(Component.text(otherPlayersJoiner.toString()));

					Component noSharePlayerComponent;
					if (noSharePlayers.isEmpty()) {
						noSharePlayerComponent = Component.empty();
					} else {
						StringJoiner noShareJoiner = new StringJoiner(", ");
						for (String other : noSharePlayers) {
							noShareJoiner.add(other);
						}
						noSharePlayerComponent = Component.text(noSharePlayers.size()
									+ " other player" + (noSharePlayers.size() == 1 ? "" : "s") + " got no share",
								NamedTextColor.GOLD)
							.hoverEvent(Component.text(otherPlayersJoiner.toString()));
						noSharePlayerComponent = Component.text(", and ", NamedTextColor.GOLD)
							.append(noSharePlayerComponent);
					}

					Component lootDistributedMessage = Component.text("Loot distributed to ", NamedTextColor.GOLD)
						.append(lootboxPlayers)
						.append(noSharePlayerComponent);

					if (player.getScoreboardTags().contains("ActionBarLootbox")) {
						player.sendActionBar(lootDistributedMessage);
					} else {
						lootDistributedMessage = lootDistributedMessage.hoverEvent(Component.text(otherPlayersJoiner.toString()));
						player.sendMessage(lootDistributedMessage);
					}
				}

				// Put the remainder of the loot in the original container
				ChestUtils.generateLootInventory(itemsForOrigContainer, inventory, player, true);

				// warning on build server
				if (!Plugin.IS_PLAY_SERVER) {
					player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 1, 1);
					player.sendMessage(Component.text("Loot table rolled!", NamedTextColor.RED).decorate(TextDecoration.BOLD)
						                   .append(Component.text(" (this message is only shown on the build server)", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));
				}
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
			buckets.add(new ArrayList<>(loot.subList(itemsMoved, itemsMoved + itemsToMove)));

			itemsMoved += itemsToMove;
		}

		// Shuffle the buckets themselves so the 1st player isn't more likely to get +1 item
		Collections.shuffle(buckets, FastUtils.RANDOM);

		MMLog.finer("LOOTBOX: Attempting to merge items in " + buckets.size() + " buckets");

		// Compact the buckets down to save space. Every little bit helps with the big lootbox
		// For example: compacting two CXP items into one CXP item with count 2
		for (List<ItemStack> bucket : buckets) {
			if (MMLog.isLevelEnabled(Level.FINER)) {
				MMLog.finer("LOOTBOX: Processing bucket which contains:");
				bucket.forEach((item) -> MMLog.finer("LOOTBOX:     " + item.toString()));
			}

			// Iterate from the end of the bucket backwards, attempting to merge each item
			// with the items that came before it
			for (int testIdx = bucket.size() - 1; testIdx >= 0; testIdx--) {
				// Get the item to try to merge into those before it
				ItemStack testItemToMerge = bucket.get(testIdx);

				// Iterate over all items before this one, trying to merge into them
				for (int baseIdx = 0; baseIdx < testIdx; baseIdx++) {
					ItemStack mergeToCandidate = bucket.get(baseIdx);

					if (mergeToCandidate.isSimilar(testItemToMerge) &&
						(mergeToCandidate.getAmount() + testItemToMerge.getAmount()) <= testItemToMerge.getMaxStackSize()) {
						// These items are the same and they can be combined without exceeding stack size - merge them

						MMLog.finer("LOOTBOX:   Merging item in slot " + testIdx + " into " + baseIdx);

						// Increment the earlier item
						mergeToCandidate.add(testItemToMerge.getAmount());

						// Remove the item that was merged
						bucket.remove(testIdx);

						// No more merge attempts needed for this item
						break;
					}
				}
			}

			if (MMLog.isLevelEnabled(Level.FINER)) {
				MMLog.finer("LOOTBOX: Bucket resulting contents:");
				bucket.forEach((item) -> MMLog.finer("LOOTBOX:     " + item.toString()));
			}
		}

		return buckets;
	}

	/**
	 * Puts a player's fractional split of loot in a LOOTBOX in their inventory if present.
	 *
	 * Returns whether this was possible. If false the loot should be shared back into the generating chest.
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
			ChestUtils.generateLootInventory(loot, chestMeta.getInventory(), player, false);
			blockMeta.setBlockState(chestMeta);
			lootShare.setItemMeta(blockMeta);
		}
		ItemUtils.setPlainTag(lootShare);

		if (isEpicLootBox(lootBox)) {
			// Add the item to the epic lootbox's Monumenta.Items[] inventory
			NBTItem nbt = new NBTItem(lootBox);
			NBTCompound monumenta = nbt.addCompound(ItemStatUtils.MONUMENTA_KEY);
			NBTCompound playerModified = monumenta.addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY);
			NBTCompoundList items = playerModified.getCompoundList(ItemStatUtils.ITEMS_KEY);
			items.addCompound(NBTItem.convertItemtoNBT(lootShare));

			// Refresh the lootbox item
			lootBox.setItemMeta(nbt.getItem().getItemMeta());

			// Update the lore text with the new count
			updateLootBoxSharesLore(lootBox, items.size(), true);
		} else {
			// Add the item to the normal lootbox's shulker inventory
			// Note that if we got here, that lootbox always has space available
			if (lootBox.getItemMeta() instanceof BlockStateMeta blockMeta && blockMeta.getBlockState() instanceof ShulkerBox shulkerMeta) {
				// Update the lootbox's inventory with the added item
				shulkerMeta.getInventory().addItem(lootShare);
				blockMeta.setBlockState(shulkerMeta);
				lootBox.setItemMeta(blockMeta);

				// Update the lore text with the new count
				updateLootBoxSharesLore(lootBox, 27 - countEmptyNormalLootboxSpaces(shulkerMeta.getInventory()), false);
			}
		}

		return true;
	}

	public static @Nullable List<ItemStack> removeOneLootshareFromLootbox(ItemStack lootBox) {
		if (isEpicLootBox(lootBox)) {
			return removeOneLootshareFromEpicLootbox(lootBox);
		} else {
			return removeOneLootshareFromNormalLootbox(lootBox);
		}
	}

	private static @Nullable List<ItemStack> removeOneLootshareFromEpicLootbox(ItemStack lootBox) {
		@Nullable List<ItemStack> returnContents = null;

		NBTItem nbt = new NBTItem(lootBox);
		NBTCompound monumenta = nbt.getCompound(ItemStatUtils.MONUMENTA_KEY);
		if (monumenta == null) {
			return null;
		}

		NBTCompound playerModified = monumenta.getCompound(ItemStatUtils.PLAYER_MODIFIED_KEY);
		if (playerModified == null) {
			return null;
		}

		// The epic stores items only in {Monumenta:{Items:[{item}]}}, it does not use the normal shulker contents at all
		NBTCompoundList items = playerModified.getCompoundList(ItemStatUtils.ITEMS_KEY);
		if (items == null || items.isEmpty()) {
			return null;
		}

		// Remove the first item from the list
		ItemStack lootShare = NBTItem.convertNBTtoItem(items.remove(0));
		if (lootShare == null) {
			return null;
		}

		// Update the lootbox to remove the item
		lootBox.setItemMeta(nbt.getItem().getItemMeta());

		// Get the contents of the removed chest
		if (lootShare.getItemMeta() instanceof BlockStateMeta shareBlockMeta && shareBlockMeta.getBlockState() instanceof Chest chestMeta) {
			returnContents = Arrays.stream(chestMeta.getInventory().getContents())
				.filter((item) -> item != null && !item.getType().isAir())
				.collect(Collectors.toList());
		}

		// Update the lore text with the new count
		updateLootBoxSharesLore(lootBox, items.size(), true);

		return returnContents;
	}

	public static void updateLootBoxSharesLore(ItemStack lootBox) {
		if (!isLootBox(lootBox)) {
			return;
		}
		if (isEpicLootBox(lootBox)) {
			NBTItem nbt = new NBTItem(lootBox);
			NBTCompound monumenta = nbt.addCompound(ItemStatUtils.MONUMENTA_KEY);
			NBTCompound playerModified = monumenta.addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY);
			NBTCompoundList items = playerModified.getCompoundList(ItemStatUtils.ITEMS_KEY);
			if (items != null) {
				updateLootBoxSharesLore(lootBox, items.size(), true);
			} else {
				updateLootBoxSharesLore(lootBox, 0, true);
			}
		} else if (isNormalLootBox(lootBox)) {
			int usedSlots = 0;
			if (lootBox.getItemMeta() instanceof BlockStateMeta blockMeta && blockMeta.getBlockState() instanceof ShulkerBox shulkerMeta) {
				@Nullable ItemStack[] lootBoxContents = shulkerMeta.getInventory().getContents();
				for (@Nullable ItemStack lootBoxItem : lootBoxContents) {
					if (lootBoxItem != null && !lootBoxItem.getType().equals(Material.AIR)) {
						++usedSlots;
					}
				}
			}
			updateLootBoxSharesLore(lootBox, usedSlots, false);
		}
	}

	private static void updateLootBoxSharesLore(ItemStack lootBox, int amount, boolean isEpic) {
		int max = isEpic ? LOOTBOX_EPIC_MAX_SIZE : 27;
		int loreIndex = isEpic ? 5 : 2;
		if (ItemStatUtils.getLore(lootBox).size() > loreIndex) {
			ItemStatUtils.removeLore(lootBox, loreIndex);
		}
		ItemStatUtils.addLore(lootBox, loreIndex, Component.text(amount + "/" + max + " shares").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE));
		ItemStatUtils.generateItemStats(lootBox);
	}

	private static @Nullable List<ItemStack> removeOneLootshareFromNormalLootbox(ItemStack lootBox) {
		@Nullable List<ItemStack> returnContents = null;

		if (lootBox.getItemMeta() instanceof BlockStateMeta blockMeta && blockMeta.getBlockState() instanceof ShulkerBox shulkerMeta) {
			@Nullable ItemStack[] lootBoxContents = shulkerMeta.getInventory().getContents();

			// Find the first lootshare item in that lootbox
			@Nullable ItemStack lootShare = null;
			for (@Nullable ItemStack lootBoxItem : lootBoxContents) {
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
				returnContents = Arrays.stream(chestMeta.getInventory().getContents())
					.filter((item) -> item != null && !item.getType().isAir())
					.collect(Collectors.toList());
			}

			// Remove this lootshare item from the lootbox
			lootShare.subtract();

			// Update the lootbox's inventory with the removed item
			blockMeta.setBlockState(shulkerMeta);
			lootBox.setItemMeta(blockMeta);

			// Update the lore text with the new count
			updateLootBoxSharesLore(lootBox, 27 - countEmptyNormalLootboxSpaces(shulkerMeta.getInventory()), false);
		}

		return returnContents;
	}

	private static int countEmptyNormalLootboxSpaces(Inventory inventory) {
		int empty = 0;
		for (ItemStack subItem : inventory.getContents()) {
			if (subItem == null || subItem.getType().isAir()) {
				empty++;
			}
		}
		return empty;
	}

	private static @Nullable ItemStack getNextLootboxWithSpace(Player player) {
		ItemStack lootBox = null;
		int numAvailSpaces = 0;
		boolean foundLootBox = false;

		for (ItemStack item : player.getInventory().getContents()) {
			if (isLootBox(item)) {
				foundLootBox = true;

				if (isEpicLootBox(item)) {
					// Epic lootbox
					NBTItem nbt = new NBTItem(item);
					NBTCompound monumenta = nbt.addCompound(ItemStatUtils.MONUMENTA_KEY);
					NBTCompound playerModified = monumenta.addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY);
					NBTCompoundList items = playerModified.getCompoundList(ItemStatUtils.ITEMS_KEY);
					if (items != null) {
						int availSpaces = LOOTBOX_EPIC_MAX_SIZE - items.size();
						// Will return the first available box
						if (availSpaces > 0 && lootBox == null) {
							lootBox = item;
						}
						numAvailSpaces += availSpaces;
					}
				} else if (item.getItemMeta() instanceof BlockStateMeta blockMeta && blockMeta.getBlockState() instanceof ShulkerBox shulkerMeta) {
					// Normal lootbox
					int availSpaces = countEmptyNormalLootboxSpaces(shulkerMeta.getInventory());
					// Will return the first available box
					if (availSpaces > 0 && lootBox == null) {
						lootBox = item;
					}
					numAvailSpaces += availSpaces;
				}

				if (numAvailSpaces > LOOTBOX_WARN_FREE_SPACES) {
					// No reason to keep examining boxes, there's at least the threshold number remaining to not complain about space
					break;
				}
			}
		}

		if (foundLootBox) {
			if (numAvailSpaces > LOOTBOX_WARN_FREE_SPACES) {
				// Plenty of space
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.8f, 1.2f);
				MessagingUtils.sendActionBarMessage(player, "LOOTBOX chest added", NamedTextColor.GREEN);
			} else if (numAvailSpaces > 0) {
				// Only a few spaces left
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 0.5f, 1.5f);
				MessagingUtils.sendActionBarMessage(player, "LOOTBOX chest added, " + (numAvailSpaces - 1) + " spaces left", NamedTextColor.YELLOW);
			} else {
				// No space left
				player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.8f, 1.8f);
				MessagingUtils.sendActionBarMessage(player, "LOOTBOX is full", NamedTextColor.RED);
			}
		}

		return lootBox;
	}

	public static boolean isLootBox(ItemStack item) {
		if (item == null || !ItemUtils.isShulkerBox(item.getType()) || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
			return false;
		}

		String plainName = ItemUtils.getPlainName(item);
		return plainName.equals("LOOTBOX") || plainName.equals("Box of Endless Echoes") || plainName.equals("Mouth of the Mimic");
	}

	public static boolean isEpicLootBox(ItemStack item) {
		if (item == null || !ItemUtils.isShulkerBox(item.getType()) || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
			return false;
		}

		String plainName = ItemUtils.getPlainName(item);
		return plainName.equals("Box of Endless Echoes") || plainName.equals("Mouth of the Mimic");
	}

	public static boolean isNormalLootBox(ItemStack item) {
		if (item == null || !ItemUtils.isShulkerBox(item.getType()) || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
			return false;
		}

		return ItemUtils.getPlainName(item).equals("LOOTBOX");
	}

	public static boolean isLootShare(ItemStack item) {
		return item != null &&
			   ItemUtils.isShulkerBox(item.getType()) &&
			   item.hasItemMeta() &&
			   item.getItemMeta().hasDisplayName() &&
			   ItemUtils.getPlainName(item).equals("Loot Share");
	}

	public static void generateLootInventory(Collection<ItemStack> populatedLoot, Inventory inventory, Player player, boolean randomlyDistribute) {
		List<ItemStack> lootList = populatedLoot.stream()
			.filter((item) -> item != null && !item.getType().isAir()).toList();

		List<Integer> freeSlots = new ArrayList<>(27);
		for (int i = 0; i < 27; i++) {
			freeSlots.add(i);
		}
		if (randomlyDistribute) {
			Collections.shuffle(freeSlots);
		}

		MMLog.finer("generateLootInventory: Started with " + lootList.size() + " items and randomlyDistribute=" + randomlyDistribute);
		ArrayDeque<Integer> slotsWithMultipleItems = new ArrayDeque<>();
		for (ItemStack lootItem : lootList) {
			if (freeSlots.size() == 0) {
				Plugin.getInstance().getLogger().severe("Tried to overfill container for player " + player.getName() + " at inventory " + inventory.getType() + " at location " + player.getLocation());
				player.sendMessage("Tried to overfill this container! Please report this");
				break;
			}
			int slot = freeSlots.remove(0);
			inventory.setItem(slot, lootItem);
			if (MMLog.isLevelEnabled(Level.FINER)) { // Performance optimization to avoid calling lootItem.toString() when this log level is disabled
				MMLog.finer("generateLootInventory: Putting item in slot " + slot + ": " + lootItem.toString());
			}
			if (lootItem.getAmount() > 1) {
				MMLog.finer("generateLootInventory: Adding slot " + slot + " to multiple items list");
				slotsWithMultipleItems.add(slot);
			}
		}

		while (randomlyDistribute && freeSlots.size() > 1 && slotsWithMultipleItems.size() > 0) {
			int splitSlot = slotsWithMultipleItems.remove();
			int slot = freeSlots.remove(0);

			ItemStack toSplitItem = inventory.getItem(splitSlot);
			if (toSplitItem == null) {
				continue;
			}
			ItemStack splitItem = toSplitItem.clone();
			int amountToSplit = toSplitItem.getAmount() / 2;
			int amountRemaining = toSplitItem.getAmount() - amountToSplit;

			if (MMLog.isLevelEnabled(Level.FINER)) {
				MMLog.finer("generateLootInventory: Splitting item type " + toSplitItem.getType() +
				            " with count " + toSplitItem.getAmount() + " in slot " + splitSlot +
							" into count " + amountRemaining + " and " + amountToSplit + " in slot " + slot);
			}

			toSplitItem.setAmount(amountRemaining);
			splitItem.setAmount(amountToSplit);
			inventory.setItem(slot, splitItem);

			if (amountToSplit > 1) {
				MMLog.finer("generateLootInventory: Adding slot " + slot + " to multiple items list");
				slotsWithMultipleItems.add(slot);
		    }
			if (amountRemaining > 1) {
				MMLog.finer("generateLootInventory: Adding slot " + splitSlot + " to multiple items list");
				slotsWithMultipleItems.add(splitSlot);
			}
		}
	}

	public static boolean isUnscaledChest(Block block) {
		return block.getState() instanceof Chest chest && chest.getLootTable() != null &&
			       chest.getSeed() == 0 && isChestBlockEmpty(chest);
	}

	public static boolean isAstrableChest(Chest chest) {
		return DelveLootTableGroup.hasDelveableLootTable(chest) && isChestBlockEmpty(chest);
	}

	/**
	 * Checks if the inventory of this chest block is empty (i.e. only checks one half of a double chest)
	 *
	 * @see #isEmpty(Chest)
	 */
	public static boolean isChestBlockEmpty(Chest chest) {
		return Arrays.stream(chest.getBlockInventory().getContents()).allMatch(ItemUtils::isNullOrAir);
	}

	public static boolean isEmpty(Block block) {
		return block.getState() instanceof Chest && isEmpty((Chest) block.getState());
	}

	/**
	 * Checks if the inventory of this entire chest is empty (i.e. checks both halves of a double chest)
	 *
	 * @see #isChestBlockEmpty(Chest)
	 */
	public static boolean isEmpty(Chest chest) {
		return Arrays.stream(chest.getInventory().getContents()).allMatch(ItemUtils::isNullOrAir);
	}

	public static boolean isChestWithLootTable(Block block) {
		Material type = block.getType();
		return (type == Material.CHEST || type == Material.TRAPPED_CHEST)
			       && block.getState() instanceof Chest chest
			       && (chest.hasLootTable() || (chest.getInventory() instanceof DoubleChestInventory doubleChestInventory
				                                    && (((Chest) doubleChestInventory.getLeftSide().getHolder()).hasLootTable() || ((Chest) doubleChestInventory.getRightSide().getHolder()).hasLootTable())));
	}

	public static ItemStack giveChestWithLootTable(String lootTable, String chestName, String chestNameColor, List<Component> lore) {
		ItemStack chest = new ItemStack(Material.CHEST);
		if (chest.getItemMeta() instanceof BlockStateMeta blockMeta && blockMeta.getBlockState() instanceof Chest chestMeta) {
			blockMeta.displayName(Component.text(chestName).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true).color(TextColor.fromHexString(chestNameColor)));
			chestMeta.setLootTable(Bukkit.getLootTable(NamespacedKey.fromString(lootTable)));
			blockMeta.setBlockState(chestMeta);
			blockMeta.lore(lore);
			chest.setItemMeta(blockMeta);
		}
		ItemUtils.setPlainTag(chest);

		return chest;
	}

}
